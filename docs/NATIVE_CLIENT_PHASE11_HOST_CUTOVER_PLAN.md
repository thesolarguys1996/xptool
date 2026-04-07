# Native Client Phase 11 Host Cutover Plan

Last updated: 2026-04-05

## Goal
Start native host ownership cutover by replacing remaining Java host/wiring and behavior surfaces with native-owned boundaries in staged waves.

## Scope
- Migration execution planning and first-wave kickoff only.
- Keep Java/RuneLite as transitional shims during wave execution, with no new long-lived ownership added.
- Preserve local-only bridge security posture and parity/soak guardrails.

## Workstreams
1. Surface-wave mapping
   - Use `docs/NATIVE_JAVA_SURFACE_INVENTORY.md` as canonical baseline.
   - Split execution into `delete-first scaffolding` and `port-first behavior` waves.
2. Host-boundary migration
   - Replace Java host-adapter/wiring ownership with native-host contracts and thin compatibility adapters.
3. Runtime behavior migration
   - Port behavior ownership from Java packages (`executor`, `activities`, `systems`, `sessions`) into native runtime components.
4. Verification + cutover gates
   - Keep parity/cutover/soak gates green for each wave before any Java removals.

## Execution Slices
1. `11.1` Define Phase 11 scope, wave plan, and kickoff artifacts.
2. `11.2` Execute delete-first scaffolding wave and remove migrated Java scaffolds.
3. `11.3` Execute bridge/executor boundary wave with native contract ownership.
4. `11.4` Execute port-first behavior wave in native runtime services.
5. `11.5` Run full gate pack and mark `PHASE 11 COMPLETE`.

## Phase 11 Slice Status
- `11.1` complete.
- `11.2` complete.
- `11.3` complete.
- `11.4` complete.
- `11.5` complete.

## Phase 11.1 Outputs
- Added Phase 11 host-cutover plan:
  - `docs/NATIVE_CLIENT_PHASE11_HOST_CUTOVER_PLAN.md`
- Updated migration plan with Phase 11 ownership and deliverables:
  - `docs/NATIVE_CLIENT_MIGRATION_PLAN.md`
- Updated phase board to mark kickoff:
  - `docs/NATIVE_CLIENT_PHASE_STATUS.md` (`PHASE 11 STARTED`)
- Updated task checklist with Phase 11 execution items:
  - `TASKS.md`

## Phase 11.2 Batch 1 Outputs
- Migrated account-runtime host wrappers into direct host implementations:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorAccountRuntimeWiring.java`
- Removed delete-first scaffolding files:
  - `runelite-plugin/src/main/java/com/xptool/executor/LoginScreenStateResolverHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/LoginRuntimeHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/LogoutRuntimeHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ResumePlannerHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/BreakRuntimeHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 2 Outputs
- Migrated account-runtime coordinator host wrappers into focused factory wiring:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorAccountRuntimeInputFactory.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Removed delete-first scaffolding files:
  - `runelite-plugin/src/main/java/com/xptool/executor/AccountRuntimeTickHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/AccountRuntimeOrchestratorHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 3 Outputs
- Migrated runtime-orchestration host wrappers into focused runtime coordinator host factory:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorRuntimeCoordinatorHostFactory.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Removed delete-first scaffolding files:
  - `runelite-plugin/src/main/java/com/xptool/executor/LoginBreakRuntimeHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/RuntimeTickOrchestratorHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/LifecycleShutdownHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 4 Outputs
- Migrated runtime-service host wrappers into focused runtime service host factory:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorRuntimeServiceHostFactory.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Removed delete-first scaffolding files:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandIngestLifecycleHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandDispatchHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/RuntimeShutdownHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 5 Outputs
- Migrated login/menu interaction host wrappers into focused login interaction host factory:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorLoginInteractionHostFactory.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Removed delete-first scaffolding files:
  - `runelite-plugin/src/main/java/com/xptool/executor/LoginSubmitTargetPlannerHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/MenuEntryTargetMatcherHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/LoginInteractionControllerHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/LogoutInteractionControllerHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 6 Outputs
- Migrated idle/runtime host wrappers into focused idle host factory:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorIdleHostFactory.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorRuntimeDomainWiring.java`
- Removed delete-first scaffolding files:
  - `runelite-plugin/src/main/java/com/xptool/executor/IdleGateTelemetryHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/IdleSuppressionHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/IdleOffscreenMoveHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/IdleRuntimeHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 7 Outputs
- Migrated resolver/policy host-wrapper ownership into domain/service wiring:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorServiceWiring.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorSkillingDomainWiring.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorCombatDomainWiring.java`
- Removed delete-first scaffolding files:
  - `runelite-plugin/src/main/java/com/xptool/executor/AgilityTargetResolverHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/FishingTargetResolverHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/CombatTargetPolicyHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/CombatTargetResolverHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 8 Outputs
- Migrated engine host-wrapper ownership into direct host wiring:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorEngineWiring.java`
- Removed delete-first scaffolding files:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandIngestorHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/TargetSelectionEngineHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/CameraMotionHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 9 Outputs
- Migrated interaction-controller host-wrapper ownership into focused host factory wiring:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorInteractionControllerHostFactories.java`
- Removed delete-first scaffolding files:
  - `runelite-plugin/src/main/java/com/xptool/executor/InventorySlotInteractionControllerHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/BankMenuInteractionControllerHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 10 Outputs
