# Native Client Phase 203 Interaction Session Factory Assembly Runtime Assembly Session Factory Extraction Plan

Last updated: 2026-04-07

## Goal
Continue thinning interaction-session assembly-runtime ownership by extracting assembly session routing into focused ownership.

## Execution Slices
1. `203.1` Define Phase 203 scope, artifacts, and completion gates.
2. `203.2` Extract `InteractionSessionFactoryAssemblyRuntimeAssemblySessionFactory` ownership for assembly session routing seams.
3. `203.3` Run Phase 203 verification + guard pack and record `PHASE 203 COMPLETE`.

## Phase 203 Slice Status
- `203.1` complete.
- `203.2` complete.
- `203.3` complete.

## Phase 203.1 Outputs
- Added dedicated Phase 203 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE203_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_ASSEMBLY_SESSION_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 203 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 203.2 Outputs
- Added focused assembly-runtime assembly session factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeAssemblySessionFactory.java`
- Added focused assembly-runtime assembly session factory coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeAssemblySessionFactoryTest.java`

## Phase 203.3 Outputs
- Added explicit Phase 203 verification script:
  - `scripts/verify_phase203_interaction_session_factory_assembly_runtime_assembly_session_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase203_interaction_session_factory_assembly_runtime_assembly_session_factory_extraction.py`
  - `python scripts/verify_phase202_interaction_session_macro_pass_ah_signoff.py`
  - `python scripts/verify_phase201_interaction_session_factory_factory_inputs_default_session_factory_extraction.py`
- Verified Java extraction tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryAssemblyRuntimeAssemblySessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryAssemblyRuntimeSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryAssemblyFactoryInputsSessionFactoryTest`
- Recorded completion markers:
  - `PHASE 203 STARTED`
  - `PHASE 203 COMPLETE`

## Exit Criteria
- Assembly-runtime assembly session routing ownership is extracted into a focused factory component.
- Focused extraction tests and phase verification guard pack pass.
- `PHASE 203 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
