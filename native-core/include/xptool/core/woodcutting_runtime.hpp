#pragma once

#include "xptool/core/activity_step_result.hpp"
#include "xptool/core/woodcutting_target_selector.hpp"

namespace xptool::core {

class StateSnapshot;

class WoodcuttingRuntime {
public:
  explicit WoodcuttingRuntime(int repeat_guard_ticks = 3);

  [[nodiscard]] ActivityStepResult step(const StateSnapshot& snapshot);
  void record_dispatch(int target_id, int tick);

private:
  WoodcuttingTargetSelector selector_;
};

} // namespace xptool::core
