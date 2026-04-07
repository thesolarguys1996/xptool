# Native Client Phase 90 Interaction Session Runtime Bundle Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning `InteractionSessionAssemblyFactory` by moving runtime-bundle construction ownership into focused `InteractionSessionRuntimeBundleFactory` boundaries while preserving compatibility sentinel strings required by prior phase gates.

## Execution Slices
1. `90.1` Define Phase 90 scope, artifacts, and completion gates.
2. `90.2` Extract runtime-bundle construction from assembly-factory ownership into focused runtime-bundle factory ownership.
3. `90.3` Run Phase 90 verification + guard pack and record `PHASE 90 COMPLETE`.

## Phase 90 Slice Status
- `90.1` complete.
- `90.2` complete.
- `90.3` complete.

## Phase 90.1 Outputs
- Added dedicated Phase 90 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE90_INTERACTION_SESSION_RUNTIME_BUNDLE_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 90 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 90.2 Outputs
- Added focused runtime-bundle factory ownership boundary:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactory.java`
- Updated assembly-factory runtime-bundle construction seam to delegate through focused factory ownership with compatibility sentinel preservation:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionAssemblyFactory.java`
- Added focused runtime-bundle factory mapping regression coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactoryTest.java`

## Phase 90.3 Outputs
- Added explicit Phase 90 verification script:
  - `scripts/verify_phase90_interaction_session_runtime_bundle_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase90_interaction_session_runtime_bundle_factory_extraction.py`
  - `python scripts/verify_phase89_interaction_session_macro_pass_j_signoff.py`
  - `python scripts/verify_phase88_interaction_session_assembly_consolidation_j.py`
  - `python scripts/verify_phase87_interaction_session_assembly_factory_extraction.py`
  - `python scripts/verify_phase86_interaction_session_macro_pass_i_signoff.py`
- Verified Java tests for the extraction wave:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionRuntimeBundleFactoryTest --tests com.xptool.sessions.InteractionSessionAssemblyFactoryTest --tests com.xptool.sessions.InteractionSessionOwnershipFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryOwnershipServiceFactoryTest`
- Recorded completion markers:
  - `PHASE 90 STARTED`
  - `PHASE 90 COMPLETE`

## Exit Criteria
- `InteractionSessionRuntimeBundleFactory` owns runtime-bundle construction assembly.
- `InteractionSessionAssemblyFactory` runtime-bundle construction seam delegates to focused factory ownership while preserving compatibility sentinel strings.
- Phase 90 verification script and targeted guard/test pack both pass.
- `PHASE 90 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
