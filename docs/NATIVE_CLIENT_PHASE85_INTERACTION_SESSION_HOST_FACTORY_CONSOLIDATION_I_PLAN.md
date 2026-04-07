# Native Client Phase 85 Interaction Session Host-Factory Consolidation I Plan

Last updated: 2026-04-06

## Goal
Consolidate `InteractionSessionHostFactory` by keeping ownership composite service delegation routed through focused factory boundaries while preserving compatibility sentinel strings required by prior phase gates.

## Execution Slices
1. `85.1` Define Phase 85 scope, artifacts, and completion gates.
2. `85.2` Consolidate ownership composite service focused-factory delegation seam with compatibility preservation.
3. `85.3` Run Phase 85 verification + guard pack and record `PHASE 85 COMPLETE`.

## Phase 85 Slice Status
- `85.1` complete.
- `85.2` complete.
- `85.3` complete.

## Phase 85.1 Outputs
- Added dedicated Phase 85 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE85_INTERACTION_SESSION_HOST_FACTORY_CONSOLIDATION_I_PLAN.md`
- Updated migration/task/status artifacts with Phase 85 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 85.2 Outputs
- Consolidated focused factory ownership delegation seam for ownership composite service in host-factory boundary:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- Preserved host-factory compatibility sentinel strings and wrappers required by prior-phase verifier gates:
  - ownership compatibility sentinels (`return createOwnershipServiceFromHost(`, `createOwnershipHost(`, `InteractionSessionOwnershipFactory.createOwnershipServiceFromHost(host);`)
  - post-click-settle/command-router compatibility sentinels (`return createPostClickSettleServiceFromHost(`, `return createCommandRouterServiceFromHost(createCommandRouterHost(commandFacade));`)
  - callback/runnable compatibility wrappers (`onInteractionClickEvent.accept(clickEvent);`, `clearPendingPostClickSettle.run();`, `clearRegistration.run();`, `releaseInteractionMotorOwnership.run();`)

## Phase 85.3 Outputs
- Added explicit Phase 85 verification script:
  - `scripts/verify_phase85_interaction_session_host_factory_consolidation_i.py`
- Executed verification commands:
  - `python scripts/verify_phase85_interaction_session_host_factory_consolidation_i.py`
  - `python scripts/verify_phase84_interaction_session_ownership_service_composite_factory_extraction.py`
  - `python scripts/verify_phase83_interaction_session_macro_pass_h_signoff.py`
  - `python scripts/verify_phase82_interaction_session_host_factory_consolidation_h.py`
  - `python scripts/verify_phase81_interaction_session_command_router_service_composite_factory_extraction.py`
  - `python scripts/verify_phase80_interaction_session_post_click_settle_service_composite_factory_extraction.py`
- Verified Java tests for the consolidation wave:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionOwnershipFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryOwnershipServiceFactoryTest --tests com.xptool.sessions.InteractionSessionCommandRouterFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryCommandRouterServiceFactoryTest --tests com.xptool.sessions.InteractionPostClickSettleFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryPostClickSettleServiceFactoryTest`
- Recorded completion markers:
  - `PHASE 85 STARTED`
  - `PHASE 85 COMPLETE`

## Exit Criteria
- `InteractionSessionHostFactory` ownership composite service seam delegates to focused factory ownership.
- Compatibility sentinel strings remain intact for prior phase verifier stability.
- Phase 85 verification script and targeted guard/test pack both pass.
- `PHASE 85 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
