# Native Client Phase 81 Interaction Session Command-Router Service Composite Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning `InteractionSessionHostFactory` by moving command-router composite service assembly ownership into `InteractionSessionCommandRouterFactory` while preserving compatibility sentinel strings required by prior phase gates.

## Execution Slices
1. `81.1` Define Phase 81 scope, artifacts, and completion gates.
2. `81.2` Extract command-router composite service assembly from host-factory ownership into focused factory ownership.
3. `81.3` Run Phase 81 verification + guard pack and record `PHASE 81 COMPLETE`.

## Phase 81 Slice Status
- `81.1` complete.
- `81.2` complete.
- `81.3` complete.

## Phase 81.1 Outputs
- Added dedicated Phase 81 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE81_INTERACTION_SESSION_COMMAND_ROUTER_SERVICE_COMPOSITE_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 81 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 81.2 Outputs
- Expanded focused command-router factory ownership for composite service assembly:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionCommandRouterFactory.java`
- Updated host-factory command-router service seam to delegate through focused factory ownership with compatibility sentinel preservation:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- Added focused command-router factory and host-factory composite service regression coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterFactoryTest.java`
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterServiceFactoryTest.java`

## Phase 81.3 Outputs
- Added explicit Phase 81 verification script:
  - `scripts/verify_phase81_interaction_session_command_router_service_composite_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase81_interaction_session_command_router_service_composite_factory_extraction.py`
  - `python scripts/verify_phase80_interaction_session_post_click_settle_service_composite_factory_extraction.py`
  - `python scripts/verify_phase79_interaction_session_macro_pass_g_signoff.py`
  - `python scripts/verify_phase78_interaction_session_host_factory_consolidation_g.py`
  - `python scripts/verify_phase77_interaction_session_motor_service_composite_factory_extraction.py`
  - `python scripts/verify_phase76_interaction_session_registration_service_composite_factory_extraction.py`
- Verified Java tests for the extraction wave:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionCommandRouterFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryCommandRouterServiceFactoryTest --tests com.xptool.sessions.InteractionSessionCommandRouterTest`
- Recorded completion markers:
  - `PHASE 81 STARTED`
  - `PHASE 81 COMPLETE`

## Exit Criteria
- `InteractionSessionCommandRouterFactory` owns command-router composite service assembly.
- `InteractionSessionHostFactory` command-router service seam delegates to focused factory ownership while preserving compatibility sentinel strings.
- Phase 81 verification script and targeted guard/test pack both pass.
- `PHASE 81 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
