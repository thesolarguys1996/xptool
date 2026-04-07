#pragma once

#include "xptool/core/activity_step_result.hpp"
#include "xptool/core/repeat_target_selector.hpp"

namespace xptool::core {

class StateSnapshot;

class MiningRuntime {
public:
  explicit MiningRuntime(int repeat_guard_ticks = 3);

  [[nodiscard]] ActivityStepResult step(const StateSnapshot& snapshot);
  void record_dispatch(int target_id, int tick);

private:
  RepeatTargetSelector selector_;
};

} // namespace xptool::core
