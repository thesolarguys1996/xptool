# Native Client Phase 191 Interaction Session Factory Factory Inputs Session Factory Extraction Plan

Last updated: 2026-04-07

## Goal
Continue thinning interaction-session factory ownership by extracting factory-input session creation into focused ownership.

## Execution Slices
1. `191.1` Define Phase 191 scope, artifacts, and completion gates.
2. `191.2` Extract `InteractionSessionFactoryFactoryInputsSessionFactory` ownership for factory-input session creation seams.
3. `191.3` Run Phase 191 verification + guard pack and record `PHASE 191 COMPLETE`.

## Phase 191 Slice Status
- `191.1` complete.
- `191.2` complete.
- `191.3` complete.

## Phase 191.1 Outputs
- Added dedicated Phase 191 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE191_INTERACTION_SESSION_FACTORY_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 191 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 191.2 Outputs
- Added focused factory-input session factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryFactoryInputsSessionFactory.java`
- Added focused factory-input session factory coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryFactoryInputsSessionFactoryTest.java`

## Phase 191.3 Outputs
- Added explicit Phase 191 verification script:
  - `scripts/verify_phase191_interaction_session_factory_factory_inputs_session_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase191_interaction_session_factory_factory_inputs_session_factory_extraction.py`
  - `python scripts/verify_phase190_interaction_session_macro_pass_af_signoff.py`
  - `python scripts/verify_phase189_interaction_session_factory_entry_session_factory_extraction.py`
- Verified Java extraction tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryFactoryInputsSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryDefaultFactoryInputsSessionFactoryTest`
- Recorded completion markers:
  - `PHASE 191 STARTED`
  - `PHASE 191 COMPLETE`

## Exit Criteria
- Factory-input session creation ownership is extracted into a focused factory component.
- Focused extraction tests and phase verification guard pack pass.
- `PHASE 191 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