- Migrated scene/runtime host-wrapper ownership into direct wiring:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorSceneDomainWiring.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorRuntimeDomainWiring.java`
- Removed delete-first scaffolding files:
  - `runelite-plugin/src/main/java/com/xptool/executor/SceneCacheScannerHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/RandomEventDismissRuntimeHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 11 Outputs
- Migrated gameplay action host-wrapper ownership into direct runtime/factory wiring:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayRuntimeWiring.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayHostFactories.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayServiceHostsAssembler.java`
- Removed delete-first scaffolding files:
  - `runelite-plugin/src/main/java/com/xptool/executor/SceneObjectActionHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/GroundItemActionHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 12 Outputs
- Migrated shop/world-hop host-wrapper ownership into direct runtime/factory wiring:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayRuntimeWiring.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayHostFactories.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayServiceHostsAssembler.java`
- Removed delete-first scaffolding files:
  - `runelite-plugin/src/main/java/com/xptool/executor/ShopBuyCommandHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/WorldHopCommandHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 13 Outputs
- Migrated typing/npc-context host-wrapper ownership into direct runtime/domain/factory wiring:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorAccountRuntimeInputs.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorAccountRuntimeInputFactory.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorAccountRuntimeWiring.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorCombatDomainWiring.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayRuntimeWiring.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayHostFactories.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayServiceHostsAssembler.java`
- Removed delete-first scaffolding files:
  - `runelite-plugin/src/main/java/com/xptool/executor/HumanTypingHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/NpcContextMenuTestHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 14 Outputs
- Migrated skilling resolver host-wrapper ownership into direct domain/factory wiring:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorSkillingDomainWiring.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorResolverHostFactories.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayServiceHostsAssembler.java`
- Removed delete-first scaffolding files:
  - `runelite-plugin/src/main/java/com/xptool/executor/WoodcuttingTargetResolverHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/MiningTargetResolverHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 15 Outputs
- Migrated combat navigation host-wrapper ownership into direct domain/factory/runtime wiring:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorCombatDomainWiring.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorCombatHostFactories.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayRuntimeWiring.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayHostFactories.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayServiceHostsAssembler.java`
- Removed delete-first scaffolding files:
  - `runelite-plugin/src/main/java/com/xptool/executor/WalkCommandHostAdapter.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/BrutusCombatSystemHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 16 Outputs
- Migrated interaction command host-wrapper ownership into direct domain/runtime/factory wiring:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorCombatDomainWiring.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayRuntimeWiring.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayHostFactories.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayServiceHostsAssembler.java`
- Removed delete-first scaffolding file:
  - `runelite-plugin/src/main/java/com/xptool/executor/InteractionCommandHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 17 Outputs
- Migrated mining command host-wrapper ownership into direct skilling domain/factory wiring:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorSkillingDomainWiring.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorSkillingHostFactories.java`
- Removed delete-first scaffolding file:
  - `runelite-plugin/src/main/java/com/xptool/executor/MiningCommandHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 18 Outputs
- Migrated fishing command host-wrapper ownership into direct skilling domain/factory wiring:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorSkillingDomainWiring.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorSkillingHostFactories.java`
- Removed delete-first scaffolding file:
  - `runelite-plugin/src/main/java/com/xptool/executor/FishingCommandHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 19 Outputs
- Migrated woodcutting command host-wrapper ownership into direct skilling domain/factory wiring:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorSkillingDomainWiring.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorSkillingHostFactories.java`
- Removed delete-first scaffolding file:
  - `runelite-plugin/src/main/java/com/xptool/executor/WoodcuttingCommandHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 20 Outputs
- Migrated drop runtime host-wrapper ownership into direct runtime domain/runtime-input wiring:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorRuntimeDomainWiring.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayRuntimeInputs.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayRuntimeInputFactory.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayRuntimeWiring.java`
- Removed delete-first scaffolding file:
  - `runelite-plugin/src/main/java/com/xptool/executor/DropRuntimeHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 21 Outputs
- Migrated combat command host-wrapper ownership into direct combat domain/runtime/factory wiring:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorCombatDomainWiring.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorCombatHostFactories.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayRuntimeWiring.java`
- Removed delete-first scaffolding file:
  - `runelite-plugin/src/main/java/com/xptool/executor/CombatCommandHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.2 Batch 22 Outputs
