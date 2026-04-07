# Native Client Phase 98 Interaction Session Runtime Operations Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning interaction-session runtime assembly ownership by extracting focused runtime-operations factory construction boundaries.

## Execution Slices
1. `98.1` Define Phase 98 scope, artifacts, and completion gates.
2. `98.2` Extract runtime-operations construction ownership into focused `InteractionSessionRuntimeOperationsFactory`.
3. `98.3` Run Phase 98 verification + guard pack and record `PHASE 98 COMPLETE`.

## Phase 98 Slice Status
- `98.1` complete.
- `98.2` complete.
- `98.3` complete.

## Phase 98.1 Outputs
- Added dedicated Phase 98 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE98_INTERACTION_SESSION_RUNTIME_OPERATIONS_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 98 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 98.2 Outputs
- Added focused runtime-operations factory ownership boundary:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeOperationsFactory.java`
- Added focused runtime-operations factory delegation coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRuntimeOperationsFactoryTest.java`

## Phase 98.3 Outputs
- Added explicit Phase 98 verification script:
  - `scripts/verify_phase98_interaction_session_runtime_operations_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase98_interaction_session_runtime_operations_factory_extraction.py`
  - `python scripts/verify_phase97_interaction_session_runtime_operations_extraction.py`
  - `python scripts/verify_phase96_interaction_session_macro_pass_l_signoff.py`
- Verified Java tests for the extraction wave:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionRuntimeOperationsFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryTest --tests com.xptool.sessions.InteractionSessionRuntimeBundleFactoryTest`
- Recorded completion markers:
  - `PHASE 98 STARTED`
  - `PHASE 98 COMPLETE`

## Exit Criteria
- `InteractionSessionRuntimeOperationsFactory` owns runtime-operations construction entrypoints.
- Phase 98 verification script and targeted guard/test pack both pass.
- `PHASE 98 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
