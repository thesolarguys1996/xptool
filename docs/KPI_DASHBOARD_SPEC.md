# KPI Dashboard Spec (Step 10)

## Purpose
Define and operationalize KPI reporting for rollout health and long-term governance.

## Data Source
- Primary: `CONTROL_PLANE_AUDIT` NDJSON events
  - default path: `runtime/xptool-state/control-plane-audit.ndjson`
- Producer:
  - `RuntimeRunner` control-plane audit events
  - decision latency samples (`decision_latency_sample`)

## Dashboard Generator

Use:

```powershell
$env:PYTHONPATH='src'
python -m runelite_planner.kpi_dashboard --audit-path runtime/xptool-state/control-plane-audit.ndjson
```

Optional output file:

```powershell
$env:PYTHONPATH='src'
python -m runelite_planner.kpi_dashboard `
  --audit-path runtime/xptool-state/control-plane-audit.ndjson `
  --json-out runtime/xptool-state/kpi-summary.json
```

Script entry point (installed):
- `xptool-kpi`

## KPI Definitions

### 1) Planner Decision Latency
- Source events: `decision_latency_sample`
- Metric:
  - `p50`, `p95`, `p99`
  - `samples`

### 2) Fallback Rate and Causes
- Source events:
  - `session_start_failed`
  - `policy_refresh_failed`
- Metric:
  - `fallback.count`
  - `fallback.rate`
  - `fallback.reasons` distribution

### 3) Command Validation Reject Rate
- Source events:
  - `dispatch_sent`
  - `dispatch_dry_run`
  - `dispatch_rejected_unsupported`
- Metric:
  - `commandValidation.totalEvaluated`
  - `commandValidation.rejectedUnsupported`
  - `commandValidation.rejectRate`

### 4) Security Event Count
- Source events:
  - failures containing replay/signature/auth indicators
- Metric:
  - `security.count`
  - `security.reasons`

### 5) Operational MTTR
- Incident model:
  - incident opens on `policy_refresh_failed`
  - incident closes on next `policy_refreshed`
- Metric:
  - `operations.incidentsClosed`
  - `operations.incidentsOpen`
  - `operations.mttrMsAvg`
  - `operations.mttrMsP95`

### 6) Client Footprint Proxy
- Source:
  - LOC of:
    - `CommandExecutor.java`
    - `runtime_core/core.py`
- Metric:
  - `clientFootprintProxy.commandExecutorLoc`
  - `clientFootprintProxy.plannerCoreLoc`

This is a proxy for trend tracking of client-side complexity.

## Suggested Alert Thresholds (Initial)

- decision latency p95 > 150ms for sustained windows
- fallback rate > 5%
- command validation reject rate > 0.5%
- security event count > 0 in stable periods
- incidents open > 0 for > 10 minutes

Tune thresholds after observing real traffic distributions.

## Step 10 Exit Criteria

Step 10 is complete when:
- KPI summary can be generated from audit data
- KPI definitions are documented with sources/formulas
- baseline thresholds are documented for operations
