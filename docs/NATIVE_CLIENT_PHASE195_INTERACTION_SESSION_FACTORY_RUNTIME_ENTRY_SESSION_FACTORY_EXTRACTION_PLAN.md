# Native Client Phase 195 Interaction Session Factory Runtime Entry Session Factory Extraction Plan

Last updated: 2026-04-07

## Goal
Continue thinning interaction-session factory ownership by extracting runtime-entry session routing into focused ownership.

## Execution Slices
1. `195.1` Define Phase 195 scope, artifacts, and completion gates.
2. `195.2` Extract `InteractionSessionFactoryRuntimeEntrySessionFactory` ownership and route runtime-entry seams through focused ownership.
3. `195.3` Run Phase 195 verification + guard pack and record `PHASE 195 COMPLETE`.

## Phase 195 Slice Status
- `195.1` complete.
- `195.2` complete.
- `195.3` complete.

## Phase 195.1 Outputs
- Added dedicated Phase 195 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE195_INTERACTION_SESSION_FACTORY_RUNTIME_ENTRY_SESSION_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 195 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 195.2 Outputs
- Added focused runtime-entry session factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeEntrySessionFactory.java`
- Consolidated runtime-entry seams through focused ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`
- Added focused runtime-entry session factory coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeEntrySessionFactoryTest.java`

## Phase 195.3 Outputs
- Added explicit Phase 195 verification script:
  - `scripts/verify_phase195_interaction_session_factory_runtime_entry_session_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase195_interaction_session_factory_runtime_entry_session_factory_extraction.py`
  - `python scripts/verify_phase194_interaction_session_factory_assembly_runtime_typed_routing_extraction.py`
  - `python scripts/verify_phase193_interaction_session_factory_assembly_runtime_session_factory_extraction.py`
- Verified Java extraction/consolidation tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryRuntimeEntrySessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleSessionFactoryTest`
- Recorded completion markers:
  - `PHASE 195 STARTED`
  - `PHASE 195 COMPLETE`

## Exit Criteria
- Runtime-entry session routing ownership is extracted into a focused factory component.
- Focused extraction/consolidation tests and phase verification guard pack pass.
- `PHASE 195 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
