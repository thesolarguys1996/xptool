# Native Client Phase 209 Interaction Session Factory Assembly Runtime Entry Assembly Session Factory Extraction Plan

Last updated: 2026-04-07

## Goal
Continue thinning interaction-session assembly-runtime entry ownership by extracting assembly session routing into focused ownership.

## Execution Slices
1. `209.1` Define Phase 209 scope, artifacts, and completion gates.
2. `209.2` Extract `InteractionSessionFactoryAssemblyRuntimeEntryAssemblySessionFactory` ownership for assembly session routing seams.
3. `209.3` Run Phase 209 verification + guard pack and record `PHASE 209 COMPLETE`.

## Phase 209 Slice Status
- `209.1` complete.
- `209.2` complete.
- `209.3` complete.

## Phase 209.1 Outputs
- Added dedicated Phase 209 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE209_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_ENTRY_ASSEMBLY_SESSION_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 209 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 209.2 Outputs
- Added focused assembly-runtime entry assembly session factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeEntryAssemblySessionFactory.java`
- Added focused assembly-runtime entry assembly session factory coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeEntryAssemblySessionFactoryTest.java`

## Phase 209.3 Outputs
- Added explicit Phase 209 verification script:
  - `scripts/verify_phase209_interaction_session_factory_assembly_runtime_entry_assembly_session_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase209_interaction_session_factory_assembly_runtime_entry_assembly_session_factory_extraction.py`
  - `python scripts/verify_phase208_interaction_session_macro_pass_ai_signoff.py`
  - `python scripts/verify_phase207_interaction_session_factory_runtime_entry_runtime_session_factory_extraction.py`
- Verified Java extraction tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryAssemblyRuntimeEntryAssemblySessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryAssemblyRuntimeSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryAssemblyRuntimeAssemblySessionFactoryTest`
- Recorded completion markers:
  - `PHASE 209 STARTED`
  - `PHASE 209 COMPLETE`

## Exit Criteria
- Assembly-runtime entry assembly session routing ownership is extracted into a focused factory component.
- Focused extraction tests and phase verification guard pack pass.
- `PHASE 209 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
