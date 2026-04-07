#pragma once

#include "xptool/core/runtime_intent.hpp"

#include <string>

namespace xptool::core {

struct ActivityStepResult {
  RuntimeIntent intent;
  std::string activity_key;
  std::string selection_reason;
  int selected_target_id = -1;
  int candidate_count = 0;
  int scored_count = 0;
  int blocked_repeat_count = 0;
};

} // namespace xptool::core
