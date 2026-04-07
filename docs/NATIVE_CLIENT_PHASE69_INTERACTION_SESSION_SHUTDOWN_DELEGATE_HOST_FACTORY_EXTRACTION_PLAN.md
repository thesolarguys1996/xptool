# Native Client Phase 69 Interaction Session Shutdown Delegate-Host Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning `InteractionSessionHostFactory` by moving shutdown delegate-host assembly ownership into `InteractionSessionShutdownFactory` while preserving compatibility sentinel strings required by prior phase gates.

## Execution Slices
1. `69.1` Define Phase 69 scope, artifacts, and completion gates.
2. `69.2` Extract shutdown delegate-host assembly from host-factory compatibility delegate ownership into focused factory ownership.
3. `69.3` Run Phase 69 verification + guard pack and record `PHASE 69 COMPLETE`.

## Phase 69 Slice Status
- `69.1` complete.
- `69.2` complete.
- `69.3` complete.

## Phase 69.1 Outputs
- Added dedicated Phase 69 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE69_INTERACTION_SESSION_SHUTDOWN_DELEGATE_HOST_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 69 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 69.2 Outputs
- Updated host-factory shutdown delegate-host seam to delegate through focused factory ownership with compatibility sentinel preservation:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- Added host-factory shutdown delegate-host regression coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`

## Phase 69.3 Outputs
- Added explicit Phase 69 verification script:
  - `scripts/verify_phase69_interaction_session_shutdown_delegate_host_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase69_interaction_session_shutdown_delegate_host_factory_extraction.py`
  - `python scripts/verify_phase68_interaction_session_click_event_delegate_host_factory_extraction.py`
  - `python scripts/verify_phase67_interaction_session_macro_pass_d_signoff.py`
  - `python scripts/verify_phase66_interaction_session_host_factory_consolidation_d.py`
  - `python scripts/verify_phase65_interaction_session_ownership_service_factory_extraction.py`
  - `python scripts/verify_phase64_interaction_session_registration_service_factory_extraction.py`
- Verified Java tests for the extraction wave:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionHostFactoryShutdownHostTest --tests com.xptool.sessions.InteractionSessionShutdownFactoryTest --tests com.xptool.sessions.InteractionSessionShutdownServiceTest`
- Recorded completion markers:
  - `PHASE 69 STARTED`
  - `PHASE 69 COMPLETE`

## Exit Criteria
- `InteractionSessionHostFactory` shutdown delegate-host seam delegates to `InteractionSessionShutdownFactory` ownership.
- Host-factory compatibility sentinel shutdown delegate strings remain preserved for prior phase verifier stability.
- Phase 69 verification script and targeted guard/test pack both pass.
- `PHASE 69 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
