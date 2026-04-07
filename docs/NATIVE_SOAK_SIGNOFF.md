# Native Soak Signoff

Last updated: 2026-04-05

## Goal
Run repeated native cutover verification and enforce a report-based signoff gate for native-only operations.

## Commands
Run soak iterations:

```powershell
python scripts/run_native_soak.py --iterations 6 --pause-ms 250
```

Validate soak report:

```powershell
python scripts/verify_native_soak_report.py --min-iterations 6 --max-failures 0 --max-age-hours 48
```

## Artifacts
- `runtime/native-soak/soak-report.json`
- `runtime/native-soak/iterations/iteration-###-cutover-report.json`
- `runtime/native-cutover/phase7-cutover-report.json`

## Required Checks
Each soak iteration is backed by `verify_native_cutover.py` and must keep these checks green:
- `required_files_present`
- `bridge_reason_coverage`
- `woodcutting_parity`
- `activity_parity`
- `state_acquisition_smoke`
- `native_ui_overlay`
- `java_runtime_ownership_guard`
- `java_shim_retirement_guard`
- `native_only_hardening_guard`
- `phase7_status_complete`
- `phase8b_status_complete`
- `phase9_status_present`
- `phase10_status_present`
- `tasks_java_runtime_removal_checked`
- `tasks_java_plugin_shim_removal_checked`
- `tasks_phase10_scope_checked`
- `tasks_phase10_ops_audit_checked`

## Incident Triage
If any required check fails, follow:
- `docs/NATIVE_INCIDENT_TRIAGE_RUNBOOK.md`

## CI Gate
`.github/workflows/tasks-priority-gate.yml` includes `verify-native-soak-signoff` on `windows-latest`:
- builds `native-bridge`, `native-core`, `native-ui`,
- runs soak iterations,
- validates soak report,
- uploads soak artifacts.
