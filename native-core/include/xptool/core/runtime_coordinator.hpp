#pragma once

#include "xptool/core/state_snapshot_translator.hpp"

#include <string>

namespace xptool::core {

class BankingRuntime;
class CombatRuntime;
class FishingRuntime;
class MotorRuntime;
class MiningRuntime;
class RuntimeGate;
class RuntimeTelemetry;
class StateSnapshot;
class WoodcuttingRuntime;

struct RuntimeTickResult {
  bool dispatched = false;
  std::string reason_code;
  std::string command_type;
  int target_id = -1;
};

class RuntimeCoordinator {
public:
  RuntimeCoordinator(
      WoodcuttingRuntime* woodcutting_runtime,
      MiningRuntime* mining_runtime,
      FishingRuntime* fishing_runtime,
      CombatRuntime* combat_runtime,
      BankingRuntime* banking_runtime,
      RuntimeGate* gate,
      MotorRuntime* motor_runtime);
  RuntimeTickResult on_tick(const StateSnapshot& snapshot, RuntimeTelemetry* telemetry);

private:
  WoodcuttingRuntime* woodcutting_runtime_;
  MiningRuntime* mining_runtime_;
  FishingRuntime* fishing_runtime_;
  CombatRuntime* combat_runtime_;
  BankingRuntime* banking_runtime_;
  RuntimeGate* gate_;
  MotorRuntime* motor_runtime_;
  StateSnapshotTranslator state_translator_;
};

} // namespace xptool::core
