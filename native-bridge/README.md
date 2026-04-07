# Native Bridge (`native-bridge`)

Last updated: 2026-04-05

## Purpose
`native-bridge` owns local IPC ingress/egress for command and telemetry traffic.

## Rules
- Loopback bind only.
- Token auth required.
- Schema-versioned envelopes only.
- No remote relay/tunneling ownership.

## Build
```powershell
cmake -S native-bridge -B build/native-bridge
cmake --build build/native-bridge
```

## Run
```powershell
$env:XPTOOL_NATIVE_BRIDGE_TOKEN="replace-with-local-token"
.\build\native-bridge\xptool_native_bridge `
  --bind-address 127.0.0.1 `
  --port 7611 `
  --command-ingest-path runtime\bridge\command-envelope.ndjson `
  --telemetry-out-path runtime\bridge\telemetry.ndjson `
  --enable-verifier
```

## Phase 1 Enforcement
- Startup rejects non-loopback bind addresses with `local_only_bind_required`.
- Startup rejects missing auth token with `missing_token`.
- Command ingest validates envelope fields before acceptance:
  - required top-level fields (`schemaVersion`, `commandId`, `commandType`, `issuedAtUtc`, `payload`),
  - `schemaVersion` must be `1.0`,
  - `commandType` must be a supported command.
- Optional verifier mode rejects:
  - stale timestamps via `replay_rejected_timestamp`,
  - duplicate command IDs via `replay_rejected_command_id`.

## Telemetry
Each emitted event is a JSON object aligned to `schemas/native/telemetry-event.v1.json` and includes:
- `eventType`,
- `reasonCode`,
- `source` (`native-bridge`),
- minimal `data` fields for diagnostics.

## Native Launcher
Preferred bootstrap script for native-default route:

```powershell
$env:XPTOOL_NATIVE_BRIDGE_TOKEN="replace-with-local-token"
.\scripts\bootstrap-native-runtime.ps1 -EnableVerifier -StartBridge
```
