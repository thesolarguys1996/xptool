# Native Client Phase 22 Interaction Click Event Packaging Decomposition Plan

Last updated: 2026-04-05

## Goal
Continue reducing `CommandExecutor` runtime ownership by extracting interaction-click event packaging ownership into dedicated interaction telemetry/event contract components.

## Execution Slices
1. `22.1` Define Phase 22 scope, artifacts, and completion gates.
2. `22.2` Extract interaction-click event packaging ownership from `CommandExecutor` and delegate runtime wiring.
3. `22.3` Run Phase 22 verification + guard pack and record `PHASE 22 COMPLETE`.

## Phase 22 Slice Status
- `22.1` complete.
- `22.2` complete.
- `22.3` complete.

## Phase 22.1 Outputs
- Added dedicated Phase 22 plan and completion gate references:
  - `docs/NATIVE_CLIENT_PHASE22_INTERACTION_CLICK_EVENT_PACKAGING_PLAN.md`
- Updated migration/task/status artifacts with Phase 22 lifecycle markers:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
  - `TASKS.md`

## Phase 22.2 Outputs
- Extracted interaction-click event DTO ownership from `CommandExecutor` into top-level runtime contract:
  - `runelite-plugin/src/main/java/com/xptool/executor/InteractionClickEvent.java`
- Updated interaction-click telemetry callback packaging to emit `InteractionClickEvent` directly from service ownership:
  - `runelite-plugin/src/main/java/com/xptool/executor/InteractionClickTelemetryService.java`
- Updated `CommandExecutor` settle-eligible callback to orchestration-only forwarding and removed nested event packaging ownership:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Updated interaction-session intake to consume top-level click event contract:
  - `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- Added focused click-event contract regression test:
  - `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- Updated interaction telemetry service regression coverage for object-based callback contract:
  - `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`

## Phase 22.3 Outputs
- Added explicit Phase 22 verification script:
  - `scripts/verify_phase22_interaction_click_event_packaging.py`
- Executed Phase 22 verification + guard pack:
  - `python scripts/verify_phase22_interaction_click_event_packaging.py`
  - `python scripts/verify_phase21_interaction_anchor_resolution.py`
  - `python scripts/verify_phase20_interaction_click_telemetry.py`
  - `python scripts/verify_phase19_motor_dispatch_context.py`
  - `python scripts/verify_phase18_motor_dispatch_admission.py`
  - `python scripts/verify_phase17_motor_terminal_decomposition.py`
  - `python scripts/verify_phase16_motor_pending_telemetry.py`
  - `python scripts/verify_java_runtime_ownership_blocked.py`
  - `python scripts/verify_java_shim_retirement_gates.py`
  - `python scripts/verify_native_only_operations_hardening.py`
  - `python scripts/verify_native_cutover.py`
  - `python scripts/verify_native_soak_report.py --min-iterations 6 --max-failures 0 --max-age-hours 48`
- Verified Java executor/session tests for extraction wave:
  - `.\gradlew.bat test --tests com.xptool.executor.InteractionClickEventTest --tests com.xptool.executor.InteractionClickTelemetryServiceTest --tests com.xptool.executor.InteractionAnchorResolverServiceTest --tests com.xptool.executor.MotorDispatchContextServiceTest --tests com.xptool.executor.MotorDispatchAdmissionServiceTest --tests com.xptool.executor.MotorProgramTerminalServiceTest --tests com.xptool.executor.PendingMoveTelemetryServiceTest`
- Recorded completion markers:
  - `PHASE 22 STARTED`
  - `PHASE 22 COMPLETE`

## Exit Criteria
- Interaction-click event packaging ownership no longer lives in `CommandExecutor`.
- `InteractionClickTelemetryService` emits `InteractionClickEvent` objects as the settle-eligible contract.
- `CommandExecutor` remains orchestration-only for click-event forwarding.
- Focused click telemetry/event tests pass.
- Phase 22 verification script and native guard pack both pass.
- `PHASE 22 COMPLETE` is recorded in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
