# Native Core (`native-core`)

Last updated: 2026-04-05

## Purpose
`native-core` owns native runtime behavior policy and execution coordination.

## Ownership
- `runtime_core`: lifecycle and orchestration.
- `activities`: activity runtime services (woodcutting/mining/fishing/combat).
- `motor`: movement/interaction execution authority.
- `state`: normalized state snapshots consumed by activity and motor services.
- `telemetry`: runtime reason codes and parity signals.

## Build
```powershell
cmake -S native-core -B build/native-core
cmake --build build/native-core
```

## Smoke
```powershell
.\build\native-core\Release\xptool_native_core_smoke.exe
```

Expected output includes gate reason codes and dispatch results, for example:
- `not_logged_in`
- `out_of_focus`
- `no_intent`
- `cooldown_active`
- `woodcutting_dispatch`

## Woodcutting Parity
```powershell
.\build\native-core\Release\xptool_native_core_woodcutting_parity.exe `
  native-core\parity\woodcutting_baseline_v1.csv
```

Pass thresholds:
- `outcome_match_rate >= 0.95`
- `dispatch_tick_match_rate == 1.0`
- `reason_coverage == 1.0`

## Multi-Activity Parity
```powershell
.\build\native-core\Release\xptool_native_core_activity_parity.exe `
  native-core\parity\activity_baseline_v1.csv
```

Per-activity pass thresholds (`mining`, `fishing`, `combat`, `banking`):
- `outcome_match_rate >= 0.95`
- `dispatch_tick_match_rate == 1.0`
- `reason_coverage == 1.0`

## State Acquisition Smoke
```powershell
.\build\native-core\Release\xptool_native_core_state_acquisition_smoke.exe
```

Expected acquisition evidence includes:
- `state_acquired`
- `state_acquired_legacy_alias`
- `deferred_*` recovery reasons
- terminal hard rejection after deferred budget exhaustion (for example `schema_version_unsupported`)

## Notes
- This module is native-first and independent of RuneLite classes.
- During migration, Java integration remains compatibility-only glue.

## Phase 7 Cutover Gate
```powershell
python scripts/verify_native_cutover.py
```

## Soak Signoff Gate
```powershell
python scripts/run_native_soak.py --iterations 6 --pause-ms 250
python scripts/verify_native_soak_report.py --min-iterations 6 --max-failures 0 --max-age-hours 48
```
