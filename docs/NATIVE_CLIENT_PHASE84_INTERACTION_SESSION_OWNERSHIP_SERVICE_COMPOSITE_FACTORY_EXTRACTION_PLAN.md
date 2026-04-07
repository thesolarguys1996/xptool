# Native Client Phase 84 Interaction Session Ownership Service Composite Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning `InteractionSessionHostFactory` by moving ownership composite service assembly ownership into `InteractionSessionOwnershipFactory` while preserving compatibility sentinel strings required by prior phase gates.

## Execution Slices
1. `84.1` Define Phase 84 scope, artifacts, and completion gates.
2. `84.2` Extract ownership composite service assembly from host-factory ownership into focused factory ownership.
3. `84.3` Run Phase 84 verification + guard pack and record `PHASE 84 COMPLETE`.

## Phase 84 Slice Status
- `84.1` complete.
- `84.2` complete.
- `84.3` complete.

## Phase 84.1 Outputs
- Added dedicated Phase 84 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE84_INTERACTION_SESSION_OWNERSHIP_SERVICE_COMPOSITE_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 84 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 84.2 Outputs
- Expanded focused ownership factory ownership for composite service assembly:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionOwnershipFactory.java`
- Updated host-factory ownership service seam to delegate through focused factory ownership with compatibility sentinel preservation:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- Reused focused ownership service and host-factory ownership service regression coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipFactoryTest.java`
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipServiceFactoryTest.java`

## Phase 84.3 Outputs
- Added explicit Phase 84 verification script:
  - `scripts/verify_phase84_interaction_session_ownership_service_composite_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase84_interaction_session_ownership_service_composite_factory_extraction.py`
  - `python scripts/verify_phase83_interaction_session_macro_pass_h_signoff.py`
  - `python scripts/verify_phase82_interaction_session_host_factory_consolidation_h.py`
  - `python scripts/verify_phase81_interaction_session_command_router_service_composite_factory_extraction.py`
  - `python scripts/verify_phase80_interaction_session_post_click_settle_service_composite_factory_extraction.py`
- Verified Java tests for the extraction wave:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionOwnershipFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryOwnershipServiceFactoryTest --tests com.xptool.sessions.InteractionSessionOwnershipServiceTest`
- Recorded completion markers:
  - `PHASE 84 STARTED`
  - `PHASE 84 COMPLETE`

## Exit Criteria
- `InteractionSessionOwnershipFactory` owns ownership composite service assembly.
- `InteractionSessionHostFactory` ownership service seam delegates to focused factory ownership while preserving compatibility sentinel strings.
- Phase 84 verification script and targeted guard/test pack both pass.
- `PHASE 84 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
