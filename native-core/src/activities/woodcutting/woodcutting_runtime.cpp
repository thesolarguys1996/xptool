#include "xptool/core/woodcutting_runtime.hpp"

#include "xptool/core/state_snapshot.hpp"

namespace xptool::core {

WoodcuttingRuntime::WoodcuttingRuntime(int repeat_guard_ticks) : selector_(repeat_guard_ticks) {}

ActivityStepResult WoodcuttingRuntime::step(const StateSnapshot& snapshot) {
  ActivityStepResult result;
  result.activity_key = "woodcutting";
  result.candidate_count = static_cast<int>(snapshot.woodcutting_candidates.size());

  const WoodcuttingTargetSelection selection = selector_.select(snapshot.woodcutting_candidates, snapshot.tick);
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
  result.intent.command_type = "WOODCUT_CHOP_NEAREST_TREE_SAFE";
  result.intent.reason_code = selection.rerouted ? "woodcutting_dispatch_repeat_reroute" : "woodcutting_dispatch_target_selected";
  result.intent.target_id = selection.selected_target_id;
  return result;
}

void WoodcuttingRuntime::record_dispatch(int target_id, int tick) { selector_.record_dispatch(target_id, tick); }

} // namespace xptool::core
