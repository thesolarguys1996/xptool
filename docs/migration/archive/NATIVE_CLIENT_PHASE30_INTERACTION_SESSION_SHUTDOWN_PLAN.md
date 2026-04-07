# Native Client Phase 30 Interaction Session Shutdown Decomposition Plan

Last updated: 2026-04-05

## Goal
Continue reducing `InteractionSession` runtime ownership by extracting interaction-session shutdown lifecycle ownership into a focused session runtime service boundary.

## Execution Slices
1. `30.1` Define Phase 30 scope, artifacts, and completion gates.
2. `30.2` Extract `InteractionSession` shutdown lifecycle ownership into dedicated service boundaries.
3. `30.3` Run Phase 30 verification + guard pack and record `PHASE 30 COMPLETE`.

## Phase 30 Slice Status
- `30.1` complete.
- `30.2` complete.
- `30.3` complete.

## Phase 30.1 Outputs
- Added dedicated Phase 30 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE30_INTERACTION_SESSION_SHUTDOWN_PLAN.md`
- Updated migration/task/status artifacts with Phase 30 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 30.2 Outputs
- Extracted interaction-session shutdown lifecycle ownership from `InteractionSession` into dedicated service:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionShutdownService.java`
- Updated `InteractionSession` shutdown path to delegate through service ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- Added focused interaction-session shutdown service regression tests:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`

## Phase 30.3 Outputs
- Added explicit Phase 30 verification script:
  - `scripts/verify_phase30_interaction_session_shutdown.py`
- Executed Phase 30 verification + guard pack:
  - `python scripts/verify_phase30_interaction_session_shutdown.py`
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
  - `.\gradlew.bat test --tests com.xptool.sessions.InteractionSessionShutdownServiceTest --tests com.xptool.sessions.InteractionSessionClickEventServiceTest --tests com.xptool.sessions.InteractionSessionMotorOwnershipServiceTest --tests com.xptool.sessions.InteractionSessionRegistrationServiceTest --tests com.xptool.sessions.InteractionSessionCommandRouterTest --tests com.xptool.sessions.InteractionSessionOwnershipServiceTest --tests com.xptool.sessions.InteractionPostClickSettleServiceTest --tests com.xptool.executor.InteractionAnchorResolverServiceTest --tests com.xptool.executor.InteractionClickEventTest --tests com.xptool.executor.InteractionClickTelemetryServiceTest --tests com.xptool.executor.MotorDispatchContextServiceTest --tests com.xptool.executor.MotorDispatchAdmissionServiceTest --tests com.xptool.executor.MotorProgramTerminalServiceTest --tests com.xptool.executor.PendingMoveTelemetryServiceTest`
- Recorded completion markers:
  - `PHASE 30 STARTED`
  - `PHASE 30 COMPLETE`

## Exit Criteria
- Interaction shutdown lifecycle delegation no longer lives directly in `InteractionSession`.
- `InteractionSession` delegates shutdown lifecycle through `InteractionSessionShutdownService`.
- Focused interaction-session shutdown service tests pass.
- Phase 30 verification script and native guard pack both pass.
- `PHASE 30 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
