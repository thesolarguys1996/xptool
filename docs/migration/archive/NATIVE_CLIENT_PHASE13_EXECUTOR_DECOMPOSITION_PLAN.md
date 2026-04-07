# Native Client Phase 13 Executor Decomposition Plan

Last updated: 2026-04-05

## Goal
Continue post-cutover Java compatibility reduction by extracting remaining command-ingest/runtime-gate behavior ownership from `CommandExecutor` into focused runtime services.

## Execution Slices
1. `13.1` Define Phase 13 scope, artifacts, and completion gates.
2. `13.2` Extract command-ingest identity/path policy + manual-metrics gate telemetry ownership from `CommandExecutor`.
3. `13.3` Run Phase 13 verification + guard pack and record `PHASE 13 COMPLETE`.

## Phase 13 Slice Status
- `13.1` complete.
- `13.2` complete.
- `13.3` complete.

## Phase 13.1 Outputs
- Added dedicated Phase 13 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE13_EXECUTOR_DECOMPOSITION_PLAN.md`
- Updated migration/task/status artifacts with Phase 13 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 13.2 Outputs
- Extracted command-id dedupe ownership from `CommandExecutor` into dedicated runtime component:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandIdDeduplicationService.java`
- Extracted command-file path policy ownership from `CommandExecutor` into dedicated runtime component:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandFilePathResolver.java`
- Extracted manual-metrics runtime gate telemetry ownership from `CommandExecutor` into dedicated runtime component:
  - `runelite-plugin/src/main/java/com/xptool/executor/ManualMetricsGateTelemetryService.java`
- Updated `CommandExecutor` wiring to delegate to extracted services:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Added focused regression tests for extracted services:
  - `runelite-plugin/src/test/java/com/xptool/executor/CommandIdDeduplicationServiceTest.java`
  - `runelite-plugin/src/test/java/com/xptool/executor/CommandFilePathResolverTest.java`
  - `runelite-plugin/src/test/java/com/xptool/executor/ManualMetricsGateTelemetryServiceTest.java`

## Phase 13.3 Outputs
- Added explicit Phase 13 verification script:
  - `scripts/verify_phase13_executor_decomposition.py`
- Executed Phase 13 verification + guard pack:
  - `python scripts/verify_phase13_executor_decomposition.py`
  - `python scripts/verify_java_runtime_ownership_blocked.py`
  - `python scripts/verify_java_shim_retirement_gates.py`
  - `python scripts/verify_native_only_operations_hardening.py`
  - `python scripts/verify_native_cutover.py`
  - `python scripts/verify_native_soak_report.py --min-iterations 6 --max-failures 0 --max-age-hours 48`
- Verified Java executor compilation/tests for extracted services:
  - `.\gradlew.bat test --tests com.xptool.executor.CommandIdDeduplicationServiceTest --tests com.xptool.executor.CommandFilePathResolverTest --tests com.xptool.executor.ManualMetricsGateTelemetryServiceTest`
- Recorded completion markers:
  - `PHASE 13 STARTED`
  - `PHASE 13 COMPLETE`

## Exit Criteria
- Command-ingest id/path policy ownership is extracted from `CommandExecutor` into focused runtime services.
- Manual-metrics runtime gate telemetry ownership is extracted from `CommandExecutor` into focused runtime service.
- Phase 13 verification script and native guard pack both pass.
- `PHASE 13 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
