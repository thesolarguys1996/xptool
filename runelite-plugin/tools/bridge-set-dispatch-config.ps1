param(
    [string]$BridgeBaseUrl = "http://127.0.0.1:17888",
    [string]$BridgeAuthToken = "",
    [switch]$BridgeLiveDispatch,
    [string]$BridgeLiveDispatchCommandAllowlist = "FISH_NEAREST_SPOT_SAFE,WOODCUT_CHOP_NEAREST_TREE_SAFE,SET_FISHING_IDLE_MODE_SAFE,WALK_TO_WORLDPOINT_SAFE,CAMERA_NUDGE_SAFE,DROP_START_SESSION,DROP_STOP_SESSION,DROP_ITEM_SAFE,STOP_ALL_RUNTIME,LOGOUT_SAFE"
)

$ErrorActionPreference = "Stop"

$effectiveToken = $BridgeAuthToken
if ([string]::IsNullOrWhiteSpace($effectiveToken)) {
    $effectiveToken = $env:XPTOOL_BRIDGE_AUTH_TOKEN
}
if ([string]::IsNullOrWhiteSpace($effectiveToken)) {
    throw "Bridge auth token is required. Set -BridgeAuthToken or XPTOOL_BRIDGE_AUTH_TOKEN."
}

$headers = @{}
$headers["X-XPTool-Bridge-Token"] = $effectiveToken

$payload = @{
    bridgeRuntimeEnabled = $true
    liveDispatchEnabled = [bool]$BridgeLiveDispatch.IsPresent
    commandAllowlistCsv = "$BridgeLiveDispatchCommandAllowlist"
} | ConvertTo-Json -Compress

$url = "$($BridgeBaseUrl.TrimEnd('/'))/v1/config/dispatch"
$response = Invoke-RestMethod -Method Post -Uri $url -Headers $headers -ContentType "application/json; charset=utf-8" -Body $payload -TimeoutSec 5

Write-Host "Bridge runtime dispatch config updated:"
Write-Host "  bridgeRuntimeEnabled: $($response.bridgeRuntimeEnabled)"
Write-Host "  liveDispatchEnabled: $($response.liveDispatchEnabled)"
Write-Host "  commandAllowlistCsv: $($response.commandAllowlistCsv)"
