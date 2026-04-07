# Native Client Phase 212 Interaction Session Factory Assembly Runtime Entry Bundle Factory Input Typed Routing Extraction Plan

Last updated: 2026-04-07

## Goal
Route interaction-session assembly-runtime entry bundle-factory-input typed seams through focused assembly-runtime entry bundle-factory-input session factory ownership.

## Execution Slices
1. `212.1` Define Phase 212 scope, artifacts, and completion gates.
2. `212.2` Consolidate `InteractionSessionFactory.createFromRuntimeBundleFactoryInputs(...)` through focused assembly-runtime entry bundle-factory-input session routing ownership.
3. `212.3` Run Phase 212 verification + guard pack and record `PHASE 212 COMPLETE`.

## Phase 212 Slice Status
- `212.1` complete.
- `212.2` complete.
- `212.3` complete.

## Phase 212.1 Outputs
- Added dedicated Phase 212 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE212_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_ENTRY_BUNDLE_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 212 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 212.2 Outputs
- Consolidated assembly-runtime entry bundle-factory-input typed routing seams through focused ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 212.3 Outputs
- Added explicit Phase 212 verification script:
  - `scripts/verify_phase212_interaction_session_factory_assembly_runtime_entry_bundle_factory_input_typed_routing_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase212_interaction_session_factory_assembly_runtime_entry_bundle_factory_input_typed_routing_extraction.py`
  - `python scripts/verify_phase211_interaction_session_factory_assembly_runtime_entry_bundle_factory_inputs_session_factory_extraction.py`
  - `python scripts/verify_phase210_interaction_session_factory_assembly_runtime_entry_assembly_typed_routing_extraction.py`
- Verified Java consolidation tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryAssemblyRuntimeEntryBundleFactoryInputsSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryAssemblyRuntimeSessionFactoryTest`
- Recorded completion markers:
  - `PHASE 212 STARTED`
  - `PHASE 212 COMPLETE`

## Exit Criteria
- Assembly-runtime entry bundle-factory-input typed routing ownership in `InteractionSessionFactory` delegates through focused ownership.
- Compatibility sentinel strings required by prior phase verifier gates remain preserved.
- `PHASE 212 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
