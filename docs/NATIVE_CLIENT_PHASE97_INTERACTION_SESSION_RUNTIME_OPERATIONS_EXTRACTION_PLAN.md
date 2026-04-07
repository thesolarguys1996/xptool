# Native Client Phase 97 Interaction Session Runtime Operations Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning `InteractionSession` runtime ownership by extracting command/click/tick/shutdown delegation into a focused runtime-operations boundary.

## Execution Slices
1. `97.1` Define Phase 97 scope, artifacts, and completion gates.
2. `97.2` Extract interaction-session runtime-operation delegation ownership into focused runtime-operations boundary.
3. `97.3` Run Phase 97 verification + guard pack and record `PHASE 97 COMPLETE`.

## Phase 97 Slice Status
- `97.1` complete.
- `97.2` complete.
- `97.3` complete.

## Phase 97.1 Outputs
- Added dedicated Phase 97 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE97_INTERACTION_SESSION_RUNTIME_OPERATIONS_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 97 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 97.2 Outputs
- Added focused runtime-operations ownership boundary for interaction-session delegation:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeOperations.java`
- Updated interaction-session to delegate runtime behavior through focused runtime-operations ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`

## Phase 97.3 Outputs
- Added explicit Phase 97 verification script:
  - `scripts/verify_phase97_interaction_session_runtime_operations_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase97_interaction_session_runtime_operations_extraction.py`
  - `python scripts/verify_phase96_interaction_session_macro_pass_l_signoff.py`
  - `python scripts/verify_phase95_interaction_session_wiring_consolidation_l.py`
- Verified Java tests for the extraction wave:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryTest --tests com.xptool.sessions.InteractionSessionRuntimeOperationsFactoryTest --tests com.xptool.sessions.InteractionSessionRuntimeBundleFactoryTest`
- Recorded completion markers:
  - `PHASE 97 STARTED`
  - `PHASE 97 COMPLETE`

## Exit Criteria
- `InteractionSession` runtime delegation is owned by focused `InteractionSessionRuntimeOperations`.
- Phase 97 verification script and targeted guard/test pack both pass.
- `PHASE 97 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
