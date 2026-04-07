# Native Client Phase 27 Interaction Session Registration Decomposition Plan

Last updated: 2026-04-05

## Goal
Continue reducing `InteractionSession` runtime ownership by extracting interaction-session registration lifecycle state and mutations into a focused registration service boundary.

## Execution Slices
1. `27.1` Define Phase 27 scope, artifacts, and completion gates.
2. `27.2` Extract `InteractionSession` registration lifecycle ownership into a dedicated registration service and delegate runtime wiring.
3. `27.3` Run Phase 27 verification + guard pack and record `PHASE 27 COMPLETE`.

## Phase 27 Slice Status
- `27.1` complete.
- `27.2` complete.
- `27.3` complete.

## Phase 27.1 Outputs
- Added dedicated Phase 27 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE27_INTERACTION_SESSION_REGISTRATION_PLAN.md`
- Updated migration/task/status artifacts with Phase 27 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 27.2 Outputs
- Extracted interaction-session registration lifecycle ownership from `InteractionSession` into dedicated service:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRegistrationService.java`
- Updated `InteractionSession` registration ownership to delegate through registration service:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- Added focused interaction-session registration regression tests:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`

## Phase 27.3 Outputs
- Added explicit Phase 27 verification script:
  - `scripts/verify_phase27_interaction_session_registration.py`
- Executed Phase 27 verification + guard pack:
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
  - `.\gradlew.bat test --tests com.xptool.sessions.InteractionSessionRegistrationServiceTest --tests com.xptool.sessions.InteractionSessionCommandRouterTest --tests com.xptool.sessions.InteractionSessionOwnershipServiceTest --tests com.xptool.sessions.InteractionPostClickSettleServiceTest --tests com.xptool.executor.InteractionAnchorResolverServiceTest --tests com.xptool.executor.InteractionClickEventTest --tests com.xptool.executor.InteractionClickTelemetryServiceTest --tests com.xptool.executor.MotorDispatchContextServiceTest --tests com.xptool.executor.MotorDispatchAdmissionServiceTest --tests com.xptool.executor.MotorProgramTerminalServiceTest --tests com.xptool.executor.PendingMoveTelemetryServiceTest`
- Recorded completion markers:
  - `PHASE 27 STARTED`
  - `PHASE 27 COMPLETE`

## Exit Criteria
- Registration state (`SessionManager.Registration`) no longer lives directly in `InteractionSession`.
- `InteractionSession` delegates registration lifecycle mutations through `InteractionSessionRegistrationService`.
- Focused interaction-session registration tests pass.
- Phase 27 verification script and native guard pack both pass.
- `PHASE 27 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
