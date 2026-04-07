# IP Protection Sensitivity Inventory (Step 2)

## Summary
The codebase already has strong modular seams (`Host` adapters + `*Wiring` classes), but high-value behavior logic is still split across:
- client plugin execution/runtime layers
- local Python planner runtime (`src/runelite_planner`)

## Classification

### A) Must Stay Client-Side
These rely on RuneLite APIs and direct local interaction control.
- `runelite-plugin/src/main/java/com/xptool/XPToolSnapshotService.java`
- `runelite-plugin/src/main/java/com/xptool/executor/MotorRuntime.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorInteractionRuntimeWiring.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorMotorRuntimeWiring.java`
- `runelite-plugin/src/main/java/com/xptool/sessions/SessionManager.java`

Note:
- `XPToolPlugin` Java shim ownership was retired in native migration Phase 9.

Why:
- owns client tick/event hooks
- owns widget/menu/canvas coordinates and input dispatch
- owns motor ownership lease/gating for local safety

### B) High-Value IP (Best Candidate to Move Backend-Side)
These contain decision policy, prioritization, and strategy behavior.
- `src/runelite_planner/runtime_core/core.py`
- `src/runelite_planner/runtime_core/intent_resolver.py`
- `src/runelite_planner/runtime_core/interaction_gate.py`
- `src/runelite_planner/runtime_core/scheduler.py`
- activity strategies in `src/runelite_planner/*.py`

Why:
- encodes action selection policy and retries
- contains strategy/priority heuristics
- easiest place to centralize proprietary decision logic

### C) Mixed / Boundary Components
These are orchestration or integration points and should remain thin.
- `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorGameplayRuntimeWiring.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutorServiceWiring.java`
- `src/runelite_planner/bridge.py`

Why:
- should primarily route, adapt, and validate
- should avoid owning deep behavioral state where possible

### D) Logging / Telemetry Privacy Surface
- `runelite-plugin/src/main/java/com/xptool/JsonLogSnapshotEmitter.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutionTelemetryFileSink.java`
- `runelite-plugin/src/main/java/com/xptool/executor/ExecutionPayloadRedactor.java`
- `src/runelite_planner/bridge.py` (snapshot and execution parsing)

Why:
- carries high-volume runtime data that may include sensitive context
- logging defaults influence data exposure risk

## Key Findings
- `CommandExecutor` is still very large and holds significant state and helper behavior.
- Existing `Host` + `Wiring` pattern is strong and should be the migration backbone.
- Current command transport is file-based NDJSON; this is a good compatibility fallback path.
- Snapshot and execution logs are rich enough to drive planning externally today.

## Recommended Ownership Direction
- Client:
  - sensing (snapshot extraction)
  - execution (motor + menu/widget actions)
  - strict safety checks local to execution
- Backend:
  - planning policy
  - intent prioritization
  - retry policy tuning
  - feature flag and runtime policy control

## Step Exit Criteria
Step 2 is complete when:
- each major component is classified as client-owned, backend-owned, or boundary-only
- migration candidates are explicit
- logging/privacy surfaces are explicitly listed
