# Native Incident Triage Runbook

Last updated: 2026-04-05

## Goal
Provide a repeatable triage flow for native runtime incidents across bridge/core/ui and preserve enough artifacts for deterministic replay.

## Trigger Conditions
Start this runbook when any of the following occur:
- `scripts/verify_native_cutover.py` exits non-zero.
- `scripts/run_native_soak.py` reports failed iterations.
- `scripts/verify_native_soak_report.py` fails.
- CI `verify-native-soak-signoff` fails.

## Triage Flow
1. Reproduce the failure once with full output:
   - `python scripts/verify_native_cutover.py`
2. Run soak to determine stability:
   - `python scripts/run_native_soak.py --iterations 6 --pause-ms 0`
3. Validate report gating:
   - `python scripts/verify_native_soak_report.py --min-iterations 6 --max-failures 0 --max-age-hours 2`
4. Categorize failing check(s):
   - bridge/security ingress (`bridge_reason_coverage`)
   - parity/runtime behavior (`woodcutting_parity`, `activity_parity`)
   - state ingestion (`state_acquisition_smoke`)
   - UI output (`native_ui_overlay`)
   - ownership/hardening guards (`java_runtime_ownership_guard`, `java_shim_retirement_guard`, `native_only_hardening_guard`)
   - phase/task governance checks (`phase*_status_*`, `tasks_*`)

## Required Artifact Bundle
Capture and attach:
- `runtime/native-cutover/phase7-cutover-report.json`
- `runtime/native-cutover/bridge-telemetry-phase7.ndjson`
- `runtime/native-cutover/native-ui-overlay-phase7.txt`
- `runtime/native-soak/soak-report.json`
- `runtime/native-soak/iterations/iteration-###-cutover-report.json`

## Initial Root-Cause Guide
1. `bridge_reason_coverage` fails:
   - Verify `XPTOOL_NATIVE_BRIDGE_TOKEN` and loopback bind usage.
   - Confirm replay/invalid-schema/unsupported command rejection events are still emitted.
2. Parity checks fail:
   - Inspect metric lines in cutover report command outputs.
   - Compare parity baseline files under `native-core/parity/`.
3. `state_acquisition_smoke` fails:
   - Check schema version alias handling and deferred recovery behavior.
4. `native_ui_overlay` fails:
   - Verify telemetry input path and overlay output write path permissions.
5. Guard checks fail:
   - Validate status/task/docs consistency and native-only policy invariants first.

## Escalation
Escalate as high-priority when:
- more than `0` soak failures in `6` iterations,
- repeated failures across two consecutive triage runs,
- any failure suggests remote exposure or auth bypass risk.

## Recovery Validation
A fix is accepted only when all pass:
- `python scripts/verify_native_cutover.py`
- `python scripts/run_native_soak.py --iterations 6 --pause-ms 0`
- `python scripts/verify_native_soak_report.py --min-iterations 6 --max-failures 0 --max-age-hours 2`