- Migrated bank command host-wrapper ownership into direct bank domain/factory/service-host wiring:
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorBankDomainWiring.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorBankHostFactories.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayServiceHostsAssembler.java`
- Removed delete-first scaffolding file:
  - `runelite-plugin/src/main/java/com/xptool/executor/BankCommandHostAdapter.java`
- Updated Java surface baseline counts after removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.3 Batch 1 Outputs
- Introduced a typed bridge/executor dispatch-settings boundary contract and isolated executor static-setting ownership behind one adapter:
  - `runelite-plugin/src/main/java/com/xptool/bridge/BridgeDispatchSettings.java`
  - `runelite-plugin/src/main/java/com/xptool/bridge/ExecutorBridgeDispatchSettings.java`
- Routed bridge runtime/config/heartbeat services through the boundary contract instead of direct executor static calls:
  - `runelite-plugin/src/main/java/com/xptool/bridge/BridgeRuntime.java`
  - `runelite-plugin/src/main/java/com/xptool/bridge/BridgeIpcServer.java`
  - `runelite-plugin/src/main/java/com/xptool/bridge/BridgeDispatchConfigService.java`
  - `runelite-plugin/src/main/java/com/xptool/bridge/BridgeHeartbeatService.java`
- Updated Java surface inventory counts after boundary-contract additions:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.3 Batch 2 Outputs
- Consolidated dispatch-settings runtime state ownership under bridge contract helpers so bridge package has no direct executor imports:
  - `runelite-plugin/src/main/java/com/xptool/bridge/BridgeDispatchSettings.java`
  - `runelite-plugin/src/main/java/com/xptool/bridge/ExecutorBridgeDispatchSettings.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/BridgeDispatchRuntimeSettings.java`
- Kept executor compatibility static API as a thin delegate to bridge-owned runtime state for staged migration safety.
- Updated inventory log with no-count-delta boundary consolidation note:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.3 Batch 3 Outputs
- Removed executor dispatch-settings compatibility wrapper and shifted command ingest/dispatch policy checks to bridge-owned runtime state:
  - `runelite-plugin/src/main/java/com/xptool/executor/BridgeDispatchRuntimeSettings.java` (removed)
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Retained bridge-side contract boundary ownership:
  - `runelite-plugin/src/main/java/com/xptool/bridge/BridgeDispatchSettings.java`
  - `runelite-plugin/src/main/java/com/xptool/bridge/ExecutorBridgeDispatchSettings.java`
- Updated Java surface inventory counts after wrapper removal:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.3 Batch 4 Outputs
- Introduced executor-side live-dispatch policy contract seam for command ingest/dispatch checks:
  - `runelite-plugin/src/main/java/com/xptool/executor/BridgeLiveDispatchPolicy.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/BridgeRuntimeStateLiveDispatchPolicy.java`
- Updated `CommandExecutor` to consume `BridgeLiveDispatchPolicy` instead of directly reading bridge runtime-state helpers:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Updated Java surface inventory counts after policy-boundary additions:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.3 Batch 5 Outputs
- Extracted bridge command dispatch-mode decisions (`shadow/live mode`, allowlist gating, bridge telemetry mode)
  from `CommandExecutor` into a focused executor policy component:
  - `runelite-plugin/src/main/java/com/xptool/executor/BridgeCommandDispatchModePolicy.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Kept bridge runtime-state access isolated to existing policy adapter chain:
  - `runelite-plugin/src/main/java/com/xptool/executor/BridgeRuntimeStateLiveDispatchPolicy.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/BridgeLiveDispatchPolicy.java`
- Updated Java surface inventory counts after policy-component addition:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.3 Batch 6 Outputs
- Moved live/shadow command-evaluation path selection and `shadow_would_dispatch` outcome classification
  behind `BridgeCommandDispatchModePolicy`:
  - `runelite-plugin/src/main/java/com/xptool/executor/BridgeCommandDispatchModePolicy.java`
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Kept command-ingest lifecycle wiring intact while replacing direct `CommandExecutor` helper ownership with policy calls:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Updated inventory log with no-count-delta boundary note:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.3 Batch 7 Outputs
- Extracted shared command ingest/dispatch precheck pipeline (command-id validation, envelope verification,
  runtime preconditions) into focused policy component:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandDispatchPrecheckPolicy.java`
- Updated `CommandExecutor` live/shadow evaluation paths to consume the shared precheck policy:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Updated Java surface inventory counts after precheck-policy addition:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.3 Batch 8 Outputs
- Extracted command-decision to execution-outcome mapping from `CommandExecutor` into a focused policy component:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandDecisionOutcomePolicy.java`
- Updated `CommandExecutor` live/shadow command evaluation paths to consume the outcome policy:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Updated Java surface inventory log after outcome-policy extraction:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.3 Batch 9 Outputs
- Extracted shadow-dispatch outcome assembly and unsupported-command outcome handling from
  `CommandExecutor` into a focused policy component:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandShadowDispatchPolicy.java`
- Updated `CommandExecutor` shadow evaluation path to consume the shadow-dispatch policy:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Updated Java surface inventory counts after shadow-dispatch policy addition:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.3 Batch 10 Outputs
- Extracted shared live/shadow row-evaluation precheck and exception-guard flow from `CommandExecutor`
  into a focused policy component:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandRowEvaluationPolicy.java`
