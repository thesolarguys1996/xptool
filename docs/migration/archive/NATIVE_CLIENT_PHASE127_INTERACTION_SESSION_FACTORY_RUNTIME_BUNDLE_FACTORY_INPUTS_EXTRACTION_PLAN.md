# Native Client Phase 127 Interaction Session Factory Runtime Bundle Factory Inputs Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning interaction-session factory runtime-bundle ownership by extracting typed runtime-bundle-factory input ownership into `InteractionSessionFactoryRuntimeBundleFactoryInputs`.

## Execution Slices
1. `127.1` Define Phase 127 scope, artifacts, and completion gates.
2. `127.2` Extract focused runtime-bundle-factory typed input ownership.
3. `127.3` Run Phase 127 verification + guard pack and record `PHASE 127 COMPLETE`.

## Phase 127 Slice Status
- `127.1` complete.
- `127.2` complete.
- `127.3` complete.

## Phase 127.1 Outputs
- Added dedicated Phase 127 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE127_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_INPUTS_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 127 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 127.2 Outputs
- Added focused runtime-bundle-factory typed input ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputs.java`
- Added focused runtime-bundle-factory typed input coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsTest.java`

## Phase 127.3 Outputs
- Added explicit Phase 127 verification script:
  - `scripts/verify_phase127_interaction_session_factory_runtime_bundle_factory_inputs_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase127_interaction_session_factory_runtime_bundle_factory_inputs_extraction.py`
  - `python scripts/verify_phase126_interaction_session_macro_pass_r_signoff.py`
  - `python scripts/verify_phase125_interaction_session_factory_wiring_consolidation_r.py`
- Verified Java extraction tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleFactoryInputsTest --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryTest`
- Recorded completion markers:
  - `PHASE 127 STARTED`
  - `PHASE 127 COMPLETE`

## Exit Criteria
- Focused typed runtime-bundle-factory input ownership exists and is used for runtime-bundle input mapping.
- Phase 127 verifier and guard/test pack pass.
- `PHASE 127 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
