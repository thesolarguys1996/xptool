# Native Client Phase 139 Interaction Session Factory Runtime Bundle Factory Inputs Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning interaction-session runtime-bundle-factory ownership by extracting typed runtime-bundle-factory-input construction into focused `InteractionSessionFactoryRuntimeBundleFactoryInputsFactory` ownership.

## Execution Slices
1. `139.1` Define Phase 139 scope, artifacts, and completion gates.
2. `139.2` Extract focused runtime-bundle-factory-input construction factory ownership.
3. `139.3` Run Phase 139 verification + guard pack and record `PHASE 139 COMPLETE`.

## Phase 139 Slice Status
- `139.1` complete.
- `139.2` complete.
- `139.3` complete.

## Phase 139.1 Outputs
- Added dedicated Phase 139 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE139_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 139 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 139.2 Outputs
- Added focused runtime-bundle-factory-input construction ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsFactory.java`
- Added focused runtime-bundle-factory-input construction coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsFactoryTest.java`
- Routed assembly-input mapping helper through focused runtime-bundle-factory-input construction ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory.java`

## Phase 139.3 Outputs
- Added explicit Phase 139 verification script:
  - `scripts/verify_phase139_interaction_session_factory_runtime_bundle_factory_inputs_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase139_interaction_session_factory_runtime_bundle_factory_inputs_factory_extraction.py`
  - `python scripts/verify_phase138_interaction_session_macro_pass_u_signoff.py`
  - `python scripts/verify_phase137_interaction_session_factory_wiring_consolidation_u.py`
- Verified Java extraction tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleFactoryInputsFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleFactoryTest`
- Recorded completion markers:
  - `PHASE 139 STARTED`
  - `PHASE 139 COMPLETE`

## Exit Criteria
- Focused runtime-bundle-factory-input construction ownership exists and is used by assembly mapping seams.
- Phase 139 verifier and guard/test pack pass.
- `PHASE 139 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
