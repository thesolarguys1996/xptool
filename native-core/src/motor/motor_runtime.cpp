#include "xptool/core/motor_runtime.hpp"

namespace xptool::core {

void MotorRuntime::enqueue(const RuntimeIntent& intent) { pending_intent_ = intent; }

bool MotorRuntime::has_pending() const { return pending_intent_.has_value(); }

std::optional<RuntimeIntent> MotorRuntime::execute_next() {
  if (!pending_intent_.has_value()) {
    return std::nullopt;
  }
  last_executed_intent_ = pending_intent_;
  pending_intent_.reset();
  return last_executed_intent_;
}

} // namespace xptool::core
