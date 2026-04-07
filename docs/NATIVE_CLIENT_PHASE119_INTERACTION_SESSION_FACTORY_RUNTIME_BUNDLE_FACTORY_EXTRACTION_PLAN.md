# Native Client Phase 119 Interaction Session Factory Runtime Bundle Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning interaction-session factory ownership by extracting runtime-bundle routing seams into focused `InteractionSessionFactoryRuntimeBundleFactory` ownership.

## Execution Slices
1. `119.1` Define Phase 119 scope, artifacts, and completion gates.
2. `119.2` Extract focused interaction-session factory runtime-bundle routing ownership.
3. `119.3` Run Phase 119 verification + guard pack and record `PHASE 119 COMPLETE`.

## Phase 119 Slice Status
- `119.1` complete.
- `119.2` complete.
- `119.3` complete.

## Phase 119.1 Outputs
- Added dedicated Phase 119 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE119_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 119 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 119.2 Outputs
- Added focused interaction-session factory runtime-bundle routing ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java`
- Added focused runtime-bundle routing ownership coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryTest.java`

## Phase 119.3 Outputs
- Added explicit Phase 119 verification script:
  - `scripts/verify_phase119_interaction_session_factory_runtime_bundle_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase119_interaction_session_factory_runtime_bundle_factory_extraction.py`
  - `python scripts/verify_phase118_interaction_session_macro_pass_p_signoff.py`
  - `python scripts/verify_phase117_interaction_session_factory_wiring_consolidation_p.py`
- Verified Java extraction tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryInputsTest`
- Recorded completion markers:
  - `PHASE 119 STARTED`
  - `PHASE 119 COMPLETE`

## Exit Criteria
- Focused runtime-bundle routing ownership exists in `InteractionSessionFactoryRuntimeBundleFactory`.
- Phase 119 verifier and guard/test pack pass.
- `PHASE 119 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
