# Native Client Phase 73 Interaction Session Shutdown Service Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning `InteractionSessionHostFactory` by moving shutdown service assembly ownership into `InteractionSessionShutdownFactory` while preserving compatibility sentinel strings required by prior phase gates.

## Execution Slices
1. `73.1` Define Phase 73 scope, artifacts, and completion gates.
2. `73.2` Extract shutdown service assembly from host-factory composite ownership into focused factory ownership.
3. `73.3` Run Phase 73 verification + guard pack and record `PHASE 73 COMPLETE`.

## Phase 73 Slice Status
- `73.1` complete.
- `73.2` complete.
- `73.3` complete.

## Phase 73.1 Outputs
- Added dedicated Phase 73 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE73_INTERACTION_SESSION_SHUTDOWN_SERVICE_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 73 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 73.2 Outputs
- Updated host-factory shutdown service seam to delegate through focused factory ownership with compatibility sentinel preservation:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- Added focused shutdown factory service-assembly regression coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownFactoryTest.java`

## Phase 73.3 Outputs
- Added explicit Phase 73 verification script:
  - `scripts/verify_phase73_interaction_session_shutdown_service_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase73_interaction_session_shutdown_service_factory_extraction.py`
  - `python scripts/verify_phase72_interaction_session_click_event_service_factory_extraction.py`
  - `python scripts/verify_phase71_interaction_session_macro_pass_e_signoff.py`
  - `python scripts/verify_phase70_interaction_session_host_factory_consolidation_e.py`
  - `python scripts/verify_phase69_interaction_session_shutdown_delegate_host_factory_extraction.py`
  - `python scripts/verify_phase68_interaction_session_click_event_delegate_host_factory_extraction.py`
- Verified Java tests for the extraction wave:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionShutdownFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryShutdownServiceFactoryTest --tests com.xptool.sessions.InteractionSessionShutdownServiceTest`
- Recorded completion markers:
  - `PHASE 73 STARTED`
  - `PHASE 73 COMPLETE`

## Exit Criteria
- `InteractionSessionShutdownFactory` owns shutdown service assembly.
- `InteractionSessionHostFactory` shutdown service seam delegates to focused factory ownership while preserving compatibility sentinel strings.
- Phase 73 verification script and targeted guard/test pack both pass.
- `PHASE 73 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
