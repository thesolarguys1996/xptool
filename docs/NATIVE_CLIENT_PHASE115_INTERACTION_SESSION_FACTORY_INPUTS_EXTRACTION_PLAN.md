# Native Client Phase 115 Interaction Session Factory Inputs Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning interaction-session factory ownership by extracting executor/session/facade positional arguments into focused factory typed-input ownership.

## Execution Slices
1. `115.1` Define Phase 115 scope, artifacts, and completion gates.
2. `115.2` Extract focused interaction-session factory typed inputs contract ownership.
3. `115.3` Run Phase 115 verification + guard pack and record `PHASE 115 COMPLETE`.

## Phase 115 Slice Status
- `115.1` complete.
- `115.2` complete.
- `115.3` complete.

## Phase 115.1 Outputs
- Added dedicated Phase 115 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE115_INTERACTION_SESSION_FACTORY_INPUTS_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 115 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 115.2 Outputs
- Added focused interaction-session factory typed-input contract:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryInputs.java`
- Added focused interaction-session factory typed-input coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryInputsTest.java`

## Phase 115.3 Outputs
- Added explicit Phase 115 verification script:
  - `scripts/verify_phase115_interaction_session_factory_inputs_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase115_interaction_session_factory_inputs_extraction.py`
  - `python scripts/verify_phase110_interaction_session_macro_pass_o_signoff.py`
  - `python scripts/verify_phase109_interaction_session_factory_wiring_consolidation_o.py`
- Verified Java tests for the extraction wave:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryInputsTest --tests com.xptool.sessions.InteractionSessionFactoryTest`
- Recorded completion markers:
  - `PHASE 115 STARTED`
  - `PHASE 115 COMPLETE`

## Exit Criteria
- Factory typed-input ownership is represented by focused `InteractionSessionFactoryInputs`.
- Factory typed-input tests validate field mapping and assembly-input conversion.
- Phase 115 verification script and targeted guard/test pack both pass.
- `PHASE 115 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
