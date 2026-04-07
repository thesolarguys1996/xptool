# Native Client Phase 131 Interaction Session Factory Runtime Bundle Assembly Inputs Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning interaction-session runtime-bundle-factory ownership by extracting assembly-input construction seams into focused `InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory` ownership.

## Execution Slices
1. `131.1` Define Phase 131 scope, artifacts, and completion gates.
2. `131.2` Extract focused runtime-bundle assembly-input factory ownership.
3. `131.3` Run Phase 131 verification + guard pack and record `PHASE 131 COMPLETE`.

## Phase 131 Slice Status
- `131.1` complete.
- `131.2` complete.
- `131.3` complete.

## Phase 131.1 Outputs
- Added dedicated Phase 131 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE131_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_ASSEMBLY_INPUTS_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 131 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 131.2 Outputs
- Added focused runtime-bundle assembly-input factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory.java`
- Added focused runtime-bundle assembly-input factory coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleAssemblyInputsFactoryTest.java`
- Routed runtime-bundle-factory input assembly mapping through the focused assembly-input factory:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputs.java`

## Phase 131.3 Outputs
- Added explicit Phase 131 verification script:
  - `scripts/verify_phase131_interaction_session_factory_runtime_bundle_assembly_inputs_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase131_interaction_session_factory_runtime_bundle_assembly_inputs_factory_extraction.py`
  - `python scripts/verify_phase130_interaction_session_macro_pass_s_signoff.py`
  - `python scripts/verify_phase129_interaction_session_factory_wiring_consolidation_s.py`
- Verified Java extraction tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleAssemblyInputsFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleFactoryInputsTest --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleFactoryTest`
- Recorded completion markers:
  - `PHASE 131 STARTED`
  - `PHASE 131 COMPLETE`

## Exit Criteria
- Focused runtime-bundle assembly-input factory ownership exists and is used by runtime-bundle-factory inputs.
- Phase 131 verifier and guard/test pack pass.
- `PHASE 131 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
