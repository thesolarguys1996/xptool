# Native Client Phase 183 Interaction Session Factory Runtime Bundle Session Factory Extraction Plan

Last updated: 2026-04-07

## Goal
Continue thinning interaction-session factory ownership by extracting runtime-bundle and runtime-operations session routing into focused ownership.

## Execution Slices
1. `183.1` Define Phase 183 scope, artifacts, and completion gates.
2. `183.2` Extract `InteractionSessionFactoryRuntimeBundleSessionFactory` ownership and route runtime-bundle/runtime-operations seams through focused ownership.
3. `183.3` Run Phase 183 verification + guard pack and record `PHASE 183 COMPLETE`.

## Phase 183 Slice Status
- `183.1` complete.
- `183.2` complete.
- `183.3` complete.

## Phase 183.1 Outputs
- Added dedicated Phase 183 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE183_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_SESSION_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 183 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 183.2 Outputs
- Added focused runtime-bundle session factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleSessionFactory.java`
- Consolidated runtime-bundle and runtime-operations seams through focused ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`
- Added focused runtime-bundle session factory coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleSessionFactoryTest.java`

## Phase 183.3 Outputs
- Added explicit Phase 183 verification script:
  - `scripts/verify_phase183_interaction_session_factory_runtime_bundle_session_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase183_interaction_session_factory_runtime_bundle_session_factory_extraction.py`
  - `python scripts/verify_phase182_interaction_session_factory_runtime_bundle_factory_input_typed_routing_extraction.py`
  - `python scripts/verify_phase181_interaction_session_factory_runtime_bundle_factory_inputs_session_factory_extraction.py`
- Verified Java extraction/consolidation tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryTest --tests com.xptool.sessions.InteractionSessionRuntimeSessionFactoryTest`
- Recorded completion markers:
  - `PHASE 183 STARTED`
  - `PHASE 183 COMPLETE`

## Exit Criteria
- Runtime-bundle/runtime-operations session routing ownership is extracted into a focused factory component.
- Focused extraction/consolidation tests and phase verification guard pack pass.
- `PHASE 183 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
