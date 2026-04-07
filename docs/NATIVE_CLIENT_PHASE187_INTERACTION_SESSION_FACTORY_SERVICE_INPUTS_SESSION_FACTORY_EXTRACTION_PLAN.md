# Native Client Phase 187 Interaction Session Factory Service Inputs Session Factory Extraction Plan

Last updated: 2026-04-07

## Goal
Continue thinning interaction-session factory ownership by extracting service-input session creation into focused ownership.

## Execution Slices
1. `187.1` Define Phase 187 scope, artifacts, and completion gates.
2. `187.2` Extract `InteractionSessionFactoryServiceInputsSessionFactory` ownership for service-input session creation seams.
3. `187.3` Run Phase 187 verification + guard pack and record `PHASE 187 COMPLETE`.

## Phase 187 Slice Status
- `187.1` complete.
- `187.2` complete.
- `187.3` complete.

## Phase 187.1 Outputs
- Added dedicated Phase 187 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE187_INTERACTION_SESSION_FACTORY_SERVICE_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 187 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 187.2 Outputs
- Added focused service-input session factory ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryServiceInputsSessionFactory.java`
- Added focused service-input session factory coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryServiceInputsSessionFactoryTest.java`

## Phase 187.3 Outputs
- Added explicit Phase 187 verification script:
  - `scripts/verify_phase187_interaction_session_factory_service_inputs_session_factory_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase187_interaction_session_factory_service_inputs_session_factory_extraction.py`
  - `python scripts/verify_phase186_interaction_session_factory_default_runtime_bundle_factory_input_typed_routing_extraction.py`
  - `python scripts/verify_phase185_interaction_session_factory_default_runtime_bundle_factory_inputs_session_factory_extraction.py`
- Verified Java extraction tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryServiceInputsSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryInputsTest`
- Recorded completion markers:
  - `PHASE 187 STARTED`
  - `PHASE 187 COMPLETE`

## Exit Criteria
- Service-input session creation ownership is extracted into a focused factory component.
- Focused extraction tests and phase verification guard pack pass.
- `PHASE 187 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
