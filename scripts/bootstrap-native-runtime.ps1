param(
    [string]$BridgeBindAddress = "127.0.0.1",
    [int]$BridgePort = 7611,
    [string]$BridgeTokenEnv = "XPTOOL_NATIVE_BRIDGE_TOKEN",
    [string]$CommandIngestPath = "runtime/bridge/command-envelope.ndjson",
    [string]$TelemetryOutPath = "runtime/bridge/telemetry.ndjson",
    [string]$UiConfigPath = "native-ui/config/default_ui_config.cfg",
    [string]$UiOverlayOutPath = "runtime/native-ui-overlay-live.txt",
    [int]$UiTailLines = 120,
    [switch]$EnableVerifier,
    [switch]$StartBridge,
    [switch]$StartUi
)

$ErrorActionPreference = "Stop"

function Test-LoopbackBindAddress {
    param([string]$Address)

    if ([string]::IsNullOrWhiteSpace($Address)) {
        return $false
    }
    try {
        $resolved = [System.Net.Dns]::GetHostAddresses($Address)
    } catch {
        return $false
    }
    if (-not $resolved -or $resolved.Count -eq 0) {
        return $false
    }
    foreach ($ip in $resolved) {
        if (-not [System.Net.IPAddress]::IsLoopback($ip)) {
            return $false
        }
    }
    return $true
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$bridgeExe = Join-Path $repoRoot "build/native-bridge/Release/xptool_native_bridge.exe"
$uiExe = Join-Path $repoRoot "build/native-ui/Release/xptool_native_ui.exe"

$commandIngestFullPath = Join-Path $repoRoot $CommandIngestPath
$telemetryOutFullPath = Join-Path $repoRoot $TelemetryOutPath
$uiConfigFullPath = Join-Path $repoRoot $UiConfigPath
$uiOverlayOutFullPath = Join-Path $repoRoot $UiOverlayOutPath

New-Item -ItemType Directory -Path (Split-Path -Parent $commandIngestFullPath) -Force | Out-Null
New-Item -ItemType Directory -Path (Split-Path -Parent $telemetryOutFullPath) -Force | Out-Null
New-Item -ItemType Directory -Path (Split-Path -Parent $uiOverlayOutFullPath) -Force | Out-Null

if (-not (Test-Path $commandIngestFullPath)) {
    New-Item -ItemType File -Path $commandIngestFullPath -Force | Out-Null
}
if (-not (Test-Path $telemetryOutFullPath)) {
    New-Item -ItemType File -Path $telemetryOutFullPath -Force | Out-Null
}

if (-not (Test-LoopbackBindAddress -Address $BridgeBindAddress)) {
    throw "Bridge bind address must resolve to loopback only. Provided: $BridgeBindAddress"
}
if ($BridgePort -lt 1 -or $BridgePort -gt 65535) {
    throw "Bridge port must be between 1 and 65535."
}

$token = [Environment]::GetEnvironmentVariable($BridgeTokenEnv)
if (($StartBridge.IsPresent) -and [string]::IsNullOrWhiteSpace($token)) {
    throw "Missing bridge auth token. Set env var '$BridgeTokenEnv' before starting bridge."
}

Write-Output "Native runtime bootstrap complete:"
Write-Output "  Command ingest: $commandIngestFullPath"
Write-Output "  Telemetry out: $telemetryOutFullPath"
Write-Output "  UI config:     $uiConfigFullPath"
Write-Output "  UI overlay:    $uiOverlayOutFullPath"

if ($StartBridge) {
    if (-not (Test-Path $bridgeExe)) {
        throw "Native bridge executable not found: $bridgeExe"
    }
    $bridgeArgs = @(
        "--bind-address", $BridgeBindAddress,
        "--port", "$BridgePort",
        "--command-ingest-path", $commandIngestFullPath,
        "--telemetry-out-path", $telemetryOutFullPath
    )
    if ($EnableVerifier) {
        $bridgeArgs += "--enable-verifier"
    }
    $bridgeProcess = Start-Process -FilePath $bridgeExe -ArgumentList $bridgeArgs -PassThru
    Write-Output "Started native bridge pid=$($bridgeProcess.Id)"
}

if ($StartUi) {
    if (-not (Test-Path $uiExe)) {
        throw "Native UI executable not found: $uiExe"
    }
    if (-not (Test-Path $uiConfigFullPath)) {
        throw "Native UI config not found: $uiConfigFullPath"
    }
    $uiArgs = @(
        "--telemetry-path", $telemetryOutFullPath,
        "--config-path", $uiConfigFullPath,
        "--tail-lines", "$UiTailLines",
        "--write-overlay", $uiOverlayOutFullPath
    )
    $uiProcess = Start-Process -FilePath $uiExe -ArgumentList $uiArgs -PassThru
    Write-Output "Started native UI pid=$($uiProcess.Id)"
}

if (-not $StartBridge -and -not $StartUi) {
    Write-Output "No processes started. Use -StartBridge and/or -StartUi to launch native services."
}
