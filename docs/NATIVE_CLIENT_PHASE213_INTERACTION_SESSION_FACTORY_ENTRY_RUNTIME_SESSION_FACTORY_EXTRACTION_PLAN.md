# Native Client Phase 213 Interaction Session Factory Entry Runtime Session Factory Extraction Plan

Last updated: 2026-04-07

## Goal
Continue thinning interaction-session factory entry runtime ownership by extracting runtime session routing into focused ownership.

## Execution Slices
1. `213.1` Define Phase 213 scope, artifacts, and completion gates.
2. `213.2` Extract `InteractionSessionFactoryEntryRuntimeSessionFactory` ownership and route entry runtime seams through focused ownership.
3. `213.3` Run Phase 213 verification + guard pack and record `PHASE 213 COMPLETE`.

## Phase 213 Slice Status
- `213.1` complete.
- `213.2` complete.
- `213.3` complete.

## Phase 213.1 Outputs
- Added dedicated Phase 213 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE213_INTERACTION_SESSION_FACTORY_ENTRY_RUNTIME_SESSION_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 213 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 213.2 Outputs
- Added focused entry runtime session factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntryRuntimeSessionFactory.java`
- Consolidated entry runtime seams through focused ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`
- Added focused entry runtime session factory coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryEntryRuntimeSessionFactoryTest.java`

## Phase 213.3 Outputs
- Added explicit Phase 213 verification script:
  - `scripts/verify_phase213_interaction_session_factory_entry_runtime_session_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase213_interaction_session_factory_entry_runtime_session_factory_extraction.py`
  - `python scripts/verify_phase212_interaction_session_factory_assembly_runtime_entry_bundle_factory_input_typed_routing_extraction.py`
  - `python scripts/verify_phase211_interaction_session_factory_assembly_runtime_entry_bundle_factory_inputs_session_factory_extraction.py`
- Verified Java extraction/consolidation tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryEntryRuntimeSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryRuntimeEntrySessionFactoryTest`
- Recorded completion markers:
  - `PHASE 213 STARTED`
  - `PHASE 213 COMPLETE`

## Exit Criteria
- Entry runtime session routing ownership is extracted into a focused factory component.
- Focused extraction/consolidation tests and phase verification guard pack pass.
- `PHASE 213 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
