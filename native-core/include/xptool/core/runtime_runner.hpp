#pragma once

#include "xptool/core/banking_runtime.hpp"
#include "xptool/core/combat_runtime.hpp"
#include "xptool/core/fishing_runtime.hpp"
#include "xptool/core/mining_runtime.hpp"
#include "xptool/core/runtime_coordinator.hpp"
#include "xptool/core/runtime_gate.hpp"
#include "xptool/core/motor_runtime.hpp"
#include "xptool/core/state_acquisition_service.hpp"
#include "xptool/core/state_frame.hpp"
#include "xptool/core/state_snapshot.hpp"
#include "xptool/core/runtime_telemetry.hpp"
#include "xptool/core/woodcutting_runtime.hpp"

namespace xptool::core {

class RuntimeRunner {
public:
  RuntimeRunner();
  RuntimeTickResult tick(const StateSnapshot& snapshot);
  RuntimeTickResult tick_frame(const StateFrame& frame);
  [[nodiscard]] const RuntimeTelemetry& telemetry() const;

private:
  WoodcuttingRuntime woodcutting_runtime_;
  MiningRuntime mining_runtime_;
  FishingRuntime fishing_runtime_;
  CombatRuntime combat_runtime_;
  BankingRuntime banking_runtime_;
  RuntimeGate gate_;
  MotorRuntime motor_runtime_;
  StateAcquisitionService state_acquisition_;
  RuntimeCoordinator coordinator_;
  RuntimeTelemetry telemetry_;
};

} // namespace xptool::core
