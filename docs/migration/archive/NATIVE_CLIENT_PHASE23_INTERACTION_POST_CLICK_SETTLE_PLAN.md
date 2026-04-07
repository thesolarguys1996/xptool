# Native Client Phase 23 Interaction Post-Click Settle Decomposition Plan

Last updated: 2026-04-05

## Goal
Continue reducing `InteractionSession` runtime ownership by extracting post-click settle scheduling/state ownership into a focused runtime service boundary.

## Execution Slices
1. `23.1` Define Phase 23 scope, artifacts, and completion gates.
2. `23.2` Extract post-click settle scheduling/state ownership from `InteractionSession` and delegate runtime wiring.
3. `23.3` Run Phase 23 verification + guard pack and record `PHASE 23 COMPLETE`.

## Phase 23 Slice Status
- `23.1` complete.
- `23.2` complete.
- `23.3` complete.

## Phase 23.1 Outputs
- Added dedicated Phase 23 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE23_INTERACTION_POST_CLICK_SETTLE_PLAN.md`
- Updated migration/task/status artifacts with Phase 23 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 23.2 Outputs
- Extracted post-click settle scheduling/state ownership from `InteractionSession` into dedicated runtime service:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionPostClickSettleService.java`
- Updated `InteractionSession` to orchestration-only wiring/delegation for post-click settle behavior:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- Added focused post-click settle runtime regression tests:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`

## Phase 23.3 Outputs
- Added explicit Phase 23 verification script:
  - `scripts/verify_phase23_interaction_post_click_settle.py`
- Executed Phase 23 verification + guard pack:
  - `python scripts/verify_phase23_interaction_post_click_settle.py`
  - `python scripts/verify_phase22_interaction_click_event_packaging.py`
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
- Verified Java executor/session tests for extraction wave:
  - `.\gradlew.bat test --tests com.xptool.sessions.InteractionPostClickSettleServiceTest --tests com.xptool.executor.InteractionClickEventTest --tests com.xptool.executor.InteractionClickTelemetryServiceTest --tests com.xptool.executor.InteractionAnchorResolverServiceTest --tests com.xptool.executor.MotorDispatchContextServiceTest --tests com.xptool.executor.MotorDispatchAdmissionServiceTest --tests com.xptool.executor.MotorProgramTerminalServiceTest --tests com.xptool.executor.PendingMoveTelemetryServiceTest`
- Recorded completion markers:
  - `PHASE 23 STARTED`
  - `PHASE 23 COMPLETE`

## Exit Criteria
- Post-click settle scheduling/state ownership no longer lives directly in `InteractionSession`.
- `InteractionSession` delegates settle scheduling/readiness/execution paths through `InteractionPostClickSettleService`.
- Focused post-click settle tests pass.
- Phase 23 verification script and native guard pack both pass.
- `PHASE 23 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
