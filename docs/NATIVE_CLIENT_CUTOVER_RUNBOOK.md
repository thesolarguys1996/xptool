# Native Client Cutover Runbook (Phase 7)

Last updated: 2026-04-05

## Goal
Cut runtime defaults to native components and freeze Java/RuneLite code as transitional compatibility only.

## Preconditions
- Phase 0 through Phase 6 are complete in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
- Native binaries are built:
  - `build/native-bridge/Release/xptool_native_bridge.exe`
  - `build/native-core/Release/xptool_native_core_woodcutting_parity.exe`
  - `build/native-core/Release/xptool_native_core_activity_parity.exe`
  - `build/native-core/Release/xptool_native_core_state_acquisition_smoke.exe`
  - `build/native-ui/Release/xptool_native_ui.exe`
- Bridge auth token is available in `XPTOOL_NATIVE_BRIDGE_TOKEN`.

## Step 1: Bootstrap Native Paths
Initialize runtime paths with native-only defaults:

```powershell
.\scripts\bootstrap-runtime.ps1
```

## Step 2: Start Native Bridge/UI
Use the native launcher script:

```powershell
$env:XPTOOL_NATIVE_BRIDGE_TOKEN = "replace-with-local-token"
.\scripts\bootstrap-native-runtime.ps1 -EnableVerifier -StartBridge -StartUi
```

Notes:
- bind address is loopback-only by default (`127.0.0.1`).
- auth token is required to start bridge.

## Step 3: Execute Cutover Verification
Run full Phase 7 verification:

```powershell
python scripts/verify_native_cutover.py
```

Expected:
- script exits `0`,
- report written to `runtime/native-cutover/phase7-cutover-report.json`,
- report shows `passed: true`.

## Step 4: Java Shim Freeze Rules
- During migration, Java plugin/runtime files are compatibility shims only.
- Any temporary Java shim behavior must include:
  - `NATIVE_MIGRATION_TODO`,
  - target native owner component.
- New runtime ownership must be implemented in native components first.
- Current state after Phase 9:
  - `XPToolPlugin` / `XPToolConfig` shim ownership is removed.
- Phase 9 shim retirement planning/gates live in:
  - `docs/NATIVE_CLIENT_PHASE9_SHIM_RETIREMENT_PLAN.md`
  - `scripts/verify_java_shim_retirement_gates.py`

## Step 5: Soak + Decommission Window
- Keep Java shim code frozen during soak.
- Track parity and rejection telemetry daily from native bridge/core/ui outputs.
- After stability window signoff, remove remaining Java runtime ownership.

Java shadow runtime policy:
- Java shadow runtime path is removed in Phase 8B.
- No JVM property or launcher switch exists to re-enable legacy shadow execution.
- Guard script `scripts/verify_java_runtime_ownership_blocked.py` must pass before signoff.

## Step 6: Automated Soak Signoff
Run repeated cutover checks:

```powershell
python scripts/run_native_soak.py --iterations 6 --pause-ms 250
```

Validate soak report gates:

```powershell
python scripts/verify_native_soak_report.py --min-iterations 6 --max-failures 0 --max-age-hours 48
```

Reference:
- `docs/NATIVE_SOAK_SIGNOFF.md`
- `docs/NATIVE_INCIDENT_TRIAGE_RUNBOOK.md`

## Exit Criteria
Phase 7 is complete when:
- native bootstrap and launcher scripts are in place,
- cutover verification script passes and report is recorded,
- native soak report gate passes,
- phase status is marked `PHASE 7 COMPLETE`,
- native migration checklist item is checked in `TASKS.md`.
