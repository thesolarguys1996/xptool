# Native Client Phase 206 Interaction Session Factory Assembly Runtime Bundle Factory Input Typed Routing Extraction Plan

Last updated: 2026-04-07

## Goal
Route interaction-session assembly-runtime bundle-factory-input typed seams through focused assembly-runtime bundle-factory-input session factory ownership.

## Execution Slices
1. `206.1` Define Phase 206 scope, artifacts, and completion gates.
2. `206.2` Consolidate `InteractionSessionFactoryAssemblyRuntimeSessionFactory.createFromRuntimeBundleFactoryInputs(...)` through focused assembly-runtime bundle-factory-input session routing ownership.
3. `206.3` Run Phase 206 verification + guard pack and record `PHASE 206 COMPLETE`.

## Phase 206 Slice Status
- `206.1` complete.
- `206.2` complete.
- `206.3` complete.

## Phase 206.1 Outputs
- Added dedicated Phase 206 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE206_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_BUNDLE_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 206 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 206.2 Outputs
- Consolidated assembly-runtime bundle-factory-input typed routing seams through focused ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeSessionFactory.java`

## Phase 206.3 Outputs
- Added explicit Phase 206 verification script:
  - `scripts/verify_phase206_interaction_session_factory_assembly_runtime_bundle_factory_input_typed_routing_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase206_interaction_session_factory_assembly_runtime_bundle_factory_input_typed_routing_extraction.py`
  - `python scripts/verify_phase205_interaction_session_factory_assembly_runtime_bundle_factory_inputs_session_factory_extraction.py`
  - `python scripts/verify_phase204_interaction_session_factory_assembly_runtime_assembly_typed_routing_extraction.py`
- Verified Java consolidation tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryAssemblyRuntimeSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactoryTest`
- Recorded completion markers:
  - `PHASE 206 STARTED`
  - `PHASE 206 COMPLETE`

## Exit Criteria
- Assembly-runtime bundle-factory-input typed routing ownership in `InteractionSessionFactoryAssemblyRuntimeSessionFactory` delegates through focused ownership.
- Compatibility sentinel strings required by prior phase verifier gates remain preserved.
- `PHASE 206 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
