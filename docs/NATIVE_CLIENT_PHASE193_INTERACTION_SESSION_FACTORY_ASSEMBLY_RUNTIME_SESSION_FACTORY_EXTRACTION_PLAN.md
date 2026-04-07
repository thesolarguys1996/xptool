# Native Client Phase 193 Interaction Session Factory Assembly Runtime Session Factory Extraction Plan

Last updated: 2026-04-07

## Goal
Continue thinning interaction-session factory ownership by extracting assembly/runtime session routing into focused ownership.

## Execution Slices
1. `193.1` Define Phase 193 scope, artifacts, and completion gates.
2. `193.2` Extract `InteractionSessionFactoryAssemblyRuntimeSessionFactory` ownership for assembly/runtime session routing seams.
3. `193.3` Run Phase 193 verification + guard pack and record `PHASE 193 COMPLETE`.

## Phase 193 Slice Status
- `193.1` complete.
- `193.2` complete.
- `193.3` complete.

## Phase 193.1 Outputs
- Added dedicated Phase 193 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE193_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_SESSION_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 193 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 193.2 Outputs
- Added focused assembly/runtime session factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeSessionFactory.java`
- Added focused assembly/runtime session factory coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeSessionFactoryTest.java`

## Phase 193.3 Outputs
- Added explicit Phase 193 verification script:
  - `scripts/verify_phase193_interaction_session_factory_assembly_runtime_session_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase193_interaction_session_factory_assembly_runtime_session_factory_extraction.py`
  - `python scripts/verify_phase192_interaction_session_factory_factory_input_typed_routing_extraction.py`
  - `python scripts/verify_phase191_interaction_session_factory_factory_inputs_session_factory_extraction.py`
- Verified Java extraction tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryAssemblyRuntimeSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryAssemblyFactoryInputsSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactoryTest`
- Recorded completion markers:
  - `PHASE 193 STARTED`
  - `PHASE 193 COMPLETE`

## Exit Criteria
- Assembly/runtime session routing ownership is extracted into a focused factory component.
- Focused extraction tests and phase verification guard pack pass.
- `PHASE 193 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
