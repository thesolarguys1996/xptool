# Native Client Phase 123 Interaction Session Factory Runtime Bundle Key Policy Extraction Plan

Last updated: 2026-04-06

## Goal
Continue thinning interaction-session factory runtime-bundle ownership by extracting default session-key policy into focused `InteractionSessionFactoryRuntimeBundleKeyPolicy` ownership.

## Execution Slices
1. `123.1` Define Phase 123 scope, artifacts, and completion gates.
2. `123.2` Extract focused runtime-bundle default session-key policy ownership.
3. `123.3` Run Phase 123 verification + guard pack and record `PHASE 123 COMPLETE`.

## Phase 123 Slice Status
- `123.1` complete.
- `123.2` complete.
- `123.3` complete.

## Phase 123.1 Outputs
- Added dedicated Phase 123 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE123_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_KEY_POLICY_EXTRACTION_PLAN.md`
- Updated migration/task/status artifacts with Phase 123 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 123.2 Outputs
- Added focused runtime-bundle default session-key policy ownership:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleKeyPolicy.java`
- Added focused runtime-bundle default session-key policy coverage:
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleKeyPolicyTest.java`
- Extended runtime-bundle factory ownership with policy-default entrypoints:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java`
  - `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryTest.java`

## Phase 123.3 Outputs
- Added explicit Phase 123 verification script:
  - `scripts/verify_phase123_interaction_session_factory_runtime_bundle_key_policy_extraction.py`
- Executed verification commands:
  - `python scripts/verify_phase123_interaction_session_factory_runtime_bundle_key_policy_extraction.py`
  - `python scripts/verify_phase122_interaction_session_macro_pass_q_signoff.py`
  - `python scripts/verify_phase121_interaction_session_factory_wiring_consolidation_q.py`
- Verified Java extraction tests:
  - `./gradlew.bat test --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleKeyPolicyTest --tests com.xptool.sessions.InteractionSessionFactoryRuntimeBundleFactoryTest --tests com.xptool.sessions.InteractionSessionFactoryTest`
- Recorded completion markers:
  - `PHASE 123 STARTED`
  - `PHASE 123 COMPLETE`

## Exit Criteria
- Focused default session-key policy ownership exists and is consumed by runtime-bundle factory entrypoints.
- Phase 123 verifier and guard/test pack pass.
- `PHASE 123 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
