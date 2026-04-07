# Native Client Phase 21 Interaction Anchor Resolution Decomposition Plan

Last updated: 2026-04-05

## Goal
Continue reducing `CommandExecutor` runtime ownership by extracting interaction anchor resolution ownership for tile-object clickbox/fallback anchor conversion into a focused runtime service.

## Execution Slices
1. `21.1` Define Phase 21 scope, artifacts, and completion gates.
2. `21.2` Extract interaction anchor resolution ownership from `CommandExecutor` and delegate runtime wiring.
3. `21.3` Run Phase 21 verification + guard pack and record `PHASE 21 COMPLETE`.

## Phase 21 Slice Status
- `21.1` complete.
- `21.2` complete.
- `21.3` complete.

## Phase 21.1 Outputs
- Added dedicated Phase 21 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE21_INTERACTION_ANCHOR_RESOLUTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 21 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 21.2 Outputs
- Extracted interaction anchor resolution ownership from `CommandExecutor` into dedicated runtime service:
  - `runelite-plugin/src/main/java/com/xptool/executor/InteractionAnchorResolverService.java`
- Updated `CommandExecutor` interaction anchor resolution routing to delegate through service ownership:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Delegated tile-object clickbox extraction/fallback conversion and anchor-center/bounds resolution through service boundary:
  - `runelite-plugin/src/main/java/com/xptool/executor/InteractionAnchorResolverService.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Added focused regression tests:
  - `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`

## Phase 21.3 Outputs
- Added explicit Phase 21 verification script:
  - `scripts/verify_phase21_interaction_anchor_resolution.py`
- Executed Phase 21 verification + guard pack:
  - `python scripts/verify_phase21_interaction_anchor_resolution.py`
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
  - `.\gradlew.bat test --tests com.xptool.executor.InteractionAnchorResolverServiceTest --tests com.xptool.executor.InteractionClickTelemetryServiceTest --tests com.xptool.executor.MotorDispatchContextServiceTest --tests com.xptool.executor.MotorDispatchAdmissionServiceTest --tests com.xptool.executor.MotorProgramTerminalServiceTest --tests com.xptool.executor.PendingMoveTelemetryServiceTest`
- Recorded completion markers:
  - `PHASE 21 STARTED`
  - `PHASE 21 COMPLETE`

## Exit Criteria
- Tile-object interaction anchor resolution ownership no longer lives directly in `CommandExecutor`.
- `CommandExecutor` delegates tile-object anchor resolution through `InteractionAnchorResolverService`.
- Focused interaction anchor resolution and adjacent regression tests pass.
- Phase 21 verification script and native guard pack both pass.
- `PHASE 21 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
