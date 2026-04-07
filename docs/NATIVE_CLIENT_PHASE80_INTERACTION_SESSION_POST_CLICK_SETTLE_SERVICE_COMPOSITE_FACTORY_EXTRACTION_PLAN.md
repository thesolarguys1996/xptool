# Native Client Phase 80 Interaction Session Post-Click Settle Service Composite Factory Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning `InteractionSessionHostFactory` by moving post-click-settle composite service assembly ownership into `InteractionPostClickSettleFactory` while preserving compatibility sentinel strings required by prior phase gates.

## Execution Slices
1. `80.1` Define Phase 80 scope, artifacts, and completion gates.
2. `80.2` Extract post-click-settle composite service assembly from host-factory ownership into focused factory ownership.
3. `80.3` Run Phase 80 verification + guard pack and record `PHASE 80 COMPLETE`.

## Phase 80 Slice Status
- `80.1` complete.
- `80.2` complete.
- `80.3` complete.

## Phase 80.1 Outputs
- Added dedicated Phase 80 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE80_INTERACTION_SESSION_POST_CLICK_SETTLE_SERVICE_COMPOSITE_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 80 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 80.2 Outputs
- Updated host-factory post-click-settle service seam to delegate through focused factory ownership with compatibility sentinel preservation:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- Retained focused post-click-settle factory composite service ownership boundary:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionPostClickSettleFactory.java`
- Reused focused post-click-settle and host-factory service-from-host regression coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleFactoryTest.java`
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleServiceFactoryTest.java`

## Phase 80.3 Outputs
- Added explicit Phase 80 verification script:
  - `scripts/verify_phase80_interaction_session_post_click_settle_service_composite_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase80_interaction_session_post_click_settle_service_composite_factory_extraction.py`
  - `python scripts/verify_phase79_interaction_session_macro_pass_g_signoff.py`
  - `python scripts/verify_phase78_interaction_session_host_factory_consolidation_g.py`
  - `python scripts/verify_phase77_interaction_session_motor_service_composite_factory_extraction.py`
  - `python scripts/verify_phase76_interaction_session_registration_service_composite_factory_extraction.py`
- Verified Java tests for the extraction wave:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionPostClickSettleFactoryTest --tests com.xptool.sessions.InteractionSessionHostFactoryPostClickSettleServiceFactoryTest --tests com.xptool.sessions.InteractionPostClickSettleServiceTest`
- Recorded completion markers:
  - `PHASE 80 STARTED`
  - `PHASE 80 COMPLETE`

## Exit Criteria
- `InteractionPostClickSettleFactory` owns post-click-settle composite service assembly.
- `InteractionSessionHostFactory` post-click-settle service seam delegates to focused factory ownership while preserving compatibility sentinel strings.
- Phase 80 verification script and targeted guard/test pack both pass.
- `PHASE 80 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
