# IP Protection Scope and Guardrails (Step 1)

## Purpose
Protect proprietary logic and reduce accidental data exposure while keeping architecture maintainable and policy-compliant.

This scope is for:
- IP protection
- operational safety
- privacy hygiene

This scope is not for:
- bypassing platform/game policy enforcement
- evading anti-cheat or compliance systems
- hiding prohibited behavior

## Goals
- Keep high-value decision logic out of shipped client code where practical.
- Minimize sensitive data in logs and telemetry.
- Keep runtime behavior deterministic enough to debug, but least-privilege by default.
- Preserve existing architecture direction: `CommandExecutor` remains orchestration/wiring focused.

## Non-Goals
- Building an "undetectable" client/plugin.
- Preventing all reverse engineering of shipped artifacts.
- Introducing new plugin-surface settings unless absolutely required.

## Threat Model (Practical)
- Assume shipped plugin binaries can be inspected.
- Assume runtime behavior and outcomes can be observed externally.
- Assume local and backend logs are discoverable by operators with access.
- Do not assume private source repos are directly visible to third parties by default.

## Data Classification
- Public-safe:
  - command types
  - non-sensitive execution outcome codes
  - generic runtime health counters
- Sensitive:
  - proprietary decision heuristics/policies
  - account-linked identifiers
  - auth material (tokens/keys/passwords)
  - high-fidelity raw snapshots not required for operation

## Guardrails
- Keep one motor/gating authority per concern (no duplicated gate logic).
- Keep behavior state with the owning runtime/service, not scattered in orchestrators.
- Prefer Host adapters for system boundaries.
- Keep DTO/spec models top-level and explicit.
- Keep `CommandExecutor` focused on:
  - dependency wiring
  - routing commands/ticks/events
  - delegating to domain services/runtimes
  - emitting telemetry/results

## Privacy and Logging Baseline
- Redact known sensitive fields before writing execution logs.
- Minimize snapshot/telemetry payloads to only required fields.
- Separate debug and production logging policy.
- Avoid storing secrets in logs under all modes.
- Define retention windows and operator access scope for local/backend logs.

## Architectural Direction for Thin Client
- Client owns:
  - RuneLite API reads
  - UI/menu/widget interaction
  - motor execution and safety constraints
- Backend owns:
  - high-value planning/decision policy
  - intent selection/scheduling policy
  - centralized feature flags and kill switches

## Success Criteria
- Sensitive planning logic reduced in client footprint.
- No secrets/tokens in logs under expected paths.
- Backend outage behavior is safe and observable.
- `CommandExecutor` complexity trends down over time as logic moves to focused components.

## Step Exit Criteria
Step 1 is complete when:
- this scope is accepted as the operating baseline
- all later steps reference these guardrails
- any change request that conflicts with this document is explicitly called out
