# Native Client Phase 197 Interaction Session Factory Entry Service Inputs Session Factory Extraction Plan

Last updated: 2026-04-07

## Goal
Continue thinning interaction-session entry ownership by extracting entry service-input session routing into focused ownership.

## Execution Slices
1. `197.1` Define Phase 197 scope, artifacts, and completion gates.
2. `197.2` Extract `InteractionSessionFactoryEntryServiceInputsSessionFactory` ownership for entry service-input session routing seams.
3. `197.3` Run Phase 197 verification + guard pack and record `PHASE 197 COMPLETE`.

## Phase 197 Slice Status
- `197.1` complete.
- `197.2` complete.
- `197.3` complete.

## Phase 197.1 Outputs
- Added dedicated Phase 197 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE197_INTERACTION_SESSION_FACTORY_ENTRY_SERVICE_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 197 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 197.2 Outputs
- Added focused entry service-input session factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntryServiceInputsSessionFactory.java`
- Added focused entry service-input session factory coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryEntryServiceInputsSessionFactoryTest.java`

## Phase 197.3 Outputs
- Added explicit Phase 197 verification script:
  - `scripts/verify_phase197_interaction_session_factory_entry_service_inputs_session_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase197_interaction_session_factory_entry_service_inputs_session_factory_extraction.py`
  - `python scripts/verify_phase196_interaction_session_macro_pass_ag_signoff.py`
  - `python scripts/verify_phase195_interaction_session_factory_runtime_entry_session_factory_extraction.py`
- Verified Java extraction tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryEntryServiceInputsSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryEntrySessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryServiceInputsSessionFactoryTest`
- Recorded completion markers:
  - `PHASE 197 STARTED`
  - `PHASE 197 COMPLETE`

## Exit Criteria
- Entry service-input session routing ownership is extracted into a focused factory component.
- Focused extraction tests and phase verification guard pack pass.
- `PHASE 197 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
