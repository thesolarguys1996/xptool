# Native Client Phase 161 Interaction Session Factory Default Runtime Bundle Factory Inputs Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning interaction-session factory default-entry ownership by extracting default runtime-bundle-factory-input construction into focused `InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory` ownership.

## Execution Slices
1. `161.1` Define Phase 161 scope, artifacts, and completion gates.
2. `161.2` Extract focused default runtime-bundle-factory-input construction ownership.
3. `161.3` Run Phase 161 verification + guard pack and record `PHASE 161 COMPLETE`.

## Phase 161 Slice Status
- `161.1` complete.
- `161.2` complete.
- `161.3` complete.

## Phase 161.1 Outputs
- Added dedicated Phase 161 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE161_INTERACTION_SESSION_FACTORY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 161 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 161.2 Outputs
- Added focused default runtime-bundle-factory-input construction ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory.java`
- Added focused default runtime-bundle-factory-input construction coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactoryTest.java`

## Phase 161.3 Outputs
- Added explicit Phase 161 verification script:
  - `scripts/verify_phase161_interaction_session_factory_default_runtime_bundle_factory_inputs_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase161_interaction_session_factory_default_runtime_bundle_factory_inputs_factory_extraction.py`
  - `python scripts/verify_phase160_interaction_session_macro_pass_z_signoff.py`
  - `python scripts/verify_phase159_interaction_session_factory_default_entry_wiring_consolidation_z.py`
- Verified Java extraction tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryDefaultEntryFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleFactoryTest`
- Recorded completion markers:
  - `PHASE 161 STARTED`
  - `PHASE 161 COMPLETE`

## Exit Criteria
- Focused default runtime-bundle-factory-input construction ownership exists.
- Phase 161 verifier and guard/test pack pass.
- `PHASE 161 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
