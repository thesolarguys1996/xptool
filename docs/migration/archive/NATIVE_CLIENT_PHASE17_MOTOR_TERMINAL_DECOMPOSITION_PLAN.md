# Native Client Phase 17 Motor Terminal Decomposition Plan

Last updated: 2026-04-05

## Goal
Continue reducing `CommandExecutor` runtime ownership by extracting motor terminal lifecycle ownership (menu validation, complete/cancel/fail terminal handling, idle-owner release hooks) into a focused motor terminal service.

## Execution Slices
1. `17.1` Define Phase 17 scope, artifacts, and completion gates.
2. `17.2` Extract motor terminal lifecycle ownership from `CommandExecutor` and delegate runtime wiring.
3. `17.3` Run Phase 17 verification + guard pack and record `PHASE 17 COMPLETE`.

## Phase 17 Slice Status
- `17.1` complete.
- `17.2` complete.
- `17.3` complete.

## Phase 17.1 Outputs
- Added dedicated Phase 17 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE17_MOTOR_TERMINAL_DECOMPOSITION_PLAN.md`
- Updated migration/task/status artifacts with Phase 17 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 17.2 Outputs
- Extracted motor terminal lifecycle ownership from `CommandExecutor` into dedicated runtime service:
  - `runelite-plugin/src/main/java/com/xptool/executor/MotorProgramTerminalService.java`
- Updated motor lifecycle wiring in `CommandExecutor` to delegate terminal operations through service ownership:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Delegated idle-owner release hooks (`runtime teardown`, `suppression`, terminal idle program completion) through service boundary:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/MotorProgramTerminalService.java`
- Added focused regression tests:
  - `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- Fixed pending regression test callback in telemetry extraction tests:
  - `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 17.3 Outputs
- Added explicit Phase 17 verification script:
  - `scripts/verify_phase17_motor_terminal_decomposition.py`
- Executed Phase 17 verification + guard pack:
  - `python scripts/verify_phase17_motor_terminal_decomposition.py`
  - `python scripts/verify_java_runtime_ownership_blocked.py`
  - `python scripts/verify_java_shim_retirement_gates.py`
  - `python scripts/verify_native_only_operations_hardening.py`
  - `python scripts/verify_native_cutover.py`
  - `python scripts/verify_native_soak_report.py --min-iterations 6 --max-failures 0 --max-age-hours 48`
- Verified Java executor tests for extraction wave:
  - `.\gradlew.bat test --tests com.xptool.executor.MotorProgramTerminalServiceTest --tests com.xptool.executor.PendingMoveTelemetryServiceTest`
- Recorded completion markers:
  - `PHASE 17 STARTED`
  - `PHASE 17 COMPLETE`

## Exit Criteria
- Motor terminal lifecycle ownership no longer lives directly in `CommandExecutor`.
- `CommandExecutor` delegates motor terminal validation/complete/cancel/fail and idle-owner release hooks through `MotorProgramTerminalService`.
- Focused motor terminal and pending telemetry regression tests pass.
- Phase 17 verification script and native guard pack both pass.
- `PHASE 17 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
