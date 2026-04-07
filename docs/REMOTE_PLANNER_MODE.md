# Remote Planner Mode

## Purpose
Move high-value decisioning out of local planner strategy code and into a backend service while preserving local command safety checks.

## Strict Policy Lock

The project entrypoint now enforces strict remote mode by default:
- remote planner URL is required
- local fallback is disabled
- startup precheck is enabled
- decisionId requirement is enabled
- HTTPS enforcement is enabled

If these requirements are not met, runner startup exits with:
- `strict_remote_policy_violation ...`

## CLI Flags

- `--remote-planner-url <base-url>` (required)
- `--remote-planner-token-env <ENV_VAR>` (default: `XPTOOL_REMOTE_PLANNER_TOKEN`)
- `--remote-planner-timeout-seconds <float>` (default: `0.5`)
- `--remote-planner-max-commands <int>` (default: `3`)
- `--remote-planner-contract-version <value>` (default: `1.0`)
- `--remote-planner-client-build <value>` (default: `xptool-local`)

## Decision Contract

Endpoint:
- `POST /v1/planner/decision`

Request includes:
- `requestId`
- `capturedAtUnixMillis`
- `sessionId`
- `strategyActivity`
- `strategyName`
- `tick`
- `contractVersion`
- `clientBuild`
- `snapshot` (compact planner snapshot DTO)
- `capabilities.supportedCommandTypes`

Response shape:
- `status` (`ok`, `no_action`, `reject`, `error`)
- `decisionId`
- `reason`
- `commands[]` where each command has:
  - `commandType`
  - `payload`
  - `reason`
  - `source` (optional)

## Runtime Behavior

- Main `xptool` entrypoint runs a remote-only strategy stub and does not execute local activity strategy logic.
- If remote planner is enabled and returns `status=ok` with commands:
  - commands are validated against local supported command policy
  - accepted commands are written to command bus
- Unsupported command types are rejected locally.
- If remote planner fails:
  - runner blocks local strategy for that tick (fail-closed)
- Startup precheck:
  - runner sends a probe decision request before log processing
  - if probe fails, runner exits immediately

## Security Defaults

- HTTPS enforced by default (localhost HTTP allowed by default for local development).
- Bearer token supported via env var.
- Optional strict response requirement for `decisionId`.
