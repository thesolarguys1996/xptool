# Native Client Phase 48 Interaction Session Shutdown Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning `InteractionSessionHostFactory` by extracting shutdown service/host assembly into a focused `InteractionSessionShutdownFactory` component.

## Execution Slices
1. `48.1` Define Phase 48 scope, artifacts, and completion gates.
2. `48.2` Extract shutdown service/host assembly from `InteractionSessionHostFactory` into focused factory ownership.
3. `48.3` Run Phase 48 verification + guard pack and record `PHASE 48 COMPLETE`.

## Phase 48 Slice Status
- `48.1` complete.
- `48.2` complete.
- `48.3` complete.

## Phase 48.1 Outputs
- Added dedicated Phase 48 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE48_INTERACTION_SESSION_SHUTDOWN_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 48 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 48.2 Outputs
- Added focused shutdown factory component:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionShutdownFactory.java`
- Updated `InteractionSessionHostFactory` shutdown assembly methods to route through focused factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- Added focused shutdown factory regression tests:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownFactoryTest.java`

## Phase 48.3 Outputs
- Added explicit Phase 48 verification script:
  - `scripts/verify_phase48_interaction_session_shutdown_factory_extraction.py`
- Executed Phase 48 verification + guard pack:
  - `python scripts/verify_phase48_interaction_session_shutdown_factory_extraction.py`
  - `python scripts/verify_phase47_interaction_session_command_router_host_factory_extraction.py`
  - `python scripts/verify_phase46_interaction_session_command_router_host_decomposition.py`
  - `python scripts/verify_phase45_interaction_session_shutdown_host_decomposition.py`
  - `python scripts/verify_phase44_interaction_session_click_event_host_decomposition.py`
  - `python scripts/verify_phase43_interaction_session_motor_ownership_host_decomposition.py`
  - `python scripts/verify_phase42_interaction_session_registration_host_decomposition.py`
  - `python scripts/verify_phase41_interaction_session_post_click_settle_host_decomposition.py`
  - `python scripts/verify_phase40_interaction_session_ownership_service_host_decomposition.py`
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
  - `.\gradlew.bat test --tests com.xptool.sessions.InteractionSessionShutdownFactoryTest --tests com.xptool.sessions.InteractionSessionCommandRouterHostFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryCommandRouterHostDecompositionTest --tests com.xptool.sessions.InteractionSessionHostFactoryShutdownServiceHostDecompositionTest --tests com.xptool.sessions.InteractionSessionHostFactoryClickEventServiceHostDecompositionTest --tests com.xptool.sessions.InteractionSessionHostFactoryMotorOwnershipServiceFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryRegistrationServiceFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryPostClickSettleServiceFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryOwnershipServiceFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryCommandRouterServiceFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryShutdownServiceFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryClickEventServiceFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryPostClickSettleHostTest --tests com.xptool.sessions.InteractionSessionHostFactoryRegistrationHostTest --tests com.xptool.sessions.InteractionSessionHostFactoryMotorOwnershipHostTest --tests com.xptool.sessions.InteractionSessionHostFactoryOwnershipHostDelegatesTest --tests com.xptool.sessions.InteractionSessionHostFactoryClickEventHostTest --tests com.xptool.sessions.InteractionSessionHostFactoryShutdownHostTest --tests com.xptool.sessions.InteractionSessionShutdownServiceTest --tests com.xptool.sessions.InteractionSessionClickEventServiceTest --tests com.xptool.sessions.InteractionSessionMotorOwnershipServiceTest --tests com.xptool.sessions.InteractionSessionRegistrationServiceTest --tests com.xptool.sessions.InteractionSessionCommandRouterTest --tests com.xptool.sessions.InteractionSessionOwnershipServiceTest --tests com.xptool.sessions.InteractionPostClickSettleServiceTest --tests com.xptool.executor.InteractionAnchorResolverServiceTest --tests com.xptool.executor.InteractionClickEventTest --tests com.xptool.executor.InteractionClickTelemetryServiceTest --tests com.xptool.executor.MotorDispatchContextServiceTest --tests com.xptool.executor.MotorDispatchAdmissionServiceTest --tests com.xptool.executor.MotorProgramTerminalServiceTest --tests com.xptool.executor.PendingMoveTelemetryServiceTest`
- Recorded completion markers:
  - `PHASE 48 STARTED`
  - `PHASE 48 COMPLETE`

## Exit Criteria
- `InteractionSessionHostFactory` shutdown service/host assembly routes through focused `InteractionSessionShutdownFactory` ownership.
- `InteractionSessionShutdownFactory` provides shutdown service/host creation boundaries with focused regression tests.
- Phase 48 verification script and native guard pack both pass.
- `PHASE 48 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
