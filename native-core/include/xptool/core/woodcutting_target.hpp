#pragma once

#include <string>

namespace xptool::core {

struct WoodcuttingTargetCandidate {
  int target_id = -1;
  int distance = 0;
  bool interactable = false;
};

struct WoodcuttingTargetScore {
  int target_id = -1;
  int distance = 0;
  bool interactable = false;
  bool repeat_blocked = false;
  int score = 0;
};

struct WoodcuttingTargetSelection {
  bool selected = false;
  int selected_target_id = -1;
  bool rerouted = false;
  std::string reason_code;
  int blocked_repeat_count = 0;
  int scored_count = 0;
};

} // namespace xptool::core
