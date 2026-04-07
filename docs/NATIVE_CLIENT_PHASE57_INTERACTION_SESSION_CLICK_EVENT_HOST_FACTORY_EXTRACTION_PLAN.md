# Native Client Phase 57 Interaction Session Click-Event Host Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning `InteractionSessionHostFactory` by extracting click-event host assembly into focused `InteractionSessionClickEventFactory` ownership while preserving legacy compatibility delegate wrappers.

## Execution Slices
1. `57.1` Define Phase 57 scope, artifacts, and completion gates.
2. `57.2` Extract click-event host assembly from `InteractionSessionHostFactory` into focused factory ownership.
3. `57.3` Run Phase 57 verification + guard pack and record `PHASE 57 COMPLETE`.

## Phase 57 Slice Status
- `57.1` complete.
- `57.2` complete.
- `57.3` complete.

## Phase 57.1 Outputs
- Added dedicated Phase 57 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE57_INTERACTION_SESSION_CLICK_EVENT_HOST_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 57 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 57.2 Outputs
- Expanded focused click-event factory ownership for host assembly:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionClickEventFactory.java`
- Updated `InteractionSessionHostFactory` click-event host assembly to route through focused factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- Added focused click-event host factory regression coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventFactoryTest.java`

## Phase 57.3 Outputs
- Added explicit Phase 57 verification script:
  - `scripts/verify_phase57_interaction_session_click_event_host_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase57_interaction_session_click_event_host_factory_extraction.py`
  - `python scripts/verify_phase56_interaction_session_command_router_factory_extraction.py`
  - `python scripts/verify_phase55_interaction_session_macro_pass_signoff.py`
  - `python scripts/verify_phase54_interaction_session_host_factory_consolidation.py`
  - `python scripts/verify_phase53_interaction_session_ownership_factory_extraction.py`
  - `python scripts/verify_phase52_interaction_session_post_click_settle_factory_extraction.py`
  - `python scripts/verify_phase51_interaction_session_click_event_factory_extraction.py`
- Verified Java tests for the extraction wave:
  - `.\gradlew.bat test --tests com.xptool.sessions.InteractionSessionClickEventFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryClickEventHostTest --tests com.xptool.sessions.InteractionSessionClickEventServiceTest`
- Recorded completion markers:
  - `PHASE 57 STARTED`
  - `PHASE 57 COMPLETE`

## Exit Criteria
- `InteractionSessionHostFactory` click-event host assembly routes through focused `InteractionSessionClickEventFactory` ownership.
- `InteractionSessionClickEventFactory` provides click-event host and service construction boundaries with focused regression tests.
- Legacy compatibility delegate wrapper remains available in `InteractionSessionHostFactory` for prior-phase verifier compatibility.
- Phase 57 verification script and targeted guard/test pack both pass.
- `PHASE 57 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
