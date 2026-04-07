# Control Plane Skeleton (Step 5)

This repo now includes a planner-side control-plane skeleton for:
- session lifecycle
- feature flags
- global/activity kill switches
- local NDJSON audit events

## Runtime Modes

1. Disabled (default)
- no control-plane calls
- planner behavior unchanged

2. File mode
- local JSON policy file controls flags and kill switches
- useful for local testing

3. HTTP mode
- planner calls control-plane endpoints
- supports bearer token via environment variable

## CLI Flags

- `--control-plane-url <base-url>`
- `--control-plane-policy-file <path>`
- `--control-plane-token-env <ENV_VAR>` (default: `XPTOOL_CONTROL_PLANE_TOKEN`)
- `--control-plane-timeout-seconds <float>` (default: `2.0`)
- `--control-plane-signing-key-env <ENV_VAR>` (default: `XPTOOL_CONTROL_PLANE_SIGNING_KEY`)
- `--control-plane-signing-key-id <id>` (optional key id header)
- `--control-plane-replay-window-seconds <float>` (default: `30.0`)
- `--control-plane-require-response-replay-fields`
- `--control-plane-allow-insecure-http` (default: disabled)
- `--control-plane-contract-version <value>` (default: `1.0`)
- `--control-plane-client-build <value>` (default: `xptool-local`)
- `--control-plane-require-decision-id`
- `--control-plane-poll-seconds <float>` (default: `3.0`)
- `--control-plane-audit-path <path>` (default: `runtime/xptool-state/control-plane-audit.ndjson`)
- `--control-plane-disable-audit`
- `--control-plane-audit-retention-days <float>` (default: `14.0`)
- `--control-plane-audit-max-event-bytes <int>` (default: `8192`)
- `--control-plane-audit-prune-seconds <float>` (default: `900.0`)

## File Policy Shape

Example `runtime/xptool-state/control-plane-policy.json`:

```json
{
  "killSwitch": {
    "global": false,
    "activities": ["woodcutting"]
  },
  "featureFlags": {
    "break_scheduler_enabled": true,
    "activity.woodcutting.enabled": false,
    "activity.mining.enabled": true
  },
  "reason": "manual_override"
}
```

Supported fields:
- `killSwitch.global` or `killSwitchGlobal`
- `killSwitch.activities` or `disabledActivities`
- `featureFlags` (boolean map)
- `reason` (optional)

## HTTP Endpoints (Skeleton Contract)

- `POST /v1/planner/session/start`
- `POST /v1/planner/session/refresh`
- `POST /v1/planner/session/close`

Payload/response parsing is intentionally tolerant so backend can iterate.

Security behavior in HTTP mode:
- request headers include timestamp and nonce (`X-XPTool-Timestamp`, `X-XPTool-Nonce`)
- when a signing key is available, requests include HMAC signature header (`X-XPTool-Signature`)
- optional strict response replay requirements can enforce nonce/timestamp presence
- duplicate response nonces inside replay window are rejected
- HTTPS is enforced by default (except localhost HTTP for local development)
- requests include `contractVersion` and `clientBuild`
- optional strict response contract can require `decisionId` on refresh responses

## Behavior Notes

- Global kill switch triggers forced local stop commands once:
  - `STOP_ALL_RUNTIME`
  - `DROP_STOP_SESSION`
- Activity-level flags can suppress:
  - whole strategy execution (`activity.<strategy>.enabled`)
  - individual intents by activity key
- `break_scheduler_enabled=false` disables break loop execution.

## Audit Events

When audit is enabled, planner emits NDJSON audit rows for:
- session start/close and failures
- policy refresh and failures
- kill-switch trigger
- dispatch sent/failed (including dry-run)
- forced stop command sent/failed

Audit sink enforcement:
- sensitive-key redaction (`token`, `password`, `authorization`, etc.)
- payload truncation/depth bounds
- per-event size cap
- retention-based pruning

## Rollout Reference

For staged rollout and rollback operations, use:
- `docs/ROLLOUT_RUNBOOK.md`
