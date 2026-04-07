# Native Client Phase 94 Interaction Session Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning executor wiring/session construction ownership by extracting interaction-session creation into focused `InteractionSessionFactory` boundaries.

## Execution Slices
1. `94.1` Define Phase 94 scope, artifacts, and completion gates.
2. `94.2` Extract interaction-session construction ownership into focused session factory boundary.
3. `94.3` Run Phase 94 verification + guard pack and record `PHASE 94 COMPLETE`.

## Phase 94 Slice Status
- `94.1` complete.
- `94.2` complete.
- `94.3` complete.

## Phase 94.1 Outputs
- Added dedicated Phase 94 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE94_INTERACTION_SESSION_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 94 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 94.2 Outputs
- Added focused interaction-session factory boundary:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`
- Added focused interaction-session factory delegation coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryTest.java`

## Phase 94.3 Outputs
- Added explicit Phase 94 verification script:
  - `scripts/verify_phase94_interaction_session_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase94_interaction_session_factory_extraction.py`
  - `python scripts/verify_phase93_interaction_session_constructor_runtime_bundle_extraction.py`
  - `python scripts/verify_phase92_interaction_session_macro_pass_k_signoff.py`
- Verified Java tests for the extraction wave:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryTest --tests com.xptool.sessions.InteractionSessionAssemblyFactoryTest --tests com.xptool.sessions.InteractionSessionRuntimeBundleFactoryTest`
- Recorded completion markers:
  - `PHASE 94 STARTED`
  - `PHASE 94 COMPLETE`

## Exit Criteria
- `InteractionSessionFactory` owns interaction-session construction boundary.
- Phase 94 verification script and targeted guard/test pack both pass.
- `PHASE 94 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
