param()

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$runtimeDir = Join-Path $repoRoot "runtime"
$stateDir = Join-Path $runtimeDir "xptool-state"
$bridgeDir = Join-Path $runtimeDir "bridge"
$nativeDir = Join-Path $runtimeDir "native"
$logPath = Join-Path $runtimeDir "client.log"
$commandEnvelopePath = Join-Path $bridgeDir "command-envelope.ndjson"
$telemetryPath = Join-Path $bridgeDir "telemetry.ndjson"

New-Item -ItemType Directory -Path $runtimeDir -Force | Out-Null
New-Item -ItemType Directory -Path $stateDir -Force | Out-Null
New-Item -ItemType Directory -Path $bridgeDir -Force | Out-Null
New-Item -ItemType Directory -Path $nativeDir -Force | Out-Null

if (-not (Test-Path $logPath)) {
    New-Item -ItemType File -Path $logPath -Force | Out-Null
}
if (-not (Test-Path $commandEnvelopePath)) {
    New-Item -ItemType File -Path $commandEnvelopePath -Force | Out-Null
}
if (-not (Test-Path $telemetryPath)) {
    New-Item -ItemType File -Path $telemetryPath -Force | Out-Null
}

Write-Output "Initialized runtime paths (native-default):"
Write-Output "  $logPath"
Write-Output "  $commandEnvelopePath"
Write-Output "  $telemetryPath"
