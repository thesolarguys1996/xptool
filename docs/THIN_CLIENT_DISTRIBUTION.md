# Thin Client Distribution

## Purpose

Export a runtime layout that excludes local strategy modules and `runtime_core`.
This keeps shipped client artifacts focused on:
- snapshot parsing
- command transport
- control-plane/remote planner communication
- strict fail-closed execution wiring

## Export Command

```powershell
$env:PYTHONPATH='src'
python scripts/export_thin_client_layout.py
```

Optional output path:

```powershell
$env:PYTHONPATH='src'
python scripts/export_thin_client_layout.py --output build/thin-client-custom
```

## Output

Default output directory:
- `build/thin-client/`

Includes:
- `src/runelite_planner` thin modules only
- `pyproject.toml` for thin-client packaging
- `THIN_LAYOUT.md` manifest

Excludes:
- activity strategy modules (`woodcutting.py`, `fishing.py`, etc.)
- `runtime_core/`
- GUI modules

## Validation

After export:
1. confirm excluded files are absent from `build/thin-client/src/runelite_planner`
2. run help command from exported tree:
   - `python -m runelite_planner.main --help`
