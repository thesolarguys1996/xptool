# Native Client Phase 14 Executor Utility Decomposition Plan

Last updated: 2026-04-05

## Goal
Continue reducing `CommandExecutor` ownership by moving utility/parsing helpers to shared executor utility components and keeping `CommandExecutor` orchestration-focused.

## Execution Slices
1. `14.1` Define Phase 14 scope, artifacts, and completion gates.
2. `14.2` Extract remaining utility helper ownership (`details`, `asInt`, dead helpers) out of `CommandExecutor`.
3. `14.3` Run Phase 14 verification + guard pack and record `PHASE 14 COMPLETE`.

## Phase 14 Slice Status
- `14.1` complete.
- `14.2` complete.
- `14.3` complete.

## Phase 14.1 Outputs
- Added dedicated Phase 14 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE14_EXECUTOR_UTILITY_DECOMPOSITION_PLAN.md`
- Updated migration/task/status artifacts with Phase 14 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 14.2 Outputs
- Removed dead helper ownership from `CommandExecutor`:
  - removed unused `elapsedTicksSince(...)`
- Extracted utility helper ownership from `CommandExecutor` into shared utility component:
  - replaced `CommandExecutor` local `details(...)` usage with `ExecutorValueParsers.details(...)`
  - replaced `CommandExecutor` local `asInt(...)` usage with `ExecutorValueParsers.asInt(...)`
  - removed `CommandExecutor` local `details(...)` and `asInt(...)` methods
- Updated runtime wiring call-sites to consume shared utility policy:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Added focused utility regression test:
  - `runelite-plugin/src/test/java/com/xptool/executor/ExecutorValueParsersTest.java`

## Phase 14.3 Outputs
- Added explicit Phase 14 verification script:
  - `scripts/verify_phase14_executor_utility_decomposition.py`
- Executed Phase 14 verification + guard pack:
  - `python scripts/verify_phase14_executor_utility_decomposition.py`
  - `python scripts/verify_java_runtime_ownership_blocked.py`
  - `python scripts/verify_java_shim_retirement_gates.py`
  - `python scripts/verify_native_only_operations_hardening.py`
  - `python scripts/verify_native_cutover.py`
  - `python scripts/verify_native_soak_report.py --min-iterations 6 --max-failures 0 --max-age-hours 48`
- Verified Java executor compilation/tests for utility extraction:
  - `.\gradlew.bat test --tests com.xptool.executor.ExecutorValueParsersTest`
- Recorded completion markers:
  - `PHASE 14 STARTED`
  - `PHASE 14 COMPLETE`

## Exit Criteria
- `CommandExecutor` no longer owns dead/local utility parser helpers that are provided by `ExecutorValueParsers`.
- Utility wiring in `CommandExecutor` delegates to shared utility component.
- Phase 14 verification script and native guard pack both pass.
- `PHASE 14 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
