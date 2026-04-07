#pragma once

#include "xptool/core/state_frame.hpp"
#include "xptool/core/state_snapshot.hpp"

#include <string>

namespace xptool::core {

struct StateAcquisitionResult {
  bool accepted = false;
  bool used_last_good = false;
  std::string reason_code;
  StateSnapshot snapshot;
  int consecutive_failures = 0;
};

class StateAcquisitionService {
public:
  explicit StateAcquisitionService(int max_deferred_failures = 3);

  StateAcquisitionResult acquire(const StateFrame& frame);

private:
  bool is_supported_schema(const std::string& schema_version) const;
  StateAcquisitionResult reject(const std::string& reason_code);
  StateAcquisitionResult accept_snapshot(const StateSnapshot& snapshot, const std::string& reason_code, bool used_last_good);

  int max_deferred_failures_;
  int last_tick_;
  bool has_last_good_;
  StateSnapshot last_good_;
  int consecutive_failures_;
};

} // namespace xptool::core
