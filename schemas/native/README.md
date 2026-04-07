# Native Shared Schemas

Last updated: 2026-04-05

## Purpose
Shared, versioned contracts for native bridge command ingress and telemetry egress.

## Versioning
- Use explicit schema versions in filenames (`*.v1.json`).
- Additive changes only within a version line.
- Breaking changes require a new major schema file (`v2`).

## Initial Contracts
- `command-envelope.v1.json`
- `telemetry-event.v1.json`
