# Native Client Migration Plan (C++)

Last updated: 2026-04-05

## Goal
Move from RuneLite/Java ownership to a fully native-owned runtime stack, with Java limited to temporary compatibility shims during migration.

## Target Repository Layout
```text
native-core/
  CMakeLists.txt
  include/xptool/core/
  src/runtime_core/
  src/motor/
  src/state/
  src/activities/woodcutting/
native-bridge/
  CMakeLists.txt
  src/
native-ui/
  CMakeLists.txt
  src/
schemas/
  native/
    command-envelope.v1.json
    telemetry-event.v1.json
```

## Phase 0 Deliverables (Architecture Lock)
- Lock native module ownership and boundaries:
  - `native-core`: runtime policy and motor authority.
  - `native-bridge`: local IPC ingress/egress and envelope validation.
  - `native-ui`: runtime visualization and operator controls.
  - `schemas/native`: canonical shared contract definitions.
- Enforce one-motor-authority rule in native runtime design (`motor::MotorRuntime`).
- Keep Java paths as compatibility adapters only, without new long-lived behavior ownership.

## Module Owners (Phase 0 Lock)
- `native-core`: Runtime Core
- `native-bridge`: Bridge + Security
- `native-ui`: Runtime UX
- `schemas/native`: Protocol Governance

These are role owners for migration execution. Team/user-name assignment can be layered on top without changing module boundaries.

## Phase 1 Deliverables (Contract-First Bridge)
- Finalize and version `command-envelope.v1.json` and `telemetry-event.v1.json`.
- Require envelope validation before command ingest.
- Require loopback-only bind and token-auth in bridge startup policy.
- Emit rejection telemetry with reason codes for:
  - invalid schema,
  - missing token,
  - unsupported command type,
  - replay/timestamp rejection (when verifier is enabled).

Phase 1 implementation files:
- `native-bridge/src/main.cpp`
- `native-bridge/src/bridge_service.cpp`
- `native-bridge/src/command_envelope_validator.cpp`
- `native-bridge/src/telemetry_event.cpp`
- `native-bridge/src/command_policy.cpp`

## Phase 2 Deliverables (Native Runtime Core)
- Port coordinator and gate ownership into native runtime services.
- Keep one motor authority (`MotorRuntime`) for dispatch execution.
- Emit explicit runtime reason-code telemetry for accepted/rejected tick outcomes.
- Provide a smoke harness that proves gate + dispatch flow end-to-end.

Phase 2 implementation files:
- `native-core/include/xptool/core/runtime_coordinator.hpp`
- `native-core/include/xptool/core/runtime_gate.hpp`
- `native-core/include/xptool/core/runtime_telemetry.hpp`
- `native-core/include/xptool/core/runtime_intent.hpp`
- `native-core/src/runtime_core/runtime_coordinator.cpp`
- `native-core/src/runtime_core/runtime_gate.cpp`
- `native-core/src/runtime_core/runtime_telemetry.cpp`
- `native-core/src/runtime_core/runtime_runner.cpp`
- `native-core/src/runtime_core/runtime_smoke_main.cpp`

## Phase 3 Deliverables (Woodcutting Vertical Slice)
- Define woodcutting target-candidate/selection contracts in native-core.
- Implement state snapshot translation for fallback candidate acquisition.
- Implement anti-repeat target selection guard in native-core ownership.
- Route woodcutting dispatch through motor path with selected target identity.
- Emit candidate-scoring and no-progress telemetry reason codes.
- Add parity harness with baseline-trace comparison and promotion thresholds.

Phase 3 implementation files:
- `native-core/include/xptool/core/woodcutting_target.hpp`
- `native-core/include/xptool/core/woodcutting_target_selector.hpp`
- `native-core/include/xptool/core/state_snapshot_translator.hpp`
- `native-core/src/activities/woodcutting/woodcutting_target_selector.cpp`
- `native-core/src/state/state_snapshot_translator.cpp`
- `native-core/src/activities/woodcutting/woodcutting_runtime.cpp`
- `native-core/src/runtime_core/runtime_coordinator.cpp`
- `native-core/src/activities/woodcutting/woodcutting_parity_main.cpp`
- `native-core/parity/woodcutting_baseline_v1.csv`

## Phase 4 Deliverables (State Acquisition Hardening)
- Add native state-frame ingestion boundary with schema-version handling.
- Add compatibility alias mapping for state fields that may change across client updates.
- Add sequence validation and deferred recovery using last-known-good snapshots.
- Enforce deferred failure budget with explicit terminal rejection reason codes.
- Provide acquisition smoke harness with reason-code evidence.

Phase 4 implementation files:
- `native-core/include/xptool/core/state_frame.hpp`
- `native-core/include/xptool/core/state_acquisition_service.hpp`
- `native-core/src/state/state_acquisition_service.cpp`
- `native-core/src/state/state_acquisition_smoke_main.cpp`
- `native-core/src/runtime_core/runtime_runner.cpp`

## Phase 5 Deliverables (Activity Migration)
- Add native runtime services for `mining`, `fishing`, `combat`, and `banking`.
- Route activity dispatch via runtime coordinator using `activity_key`.
- Keep repeated-target guard ownership in per-activity runtime components.
- Validate each migrated activity via parity gates before promotion.

Phase 5 implementation files:
- `native-core/include/xptool/core/mining_runtime.hpp`
- `native-core/include/xptool/core/fishing_runtime.hpp`
- `native-core/include/xptool/core/combat_runtime.hpp`
- `native-core/include/xptool/core/banking_runtime.hpp`
- `native-core/include/xptool/core/repeat_target_selector.hpp`
- `native-core/src/runtime_core/repeat_target_selector.cpp`
- `native-core/src/activities/mining/mining_runtime.cpp`
- `native-core/src/activities/fishing/fishing_runtime.cpp`
- `native-core/src/activities/combat/combat_runtime.cpp`
- `native-core/src/activities/banking/banking_runtime.cpp`
- `native-core/src/activities/activity_parity_main.cpp`
- `native-core/parity/activity_baseline_v1.csv`

## Phase 6 Deliverables (UI/Overlay Replacement)
- Implement native telemetry-driven overlay rendering path in `native-ui`.
- Add native UI config controls for activity focus and rejected-event visibility.
- Validate overlay rendering against bridge telemetry and native-core activity telemetry.
- Persist rendered overlay snapshots for local diagnostics.

Phase 6 implementation files:
- `native-ui/include/xptool/ui/ui_config.hpp`
- `native-ui/include/xptool/ui/telemetry_parser.hpp`
- `native-ui/include/xptool/ui/status_overlay.hpp`
- `native-ui/src/ui_config.cpp`
- `native-ui/src/telemetry_parser.cpp`
- `native-ui/src/status_overlay.cpp`
- `native-ui/src/main.cpp`
- `native-ui/config/default_ui_config.cfg`
- `native-ui/config/mining_focus.cfg`

## Phase 7 Deliverables (Cutover + Java Shim Freeze)
- Switch bootstrap defaults to native runtime paths and artifacts.
- Add native-first launcher script for bridge/UI startup.
- Freeze Java shim ownership and mark temporary shim files with `NATIVE_MIGRATION_TODO` + target native owner.
- Add one-command cutover verification that proves:
  - bridge enforcement and rejection telemetry,
  - native-core parity gates,
  - state-acquisition hardening,
  - native-ui rendering output,
  - phase/task status completion markers.

Phase 7 implementation files:
- `scripts/bootstrap-runtime.ps1`
- `scripts/bootstrap-native-runtime.ps1`
- `scripts/verify_native_cutover.py`
- `docs/NATIVE_CLIENT_CUTOVER_RUNBOOK.md`

## Post-Phase 7 Signoff (Soak Gate)
- Run repeated native cutover checks via `scripts/run_native_soak.py`.
- Validate soak thresholds via `scripts/verify_native_soak_report.py`.
- Keep Java shim decommission blocked until soak report gate passes.

## Phase 8A (Hard-Disable Java Runtime Ownership)
- Keep Java shadow runtime disabled by default during transitional cutover.
- Enforce no-regression guard in CI (`scripts/verify_java_runtime_ownership_blocked.py`).

## Phase 8B (Remove Java Shadow Runtime Path)
- Remove Java shadow runtime/context implementations and injector polling settings.
- Remove legacy shadow launcher/property control paths.
- Keep CI/runtime guard enforcing full absence of Java shadow runtime ownership.

## Phase 9 (Retire Remaining Java Plugin Shim)
- Defined explicit `XPToolPlugin`/`XPToolConfig` retirement checklist and native-owner mapping.
- Enforced shim-retirement pre-removal gates in automation.
- Removed Java plugin/config shim surfaces after parity + soak + guard pack passes.

Phase 9 implementation files:
- `docs/NATIVE_CLIENT_PHASE9_SHIM_RETIREMENT_PLAN.md`
- `scripts/verify_java_shim_retirement_gates.py`
- `.github/workflows/tasks-priority-gate.yml`

## Phase 10 (Native-Only Operations Hardening)
- Define and execute post-Phase-9 native-only hardening checklist.
- Remove stale operational assumptions tied to Java plugin ownership.
- Tighten verification and runbook guidance for native-only runtime operation.

Phase 10 implementation files:
- `docs/NATIVE_CLIENT_PHASE10_HARDENING_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `docs/NATIVE_INCIDENT_TRIAGE_RUNBOOK.md`
- `docs/NATIVE_SOAK_SIGNOFF.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `TASKS.md`
- `scripts/verify_native_only_operations_hardening.py`
- `scripts/verify_native_cutover.py`
- `scripts/verify_native_soak_report.py`

## Phase 11 (Native Host Cutover Waves)
- Execute staged host cutover from Java surfaces into native-owned boundaries.
- Use `docs/NATIVE_JAVA_SURFACE_INVENTORY.md` baseline to sequence `delete-first` then `port-first` waves.
- Keep Java layers compatibility-only during migration and remove wave by wave after parity gates pass.

Phase 11 implementation files:
- `docs/NATIVE_CLIENT_PHASE11_HOST_CUTOVER_PLAN.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `TASKS.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorAccountRuntimeInputs.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorAccountRuntimeWiring.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorAccountRuntimeInputFactory.java`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- `runelite-plugin/src/main/java/com/xptool/executor/BridgeLiveDispatchPolicy.java`
- `runelite-plugin/src/main/java/com/xptool/executor/BridgeRuntimeStateLiveDispatchPolicy.java`
- `runelite-plugin/src/main/java/com/xptool/executor/BridgeCommandDispatchModePolicy.java`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandDispatchPrecheckPolicy.java`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandDecisionOutcomePolicy.java`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandShadowDispatchPolicy.java`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandRowEvaluationPolicy.java`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandRowDispatchRoutingPolicy.java`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandLiveDispatchPolicy.java`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandShadowEvaluationPolicy.java`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandEvaluationService.java`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandQueueIdleArmingService.java`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandRowPlannerTagPolicy.java`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandQueueIngestService.java`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandIngestCallbackService.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorCombatDomainWiring.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorCombatHostFactories.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorBankDomainWiring.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorBankHostFactories.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorSkillingDomainWiring.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorSkillingHostFactories.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorResolverHostFactories.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorRuntimeCoordinatorHostFactory.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorRuntimeServiceHostFactory.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorLoginInteractionHostFactory.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorIdleHostFactory.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorEngineWiring.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorInteractionControllerHostFactories.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorSceneDomainWiring.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorRuntimeDomainWiring.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayRuntimeInputs.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayRuntimeInputFactory.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayRuntimeWiring.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayHostFactories.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayServiceHostsAssembler.java`
- `runelite-plugin/src/main/java/com/xptool/bridge/BridgeDispatchSettings.java`
- `runelite-plugin/src/main/java/com/xptool/bridge/ExecutorBridgeDispatchSettings.java`
- `runelite-plugin/src/main/java/com/xptool/bridge/BridgeDispatchConfigService.java`
- `runelite-plugin/src/main/java/com/xptool/bridge/BridgeHeartbeatService.java`
- `runelite-plugin/src/main/java/com/xptool/bridge/BridgeIpcServer.java`
- `runelite-plugin/src/main/java/com/xptool/bridge/BridgeRuntime.java`

## Phase 12 (Native-Default Runtime Completion)
- Define and execute native-default runtime completion gates after Phase 11 host cutover.
- Validate bootstrap/launcher enforcement (native-default pathing, loopback-only bind checks, token-required bridge startup).
- Run a full signoff gate pack with soak-report recency checks and record `PHASE 12 COMPLETE`.

Phase 12 implementation files:
- `docs/NATIVE_CLIENT_PHASE12_NATIVE_DEFAULT_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase12_native_default.py`
- `scripts/bootstrap-runtime.ps1`
- `scripts/bootstrap-native-runtime.ps1`
- `docs/NATIVE_CLIENT_CUTOVER_RUNBOOK.md`
- `scripts/verify_java_runtime_ownership_blocked.py`
- `scripts/verify_java_shim_retirement_gates.py`
- `scripts/verify_native_only_operations_hardening.py`
- `scripts/verify_native_cutover.py`
- `scripts/verify_native_soak_report.py`

## Phase 13 (Executor Compatibility Decomposition)
- Continue extracting residual command-ingest and runtime-gate behavior ownership from `CommandExecutor` into focused runtime services.
- Keep `CommandExecutor` orchestration-only by moving command id-cache, command file-path policy, and manual-metrics gate telemetry logic into dedicated components.
- Validate via focused executor tests plus phase guard/signoff checks, then record `PHASE 13 COMPLETE`.

Phase 13 implementation files:
- `docs/NATIVE_CLIENT_PHASE13_EXECUTOR_DECOMPOSITION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase13_executor_decomposition.py`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandIdDeduplicationService.java`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandFilePathResolver.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ManualMetricsGateTelemetryService.java`
- `runelite-plugin/src/test/java/com/xptool/executor/CommandIdDeduplicationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/CommandFilePathResolverTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/ManualMetricsGateTelemetryServiceTest.java`

## Phase 14 (Executor Utility Decomposition)
- Continue reducing `CommandExecutor` helper ownership by delegating shared utility parsing/building methods to focused utility components.
- Remove dead helper paths (`elapsedTicksSince`) and keep `CommandExecutor` orchestration-focused.
- Validate via focused utility tests plus phase guard/signoff checks, then record `PHASE 14 COMPLETE`.

Phase 14 implementation files:
- `docs/NATIVE_CLIENT_PHASE14_EXECUTOR_UTILITY_DECOMPOSITION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase14_executor_utility_decomposition.py`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorValueParsers.java`
- `runelite-plugin/src/test/java/com/xptool/executor/ExecutorValueParsersTest.java`

## Phase 15 (Drop Runtime Decomposition)
- Continue reducing `CommandExecutor` runtime ownership by extracting drop-sweep session state and drop-target inventory policy into dedicated drop runtime services.
- Keep `CommandExecutor` orchestration-focused and delegate drop-session state transitions, progress tracking, and target item matching through dedicated components.
- Validate via focused drop-service tests plus phase guard/signoff checks, then record `PHASE 15 COMPLETE`.

Phase 15 implementation files:
- `docs/NATIVE_CLIENT_PHASE15_DROP_RUNTIME_DECOMPOSITION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase15_drop_runtime_decomposition.py`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- `runelite-plugin/src/main/java/com/xptool/executor/DropSweepSessionService.java`
- `runelite-plugin/src/main/java/com/xptool/executor/DropSweepInventoryService.java`
- `runelite-plugin/src/test/java/com/xptool/executor/DropSweepSessionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/DropSweepInventoryServiceTest.java`

## Phase 16 (Motor Pending-Telemetry Decomposition)
- Continue reducing `CommandExecutor` runtime ownership by extracting pending-move telemetry aggregation/event emission into a focused motor telemetry component.
- Keep `CommandExecutor` orchestration-focused by delegating motor pending-move telemetry callbacks and debug counter state to dedicated services.
- Validate via focused pending-move telemetry tests plus phase guard/signoff checks, then record `PHASE 16 COMPLETE`.

Phase 16 implementation files:
- `docs/NATIVE_CLIENT_PHASE16_MOTOR_PENDING_TELEMETRY_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase16_motor_pending_telemetry.py`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- `runelite-plugin/src/main/java/com/xptool/executor/PendingMoveTelemetryService.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 17 (Motor Terminal Decomposition)
- Continue reducing `CommandExecutor` runtime ownership by extracting motor terminal lifecycle ownership (menu validation, terminal complete/cancel/fail handling, idle-owner release hooks) into a focused service.
- Keep `CommandExecutor` orchestration-focused by delegating terminal lifecycle and idle-owner release hooks through dedicated motor terminal service boundaries.
- Validate via focused motor terminal tests plus phase guard/signoff checks, then record `PHASE 17 COMPLETE`.

Phase 17 implementation files:
- `docs/NATIVE_CLIENT_PHASE17_MOTOR_TERMINAL_DECOMPOSITION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase17_motor_terminal_decomposition.py`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- `runelite-plugin/src/main/java/com/xptool/executor/MotorProgramTerminalService.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 18 (Motor Dispatch Admission Decomposition)
- Continue reducing `CommandExecutor` runtime ownership by extracting motor dispatch admission and cooldown gating ownership into a focused service boundary.
- Keep `CommandExecutor` orchestration-focused by delegating motor owner admission, cooldown readiness, and gesture scheduling through dedicated motor dispatch admission service boundaries.
- Validate via focused motor dispatch admission tests plus phase guard/signoff checks, then record `PHASE 18 COMPLETE`.

