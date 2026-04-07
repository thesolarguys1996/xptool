# Native Client Phase 170 Interaction Session Factory Default Entry Runtime Session Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning default-entry ownership by extracting default-entry runtime-session routing into focused `InteractionSessionFactoryDefaultEntryRuntimeSessionFactory` ownership.

## Execution Slices
1. `170.1` Define Phase 170 scope, artifacts, and completion gates.
2. `170.2` Extract focused default-entry runtime-session factory ownership.
3. `170.3` Run Phase 170 verification + guard pack and record `PHASE 170 COMPLETE`.

## Phase 170 Slice Status
- `170.1` complete.
- `170.2` complete.
- `170.3` complete.

## Phase 170.1 Outputs
- Added dedicated Phase 170 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE170_INTERACTION_SESSION_FACTORY_DEFAULT_ENTRY_RUNTIME_SESSION_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 170 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 170.2 Outputs
- Added focused default-entry runtime-session factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryRuntimeSessionFactory.java`
- Added focused default-entry runtime-session factory coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryRuntimeSessionFactoryTest.java`

## Phase 170.3 Outputs
- Added explicit Phase 170 verification script:
  - `scripts/verify_phase170_interaction_session_factory_default_entry_runtime_session_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase170_interaction_session_factory_default_entry_runtime_session_factory_extraction.py`
  - `python scripts/verify_phase169_interaction_session_factory_default_runtime_session_wiring_consolidation_ac.py`
  - `python scripts/verify_phase168_interaction_session_factory_default_runtime_bundle_typed_routing_extraction.py`
- Verified Java extraction tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryDefaultEntryRuntimeSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryDefaultEntryFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryDefaultRuntimeSessionFactoryTest`
- Recorded completion markers:
  - `PHASE 170 STARTED`
  - `PHASE 170 COMPLETE`

## Exit Criteria
- Focused default-entry runtime-session factory ownership exists.
- Phase 170 verifier and guard/test pack pass.
- `PHASE 170 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
