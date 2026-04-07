# Native Client Phase 185 Interaction Session Factory Default Runtime Bundle Factory Inputs Session Factory Extraction Plan

Last updated: 2026-04-07

## Goal
Continue thinning interaction-session factory ownership by extracting default-runtime-bundle-factory-input session creation into focused ownership.

## Execution Slices
1. `185.1` Define Phase 185 scope, artifacts, and completion gates.
2. `185.2` Extract `InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactory` ownership for default-runtime-bundle-factory-input session creation seams.
3. `185.3` Run Phase 185 verification + guard pack and record `PHASE 185 COMPLETE`.

## Phase 185 Slice Status
- `185.1` complete.
- `185.2` complete.
- `185.3` complete.

## Phase 185.1 Outputs
- Added dedicated Phase 185 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE185_INTERACTION_SESSION_FACTORY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 185 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 185.2 Outputs
- Added focused default-runtime-bundle-factory-input session factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactory.java`
- Added focused default-runtime-bundle-factory-input session factory coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactoryTest.java`

## Phase 185.3 Outputs
- Added explicit Phase 185 verification script:
  - `scripts/verify_phase185_interaction_session_factory_default_runtime_bundle_factory_inputs_session_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase185_interaction_session_factory_default_runtime_bundle_factory_inputs_session_factory_extraction.py`
  - `python scripts/verify_phase184_interaction_session_macro_pass_ae_signoff.py`
  - `python scripts/verify_phase183_interaction_session_factory_runtime_bundle_session_factory_extraction.py`
- Verified Java extraction tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryDefaultEntryFactoryTest`
- Recorded completion markers:
  - `PHASE 185 STARTED`
  - `PHASE 185 COMPLETE`

## Exit Criteria
- Default-runtime-bundle-factory-input session creation ownership is extracted into a focused factory component.
- Focused extraction tests and phase verification guard pack pass.
- `PHASE 185 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
