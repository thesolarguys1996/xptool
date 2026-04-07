# Native Client Phase 210 Interaction Session Factory Assembly Runtime Entry Assembly Typed Routing Extraction Plan

Last updated: 2026-04-07

## Goal
Route interaction-session assembly-runtime entry assembly typed seams through focused assembly-runtime entry assembly session factory ownership.

## Execution Slices
1. `210.1` Define Phase 210 scope, artifacts, and completion gates.
2. `210.2` Consolidate `InteractionSessionFactory.createFromAssemblyFactoryInputs(...)` through focused assembly-runtime entry assembly session routing ownership.
3. `210.3` Run Phase 210 verification + guard pack and record `PHASE 210 COMPLETE`.

## Phase 210 Slice Status
- `210.1` complete.
- `210.2` complete.
- `210.3` complete.

## Phase 210.1 Outputs
- Added dedicated Phase 210 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE210_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_ENTRY_ASSEMBLY_TYPED_ROUTING_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 210 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 210.2 Outputs
- Consolidated assembly-runtime entry assembly typed routing seams through focused ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 210.3 Outputs
- Added explicit Phase 210 verification script:
  - `scripts/verify_phase210_interaction_session_factory_assembly_runtime_entry_assembly_typed_routing_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase210_interaction_session_factory_assembly_runtime_entry_assembly_typed_routing_extraction.py`
  - `python scripts/verify_phase209_interaction_session_factory_assembly_runtime_entry_assembly_session_factory_extraction.py`
  - `python scripts/verify_phase208_interaction_session_macro_pass_ai_signoff.py`
- Verified Java consolidation tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryAssemblyRuntimeEntryAssemblySessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryAssemblyRuntimeSessionFactoryTest`
- Recorded completion markers:
  - `PHASE 210 STARTED`
  - `PHASE 210 COMPLETE`

## Exit Criteria
- Assembly-runtime entry assembly typed routing ownership in `InteractionSessionFactory` delegates through focused ownership.
- Compatibility sentinel strings required by prior phase verifier gates remain preserved.
- `PHASE 210 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
