#pragma once

#include "xptool/core/runtime_intent.hpp"

#include <optional>

namespace xptool::core {

class MotorRuntime {
public:
  void enqueue(const RuntimeIntent& intent);
  [[nodiscard]] bool has_pending() const;
  [[nodiscard]] std::optional<RuntimeIntent> execute_next();

private:
  std::optional<RuntimeIntent> pending_intent_;
  std::optional<RuntimeIntent> last_executed_intent_;
};

} // namespace xptool::core
