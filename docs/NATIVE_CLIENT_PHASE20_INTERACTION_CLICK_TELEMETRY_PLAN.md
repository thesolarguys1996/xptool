# Native Client Phase 20 Interaction Click Telemetry Decomposition Plan

Last updated: 2026-04-05

## Goal
Continue reducing `CommandExecutor` runtime ownership by extracting interaction-click telemetry/state ownership (click serial/timestamps, anchor state, telemetry payload assembly, and settle-event packaging callbacks) into a focused runtime service.

## Execution Slices
1. `20.1` Define Phase 20 scope, artifacts, and completion gates.
2. `20.2` Extract interaction-click telemetry/state ownership from `CommandExecutor` and delegate runtime wiring.
3. `20.3` Run Phase 20 verification + guard pack and record `PHASE 20 COMPLETE`.

## Phase 20 Slice Status
- `20.1` complete.
- `20.2` complete.
- `20.3` complete.

## Phase 20.1 Outputs
- Added dedicated Phase 20 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE20_INTERACTION_CLICK_TELEMETRY_PLAN.md`
- Updated migration/task/status artifacts with Phase 20 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 20.2 Outputs
- Extracted interaction-click telemetry/state ownership from `CommandExecutor` into dedicated runtime service:
  - `runelite-plugin/src/main/java/com/xptool/executor/InteractionClickTelemetryService.java`
- Updated `CommandExecutor` interaction-click telemetry/anchor/state routing to delegate through service ownership:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Delegated click serial/freshness tracking, telemetry payload assembly, and settle-eligible event callback argument packaging through service boundary:
  - `runelite-plugin/src/main/java/com/xptool/executor/InteractionClickTelemetryService.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Added focused regression tests:
  - `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`

## Phase 20.3 Outputs
- Added explicit Phase 20 verification script:
  - `scripts/verify_phase20_interaction_click_telemetry.py`
- Executed Phase 20 verification + guard pack:
  - `python scripts/verify_phase20_interaction_click_telemetry.py`
  - `python scripts/verify_phase19_motor_dispatch_context.py`
  - `python scripts/verify_phase18_motor_dispatch_admission.py`
  - `python scripts/verify_phase17_motor_terminal_decomposition.py`
  - `python scripts/verify_phase16_motor_pending_telemetry.py`
  - `python scripts/verify_java_runtime_ownership_blocked.py`
  - `python scripts/verify_java_shim_retirement_gates.py`
  - `python scripts/verify_native_only_operations_hardening.py`
  - `python scripts/verify_native_cutover.py`
  - `python scripts/verify_native_soak_report.py --min-iterations 6 --max-failures 0 --max-age-hours 48`
- Verified Java executor tests for extraction wave:
  - `.\gradlew.bat test --tests com.xptool.executor.InteractionClickTelemetryServiceTest --tests com.xptool.executor.MotorDispatchContextServiceTest --tests com.xptool.executor.MotorDispatchAdmissionServiceTest --tests com.xptool.executor.MotorProgramTerminalServiceTest --tests com.xptool.executor.PendingMoveTelemetryServiceTest`
- Recorded completion markers:
  - `PHASE 20 STARTED`
  - `PHASE 20 COMPLETE`

## Exit Criteria
- Interaction-click telemetry/state ownership no longer lives directly in `CommandExecutor`.
- `CommandExecutor` delegates click telemetry/state operations through `InteractionClickTelemetryService`.
- Focused interaction-click telemetry and adjacent regression tests pass.
- Phase 20 verification script and native guard pack both pass.
- `PHASE 20 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
