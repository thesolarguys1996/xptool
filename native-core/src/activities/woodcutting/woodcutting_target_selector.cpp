#include "xptool/core/woodcutting_target_selector.hpp"

#include <algorithm>

namespace xptool::core {

WoodcuttingTargetSelector::WoodcuttingTargetSelector(int repeat_guard_ticks)
    : repeat_guard_ticks_(repeat_guard_ticks <= 0 ? 1 : repeat_guard_ticks), last_target_id_(-1), last_target_tick_(-1) {}

WoodcuttingTargetSelection WoodcuttingTargetSelector::select(const std::vector<WoodcuttingTargetCandidate>& candidates, int tick) {
  WoodcuttingTargetSelection result;
  WoodcuttingTargetCandidate best_candidate;
  bool has_best = false;
  WoodcuttingTargetCandidate best_repeat_blocked_candidate;
  bool has_repeat_blocked_best = false;

  for (const auto& candidate : candidates) {
    if (!candidate.interactable || candidate.target_id <= 0) {
      continue;
    }
    ++result.scored_count;
    const bool repeat_blocked = is_repeat_blocked(candidate.target_id, tick);
    if (repeat_blocked) {
      ++result.blocked_repeat_count;
      if (!has_repeat_blocked_best || score_candidate(candidate) > score_candidate(best_repeat_blocked_candidate)) {
        best_repeat_blocked_candidate = candidate;
        has_repeat_blocked_best = true;
      }
      continue;
    }
    if (!has_best || score_candidate(candidate) > score_candidate(best_candidate)) {
      best_candidate = candidate;
      has_best = true;
    }
  }

  if (has_best) {
    result.selected = true;
    result.selected_target_id = best_candidate.target_id;
    result.rerouted = has_repeat_blocked_best;
    result.reason_code = has_repeat_blocked_best ? "repeat_rerouted" : "target_selected";
    return result;
  }

  result.selected = false;
  result.selected_target_id = -1;
  if (has_repeat_blocked_best) {
    result.reason_code = "repeat_blocked_no_alternate";
    return result;
  }

  result.reason_code = "no_target_candidate";
  return result;
}

bool WoodcuttingTargetSelector::is_repeat_blocked(int target_id, int tick) const {
  if (target_id <= 0 || last_target_id_ <= 0 || last_target_tick_ < 0) {
    return false;
  }
  if (target_id != last_target_id_) {
    return false;
  }
  return (tick - last_target_tick_) < repeat_guard_ticks_;
}

int WoodcuttingTargetSelector::score_candidate(const WoodcuttingTargetCandidate& candidate) const {
  return 10000 - std::max(0, candidate.distance) * 10;
}

void WoodcuttingTargetSelector::record_dispatch(int target_id, int tick) {
  if (target_id <= 0 || tick < 0) {
    return;
  }
  last_target_id_ = target_id;
  last_target_tick_ = tick;
}

} // namespace xptool::core
