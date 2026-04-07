# Logging and Telemetry Policy (Step 6)

## Scope
This policy defines:
- what is logged
- where it is stored
- retention defaults
- access scope expectations

It applies to planner/runtime logs in this repository and documents plugin-side surfaces.

## Logging Matrix

| Channel | Event Type | Path / Sink | Retention Default | Access Scope |
|---|---|---|---|---|
| Planner stdout | `[INFO]`, `[WARN]`, `[ERROR]`, `[CORE]`, `[EXEC]`, `[TELEM]` | Console process output | Process lifetime (external collector policy if redirected) | Local operator |
| Control-plane audit NDJSON | `CONTROL_PLANE_AUDIT` | `runtime/xptool-state/control-plane-audit.ndjson` | 14 days (configurable) | Local operator |
| Plugin snapshot logs | `xptool.snapshot` JSON lines | RuneLite `client.log` | RuneLite log policy | Local machine user |
| Plugin execution logs | `xptool.execution` JSON lines | RuneLite `client.log` | RuneLite log policy | Local machine user |
| Plugin telemetry file | redacted execution telemetry | `~/.runelite/logs/xptool-telemetry.ndjson` (or override) | Operator-managed | Local machine user |

## Enforcement (Current)

Control-plane audit sink enforces:
- sensitive-key redaction (`token`, `password`, `authorization`, etc.)
- nested payload sanitization with depth and collection limits
- per-event serialized size cap (`max_event_bytes`)
- retention pruning by timestamp (`capturedAtUnixMillis`)

Defaults:
- retention: 14 days
- max event bytes: 8192
- prune interval: 900 seconds

## CLI Controls (Control-Plane Audit)

- `--control-plane-audit-retention-days`
- `--control-plane-audit-max-event-bytes`
- `--control-plane-audit-prune-seconds`
- `--control-plane-disable-audit`

## Data Handling Rules

- Do not log secrets, credentials, or auth tokens.
- Prefer event codes and bounded metadata over raw payload dumps.
- Keep payloads compact and deterministic for easier incident review.
- Use pseudonymous identifiers where feasible.

## Purge Policy

Control-plane audit purge is best-effort and runs during audit writes.
- Records older than retention cutoff are removed.
- Corrupt lines are preserved unless they are clearly expired parseable rows.
- Purge interval throttles rewrite frequency to avoid excessive IO.

## Residual Gaps

- RuneLite `client.log` and plugin telemetry retention remain host-managed outside planner controls.
- Backend operational logs must adopt equivalent redaction/retention policy server-side.
