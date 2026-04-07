# Migration Checklists

Last updated: 2026-04-07

## Engineering Checklist
- [ ] Keep native cutover verification passing.
- [ ] Keep native soak verification passing.
- [ ] Record phase status updates in `docs/NATIVE_CLIENT_PHASE_STATUS.md`.
- [ ] Record high-level milestone updates in `docs/migration/current-phase.md`.
- [ ] Archive per-phase planning artifacts under `docs/migration/archive/`.

## Verification Checklist
- [ ] Run `python scripts/verify_migration.py --phase <N>` for the phase being advanced.
- [ ] Run targeted checks with `python scripts/verify_migration.py --check <name>`.
- [ ] Run native cutover guard: `python scripts/verify_native_cutover.py`.
- [ ] Run native soak guard:
  - `python scripts/run_native_soak.py --iterations 3 --pause-ms 0`
  - `python scripts/verify_native_soak_report.py --min-iterations 3 --max-failures 0 --max-age-hours 2`

## Operator Checklist
- [ ] Confirm local branch is synced to `origin/main`.
- [ ] Confirm latest GitHub Actions run is green.
- [ ] Confirm no unintended Java-runtime ownership regressions.

## Notes
- Legacy detailed checklists in `TASKS.md` remain as historical record.
- This file is the concise checklist used for day-to-day migration operation.
