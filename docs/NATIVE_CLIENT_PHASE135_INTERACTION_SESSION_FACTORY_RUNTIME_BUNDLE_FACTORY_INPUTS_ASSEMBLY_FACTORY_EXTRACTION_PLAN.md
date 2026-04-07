# Native Client Phase 135 Interaction Session Factory Runtime Bundle Factory Inputs Assembly Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning interaction-session runtime-bundle-factory ownership by extracting assembly-input to runtime-bundle-factory-input mapping into focused `InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory` ownership.

## Execution Slices
1. `135.1` Define Phase 135 scope, artifacts, and completion gates.
2. `135.2` Extract focused runtime-bundle-factory-input assembly mapping ownership.
3. `135.3` Run Phase 135 verification + guard pack and record `PHASE 135 COMPLETE`.

## Phase 135 Slice Status
- `135.1` complete.
- `135.2` complete.
- `135.3` complete.

## Phase 135.1 Outputs
- Added dedicated Phase 135 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE135_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_INPUTS_ASSEMBLY_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 135 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 135.2 Outputs
- Added focused runtime-bundle-factory-input assembly mapping ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory.java`
- Added focused runtime-bundle-factory-input assembly mapping coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactoryTest.java`

## Phase 135.3 Outputs
- Added explicit Phase 135 verification script:
  - `scripts/verify_phase135_interaction_session_factory_runtime_bundle_factory_inputs_assembly_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase135_interaction_session_factory_runtime_bundle_factory_inputs_assembly_factory_extraction.py`
  - `python scripts/verify_phase134_interaction_session_macro_pass_t_signoff.py`
  - `python scripts/verify_phase133_interaction_session_factory_wiring_consolidation_t.py`
- Verified Java extraction tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryTest`
- Recorded completion markers:
  - `PHASE 135 STARTED`
  - `PHASE 135 COMPLETE`

## Exit Criteria
- Focused runtime-bundle-factory-input assembly mapping ownership exists.
- Phase 135 verifier and guard/test pack pass.
- `PHASE 135 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
