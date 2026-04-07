# Native Client Phase 107 Interaction Session Assembly Factory Inputs Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning interaction-session assembly ownership by extracting command/session/facade/session-key positional arguments into focused assembly-factory typed inputs ownership.

## Execution Slices
1. `107.1` Define Phase 107 scope, artifacts, and completion gates.
2. `107.2` Extract focused assembly-factory typed inputs contract ownership.
3. `107.3` Run Phase 107 verification + guard pack and record `PHASE 107 COMPLETE`.

## Phase 107 Slice Status
- `107.1` complete.
- `107.2` complete.
- `107.3` complete.

## Phase 107.1 Outputs
- Added dedicated Phase 107 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE107_INTERACTION_SESSION_ASSEMBLY_FACTORY_INPUTS_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 107 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 107.2 Outputs
- Added focused assembly-factory typed inputs contract:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionAssemblyFactoryInputs.java`
- Added focused assembly-factory typed inputs coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionAssemblyFactoryInputsTest.java`

## Phase 107.3 Outputs
- Added explicit Phase 107 verification script:
  - `scripts/verify_phase107_interaction_session_assembly_factory_inputs_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase107_interaction_session_assembly_factory_inputs_extraction.py`
  - `python scripts/verify_phase106_interaction_session_macro_pass_n_signoff.py`
  - `python scripts/verify_phase105_interaction_session_assembly_wiring_consolidation_n.py`
- Verified Java tests for the extraction wave:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionAssemblyFactoryInputsTest --tests com.xptool.sessions.InteractionSessionAssemblyFactoryTest`
- Recorded completion markers:
  - `PHASE 107 STARTED`
  - `PHASE 107 COMPLETE`

## Exit Criteria
- Assembly-factory typed-input ownership is represented by focused `InteractionSessionAssemblyFactoryInputs`.
- Assembly-factory typed-input tests validate field/session-key mapping.
- Phase 107 verification script and targeted guard/test pack both pass.
- `PHASE 107 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
