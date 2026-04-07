#pragma once

#include <string>

namespace xptool::core {

enum class IntentType {
  kNone = 0,
  kInteractTarget = 1,
  kPause = 2,
  kAbort = 3,
};

struct RuntimeIntent {
  IntentType type = IntentType::kNone;
  std::string command_type;
  std::string reason_code;
  int target_id = -1;

  [[nodiscard]] bool has_value() const { return type != IntentType::kNone; }
};

} // namespace xptool::core
