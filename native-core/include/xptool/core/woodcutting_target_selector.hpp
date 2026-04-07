#pragma once

#include "xptool/core/woodcutting_target.hpp"

#include <vector>

namespace xptool::core {

class WoodcuttingTargetSelector {
public:
  explicit WoodcuttingTargetSelector(int repeat_guard_ticks = 3);

  WoodcuttingTargetSelection select(const std::vector<WoodcuttingTargetCandidate>& candidates, int tick);
  void record_dispatch(int target_id, int tick);

private:
  bool is_repeat_blocked(int target_id, int tick) const;
  int score_candidate(const WoodcuttingTargetCandidate& candidate) const;

  int repeat_guard_ticks_;
  int last_target_id_;
  int last_target_tick_;
};

} // namespace xptool::core
