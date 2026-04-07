# Native Client Phase 60 Interaction Session Motor-Ownership Delegate Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning `InteractionSessionHostFactory` by moving motor-ownership delegate-host assembly ownership into `InteractionSessionMotorOwnershipFactory` and removing focused-factory back-dependency on host-factory delegate construction.

## Execution Slices
1. `60.1` Define Phase 60 scope, artifacts, and completion gates.
2. `60.2` Extract motor-ownership delegate-host assembly from host-factory back-dependency ownership into focused factory ownership.
3. `60.3` Run Phase 60 verification + guard pack and record `PHASE 60 COMPLETE`.

## Phase 60 Slice Status
- `60.1` complete.
- `60.2` complete.
- `60.3` complete.

## Phase 60.1 Outputs
- Added dedicated Phase 60 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE60_INTERACTION_SESSION_MOTOR_OWNERSHIP_DELEGATE_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 60 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 60.2 Outputs
- Expanded focused motor-ownership factory ownership for delegate-host assembly:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionMotorOwnershipFactory.java`
- Updated host-factory compatibility method to delegate motor-ownership delegate-host assembly through focused factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- Added focused motor-ownership factory delegate-host regression coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipFactoryTest.java`

## Phase 60.3 Outputs
- Added explicit Phase 60 verification script:
  - `scripts/verify_phase60_interaction_session_motor_ownership_delegate_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase60_interaction_session_motor_ownership_delegate_factory_extraction.py`
  - `python scripts/verify_phase59_interaction_session_macro_pass_b_signoff.py`
  - `python scripts/verify_phase58_interaction_session_host_factory_consolidation_b.py`
  - `python scripts/verify_phase57_interaction_session_click_event_host_factory_extraction.py`
  - `python scripts/verify_phase56_interaction_session_command_router_factory_extraction.py`
  - `python scripts/verify_phase55_interaction_session_macro_pass_signoff.py`
- Verified Java tests for the extraction wave:
  - `.\gradlew.bat test --tests com.xptool.sessions.InteractionSessionMotorOwnershipFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryMotorOwnershipHostTest --tests com.xptool.sessions.InteractionSessionMotorOwnershipServiceTest`
- Recorded completion markers:
  - `PHASE 60 STARTED`
  - `PHASE 60 COMPLETE`

## Exit Criteria
- `InteractionSessionMotorOwnershipFactory` owns motor-ownership delegate-host assembly without host-factory back-dependency.
- `InteractionSessionHostFactory` retains compatibility delegate wrapper method while delegating to focused factory ownership.
- Phase 60 verification script and targeted guard/test pack both pass.
- `PHASE 60 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
