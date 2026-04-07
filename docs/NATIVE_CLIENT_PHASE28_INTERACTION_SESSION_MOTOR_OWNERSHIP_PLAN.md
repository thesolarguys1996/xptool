# Native Client Phase 28 Interaction Session Motor-Ownership Adapter Decomposition Plan

Last updated: 2026-04-05

## Goal
Continue reducing `InteractionSession` runtime ownership by extracting remaining interaction motor-ownership adapter delegation into a focused motor-ownership service boundary.

## Execution Slices
1. `28.1` Define Phase 28 scope, artifacts, and completion gates.
2. `28.2` Extract `InteractionSession` motor-ownership adapter delegation into dedicated service boundaries and host-factory wiring.
3. `28.3` Run Phase 28 verification + guard pack and record `PHASE 28 COMPLETE`.

## Phase 28 Slice Status
- `28.1` complete.
- `28.2` complete.
- `28.3` complete.

## Phase 28.1 Outputs
- Added dedicated Phase 28 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE28_INTERACTION_SESSION_MOTOR_OWNERSHIP_PLAN.md`
- Updated migration/task/status artifacts with Phase 28 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 28.2 Outputs
- Extracted interaction-session motor-ownership adapter ownership from `InteractionSession` into dedicated service:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionMotorOwnershipService.java`
- Extended session host factory with motor-ownership host assembly and delegated ownership release path in ownership host wiring:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- Updated `InteractionSession` to delegate motor-ownership acquire/release via service ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- Added focused interaction-session motor-ownership service regression tests:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`

## Phase 28.3 Outputs
- Added explicit Phase 28 verification script:
  - `scripts/verify_phase28_interaction_session_motor_ownership.py`
- Executed Phase 28 verification + guard pack:
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
  - `.\gradlew.bat test --tests com.xptool.sessions.InteractionSessionMotorOwnershipServiceTest --tests com.xptool.sessions.InteractionSessionRegistrationServiceTest --tests com.xptool.sessions.InteractionSessionCommandRouterTest --tests com.xptool.sessions.InteractionSessionOwnershipServiceTest --tests com.xptool.sessions.InteractionPostClickSettleServiceTest --tests com.xptool.executor.InteractionAnchorResolverServiceTest --tests com.xptool.executor.InteractionClickEventTest --tests com.xptool.executor.InteractionClickTelemetryServiceTest --tests com.xptool.executor.MotorDispatchContextServiceTest --tests com.xptool.executor.MotorDispatchAdmissionServiceTest --tests com.xptool.executor.MotorProgramTerminalServiceTest --tests com.xptool.executor.PendingMoveTelemetryServiceTest`
- Recorded completion markers:
  - `PHASE 28 STARTED`
  - `PHASE 28 COMPLETE`

## Exit Criteria
- Remaining interaction motor-ownership adapter delegation no longer lives directly in `InteractionSession`.
- `InteractionSession` delegates motor-ownership acquire/release through `InteractionSessionMotorOwnershipService`.
- Focused interaction-session motor-ownership tests pass.
- Phase 28 verification script and native guard pack both pass.
- `PHASE 28 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
