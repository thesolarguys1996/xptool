#include "xptool/core/runtime_runner.hpp"

namespace xptool::core {

RuntimeRunner::RuntimeRunner()
    : woodcutting_runtime_(),
      mining_runtime_(),
      fishing_runtime_(),
      combat_runtime_(),
      banking_runtime_(),
      gate_(2),
      motor_runtime_(),
      state_acquisition_(3),
      coordinator_(
          &woodcutting_runtime_,
          &mining_runtime_,
          &fishing_runtime_,
          &combat_runtime_,
          &banking_runtime_,
          &gate_,
          &motor_runtime_),
      telemetry_() {}

RuntimeTickResult RuntimeRunner::tick(const StateSnapshot& snapshot) { return coordinator_.on_tick(snapshot, &telemetry_); }

RuntimeTickResult RuntimeRunner::tick_frame(const StateFrame& frame) {
  const StateAcquisitionResult acquired = state_acquisition_.acquire(frame);

  RuntimeTelemetryEvent acquisition_event;
  acquisition_event.event_type = "state_acquisition";
  acquisition_event.reason_code = acquired.reason_code;
  acquisition_event.data = {
      {"accepted", acquired.accepted ? "true" : "false"},
      {"usedLastGood", acquired.used_last_good ? "true" : "false"},
      {"tick", std::to_string(acquired.snapshot.tick)},
      {"consecutiveFailures", std::to_string(acquired.consecutive_failures)},
  };
  telemetry_.record(acquisition_event);

  if (!acquired.accepted) {
    RuntimeTickResult result;
    result.dispatched = false;
    result.reason_code = acquired.reason_code;
    return result;
  }

  return coordinator_.on_tick(acquired.snapshot, &telemetry_);
}

const RuntimeTelemetry& RuntimeRunner::telemetry() const { return telemetry_; }

} // namespace xptool::core
