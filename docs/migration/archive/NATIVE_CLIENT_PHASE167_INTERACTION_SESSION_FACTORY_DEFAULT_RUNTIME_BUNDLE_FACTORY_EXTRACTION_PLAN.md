# Native Client Phase 167 Interaction Session Factory Default Runtime Bundle Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning interaction-session default-runtime-session ownership by extracting default runtime-bundle creation into focused `InteractionSessionFactoryDefaultRuntimeBundleFactory` ownership.

## Execution Slices
1. `167.1` Define Phase 167 scope, artifacts, and completion gates.
2. `167.2` Extract focused default runtime-bundle factory ownership.
3. `167.3` Run Phase 167 verification + guard pack and record `PHASE 167 COMPLETE`.

## Phase 167 Slice Status
- `167.1` complete.
- `167.2` complete.
- `167.3` complete.

## Phase 167.1 Outputs
- Added dedicated Phase 167 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE167_INTERACTION_SESSION_FACTORY_DEFAULT_RUNTIME_BUNDLE_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 167 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 167.2 Outputs
- Added focused default runtime-bundle factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactory.java`
- Added focused default runtime-bundle factory coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactoryTest.java`

## Phase 167.3 Outputs
- Added explicit Phase 167 verification script:
  - `scripts/verify_phase167_interaction_session_factory_default_runtime_bundle_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase167_interaction_session_factory_default_runtime_bundle_factory_extraction.py`
  - `python scripts/verify_phase166_interaction_session_macro_pass_ab_signoff.py`
  - `python scripts/verify_phase165_interaction_session_factory_default_entry_wiring_consolidation_ab.py`
- Verified Java extraction tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryDefaultRuntimeBundleFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryDefaultRuntimeSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryDefaultEntryFactoryTest`
- Recorded completion markers:
  - `PHASE 167 STARTED`
  - `PHASE 167 COMPLETE`

## Exit Criteria
- Focused default runtime-bundle factory ownership exists.
- Phase 167 verifier and guard/test pack pass.
- `PHASE 167 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