Phase 18 implementation files:
- `docs/NATIVE_CLIENT_PHASE18_MOTOR_DISPATCH_ADMISSION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase18_motor_dispatch_admission.py`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- `runelite-plugin/src/main/java/com/xptool/executor/MotorDispatchAdmissionService.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 19 (Motor Dispatch Context Decomposition)
- Continue reducing `CommandExecutor` runtime ownership by extracting active motor owner/click-type context ownership into a focused service boundary.
- Keep `CommandExecutor` orchestration-focused by delegating context push/pop transitions and active-context reads through dedicated motor dispatch context service boundaries.
- Validate via focused motor dispatch context tests plus phase guard/signoff checks, then record `PHASE 19 COMPLETE`.

Phase 19 implementation files:
- `docs/NATIVE_CLIENT_PHASE19_MOTOR_DISPATCH_CONTEXT_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase19_motor_dispatch_context.py`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- `runelite-plugin/src/main/java/com/xptool/executor/MotorDispatchContextService.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 20 (Interaction Click Telemetry Decomposition)
- Continue reducing `CommandExecutor` runtime ownership by extracting interaction-click telemetry/state ownership into a focused service boundary.
- Keep `CommandExecutor` orchestration-focused by delegating click serial/freshness/anchor state and telemetry payload assembly through dedicated interaction click telemetry service boundaries.
- Validate via focused interaction click telemetry tests plus phase guard/signoff checks, then record `PHASE 20 COMPLETE`.

Phase 20 implementation files:
- `docs/NATIVE_CLIENT_PHASE20_INTERACTION_CLICK_TELEMETRY_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase20_interaction_click_telemetry.py`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- `runelite-plugin/src/main/java/com/xptool/executor/InteractionClickTelemetryService.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 21 (Interaction Anchor Resolution Decomposition)
- Continue reducing `CommandExecutor` runtime ownership by extracting tile-object interaction anchor resolution ownership into a focused service boundary.
- Keep `CommandExecutor` orchestration-focused by delegating tile-object clickbox/fallback anchor conversion through dedicated interaction anchor resolver service boundaries.
- Validate via focused interaction anchor resolution tests plus phase guard/signoff checks, then record `PHASE 21 COMPLETE`.

Phase 21 implementation files:
- `docs/NATIVE_CLIENT_PHASE21_INTERACTION_ANCHOR_RESOLUTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase21_interaction_anchor_resolution.py`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- `runelite-plugin/src/main/java/com/xptool/executor/InteractionAnchorResolverService.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 22 (Interaction Click Event Packaging Decomposition)
- Continue reducing `CommandExecutor` runtime ownership by extracting settle-eligible interaction-click event packaging ownership into focused interaction telemetry/event contract components.
- Keep `CommandExecutor` orchestration-focused by delegating click-event construction to telemetry services and forwarding only typed runtime event objects.
- Validate via focused click-event contract tests plus phase guard/signoff checks, then record `PHASE 22 COMPLETE`.

Phase 22 implementation files:
- `docs/NATIVE_CLIENT_PHASE22_INTERACTION_CLICK_EVENT_PACKAGING_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase22_interaction_click_event_packaging.py`
- `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- `runelite-plugin/src/main/java/com/xptool/executor/InteractionClickTelemetryService.java`
- `runelite-plugin/src/main/java/com/xptool/executor/InteractionClickEvent.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 23 (Interaction Post-Click Settle Decomposition)
- Continue reducing session runtime ownership by extracting interaction post-click settle scheduling/state ownership from `InteractionSession` into a focused session runtime service boundary.
- Keep `InteractionSession` orchestration-focused by delegating settle scheduling/readiness/execution state transitions through dedicated post-click settle runtime service boundaries.
- Validate via focused post-click settle tests plus phase guard/signoff checks, then record `PHASE 23 COMPLETE`.

Phase 23 implementation files:
- `docs/NATIVE_CLIENT_PHASE23_INTERACTION_POST_CLICK_SETTLE_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase23_interaction_post_click_settle.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionPostClickSettleService.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 24 (Interaction Session Ownership Decomposition)
- Continue reducing session runtime ownership by extracting interaction session registration and motor-ownership orchestration from `InteractionSession` into a focused session runtime service boundary.
- Keep `InteractionSession` orchestration-focused by delegating ownership tick branching, registration gates, and motor-ownership release/acquire orchestration through dedicated session ownership runtime service boundaries.
- Validate via focused interaction-session ownership tests plus phase guard/signoff checks, then record `PHASE 24 COMPLETE`.

Phase 24 implementation files:
- `docs/NATIVE_CLIENT_PHASE24_INTERACTION_SESSION_OWNERSHIP_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase24_interaction_session_ownership.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionOwnershipService.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 25 (Interaction Session Host-Factory Decomposition)
- Continue reducing `InteractionSession` constructor ownership by extracting session-host assembly wiring into a focused host-factory boundary.
- Keep `InteractionSession` orchestration-focused by delegating interaction settle/ownership host construction through dedicated session host-factory methods.
- Validate via focused session regression tests plus phase guard/signoff checks, then record `PHASE 25 COMPLETE`.

Phase 25 implementation files:
- `docs/NATIVE_CLIENT_PHASE25_INTERACTION_SESSION_HOST_FACTORY_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase25_interaction_session_host_factory.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 26 (Interaction Session Command Router Decomposition)
- Continue reducing `InteractionSession` runtime ownership by extracting interaction-session command support/dispatch routing into a focused command-router boundary.
- Keep `InteractionSession` orchestration-focused by delegating command-type support checks and command dispatch routing through dedicated router service methods.
- Validate via focused interaction command router tests plus phase guard/signoff checks, then record `PHASE 26 COMPLETE`.

Phase 26 implementation files:
- `docs/NATIVE_CLIENT_PHASE26_INTERACTION_SESSION_COMMAND_ROUTER_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase26_interaction_session_command_router.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionCommandRouter.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 27 (Interaction Session Registration Decomposition)
- Continue reducing `InteractionSession` runtime ownership by extracting interaction-session registration lifecycle state and mutation ownership into a focused registration service boundary.
- Keep `InteractionSession` orchestration-focused by delegating registration lifecycle transitions (`ensure`/`clear`) through dedicated registration service methods.
- Validate via focused interaction-session registration tests plus phase guard/signoff checks, then record `PHASE 27 COMPLETE`.

Phase 27 implementation files:
- `docs/NATIVE_CLIENT_PHASE27_INTERACTION_SESSION_REGISTRATION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase27_interaction_session_registration.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRegistrationService.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionCommandRouter.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 28 (Interaction Session Motor-Ownership Adapter Decomposition)
- Continue reducing `InteractionSession` runtime ownership by extracting remaining interaction-session motor-ownership adapter delegation into a focused motor-ownership service boundary.
- Keep `InteractionSession` orchestration-focused by delegating motor acquire/release adapters through dedicated motor-ownership service methods and host-factory wiring.
- Validate via focused interaction-session motor-ownership tests plus phase guard/signoff checks, then record `PHASE 28 COMPLETE`.

Phase 28 implementation files:
- `docs/NATIVE_CLIENT_PHASE28_INTERACTION_SESSION_MOTOR_OWNERSHIP_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase28_interaction_session_motor_ownership.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionMotorOwnershipService.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRegistrationService.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionCommandRouter.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 29 (Interaction Session Click-Event Intake Decomposition)
- Continue reducing `InteractionSession` runtime ownership by extracting interaction-session click-event intake delegation into a focused session runtime service boundary.
- Keep `InteractionSession` orchestration-focused by delegating interaction click-event intake through dedicated click-event service methods.
- Validate via focused interaction-session click-event tests plus phase guard/signoff checks, then record `PHASE 29 COMPLETE`.

Phase 29 implementation files:
- `docs/NATIVE_CLIENT_PHASE29_INTERACTION_SESSION_CLICK_EVENT_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase29_interaction_session_click_event.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionClickEventService.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionPostClickSettleService.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 30 (Interaction Session Shutdown Decomposition)
- Continue reducing `InteractionSession` runtime ownership by extracting interaction-session shutdown lifecycle ownership into a focused session runtime service boundary.
- Keep `InteractionSession` orchestration-focused by delegating shutdown lifecycle sequencing through dedicated shutdown service methods.
- Validate via focused interaction-session shutdown tests plus phase guard/signoff checks, then record `PHASE 30 COMPLETE`.

Phase 30 implementation files:
- `docs/NATIVE_CLIENT_PHASE30_INTERACTION_SESSION_SHUTDOWN_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase30_interaction_session_shutdown.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionShutdownService.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionClickEventService.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionMotorOwnershipService.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRegistrationService.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionPostClickSettleService.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 31 (Interaction Session Shutdown Host-Factory Decomposition)
- Continue reducing `InteractionSession` constructor ownership by extracting shutdown host wiring assembly into a focused session host-factory boundary.
- Keep `InteractionSession` orchestration-focused by consuming shutdown host factory methods instead of inline host adapters.
- Validate via focused shutdown host-factory tests plus phase guard/signoff checks, then record `PHASE 31 COMPLETE`.

Phase 31 implementation files:
- `docs/NATIVE_CLIENT_PHASE31_INTERACTION_SESSION_SHUTDOWN_HOST_FACTORY_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase31_interaction_session_shutdown_host_factory.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionShutdownService.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 32 (Interaction Session Click-Event Host-Factory Decomposition)
- Continue reducing `InteractionSession` constructor ownership by extracting click-event host wiring assembly into a focused session host-factory boundary.
- Keep `InteractionSession` orchestration-focused by consuming click-event host factory methods instead of inline host adapters.
- Validate via focused click-event host-factory tests plus phase guard/signoff checks, then record `PHASE 32 COMPLETE`.

Phase 32 implementation files:
- `docs/NATIVE_CLIENT_PHASE32_INTERACTION_SESSION_CLICK_EVENT_HOST_FACTORY_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase32_interaction_session_click_event_host_factory.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionClickEventService.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 33 (Interaction Session Ownership Service Factory Decomposition)
- Continue reducing `InteractionSession` constructor ownership by extracting ownership-service construction and host assembly into focused host-factory boundaries.
- Keep `InteractionSession` orchestration-focused by consuming ownership-service factory methods instead of inline ownership-service construction.
- Validate via focused ownership-host delegate tests plus phase guard/signoff checks, then record `PHASE 33 COMPLETE`.

Phase 33 implementation files:
- `docs/NATIVE_CLIENT_PHASE33_INTERACTION_SESSION_OWNERSHIP_SERVICE_FACTORY_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase33_interaction_session_ownership_service_factory.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionOwnershipService.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 34 (Interaction Session Motor-Ownership Service Factory Decomposition)
- Continue reducing `InteractionSession` constructor ownership by extracting motor-ownership service construction and host assembly into focused host-factory boundaries.
- Keep `InteractionSession` orchestration-focused by consuming motor-ownership service factory methods instead of inline motor-ownership service construction.
- Validate via focused motor-ownership-host delegate tests plus phase guard/signoff checks, then record `PHASE 34 COMPLETE`.

Phase 34 implementation files:
- `docs/NATIVE_CLIENT_PHASE34_INTERACTION_SESSION_MOTOR_OWNERSHIP_SERVICE_FACTORY_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase34_interaction_session_motor_ownership_service_factory.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionMotorOwnershipService.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 35 (Interaction Session Registration Service Factory Decomposition)
- Continue reducing `InteractionSession` constructor ownership by extracting registration-service construction and host assembly into focused host-factory boundaries.
- Keep `InteractionSession` orchestration-focused by consuming registration-service factory methods instead of inline registration-service construction.
- Validate via focused registration-host delegate tests plus phase guard/signoff checks, then record `PHASE 35 COMPLETE`.

Phase 35 implementation files:
- `docs/NATIVE_CLIENT_PHASE35_INTERACTION_SESSION_REGISTRATION_SERVICE_FACTORY_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase35_interaction_session_registration_service_factory.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRegistrationService.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 36 (Interaction Session Post-Click Settle Service Factory Decomposition)
- Continue reducing `InteractionSession` constructor ownership by extracting post-click-settle service construction and host assembly into focused host-factory boundaries.
- Keep `InteractionSession` orchestration-focused by consuming post-click-settle service factory methods instead of inline post-click-settle service construction.
- Validate via focused post-click-settle host delegate tests plus phase guard/signoff checks, then record `PHASE 36 COMPLETE`.

Phase 36 implementation files:
- `docs/NATIVE_CLIENT_PHASE36_INTERACTION_SESSION_POST_CLICK_SETTLE_SERVICE_FACTORY_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase36_interaction_session_post_click_settle_service_factory.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 37 (Interaction Session Click-Event Service Factory Decomposition)
- Continue reducing `InteractionSession` constructor ownership by extracting click-event service construction and host assembly into focused host-factory boundaries.
- Keep `InteractionSession` orchestration-focused by consuming click-event service factory methods instead of inline click-event service construction.
- Validate via focused click-event service-factory delegate tests plus phase guard/signoff checks, then record `PHASE 37 COMPLETE`.

Phase 37 implementation files:
- `docs/NATIVE_CLIENT_PHASE37_INTERACTION_SESSION_CLICK_EVENT_SERVICE_FACTORY_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase37_interaction_session_click_event_service_factory.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 38 (Interaction Session Shutdown Service Factory Decomposition)
- Continue reducing `InteractionSession` constructor ownership by extracting shutdown service construction and host assembly into focused host-factory boundaries.
- Keep `InteractionSession` orchestration-focused by consuming shutdown service factory methods instead of inline shutdown service construction.
- Validate via focused shutdown service-factory delegate tests plus phase guard/signoff checks, then record `PHASE 38 COMPLETE`.

Phase 38 implementation files:
- `docs/NATIVE_CLIENT_PHASE38_INTERACTION_SESSION_SHUTDOWN_SERVICE_FACTORY_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase38_interaction_session_shutdown_service_factory.py`
- `scripts/verify_phase31_interaction_session_shutdown_host_factory.py`
- `scripts/verify_phase30_interaction_session_shutdown.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 39 (Interaction Session Command Router Service Factory Decomposition)
- Continue reducing `InteractionSession` constructor ownership by extracting command-router service construction into focused host-factory boundaries.
- Keep `InteractionSession` orchestration-focused by consuming command-router service factory methods instead of inline command-router construction.
- Validate via focused command-router service-factory delegate tests plus phase guard/signoff checks, then record `PHASE 39 COMPLETE`.

Phase 39 implementation files:
- `docs/NATIVE_CLIENT_PHASE39_INTERACTION_SESSION_COMMAND_ROUTER_SERVICE_FACTORY_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase39_interaction_session_command_router_service_factory.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 40 (Interaction Session Ownership Service Host Decomposition)
- Continue decomposing session runtime assembly by introducing an explicit host-based ownership-service construction boundary in `InteractionSessionHostFactory`.
- Keep `InteractionSession` orchestration-focused by preserving ownership service factory usage while reducing host factory assembly coupling.
- Validate via focused ownership service host-boundary tests plus phase guard/signoff checks, then record `PHASE 40 COMPLETE`.

Phase 40 implementation files:
- `docs/NATIVE_CLIENT_PHASE40_INTERACTION_SESSION_OWNERSHIP_SERVICE_HOST_DECOMPOSITION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase40_interaction_session_ownership_service_host_decomposition.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 41 (Interaction Session Post-Click Settle Host Decomposition)
- Continue decomposing session runtime assembly by introducing an explicit host-based post-click-settle service construction boundary in `InteractionSessionHostFactory`.
- Keep `InteractionSession` orchestration-focused by preserving post-click-settle service factory usage while reducing host factory assembly coupling.
- Validate via focused post-click-settle service host-boundary tests plus phase guard/signoff checks, then record `PHASE 41 COMPLETE`.

Phase 41 implementation files:
- `docs/NATIVE_CLIENT_PHASE41_INTERACTION_SESSION_POST_CLICK_SETTLE_HOST_DECOMPOSITION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase41_interaction_session_post_click_settle_host_decomposition.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 42 (Interaction Session Registration Host Decomposition)
- Continue decomposing session runtime assembly by introducing an explicit host-based registration service construction boundary in `InteractionSessionHostFactory`.
- Keep `InteractionSession` orchestration-focused by preserving registration service factory usage while reducing host factory assembly coupling.
- Validate via focused registration service host-boundary tests plus phase guard/signoff checks, then record `PHASE 42 COMPLETE`.

Phase 42 implementation files:
- `docs/NATIVE_CLIENT_PHASE42_INTERACTION_SESSION_REGISTRATION_HOST_DECOMPOSITION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase42_interaction_session_registration_host_decomposition.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 43 (Interaction Session Motor Ownership Host Decomposition)
- Continue decomposing session runtime assembly by introducing an explicit host-based motor-ownership service construction boundary in `InteractionSessionHostFactory`.
- Keep `InteractionSession` orchestration-focused by preserving motor-ownership service factory usage while reducing host factory assembly coupling.
- Validate via focused motor-ownership service host-boundary tests plus phase guard/signoff checks, then record `PHASE 43 COMPLETE`.

Phase 43 implementation files:
- `docs/NATIVE_CLIENT_PHASE43_INTERACTION_SESSION_MOTOR_OWNERSHIP_HOST_DECOMPOSITION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase43_interaction_session_motor_ownership_host_decomposition.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 44 (Interaction Session Click-Event Host Decomposition)
- Continue decomposing session runtime assembly by introducing an explicit host-based click-event service construction boundary in `InteractionSessionHostFactory`.
- Keep `InteractionSession` orchestration-focused by preserving click-event service factory usage while reducing host factory assembly coupling.
- Validate via focused click-event service host-boundary tests plus phase guard/signoff checks, then record `PHASE 44 COMPLETE`.

Phase 44 implementation files:
- `docs/NATIVE_CLIENT_PHASE44_INTERACTION_SESSION_CLICK_EVENT_HOST_DECOMPOSITION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase44_interaction_session_click_event_host_decomposition.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 45 (Interaction Session Shutdown Host Decomposition)
- Continue decomposing session runtime assembly by introducing an explicit host-based shutdown service construction boundary in `InteractionSessionHostFactory`.
- Keep `InteractionSession` orchestration-focused by preserving shutdown service factory usage while reducing host factory assembly coupling.
- Validate via focused shutdown service host-boundary tests plus phase guard/signoff checks, then record `PHASE 45 COMPLETE`.

