#pragma once

#include "xptool/core/repeat_target_selector.hpp"
#include "xptool/core/woodcutting_target.hpp"

#include <optional>
#include <string>
#include <vector>

namespace xptool::core {

struct StateFrame {
  std::string schema_version = "1.0";
  std::optional<std::string> activity_key;

  std::optional<int> tick;
  std::optional<bool> logged_in;
  std::optional<bool> in_focus;
  std::optional<bool> bank_open;
  std::optional<bool> has_target_candidate;

  std::optional<int> nearest_target_id;
  std::optional<int> nearest_target_distance;
  std::optional<bool> nearest_target_interactable;

  std::optional<int> legacy_nearest_tree_id;
  std::optional<int> legacy_nearest_tree_distance;
  std::optional<bool> legacy_nearest_tree_interactable;

  std::vector<WoodcuttingTargetCandidate> woodcutting_candidates;
  std::vector<RepeatTargetCandidate> mining_candidates;
  std::vector<RepeatTargetCandidate> fishing_candidates;
  std::vector<RepeatTargetCandidate> combat_candidates;
  std::vector<RepeatTargetCandidate> banking_candidates;
};

} // namespace xptool::core
