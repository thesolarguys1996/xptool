#include "xptool/core/banking_runtime.hpp"

#include "xptool/core/state_snapshot.hpp"

namespace xptool::core {

BankingRuntime::BankingRuntime(int repeat_guard_ticks) : selector_(repeat_guard_ticks) {}

ActivityStepResult BankingRuntime::step(const StateSnapshot& snapshot) {
  ActivityStepResult result;
  result.activity_key = "banking";
  result.candidate_count = static_cast<int>(snapshot.banking_candidates.size());

  if (snapshot.bank_open) {
    result.intent.type = IntentType::kNone;
    result.intent.reason_code = "bank_already_open";
    result.selection_reason = "bank_already_open";
    return result;
  }

  const RepeatTargetSelection selection = selector_.select(snapshot.banking_candidates, snapshot.tick);
  result.selection_reason = selection.reason_code;
  result.selected_target_id = selection.selected_target_id;
  result.scored_count = selection.scored_count;
  result.blocked_repeat_count = selection.blocked_repeat_count;

  if (!selection.selected) {
    result.intent.type = IntentType::kNone;
    result.intent.reason_code = selection.reason_code;
    return result;
  }

  result.intent.type = IntentType::kInteractTarget;
  result.intent.command_type = "BANK_OPEN_SAFE";
  result.intent.reason_code = selection.rerouted ? "banking_dispatch_repeat_reroute" : "banking_dispatch_target_selected";
  result.intent.target_id = selection.selected_target_id;
  return result;
}

void BankingRuntime::record_dispatch(int target_id, int tick) { selector_.record_dispatch(target_id, tick); }

} // namespace xptool::core
