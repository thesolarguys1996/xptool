# Native Client Phase 101 Interaction Session Runtime Operations Bundle Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning interaction-session bundle ownership by extracting a focused runtime-operations bundle boundary so runtime delegation factories no longer depend on the full runtime bundle surface.

## Execution Slices
1. `101.1` Define Phase 101 scope, artifacts, and completion gates.
2. `101.2` Extract interaction-session runtime-operations bundle ownership and route runtime-operations factory through it.
3. `101.3` Run Phase 101 verification + guard pack and record `PHASE 101 COMPLETE`.

## Phase 101 Slice Status
- `101.1` complete.
- `101.2` complete.
- `101.3` complete.

## Phase 101.1 Outputs
- Added dedicated Phase 101 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE101_INTERACTION_SESSION_RUNTIME_OPERATIONS_BUNDLE_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 101 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 101.2 Outputs
- Added focused runtime-operations bundle boundary:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeOperationsBundle.java`
- Updated runtime bundle and runtime-operations factory seams to route through focused runtime-operations bundle ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeBundle.java`
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeOperationsFactory.java`
- Added runtime-operations bundle mapping assertions in existing runtime-bundle assembly tests:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactoryTest.java`
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionAssemblyFactoryTest.java`

## Phase 101.3 Outputs
- Added explicit Phase 101 verification script:
  - `scripts/verify_phase101_interaction_session_runtime_operations_bundle_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase101_interaction_session_runtime_operations_bundle_extraction.py`
  - `python scripts/verify_phase100_interaction_session_macro_pass_m_signoff.py`
  - `python scripts/verify_phase99_interaction_session_wiring_consolidation_m.py`
- Verified Java tests for the extraction wave:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionRuntimeBundleFactoryTest --tests com.xptool.sessions.InteractionSessionAssemblyFactoryTest --tests com.xptool.sessions.InteractionSessionRuntimeOperationsFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryTest`
- Recorded completion markers:
  - `PHASE 101 STARTED`
  - `PHASE 101 COMPLETE`

## Exit Criteria
- Runtime-operations factory no longer reaches into the full runtime-bundle field surface and routes through a focused runtime-operations bundle seam.
- Runtime-bundle tests validate runtime-operations bundle mapping.
- Phase 101 verification script and targeted guard/test pack both pass.
- `PHASE 101 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
