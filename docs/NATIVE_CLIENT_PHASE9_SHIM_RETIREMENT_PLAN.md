# Native Client Phase 9 Shim Retirement Plan

Last updated: 2026-04-05

## Goal
Retire remaining RuneLite Java shim ownership by removing `XPToolPlugin` and `XPToolConfig` only after explicit native cutover gates pass.

## Current Shim Inventory
No remaining Java plugin shim inventory after Phase 9.4 removal.

## Cutover Gates (Must Pass Before Removal)
1. `G1`: Java shadow runtime removal guard passes.
   - `python scripts/verify_java_runtime_ownership_blocked.py`
2. `G2`: Java shim retirement guard passes (stub-only + no config-surface expansion).
   - `python scripts/verify_java_shim_retirement_gates.py`
3. `G3`: Native cutover verification passes.
   - `python scripts/verify_native_cutover.py`
4. `G4`: Native soak verification passes with zero failures.
   - `python scripts/run_native_soak.py --iterations 6 --pause-ms 0`
   - `python scripts/verify_native_soak_report.py --min-iterations 6 --max-failures 0 --max-age-hours 2`
5. `G5`: No runtime ownership regression into Java shim surfaces.
   - `python scripts/verify_java_runtime_ownership_blocked.py`
   - `python scripts/verify_java_shim_retirement_gates.py`
6. `G6`: Phase/task tracking evidence updated before deletion.
   - `docs/NATIVE_CLIENT_PHASE_STATUS.md`
   - `TASKS.md`

## Execution Slices
1. `9.1` Checklist + gate definition and guard automation.
2. `9.2` Verify `XPToolConfig` has no runtime consumers and migrate any residual policy/config usage to native contracts.
3. `9.3` Remove `XPToolConfig` and `provideConfig(...)` shim wiring.
4. `9.4` Remove `XPToolPlugin` class and any remaining compatibility references.
5. `9.5` Run full verification pack (`G1-G6`) and mark `PHASE 9 COMPLETE`.

## Phase 9 Slice Status
- `9.1` complete.
- `9.2` complete.
- `9.3` complete.
- `9.4` complete.
- `9.5` complete.

## Exit Criteria
- `XPToolPlugin` and `XPToolConfig` are removed.
- No RuneLite plugin-surface ownership remains for runtime behavior.
- Native bridge/core/ui remain authoritative with parity/soak checks passing.
