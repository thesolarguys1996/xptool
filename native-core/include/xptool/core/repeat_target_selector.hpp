#pragma once

#include <string>
#include <vector>

namespace xptool::core {

struct RepeatTargetCandidate {
  int target_id = -1;
  int distance = 0;
  bool interactable = false;
};

struct RepeatTargetSelection {
  bool selected = false;
  int selected_target_id = -1;
  bool rerouted = false;
  std::string reason_code;
  int blocked_repeat_count = 0;
  int scored_count = 0;
};

class RepeatTargetSelector {
public:
  explicit RepeatTargetSelector(int repeat_guard_ticks = 3);

  RepeatTargetSelection select(const std::vector<RepeatTargetCandidate>& candidates, int tick);
  void record_dispatch(int target_id, int tick);

private:
  bool is_repeat_blocked(int target_id, int tick) const;
  int score_candidate(const RepeatTargetCandidate& candidate) const;

  int repeat_guard_ticks_;
  int last_target_id_;
  int last_target_tick_;
};

} // namespace xptool::core
