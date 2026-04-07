param(
    [string]$Path = "$env:USERPROFILE\.runelite\logs\xptool-telemetry.ndjson",
    [string[]]$StopReasons = @("runtime_stop_all_dispatched"),
    [int]$NearDropPx = 6,
    [int]$NearWoodcutPx = 4,
    [bool]$ReportLatestOnStart = $true,
    [bool]$FallbackToLatestSessionOnStart = $true,
    [switch]$Once
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Parse-JsonLine {
    param([string]$Line)
    if ([string]::IsNullOrWhiteSpace($Line)) {
        return $null
    }
    try {
        try {
            return $Line | ConvertFrom-Json -Depth 12
        } catch {
            return $Line | ConvertFrom-Json
        }
    } catch {
        return $null
    }
}

function Read-SessionEvents {
    param(
        [string]$TelemetryPath,
        [string]$SessionId
    )
    if (-not (Test-Path -Path $TelemetryPath)) {
        return @()
    }
    $events = New-Object System.Collections.Generic.List[object]
    foreach ($line in (Get-Content -Path $TelemetryPath)) {
        $parsed = Parse-JsonLine -Line $line
        if ($null -eq $parsed) {
            continue
        }
        if ([string]::Equals([string]$parsed.sessionId, $SessionId, [System.StringComparison]::Ordinal)) {
            $events.Add($parsed) | Out-Null
        }
    }
    return @($events.ToArray())
}

function Read-AllEvents {
    param([string]$TelemetryPath)
    if (-not (Test-Path -Path $TelemetryPath)) {
        return @()
    }
    $events = New-Object System.Collections.Generic.List[object]
    foreach ($line in (Get-Content -Path $TelemetryPath)) {
        $parsed = Parse-JsonLine -Line $line
        if ($null -ne $parsed) {
            $events.Add($parsed) | Out-Null
        }
    }
    return @($events.ToArray())
}

function Measure-ClickPattern {
    param(
        [string]$Label,
        [object[]]$Clicks,
        [int]$NearPx
    )
    $count = $Clicks.Count
    Write-Host ("{0}: clicks={1}" -f $Label, $count)
    if ($count -le 0) {
        return
    }

    $keys = $Clicks | ForEach-Object { "{0},{1}" -f $_.x, $_.y }
    $uniqueCount = @($keys | Sort-Object -Unique).Count
    $exactRepeatCount = [Math]::Max(0, $count - $uniqueCount)
    $exactConsecutive = 0
    $nearConsecutive = 0

    for ($i = 1; $i -lt $count; $i++) {
        $x1 = [int]$Clicks[$i - 1].x
        $y1 = [int]$Clicks[$i - 1].y
        $x2 = [int]$Clicks[$i].x
        $y2 = [int]$Clicks[$i].y
        if ($x1 -eq $x2 -and $y1 -eq $y2) {
            $exactConsecutive++
        }
        $dx = $x2 - $x1
        $dy = $y2 - $y1
        $dist = [Math]::Sqrt(($dx * $dx) + ($dy * $dy))
        if ($dist -le $NearPx) {
            $nearConsecutive++
        }
    }

    Write-Host ("  unique={0} exactRepeats={1} exactConsecutive={2} nearConsecutive(<= {3}px)={4}" -f $uniqueCount, $exactRepeatCount, $exactConsecutive, $NearPx, $nearConsecutive)
}

function Show-SessionPatternReport {
    param(
        [string]$TelemetryPath,
        [string]$SessionId,
        [int]$NearDropThresholdPx,
        [int]$NearWoodcutThresholdPx
    )
    $events = Read-SessionEvents -TelemetryPath $TelemetryPath -SessionId $SessionId
    if ($events.Count -eq 0) {
        Write-Host ("Pattern Report: no events for session {0}" -f $SessionId)
        return
    }

    $clickEvents = @($events | Where-Object { $_.reason -eq "interaction_click_telemetry" -and $null -ne $_.details })
    $ordered = $clickEvents | Sort-Object -Property emittedAtMs
    $drop = New-Object System.Collections.Generic.List[object]
    $wood = New-Object System.Collections.Generic.List[object]
    foreach ($evt in $ordered) {
        $details = $evt.details
        if ($null -eq $details.canvasX -or $null -eq $details.canvasY) {
            continue
        }
        $point = [PSCustomObject]@{
            x = [int]$details.canvasX
            y = [int]$details.canvasY
        }
        if ([string]$details.motorOwner -eq "drop_sweep") {
            $drop.Add($point) | Out-Null
        }
        if ([string]$details.clickType -eq "woodcut_world_interaction") {
            $wood.Add($point) | Out-Null
        }
    }

    Write-Host ("Pattern Report Session: {0}" -f $SessionId)
    Measure-ClickPattern -Label "Drop" -Clicks ($drop.ToArray()) -NearPx $NearDropThresholdPx
    Measure-ClickPattern -Label "Woodcut" -Clicks ($wood.ToArray()) -NearPx $NearWoodcutThresholdPx
}

function Emit-ReportForSession {
    param(
        [string]$TelemetryPath,
        [string]$SessionId,
        [string]$TriggerReason,
        [int]$DropNearPx,
        [int]$WoodcutNearPx
    )
    if ([string]::IsNullOrWhiteSpace($SessionId)) {
        return
    }
    Write-Host ""
    Write-Host ("==== Stop Event Detected ({0}) session={1} ====" -f $TriggerReason, $SessionId)
    & powershell -ExecutionPolicy Bypass -File $analyzerScript -Path $TelemetryPath -SessionId $SessionId
    Show-SessionPatternReport -TelemetryPath $TelemetryPath -SessionId $SessionId -NearDropThresholdPx $DropNearPx -NearWoodcutThresholdPx $WoodcutNearPx
    Write-Host "==== End Auto Report ===="
    Write-Host ""
}

if (-not (Test-Path -Path $Path)) {
    Write-Host ("Telemetry file not found: {0}" -f $Path)
    exit 1
}

$analyzerScript = Join-Path -Path $PSScriptRoot -ChildPath "analyze-xptool-telemetry.ps1"
if (-not (Test-Path -Path $analyzerScript)) {
    Write-Host ("Analyzer script not found: {0}" -f $analyzerScript)
    exit 1
}

$triggeredSessions = New-Object System.Collections.Generic.HashSet[string]
Write-Host ("Watching telemetry for stop reasons: {0}" -f ($StopReasons -join ", "))
Write-Host ("Path: {0}" -f $Path)

if ($ReportLatestOnStart) {
    $all = Read-AllEvents -TelemetryPath $Path
    $latestStop = $all |
        Where-Object {
            $null -ne $_ `
                -and $null -ne $_.PSObject `
                -and $null -ne $_.PSObject.Properties["sessionId"] `
                -and -not [string]::IsNullOrWhiteSpace([string]$_.sessionId) `
                -and $null -ne $_.PSObject.Properties["reason"] `
                -and ($StopReasons -contains [string]$_.reason)
        } |
        Select-Object -Last 1
    if ($null -ne $latestStop) {
        $bootstrapSession = [string]$latestStop.sessionId
        if (-not $triggeredSessions.Contains($bootstrapSession)) {
            $triggeredSessions.Add($bootstrapSession) | Out-Null
            Emit-ReportForSession -TelemetryPath $Path -SessionId $bootstrapSession -TriggerReason ([string]$latestStop.reason) -DropNearPx $NearDropPx -WoodcutNearPx $NearWoodcutPx
        }
    } else {
        if ($FallbackToLatestSessionOnStart) {
            $latestSessionEvent = ($all |
                Where-Object {
                    $null -ne $_ `
                        -and $null -ne $_.PSObject `
                        -and $null -ne $_.PSObject.Properties["sessionId"] `
                        -and -not [string]::IsNullOrWhiteSpace([string]$_.sessionId)
                } |
                Select-Object -Last 1)
            $latestSession = ""
            if ($null -ne $latestSessionEvent `
                -and $null -ne $latestSessionEvent.PSObject `
                -and $null -ne $latestSessionEvent.PSObject.Properties["sessionId"]) {
                $latestSession = [string]$latestSessionEvent.sessionId
            }
            if (-not [string]::IsNullOrWhiteSpace([string]$latestSession)) {
                $bootstrapSession = [string]$latestSession
                if (-not $triggeredSessions.Contains($bootstrapSession)) {
                    $triggeredSessions.Add($bootstrapSession) | Out-Null
                    Emit-ReportForSession -TelemetryPath $Path -SessionId $bootstrapSession -TriggerReason "latest_session_fallback" -DropNearPx $NearDropPx -WoodcutNearPx $NearWoodcutPx
                }
            } else {
                Write-Host "No session data found yet; waiting for next stop."
            }
        } else {
            Write-Host "No matching stop event found yet; waiting for next stop."
        }
    }
}

if ($Once.IsPresent) {
    exit 0
}

Get-Content -Path $Path -Wait -Tail 0 | ForEach-Object {
    $evt = Parse-JsonLine -Line $_
    if ($null -eq $evt -or $null -eq $evt.PSObject) {
        return
    }
    if ($null -eq $evt.PSObject.Properties["reason"] -or $null -eq $evt.PSObject.Properties["sessionId"]) {
        return
    }
    $reason = [string]$evt.reason
    $sessionId = [string]$evt.sessionId
    if ([string]::IsNullOrWhiteSpace($sessionId)) {
        return
    }
    if (-not ($StopReasons -contains $reason)) {
        return
    }
    if ($triggeredSessions.Contains($sessionId)) {
        return
    }
    $triggeredSessions.Add($sessionId) | Out-Null
    Emit-ReportForSession -TelemetryPath $Path -SessionId $sessionId -TriggerReason $reason -DropNearPx $NearDropPx -WoodcutNearPx $NearWoodcutPx
}
