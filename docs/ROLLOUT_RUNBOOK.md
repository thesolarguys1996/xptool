# Rollout Runbook (Step 9)

## Purpose
Operational guide for introducing control-plane backed planning safely:
- shadow mode first
- staged cohort enablement
- fast rollback with verified kill-switch paths

## Preconditions

- Step 1-8 docs are complete.
- Control-plane client flags are available in runner CLI.
- Strict remote policy lock is active in main entrypoint.
- Test suite baseline is green:
  - `PYTHONPATH=src python -m unittest tests.test_control_plane tests.test_command_policy tests.test_motion_engine`

## Phase 0: Baseline (No Control Plane)

Run planner in normal mode and capture baseline telemetry:

```powershell
$env:PYTHONPATH='src'
python -m runelite_planner.main --activity woodcutting --follow
```

Collect:
- dispatch success/deferred/failed rates
- average dispatch-to-effect ticks
- retry reasons
- KPI summary snapshot:
  - `python -m runelite_planner.kpi_dashboard --audit-path runtime/xptool-state/control-plane-audit.ndjson`

## Phase 1: Shadow Mode

Goal:
- exercise control-plane transport/security/audit path
- do not rely on control-plane for decisions yet

Recommended launch:

```powershell
$env:PYTHONPATH='src'
$env:XPTOOL_CONTROL_PLANE_SIGNING_KEY='replace-with-real-key'
python -m runelite_planner.main `
  --activity woodcutting `
  --follow `
  --dry-run `
  --control-plane-url https://control.example `
  --control-plane-contract-version 1.0 `
  --control-plane-client-build xptool-shadow `
  --control-plane-token-env XPTOOL_CONTROL_PLANE_TOKEN `
  --control-plane-signing-key-env XPTOOL_CONTROL_PLANE_SIGNING_KEY `
  --control-plane-replay-window-seconds 30 `
  --control-plane-require-response-replay-fields `
  --control-plane-require-decision-id `
  --control-plane-audit-path runtime/xptool-state/control-plane-audit.ndjson
```

Pass criteria:
- no replay/signature transport errors under nominal conditions
- no control-plane crashes
- audit stream contains expected session/policy events

## Phase 2: Cohort A (Small Enablement)

Goal:
- enable a small controlled cohort with real dispatch

Approach:
- keep strict replay settings on
- keep audit enabled
- limit to one activity first

Suggested controls:
- control-plane policy file or service feature flags:
  - `activity.woodcutting.enabled=true`
  - all other activity flags false initially

Pass criteria:
- stable dispatch behavior (no material regression vs baseline)
- fallback path remains healthy during temporary control-plane failures
- unsupported command rejection count remains zero in normal flow

Remote planner enablement example:

```powershell
$env:PYTHONPATH='src'
$env:XPTOOL_REMOTE_PLANNER_TOKEN='replace-with-real-token'
python -m runelite_planner.main `
  --activity woodcutting `
  --follow `
  --remote-planner-url https://planner.example `
  --remote-planner-token-env XPTOOL_REMOTE_PLANNER_TOKEN `
  --remote-planner-contract-version 1.0 `
  --remote-planner-client-build xptool-cohort-a
```

## Phase 3: Cohort B (Medium Enablement)

Goal:
- expand activities and session count gradually

Incremental rollout:
1. Enable one additional activity.
2. Observe for one full break-cycle window.
3. Expand only if:
   - error rates remain within budget
   - no sustained retry storm
   - no persistent control-plane refresh failure loop

## Phase 4: Full Enablement

Goal:
- default all target activities to enabled
- retain immediate rollback controls

Must keep:
- global kill switch enabled and tested
- audit retention and payload cap settings enforced
- replay/signature settings unchanged from validated cohorts

## Rollback Strategy

### Immediate (seconds)
1. Set global kill switch in control-plane policy.
2. Confirm runner logs show:
   - `global_kill_switch_active`
   - forced stop commands emitted

### Fast (minutes)
1. Disable control-plane mode by removing URL/policy flags.
2. Restart runner in baseline local mode.

### Fallback Verification
- confirm planner still executes local strategy path when control-plane unavailable
- confirm no unsupported command type reaches command bus

## Rollback Drill Checklist

Perform at least once before full enablement:
1. Induce temporary control-plane outage.
2. Verify fail-open local strategy continuity.
3. Toggle global kill switch and verify forced stop commands.
4. Restore service and verify clean session re-establishment.
5. Confirm audit rows capture the full incident timeline.

## Operational Commands

### File-mode kill switch (local emergency)
Create/update `runtime/xptool-state/control-plane-policy.json`:

```json
{
  "killSwitchGlobal": true,
  "reason": "emergency_rollback"
}
```

### File-mode partial disable

```json
{
  "killSwitchGlobal": false,
  "disabledActivities": ["combat", "fishing"],
  "featureFlags": {
    "activity.woodcutting.enabled": true,
    "activity.mining.enabled": true
  }
}
```

## Exit Criteria (Step 9)

Step 9 is complete when:
- shadow mode runbook is executed and documented
- staged cohort rollout has explicit gates and criteria
- rollback drill is performed and recorded
- operators can execute emergency kill switch within one minute
