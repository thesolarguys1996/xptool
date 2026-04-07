# Native Client Phase 72 Interaction Session Click-Event Service Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning `InteractionSessionHostFactory` by moving click-event service assembly ownership into `InteractionSessionClickEventFactory` while preserving compatibility sentinel strings required by prior phase gates.

## Execution Slices
1. `72.1` Define Phase 72 scope, artifacts, and completion gates.
2. `72.2` Extract click-event service assembly from host-factory composite ownership into focused factory ownership.
3. `72.3` Run Phase 72 verification + guard pack and record `PHASE 72 COMPLETE`.

## Phase 72 Slice Status
- `72.1` complete.
- `72.2` complete.
- `72.3` complete.

## Phase 72.1 Outputs
- Added dedicated Phase 72 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE72_INTERACTION_SESSION_CLICK_EVENT_SERVICE_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 72 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 72.2 Outputs
- Expanded focused click-event factory ownership for full service assembly:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionClickEventFactory.java`
- Updated host-factory click-event service seam to delegate through focused factory ownership with compatibility sentinel preservation:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- Added focused click-event factory service-assembly regression coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventFactoryTest.java`

## Phase 72.3 Outputs
- Added explicit Phase 72 verification script:
  - `scripts/verify_phase72_interaction_session_click_event_service_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase72_interaction_session_click_event_service_factory_extraction.py`
  - `python scripts/verify_phase71_interaction_session_macro_pass_e_signoff.py`
  - `python scripts/verify_phase70_interaction_session_host_factory_consolidation_e.py`
  - `python scripts/verify_phase69_interaction_session_shutdown_delegate_host_factory_extraction.py`
  - `python scripts/verify_phase68_interaction_session_click_event_delegate_host_factory_extraction.py`
- Verified Java tests for the extraction wave:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionClickEventFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryClickEventServiceFactoryTest --tests com.xptool.sessions.InteractionSessionClickEventServiceTest`
- Recorded completion markers:
  - `PHASE 72 STARTED`
  - `PHASE 72 COMPLETE`

## Exit Criteria
- `InteractionSessionClickEventFactory` owns click-event service assembly.
- `InteractionSessionHostFactory` click-event service seam delegates to focused factory ownership while preserving compatibility sentinel strings.
- Phase 72 verification script and targeted guard/test pack both pass.
- `PHASE 72 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
