# Native Client Phase 18 Motor Dispatch Admission Plan

Last updated: 2026-04-05

## Goal
Continue reducing `CommandExecutor` runtime ownership by extracting motor dispatch admission and cooldown gating ownership (owner admission, cooldown readiness, mutation-budget gate ownership, and gesture admission scheduling) into a focused motor dispatch service.

## Execution Slices
1. `18.1` Define Phase 18 scope, artifacts, and completion gates.
2. `18.2` Extract motor dispatch admission/cooldown ownership from `CommandExecutor` and delegate runtime wiring.
3. `18.3` Run Phase 18 verification + guard pack and record `PHASE 18 COMPLETE`.

## Phase 18 Slice Status
- `18.1` complete.
- `18.2` complete.
- `18.3` complete.

## Phase 18.1 Outputs
- Added dedicated Phase 18 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE18_MOTOR_DISPATCH_ADMISSION_PLAN.md`
- Updated migration/task/status artifacts with Phase 18 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 18.2 Outputs
- Extracted motor dispatch admission/cooldown ownership from `CommandExecutor` into dedicated runtime service:
  - `runelite-plugin/src/main/java/com/xptool/executor/MotorDispatchAdmissionService.java`
- Updated `CommandExecutor` motor admission/gating wiring to delegate through service ownership:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Delegated gesture admission scheduling, owner acquisition checks, cooldown state, action serial tracking,
  and mutation-budget reset/consume operations through service boundary:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/MotorDispatchAdmissionService.java`
- Added focused regression tests:
  - `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`

## Phase 18.3 Outputs
- Added explicit Phase 18 verification script:
  - `scripts/verify_phase18_motor_dispatch_admission.py`
- Executed Phase 18 verification + guard pack:
  - `python scripts/verify_phase18_motor_dispatch_admission.py`
  - `python scripts/verify_java_runtime_ownership_blocked.py`
  - `python scripts/verify_java_shim_retirement_gates.py`
  - `python scripts/verify_native_only_operations_hardening.py`
  - `python scripts/verify_native_cutover.py`
  - `python scripts/verify_native_soak_report.py --min-iterations 6 --max-failures 0 --max-age-hours 48`
- Verified Java executor tests for extraction wave:
  - `.\gradlew.bat test --tests com.xptool.executor.MotorDispatchAdmissionServiceTest --tests com.xptool.executor.MotorProgramTerminalServiceTest --tests com.xptool.executor.PendingMoveTelemetryServiceTest`
- Recorded completion markers:
  - `PHASE 18 STARTED`
  - `PHASE 18 COMPLETE`

## Exit Criteria
- Motor dispatch admission/cooldown gating ownership no longer lives directly in `CommandExecutor`.
- `CommandExecutor` delegates admission/cooldown checks and gesture scheduling through `MotorDispatchAdmissionService`.
- Focused motor admission and adjacent regression tests pass.
- Phase 18 verification script and native guard pack both pass.
- `PHASE 18 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
