# Native Client Phase 10 Hardening Plan

Last updated: 2026-04-05

## Goal
Harden native-only operations after Phase 9 completion by removing residual RuneLite-era operational assumptions from docs/scripts and tightening native runtime verification.

## Scope
- Operational hardening only.
- No reintroduction of Java plugin/runtime ownership.
- Keep local-only bridge policy and existing parity guardrails intact.

## Workstreams
1. Native-only operations baseline
   - Audit scripts/docs for stale Java/RuneLite runtime ownership assumptions.
   - Normalize runbooks to native bridge/core/ui as operational authority.
2. Verification hardening
   - Expand cutover/soak checks for native-only invariants.
   - Keep CI gates aligned with post-Phase-9 ownership model.
3. Reliability hardening
   - Define repeatable failure triage flow for bridge/core/ui.
   - Standardize artifact capture requirements for incident replay.
4. Security posture hardening
   - Reconfirm loopback-only + token-required behavior in all startup paths.
   - Ensure no remote exposure regression paths in tooling docs.

## Execution Slices
1. `10.1` Define Phase 10 scope, ownership, and gating checklist.
2. `10.2` Audit and patch docs/scripts for native-only operational consistency.
3. `10.3` Tighten verification gates for native-only invariants.
4. `10.4` Add reliability/incident triage runbook updates.
5. `10.5` Run full verification pack and mark `PHASE 10 COMPLETE`.

## Phase 10 Slice Status
- `10.1` complete.
- `10.2` complete.
- `10.3` complete.
- `10.4` complete.
- `10.5` complete.

## Phase 10.2 Outputs
- Native-only operational assumption cleanup:
  - `scripts/bootstrap-runtime.ps1` (removed `-LegacyJavaShim`)
  - `docs/NATIVE_CLIENT_CUTOVER_RUNBOOK.md` (removed legacy Java shim bootstrap path)
- Remaining Java surface inventory with `port` vs `delete` baseline:
  - `docs/NATIVE_JAVA_SURFACE_INVENTORY.md`
- New hardening guard and CI wiring:
  - `scripts/verify_native_only_operations_hardening.py`
  - `.github/workflows/tasks-priority-gate.yml`

## Phase 10.3 Outputs
- Tightened cutover checks with Phase 10/task invariants:
  - `scripts/verify_native_cutover.py`
- Tightened soak verification defaults and required-check presence validation:
  - `scripts/verify_native_soak_report.py`
- Tightened native-only hardening guard with Java inventory drift validation:
  - `scripts/verify_native_only_operations_hardening.py`

## Phase 10.4 Outputs
- Added reliability/incident triage runbook with repeatable failure flow and required artifact bundle:
  - `docs/NATIVE_INCIDENT_TRIAGE_RUNBOOK.md`
- Updated soak/cutover references to incident triage procedures:
  - `docs/NATIVE_SOAK_SIGNOFF.md`
  - `docs/NATIVE_CLIENT_CUTOVER_RUNBOOK.md`
- Tightened hardening/cutover/soak gates with Phase 10.4 requirements:
  - `scripts/verify_native_only_operations_hardening.py`
  - `scripts/verify_native_cutover.py`
  - `scripts/verify_native_soak_report.py`

## Phase 10.5 Outputs
- Completed full signoff gate pack:
  - `.\gradlew.bat compileJava` (`runelite-plugin/`)
  - `python scripts/verify_java_runtime_ownership_blocked.py`
  - `python scripts/verify_java_shim_retirement_gates.py`
  - `python scripts/verify_native_only_operations_hardening.py`
  - `python scripts/verify_native_cutover.py`
  - `python scripts/run_native_soak.py --iterations 6 --pause-ms 0`
  - `python scripts/verify_native_soak_report.py --min-iterations 6 --max-failures 0 --max-age-hours 2`

## Exit Criteria
- All operational docs/scripts reflect native-only runtime ownership.
- Verification/CI gates cover native-only invariants with no Java shim assumptions.
- Full cutover + soak gate pack passes after hardening updates.
