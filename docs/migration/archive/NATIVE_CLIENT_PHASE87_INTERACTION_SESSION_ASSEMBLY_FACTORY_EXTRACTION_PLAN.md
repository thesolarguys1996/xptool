# Native Client Phase 87 Interaction Session Assembly Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning direct constructor ownership in `InteractionSession` by extracting runtime service assembly into a focused `InteractionSessionAssemblyFactory` boundary while preserving compatibility sentinel strings required by prior phase gates.

## Execution Slices
1. `87.1` Define Phase 87 scope, artifacts, and completion gates.
2. `87.2` Extract interaction-session constructor assembly into focused assembly factory + runtime bundle ownership.
3. `87.3` Run Phase 87 verification + guard pack and record `PHASE 87 COMPLETE`.

## Phase 87 Slice Status
- `87.1` complete.
- `87.2` complete.
- `87.3` complete.

## Phase 87.1 Outputs
- Added dedicated Phase 87 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE87_INTERACTION_SESSION_ASSEMBLY_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 87 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 87.2 Outputs
- Added focused interaction-session runtime bundle model:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeBundle.java`
- Added focused interaction-session assembly factory boundary:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionAssemblyFactory.java`
- Updated interaction-session constructor to consume assembly factory bundle ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- Added focused bundle mapping regression coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionAssemblyFactoryTest.java`

## Phase 87.3 Outputs
- Added explicit Phase 87 verification script:
  - `scripts/verify_phase87_interaction_session_assembly_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase87_interaction_session_assembly_factory_extraction.py`
  - `python scripts/verify_phase86_interaction_session_macro_pass_i_signoff.py`
  - `python scripts/verify_phase85_interaction_session_host_factory_consolidation_i.py`
  - `python scripts/verify_phase84_interaction_session_ownership_service_composite_factory_extraction.py`
  - `python scripts/verify_phase83_interaction_session_macro_pass_h_signoff.py`
  - `python scripts/verify_phase82_interaction_session_host_factory_consolidation_h.py`
  - `python scripts/verify_phase81_interaction_session_command_router_service_composite_factory_extraction.py`
  - `python scripts/verify_phase80_interaction_session_post_click_settle_service_composite_factory_extraction.py`
- Verified Java tests for the extraction wave:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionAssemblyFactoryTest --tests com.xptool.sessions.InteractionSessionOwnershipFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryOwnershipServiceFactoryTest --tests com.xptool.sessions.InteractionSessionCommandRouterFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryCommandRouterServiceFactoryTest --tests com.xptool.sessions.InteractionPostClickSettleFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryPostClickSettleServiceFactoryTest`
- Recorded completion markers:
  - `PHASE 87 STARTED`
  - `PHASE 87 COMPLETE`

## Exit Criteria
- `InteractionSession` constructor assembly ownership is extracted to `InteractionSessionAssemblyFactory` + `InteractionSessionRuntimeBundle`.
- Compatibility sentinel strings remain intact for prior phase verifier stability.
- Phase 87 verification script and targeted guard/test pack both pass.
- `PHASE 87 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
