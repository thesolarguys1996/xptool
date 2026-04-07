# Native Client Phase 39 Interaction Session Command Router Service Factory Decomposition Plan

Last updated: 2026-04-06

## Goal
Continue reducing `InteractionSession` constructor ownership by extracting command-router service construction into focused host-factory methods.

## Execution Slices
1. `39.1` Define Phase 39 scope, artifacts, and completion gates.
2. `39.2` Extract `InteractionSession` command-router service construction into dedicated host-factory boundaries.
3. `39.3` Run Phase 39 verification + guard pack and record `PHASE 39 COMPLETE`.

## Phase 39 Slice Status
- `39.1` complete.
- `39.2` complete.
- `39.3` complete.

## Phase 39.1 Outputs
- Added dedicated Phase 39 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE39_INTERACTION_SESSION_COMMAND_ROUTER_SERVICE_FACTORY_PLAN.md`
- Updated migration/task/status artifacts with Phase 39 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 39.2 Outputs
- Extracted interaction-session command-router service construction from `InteractionSession` constructor into dedicated host-factory method:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- Added host-backed command-router service factory overload for focused command-router service wiring boundaries:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- Updated `InteractionSession` command-router service construction to host-factory delegation:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- Added focused command-router service-factory regression test:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterServiceFactoryTest.java`

## Phase 39.3 Outputs
- Added explicit Phase 39 verification script:
  - `scripts/verify_phase39_interaction_session_command_router_service_factory.py`
- Executed Phase 39 verification + guard pack:
  - `python scripts/verify_phase39_interaction_session_command_router_service_factory.py`
  - `python scripts/verify_phase38_interaction_session_shutdown_service_factory.py`
  - `python scripts/verify_phase37_interaction_session_click_event_service_factory.py`
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
  - `.\gradlew.bat test --tests com.xptool.sessions.InteractionSessionHostFactoryCommandRouterServiceFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryShutdownServiceFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryClickEventServiceFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryPostClickSettleHostTest --tests com.xptool.sessions.InteractionSessionHostFactoryRegistrationHostTest --tests com.xptool.sessions.InteractionSessionHostFactoryMotorOwnershipHostTest --tests com.xptool.sessions.InteractionSessionHostFactoryOwnershipHostDelegatesTest --tests com.xptool.sessions.InteractionSessionHostFactoryClickEventHostTest --tests com.xptool.sessions.InteractionSessionHostFactoryShutdownHostTest --tests com.xptool.sessions.InteractionSessionShutdownServiceTest --tests com.xptool.sessions.InteractionSessionClickEventServiceTest --tests com.xptool.sessions.InteractionSessionMotorOwnershipServiceTest --tests com.xptool.sessions.InteractionSessionRegistrationServiceTest --tests com.xptool.sessions.InteractionSessionCommandRouterTest --tests com.xptool.sessions.InteractionSessionOwnershipServiceTest --tests com.xptool.sessions.InteractionPostClickSettleServiceTest --tests com.xptool.executor.InteractionAnchorResolverServiceTest --tests com.xptool.executor.InteractionClickEventTest --tests com.xptool.executor.InteractionClickTelemetryServiceTest --tests com.xptool.executor.MotorDispatchContextServiceTest --tests com.xptool.executor.MotorDispatchAdmissionServiceTest --tests com.xptool.executor.MotorProgramTerminalServiceTest --tests com.xptool.executor.PendingMoveTelemetryServiceTest`
- Recorded completion markers:
  - `PHASE 39 STARTED`
  - `PHASE 39 COMPLETE`

## Exit Criteria
- `InteractionSession` constructor no longer constructs `InteractionSessionCommandRouter` inline.
- `InteractionSession` consumes `InteractionSessionHostFactory.createCommandRouterService(...)`.
- Focused command-router service-factory regression test passes.
- Phase 39 verification script and native guard pack both pass.
- `PHASE 39 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
