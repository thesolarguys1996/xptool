# Native Client Phase 207 Interaction Session Factory Runtime Entry Runtime Session Factory Extraction Plan

Last updated: 2026-04-07

## Goal
Continue thinning interaction-session runtime-entry ownership by extracting runtime session routing into focused ownership.

## Execution Slices
1. `207.1` Define Phase 207 scope, artifacts, and completion gates.
2. `207.2` Extract `InteractionSessionFactoryRuntimeEntryRuntimeSessionFactory` ownership and route runtime-entry seams through focused ownership.
3. `207.3` Run Phase 207 verification + guard pack and record `PHASE 207 COMPLETE`.

## Phase 207 Slice Status
- `207.1` complete.
- `207.2` complete.
- `207.3` complete.

## Phase 207.1 Outputs
- Added dedicated Phase 207 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE207_INTERACTION_SESSION_FACTORY_RUNTIME_ENTRY_RUNTIME_SESSION_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 207 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 207.2 Outputs
- Added focused runtime-entry runtime session factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeEntryRuntimeSessionFactory.java`
- Consolidated runtime-entry seams through focused ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeEntrySessionFactory.java`
- Added focused runtime-entry runtime session factory coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeEntryRuntimeSessionFactoryTest.java`

## Phase 207.3 Outputs
- Added explicit Phase 207 verification script:
  - `scripts/verify_phase207_interaction_session_factory_runtime_entry_runtime_session_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase207_interaction_session_factory_runtime_entry_runtime_session_factory_extraction.py`
  - `python scripts/verify_phase206_interaction_session_factory_assembly_runtime_bundle_factory_input_typed_routing_extraction.py`
  - `python scripts/verify_phase205_interaction_session_factory_assembly_runtime_bundle_factory_inputs_session_factory_extraction.py`
- Verified Java extraction/consolidation tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryRuntimeEntryRuntimeSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryRuntimeEntrySessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleSessionFactoryTest`
- Recorded completion markers:
  - `PHASE 207 STARTED`
  - `PHASE 207 COMPLETE`

## Exit Criteria
- Runtime-entry runtime session routing ownership is extracted into a focused factory component.
- Focused extraction/consolidation tests and phase verification guard pack pass.
- `PHASE 207 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
