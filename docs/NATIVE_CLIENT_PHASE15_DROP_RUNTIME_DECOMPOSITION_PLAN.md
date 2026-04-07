# Native Client Phase 15 Drop Runtime Decomposition Plan

Last updated: 2026-04-05

## Goal
Continue reducing `CommandExecutor` behavior ownership by extracting drop-sweep session state and drop-target inventory policy into dedicated runtime services.

## Execution Slices
1. `15.1` Define Phase 15 scope, artifacts, and completion gates.
2. `15.2` Extract drop-sweep state + inventory target policy ownership from `CommandExecutor`.
3. `15.3` Run Phase 15 verification + guard pack and record `PHASE 15 COMPLETE`.

## Phase 15 Slice Status
- `15.1` complete.
- `15.2` complete.
- `15.3` complete.

## Phase 15.1 Outputs
- Added dedicated Phase 15 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE15_DROP_RUNTIME_DECOMPOSITION_PLAN.md`
- Updated migration/task/status artifacts with Phase 15 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 15.2 Outputs
- Extracted drop-sweep session state ownership from `CommandExecutor`:
  - `runelite-plugin/src/main/java/com/xptool/executor/DropSweepSessionService.java`
- Extracted drop-target inventory policy and target matching from `CommandExecutor`:
  - `runelite-plugin/src/main/java/com/xptool/executor/DropSweepInventoryService.java`
- Updated `CommandExecutor` drop runtime wiring and helper methods to delegate through extracted services:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Added focused regression tests for extracted drop services:
  - `runelite-plugin/src/test/java/com/xptool/executor/DropSweepSessionServiceTest.java`
  - `runelite-plugin/src/test/java/com/xptool/executor/DropSweepInventoryServiceTest.java`

## Phase 15.3 Outputs
- Added explicit Phase 15 verification script:
  - `scripts/verify_phase15_drop_runtime_decomposition.py`
- Executed Phase 15 verification + guard pack:
  - `python scripts/verify_phase15_drop_runtime_decomposition.py`
  - `python scripts/verify_java_runtime_ownership_blocked.py`
  - `python scripts/verify_java_shim_retirement_gates.py`
  - `python scripts/verify_native_only_operations_hardening.py`
  - `python scripts/verify_native_cutover.py`
  - `python scripts/verify_native_soak_report.py --min-iterations 6 --max-failures 0 --max-age-hours 48`
- Verified Java executor compilation/tests for extraction wave:
  - `.\gradlew.bat test --tests com.xptool.executor.DropSweepSessionServiceTest --tests com.xptool.executor.DropSweepInventoryServiceTest`
- Recorded completion markers:
  - `PHASE 15 STARTED`
  - `PHASE 15 COMPLETE`

## Exit Criteria
- Drop-sweep session mutable state is owned by a dedicated runtime service.
- Drop-target inventory selection/matching policy is owned by a dedicated runtime service.
- `CommandExecutor` keeps orchestration/wiring ownership only for drop runtime flow.
- Phase 15 verification script and native guard pack both pass.
- `PHASE 15 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
