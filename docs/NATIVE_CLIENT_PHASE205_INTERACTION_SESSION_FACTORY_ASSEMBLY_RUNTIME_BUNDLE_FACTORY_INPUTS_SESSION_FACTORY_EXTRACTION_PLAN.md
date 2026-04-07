# Native Client Phase 205 Interaction Session Factory Assembly Runtime Bundle Factory Inputs Session Factory Extraction Plan

Last updated: 2026-04-07

## Goal
Continue thinning interaction-session assembly-runtime ownership by extracting bundle-factory-input session routing into focused ownership.

## Execution Slices
1. `205.1` Define Phase 205 scope, artifacts, and completion gates.
2. `205.2` Extract `InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactory` ownership for bundle-factory-input session routing seams.
3. `205.3` Run Phase 205 verification + guard pack and record `PHASE 205 COMPLETE`.

## Phase 205 Slice Status
- `205.1` complete.
- `205.2` complete.
- `205.3` complete.

## Phase 205.1 Outputs
- Added dedicated Phase 205 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE205_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 205 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 205.2 Outputs
- Added focused assembly-runtime bundle-factory-input session factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactory.java`
- Added focused assembly-runtime bundle-factory-input session factory coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactoryTest.java`

## Phase 205.3 Outputs
- Added explicit Phase 205 verification script:
  - `scripts/verify_phase205_interaction_session_factory_assembly_runtime_bundle_factory_inputs_session_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase205_interaction_session_factory_assembly_runtime_bundle_factory_inputs_session_factory_extraction.py`
  - `python scripts/verify_phase204_interaction_session_factory_assembly_runtime_assembly_typed_routing_extraction.py`
  - `python scripts/verify_phase203_interaction_session_factory_assembly_runtime_assembly_session_factory_extraction.py`
- Verified Java extraction tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryAssemblyRuntimeSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactoryTest`
- Recorded completion markers:
  - `PHASE 205 STARTED`
  - `PHASE 205 COMPLETE`

## Exit Criteria
- Assembly-runtime bundle-factory-input session routing ownership is extracted into a focused factory component.
- Focused extraction tests and phase verification guard pack pass.
- `PHASE 205 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
