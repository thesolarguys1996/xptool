# Project Structure: Global vs Activity Boundaries

## Goal
Prevent accidental global behavior changes when tuning a single activity/task.

## Global Runtime Layer
- `src/runelite_planner/runtime_core/`
- `src/runelite_planner/runner.py`
- `src/runelite_planner/bridge.py`
- `src/runelite_planner/control_plane.py`
- `src/runelite_planner/remote_planner.py`
- `runelite-plugin/src/main/java/com/xptool/executor/` runtime/orchestration wiring
- `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java` should remain orchestration-focused; activity runtime transitions belong in dedicated coordinators (`SkillingRuntimeCoordinator`, `CombatRuntimeCoordinator`).
- `runelite-plugin/src/main/java/com/xptool/executor/activity/` contains activity transition interfaces (`SkillingRuntimeTransitions`, `CombatRuntimeTransitions`) used as package boundaries for `CommandExecutor` wiring.
- `runelite-plugin/src/main/java/com/xptool/executor/activity/` also hosts per-activity runtime contracts/adapters (`ActivityRuntime`, `FishingActivityRuntime`, `MiningActivityRuntime`, `WoodcuttingActivityRuntime`, and delegating adapters) so activity behavior can evolve independently.
- `runelite-plugin/src/main/java/com/xptool/executor/activity/ActivityRuntimeRegistry.java` is the runtime routing registry used by `CommandExecutor` for typed activity runtime resolution.
- `runelite-plugin/src/main/java/com/xptool/executor/activity/FishingRuntimeService.java`, `WoodcuttingRuntimeService.java`, and `MiningRuntimeService.java` are extracted per-activity runtime services for transition/timing behavior.
- `runelite-plugin/src/main/java/com/xptool/executor/activity/WoodcuttingTargetStateService.java`, `MiningTargetStateService.java`, and `FishingTargetStateService.java` own activity target-lock state so `CommandExecutor` does not mutate lock/preference fields directly.
- `runelite-plugin/src/main/java/com/xptool/executor/activity/WoodcuttingSelectionService.java` and `MiningSelectionService.java` own selected-target collections so `CommandExecutor` does not mutate selection sets directly.
- `runelite-plugin/src/main/java/com/xptool/executor/activity/WoodcuttingSelectionController.java` and `MiningSelectionController.java` own UI selection/toggle flows (including scene->world resolution via `SelectionWorldPointResolver`) so `CommandExecutor` only delegates.
- `runelite-plugin/src/main/java/com/xptool/executor/WorldViewResolver.java` centralizes world-view lookup/fallback (`byId -> top-level`) so runtime wiring and scanners use one world-view access boundary.
- `runelite-plugin/src/main/java/com/xptool/executor/SceneQueryService.java` owns top-level NPC/object/ground-item proximity scans so scanning loops are not embedded in `CommandExecutor`.
- `runelite-plugin/src/main/java/com/xptool/executor/ItemObjectNameResolver.java` owns item/object display-name lookups so interaction host wiring does not depend on in-class lookup helpers.
- `runelite-plugin/src/main/java/com/xptool/executor/SceneInteractionPointService.java` owns scene/ground interaction click-point resolution so interaction-point helper logic is not embedded in `CommandExecutor`.
- `runelite-plugin/src/main/java/com/xptool/executor/*HostFactories.java` owns host/supplier assembly (`ExecutorSkillingHostFactories`, `ExecutorCombatHostFactories`, `ExecutorBankHostFactories`, `ExecutorGameplayHostFactories`, `ExecutorResolverHostFactories`) so constructor wiring stays thin.
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayRuntimeInputs.java` provides typed gameplay wiring inputs (`ServiceHosts`, `RuntimeInputs`) to avoid fragile positional wiring.
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorMotorRuntimeInputs.java` and `ExecutorAccountRuntimeInputs.java` provide typed wiring inputs for motor/account runtime assembly.
- `runelite-plugin/src/main/java/com/xptool/executor/ActivityIdlePolicyRegistry.java` centralizes per-activity idle defaults (profile key + idle mode) so default idle tuning does not live in `CommandExecutor` fields.
- `runelite-plugin/src/main/java/com/xptool/executor/IdleRuntime.java` resolves context behavior/cadence through activity idle policy wiring rather than embedding activity-specific cadence constants.
- `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java` keeps idle arming activity-scoped (per planner/activity key) so one activity tune path does not globally disarm or retune others.
- `runelite-plugin/src/main/java/com/xptool/executor/IdleArmingService.java` owns idle arming/offscreen-arming state and plannerTag->activity/context mapping so `CommandExecutor` delegates arming mutations.
- Idle arming telemetry should include arming source tags from `IdleArmingService` (`idleArmSource`, `idleOffscreenArmSource`) so loop-test diagnostics can identify whether arming came from queue flow, override flow, or bootstrap flow.
- `runelite-plugin/src/main/java/com/xptool/executor/CommandDispatchService.java` owns command dispatch routing (`execute(...)` branch handling + generic family routing) so `CommandExecutor` delegates command execution.
- `runelite-plugin/src/main/java/com/xptool/executor/IdleGateTelemetryService.java` owns idle action/camera/interact gate checks and idle gate telemetry/snapshot assembly so `CommandExecutor` delegates idle window policy decisions.
- `runelite-plugin/src/main/java/com/xptool/executor/AccountRuntimeOrchestrator.java` owns login/logout/break runtime start/stop orchestration helpers so `CommandExecutor` delegates account-runtime control flow.
- `runelite-plugin/src/main/java/com/xptool/executor/RuntimeShutdownService.java` owns operational runtime teardown/reset flow (`stopOperationalRuntimeState(...)`) so `CommandExecutor` delegates shutdown sequencing.
- `runelite-plugin/src/main/java/com/xptool/executor/AccountRuntimeTickCoordinator.java` owns login/logout runtime tick-advance sequencing and throttle windows so `CommandExecutor` delegates client-tick account advancement.
- `runelite-plugin/src/main/java/com/xptool/executor/CommandIngestLifecycleService.java` owns command queue ingest pumping, per-tick mechanical dispatch limits, and outcome emit-status mapping so `CommandExecutor` delegates ingest/outcome lifecycle.

This layer owns shared contracts, transport, dispatch, and orchestration.

## Activity/Task Layer
- `src/runelite_planner/activities/registry.py`: centralized activity metadata/defaults
- `src/runelite_planner/activities/builders.py`: per-activity validation + strategy assembly
- `src/runelite_planner/activities/combat_presets.py`: combat preset/task config
- Activity strategy modules:
  - `woodcutting.py`, `mining.py`, `fishing.py`, `agility.py`, `combat.py`, `store_bank.py`, probes

This layer owns activity-specific behavior and parameter parsing.

## UI Layer
- `src/runelite_planner/gui.py` should stay orchestration-focused.
- GUI should call activity builders instead of embedding activity-specific parsing branches.
- Activity lists/defaults should come from `activities/registry.py` rather than hardcoded lists.

## Native Migration Layer
- `native-core/` is the native runtime ownership path for orchestration, activity runtimes, state normalization, and motor dispatch.
- `native-bridge/` is the native contract ingress/egress path for loopback authenticated command/telemetry envelopes.
- `native-ui/` is the native status/overlay/config replacement path.
- `schemas/native/` is the canonical shared contract package for native command and telemetry schema versions.
- During migration, Java/RuneLite code should remain compatibility adapters and must not accumulate new long-lived behavior ownership.

## Refactor Rule
If a change affects only one activity/task, implement it in the activity/task layer first.
Touch global runtime only for shared contract or cross-activity interface changes.
