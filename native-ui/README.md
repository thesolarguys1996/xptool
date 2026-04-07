# Native UI (`native-ui`)

Last updated: 2026-04-05

## Purpose
`native-ui` replaces RuneLite overlay/config ownership over time.

## Scope
- Status overlays and runtime diagnostics.
- Local controls for activity and runtime state.
- Telemetry inspection for parity validation.

## Build
```powershell
cmake -S native-ui -B build/native-ui
cmake --build build/native-ui
```

## Run
```powershell
.\build\native-ui\Release\xptool_native_ui.exe `
  --telemetry-path runtime\bridge\telemetry.ndjson `
  --config-path native-ui\config\default_ui_config.cfg `
  --tail-lines 100 `
  --write-overlay runtime\native-ui-overlay.txt
```

## Config
Config files are `key=value` lines.

Supported keys:
- `focus_activity` (`all`, `woodcutting`, `mining`, `fishing`, `combat`, `banking`)
- `show_rejected` (`true`/`false`)
- `max_recent_events` (integer)
- `compact` (`true`/`false`)

Sample files:
- `native-ui/config/default_ui_config.cfg`
- `native-ui/config/mining_focus.cfg`

## Output
The app renders a native overlay summary from telemetry:
- total/dispatched/rejected/no-progress counters,
- top reason-code distribution,
- recent event lines with activity/command/target context.

## Native Launcher
Start UI through native bootstrap script:

```powershell
.\scripts\bootstrap-native-runtime.ps1 -StartUi
```
