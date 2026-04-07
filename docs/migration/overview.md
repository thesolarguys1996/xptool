# Migration Overview

Last updated: 2026-04-07

## Goal
Migrate runtime ownership from Java/RuneLite to native components while keeping compatibility shims only where required during transition.

## Canonical Migration Docs
- `docs/migration/current-phase.md`: authoritative phase status and active target.
- `docs/migration/checklists.md`: operator and engineering checklists.
- `docs/migration/archive/`: historical per-phase plan artifacts.

## Native Ownership Model
- `native-core`: runtime policy, activity coordination, motor ownership.
- `native-bridge`: local ingest/dispatch boundary, envelope validation, telemetry.
- `native-ui`: native overlay/status UX.
- `schemas/native`: shared command + telemetry contracts.

## Verification Entry Point
Use one migration verifier command:

```powershell
python scripts/verify_migration.py --phase 119
python scripts/verify_migration.py --bundle 215-220 --require-complete
python scripts/verify_migration.py --bundle 221-226 --require-complete
python scripts/verify_migration.py --bundle 227-232 --require-complete
python scripts/verify_migration.py --check runtime_bundle_factory
python scripts/verify_migration.py --list
```

## Compatibility Notes
- Existing legacy phase docs are archived under `docs/migration/archive/`.
- Existing legacy phase verifier scripts are archived under `scripts/migration/archive/phase_verifiers/`.
- CI gates continue to run native cutover/soak guards via dedicated scripts.
