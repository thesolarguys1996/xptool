# Test and Compliance Plan (Step 8)

## Scope
This plan covers planner/control-plane contract safety, failure fallback behavior, and command policy conformance.

## Implemented Coverage

### 1) Contract Tests
- Control-plane HTTP request payload contract:
  - start-session request requires `runnerId`, `strategyName`, `writerPath`, `dryRun`
  - refresh request requires `sessionId`, `strategyName`, `tick`
  - all requests include `contractVersion` and `clientBuild`
- Policy response default contract:
  - empty payload resolves to safe defaults (no global kill switch, no disabled activities)
  - optional strict mode rejects refresh responses missing `decisionId`

### 2) Fallback / Integration Tests
- Control-plane session start failure is fail-open for local strategy execution.
- Control-plane refresh failure is fail-open for local strategy execution.

### 3) Chaos Tests
- HTTP timeout/unavailable path raises deterministic runtime error and does not silently pass.
- Duplicate response nonce inside replay window is rejected.
- insecure non-localhost HTTP control-plane URL is rejected when HTTPS enforcement is enabled.
- expired session access token falls back to external token provider.

### 4) Policy Conformance
- Planner-side allowlist gate rejects unsupported command types before writing command bus entries.
- Rejected dispatches are routed through `on_dispatch_failed(...)` with explicit reason.
- dry-run stop path does not crash when forced-stop telemetry emits.

## Test Files
- `tests/test_control_plane.py`
- `tests/test_command_policy.py`

## Pass Criteria
- All control-plane tests pass.
- Command policy tests pass.
- Existing motion-engine baseline tests continue to pass.
- No unsupported command type reaches command-bus writer in conformance tests.

## Operational Criteria
- On control-plane partial outage, runner continues local planning/execution path.
- On control-plane transport anomalies, errors are explicit and observable.
- Command-type policy remains aligned with plugin-side supported command set.
