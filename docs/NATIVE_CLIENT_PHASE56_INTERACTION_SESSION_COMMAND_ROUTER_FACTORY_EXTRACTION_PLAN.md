# Native Client Phase 56 Interaction Session Command-Router Service Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning `InteractionSessionHostFactory` by extracting command-router service-from-host assembly into a focused `InteractionSessionCommandRouterFactory` component.

## Execution Slices
1. `56.1` Define Phase 56 scope, artifacts, and completion gates.
2. `56.2` Extract command-router service-from-host assembly from `InteractionSessionHostFactory` into focused factory ownership.
3. `56.3` Run Phase 56 verification + guard pack and record `PHASE 56 COMPLETE`.

## Phase 56 Slice Status
- `56.1` complete.
- `56.2` complete.
- `56.3` complete.

## Phase 56.1 Outputs
- Added dedicated Phase 56 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE56_INTERACTION_SESSION_COMMAND_ROUTER_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 56 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 56.2 Outputs
- Added focused command-router service factory component:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionCommandRouterFactory.java`
- Updated `InteractionSessionHostFactory` command-router service-from-host assembly to route through focused factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- Added focused command-router service factory regression test:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterFactoryTest.java`

## Phase 56.3 Outputs
- Added explicit Phase 56 verification script:
  - `scripts/verify_phase56_interaction_session_command_router_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase56_interaction_session_command_router_factory_extraction.py`
  - `python scripts/verify_phase55_interaction_session_macro_pass_signoff.py`
  - `python scripts/verify_phase54_interaction_session_host_factory_consolidation.py`
  - `python scripts/verify_phase53_interaction_session_ownership_factory_extraction.py`
  - `python scripts/verify_phase52_interaction_session_post_click_settle_factory_extraction.py`
  - `python scripts/verify_phase51_interaction_session_click_event_factory_extraction.py`
- Verified Java tests for the extraction wave:
  - `.\gradlew.bat test --tests com.xptool.sessions.InteractionSessionCommandRouterFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryCommandRouterServiceFactoryTest --tests com.xptool.sessions.InteractionSessionCommandRouterTest`
- Recorded completion markers:
  - `PHASE 56 STARTED`
  - `PHASE 56 COMPLETE`

## Exit Criteria
- `InteractionSessionHostFactory` command-router service-from-host assembly routes through focused `InteractionSessionCommandRouterFactory` ownership.
- `InteractionSessionCommandRouterFactory` provides command-router service-from-host construction boundaries with focused regression tests.
- Phase 56 verification script and targeted guard/test pack both pass.
- `PHASE 56 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
