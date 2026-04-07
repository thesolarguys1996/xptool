# Native Client Phase 36 Interaction Session Post-Click Settle Service Factory Decomposition Plan

Last updated: 2026-04-05

## Goal
Continue reducing `InteractionSession` constructor ownership by extracting post-click-settle service construction and host assembly into focused host-factory methods.

## Execution Slices
1. `36.1` Define Phase 36 scope, artifacts, and completion gates.
2. `36.2` Extract `InteractionSession` post-click-settle service construction into dedicated host-factory boundaries.
3. `36.3` Run Phase 36 verification + guard pack and record `PHASE 36 COMPLETE`.

## Phase 36 Slice Status
- `36.1` complete.
- `36.2` complete.
- `36.3` complete.

## Phase 36.1 Outputs
- Added dedicated Phase 36 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE36_INTERACTION_SESSION_POST_CLICK_SETTLE_SERVICE_FACTORY_PLAN.md`
- Updated migration/task/status artifacts with Phase 36 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 36.2 Outputs
- Extracted interaction-session post-click-settle service construction from `InteractionSession` constructor into dedicated host-factory method:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- Added delegate-first post-click-settle host adapter builder for focused settle-host wiring boundaries:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- Updated `InteractionSession` post-click-settle service construction to host-factory delegation:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- Added focused post-click-settle host delegate regression test:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleHostTest.java`

## Phase 36.3 Outputs
- Added explicit Phase 36 verification script:
  - `scripts/verify_phase36_interaction_session_post_click_settle_service_factory.py`
- Executed Phase 36 verification + guard pack:
  - `python scripts/verify_phase36_interaction_session_post_click_settle_service_factory.py`
  - `python scripts/verify_phase35_interaction_session_registration_service_factory.py`
  - `python scripts/verify_phase34_interaction_session_motor_ownership_service_factory.py`
  - `python scripts/verify_phase33_interaction_session_ownership_service_factory.py`
  - `python scripts/verify_phase32_interaction_session_click_event_host_factory.py`
  - `python scripts/verify_phase31_interaction_session_shutdown_host_factory.py`
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
  - `.\gradlew.bat test --tests com.xptool.sessions.InteractionSessionHostFactoryPostClickSettleHostTest --tests com.xptool.sessions.InteractionSessionHostFactoryRegistrationHostTest --tests com.xptool.sessions.InteractionSessionHostFactoryMotorOwnershipHostTest --tests com.xptool.sessions.InteractionSessionHostFactoryOwnershipHostDelegatesTest --tests com.xptool.sessions.InteractionSessionHostFactoryClickEventHostTest --tests com.xptool.sessions.InteractionSessionHostFactoryShutdownHostTest --tests com.xptool.sessions.InteractionSessionShutdownServiceTest --tests com.xptool.sessions.InteractionSessionClickEventServiceTest --tests com.xptool.sessions.InteractionSessionMotorOwnershipServiceTest --tests com.xptool.sessions.InteractionSessionRegistrationServiceTest --tests com.xptool.sessions.InteractionSessionCommandRouterTest --tests com.xptool.sessions.InteractionSessionOwnershipServiceTest --tests com.xptool.sessions.InteractionPostClickSettleServiceTest --tests com.xptool.executor.InteractionAnchorResolverServiceTest --tests com.xptool.executor.InteractionClickEventTest --tests com.xptool.executor.InteractionClickTelemetryServiceTest --tests com.xptool.executor.MotorDispatchContextServiceTest --tests com.xptool.executor.MotorDispatchAdmissionServiceTest --tests com.xptool.executor.MotorProgramTerminalServiceTest --tests com.xptool.executor.PendingMoveTelemetryServiceTest`
- Recorded completion markers:
  - `PHASE 36 STARTED`
  - `PHASE 36 COMPLETE`

## Exit Criteria
- `InteractionSession` constructor no longer constructs `InteractionPostClickSettleService` inline.
- `InteractionSession` consumes `InteractionSessionHostFactory.createPostClickSettleService(...)`.
- Focused post-click-settle host delegate regression test passes.
- Phase 36 verification script and native guard pack both pass.
- `PHASE 36 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