Phase 45 implementation files:
- `docs/NATIVE_CLIENT_PHASE45_INTERACTION_SESSION_SHUTDOWN_HOST_DECOMPOSITION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase45_interaction_session_shutdown_host_decomposition.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 46 (Interaction Session Command Router Host Decomposition)
- Continue decomposing session runtime assembly by introducing an explicit delegate-based command-router host construction boundary in `InteractionSessionHostFactory`.
- Keep `InteractionSession` orchestration-focused by preserving command-router host factory usage while reducing host factory assembly coupling.
- Validate via focused command-router host-boundary tests plus phase guard/signoff checks, then record `PHASE 46 COMPLETE`.

Phase 46 implementation files:
- `docs/NATIVE_CLIENT_PHASE46_INTERACTION_SESSION_COMMAND_ROUTER_HOST_DECOMPOSITION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase46_interaction_session_command_router_host_decomposition.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 47 (Interaction Session Command Router Host-Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting command-router host delegate assembly into focused factory ownership.
- Keep `InteractionSessionHostFactory` orchestration-focused by preserving command-router method signatures while delegating host delegate wiring to focused component boundaries.
- Validate via focused command-router host-factory tests plus phase guard/signoff checks, then record `PHASE 47 COMPLETE`.

Phase 47 implementation files:
- `docs/NATIVE_CLIENT_PHASE47_INTERACTION_SESSION_COMMAND_ROUTER_HOST_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase47_interaction_session_command_router_host_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionCommandRouterHostFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterHostFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 48 (Interaction Session Shutdown Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting shutdown service/host assembly into focused factory ownership.
- Keep `InteractionSessionHostFactory` orchestration-focused by preserving shutdown method signatures while delegating shutdown assembly wiring to focused component boundaries.
- Validate via focused shutdown factory tests plus phase guard/signoff checks, then record `PHASE 48 COMPLETE`.

Phase 48 implementation files:
- `docs/NATIVE_CLIENT_PHASE48_INTERACTION_SESSION_SHUTDOWN_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase48_interaction_session_shutdown_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionShutdownFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterHostFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 49 (Interaction Session Registration Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting registration host assembly into focused factory ownership.
- Keep `InteractionSessionHostFactory` orchestration-focused by preserving registration method signatures while delegating registration host assembly wiring to focused component boundaries.
- Validate via focused registration factory tests plus phase guard/signoff checks, then record `PHASE 49 COMPLETE`.

Phase 49 implementation files:
- `docs/NATIVE_CLIENT_PHASE49_INTERACTION_SESSION_REGISTRATION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase49_interaction_session_registration_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRegistrationFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterHostFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 50 (Interaction Session Motor-Ownership Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting motor-ownership host assembly into focused factory ownership.
- Keep `InteractionSessionHostFactory` orchestration-focused by preserving motor-ownership method signatures while delegating motor-ownership host assembly wiring to focused component boundaries.
- Validate via focused motor-ownership factory tests plus phase guard/signoff checks, then record `PHASE 50 COMPLETE`.

Phase 50 implementation files:
- `docs/NATIVE_CLIENT_PHASE50_INTERACTION_SESSION_MOTOR_OWNERSHIP_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase50_interaction_session_motor_ownership_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionMotorOwnershipFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterHostFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 51 (Interaction Session Click-Event Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting click-event service assembly into focused factory ownership.
- Keep `InteractionSessionHostFactory` orchestration-focused by preserving click-event method signatures while delegating click-event service assembly wiring to focused component boundaries.
- Validate via focused click-event factory tests plus phase guard/signoff checks, then record `PHASE 51 COMPLETE`.

Phase 51 implementation files:
- `docs/NATIVE_CLIENT_PHASE51_INTERACTION_SESSION_CLICK_EVENT_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase51_interaction_session_click_event_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionClickEventFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterHostFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceHostDecompositionTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java`
- `runelite-plugin/src/test/java/com/xptool/executor/PendingMoveTelemetryServiceTest.java`

## Phase 52 (Interaction Session Post-Click Settle Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting post-click-settle service/host assembly into focused factory ownership.
- Keep `InteractionSessionHostFactory` orchestration-focused by preserving post-click-settle method signatures while delegating focused assembly wiring to dedicated component boundaries.
- Validate via focused post-click-settle factory tests plus phase guard/signoff checks, then record `PHASE 52 COMPLETE`.

Phase 52 implementation files:
- `docs/NATIVE_CLIENT_PHASE52_INTERACTION_SESSION_POST_CLICK_SETTLE_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase52_interaction_session_post_click_settle_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionPostClickSettleFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleFactoryTest.java`

## Phase 53 (Interaction Session Ownership Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting ownership host assembly into focused factory ownership.
- Keep `InteractionSessionHostFactory` orchestration-focused by preserving ownership-service compatibility method signatures while delegating ownership host assembly wiring to dedicated component boundaries.
- Validate via focused ownership factory tests plus phase guard/signoff checks, then record `PHASE 53 COMPLETE`.

Phase 53 implementation files:
- `docs/NATIVE_CLIENT_PHASE53_INTERACTION_SESSION_OWNERSHIP_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase53_interaction_session_ownership_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionOwnershipFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipFactoryTest.java`

## Phase 54 (Interaction Session Host-Factory Focused-Factory Consolidation)
- Consolidate `InteractionSessionHostFactory` as an orchestration-only compatibility boundary by aligning focused-factory delegation across post-click-settle, ownership, command-router, click-event, registration, motor-ownership, and shutdown assembly seams.
- Preserve compatibility signatures/strings required by prior phase verifiers while keeping new ownership in dedicated focused components.
- Validate via consolidation guard/signoff checks, then record `PHASE 54 COMPLETE`.

Phase 54 implementation files:
- `docs/NATIVE_CLIENT_PHASE54_INTERACTION_SESSION_HOST_FACTORY_CONSOLIDATION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase54_interaction_session_host_factory_consolidation.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`

## Phase 55 (Interaction Session Macro Pass A Signoff)
- Complete macro-pass signoff for phases 52-55 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 55 COMPLETE` after signoff gates pass.

Phase 55 implementation files:
- `docs/NATIVE_CLIENT_PHASE55_INTERACTION_SESSION_MACRO_PASS_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase55_interaction_session_macro_pass_signoff.py`
- `scripts/verify_phase54_interaction_session_host_factory_consolidation.py`
- `scripts/verify_phase53_interaction_session_ownership_factory_extraction.py`
- `scripts/verify_phase52_interaction_session_post_click_settle_factory_extraction.py`

## Phase 56 (Interaction Session Command-Router Service Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting command-router service-from-host assembly into focused factory ownership.
- Keep `InteractionSessionHostFactory` orchestration-focused by preserving command-router method signatures while delegating service-from-host assembly wiring to dedicated component boundaries.
- Validate via focused command-router factory tests plus phase guard/signoff checks, then record `PHASE 56 COMPLETE`.

Phase 56 implementation files:
- `docs/NATIVE_CLIENT_PHASE56_INTERACTION_SESSION_COMMAND_ROUTER_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase56_interaction_session_command_router_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionCommandRouterFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterFactoryTest.java`

## Phase 57 (Interaction Session Click-Event Host Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting click-event host assembly into focused factory ownership.
- Keep `InteractionSessionHostFactory` orchestration-focused by preserving click-event compatibility delegate wrappers while delegating click-event host assembly wiring to dedicated component boundaries.
- Validate via focused click-event factory tests plus phase guard/signoff checks, then record `PHASE 57 COMPLETE`.

Phase 57 implementation files:
- `docs/NATIVE_CLIENT_PHASE57_INTERACTION_SESSION_CLICK_EVENT_HOST_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase57_interaction_session_click_event_host_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionClickEventFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventFactoryTest.java`

## Phase 58 (Interaction Session Host-Factory Consolidation B)
- Consolidate `InteractionSessionHostFactory` by enforcing focused-factory delegation for command-router service and click-event host seams.
- Preserve compatibility wrappers required by prior phase gates while reducing direct service-construction ownership in host-factory boundaries.
- Validate via consolidation guard/signoff checks, then record `PHASE 58 COMPLETE`.

Phase 58 implementation files:
- `docs/NATIVE_CLIENT_PHASE58_INTERACTION_SESSION_HOST_FACTORY_CONSOLIDATION_B_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase58_interaction_session_host_factory_consolidation_b.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`

## Phase 59 (Interaction Session Macro Pass B Signoff)
- Complete macro-pass signoff for phases 56-59 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 59 COMPLETE` after signoff gates pass.

Phase 59 implementation files:
- `docs/NATIVE_CLIENT_PHASE59_INTERACTION_SESSION_MACRO_PASS_B_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase59_interaction_session_macro_pass_b_signoff.py`
- `scripts/verify_phase58_interaction_session_host_factory_consolidation_b.py`
- `scripts/verify_phase57_interaction_session_click_event_host_factory_extraction.py`
- `scripts/verify_phase56_interaction_session_command_router_factory_extraction.py`

## Phase 60 (Interaction Session Motor-Ownership Delegate Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting motor-ownership delegate-host assembly ownership into focused factory boundaries.
- Keep `InteractionSessionHostFactory` compatibility delegate wrapper method signatures while delegating motor-ownership delegate-host construction ownership to focused components.
- Validate via focused motor-ownership factory tests plus phase guard/signoff checks, then record `PHASE 60 COMPLETE`.

Phase 60 implementation files:
- `docs/NATIVE_CLIENT_PHASE60_INTERACTION_SESSION_MOTOR_OWNERSHIP_DELEGATE_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase60_interaction_session_motor_ownership_delegate_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionMotorOwnershipFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipFactoryTest.java`

## Phase 61 (Interaction Session Shutdown Delegate Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting shutdown delegate-host assembly ownership into focused factory boundaries.
- Keep `InteractionSessionHostFactory` shutdown compatibility delegate wrapper method signatures and legacy delegate strings required by prior phase gates.
- Validate via focused shutdown factory tests plus phase guard/signoff checks, then record `PHASE 61 COMPLETE`.

Phase 61 implementation files:
- `docs/NATIVE_CLIENT_PHASE61_INTERACTION_SESSION_SHUTDOWN_DELEGATE_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase61_interaction_session_shutdown_delegate_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionShutdownFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownFactoryTest.java`

## Phase 62 (Interaction Session Host-Factory Consolidation C)
- Consolidate `InteractionSessionHostFactory` by aligning focused-factory delegate ownership for motor-ownership and shutdown seams while preserving compatibility wrappers.
- Preserve prior-phase compatibility delegate strings and service-construction compatibility boundaries.
- Validate via consolidation guard/signoff checks, then record `PHASE 62 COMPLETE`.

Phase 62 implementation files:
- `docs/NATIVE_CLIENT_PHASE62_INTERACTION_SESSION_HOST_FACTORY_CONSOLIDATION_C_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase62_interaction_session_host_factory_consolidation_c.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionMotorOwnershipFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionShutdownFactory.java`

## Phase 63 (Interaction Session Macro Pass C Signoff)
- Complete macro-pass signoff for phases 60-63 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 63 COMPLETE` after signoff gates pass.

Phase 63 implementation files:
- `docs/NATIVE_CLIENT_PHASE63_INTERACTION_SESSION_MACRO_PASS_C_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase63_interaction_session_macro_pass_c_signoff.py`
- `scripts/verify_phase62_interaction_session_host_factory_consolidation_c.py`
- `scripts/verify_phase61_interaction_session_shutdown_delegate_factory_extraction.py`
- `scripts/verify_phase60_interaction_session_motor_ownership_delegate_factory_extraction.py`

## Phase 64 (Interaction Session Registration Service-From-Host Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting registration service-from-host assembly ownership into focused factory boundaries.
- Keep `InteractionSessionHostFactory` registration service compatibility wrapper signatures while delegating service-from-host assembly ownership to focused components.
- Validate via focused registration factory tests plus phase guard/signoff checks, then record `PHASE 64 COMPLETE`.

Phase 64 implementation files:
- `docs/NATIVE_CLIENT_PHASE64_INTERACTION_SESSION_REGISTRATION_SERVICE_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase64_interaction_session_registration_service_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRegistrationFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationFactoryTest.java`

## Phase 65 (Interaction Session Ownership Service-From-Host Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting ownership service-from-host assembly ownership into focused factory boundaries.
- Keep `InteractionSessionHostFactory` ownership service compatibility wrapper signatures while delegating service-from-host assembly ownership to focused components.
- Validate via focused ownership factory tests plus phase guard/signoff checks, then record `PHASE 65 COMPLETE`.

Phase 65 implementation files:
- `docs/NATIVE_CLIENT_PHASE65_INTERACTION_SESSION_OWNERSHIP_SERVICE_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase65_interaction_session_ownership_service_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionOwnershipFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipFactoryTest.java`

## Phase 66 (Interaction Session Host-Factory Consolidation D)
- Consolidate `InteractionSessionHostFactory` service-from-host seams by routing registration, motor-ownership, and ownership service construction through focused factory boundaries.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 66 COMPLETE`.

Phase 66 implementation files:
- `docs/NATIVE_CLIENT_PHASE66_INTERACTION_SESSION_HOST_FACTORY_CONSOLIDATION_D_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase66_interaction_session_host_factory_consolidation_d.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRegistrationFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionMotorOwnershipFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionOwnershipFactory.java`

## Phase 67 (Interaction Session Macro Pass D Signoff)
- Complete macro-pass signoff for phases 64-67 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 67 COMPLETE` after signoff gates pass.

Phase 67 implementation files:
- `docs/NATIVE_CLIENT_PHASE67_INTERACTION_SESSION_MACRO_PASS_D_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase67_interaction_session_macro_pass_d_signoff.py`
- `scripts/verify_phase66_interaction_session_host_factory_consolidation_d.py`
- `scripts/verify_phase65_interaction_session_ownership_service_factory_extraction.py`
- `scripts/verify_phase64_interaction_session_registration_service_factory_extraction.py`

## Phase 68 (Interaction Session Click-Event Delegate-Host Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting click-event delegate-host assembly ownership into focused factory boundaries.
- Keep `InteractionSessionHostFactory` click-event delegate-host compatibility method signatures while delegating assembly ownership to focused components.
- Validate via focused click-event host tests plus phase guard/signoff checks, then record `PHASE 68 COMPLETE`.

Phase 68 implementation files:
- `docs/NATIVE_CLIENT_PHASE68_INTERACTION_SESSION_CLICK_EVENT_DELEGATE_HOST_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase68_interaction_session_click_event_delegate_host_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java`

## Phase 69 (Interaction Session Shutdown Delegate-Host Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting shutdown delegate-host assembly ownership into focused factory boundaries.
- Keep `InteractionSessionHostFactory` shutdown delegate-host compatibility method signatures while delegating assembly ownership to focused components.
- Validate via focused shutdown host tests plus phase guard/signoff checks, then record `PHASE 69 COMPLETE`.

Phase 69 implementation files:
- `docs/NATIVE_CLIENT_PHASE69_INTERACTION_SESSION_SHUTDOWN_DELEGATE_HOST_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase69_interaction_session_shutdown_delegate_host_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java`

## Phase 70 (Interaction Session Host-Factory Consolidation E)
- Consolidate `InteractionSessionHostFactory` delegate-host seams by routing click-event and shutdown delegate-host construction through focused factory boundaries.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 70 COMPLETE`.

Phase 70 implementation files:
- `docs/NATIVE_CLIENT_PHASE70_INTERACTION_SESSION_HOST_FACTORY_CONSOLIDATION_E_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase70_interaction_session_host_factory_consolidation_e.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`

## Phase 71 (Interaction Session Macro Pass E Signoff)
- Complete macro-pass signoff for phases 68-71 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 71 COMPLETE` after signoff gates pass.

Phase 71 implementation files:
- `docs/NATIVE_CLIENT_PHASE71_INTERACTION_SESSION_MACRO_PASS_E_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase71_interaction_session_macro_pass_e_signoff.py`
- `scripts/verify_phase70_interaction_session_host_factory_consolidation_e.py`
- `scripts/verify_phase69_interaction_session_shutdown_delegate_host_factory_extraction.py`
- `scripts/verify_phase68_interaction_session_click_event_delegate_host_factory_extraction.py`

## Phase 72 (Interaction Session Click-Event Service Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting click-event service assembly ownership into focused factory boundaries.
- Keep `InteractionSessionHostFactory` click-event service compatibility method signatures while delegating assembly ownership to focused components.
- Validate via focused click-event factory tests plus phase guard/signoff checks, then record `PHASE 72 COMPLETE`.

Phase 72 implementation files:
- `docs/NATIVE_CLIENT_PHASE72_INTERACTION_SESSION_CLICK_EVENT_SERVICE_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase72_interaction_session_click_event_service_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionClickEventFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventFactoryTest.java`

## Phase 73 (Interaction Session Shutdown Service Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting shutdown service assembly ownership into focused factory boundaries.
- Keep `InteractionSessionHostFactory` shutdown service compatibility method signatures while delegating assembly ownership to focused components.
- Validate via focused shutdown factory tests plus phase guard/signoff checks, then record `PHASE 73 COMPLETE`.

Phase 73 implementation files:
- `docs/NATIVE_CLIENT_PHASE73_INTERACTION_SESSION_SHUTDOWN_SERVICE_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase73_interaction_session_shutdown_service_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownFactoryTest.java`

## Phase 74 (Interaction Session Host-Factory Consolidation F)
- Consolidate `InteractionSessionHostFactory` service seams by routing click-event and shutdown service construction through focused factory boundaries.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 74 COMPLETE`.

Phase 74 implementation files:
- `docs/NATIVE_CLIENT_PHASE74_INTERACTION_SESSION_HOST_FACTORY_CONSOLIDATION_F_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase74_interaction_session_host_factory_consolidation_f.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`

## Phase 75 (Interaction Session Macro Pass F Signoff)
- Complete macro-pass signoff for phases 72-75 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 75 COMPLETE` after signoff gates pass.

Phase 75 implementation files:
- `docs/NATIVE_CLIENT_PHASE75_INTERACTION_SESSION_MACRO_PASS_F_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase75_interaction_session_macro_pass_f_signoff.py`
- `scripts/verify_phase74_interaction_session_host_factory_consolidation_f.py`
- `scripts/verify_phase73_interaction_session_shutdown_service_factory_extraction.py`
- `scripts/verify_phase72_interaction_session_click_event_service_factory_extraction.py`

## Phase 76 (Interaction Session Registration Service Composite Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting registration composite service assembly ownership into focused factory boundaries.
- Keep `InteractionSessionHostFactory` registration service compatibility method signatures while delegating assembly ownership to focused components.
- Validate via focused registration factory tests plus phase guard/signoff checks, then record `PHASE 76 COMPLETE`.

Phase 76 implementation files:
- `docs/NATIVE_CLIENT_PHASE76_INTERACTION_SESSION_REGISTRATION_SERVICE_COMPOSITE_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase76_interaction_session_registration_service_composite_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRegistrationFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationServiceFactoryTest.java`

## Phase 77 (Interaction Session Motor Service Composite Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting motor composite service assembly ownership into focused factory boundaries.
- Keep `InteractionSessionHostFactory` motor service compatibility method signatures while delegating assembly ownership to focused components.
- Validate via focused motor factory tests plus phase guard/signoff checks, then record `PHASE 77 COMPLETE`.

Phase 77 implementation files:
- `docs/NATIVE_CLIENT_PHASE77_INTERACTION_SESSION_MOTOR_SERVICE_COMPOSITE_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase77_interaction_session_motor_service_composite_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionMotorOwnershipFactory.java`

## Phase 78 (Interaction Session Host-Factory Consolidation G)
- Consolidate `InteractionSessionHostFactory` composite service seams by routing registration and motor service construction through focused factory boundaries.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 78 COMPLETE`.

Phase 78 implementation files:
- `docs/NATIVE_CLIENT_PHASE78_INTERACTION_SESSION_HOST_FACTORY_CONSOLIDATION_G_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase78_interaction_session_host_factory_consolidation_g.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`

## Phase 79 (Interaction Session Macro Pass G Signoff)
- Complete macro-pass signoff for phases 76-79 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 79 COMPLETE` after signoff gates pass.

Phase 79 implementation files:
- `docs/NATIVE_CLIENT_PHASE79_INTERACTION_SESSION_MACRO_PASS_G_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase79_interaction_session_macro_pass_g_signoff.py`
- `scripts/verify_phase78_interaction_session_host_factory_consolidation_g.py`
- `scripts/verify_phase77_interaction_session_motor_service_composite_factory_extraction.py`
- `scripts/verify_phase76_interaction_session_registration_service_composite_factory_extraction.py`

## Phase 80 (Interaction Session Post-Click Settle Service Composite Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting post-click-settle composite service assembly ownership into focused factory boundaries.
- Keep `InteractionSessionHostFactory` post-click-settle service compatibility method signatures while delegating assembly ownership to focused components.
- Validate via focused post-click-settle factory tests plus phase guard/signoff checks, then record `PHASE 80 COMPLETE`.

Phase 80 implementation files:
- `docs/NATIVE_CLIENT_PHASE80_INTERACTION_SESSION_POST_CLICK_SETTLE_SERVICE_COMPOSITE_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase80_interaction_session_post_click_settle_service_composite_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionPostClickSettleFactory.java`

## Phase 81 (Interaction Session Command-Router Service Composite Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting command-router composite service assembly ownership into focused factory boundaries.
- Keep `InteractionSessionHostFactory` command-router service compatibility method signatures while delegating assembly ownership to focused components.
- Validate via focused command-router factory tests plus phase guard/signoff checks, then record `PHASE 81 COMPLETE`.

Phase 81 implementation files:
- `docs/NATIVE_CLIENT_PHASE81_INTERACTION_SESSION_COMMAND_ROUTER_SERVICE_COMPOSITE_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase81_interaction_session_command_router_service_composite_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionCommandRouterFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterServiceFactoryTest.java`

## Phase 82 (Interaction Session Host-Factory Consolidation H)
- Consolidate `InteractionSessionHostFactory` composite service seams by routing post-click-settle and command-router service construction through focused factory boundaries.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 82 COMPLETE`.

Phase 82 implementation files:
- `docs/NATIVE_CLIENT_PHASE82_INTERACTION_SESSION_HOST_FACTORY_CONSOLIDATION_H_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase82_interaction_session_host_factory_consolidation_h.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`

## Phase 83 (Interaction Session Macro Pass H Signoff)
- Complete macro-pass signoff for phases 80-83 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 83 COMPLETE` after signoff gates pass.

Phase 83 implementation files:
- `docs/NATIVE_CLIENT_PHASE83_INTERACTION_SESSION_MACRO_PASS_H_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase83_interaction_session_macro_pass_h_signoff.py`
- `scripts/verify_phase82_interaction_session_host_factory_consolidation_h.py`
- `scripts/verify_phase81_interaction_session_command_router_service_composite_factory_extraction.py`
- `scripts/verify_phase80_interaction_session_post_click_settle_service_composite_factory_extraction.py`

## Phase 84 (Interaction Session Ownership Service Composite Factory Extraction)
- Continue thinning session runtime host-factory composition by extracting ownership composite service assembly ownership into focused factory boundaries.
- Keep `InteractionSessionHostFactory` ownership-service compatibility method signatures while delegating assembly ownership to focused components.
- Validate via focused ownership factory tests plus phase guard/signoff checks, then record `PHASE 84 COMPLETE`.

Phase 84 implementation files:
- `docs/NATIVE_CLIENT_PHASE84_INTERACTION_SESSION_OWNERSHIP_SERVICE_COMPOSITE_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase84_interaction_session_ownership_service_composite_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionOwnershipFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipServiceFactoryTest.java`

## Phase 85 (Interaction Session Host-Factory Consolidation I)
- Consolidate `InteractionSessionHostFactory` composite service seams by routing ownership service construction through focused factory boundaries.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 85 COMPLETE`.

Phase 85 implementation files:
- `docs/NATIVE_CLIENT_PHASE85_INTERACTION_SESSION_HOST_FACTORY_CONSOLIDATION_I_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase85_interaction_session_host_factory_consolidation_i.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java`

## Phase 86 (Interaction Session Macro Pass I Signoff)
- Complete macro-pass signoff for phases 84-86 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 86 COMPLETE` after signoff gates pass.

Phase 86 implementation files:
- `docs/NATIVE_CLIENT_PHASE86_INTERACTION_SESSION_MACRO_PASS_I_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase86_interaction_session_macro_pass_i_signoff.py`
- `scripts/verify_phase85_interaction_session_host_factory_consolidation_i.py`
- `scripts/verify_phase84_interaction_session_ownership_service_composite_factory_extraction.py`
- `scripts/verify_phase83_interaction_session_macro_pass_h_signoff.py`

## Phase 87 (Interaction Session Assembly Factory Extraction)
- Continue thinning direct constructor ownership in `InteractionSession` by extracting runtime service assembly into focused assembly-factory boundaries.
- Keep `InteractionSession` constructor behavior parity while delegating initialization assembly ownership to focused components.
- Validate via focused assembly-factory tests plus phase guard/signoff checks, then record `PHASE 87 COMPLETE`.

Phase 87 implementation files:
- `docs/NATIVE_CLIENT_PHASE87_INTERACTION_SESSION_ASSEMBLY_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase87_interaction_session_assembly_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionAssemblyFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeBundle.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionAssemblyFactoryTest.java`

## Phase 88 (Interaction Session Assembly Consolidation J)
- Consolidate `InteractionSessionAssemblyFactory` runtime bundle seams by routing session assembly through explicit session-key ownership boundaries.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 88 COMPLETE`.

Phase 88 implementation files:
- `docs/NATIVE_CLIENT_PHASE88_INTERACTION_SESSION_ASSEMBLY_CONSOLIDATION_J_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase88_interaction_session_assembly_consolidation_j.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionAssemblyFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`

## Phase 89 (Interaction Session Macro Pass J Signoff)
- Complete macro-pass signoff for phases 87-89 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 89 COMPLETE` after signoff gates pass.

Phase 89 implementation files:
- `docs/NATIVE_CLIENT_PHASE89_INTERACTION_SESSION_MACRO_PASS_J_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase89_interaction_session_macro_pass_j_signoff.py`
- `scripts/verify_phase88_interaction_session_assembly_consolidation_j.py`
- `scripts/verify_phase87_interaction_session_assembly_factory_extraction.py`
- `scripts/verify_phase86_interaction_session_macro_pass_i_signoff.py`

## Phase 90 (Interaction Session Runtime Bundle Factory Extraction)
- Continue thinning assembly-factory composition by extracting runtime-bundle construction ownership into focused factory boundaries.
- Keep `InteractionSessionAssemblyFactory` runtime-bundle compatibility method signatures while delegating construction ownership to focused components.
- Validate via focused runtime-bundle factory tests plus phase guard/signoff checks, then record `PHASE 90 COMPLETE`.

Phase 90 implementation files:
- `docs/NATIVE_CLIENT_PHASE90_INTERACTION_SESSION_RUNTIME_BUNDLE_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase90_interaction_session_runtime_bundle_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionAssemblyFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactoryTest.java`

## Phase 91 (Interaction Session Assembly Consolidation K)
- Consolidate `InteractionSessionAssemblyFactory` runtime-bundle seams by routing construction through focused runtime-bundle factory boundaries.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 91 COMPLETE`.

Phase 91 implementation files:
- `docs/NATIVE_CLIENT_PHASE91_INTERACTION_SESSION_ASSEMBLY_CONSOLIDATION_K_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase91_interaction_session_assembly_consolidation_k.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionAssemblyFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`

## Phase 92 (Interaction Session Macro Pass K Signoff)
- Complete macro-pass signoff for phases 90-92 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 92 COMPLETE` after signoff gates pass.

Phase 92 implementation files:
- `docs/NATIVE_CLIENT_PHASE92_INTERACTION_SESSION_MACRO_PASS_K_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase92_interaction_session_macro_pass_k_signoff.py`
- `scripts/verify_phase91_interaction_session_assembly_consolidation_k.py`
- `scripts/verify_phase90_interaction_session_runtime_bundle_factory_extraction.py`
- `scripts/verify_phase89_interaction_session_macro_pass_j_signoff.py`

## Phase 93 (Interaction Session Constructor Runtime Bundle Extraction)
- Continue thinning `InteractionSession` constructor ownership by extracting runtime-bundle injection boundaries.
- Keep constructor delegation behavior parity while removing unused constructor-owned runtime-service fields.
- Validate via focused constructor/runtime-bundle tests plus phase guard/signoff checks, then record `PHASE 93 COMPLETE`.

Phase 93 implementation files:
- `docs/NATIVE_CLIENT_PHASE93_INTERACTION_SESSION_CONSTRUCTOR_RUNTIME_BUNDLE_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase93_interaction_session_constructor_runtime_bundle_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`

## Phase 94 (Interaction Session Factory Extraction)
- Continue thinning executor/session construction ownership by extracting interaction-session construction into focused `InteractionSessionFactory` boundaries.
- Keep runtime bundle assembly contracts stable while introducing focused factory entrypoints.
- Validate via focused session-factory tests plus phase guard/signoff checks, then record `PHASE 94 COMPLETE`.

Phase 94 implementation files:
- `docs/NATIVE_CLIENT_PHASE94_INTERACTION_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase94_interaction_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryTest.java`

## Phase 95 (Interaction Session Wiring Consolidation L)
- Consolidate executor/session wiring seams by routing interaction-session construction through focused `InteractionSessionFactory` boundaries.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 95 COMPLETE`.

Phase 95 implementation files:
- `docs/NATIVE_CLIENT_PHASE95_INTERACTION_SESSION_WIRING_CONSOLIDATION_L_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase95_interaction_session_wiring_consolidation_l.py`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorServiceWiring.java`

## Phase 96 (Interaction Session Macro Pass L Signoff)
- Complete macro-pass signoff for phases 93-96 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 96 COMPLETE` after signoff gates pass.

Phase 96 implementation files:
- `docs/NATIVE_CLIENT_PHASE96_INTERACTION_SESSION_MACRO_PASS_L_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase96_interaction_session_macro_pass_l_signoff.py`
- `scripts/verify_phase95_interaction_session_wiring_consolidation_l.py`
- `scripts/verify_phase94_interaction_session_factory_extraction.py`
- `scripts/verify_phase93_interaction_session_constructor_runtime_bundle_extraction.py`

## Phase 97 (Interaction Session Runtime Operations Extraction)
- Continue thinning `InteractionSession` runtime ownership by extracting command/click/tick/shutdown delegation into focused runtime-operations boundaries.
- Keep constructor/runtime-bundle compatibility contracts stable while shifting runtime delegation ownership.
- Validate via focused runtime-operations tests plus phase guard/signoff checks, then record `PHASE 97 COMPLETE`.

Phase 97 implementation files:
- `docs/NATIVE_CLIENT_PHASE97_INTERACTION_SESSION_RUNTIME_OPERATIONS_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase97_interaction_session_runtime_operations_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeOperations.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java`

## Phase 98 (Interaction Session Runtime Operations Factory Extraction)
- Continue thinning interaction-session runtime assembly ownership by extracting runtime-operations construction into focused factory boundaries.
- Keep runtime-operations behavior parity while introducing focused construction entrypoints.
- Validate via focused runtime-operations factory tests plus phase guard/signoff checks, then record `PHASE 98 COMPLETE`.

Phase 98 implementation files:
- `docs/NATIVE_CLIENT_PHASE98_INTERACTION_SESSION_RUNTIME_OPERATIONS_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase98_interaction_session_runtime_operations_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeOperationsFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRuntimeOperationsFactoryTest.java`

## Phase 99 (Interaction Session Wiring Consolidation M)
- Consolidate interaction-session runtime wiring seams by routing runtime-bundle construction through focused runtime-operations factory ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 99 COMPLETE`.

Phase 99 implementation files:
- `docs/NATIVE_CLIENT_PHASE99_INTERACTION_SESSION_WIRING_CONSOLIDATION_M_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase99_interaction_session_wiring_consolidation_m.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 100 (Interaction Session Macro Pass M Signoff)
- Complete macro-pass signoff for phases 97-100 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 100 COMPLETE` after signoff gates pass.

Phase 100 implementation files:
- `docs/NATIVE_CLIENT_PHASE100_INTERACTION_SESSION_MACRO_PASS_M_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase100_interaction_session_macro_pass_m_signoff.py`
- `scripts/verify_phase99_interaction_session_wiring_consolidation_m.py`
- `scripts/verify_phase98_interaction_session_runtime_operations_factory_extraction.py`
- `scripts/verify_phase97_interaction_session_runtime_operations_extraction.py`

## Phase 101 (Interaction Session Runtime Operations Bundle Extraction)
- Continue thinning interaction-session runtime bundle ownership by extracting a focused runtime-operations bundle boundary.
- Route runtime-operations factory delegation through the focused runtime-operations bundle seam instead of direct full runtime-bundle field access.
- Validate via focused runtime-bundle/runtime-operations tests plus phase guard checks, then record `PHASE 101 COMPLETE`.

Phase 101 implementation files:
- `docs/NATIVE_CLIENT_PHASE101_INTERACTION_SESSION_RUNTIME_OPERATIONS_BUNDLE_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase101_interaction_session_runtime_operations_bundle_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeOperationsBundle.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeBundle.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeOperationsFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionAssemblyFactoryTest.java`

## Phase 102 (Interaction Session Runtime Control Bundle Extraction)
- Continue thinning interaction-session runtime-bundle ownership by extracting remaining non-runtime-operations services into focused runtime-control bundle boundaries.
- Route runtime-bundle composition through runtime-operations + runtime-control bundles while preserving compatibility sentinel continuity.
- Validate via focused runtime-bundle composition tests plus phase guard checks, then record `PHASE 102 COMPLETE`.

Phase 102 implementation files:
- `docs/NATIVE_CLIENT_PHASE102_INTERACTION_SESSION_RUNTIME_CONTROL_BUNDLE_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase102_interaction_session_runtime_control_bundle_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeControlBundle.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeBundle.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactoryTest.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionAssemblyFactoryTest.java`

## Phase 103 (Interaction Session Runtime Bundle Factory Inputs Extraction)
- Continue thinning interaction-session runtime-bundle-factory ownership by extracting long positional service arguments into focused typed-input ownership.
- Keep runtime-bundle-factory behavior parity while shifting service-entry construction toward typed inputs.
- Validate via focused runtime-bundle-factory input/assembly tests plus phase guard checks, then record `PHASE 103 COMPLETE`.

Phase 103 implementation files:
- `docs/NATIVE_CLIENT_PHASE103_INTERACTION_SESSION_RUNTIME_BUNDLE_FACTORY_INPUTS_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase103_interaction_session_runtime_bundle_factory_inputs_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactoryInputs.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactory.java`

## Phase 104 (Interaction Session Runtime Bundle Factory Typed Entry Extraction)
- Continue runtime-bundle-factory decomposition by extracting typed-entry bundle construction seam ownership.
- Keep service-entry compatibility wrapper behavior while promoting typed-entry construction contracts.
- Validate via focused runtime-bundle-factory typed-entry tests plus phase guard checks, then record `PHASE 104 COMPLETE`.

Phase 104 implementation files:
- `docs/NATIVE_CLIENT_PHASE104_INTERACTION_SESSION_RUNTIME_BUNDLE_FACTORY_TYPED_ENTRY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase104_interaction_session_runtime_bundle_factory_typed_entry_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactoryTest.java`

## Phase 105 (Interaction Session Assembly Wiring Consolidation N)
- Consolidate interaction-session assembly runtime-bundle seam through typed runtime-bundle-factory input ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 105 COMPLETE`.

Phase 105 implementation files:
- `docs/NATIVE_CLIENT_PHASE105_INTERACTION_SESSION_ASSEMBLY_WIRING_CONSOLIDATION_N_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase105_interaction_session_assembly_wiring_consolidation_n.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionAssemblyFactory.java`

## Phase 106 (Interaction Session Macro Pass N Signoff)
- Complete macro-pass signoff for phases 103-106 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 106 COMPLETE` after signoff gates pass.

Phase 106 implementation files:
- `docs/NATIVE_CLIENT_PHASE106_INTERACTION_SESSION_MACRO_PASS_N_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase106_interaction_session_macro_pass_n_signoff.py`
- `scripts/verify_phase105_interaction_session_assembly_wiring_consolidation_n.py`
- `scripts/verify_phase104_interaction_session_runtime_bundle_factory_typed_entry_extraction.py`
- `scripts/verify_phase103_interaction_session_runtime_bundle_factory_inputs_extraction.py`

## Phase 107 (Interaction Session Assembly Factory Inputs Extraction)
- Continue thinning interaction-session assembly ownership by extracting command/session/facade/session-key positional arguments into focused typed-input ownership.
- Keep assembly runtime-bundle behavior parity while introducing focused typed assembly-input contracts.
- Validate via focused assembly-input and assembly-factory tests plus phase guard checks, then record `PHASE 107 COMPLETE`.

Phase 107 implementation files:
- `docs/NATIVE_CLIENT_PHASE107_INTERACTION_SESSION_ASSEMBLY_FACTORY_INPUTS_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase107_interaction_session_assembly_factory_inputs_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionAssemblyFactoryInputs.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionAssemblyFactoryInputsTest.java`

## Phase 108 (Interaction Session Assembly Factory Typed Entry Extraction)
- Continue assembly-factory decomposition by extracting typed-entry runtime-bundle construction seams.
- Route session-key assembly entrypoints through typed assembly-input contracts while preserving compatibility sentinels.
- Validate via focused assembly-factory typed-entry tests plus phase guard checks, then record `PHASE 108 COMPLETE`.

Phase 108 implementation files:
- `docs/NATIVE_CLIENT_PHASE108_INTERACTION_SESSION_ASSEMBLY_FACTORY_TYPED_ENTRY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase108_interaction_session_assembly_factory_typed_entry_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionAssemblyFactory.java`

## Phase 109 (Interaction Session Factory Wiring Consolidation O)
- Consolidate interaction-session factory runtime-bundle seam through typed assembly-input ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 109 COMPLETE`.

Phase 109 implementation files:
- `docs/NATIVE_CLIENT_PHASE109_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_O_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase109_interaction_session_factory_wiring_consolidation_o.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 110 (Interaction Session Macro Pass O Signoff)
- Complete macro-pass signoff for phases 107-110 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 110 COMPLETE` after signoff gates pass.

Phase 110 implementation files:
- `docs/NATIVE_CLIENT_PHASE110_INTERACTION_SESSION_MACRO_PASS_O_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase110_interaction_session_macro_pass_o_signoff.py`
- `scripts/verify_phase109_interaction_session_factory_wiring_consolidation_o.py`
- `scripts/verify_phase108_interaction_session_assembly_factory_typed_entry_extraction.py`
- `scripts/verify_phase107_interaction_session_assembly_factory_inputs_extraction.py`

## Phase 115 (Interaction Session Factory Inputs Extraction)
- Continue thinning interaction-session factory ownership by extracting executor/session/facade positional arguments into focused factory-input ownership.
- Keep factory construction behavior parity while introducing focused `InteractionSessionFactoryInputs` contracts.
- Validate via focused factory-input/factory tests plus phase guard checks, then record `PHASE 115 COMPLETE`.

Phase 115 implementation files:
- `docs/NATIVE_CLIENT_PHASE115_INTERACTION_SESSION_FACTORY_INPUTS_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase115_interaction_session_factory_inputs_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryInputs.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryInputsTest.java`

## Phase 116 (Interaction Session Factory Typed Entry Extraction)
- Continue interaction-session factory decomposition by extracting typed-entry construction seams.
- Route factory runtime-bundle construction through `InteractionSessionFactoryInputs` while preserving compatibility sentinels.
- Validate via focused factory typed-entry tests plus phase guard checks, then record `PHASE 116 COMPLETE`.

Phase 116 implementation files:
- `docs/NATIVE_CLIENT_PHASE116_INTERACTION_SESSION_FACTORY_TYPED_ENTRY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase116_interaction_session_factory_typed_entry_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 117 (Interaction Session Factory Wiring Consolidation P)
- Consolidate public interaction-session factory creation seam through typed factory-input ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 117 COMPLETE`.

Phase 117 implementation files:
- `docs/NATIVE_CLIENT_PHASE117_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_P_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase117_interaction_session_factory_wiring_consolidation_p.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 118 (Interaction Session Macro Pass P Signoff)
- Complete macro-pass signoff for phases 115-118 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 118 COMPLETE` after signoff gates pass.

Phase 118 implementation files:
- `docs/NATIVE_CLIENT_PHASE118_INTERACTION_SESSION_MACRO_PASS_P_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase118_interaction_session_macro_pass_p_signoff.py`
- `scripts/verify_phase117_interaction_session_factory_wiring_consolidation_p.py`
- `scripts/verify_phase116_interaction_session_factory_typed_entry_extraction.py`
- `scripts/verify_phase115_interaction_session_factory_inputs_extraction.py`

## Phase 119 (Interaction Session Factory Runtime Bundle Factory Extraction)
- Continue thinning interaction-session factory ownership by extracting runtime-bundle routing seams into focused `InteractionSessionFactoryRuntimeBundleFactory` ownership.
- Keep factory runtime-bundle routing behavior parity while introducing focused runtime-bundle factory boundaries.
- Validate via focused runtime-bundle factory tests plus phase guard checks, then record `PHASE 119 COMPLETE`.

Phase 119 implementation files:
- `docs/NATIVE_CLIENT_PHASE119_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase119_interaction_session_factory_runtime_bundle_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryTest.java`

## Phase 120 (Interaction Session Factory Runtime Bundle Typed Entry Extraction)
- Continue interaction-session factory decomposition by extracting typed-entry runtime-bundle creation seams.
- Route factory runtime-bundle creation through `InteractionSessionFactoryRuntimeBundleFactory` while preserving compatibility sentinels.
- Validate via focused factory typed-entry tests plus phase guard checks, then record `PHASE 120 COMPLETE`.

Phase 120 implementation files:
- `docs/NATIVE_CLIENT_PHASE120_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_TYPED_ENTRY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase120_interaction_session_factory_runtime_bundle_typed_entry_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 121 (Interaction Session Factory Wiring Consolidation Q)
- Consolidate public interaction-session factory creation seam through typed runtime-bundle factory ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 121 COMPLETE`.

Phase 121 implementation files:
- `docs/NATIVE_CLIENT_PHASE121_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_Q_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase121_interaction_session_factory_wiring_consolidation_q.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 122 (Interaction Session Macro Pass Q Signoff)
- Complete macro-pass signoff for phases 119-122 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 122 COMPLETE` after signoff gates pass.

Phase 122 implementation files:
- `docs/NATIVE_CLIENT_PHASE122_INTERACTION_SESSION_MACRO_PASS_Q_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase122_interaction_session_macro_pass_q_signoff.py`
- `scripts/verify_phase121_interaction_session_factory_wiring_consolidation_q.py`
- `scripts/verify_phase120_interaction_session_factory_runtime_bundle_typed_entry_extraction.py`
- `scripts/verify_phase119_interaction_session_factory_runtime_bundle_factory_extraction.py`

## Phase 123 (Interaction Session Factory Runtime Bundle Key Policy Extraction)
- Continue thinning interaction-session factory runtime-bundle ownership by extracting default session-key policy into focused ownership.
- Keep runtime-bundle factory entry behavior parity while introducing focused policy-driven default key resolution.
- Validate via focused key-policy/runtime-bundle-factory tests plus phase guard checks, then record `PHASE 123 COMPLETE`.

Phase 123 implementation files:
- `docs/NATIVE_CLIENT_PHASE123_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_KEY_POLICY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase123_interaction_session_factory_runtime_bundle_key_policy_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleKeyPolicy.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleKeyPolicyTest.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryTest.java`

## Phase 124 (Interaction Session Factory Runtime Bundle Default Entry Extraction)
- Continue interaction-session factory decomposition by extracting typed default runtime-bundle entry seam ownership.
- Route factory runtime-bundle default-entry creation through `InteractionSessionFactoryRuntimeBundleFactory` while preserving compatibility sentinels.
- Validate via focused default-entry tests plus phase guard checks, then record `PHASE 124 COMPLETE`.

Phase 124 implementation files:
- `docs/NATIVE_CLIENT_PHASE124_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_DEFAULT_ENTRY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase124_interaction_session_factory_runtime_bundle_default_entry_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 125 (Interaction Session Factory Wiring Consolidation R)
- Consolidate public interaction-session factory creation seam through typed default runtime-bundle entry ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 125 COMPLETE`.

Phase 125 implementation files:
- `docs/NATIVE_CLIENT_PHASE125_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_R_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase125_interaction_session_factory_wiring_consolidation_r.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 126 (Interaction Session Macro Pass R Signoff)
- Complete macro-pass signoff for phases 123-126 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 126 COMPLETE` after signoff gates pass.

Phase 126 implementation files:
- `docs/NATIVE_CLIENT_PHASE126_INTERACTION_SESSION_MACRO_PASS_R_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase126_interaction_session_macro_pass_r_signoff.py`
- `scripts/verify_phase125_interaction_session_factory_wiring_consolidation_r.py`
- `scripts/verify_phase124_interaction_session_factory_runtime_bundle_default_entry_extraction.py`
- `scripts/verify_phase123_interaction_session_factory_runtime_bundle_key_policy_extraction.py`

## Phase 127 (Interaction Session Factory Runtime Bundle Factory Inputs Extraction)
- Continue thinning interaction-session factory runtime-bundle ownership by extracting typed runtime-bundle-factory input ownership.
- Keep runtime-bundle-factory routing behavior parity while introducing focused typed input contracts.
- Validate via focused runtime-bundle-factory inputs tests plus phase guard checks, then record `PHASE 127 COMPLETE`.

Phase 127 implementation files:
- `docs/NATIVE_CLIENT_PHASE127_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_INPUTS_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase127_interaction_session_factory_runtime_bundle_factory_inputs_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputs.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsTest.java`

## Phase 128 (Interaction Session Factory Runtime Bundle Factory Typed Entry Extraction)
- Continue runtime-bundle-factory decomposition by extracting typed-entry runtime-bundle factory seam ownership.
- Route runtime-bundle-factory typed-entry creation through `InteractionSessionFactoryRuntimeBundleFactoryInputs` while preserving compatibility sentinels.
- Validate via focused runtime-bundle-factory typed-entry tests plus phase guard checks, then record `PHASE 128 COMPLETE`.

Phase 128 implementation files:
- `docs/NATIVE_CLIENT_PHASE128_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_TYPED_ENTRY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase128_interaction_session_factory_runtime_bundle_factory_typed_entry_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryTest.java`

## Phase 129 (Interaction Session Factory Wiring Consolidation S)
- Consolidate public interaction-session factory creation seam through typed runtime-bundle-factory input ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 129 COMPLETE`.

Phase 129 implementation files:
- `docs/NATIVE_CLIENT_PHASE129_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_S_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase129_interaction_session_factory_wiring_consolidation_s.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 130 (Interaction Session Macro Pass S Signoff)
- Complete macro-pass signoff for phases 127-130 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 130 COMPLETE` after signoff gates pass.

Phase 130 implementation files:
- `docs/NATIVE_CLIENT_PHASE130_INTERACTION_SESSION_MACRO_PASS_S_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase130_interaction_session_macro_pass_s_signoff.py`
- `scripts/verify_phase129_interaction_session_factory_wiring_consolidation_s.py`
- `scripts/verify_phase128_interaction_session_factory_runtime_bundle_factory_typed_entry_extraction.py`
- `scripts/verify_phase127_interaction_session_factory_runtime_bundle_factory_inputs_extraction.py`

## Phase 131 (Interaction Session Factory Runtime Bundle Assembly Inputs Factory Extraction)
- Continue thinning interaction-session runtime-bundle-factory ownership by extracting assembly-input factory seams into focused ownership.
- Keep runtime-bundle-factory input assembly behavior parity while introducing focused assembly-input factory boundaries.
- Validate via focused assembly-input factory tests plus phase guard checks, then record `PHASE 131 COMPLETE`.

Phase 131 implementation files:
- `docs/NATIVE_CLIENT_PHASE131_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_ASSEMBLY_INPUTS_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase131_interaction_session_factory_runtime_bundle_assembly_inputs_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleAssemblyInputsFactoryTest.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputs.java`

## Phase 132 (Interaction Session Factory Runtime Bundle Factory Input Typed Entry Extraction)
- Continue runtime-bundle-factory decomposition by extracting typed-entry runtime-bundle-factory input seams through focused assembly-input factory ownership.
- Route runtime-bundle-factory typed-entry creation through `InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory` while preserving compatibility sentinels.
- Validate via focused typed-entry tests plus phase guard checks, then record `PHASE 132 COMPLETE`.

Phase 132 implementation files:
- `docs/NATIVE_CLIENT_PHASE132_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_INPUT_TYPED_ENTRY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase132_interaction_session_factory_runtime_bundle_factory_input_typed_entry_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputs.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryTest.java`

## Phase 133 (Interaction Session Factory Wiring Consolidation T)
- Consolidate public interaction-session factory creation seam through typed runtime-bundle-factory input routing ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 133 COMPLETE`.

Phase 133 implementation files:
- `docs/NATIVE_CLIENT_PHASE133_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_T_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase133_interaction_session_factory_wiring_consolidation_t.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 134 (Interaction Session Macro Pass T Signoff)
- Complete macro-pass signoff for phases 131-134 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 134 COMPLETE` after signoff gates pass.

Phase 134 implementation files:
- `docs/NATIVE_CLIENT_PHASE134_INTERACTION_SESSION_MACRO_PASS_T_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase134_interaction_session_macro_pass_t_signoff.py`
- `scripts/verify_phase133_interaction_session_factory_wiring_consolidation_t.py`
- `scripts/verify_phase132_interaction_session_factory_runtime_bundle_factory_input_typed_entry_extraction.py`
- `scripts/verify_phase131_interaction_session_factory_runtime_bundle_assembly_inputs_factory_extraction.py`

## Phase 135 (Interaction Session Factory Runtime Bundle Factory Inputs Assembly Factory Extraction)
- Continue thinning interaction-session runtime-bundle-factory ownership by extracting assembly-input to runtime-bundle-factory-input mapping into focused ownership.
- Keep runtime-bundle-factory assembly mapping behavior parity while introducing focused typed mapping boundaries.
- Validate via focused assembly-mapping tests plus phase guard checks, then record `PHASE 135 COMPLETE`.

Phase 135 implementation files:
- `docs/NATIVE_CLIENT_PHASE135_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_INPUTS_ASSEMBLY_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase135_interaction_session_factory_runtime_bundle_factory_inputs_assembly_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactoryTest.java`

## Phase 136 (Interaction Session Factory Runtime Bundle Assembly Entry Typed Routing Extraction)
- Continue runtime-bundle-factory decomposition by extracting typed assembly-entry runtime-bundle routing ownership.
- Route runtime-bundle-factory assembly-entry creation through `InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory` while preserving compatibility sentinels.
- Validate via focused assembly-entry routing tests plus phase guard checks, then record `PHASE 136 COMPLETE`.

Phase 136 implementation files:
- `docs/NATIVE_CLIENT_PHASE136_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_ASSEMBLY_ENTRY_TYPED_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase136_interaction_session_factory_runtime_bundle_assembly_entry_typed_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory.java`

## Phase 137 (Interaction Session Factory Wiring Consolidation U)
- Consolidate interaction-session factory assembly-entry seam through typed runtime-bundle-factory-input assembly mapping ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 137 COMPLETE`.

Phase 137 implementation files:
- `docs/NATIVE_CLIENT_PHASE137_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_U_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase137_interaction_session_factory_wiring_consolidation_u.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 138 (Interaction Session Macro Pass U Signoff)
- Complete macro-pass signoff for phases 135-138 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 138 COMPLETE` after signoff gates pass.

Phase 138 implementation files:
- `docs/NATIVE_CLIENT_PHASE138_INTERACTION_SESSION_MACRO_PASS_U_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase138_interaction_session_macro_pass_u_signoff.py`
- `scripts/verify_phase137_interaction_session_factory_wiring_consolidation_u.py`
- `scripts/verify_phase136_interaction_session_factory_runtime_bundle_assembly_entry_typed_routing_extraction.py`
- `scripts/verify_phase135_interaction_session_factory_runtime_bundle_factory_inputs_assembly_factory_extraction.py`

## Phase 139 (Interaction Session Factory Runtime Bundle Factory Inputs Factory Extraction)
- Continue thinning interaction-session runtime-bundle-factory ownership by extracting typed runtime-bundle-factory-input construction into focused ownership.
- Keep runtime-bundle-factory typed-input construction parity while introducing focused construction boundaries.
- Validate via focused typed-input construction tests plus phase guard checks, then record `PHASE 139 COMPLETE`.

Phase 139 implementation files:
- `docs/NATIVE_CLIENT_PHASE139_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase139_interaction_session_factory_runtime_bundle_factory_inputs_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsFactoryTest.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory.java`

## Phase 140 (Interaction Session Factory Runtime Bundle Factory Input Typed Entry Routing Extraction)
- Continue runtime-bundle-factory decomposition by extracting typed runtime-bundle-factory-input entry routing ownership.
- Route runtime-bundle-factory typed-entry seams through `InteractionSessionFactoryRuntimeBundleFactoryInputsFactory` while preserving compatibility sentinels.
- Validate via focused typed-entry routing tests plus phase guard checks, then record `PHASE 140 COMPLETE`.

Phase 140 implementation files:
- `docs/NATIVE_CLIENT_PHASE140_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_INPUT_TYPED_ENTRY_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase140_interaction_session_factory_runtime_bundle_factory_input_typed_entry_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryTest.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsFactory.java`

## Phase 141 (Interaction Session Factory Wiring Consolidation V)
- Consolidate interaction-session factory assembly-entry seam through runtime-bundle-factory typed input routing ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 141 COMPLETE`.

Phase 141 implementation files:
- `docs/NATIVE_CLIENT_PHASE141_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_V_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase141_interaction_session_factory_wiring_consolidation_v.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 142 (Interaction Session Macro Pass V Signoff)
- Complete macro-pass signoff for phases 139-142 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 142 COMPLETE` after signoff gates pass.

Phase 142 implementation files:
- `docs/NATIVE_CLIENT_PHASE142_INTERACTION_SESSION_MACRO_PASS_V_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase142_interaction_session_macro_pass_v_signoff.py`
- `scripts/verify_phase141_interaction_session_factory_wiring_consolidation_v.py`
- `scripts/verify_phase140_interaction_session_factory_runtime_bundle_factory_input_typed_entry_routing_extraction.py`
- `scripts/verify_phase139_interaction_session_factory_runtime_bundle_factory_inputs_factory_extraction.py`

## Phase 143 (Interaction Session Factory Runtime Bundle Default Assembly Inputs Factory Extraction)
- Continue thinning interaction-session runtime-bundle-factory ownership by extracting default assembly-input construction into focused ownership.
- Keep runtime-bundle default assembly-input construction parity while introducing focused default assembly-input factory boundaries.
- Validate via focused default assembly-input factory tests plus phase guard checks, then record `PHASE 143 COMPLETE`.

Phase 143 implementation files:
- `docs/NATIVE_CLIENT_PHASE143_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_DEFAULT_ASSEMBLY_INPUTS_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase143_interaction_session_factory_runtime_bundle_default_assembly_inputs_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultAssemblyInputsFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultAssemblyInputsFactoryTest.java`

## Phase 144 (Interaction Session Factory Runtime Bundle Default Entry Typed Routing Extraction)
- Continue runtime-bundle-factory decomposition by extracting default-entry runtime-bundle routing ownership.
- Route runtime-bundle-factory default-entry seams through `InteractionSessionFactoryRuntimeBundleDefaultAssemblyInputsFactory` while preserving compatibility sentinels.
- Validate via focused default-entry routing tests plus phase guard checks, then record `PHASE 144 COMPLETE`.

Phase 144 implementation files:
- `docs/NATIVE_CLIENT_PHASE144_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_DEFAULT_ENTRY_TYPED_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase144_interaction_session_factory_runtime_bundle_default_entry_typed_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultAssemblyInputsFactory.java`

## Phase 145 (Interaction Session Factory Wiring Consolidation W)
- Consolidate interaction-session factory default-entry seam through runtime-bundle-factory default assembly-input routing ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 145 COMPLETE`.

Phase 145 implementation files:
- `docs/NATIVE_CLIENT_PHASE145_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_W_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase145_interaction_session_factory_wiring_consolidation_w.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 146 (Interaction Session Macro Pass W Signoff)
- Complete macro-pass signoff for phases 143-146 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 146 COMPLETE` after signoff gates pass.

Phase 146 implementation files:
- `docs/NATIVE_CLIENT_PHASE146_INTERACTION_SESSION_MACRO_PASS_W_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase146_interaction_session_macro_pass_w_signoff.py`
- `scripts/verify_phase145_interaction_session_factory_wiring_consolidation_w.py`
- `scripts/verify_phase144_interaction_session_factory_runtime_bundle_default_entry_typed_routing_extraction.py`
- `scripts/verify_phase143_interaction_session_factory_runtime_bundle_default_assembly_inputs_factory_extraction.py`

## Phase 147 (Interaction Session Factory Runtime Bundle Default Factory Inputs Factory Extraction)
- Continue thinning interaction-session runtime-bundle-factory ownership by extracting default runtime-bundle-factory-input construction into focused ownership.
- Keep runtime-bundle default factory-input construction parity while introducing focused default factory-input factory boundaries.
- Validate via focused default factory-input factory tests plus phase guard checks, then record `PHASE 147 COMPLETE`.

Phase 147 implementation files:
- `docs/NATIVE_CLIENT_PHASE147_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_DEFAULT_FACTORY_INPUTS_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase147_interaction_session_factory_runtime_bundle_default_factory_inputs_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactoryTest.java`

## Phase 148 (Interaction Session Factory Runtime Bundle Default Factory Input Typed Routing Extraction)
- Continue runtime-bundle-factory decomposition by extracting default-entry runtime-bundle routing ownership.
- Route runtime-bundle-factory default-entry seams through `InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactory` while preserving compatibility sentinels.
- Validate via focused default-entry routing tests plus phase guard checks, then record `PHASE 148 COMPLETE`.

Phase 148 implementation files:
- `docs/NATIVE_CLIENT_PHASE148_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_DEFAULT_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase148_interaction_session_factory_runtime_bundle_default_factory_input_typed_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactory.java`

## Phase 149 (Interaction Session Factory Wiring Consolidation X)
- Consolidate interaction-session factory default-entry seam through runtime-bundle-factory default factory-input routing ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 149 COMPLETE`.

Phase 149 implementation files:
- `docs/NATIVE_CLIENT_PHASE149_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_X_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase149_interaction_session_factory_wiring_consolidation_x.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 150 (Interaction Session Macro Pass X Signoff)
- Complete macro-pass signoff for phases 147-150 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 150 COMPLETE` after signoff gates pass.

Phase 150 implementation files:
- `docs/NATIVE_CLIENT_PHASE150_INTERACTION_SESSION_MACRO_PASS_X_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase150_interaction_session_macro_pass_x_signoff.py`
- `scripts/verify_phase149_interaction_session_factory_wiring_consolidation_x.py`
- `scripts/verify_phase148_interaction_session_factory_runtime_bundle_default_factory_input_typed_routing_extraction.py`
- `scripts/verify_phase147_interaction_session_factory_runtime_bundle_default_factory_inputs_factory_extraction.py`

## Phase 151 (Interaction Session Factory Runtime Bundle Default Entry Factory Extraction)
- Continue thinning interaction-session runtime-bundle-factory ownership by extracting default-entry runtime-bundle creation into focused ownership.
- Keep runtime-bundle default-entry creation parity while introducing focused default-entry factory boundaries.
- Validate via focused default-entry factory tests plus phase guard checks, then record `PHASE 151 COMPLETE`.

Phase 151 implementation files:
- `docs/NATIVE_CLIENT_PHASE151_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_DEFAULT_ENTRY_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase151_interaction_session_factory_runtime_bundle_default_entry_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultEntryFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultEntryFactoryTest.java`

## Phase 152 (Interaction Session Factory Runtime Bundle Default Factory Input Typed Entry Routing Extraction)
- Continue runtime-bundle-factory decomposition by extracting default-entry runtime-bundle typed routing ownership.
- Route runtime-bundle-factory default-entry seams through `InteractionSessionFactoryRuntimeBundleDefaultEntryFactory` while preserving compatibility sentinels.
- Validate via focused typed-entry routing tests plus phase guard checks, then record `PHASE 152 COMPLETE`.

Phase 152 implementation files:
- `docs/NATIVE_CLIENT_PHASE152_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_DEFAULT_FACTORY_INPUT_TYPED_ENTRY_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase152_interaction_session_factory_runtime_bundle_default_factory_input_typed_entry_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryTest.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultEntryFactory.java`

## Phase 153 (Interaction Session Factory Wiring Consolidation Y)
- Consolidate interaction-session factory default-entry seam through runtime-bundle-factory default-entry runtime-bundle routing ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 153 COMPLETE`.

Phase 153 implementation files:
- `docs/NATIVE_CLIENT_PHASE153_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_Y_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase153_interaction_session_factory_wiring_consolidation_y.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 154 (Interaction Session Macro Pass Y Signoff)
- Complete macro-pass signoff for phases 151-154 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 154 COMPLETE` after signoff gates pass.

Phase 154 implementation files:
- `docs/NATIVE_CLIENT_PHASE154_INTERACTION_SESSION_MACRO_PASS_Y_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase154_interaction_session_macro_pass_y_signoff.py`
- `scripts/verify_phase153_interaction_session_factory_wiring_consolidation_y.py`
- `scripts/verify_phase152_interaction_session_factory_runtime_bundle_default_factory_input_typed_entry_routing_extraction.py`
- `scripts/verify_phase151_interaction_session_factory_runtime_bundle_default_entry_factory_extraction.py`

## Phase 155 (Interaction Session Factory Runtime Bundle Default Factory Input Runtime Bundle Factory Extraction)
- Continue thinning interaction-session runtime-bundle default-entry ownership by extracting default-runtime-bundle-factory-input runtime-bundle creation into focused ownership.
- Keep default runtime-bundle-factory-input runtime-bundle routing parity while introducing focused runtime-bundle factory boundaries.
- Validate via focused runtime-bundle factory tests plus phase guard checks, then record `PHASE 155 COMPLETE`.

Phase 155 implementation files:
- `docs/NATIVE_CLIENT_PHASE155_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_DEFAULT_FACTORY_INPUT_RUNTIME_BUNDLE_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase155_interaction_session_factory_runtime_bundle_default_factory_input_runtime_bundle_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactoryTest.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultRuntimeBundleFactory.java`

## Phase 156 (Interaction Session Factory Runtime Bundle Default Factory Input Typed Runtime Bundle Routing Extraction)
- Continue runtime-bundle default-entry decomposition by extracting typed default-runtime-bundle-factory-input routing ownership.
- Route default-runtime-bundle-factory-input typed runtime-bundle seams through focused runtime-bundle factory ownership while preserving compatibility sentinels.
- Validate via focused typed-routing tests plus phase guard checks, then record `PHASE 156 COMPLETE`.

Phase 156 implementation files:
- `docs/NATIVE_CLIENT_PHASE156_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_DEFAULT_FACTORY_INPUT_TYPED_RUNTIME_BUNDLE_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase156_interaction_session_factory_runtime_bundle_default_factory_input_typed_runtime_bundle_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultEntryFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultRuntimeBundleFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactory.java`

## Phase 157 (Interaction Session Factory Runtime Bundle Factory Wiring Consolidation Z)
- Consolidate runtime-bundle-factory default-runtime-bundle-factory-input routing seam through focused default-entry routing ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 157 COMPLETE`.

Phase 157 implementation files:
- `docs/NATIVE_CLIENT_PHASE157_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_WIRING_CONSOLIDATION_Z_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase157_interaction_session_factory_runtime_bundle_factory_wiring_consolidation_z.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java`

## Phase 158 (Interaction Session Factory Default Entry Factory Extraction)
- Continue thinning interaction-session factory ownership by extracting default-entry session creation into focused ownership.
- Keep interaction-session default-entry creation parity while introducing focused factory boundaries.
- Validate via focused default-entry factory tests plus phase guard checks, then record `PHASE 158 COMPLETE`.

Phase 158 implementation files:
- `docs/NATIVE_CLIENT_PHASE158_INTERACTION_SESSION_FACTORY_DEFAULT_ENTRY_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase158_interaction_session_factory_default_entry_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryFactoryTest.java`

## Phase 159 (Interaction Session Factory Default Entry Wiring Consolidation Z)
- Consolidate interaction-session factory default-entry seam through focused default-entry factory routing ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 159 COMPLETE`.

Phase 159 implementation files:
- `docs/NATIVE_CLIENT_PHASE159_INTERACTION_SESSION_FACTORY_DEFAULT_ENTRY_WIRING_CONSOLIDATION_Z_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase159_interaction_session_factory_default_entry_wiring_consolidation_z.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 160 (Interaction Session Macro Pass Z Signoff)
- Complete macro-pass signoff for phases 155-160 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 160 COMPLETE` after signoff gates pass.

Phase 160 implementation files:
- `docs/NATIVE_CLIENT_PHASE160_INTERACTION_SESSION_MACRO_PASS_Z_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase160_interaction_session_macro_pass_z_signoff.py`
- `scripts/verify_phase159_interaction_session_factory_default_entry_wiring_consolidation_z.py`
- `scripts/verify_phase158_interaction_session_factory_default_entry_factory_extraction.py`
- `scripts/verify_phase157_interaction_session_factory_runtime_bundle_factory_wiring_consolidation_z.py`
- `scripts/verify_phase156_interaction_session_factory_runtime_bundle_default_factory_input_typed_runtime_bundle_routing_extraction.py`
- `scripts/verify_phase155_interaction_session_factory_runtime_bundle_default_factory_input_runtime_bundle_factory_extraction.py`

## Phase 161 (Interaction Session Factory Default Runtime Bundle Factory Inputs Factory Extraction)
- Continue thinning interaction-session factory default-entry ownership by extracting default runtime-bundle-factory-input construction into focused ownership.
- Keep default runtime-bundle-factory-input construction parity while introducing focused factory boundaries.
- Validate via focused default runtime-bundle-factory-input construction tests plus phase guard checks, then record `PHASE 161 COMPLETE`.

Phase 161 implementation files:
- `docs/NATIVE_CLIENT_PHASE161_INTERACTION_SESSION_FACTORY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase161_interaction_session_factory_default_runtime_bundle_factory_inputs_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactoryTest.java`

## Phase 162 (Interaction Session Factory Default Runtime Bundle Factory Input Typed Routing Extraction)
- Continue interaction-session default-entry decomposition by extracting typed default runtime-bundle-factory-input routing ownership.
- Route default runtime-bundle-factory-input typed seams through focused default runtime-bundle-factory-input construction ownership while preserving compatibility sentinels.
- Validate via focused typed-routing tests plus phase guard checks, then record `PHASE 162 COMPLETE`.

Phase 162 implementation files:
- `docs/NATIVE_CLIENT_PHASE162_INTERACTION_SESSION_FACTORY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase162_interaction_session_factory_default_runtime_bundle_factory_input_typed_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory.java`

## Phase 163 (Interaction Session Factory Default Entry Factory Wiring Consolidation AA)
- Consolidate interaction-session default-entry-factory routing seams through focused default runtime-bundle-factory-input construction ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 163 COMPLETE`.

Phase 163 implementation files:
- `docs/NATIVE_CLIENT_PHASE163_INTERACTION_SESSION_FACTORY_DEFAULT_ENTRY_FACTORY_WIRING_CONSOLIDATION_AA_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase163_interaction_session_factory_default_entry_factory_wiring_consolidation_aa.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryFactory.java`

## Phase 164 (Interaction Session Factory Default Runtime Session Factory Extraction)
- Continue thinning interaction-session default-entry ownership by extracting default runtime session creation into focused ownership.
- Keep default runtime session creation parity while introducing focused factory boundaries.
- Validate via focused default runtime session creation tests plus phase guard checks, then record `PHASE 164 COMPLETE`.

Phase 164 implementation files:
- `docs/NATIVE_CLIENT_PHASE164_INTERACTION_SESSION_FACTORY_DEFAULT_RUNTIME_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase164_interaction_session_factory_default_runtime_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeSessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeSessionFactoryTest.java`

## Phase 165 (Interaction Session Factory Default Entry Wiring Consolidation AB)
- Consolidate interaction-session factory default runtime-bundle-factory-input seam through focused default-entry routing ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 165 COMPLETE`.

Phase 165 implementation files:
- `docs/NATIVE_CLIENT_PHASE165_INTERACTION_SESSION_FACTORY_DEFAULT_ENTRY_WIRING_CONSOLIDATION_AB_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase165_interaction_session_factory_default_entry_wiring_consolidation_ab.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 166 (Interaction Session Macro Pass AB Signoff)
- Complete macro-pass signoff for phases 161-166 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 166 COMPLETE` after signoff gates pass.

Phase 166 implementation files:
- `docs/NATIVE_CLIENT_PHASE166_INTERACTION_SESSION_MACRO_PASS_AB_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase166_interaction_session_macro_pass_ab_signoff.py`
- `scripts/verify_phase165_interaction_session_factory_default_entry_wiring_consolidation_ab.py`
- `scripts/verify_phase164_interaction_session_factory_default_runtime_session_factory_extraction.py`
- `scripts/verify_phase163_interaction_session_factory_default_entry_factory_wiring_consolidation_aa.py`
- `scripts/verify_phase162_interaction_session_factory_default_runtime_bundle_factory_input_typed_routing_extraction.py`
- `scripts/verify_phase161_interaction_session_factory_default_runtime_bundle_factory_inputs_factory_extraction.py`

## Phase 167 (Interaction Session Factory Default Runtime Bundle Factory Extraction)
- Continue thinning interaction-session default-runtime-session ownership by extracting default runtime-bundle creation into focused ownership.
- Keep default runtime-bundle creation parity while introducing focused factory boundaries.
- Validate via focused default runtime-bundle factory tests plus phase guard checks, then record `PHASE 167 COMPLETE`.

Phase 167 implementation files:
- `docs/NATIVE_CLIENT_PHASE167_INTERACTION_SESSION_FACTORY_DEFAULT_RUNTIME_BUNDLE_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase167_interaction_session_factory_default_runtime_bundle_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactoryTest.java`

## Phase 168 (Interaction Session Factory Default Runtime Bundle Typed Routing Extraction)
- Continue default-runtime-session decomposition by extracting typed default runtime-bundle routing ownership.
- Route default runtime-bundle typed seams through focused default runtime-bundle factory ownership while preserving compatibility sentinels.
- Validate via focused typed-routing tests plus phase guard checks, then record `PHASE 168 COMPLETE`.

Phase 168 implementation files:
- `docs/NATIVE_CLIENT_PHASE168_INTERACTION_SESSION_FACTORY_DEFAULT_RUNTIME_BUNDLE_TYPED_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase168_interaction_session_factory_default_runtime_bundle_typed_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeSessionFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactory.java`

## Phase 169 (Interaction Session Factory Default Runtime Session Wiring Consolidation AC)
- Consolidate default-runtime-session routing seams through focused default runtime-bundle factory ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 169 COMPLETE`.

Phase 169 implementation files:
- `docs/NATIVE_CLIENT_PHASE169_INTERACTION_SESSION_FACTORY_DEFAULT_RUNTIME_SESSION_WIRING_CONSOLIDATION_AC_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase169_interaction_session_factory_default_runtime_session_wiring_consolidation_ac.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeSessionFactory.java`

## Phase 170 (Interaction Session Factory Default Entry Runtime Session Factory Extraction)
- Continue thinning default-entry ownership by extracting default-entry runtime-session routing into focused ownership.
- Keep default-entry runtime-session routing parity while introducing focused factory boundaries.
- Validate via focused default-entry runtime-session factory tests plus phase guard checks, then record `PHASE 170 COMPLETE`.

Phase 170 implementation files:
- `docs/NATIVE_CLIENT_PHASE170_INTERACTION_SESSION_FACTORY_DEFAULT_ENTRY_RUNTIME_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase170_interaction_session_factory_default_entry_runtime_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryRuntimeSessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryRuntimeSessionFactoryTest.java`

## Phase 171 (Interaction Session Factory Default Entry Wiring Consolidation AC)
- Consolidate default-entry routing seams through focused default-entry runtime-session factory ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 171 COMPLETE`.

Phase 171 implementation files:
- `docs/NATIVE_CLIENT_PHASE171_INTERACTION_SESSION_FACTORY_DEFAULT_ENTRY_WIRING_CONSOLIDATION_AC_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase171_interaction_session_factory_default_entry_wiring_consolidation_ac.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryFactory.java`

## Phase 172 (Interaction Session Macro Pass AC Signoff)
- Complete macro-pass signoff for phases 167-172 by synchronizing migration/task/status/inventory artifacts.
- Verify extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 172 COMPLETE` after signoff gates pass.

Phase 172 implementation files:
- `docs/NATIVE_CLIENT_PHASE172_INTERACTION_SESSION_MACRO_PASS_AC_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase172_interaction_session_macro_pass_ac_signoff.py`
- `scripts/verify_phase171_interaction_session_factory_default_entry_wiring_consolidation_ac.py`
- `scripts/verify_phase170_interaction_session_factory_default_entry_runtime_session_factory_extraction.py`
- `scripts/verify_phase169_interaction_session_factory_default_runtime_session_wiring_consolidation_ac.py`
- `scripts/verify_phase168_interaction_session_factory_default_runtime_bundle_typed_routing_extraction.py`
- `scripts/verify_phase167_interaction_session_factory_default_runtime_bundle_factory_extraction.py`

## Phase 173 (Interaction Session Factory Default Entry Runtime Bundle Factory Inputs Factory Extraction)
- Continue thinning default-entry ownership by extracting default-entry runtime-bundle-factory-input construction into focused ownership.
- Keep default-entry runtime-bundle-factory-input construction parity while introducing focused factory boundaries.
- Validate via focused default-entry runtime-bundle-factory-input construction tests plus phase guard checks, then record `PHASE 173 COMPLETE`.

Phase 173 implementation files:
- `docs/NATIVE_CLIENT_PHASE173_INTERACTION_SESSION_FACTORY_DEFAULT_ENTRY_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase173_interaction_session_factory_default_entry_runtime_bundle_factory_inputs_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryRuntimeBundleFactoryInputsFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryRuntimeBundleFactoryInputsFactoryTest.java`

## Phase 174 (Interaction Session Factory Default Entry Runtime Bundle Factory Input Typed Routing Extraction)
- Continue default-entry decomposition by extracting typed default-entry runtime-bundle-factory-input routing ownership.
- Route default-entry runtime-bundle-factory-input typed seams through focused default-entry runtime-bundle-factory-input construction ownership while preserving compatibility sentinels.
- Validate via focused typed-routing tests plus phase guard checks, then record `PHASE 174 COMPLETE`.

Phase 174 implementation files:
- `docs/NATIVE_CLIENT_PHASE174_INTERACTION_SESSION_FACTORY_DEFAULT_ENTRY_RUNTIME_BUNDLE_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase174_interaction_session_factory_default_entry_runtime_bundle_factory_input_typed_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryRuntimeBundleFactoryInputsFactory.java`

## Phase 175 (Interaction Session Factory Default Entry Wiring Consolidation AD)
- Consolidate default-entry factory-input routing seams through focused default-entry runtime-bundle-factory-input construction ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 175 COMPLETE`.

Phase 175 implementation files:
- `docs/NATIVE_CLIENT_PHASE175_INTERACTION_SESSION_FACTORY_DEFAULT_ENTRY_WIRING_CONSOLIDATION_AD_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase175_interaction_session_factory_default_entry_wiring_consolidation_ad.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryFactory.java`

## Phase 176 (Interaction Session Factory Default Factory Inputs Session Factory Extraction)
- Continue thinning interaction-session factory-input ownership by extracting default factory-input session creation into focused ownership.
- Keep default factory-input session creation parity while introducing focused factory boundaries.
- Validate via focused default factory-input session factory tests plus phase guard checks, then record `PHASE 176 COMPLETE`.

Phase 176 implementation files:
- `docs/NATIVE_CLIENT_PHASE176_INTERACTION_SESSION_FACTORY_DEFAULT_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase176_interaction_session_factory_default_factory_inputs_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultFactoryInputsSessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultFactoryInputsSessionFactoryTest.java`

## Phase 177 (Interaction Session Factory Wiring Consolidation AC)
- Consolidate interaction-session factory-input routing seams through focused default factory-input session factory ownership.
- Preserve compatibility sentinel strings and compatibility wrappers required by prior phase verifier gates.
- Validate via consolidation guard/signoff checks, then record `PHASE 177 COMPLETE`.

Phase 177 implementation files:
- `docs/NATIVE_CLIENT_PHASE177_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_AC_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase177_interaction_session_factory_wiring_consolidation_ac.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 178 (Interaction Session Macro Pass AD Signoff)
- Close Macro Pass AD (Phases 173-178) with synchronized migration artifacts, Java-surface inventory updates, and full verification signoff.
- Validate extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 178 COMPLETE` after signoff gates pass.

Phase 178 implementation files:
- `docs/NATIVE_CLIENT_PHASE178_INTERACTION_SESSION_MACRO_PASS_AD_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase178_interaction_session_macro_pass_ad_signoff.py`
- `scripts/verify_phase177_interaction_session_factory_wiring_consolidation_ac.py`
- `scripts/verify_phase176_interaction_session_factory_default_factory_inputs_session_factory_extraction.py`
- `scripts/verify_phase175_interaction_session_factory_default_entry_wiring_consolidation_ad.py`
- `scripts/verify_phase174_interaction_session_factory_default_entry_runtime_bundle_factory_input_typed_routing_extraction.py`
- `scripts/verify_phase173_interaction_session_factory_default_entry_runtime_bundle_factory_inputs_factory_extraction.py`

## Phase 179 (Interaction Session Factory Assembly Factory Inputs Session Factory Extraction)
- Continue thinning interaction-session factory ownership by extracting assembly-factory-input session creation into focused ownership.
- Keep assembly-factory-input session creation parity while introducing focused factory boundaries.
- Validate via focused assembly-factory-input session factory tests plus phase guard checks, then record `PHASE 179 COMPLETE`.

Phase 179 implementation files:
- `docs/NATIVE_CLIENT_PHASE179_INTERACTION_SESSION_FACTORY_ASSEMBLY_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase179_interaction_session_factory_assembly_factory_inputs_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyFactoryInputsSessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryAssemblyFactoryInputsSessionFactoryTest.java`

## Phase 180 (Interaction Session Factory Assembly Factory Input Typed Routing Extraction)
- Continue interaction-session factory decomposition by consolidating assembly-factory-input typed routing through focused ownership.
- Route assembly-factory-input typed seams through focused assembly-factory-input session factory ownership while preserving compatibility sentinels.
- Validate via focused typed-routing tests plus phase guard checks, then record `PHASE 180 COMPLETE`.

Phase 180 implementation files:
- `docs/NATIVE_CLIENT_PHASE180_INTERACTION_SESSION_FACTORY_ASSEMBLY_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase180_interaction_session_factory_assembly_factory_input_typed_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyFactoryInputsSessionFactory.java`

## Phase 181 (Interaction Session Factory Runtime Bundle Factory Inputs Session Factory Extraction)
- Continue thinning interaction-session factory ownership by extracting runtime-bundle-factory-input session creation into focused ownership.
- Keep runtime-bundle-factory-input session creation parity while introducing focused factory boundaries.
- Validate via focused runtime-bundle-factory-input session factory tests plus phase guard checks, then record `PHASE 181 COMPLETE`.

Phase 181 implementation files:
- `docs/NATIVE_CLIENT_PHASE181_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase181_interaction_session_factory_runtime_bundle_factory_inputs_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactoryTest.java`

## Phase 182 (Interaction Session Factory Runtime Bundle Factory Input Typed Routing Extraction)
- Continue interaction-session factory decomposition by consolidating runtime-bundle-factory-input typed routing through focused ownership.
- Route runtime-bundle-factory-input typed seams through focused runtime-bundle-factory-input session factory ownership while preserving compatibility sentinels.
- Validate via focused typed-routing tests plus phase guard checks, then record `PHASE 182 COMPLETE`.

Phase 182 implementation files:
- `docs/NATIVE_CLIENT_PHASE182_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase182_interaction_session_factory_runtime_bundle_factory_input_typed_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactory.java`

## Phase 183 (Interaction Session Factory Runtime Bundle Session Factory Extraction)
- Continue thinning interaction-session factory ownership by extracting runtime-bundle/runtime-operations session routing into focused ownership.
- Keep runtime-bundle/runtime-operations session routing parity while introducing focused factory boundaries.
- Validate via focused runtime-bundle session factory tests plus phase guard checks, then record `PHASE 183 COMPLETE`.

Phase 183 implementation files:
- `docs/NATIVE_CLIENT_PHASE183_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase183_interaction_session_factory_runtime_bundle_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleSessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleSessionFactoryTest.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 184 (Interaction Session Macro Pass AE Signoff)
- Close Macro Pass AE (Phases 179-184) with synchronized migration artifacts, Java-surface inventory updates, and full verification signoff.
- Validate extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 184 COMPLETE` after signoff gates pass.

Phase 184 implementation files:
- `docs/NATIVE_CLIENT_PHASE184_INTERACTION_SESSION_MACRO_PASS_AE_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase184_interaction_session_macro_pass_ae_signoff.py`
- `scripts/verify_phase183_interaction_session_factory_runtime_bundle_session_factory_extraction.py`
- `scripts/verify_phase182_interaction_session_factory_runtime_bundle_factory_input_typed_routing_extraction.py`
- `scripts/verify_phase181_interaction_session_factory_runtime_bundle_factory_inputs_session_factory_extraction.py`
- `scripts/verify_phase180_interaction_session_factory_assembly_factory_input_typed_routing_extraction.py`
- `scripts/verify_phase179_interaction_session_factory_assembly_factory_inputs_session_factory_extraction.py`

## Phase 185 (Interaction Session Factory Default Runtime Bundle Factory Inputs Session Factory Extraction)
- Continue thinning interaction-session factory ownership by extracting default-runtime-bundle-factory-input session creation into focused ownership.
- Keep default-runtime-bundle-factory-input session creation parity while introducing focused factory boundaries.
- Validate via focused default-runtime-bundle-factory-input session factory tests plus phase guard checks, then record `PHASE 185 COMPLETE`.

Phase 185 implementation files:
- `docs/NATIVE_CLIENT_PHASE185_INTERACTION_SESSION_FACTORY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase185_interaction_session_factory_default_runtime_bundle_factory_inputs_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactoryTest.java`

## Phase 186 (Interaction Session Factory Default Runtime Bundle Factory Input Typed Routing Extraction)
- Continue interaction-session factory decomposition by consolidating default-runtime-bundle-factory-input typed routing through focused ownership.
- Route default-runtime-bundle-factory-input typed seams through focused session factory ownership while preserving compatibility sentinels.
- Validate via focused typed-routing tests plus phase guard checks, then record `PHASE 186 COMPLETE`.

Phase 186 implementation files:
- `docs/NATIVE_CLIENT_PHASE186_INTERACTION_SESSION_FACTORY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase186_interaction_session_factory_default_runtime_bundle_factory_input_typed_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntrySessionFactory.java`

## Phase 187 (Interaction Session Factory Service Inputs Session Factory Extraction)
- Continue thinning interaction-session factory ownership by extracting service-input session creation into focused ownership.
- Keep service-input session creation parity while introducing focused factory boundaries.
- Validate via focused service-input session factory tests plus phase guard checks, then record `PHASE 187 COMPLETE`.

Phase 187 implementation files:
- `docs/NATIVE_CLIENT_PHASE187_INTERACTION_SESSION_FACTORY_SERVICE_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase187_interaction_session_factory_service_inputs_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryServiceInputsSessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryServiceInputsSessionFactoryTest.java`

## Phase 188 (Interaction Session Factory Service Input Typed Routing Extraction)
- Continue interaction-session factory decomposition by consolidating service-input typed routing through focused ownership.
- Route service-input typed seams through focused service-input session factory ownership while preserving compatibility sentinels.
- Validate via focused typed-routing tests plus phase guard checks, then record `PHASE 188 COMPLETE`.

Phase 188 implementation files:
- `docs/NATIVE_CLIENT_PHASE188_INTERACTION_SESSION_FACTORY_SERVICE_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase188_interaction_session_factory_service_input_typed_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryServiceInputsSessionFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntrySessionFactory.java`

## Phase 189 (Interaction Session Factory Entry Session Factory Extraction)
- Consolidate interaction-session factory top-level entry seams through a focused entry-session factory ownership boundary.
- Keep top-level entry seam parity while introducing focused entry-session factory ownership.
- Validate via focused entry-session factory tests plus phase guard checks, then record `PHASE 189 COMPLETE`.

Phase 189 implementation files:
- `docs/NATIVE_CLIENT_PHASE189_INTERACTION_SESSION_FACTORY_ENTRY_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase189_interaction_session_factory_entry_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntrySessionFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryEntrySessionFactoryTest.java`

## Phase 190 (Interaction Session Macro Pass AF Signoff)
- Close Macro Pass AF (Phases 185-190) with synchronized migration artifacts, Java-surface inventory updates, and full verification signoff.
- Validate extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 190 COMPLETE` after signoff gates pass.

Phase 190 implementation files:
- `docs/NATIVE_CLIENT_PHASE190_INTERACTION_SESSION_MACRO_PASS_AF_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase190_interaction_session_macro_pass_af_signoff.py`
- `scripts/verify_phase189_interaction_session_factory_entry_session_factory_extraction.py`
- `scripts/verify_phase188_interaction_session_factory_service_input_typed_routing_extraction.py`
- `scripts/verify_phase187_interaction_session_factory_service_inputs_session_factory_extraction.py`
- `scripts/verify_phase186_interaction_session_factory_default_runtime_bundle_factory_input_typed_routing_extraction.py`
- `scripts/verify_phase185_interaction_session_factory_default_runtime_bundle_factory_inputs_session_factory_extraction.py`

## Phase 191 (Interaction Session Factory Factory Inputs Session Factory Extraction)
- Continue thinning interaction-session factory ownership by extracting factory-input session creation into focused ownership.
- Keep factory-input session creation parity while introducing focused factory boundaries.
- Validate via focused factory-input session factory tests plus phase guard checks, then record `PHASE 191 COMPLETE`.

Phase 191 implementation files:
- `docs/NATIVE_CLIENT_PHASE191_INTERACTION_SESSION_FACTORY_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase191_interaction_session_factory_factory_inputs_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryFactoryInputsSessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryFactoryInputsSessionFactoryTest.java`

## Phase 192 (Interaction Session Factory Factory Input Typed Routing Extraction)
- Continue interaction-session factory decomposition by consolidating factory-input typed routing through focused ownership.
- Route factory-input typed seams through focused factory-input session factory ownership while preserving compatibility sentinels.
- Validate via focused typed-routing tests plus phase guard checks, then record `PHASE 192 COMPLETE`.

Phase 192 implementation files:
- `docs/NATIVE_CLIENT_PHASE192_INTERACTION_SESSION_FACTORY_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase192_interaction_session_factory_factory_input_typed_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryFactoryInputsSessionFactory.java`

## Phase 193 (Interaction Session Factory Assembly Runtime Session Factory Extraction)
- Continue thinning interaction-session factory ownership by extracting assembly/runtime session routing into focused ownership.
- Keep assembly/runtime session routing parity while introducing focused factory boundaries.
- Validate via focused assembly/runtime session factory tests plus phase guard checks, then record `PHASE 193 COMPLETE`.

Phase 193 implementation files:
- `docs/NATIVE_CLIENT_PHASE193_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase193_interaction_session_factory_assembly_runtime_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeSessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeSessionFactoryTest.java`

## Phase 194 (Interaction Session Factory Assembly Runtime Typed Routing Extraction)
- Continue interaction-session factory decomposition by consolidating assembly/runtime typed routing through focused ownership.
- Route assembly/runtime typed seams through focused assembly/runtime session factory ownership while preserving compatibility sentinels.
- Validate via focused typed-routing tests plus phase guard checks, then record `PHASE 194 COMPLETE`.

Phase 194 implementation files:
- `docs/NATIVE_CLIENT_PHASE194_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_TYPED_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase194_interaction_session_factory_assembly_runtime_typed_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeSessionFactory.java`

## Phase 195 (Interaction Session Factory Runtime Entry Session Factory Extraction)
- Continue thinning interaction-session factory ownership by extracting runtime-entry session routing into focused ownership.
- Keep runtime-entry session routing parity while introducing focused factory boundaries.
- Validate via focused runtime-entry session factory tests plus phase guard checks, then record `PHASE 195 COMPLETE`.

Phase 195 implementation files:
- `docs/NATIVE_CLIENT_PHASE195_INTERACTION_SESSION_FACTORY_RUNTIME_ENTRY_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase195_interaction_session_factory_runtime_entry_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeEntrySessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeEntrySessionFactoryTest.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`

## Phase 196 (Interaction Session Macro Pass AG Signoff)
- Close Macro Pass AG (Phases 191-196) with synchronized migration artifacts, Java-surface inventory updates, and full verification signoff.
- Validate extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 196 COMPLETE` after signoff gates pass.

Phase 196 implementation files:
- `docs/NATIVE_CLIENT_PHASE196_INTERACTION_SESSION_MACRO_PASS_AG_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase196_interaction_session_macro_pass_ag_signoff.py`
- `scripts/verify_phase195_interaction_session_factory_runtime_entry_session_factory_extraction.py`
- `scripts/verify_phase194_interaction_session_factory_assembly_runtime_typed_routing_extraction.py`
- `scripts/verify_phase193_interaction_session_factory_assembly_runtime_session_factory_extraction.py`
- `scripts/verify_phase192_interaction_session_factory_factory_input_typed_routing_extraction.py`
- `scripts/verify_phase191_interaction_session_factory_factory_inputs_session_factory_extraction.py`

## Phase 197 (Interaction Session Factory Entry Service Inputs Session Factory Extraction)
- Continue thinning interaction-session entry ownership by extracting entry service-input session routing into focused ownership.
- Keep entry service-input session routing parity while introducing focused factory boundaries.
- Validate via focused entry service-input session factory tests plus phase guard checks, then record `PHASE 197 COMPLETE`.

Phase 197 implementation files:
- `docs/NATIVE_CLIENT_PHASE197_INTERACTION_SESSION_FACTORY_ENTRY_SERVICE_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase197_interaction_session_factory_entry_service_inputs_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntryServiceInputsSessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryEntryServiceInputsSessionFactoryTest.java`

## Phase 198 (Interaction Session Factory Entry Service Input Typed Routing Extraction)
- Continue interaction-session entry decomposition by consolidating entry service-input typed routing through focused ownership.
- Route entry service-input typed seams through focused entry service-input session factory ownership while preserving compatibility sentinels.
- Validate via focused typed-routing tests plus phase guard checks, then record `PHASE 198 COMPLETE`.

Phase 198 implementation files:
- `docs/NATIVE_CLIENT_PHASE198_INTERACTION_SESSION_FACTORY_ENTRY_SERVICE_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase198_interaction_session_factory_entry_service_input_typed_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntrySessionFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntryServiceInputsSessionFactory.java`

## Phase 199 (Interaction Session Factory Entry Default Runtime Bundle Factory Inputs Session Factory Extraction)
- Continue thinning interaction-session entry ownership by extracting entry default-runtime-bundle-factory-input session routing into focused ownership.
- Keep entry default-runtime-bundle-factory-input session routing parity while introducing focused factory boundaries.
- Validate via focused entry default-runtime-bundle-factory-input session factory tests plus phase guard checks, then record `PHASE 199 COMPLETE`.

Phase 199 implementation files:
- `docs/NATIVE_CLIENT_PHASE199_INTERACTION_SESSION_FACTORY_ENTRY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase199_interaction_session_factory_entry_default_runtime_bundle_factory_inputs_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntryDefaultRuntimeBundleFactoryInputsSessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryEntryDefaultRuntimeBundleFactoryInputsSessionFactoryTest.java`

## Phase 200 (Interaction Session Factory Entry Default Runtime Bundle Factory Input Typed Routing Extraction)
- Continue interaction-session entry decomposition by consolidating entry default-runtime-bundle-factory-input typed routing through focused ownership.
- Route entry default-runtime-bundle-factory-input typed seams through focused entry default-runtime-bundle-factory-input session factory ownership while preserving compatibility sentinels.
- Validate via focused typed-routing tests plus phase guard checks, then record `PHASE 200 COMPLETE`.

Phase 200 implementation files:
- `docs/NATIVE_CLIENT_PHASE200_INTERACTION_SESSION_FACTORY_ENTRY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase200_interaction_session_factory_entry_default_runtime_bundle_factory_input_typed_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntrySessionFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntryDefaultRuntimeBundleFactoryInputsSessionFactory.java`

## Phase 201 (Interaction Session Factory Factory Inputs Default Session Factory Extraction)
- Continue thinning interaction-session factory-input ownership by extracting factory-input default session routing into focused ownership.
- Keep factory-input default session routing parity while introducing focused factory boundaries.
- Validate via focused factory-input default session factory tests plus phase guard checks, then record `PHASE 201 COMPLETE`.

Phase 201 implementation files:
- `docs/NATIVE_CLIENT_PHASE201_INTERACTION_SESSION_FACTORY_FACTORY_INPUTS_DEFAULT_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase201_interaction_session_factory_factory_inputs_default_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryFactoryInputsDefaultSessionFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryFactoryInputsSessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryFactoryInputsDefaultSessionFactoryTest.java`

## Phase 202 (Interaction Session Macro Pass AH Signoff)
- Close Macro Pass AH (Phases 197-202) with synchronized migration artifacts, Java-surface inventory updates, and full verification signoff.
- Validate extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 202 COMPLETE` after signoff gates pass.

Phase 202 implementation files:
- `docs/NATIVE_CLIENT_PHASE202_INTERACTION_SESSION_MACRO_PASS_AH_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase202_interaction_session_macro_pass_ah_signoff.py`
- `scripts/verify_phase201_interaction_session_factory_factory_inputs_default_session_factory_extraction.py`
- `scripts/verify_phase200_interaction_session_factory_entry_default_runtime_bundle_factory_input_typed_routing_extraction.py`
- `scripts/verify_phase199_interaction_session_factory_entry_default_runtime_bundle_factory_inputs_session_factory_extraction.py`
- `scripts/verify_phase198_interaction_session_factory_entry_service_input_typed_routing_extraction.py`
- `scripts/verify_phase197_interaction_session_factory_entry_service_inputs_session_factory_extraction.py`

## Phase 203 (Interaction Session Factory Assembly Runtime Assembly Session Factory Extraction)
- Continue thinning interaction-session assembly-runtime ownership by extracting assembly session routing into focused ownership.
- Keep assembly-runtime assembly session routing parity while introducing focused factory boundaries.
- Validate via focused assembly-runtime assembly session factory tests plus phase guard checks, then record `PHASE 203 COMPLETE`.

Phase 203 implementation files:
- `docs/NATIVE_CLIENT_PHASE203_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_ASSEMBLY_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase203_interaction_session_factory_assembly_runtime_assembly_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeAssemblySessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeAssemblySessionFactoryTest.java`

## Phase 204 (Interaction Session Factory Assembly Runtime Assembly Typed Routing Extraction)
- Continue interaction-session assembly-runtime decomposition by consolidating assembly typed routing through focused ownership.
- Route assembly-runtime assembly typed seams through focused assembly-runtime assembly session factory ownership while preserving compatibility sentinels.
- Validate via focused typed-routing tests plus phase guard checks, then record `PHASE 204 COMPLETE`.

Phase 204 implementation files:
- `docs/NATIVE_CLIENT_PHASE204_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_ASSEMBLY_TYPED_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase204_interaction_session_factory_assembly_runtime_assembly_typed_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeSessionFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeAssemblySessionFactory.java`

## Phase 205 (Interaction Session Factory Assembly Runtime Bundle Factory Inputs Session Factory Extraction)
- Continue thinning interaction-session assembly-runtime ownership by extracting bundle-factory-input session routing into focused ownership.
- Keep assembly-runtime bundle-factory-input session routing parity while introducing focused factory boundaries.
- Validate via focused assembly-runtime bundle-factory-input session factory tests plus phase guard checks, then record `PHASE 205 COMPLETE`.

Phase 205 implementation files:
- `docs/NATIVE_CLIENT_PHASE205_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase205_interaction_session_factory_assembly_runtime_bundle_factory_inputs_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactoryTest.java`

## Phase 206 (Interaction Session Factory Assembly Runtime Bundle Factory Input Typed Routing Extraction)
- Continue interaction-session assembly-runtime decomposition by consolidating bundle-factory-input typed routing through focused ownership.
- Route assembly-runtime bundle-factory-input typed seams through focused assembly-runtime bundle-factory-input session factory ownership while preserving compatibility sentinels.
- Validate via focused typed-routing tests plus phase guard checks, then record `PHASE 206 COMPLETE`.

Phase 206 implementation files:
- `docs/NATIVE_CLIENT_PHASE206_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_BUNDLE_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase206_interaction_session_factory_assembly_runtime_bundle_factory_input_typed_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeSessionFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactory.java`

## Phase 207 (Interaction Session Factory Runtime Entry Runtime Session Factory Extraction)
- Continue thinning interaction-session runtime-entry ownership by extracting runtime session routing into focused ownership.
- Keep runtime-entry runtime session routing parity while introducing focused factory boundaries.
- Validate via focused runtime-entry runtime session factory tests plus phase guard checks, then record `PHASE 207 COMPLETE`.

Phase 207 implementation files:
- `docs/NATIVE_CLIENT_PHASE207_INTERACTION_SESSION_FACTORY_RUNTIME_ENTRY_RUNTIME_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase207_interaction_session_factory_runtime_entry_runtime_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeEntryRuntimeSessionFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeEntrySessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeEntryRuntimeSessionFactoryTest.java`

## Phase 208 (Interaction Session Macro Pass AI Signoff)
- Close Macro Pass AI (Phases 203-208) with synchronized migration artifacts, Java-surface inventory updates, and full verification signoff.
- Validate extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 208 COMPLETE` after signoff gates pass.

Phase 208 implementation files:
- `docs/NATIVE_CLIENT_PHASE208_INTERACTION_SESSION_MACRO_PASS_AI_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase208_interaction_session_macro_pass_ai_signoff.py`
- `scripts/verify_phase207_interaction_session_factory_runtime_entry_runtime_session_factory_extraction.py`
- `scripts/verify_phase206_interaction_session_factory_assembly_runtime_bundle_factory_input_typed_routing_extraction.py`
- `scripts/verify_phase205_interaction_session_factory_assembly_runtime_bundle_factory_inputs_session_factory_extraction.py`
- `scripts/verify_phase204_interaction_session_factory_assembly_runtime_assembly_typed_routing_extraction.py`
- `scripts/verify_phase203_interaction_session_factory_assembly_runtime_assembly_session_factory_extraction.py`

## Phase 209 (Interaction Session Factory Assembly Runtime Entry Assembly Session Factory Extraction)
- Continue thinning interaction-session assembly-runtime entry ownership by extracting assembly session routing into focused ownership.
- Keep assembly-runtime entry assembly session routing parity while introducing focused factory boundaries.
- Validate via focused assembly-runtime entry assembly session factory tests plus phase guard checks, then record `PHASE 209 COMPLETE`.

Phase 209 implementation files:
- `docs/NATIVE_CLIENT_PHASE209_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_ENTRY_ASSEMBLY_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase209_interaction_session_factory_assembly_runtime_entry_assembly_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeEntryAssemblySessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeEntryAssemblySessionFactoryTest.java`

## Phase 210 (Interaction Session Factory Assembly Runtime Entry Assembly Typed Routing Extraction)
- Continue interaction-session assembly-runtime entry decomposition by consolidating assembly typed routing through focused ownership.
- Route assembly-runtime entry assembly typed seams through focused assembly-runtime entry assembly session factory ownership while preserving compatibility sentinels.
- Validate via focused typed-routing tests plus phase guard checks, then record `PHASE 210 COMPLETE`.

Phase 210 implementation files:
- `docs/NATIVE_CLIENT_PHASE210_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_ENTRY_ASSEMBLY_TYPED_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase210_interaction_session_factory_assembly_runtime_entry_assembly_typed_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeEntryAssemblySessionFactory.java`

## Phase 211 (Interaction Session Factory Assembly Runtime Entry Bundle Factory Inputs Session Factory Extraction)
- Continue thinning interaction-session assembly-runtime entry ownership by extracting bundle-factory-input session routing into focused ownership.
- Keep assembly-runtime entry bundle-factory-input session routing parity while introducing focused factory boundaries.
- Validate via focused assembly-runtime entry bundle-factory-input session factory tests plus phase guard checks, then record `PHASE 211 COMPLETE`.

Phase 211 implementation files:
- `docs/NATIVE_CLIENT_PHASE211_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_ENTRY_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase211_interaction_session_factory_assembly_runtime_entry_bundle_factory_inputs_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeEntryBundleFactoryInputsSessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeEntryBundleFactoryInputsSessionFactoryTest.java`

## Phase 212 (Interaction Session Factory Assembly Runtime Entry Bundle Factory Input Typed Routing Extraction)
- Continue interaction-session assembly-runtime entry decomposition by consolidating bundle-factory-input typed routing through focused ownership.
- Route assembly-runtime entry bundle-factory-input typed seams through focused assembly-runtime entry bundle-factory-input session factory ownership while preserving compatibility sentinels.
- Validate via focused typed-routing tests plus phase guard checks, then record `PHASE 212 COMPLETE`.

Phase 212 implementation files:
- `docs/NATIVE_CLIENT_PHASE212_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_ENTRY_BUNDLE_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase212_interaction_session_factory_assembly_runtime_entry_bundle_factory_input_typed_routing_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeEntryBundleFactoryInputsSessionFactory.java`

## Phase 213 (Interaction Session Factory Entry Runtime Session Factory Extraction)
- Continue thinning interaction-session factory entry runtime ownership by extracting runtime session routing into focused ownership.
- Keep entry runtime session routing parity while introducing focused factory boundaries.
- Validate via focused entry runtime session factory tests plus phase guard checks, then record `PHASE 213 COMPLETE`.

Phase 213 implementation files:
- `docs/NATIVE_CLIENT_PHASE213_INTERACTION_SESSION_FACTORY_ENTRY_RUNTIME_SESSION_FACTORY_EXTRACTION_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `TASKS.md`
- `scripts/verify_phase213_interaction_session_factory_entry_runtime_session_factory_extraction.py`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntryRuntimeSessionFactory.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java`
- `runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryEntryRuntimeSessionFactoryTest.java`

## Phase 214 (Interaction Session Macro Pass AJ Signoff)
- Close Macro Pass AJ (Phases 209-214) with synchronized migration artifacts, Java-surface inventory updates, and full verification signoff.
- Validate extraction + consolidation parity with phase guard pack and targeted Java tests.
- Record `PHASE 214 COMPLETE` after signoff gates pass.

Phase 214 implementation files:
- `docs/NATIVE_CLIENT_PHASE214_INTERACTION_SESSION_MACRO_PASS_AJ_SIGNOFF_PLAN.md`
- `docs/NATIVE_CLIENT_PHASE_STATUS.md`
- `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- `TASKS.md`
- `scripts/verify_phase214_interaction_session_macro_pass_aj_signoff.py`
- `scripts/verify_phase213_interaction_session_factory_entry_runtime_session_factory_extraction.py`
- `scripts/verify_phase212_interaction_session_factory_assembly_runtime_entry_bundle_factory_input_typed_routing_extraction.py`
- `scripts/verify_phase211_interaction_session_factory_assembly_runtime_entry_bundle_factory_inputs_session_factory_extraction.py`
- `scripts/verify_phase210_interaction_session_factory_assembly_runtime_entry_assembly_typed_routing_extraction.py`
- `scripts/verify_phase209_interaction_session_factory_assembly_runtime_entry_assembly_session_factory_extraction.py`

## Runtime Ownership Model
- `runtime_core`: orchestration and lifecycle.
- `activities/*`: per-activity behavior ownership.
- `motor`: physical execution policy and dispatch.
- `state`: game-state ingestion and normalization.
- `protocol`: command and response envelope contracts.
- `security`: local-only bind and token/auth enforcement.
- `telemetry`: event snapshots, parity signals, and reason codes.

## Native Motor Design (No Java Ownership)
The motor becomes a native runtime service (`motor::MotorRuntime`) with one authority over movement/interaction execution.

### Inputs
- Intent stream from runtime/activity services (`InteractTarget`, `MoveCamera`, `DropItem`, `Pause`, `Abort`).
- Normalized target data from `state` (screen bounds, target metadata, confidence, world context).
- Timing/humanization profiles from DB-backed metrics and policy config.

### Pipeline
1. Gate check: cooldown, anti-repeat, focus/context safety.
2. Path synthesis: stochastic/humanized path generation.
3. Timing plan: dwell, burst, hesitation, and slowdown curves.
4. Dispatch: native input adapter emits mouse/keyboard events.
5. Verification: confirm progress from state feedback.
6. Recover/defer: retry/reacquire/abort with explicit reason codes.

### Motor Guardrails
- Keep one motor authority (no duplicate gating layers).
- No deterministic fallback anchors for cursor/camera/target resolution.
- On sampling failure, defer/reacquire/abort rather than deterministic substitution.
- Keep anti-repeat target protections owned by activity/runtime services.

## Migration Phases
1. Contract freeze and schema versioning.
2. Native bridge daemon (loopback + auth token) and protocol validation.
3. Native runtime core + motor baseline.
4. Woodcutting vertical slice (end-to-end native ownership).
5. Remaining activity migration (mining, fishing, combat, banking).
6. Native overlay/UI replacement.
7. Java shim freeze + native-default cutover.
8. Java shadow runtime path removal.
9. Java plugin shim retirement (`XPToolPlugin`/`XPToolConfig`) after gate pack signoff.
10. Native-only operations hardening and verification tightening.
11. Native host cutover waves (delete-first scaffolding, then port-first behavior).

## Woodcutting Vertical Slice Task Breakdown
1. Define native woodcutting runtime contracts.
2. Implement state snapshot translator for woodcutting target candidates.
3. Implement target selection and anti-repeat guard window ownership in `native-core`.
4. Implement motor dispatch integration for woodcutting intents (`InteractTarget`, `Pause`, `Abort`).
5. Add telemetry events for candidate scoring, dispatch decisions, and no-progress outcomes.
6. Build parity harness scenario for woodcutting loop runs against accepted baseline traces.
7. Gate promotion on parity thresholds (tick cadence, outcome rate, reason-code coverage).

## Cutover Criteria
- Tick cadence parity within agreed tolerance.
- Command semantics parity with reason-code coverage.
- Activity outcome parity in parity harness tests.
- Security parity: local-only enforcement and token-auth required.
- Soak window passes without critical regressions.
