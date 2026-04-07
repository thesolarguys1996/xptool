# Native Client Phase 181 Interaction Session Factory Runtime Bundle Factory Inputs Session Factory Extraction Plan

Last updated: 2026-04-07

## Goal
Continue thinning interaction-session factory ownership by extracting runtime-bundle-factory-input session creation into focused ownership.

## Execution Slices
1. `181.1` Define Phase 181 scope, artifacts, and completion gates.
2. `181.2` Extract `InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactory` ownership for runtime-bundle-factory-input session creation seams.
3. `181.3` Run Phase 181 verification + guard pack and record `PHASE 181 COMPLETE`.

## Phase 181 Slice Status
- `181.1` complete.
- `181.2` complete.
- `181.3` complete.

## Phase 181.1 Outputs
- Added dedicated Phase 181 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE181_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 181 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 181.2 Outputs
- Added focused runtime-bundle-factory-input session factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactory.java`
- Added focused runtime-bundle-factory-input session factory coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactoryTest.java`

## Phase 181.3 Outputs
- Added explicit Phase 181 verification script:
  - `scripts/verify_phase181_interaction_session_factory_runtime_bundle_factory_inputs_session_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase181_interaction_session_factory_runtime_bundle_factory_inputs_session_factory_extraction.py`
  - `python scripts/verify_phase180_interaction_session_factory_assembly_factory_input_typed_routing_extraction.py`
  - `python scripts/verify_phase179_interaction_session_factory_assembly_factory_inputs_session_factory_extraction.py`
- Verified Java extraction tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleFactoryTest`
- Recorded completion markers:
  - `PHASE 181 STARTED`
  - `PHASE 181 COMPLETE`

## Exit Criteria
- Runtime-bundle-factory-input session creation ownership is extracted into a focused factory component.
- Focused extraction tests and phase verification guard pack pass.
- `PHASE 181 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