- Updated `CommandExecutor` live/shadow evaluation paths to consume row-evaluation policy helpers:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Updated Java surface inventory counts after row-evaluation policy addition:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.3 Batch 11 Outputs
- Extracted live/shadow command-row routing branch ownership from `CommandExecutor` into a focused
  policy component:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandRowDispatchRoutingPolicy.java`
- Updated `CommandExecutor` command-row router to delegate through the routing policy:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Updated Java surface inventory counts after routing-policy addition:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.3 Batch 12 Outputs
- Extracted live command-evaluation precheck/dispatch outcome ownership from `CommandExecutor` into a
  focused policy component:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandLiveDispatchPolicy.java`
- Updated `CommandExecutor` live command evaluation path to delegate through the policy:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Updated Java surface inventory counts after live-dispatch policy addition:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.3 Batch 13 Outputs
- Extracted shadow command-evaluation precheck/support-check/outcome ownership from `CommandExecutor`
  into a focused policy component:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandShadowEvaluationPolicy.java`
- Updated `CommandExecutor` shadow command evaluation path to delegate through the policy:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Updated Java surface inventory counts after shadow-evaluation policy addition:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.3 Completion
- Bridge/executor boundary wave completed with focused policy ownership for:
  - live/shadow routing,
  - precheck evaluation,
  - live dispatch decision/outcome mapping,
  - shadow dispatch evaluation/outcome mapping.
- Phase status transitioned to:
  - `11.3` complete,
  - `11.4` in progress.

## Phase 11.4 Batch 1 Outputs
- Extracted command-row evaluation behavior ownership from `CommandExecutor` into a dedicated runtime service:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandEvaluationService.java`
- Updated command ingest lifecycle wiring to consume runtime service evaluation callbacks:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Updated Java surface inventory counts after runtime-service extraction:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.4 Batch 2 Outputs
- Extracted queued-command idle arming/suppression behavior ownership from `CommandExecutor`
  into a focused runtime service:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandQueueIdleArmingService.java`
- Extracted planner-tag parsing from `CommandExecutor` into focused policy helper:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandRowPlannerTagPolicy.java`
- Updated command-queue ingest wiring to delegate idle arming + planner-tag parsing behavior:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Updated Java surface inventory counts after queue-idle/planner-tag extraction:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.4 Batch 3 Outputs
- Extracted parsed-command queue ingest behavior and row mapping ownership from `CommandExecutor`
  into a focused runtime service:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandQueueIngestService.java`
- Updated command-ingest callback wiring to enqueue parsed command rows via runtime service:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Updated Java surface inventory counts after queue-ingest service extraction:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.4 Batch 4 Outputs
- Extracted command-ingest callback behavior ownership from `CommandExecutor` into a focused runtime service:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandIngestCallbackService.java`
- Updated command-ingest wiring to delegate config/attach/truncate/parse/queue callbacks through runtime service:
  - `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- Updated Java surface inventory counts after command-ingest callback extraction:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`

## Phase 11.4 Completion
- Port-first behavior migration wave completed with parity-gated runtime-service extractions across:
  - command evaluation/routing,
  - queue idle arming/planner-tag parsing,
  - parsed-command ingest/callback paths.
- Phase status transitioned to:
  - `11.4` complete.

## Phase 11.5 Outputs
- Full Phase 11 signoff gate pack executed successfully:
  - `python scripts/verify_java_runtime_ownership_blocked.py`
  - `python scripts/verify_java_shim_retirement_gates.py`
  - `python scripts/verify_native_only_operations_hardening.py`
  - `python scripts/verify_native_cutover.py`
  - `python scripts/verify_native_soak_report.py --min-iterations 6 --max-failures 0 --max-age-hours 48`
- Soak verification summary:
  - iterations: `6`
  - failedIterations: `0`
  - report: `runtime/native-soak/soak-report.json`
- Phase status transitioned to:
  - `11.5` complete.
  - `PHASE 11 COMPLETE` recorded.

## Exit Criteria
- Delete-first scaffolding wave is migrated and removed behind passing parity/cutover gates.
- Port-first behavior ownership is migrated into native runtime modules with regression coverage.
- Full verification/signoff pack passes and `PHASE 11 COMPLETE` is recorded.
