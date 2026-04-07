# Native Client Phase 120 Interaction Session Factory Runtime Bundle Typed Entry Extraction Plan

Last updated: 2026-04-06

## Goal
Continue interaction-session factory decomposition by extracting typed-entry runtime-bundle creation seams through `InteractionSessionFactoryRuntimeBundleFactory` ownership.

## Execution Slices
1. `120.1` Define Phase 120 scope, artifacts, and completion gates.
2. `120.2` Extract typed-entry runtime-bundle creation seam in `InteractionSessionFactory` ownership.
3. `120.3` Run Phase 120 verification + guard pack and record `PHASE 120 COMPLETE`.

## Phase 120 Slice Status
- `120.1` complete.
- `120.2` complete.
- `120.3` complete.

## Phase 120.1 Outputs
- Added dedicated Phase 120 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE120_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_TYPED_ENTRY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 120 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 120.2 Outputs
- Added typed-entry runtime-bundle creation seam routing in interaction-session factory:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 120.3 Outputs
- Added explicit Phase 120 verification script:
  - `scripts/verify_phase120_interaction_session_factory_runtime_bundle_typed_entry_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase120_interaction_session_factory_runtime_bundle_typed_entry_extraction.py`
  - `python scripts/verify_phase119_interaction_session_factory_runtime_bundle_factory_extraction.py`
  - `python scripts/verify_phase118_interaction_session_macro_pass_p_signoff.py`
- Verified Java typed-entry tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleFactoryTest`
- Recorded completion markers:
  - `PHASE 120 STARTED`
  - `PHASE 120 COMPLETE`

## Exit Criteria
- `InteractionSessionFactory` routes typed-entry runtime-bundle creation via `InteractionSessionFactoryRuntimeBundleFactory`.
- Phase 120 verifier and guard/test pack pass.
- `PHASE 120 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
