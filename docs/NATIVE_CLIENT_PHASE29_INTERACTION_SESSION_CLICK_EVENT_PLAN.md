# Native Client Phase 29 Interaction Session Click-Event Intake Decomposition Plan

Last updated: 2026-04-05

## Goal
Continue reducing `InteractionSession` runtime ownership by extracting interaction click-event intake delegation into a focused session runtime service boundary.

## Execution Slices
1. `29.1` Define Phase 29 scope, artifacts, and completion gates.
2. `29.2` Extract `InteractionSession` click-event intake delegation into dedicated service boundaries.
3. `29.3` Run Phase 29 verification + guard pack and record `PHASE 29 COMPLETE`.

## Phase 29 Slice Status
- `29.1` complete.
- `29.2` complete.
- `29.3` complete.

## Phase 29.1 Outputs
- Added dedicated Phase 29 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE29_INTERACTION_SESSION_CLICK_EVENT_PLAN.md`
- Updated migration/task/status artifacts with Phase 29 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 29.2 Outputs
- Extracted interaction-session click-event intake delegation from `InteractionSession` into dedicated service:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionClickEventService.java`
- Updated `InteractionSession` click-event intake path to delegate through service ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- Added focused interaction-session click-event service regression tests:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`

## Phase 29.3 Outputs
- Added explicit Phase 29 verification script:
  - `scripts/verify_phase29_interaction_session_click_event.py`
- Executed Phase 29 verification + guard pack:
  - `python scripts/verify_phase29_interaction_session_click_event.py`
  - `python scripts/verify_phase28_interaction_session_motor_ownership.py`
  - `python scripts/verify_phase27_interaction_session_registration.py`
  - `python scripts/verify_phase26_interaction_session_command_router.py`
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
  - `.\gradlew.bat test --tests com.xptool.sessions.InteractionSessionClickEventServiceTest --tests com.xptool.sessions.InteractionSessionMotorOwnershipServiceTest --tests com.xptool.sessions.InteractionSessionRegistrationServiceTest --tests com.xptool.sessions.InteractionSessionCommandRouterTest --tests com.xptool.sessions.InteractionSessionOwnershipServiceTest --tests com.xptool.sessions.InteractionPostClickSettleServiceTest --tests com.xptool.executor.InteractionAnchorResolverServiceTest --tests com.xptool.executor.InteractionClickEventTest --tests com.xptool.executor.InteractionClickTelemetryServiceTest --tests com.xptool.executor.MotorDispatchContextServiceTest --tests com.xptool.executor.MotorDispatchAdmissionServiceTest --tests com.xptool.executor.MotorProgramTerminalServiceTest --tests com.xptool.executor.PendingMoveTelemetryServiceTest`
- Recorded completion markers:
  - `PHASE 29 STARTED`
  - `PHASE 29 COMPLETE`

## Exit Criteria
- Interaction click-event intake delegation no longer lives directly in `InteractionSession`.
- `InteractionSession` delegates interaction click-event intake through `InteractionSessionClickEventService`.
- Focused interaction-session click-event service tests pass.
- Phase 29 verification script and native guard pack both pass.
- `PHASE 29 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
