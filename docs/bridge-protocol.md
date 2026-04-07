# Bridge Protocol v1

This document defines the `xptool-bridge/1` wire contract for the new `1C + 2B` architecture.

## Message Flow

| Message | Direction | Purpose | Required Checks |
|---|---|---|---|
| `AuthHello` | controller -> bridge | Start authenticated session | protocol version, timestamp freshness, nonce format |
| `AuthProof` | controller -> bridge | Prove controller identity for issued challenge | challenge match, signature verify, nonce uniqueness |
| `AuthSession` | bridge -> controller | Return short-lived authenticated session | expiry > issued, key id present |
| `SignedCommandEnvelope` | controller -> bridge | Send command for execution | session active, signature verify, nonce replay guard, timestamp freshness |
| `CommandAck` | bridge -> controller | Per-command acceptance/execution result | command id match, status enum, ack timestamp |
| `BridgeHealth` | bridge -> controller | Runtime heartbeat and diagnostics | status enum, mapping version present |
| `SnapshotDto` | bridge -> controller | Snapshot stream for planning/telemetry | tick int, game state string |
| `InteractionCommand` | controller -> bridge | Low-level interaction dispatch request | type enum, args object |
| `InteractionResult` | bridge -> controller | Interaction dispatch outcome | status string, reason |
| `RuntimeStateDto` | bridge -> controller | Compact runtime flags for gating/state sync | boolean flags + tick |

## Security Baseline

- Authenticated encrypted IPC is mandatory before command execution.
- Each command must include a signature, nonce, and issued-at timestamp.
- Bridge rejects stale timestamps and nonce reuse inside active session windows.
- Secrets and key material should come from OS-backed secure storage.

## Error Semantics

- Schema/shape violation: reject with `status=rejected` and a specific reason code.
- Authentication failure: reject and close/deny session.
- Mapping miss: reject with `mapping_symbol_missing` and emit diagnostics.
- Runtime failure: return `status=failed` and include telemetry-safe details.
