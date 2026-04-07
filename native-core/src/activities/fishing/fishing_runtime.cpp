#include "xptool/core/fishing_runtime.hpp"

#include "xptool/core/state_snapshot.hpp"

namespace xptool::core {

FishingRuntime::FishingRuntime(int repeat_guard_ticks) : selector_(repeat_guard_ticks) {}

ActivityStepResult FishingRuntime::step(const StateSnapshot& snapshot) {
  ActivityStepResult result;
  result.activity_key = "fishing";
  result.candidate_count = static_cast<int>(snapshot.fishing_candidates.size());

  const RepeatTargetSelection selection = selector_.select(snapshot.fishing_candidates, snapshot.tick);
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
  result.intent.command_type = "FISH_NEAREST_SPOT_SAFE";
  result.intent.reason_code = selection.rerouted ? "fishing_dispatch_repeat_reroute" : "fishing_dispatch_target_selected";
  result.intent.target_id = selection.selected_target_id;
  return result;
}

void FishingRuntime::record_dispatch(int target_id, int tick) { selector_.record_dispatch(target_id, tick); }

} // namespace xptool::core
