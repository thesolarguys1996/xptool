# Security Hardening (Step 7)

This step adds planner-side transport hardening for control-plane HTTP mode.

## Implemented Controls

- Request HMAC signing (optional; enabled when signing key is available)
  - Header: `X-XPTool-Signature`
  - Optional key id header: `X-XPTool-Signing-Key-Id`
- Request anti-replay metadata
  - Headers: `X-XPTool-Timestamp`, `X-XPTool-Nonce`
- Response replay-window validation
  - Rejects duplicate nonces in replay window
  - Optional strict mode requires response nonce/timestamp fields
- HTTPS transport enforcement
  - non-localhost HTTP URLs are rejected by default
  - localhost HTTP remains allowed for local development
- Contract strictness option
  - optional requirement for `decisionId` on refresh responses

## Replay Window

- Config: `--control-plane-replay-window-seconds` (default `30`)
- Applied to:
  - locally generated request nonces
  - response nonce/timestamp acceptance checks

## Signing Configuration

- `--control-plane-signing-key-env` (default `XPTOOL_CONTROL_PLANE_SIGNING_KEY`)
- `--control-plane-signing-key-id` (optional identifier)

If key material is absent, request signing is skipped while nonce/timestamp headers still emit.

## Strict Response Requirements

- `--control-plane-require-response-replay-fields`
- `--control-plane-require-decision-id`

When enabled, HTTP responses must include replay metadata (header or body fields), otherwise request fails.
When `--control-plane-require-decision-id` is enabled, refresh responses must also include `decisionId`.

## HTTPS Enforcement

- default behavior enforces HTTPS for control-plane URLs
- localhost HTTP endpoints are allowed by default for development
- override with `--control-plane-allow-insecure-http` only in trusted local/test environments

## Notes

- This hardening is client-side and requires server-side verification for full effectiveness.
- Server should validate signature, nonce uniqueness, and timestamp freshness with the same replay window policy.
