# Thin Client Contract (Step 4)

## Goal
Define a stable planner contract so the client can remain thin while backend owns decision policy.

## Transport
- Primary: HTTPS JSON API (request/response).
- Compatibility fallback: existing NDJSON command bus.
- Client must fail closed to safe/no-op behavior when planner is unavailable.

## Versioning
- Every request includes:
  - `contractVersion` (semantic string, ex: `1.0`)
  - `clientBuild` (plugin/runtime build id)
  - `schemaHash` (optional integrity hint)
- Server replies with:
  - `contractVersion`
  - `decisionId`
  - `status` (`ok`, `no_action`, `reject`, `error`)

## Request DTO: `PlannerDecisionRequest`
- `sessionId`: string
- `capturedAtUnixMillis`: long
- `tick`: int
- `snapshot`: minimized object
- `capabilities`: object

### Snapshot Fields (minimum)
- `loggedIn`: bool
- `bankOpen`: bool
- `shopOpen`: bool
- `worldId`: int?
- `playerAnimation`: int?
- `inventoryCounts`: map<int,int>
- `bankCounts`: map<int,int> (when relevant)
- `shopCounts`: map<int,int> (when relevant)
- `inventorySlotsUsed`: int?
- `hitpointsCurrent`: int?
- `hitpointsMax`: int?
- `nearbyObjects`: compact list (only required fields)
- `nearbyNpcs`: compact list (only required fields)
- `nearbyGroundItems`: compact list (only required fields)

### Capabilities Fields
- `supportedCommandTypes`: string[]
- `motorModes`: string[]
- `featureFlags`: map<string,bool>

## Response DTO: `PlannerDecisionResponse`
- `decisionId`: string
- `status`: string
- `commands`: `CommandSpec[]`
- `holdUntilTick`: int? (optional throttle/no-op hint)
- `reason`: string
- `diagnostics`: object? (non-sensitive)

## Command DTO: `CommandSpec`
- `commandType`: string
- `payload`: object
- `reason`: string
- `priority`: int? (optional)
- `idempotencyKey`: string

## Reliability Rules
- Request timeout: `250ms` target, `750ms` hard cap.
- Retry: at most 1 retry for transient network errors.
- Use idempotency key per decision request.
- If timeout/error:
  - fallback to local safe no-op/deferred path
  - emit bounded telemetry reason code

## Safety Rules
- Client validates command type against `CommandSupportPolicy`.
- Client validates required payload fields before dispatch.
- Unknown command types are rejected locally and logged as policy-safe failures.
- No server command may bypass local motor ownership/gating checks.

## Privacy Rules
- Do not send secrets, credentials, or auth tokens inside snapshot payload.
- Keep identifiers pseudonymous where possible.
- Diagnostics from server must be non-sensitive and bounded.

## Compatibility / Rollout
- Phase 1: shadow mode (request decisions, do not execute).
- Phase 2: compare backend vs local decisions, record divergence.
- Phase 3: controlled execution for limited sessions.
- Phase 4: default to backend planner with local fallback.

## Step Exit Criteria
Step 4 is complete when:
- request/response DTOs are frozen for v1
- timeout/retry/fallback behavior is explicit
- command validation and safety ownership remain local
