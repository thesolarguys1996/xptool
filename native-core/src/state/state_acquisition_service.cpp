#include "xptool/core/state_acquisition_service.hpp"

namespace xptool::core {
StateAcquisitionService::StateAcquisitionService(int max_deferred_failures)
    : max_deferred_failures_(max_deferred_failures <= 0 ? 1 : max_deferred_failures),
      last_tick_(-1),
      has_last_good_(false),
      last_good_(),
      consecutive_failures_(0) {}

StateAcquisitionResult StateAcquisitionService::acquire(const StateFrame& frame) {
  if (!is_supported_schema(frame.schema_version)) {
    return reject("schema_version_unsupported");
  }
  if (!frame.tick.has_value()) {
    return reject("missing_tick");
  }

  const int frame_tick = frame.tick.value();
  if (last_tick_ >= 0 && frame_tick <= last_tick_) {
    return reject("sequence_regression");
  }
  if (!frame.logged_in.has_value()) {
    return reject("missing_logged_in");
  }

  StateSnapshot snapshot;
  snapshot.tick = frame_tick;
  if (frame.activity_key.has_value() && !frame.activity_key->empty()) {
    snapshot.activity_key = frame.activity_key.value();
  }
  snapshot.logged_in = frame.logged_in.value();
  snapshot.in_focus = frame.in_focus.has_value() ? frame.in_focus.value() : true;
  snapshot.bank_open = frame.bank_open.has_value() ? frame.bank_open.value() : false;

  if (frame.nearest_target_id.has_value()) {
    snapshot.nearest_target_id = frame.nearest_target_id.value();
  } else if (frame.legacy_nearest_tree_id.has_value()) {
    snapshot.nearest_target_id = frame.legacy_nearest_tree_id.value();
  }

  if (frame.nearest_target_distance.has_value()) {
    snapshot.nearest_target_distance = frame.nearest_target_distance.value();
  } else if (frame.legacy_nearest_tree_distance.has_value()) {
    snapshot.nearest_target_distance = frame.legacy_nearest_tree_distance.value();
  }

  if (frame.nearest_target_interactable.has_value()) {
    snapshot.nearest_target_interactable = frame.nearest_target_interactable.value();
  } else if (frame.legacy_nearest_tree_interactable.has_value()) {
    snapshot.nearest_target_interactable = frame.legacy_nearest_tree_interactable.value();
  }

  snapshot.woodcutting_candidates = frame.woodcutting_candidates;
  snapshot.mining_candidates = frame.mining_candidates;
  snapshot.fishing_candidates = frame.fishing_candidates;
  snapshot.combat_candidates = frame.combat_candidates;
  snapshot.banking_candidates = frame.banking_candidates;
  if (frame.has_target_candidate.has_value()) {
    snapshot.has_target_candidate = frame.has_target_candidate.value();
  } else {
    snapshot.has_target_candidate = !snapshot.woodcutting_candidates.empty() ||
                                    (snapshot.nearest_target_id > 0 && snapshot.nearest_target_interactable);
  }

  std::string reason_code = "state_acquired";
  if (frame.schema_version == "1.0" && frame.legacy_nearest_tree_id.has_value()) {
    reason_code = "state_acquired_legacy_alias";
  }

  return accept_snapshot(snapshot, reason_code, false);
}

bool StateAcquisitionService::is_supported_schema(const std::string& schema_version) const {
  return schema_version == "1.0" || schema_version == "1.1";
}

StateAcquisitionResult StateAcquisitionService::reject(const std::string& reason_code) {
  ++consecutive_failures_;
  if (has_last_good_ && consecutive_failures_ <= max_deferred_failures_) {
    StateSnapshot deferred_snapshot = last_good_;
    deferred_snapshot.tick = last_tick_ + 1;
    return accept_snapshot(deferred_snapshot, "deferred_" + reason_code, true);
  }

  StateAcquisitionResult result;
  result.accepted = false;
  result.used_last_good = false;
  result.reason_code = reason_code;
  result.consecutive_failures = consecutive_failures_;
  return result;
}

StateAcquisitionResult StateAcquisitionService::accept_snapshot(
    const StateSnapshot& snapshot,
    const std::string& reason_code,
    bool used_last_good) {
  StateAcquisitionResult result;
  result.accepted = true;
  result.used_last_good = used_last_good;
  result.reason_code = reason_code;
  result.snapshot = snapshot;
  result.consecutive_failures = consecutive_failures_;

  last_tick_ = snapshot.tick;
  if (!used_last_good) {
    has_last_good_ = true;
    last_good_ = snapshot;
    consecutive_failures_ = 0;
    result.consecutive_failures = 0;
  }
  return result;
}

} // namespace xptool::core
