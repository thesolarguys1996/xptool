#pragma once

#include "xptool/core/repeat_target_selector.hpp"
#include "xptool/core/woodcutting_target.hpp"

#include <string>
#include <vector>

namespace xptool::core {

class StateSnapshot {
public:
  int tick = -1;
  std::string activity_key = "woodcutting";
  bool logged_in = false;
  bool in_focus = true;
  bool bank_open = false;
  bool has_target_candidate = false;
  int nearest_target_id = -1;
  int nearest_target_distance = 0;
  bool nearest_target_interactable = false;
  std::vector<WoodcuttingTargetCandidate> woodcutting_candidates;
  std::vector<RepeatTargetCandidate> mining_candidates;
  std::vector<RepeatTargetCandidate> fishing_candidates;
  std::vector<RepeatTargetCandidate> combat_candidates;
  std::vector<RepeatTargetCandidate> banking_candidates;

  [[nodiscard]] bool is_valid() const;
};

} // namespace xptool::core
