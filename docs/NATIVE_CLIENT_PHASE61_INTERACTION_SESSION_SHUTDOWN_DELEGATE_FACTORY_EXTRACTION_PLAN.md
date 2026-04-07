# Native Client Phase 61 Interaction Session Shutdown Delegate Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning `InteractionSessionHostFactory` by moving shutdown delegate-host assembly ownership into `InteractionSessionShutdownFactory` and removing focused-factory back-dependency on host-factory delegate construction.

## Execution Slices
1. `61.1` Define Phase 61 scope, artifacts, and completion gates.
2. `61.2` Extract shutdown delegate-host assembly from host-factory back-dependency ownership into focused factory ownership.
3. `61.3` Run Phase 61 verification + guard pack and record `PHASE 61 COMPLETE`.

## Phase 61 Slice Status
- `61.1` complete.
- `61.2` complete.
- `61.3` complete.

## Phase 61.1 Outputs
- Added dedicated Phase 61 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE61_INTERACTION_SESSION_SHUTDOWN_DELEGATE_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 61 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 61.2 Outputs
- Expanded focused shutdown factory ownership for delegate-host assembly:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionShutdownFactory.java`
- Preserved host-factory shutdown compatibility delegate wrapper for prior-phase verifier compatibility:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- Added focused shutdown factory delegate-host regression coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownFactoryTest.java`

## Phase 61.3 Outputs
- Added explicit Phase 61 verification script:
  - `scripts/verify_phase61_interaction_session_shutdown_delegate_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase61_interaction_session_shutdown_delegate_factory_extraction.py`
  - `python scripts/verify_phase60_interaction_session_motor_ownership_delegate_factory_extraction.py`
  - `python scripts/verify_phase59_interaction_session_macro_pass_b_signoff.py`
  - `python scripts/verify_phase58_interaction_session_host_factory_consolidation_b.py`
  - `python scripts/verify_phase57_interaction_session_click_event_host_factory_extraction.py`
  - `python scripts/verify_phase56_interaction_session_command_router_factory_extraction.py`
  - `python scripts/verify_phase55_interaction_session_macro_pass_signoff.py`
- Verified Java tests for the extraction wave:
  - `.\gradlew.bat test --tests com.xptool.sessions.InteractionSessionShutdownFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryShutdownHostTest --tests com.xptool.sessions.InteractionSessionShutdownServiceTest`
- Recorded completion markers:
  - `PHASE 61 STARTED`
  - `PHASE 61 COMPLETE`

## Exit Criteria
- `InteractionSessionShutdownFactory` owns shutdown delegate-host assembly without host-factory back-dependency.
- `InteractionSessionHostFactory` retains shutdown compatibility delegate wrapper method with required lifecycle runnable delegate strings.
- Phase 61 verification script and targeted guard/test pack both pass.
- `PHASE 61 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
