# IP Protection Roadmap (Steps 5-10)

## Status
- Step 1 complete: `docs/IP_PROTECTION_SCOPE.md`
- Step 2 complete: `docs/IP_PROTECTION_SENSITIVITY_INVENTORY.md`
- Step 3 complete: `docs/IP_PROTECTION_COMPONENT_MAP.md`
- Step 4 complete: `docs/IP_PROTECTION_THIN_CLIENT_CONTRACT.md`
- Step 5 complete: `docs/CONTROL_PLANE.md`
- Step 6 complete: `docs/LOGGING_TELEMETRY_POLICY.md`
- Step 7 complete: `docs/SECURITY_HARDENING.md`
- Step 8 complete: `docs/TEST_COMPLIANCE_PLAN.md`
- Step 9 complete: `docs/ROLLOUT_RUNBOOK.md`
- Step 10 complete: `docs/KPI_DASHBOARD_SPEC.md`
- Remote planner mode added (post-step implementation hardening): `docs/REMOTE_PLANNER_MODE.md`
- Thin-client export layout added: `docs/THIN_CLIENT_DISTRIBUTION.md`

## Step 5: Backend Control Plane
### Deliverables
- Auth/session service for planner requests.
- Feature-flag service for activity/policy toggles.
- Kill switch for global and per-activity execution.
- Audit log stream for decisions and command outputs.

### Minimum Requirements
- short-lived access tokens
- server-side rate limit per session and per source
- decision trace id (`decisionId`) in every response

## Step 6: Logging and Telemetry Policy
### Deliverables
- Logging matrix by event type:
  - fields logged
  - storage destination
  - retention period
  - access scope
- Separate policy for:
  - local debug logs
  - local production logs
  - backend operational logs

### Minimum Requirements
- no secrets/tokens in any log sink
- bounded telemetry payload size
- explicit retention defaults and purge policy

## Step 7: Security Hardening
### Deliverables
- request signing + replay window checks
- TLS enforcement and certificate pinning policy (if used)
- plugin artifact signing process
- threat review checklist before release

### Minimum Requirements
- reject stale/replayed decision requests
- rotate credentials/tokens
- environment-based secret injection (no hardcoded secrets)

## Step 8: Test and Compliance Plan
### Deliverables
- contract tests for request/response schema
- integration tests for fallback modes
- chaos tests for backend timeout/unavailable scenarios
- policy conformance checks for supported command types only

### Minimum Requirements
- pass criteria for latency, error budget, and fallback correctness
- regression suite for command routing and gating ownership

## Step 9: Gradual Rollout
### Deliverables
- shadow mode runbook
- staged enablement by cohort
- rollback runbook
- divergence dashboard (backend vs local planner decisions)

### Minimum Requirements
- instant kill-switch path verified
- rollback verified in pre-prod and first prod cohort

## Step 10: Success Metrics
### Deliverables
- KPI definitions and dashboard wiring

### Core KPIs
- client-sensitive-logic footprint trend (downward)
- planner decision latency p95/p99
- fallback rate and cause distribution
- command validation reject rate
- security event count (replay/signature failures)
- operational MTTR for planner incidents

## Suggested Execution Order
1. Implement Step 5 auth/flags/kill switch skeleton.
2. Implement Step 6 logging matrix and redaction enforcement.
3. Implement Step 7 request signing + replay prevention.
4. Add Step 8 tests around timeout/fallback and command validation.
5. Run Step 9 shadow rollout with Step 10 KPIs enabled.
