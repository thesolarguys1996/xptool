# Native Client Phase 24 Interaction Session Ownership Decomposition Plan

Last updated: 2026-04-05

## Goal
Continue reducing `InteractionSession` runtime ownership by extracting registration and motor-ownership orchestration into a focused session ownership runtime service boundary.

## Execution Slices
1. `24.1` Define Phase 24 scope, artifacts, and completion gates.
2. `24.2` Extract interaction session ownership orchestration from `InteractionSession` and delegate runtime wiring.
3. `24.3` Run Phase 24 verification + guard pack and record `PHASE 24 COMPLETE`.

## Phase 24 Slice Status
- `24.1` complete.
- `24.2` complete.
- `24.3` complete.

## Phase 24.1 Outputs
- Added dedicated Phase 24 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE24_INTERACTION_SESSION_OWNERSHIP_PLAN.md`
- Updated migration/task/status artifacts with Phase 24 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 24.2 Outputs
- Extracted interaction session registration + motor-ownership orchestration from `InteractionSession` into dedicated session runtime service:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionOwnershipService.java`
- Updated `InteractionSession` game-tick ownership flow to orchestration-only delegation:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- Added focused session ownership orchestration regression tests:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- Removed stale unused import flagged by IDE diagnostics:
  - `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`

## Phase 24.3 Outputs
- Added explicit Phase 24 verification script:
  - `scripts/verify_phase24_interaction_session_ownership.py`
- Executed Phase 24 verification + guard pack:
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
  - `PHASE 24 STARTED`
  - `PHASE 24 COMPLETE`

## Exit Criteria
- Interaction-session registration/motor-ownership orchestration no longer lives directly in `InteractionSession.onGameTick(...)`.
- `InteractionSession` delegates ownership orchestration through `InteractionSessionOwnershipService`.
- Focused interaction-session ownership tests pass.
- Phase 24 verification script and native guard pack both pass.
- `PHASE 24 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
