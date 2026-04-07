# Native Client Phase 25 Interaction Session Host-Factory Decomposition Plan

Last updated: 2026-04-05

## Goal
Continue reducing `InteractionSession` constructor ownership by extracting session host-assembly blocks into a focused host-factory boundary.

## Execution Slices
1. `25.1` Define Phase 25 scope, artifacts, and completion gates.
2. `25.2` Extract `InteractionSession` host wiring assembly into a dedicated host-factory service boundary.
3. `25.3` Run Phase 25 verification + guard pack and record `PHASE 25 COMPLETE`.

## Phase 25 Slice Status
- `25.1` complete.
- `25.2` complete.
- `25.3` complete.

## Phase 25.1 Outputs
- Added dedicated Phase 25 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE25_INTERACTION_SESSION_HOST_FACTORY_PLAN.md`
- Updated migration/task/status artifacts with Phase 25 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 25.2 Outputs
- Extracted interaction session host-construction assembly into dedicated host factory:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- Updated `InteractionSession` constructor wiring to consume host-factory boundaries:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- Preserved behavior contracts while removing large in-constructor anonymous host blocks:
  - `InteractionPostClickSettleService.Host`
  - `InteractionSessionOwnershipService.Host`

## Phase 25.3 Outputs
- Added explicit Phase 25 verification script:
  - `scripts/verify_phase25_interaction_session_host_factory.py`
- Executed Phase 25 verification + guard pack:
  - `python scripts/verify_phase25_interaction_session_host_factory.py`
  - `python scripts/verify_phase24_interaction_session_ownership.py`
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
  - `.\gradlew.bat test --tests com.xptool.sessions.InteractionSessionOwnershipServiceTest --tests com.xptool.sessions.InteractionPostClickSettleServiceTest --tests com.xptool.executor.InteractionAnchorResolverServiceTest --tests com.xptool.executor.InteractionClickEventTest --tests com.xptool.executor.InteractionClickTelemetryServiceTest --tests com.xptool.executor.MotorDispatchContextServiceTest --tests com.xptool.executor.MotorDispatchAdmissionServiceTest --tests com.xptool.executor.MotorProgramTerminalServiceTest --tests com.xptool.executor.PendingMoveTelemetryServiceTest`
- Recorded completion markers:
  - `PHASE 25 STARTED`
  - `PHASE 25 COMPLETE`

## Exit Criteria
- `InteractionSession` no longer assembles large anonymous host blocks inline.
- Host assembly for interaction settle/ownership runtime services is centralized in `InteractionSessionHostFactory`.
- Focused session regression tests remain green.
- Phase 25 verification script and native guard pack both pass.
- `PHASE 25 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
