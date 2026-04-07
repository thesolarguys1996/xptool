#include "xptool/core/state_snapshot_translator.hpp"

#include "xptool/core/state_snapshot.hpp"

namespace xptool::core {

void StateSnapshotTranslator::ensure_woodcutting_candidates(StateSnapshot* snapshot) const {
  if (snapshot == nullptr) {
    return;
  }

  const bool can_fallback = snapshot->has_target_candidate && snapshot->nearest_target_id > 0 && snapshot->nearest_target_interactable;
  const std::string activity = snapshot->activity_key;

  if (activity == "woodcutting") {
    if (snapshot->woodcutting_candidates.empty() && can_fallback) {
      WoodcuttingTargetCandidate fallback;
      fallback.target_id = snapshot->nearest_target_id;
      fallback.distance = snapshot->nearest_target_distance;
      fallback.interactable = snapshot->nearest_target_interactable;
      snapshot->woodcutting_candidates.push_back(fallback);
    }
    snapshot->has_target_candidate = !snapshot->woodcutting_candidates.empty();
    return;
  }

  RepeatTargetCandidate fallback;
  fallback.target_id = snapshot->nearest_target_id;
  fallback.distance = snapshot->nearest_target_distance;
  fallback.interactable = snapshot->nearest_target_interactable;

  if (activity == "mining") {
    if (snapshot->mining_candidates.empty() && can_fallback) {
      snapshot->mining_candidates.push_back(fallback);
    }
    snapshot->has_target_candidate = !snapshot->mining_candidates.empty();
    return;
  }

  if (activity == "fishing") {
    if (snapshot->fishing_candidates.empty() && can_fallback) {
      snapshot->fishing_candidates.push_back(fallback);
    }
    snapshot->has_target_candidate = !snapshot->fishing_candidates.empty();
    return;
  }

  if (activity == "combat") {
    if (snapshot->combat_candidates.empty() && can_fallback) {
      snapshot->combat_candidates.push_back(fallback);
    }
    snapshot->has_target_candidate = !snapshot->combat_candidates.empty();
    return;
  }

  if (activity == "banking") {
    if (!snapshot->bank_open && snapshot->banking_candidates.empty() && can_fallback) {
      snapshot->banking_candidates.push_back(fallback);
    }
    snapshot->has_target_candidate = !snapshot->bank_open && !snapshot->banking_candidates.empty();
    return;
  }

  snapshot->has_target_candidate = false;
}

} // namespace xptool::core
