# Native Client Phase 93 Interaction Session Constructor Runtime Bundle Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning `InteractionSession` constructor ownership by extracting runtime-bundle injection boundaries while preserving compatibility sentinel strings required by prior phase gates.

## Execution Slices
1. `93.1` Define Phase 93 scope, artifacts, and completion gates.
2. `93.2` Extract interaction-session constructor runtime-bundle ownership seam.
3. `93.3` Run Phase 93 verification + guard pack and record `PHASE 93 COMPLETE`.

## Phase 93 Slice Status
- `93.1` complete.
- `93.2` complete.
- `93.3` complete.

## Phase 93.1 Outputs
- Added dedicated Phase 93 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE93_INTERACTION_SESSION_CONSTRUCTOR_RUNTIME_BUNDLE_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 93 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 93.2 Outputs
- Updated interaction-session constructor to route through runtime-bundle injection seam:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- Removed unused runtime-service fields from interaction-session constructor ownership while preserving active runtime delegation fields.

## Phase 93.3 Outputs
- Added explicit Phase 93 verification script:
  - `scripts/verify_phase93_interaction_session_constructor_runtime_bundle_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase93_interaction_session_constructor_runtime_bundle_extraction.py`
  - `python scripts/verify_phase92_interaction_session_macro_pass_k_signoff.py`
  - `python scripts/verify_phase91_interaction_session_assembly_consolidation_k.py`
- Verified Java tests for the extraction wave:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryTest --tests com.xptool.sessions.InteractionSessionAssemblyFactoryTest --tests com.xptool.sessions.InteractionSessionRuntimeBundleFactoryTest`
- Recorded completion markers:
  - `PHASE 93 STARTED`
  - `PHASE 93 COMPLETE`

## Exit Criteria
- `InteractionSession` constructor delegates runtime assembly through runtime-bundle injection seam.
- Unused constructor-owned runtime-service fields are removed.
- Phase 93 verification script and targeted guard/test pack both pass.
- `PHASE 93 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
