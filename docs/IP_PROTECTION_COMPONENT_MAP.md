# Component Ownership and Refactor Map (Step 3)

## Objective
Keep `CommandExecutor` orchestration-only while creating clear boundaries between:
- client execution runtime
- planner decision runtime
- transport/adaptation layers

## Target Logical Architecture

`Snapshot Source -> Planner Boundary -> Decision Runtime -> Command Contract -> Executor Boundary -> Motor/Action Runtime`

## Component Ownership

### 1) Client Snapshot Adapter (Client-Owned)
Responsibilities:
- read RuneLite state/events
- emit minimized snapshot DTOs for planner use

Current anchors:
- `XPToolSnapshotService`
- `Snapshot` models

### 2) Planner Gateway (Boundary)
Responsibilities:
- request/response transport with timeout/fallback
- schema/version enforcement
- auth/session headers

Current anchors:
- `src/runelite_planner/bridge.py` (will evolve from file bridge)

### 3) Decision Runtime (Backend-Owned)
Responsibilities:
- intent selection
- retry and scheduling policy
- activity strategy heuristics

Current anchors:
- `src/runelite_planner/runtime_core/*`
- `src/runelite_planner/* activity modules`

### 4) Command Contract Layer (Boundary)
Responsibilities:
- map planner decisions to supported executor command family
- enforce command type allowlist and payload constraints

Current anchors:
- `CommandSupportPolicy`
- `CommandRowParser`
- `CommandFamilyRouter`

### 5) Executor Runtime (Client-Owned)
Responsibilities:
- validate runtime preconditions
- perform local interactions (menu/widget/input)
- maintain motor lease/gating authority

Current anchors:
- `CommandExecutor` (orchestration)
- `Executor*Wiring`
- runtime/services (`DropRuntime`, `IdleRuntime`, etc.)
- `SessionManager`

## Refactor Direction for `CommandExecutor`
- Keep:
  - dependency wiring
  - tick/event routing
  - telemetry emission
  - command dispatch delegation
- Move out (as needed):
  - any remaining behavior-heavy helpers
  - domain-specific gate/timing logic not already owned by a runtime/service

## Minimal Change Principle
- Preserve current `Host` adapter seams.
- Add new boundary adapters instead of expanding plugin UI/config surface.
- Keep file command bus as fallback compatibility path during migration.

## Step Exit Criteria
Step 3 is complete when:
- each logical component has one owner
- `CommandExecutor` responsibilities are explicitly bounded
- migration path does not require plugin-surface expansion by default
