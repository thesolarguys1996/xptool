# Native Client Phase 211 Interaction Session Factory Assembly Runtime Entry Bundle Factory Inputs Session Factory Extraction Plan

Last updated: 2026-04-07

## Goal
Continue thinning interaction-session assembly-runtime entry ownership by extracting bundle-factory-input session routing into focused ownership.

## Execution Slices
1. `211.1` Define Phase 211 scope, artifacts, and completion gates.
2. `211.2` Extract `InteractionSessionFactoryAssemblyRuntimeEntryBundleFactoryInputsSessionFactory` ownership for bundle-factory-input session routing seams.
3. `211.3` Run Phase 211 verification + guard pack and record `PHASE 211 COMPLETE`.

## Phase 211 Slice Status
- `211.1` complete.
- `211.2` complete.
- `211.3` complete.

## Phase 211.1 Outputs
- Added dedicated Phase 211 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE211_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_ENTRY_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 211 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 211.2 Outputs
- Added focused assembly-runtime entry bundle-factory-input session factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeEntryBundleFactoryInputsSessionFactory.java`
- Added focused assembly-runtime entry bundle-factory-input session factory coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeEntryBundleFactoryInputsSessionFactoryTest.java`

## Phase 211.3 Outputs
- Added explicit Phase 211 verification script:
  - `scripts/verify_phase211_interaction_session_factory_assembly_runtime_entry_bundle_factory_inputs_session_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase211_interaction_session_factory_assembly_runtime_entry_bundle_factory_inputs_session_factory_extraction.py`
  - `python scripts/verify_phase210_interaction_session_factory_assembly_runtime_entry_assembly_typed_routing_extraction.py`
  - `python scripts/verify_phase209_interaction_session_factory_assembly_runtime_entry_assembly_session_factory_extraction.py`
- Verified Java extraction tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryAssemblyRuntimeEntryBundleFactoryInputsSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryAssemblyRuntimeSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactoryTest`
- Recorded completion markers:
  - `PHASE 211 STARTED`
  - `PHASE 211 COMPLETE`

## Exit Criteria
- Assembly-runtime entry bundle-factory-input session routing ownership is extracted into a focused factory component.
- Focused extraction tests and phase verification guard pack pass.
- `PHASE 211 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
