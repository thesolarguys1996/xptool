# Native Client Phase 91 Interaction Session Assembly Consolidation K Plan

Last updated: 2026-04-06

## Goal
Consolidate `InteractionSessionAssemblyFactory` by routing runtime-bundle construction seams through focused runtime-bundle factory boundaries while preserving compatibility sentinel strings required by prior phase gates.

## Execution Slices
1. `91.1` Define Phase 91 scope, artifacts, and completion gates.
2. `91.2` Consolidate runtime-bundle focused-factory delegation seam with compatibility preservation.
3. `91.3` Run Phase 91 verification + guard pack and record `PHASE 91 COMPLETE`.

## Phase 91 Slice Status
- `91.1` complete.
- `91.2` complete.
- `91.3` complete.

## Phase 91.1 Outputs
- Added dedicated Phase 91 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE91_INTERACTION_SESSION_ASSEMBLY_CONSOLIDATION_K_PLAN.md`
- Updated migration/task/status artifacts with Phase 91 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 91.2 Outputs
- Consolidated focused runtime-bundle factory delegation seam in assembly-factory boundary:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionAssemblyFactory.java`
- Preserved assembly-factory compatibility sentinel strings and wrappers required by prior-phase verifier gates:
  - runtime-bundle compatibility sentinels (`return createRuntimeBundleForSession(`, `return new InteractionSessionRuntimeBundle(`)
  - session-constructor compatibility sentinels (`InteractionSessionAssemblyFactory.createRuntimeBundle(`, `this.interactionSessionCommandRouter = runtimeBundle.interactionSessionCommandRouter;`)

## Phase 91.3 Outputs
- Added explicit Phase 91 verification script:
  - `scripts/verify_phase91_interaction_session_assembly_consolidation_k.py`
- Executed verification commands:
  - `python scripts/verify_phase91_interaction_session_assembly_consolidation_k.py`
  - `python scripts/verify_phase90_interaction_session_runtime_bundle_factory_extraction.py`
  - `python scripts/verify_phase89_interaction_session_macro_pass_j_signoff.py`
  - `python scripts/verify_phase88_interaction_session_assembly_consolidation_j.py`
  - `python scripts/verify_phase87_interaction_session_assembly_factory_extraction.py`
  - `python scripts/verify_phase86_interaction_session_macro_pass_i_signoff.py`
- Verified Java tests for the consolidation wave:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionRuntimeBundleFactoryTest --tests com.xptool.sessions.InteractionSessionAssemblyFactoryTest --tests com.xptool.sessions.InteractionSessionOwnershipFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryOwnershipServiceFactoryTest`
- Recorded completion markers:
  - `PHASE 91 STARTED`
  - `PHASE 91 COMPLETE`

## Exit Criteria
- `InteractionSessionAssemblyFactory` runtime-bundle seam delegates to focused runtime-bundle factory ownership.
- Compatibility sentinel strings remain intact for prior phase verifier stability.
- Phase 91 verification script and targeted guard/test pack both pass.
- `PHASE 91 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
