# Native Client Phase 117 Interaction Session Factory Wiring Consolidation P Plan

Last updated: 2026-04-06

## Goal
Consolidate interaction-session factory wiring seams by routing public factory entry through typed factory-input ownership while preserving compatibility sentinels.

## Execution Slices
1. `117.1` Define Phase 117 scope, artifacts, and completion gates.
2. `117.2` Consolidate interaction-session factory public entry seam through typed factory-input ownership.
3. `117.3` Run Phase 117 verification + guard pack and record `PHASE 117 COMPLETE`.

## Phase 117 Slice Status
- `117.1` complete.
- `117.2` complete.
- `117.3` complete.

## Phase 117.1 Outputs
- Added dedicated Phase 117 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE117_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_P_PLAN.md`
- Updated migration/task/status artifacts with Phase 117 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 117.2 Outputs
- Consolidated interaction-session factory public creation seam through typed factory-input ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 117.3 Outputs
- Added explicit Phase 117 verification script:
  - `scripts/verify_phase117_interaction_session_factory_wiring_consolidation_p.py`
- Executed verification commands:
  - `python scripts/verify_phase117_interaction_session_factory_wiring_consolidation_p.py`
  - `python scripts/verify_phase116_interaction_session_factory_typed_entry_extraction.py`
  - `python scripts/verify_phase115_interaction_session_factory_inputs_extraction.py`
  - `python scripts/verify_phase110_interaction_session_macro_pass_o_signoff.py`
- Verified Java tests for the consolidation wave:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryInputsTest --tests com.xptool.sessions.InteractionSessionAssemblyFactoryInputsTest`
- Recorded completion markers:
  - `PHASE 117 STARTED`
  - `PHASE 117 COMPLETE`

## Exit Criteria
- Public interaction-session factory creation seam delegates through typed `InteractionSessionFactoryInputs`.
- Compatibility sentinel strings remain intact for prior verifier stability.
- Phase 117 verification script and targeted guard/test pack both pass.
- `PHASE 117 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
