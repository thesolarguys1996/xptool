# Native Client Phase 16 Motor Pending-Telemetry Decomposition Plan

Last updated: 2026-04-05

## Goal
Continue reducing `CommandExecutor` runtime ownership by extracting pending-move telemetry aggregation/event emission into a focused motor telemetry component.

## Execution Slices
1. `16.1` Define Phase 16 scope, artifacts, and completion gates.
2. `16.2` Extract pending-move telemetry ownership from `CommandExecutor` and delegate runtime wiring.
3. `16.3` Run Phase 16 verification + guard pack and record `PHASE 16 COMPLETE`.

## Phase 16 Slice Status
- `16.1` complete.
- `16.2` complete.
- `16.3` complete.

## Phase 16.1 Outputs
- Added dedicated Phase 16 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE16_MOTOR_PENDING_TELEMETRY_PLAN.md`
- Updated migration/task/status artifacts with Phase 16 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 16.2 Outputs
- Extracted pending-move telemetry ownership from `CommandExecutor` into dedicated runtime service:
  - `runelite-plugin/src/main/java/com/xptool/executor/PendingMoveTelemetryService.java`
- Updated motor runtime input wiring in `CommandExecutor` to delegate pending-move telemetry callbacks:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Updated executor debug-counter reporting/reset to consume telemetry service snapshot/reset contract:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Removed obsolete/local pending-move telemetry helpers from `CommandExecutor`:
  - `notePendingMove*` methods
  - `emitOffscreenPendingMoveEvent(...)`
  - `pointsEqual(...)`
- Cleared reported diagnostics:
  - removed unused `java.util.LinkedHashSet` import
  - removed unused `setDropSweepNextSlot(int)` helper
- Added focused regression tests:
  - `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 16.3 Outputs
- Added explicit Phase 16 verification script:
  - `scripts/verify_phase16_motor_pending_telemetry.py`
- Executed Phase 16 verification + guard pack:
  - `python scripts/verify_phase16_motor_pending_telemetry.py`
  - `python scripts/verify_java_runtime_ownership_blocked.py`
  - `python scripts/verify_java_shim_retirement_gates.py`
  - `python scripts/verify_native_only_operations_hardening.py`
  - `python scripts/verify_native_cutover.py`
  - `python scripts/verify_native_soak_report.py --min-iterations 6 --max-failures 0 --max-age-hours 48`
- Verified Java executor compilation/tests for extraction wave:
  - `.\gradlew.bat test --tests com.xptool.executor.PendingMoveTelemetryServiceTest`
- Recorded completion markers:
  - `PHASE 16 STARTED`
  - `PHASE 16 COMPLETE`

## Exit Criteria
- Pending-move telemetry aggregation/event emission no longer lives in `CommandExecutor`.
- `CommandExecutor` motor input wiring delegates pending-move telemetry through dedicated service.
- Reported diagnostics are resolved and do not reappear in compile/tests.
- Phase 16 verification script and native guard pack both pass.
- `PHASE 16 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
