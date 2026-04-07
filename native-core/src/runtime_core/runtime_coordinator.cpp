#include "xptool/core/runtime_coordinator.hpp"

#include "xptool/core/activity_step_result.hpp"
#include "xptool/core/banking_runtime.hpp"
#include "xptool/core/combat_runtime.hpp"
#include "xptool/core/fishing_runtime.hpp"
#include "xptool/core/mining_runtime.hpp"
#include "xptool/core/motor_runtime.hpp"
#include "xptool/core/runtime_gate.hpp"
#include "xptool/core/runtime_intent.hpp"
#include "xptool/core/runtime_telemetry.hpp"
#include "xptool/core/state_snapshot.hpp"
#include "xptool/core/woodcutting_runtime.hpp"

namespace xptool::core {
namespace {

std::string normalize_activity_key(const std::string& value) {
  if (value.empty()) {
    return "woodcutting";
  }
  std::string lowered = value;
  for (char& c : lowered) {
    if (c >= 'A' && c <= 'Z') {
      c = static_cast<char>(c - 'A' + 'a');
    }
  }
  return lowered;
}

std::string activity_event_name(const std::string& activity_key, const std::string& suffix) {
  return activity_key + "_" + suffix;
}

} // namespace

RuntimeCoordinator::RuntimeCoordinator(
    WoodcuttingRuntime* woodcutting_runtime,
    MiningRuntime* mining_runtime,
    FishingRuntime* fishing_runtime,
    CombatRuntime* combat_runtime,
    BankingRuntime* banking_runtime,
    RuntimeGate* gate,
    MotorRuntime* motor_runtime)
    : woodcutting_runtime_(woodcutting_runtime),
      mining_runtime_(mining_runtime),
      fishing_runtime_(fishing_runtime),
      combat_runtime_(combat_runtime),
      banking_runtime_(banking_runtime),
      gate_(gate),
      motor_runtime_(motor_runtime) {}

RuntimeTickResult RuntimeCoordinator::on_tick(const StateSnapshot& snapshot, RuntimeTelemetry* telemetry) {
  if (woodcutting_runtime_ == nullptr || mining_runtime_ == nullptr || fishing_runtime_ == nullptr || combat_runtime_ == nullptr ||
      banking_runtime_ == nullptr || gate_ == nullptr || motor_runtime_ == nullptr) {
    RuntimeTickResult result;
    result.dispatched = false;
    result.reason_code = "runtime_not_wired";
    return result;
  }

  StateSnapshot translated_snapshot = snapshot;
  translated_snapshot.activity_key = normalize_activity_key(translated_snapshot.activity_key);
  state_translator_.ensure_woodcutting_candidates(&translated_snapshot);

  ActivityStepResult step_result;
  if (translated_snapshot.activity_key == "woodcutting") {
    step_result = woodcutting_runtime_->step(translated_snapshot);
  } else if (translated_snapshot.activity_key == "mining") {
    step_result = mining_runtime_->step(translated_snapshot);
  } else if (translated_snapshot.activity_key == "fishing") {
    step_result = fishing_runtime_->step(translated_snapshot);
  } else if (translated_snapshot.activity_key == "combat") {
    step_result = combat_runtime_->step(translated_snapshot);
  } else if (translated_snapshot.activity_key == "banking") {
    step_result = banking_runtime_->step(translated_snapshot);
  } else {
    RuntimeTickResult result;
    result.dispatched = false;
    result.reason_code = "unsupported_activity";
    return result;
  }

  const RuntimeIntent intent = step_result.intent;
  const GateDecision gate_decision = gate_->evaluate(translated_snapshot, intent.has_value());

  if (telemetry != nullptr) {
    RuntimeTelemetryEvent candidate_event;
    candidate_event.event_type = activity_event_name(step_result.activity_key, "candidate_scored");
    candidate_event.reason_code = step_result.selection_reason;
    candidate_event.data = {
        {"tick", std::to_string(translated_snapshot.tick)},
        {"activity", step_result.activity_key},
        {"candidateCount", std::to_string(step_result.candidate_count)},
        {"scoredCount", std::to_string(step_result.scored_count)},
        {"blockedRepeatCount", std::to_string(step_result.blocked_repeat_count)},
        {"selectedTargetId", std::to_string(step_result.selected_target_id)},
    };
    telemetry->record(candidate_event);
  }

  if (!gate_decision.allowed) {
    if (telemetry != nullptr) {
      RuntimeTelemetryEvent event;
      event.event_type = "runtime_tick_rejected";
      event.reason_code = gate_decision.reason_code;
      event.data = {
          {"tick", std::to_string(translated_snapshot.tick)},
          {"activity", step_result.activity_key},
          {"hasTargetCandidate", translated_snapshot.has_target_candidate ? "true" : "false"},
      };
      telemetry->record(event);

      if (gate_decision.reason_code == "no_intent") {
        RuntimeTelemetryEvent no_progress_event;
        no_progress_event.event_type = activity_event_name(step_result.activity_key, "no_progress");
        no_progress_event.reason_code = step_result.selection_reason;
        no_progress_event.data = {
            {"tick", std::to_string(translated_snapshot.tick)},
            {"selectedTargetId", std::to_string(step_result.selected_target_id)},
        };
        telemetry->record(no_progress_event);
      }
    }
    RuntimeTickResult result;
    result.dispatched = false;
    if (gate_decision.reason_code == "no_intent" && !step_result.selection_reason.empty()) {
      result.reason_code = "no_intent_" + step_result.selection_reason;
    } else {
      result.reason_code = gate_decision.reason_code;
    }
    result.command_type = intent.command_type;
    result.target_id = intent.target_id;
    return result;
  }

  motor_runtime_->enqueue(intent);
  const auto executed = motor_runtime_->execute_next();
  if (executed.has_value()) {
    if (step_result.activity_key == "woodcutting") {
      woodcutting_runtime_->record_dispatch(intent.target_id, translated_snapshot.tick);
    } else if (step_result.activity_key == "mining") {
      mining_runtime_->record_dispatch(intent.target_id, translated_snapshot.tick);
    } else if (step_result.activity_key == "fishing") {
      fishing_runtime_->record_dispatch(intent.target_id, translated_snapshot.tick);
    } else if (step_result.activity_key == "combat") {
      combat_runtime_->record_dispatch(intent.target_id, translated_snapshot.tick);
    } else if (step_result.activity_key == "banking") {
      banking_runtime_->record_dispatch(intent.target_id, translated_snapshot.tick);
    }
    gate_->record_dispatch(translated_snapshot.tick);
  }

  if (telemetry != nullptr) {
    RuntimeTelemetryEvent event;
    event.event_type = "runtime_tick_dispatched";
    event.reason_code = intent.reason_code;
    event.data = {
        {"tick", std::to_string(translated_snapshot.tick)},
        {"activity", step_result.activity_key},
        {"commandType", intent.command_type},
        {"targetId", std::to_string(intent.target_id)},
        {"executed", executed.has_value() ? "true" : "false"},
    };
    telemetry->record(event);
  }

  RuntimeTickResult result;
  result.dispatched = executed.has_value();
  result.reason_code = intent.reason_code;
  result.command_type = intent.command_type;
  result.target_id = intent.target_id;
  return result;
}

} // namespace xptool::core
