#include "xptool/core/mining_runtime.hpp"

#include "xptool/core/state_snapshot.hpp"

namespace xptool::core {

MiningRuntime::MiningRuntime(int repeat_guard_ticks) : selector_(repeat_guard_ticks) {}

ActivityStepResult MiningRuntime::step(const StateSnapshot& snapshot) {
  ActivityStepResult result;
  result.activity_key = "mining";
  result.candidate_count = static_cast<int>(snapshot.mining_candidates.size());

  const RepeatTargetSelection selection = selector_.select(snapshot.mining_candidates, snapshot.tick);
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
  result.intent.command_type = "MINE_NEAREST_ROCK_SAFE";
  result.intent.reason_code = selection.rerouted ? "mining_dispatch_repeat_reroute" : "mining_dispatch_target_selected";
  result.intent.target_id = selection.selected_target_id;
  return result;
}

void MiningRuntime::record_dispatch(int target_id, int tick) { selector_.record_dispatch(target_id, tick); }

} // namespace xptool::core
