param(
    [string]$Path = "$env:USERPROFILE\.runelite\logs\xptool-telemetry.ndjson",
    [string]$SessionId = "",
    [int]$Tail = 0
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Read-TelemetryEvents {
    param(
        [string]$InputPath,
        [int]$TailCount
    )
    if (-not (Test-Path -Path $InputPath)) {
        Write-Host "Telemetry file not found: $InputPath"
        return $null
    }

    $lines = if ($TailCount -gt 0) {
        Get-Content -Path $InputPath | Select-Object -Last $TailCount
    } else {
        Get-Content -Path $InputPath
    }

    $events = New-Object System.Collections.Generic.List[object]
    foreach ($line in $lines) {
        if ([string]::IsNullOrWhiteSpace($line)) {
            continue
        }
        try {
            $parsed = $null
            try {
                $parsed = $line | ConvertFrom-Json -Depth 12
            } catch {
                $parsed = $line | ConvertFrom-Json
            }
            if ($null -ne $parsed) {
                $events.Add($parsed) | Out-Null
            }
        } catch {
            # Keep parsing resilient to partial/corrupt lines.
            continue
        }
    }
    return $events
}

function Get-IntValue {
    param(
        $Value,
        [int]$Fallback = 0
    )
    if ($null -eq $Value) {
        return $Fallback
    }
    try {
        return [int]$Value
    } catch {
        return $Fallback
    }
}

function Get-LongValue {
    param(
        $Value,
        [long]$Fallback = 0
    )
    if ($null -eq $Value) {
        return $Fallback
    }
    try {
        return [long]$Value
    } catch {
        return $Fallback
    }
}

$events = Read-TelemetryEvents -InputPath $Path -TailCount $Tail
if ($null -eq $events -or $events.Count -eq 0) {
    Write-Host "No telemetry events found in $Path"
    exit 0
}

if ([string]::IsNullOrWhiteSpace($SessionId)) {
    $SessionId = ($events | Select-Object -Last 1).sessionId
}

$sessionEvents = @($events | Where-Object { $_.sessionId -eq $SessionId })
if (-not $sessionEvents -or $sessionEvents.Count -eq 0) {
    Write-Host "No events found for sessionId: $SessionId"
    exit 0
}

$nonRollup = @($sessionEvents | Where-Object { $_.reason -ne "telemetry_rollup" })
$rollups = @($sessionEvents | Where-Object { $_.reason -eq "telemetry_rollup" })

$dropClicks = @($nonRollup | Where-Object {
    $_.reason -eq "interaction_click_telemetry" -and $_.details.motorOwner -eq "drop_sweep"
})
$dropRepeated = @($dropClicks | Where-Object { $_.details.repeatedExactPixelFromPrevious -eq $true })

$dropRepeatBlockedEvents = @($nonRollup | Where-Object { $_.reason -eq "drop_repeat_blocked" })
$dropRepeatBlockedCount = 0L
foreach ($evt in $dropRepeatBlockedEvents) {
    $raw = if ($null -ne $evt.details) { $evt.details.dropRepeatBlockedCount } else { $null }
    $value = Get-LongValue -Value $raw -Fallback 1
    if ($value -lt 1) {
        $value = 1
    }
    $dropRepeatBlockedCount += $value
}

$idleCursorMoves = @($nonRollup | Where-Object {
    $_.reason -eq "idle_hover_move" -or
    $_.reason -eq "idle_drift_move" -or
    $_.reason -eq "idle_hand_park_move" -or
    $_.reason -eq "idle_fishing_offscreen_park_move"
}).Count

$idleCameraMoves = @($nonRollup | Where-Object { $_.reason -eq "idle_camera_micro_adjust" }).Count

$clickTelemetry = @($nonRollup | Where-Object { $_.reason -eq "interaction_click_telemetry" })
$latencies = New-Object System.Collections.Generic.List[double]
foreach ($evt in $clickTelemetry) {
    $emittedAt = Get-LongValue -Value $evt.emittedAtMs -Fallback -1
    $clickAt = if ($null -ne $evt.details) { Get-LongValue -Value $evt.details.clickAtMs -Fallback -1 } else { -1 }
    if ($emittedAt -gt 0 -and $clickAt -gt 0 -and $emittedAt -ge $clickAt) {
        $latencies.Add([double]($emittedAt - $clickAt)) | Out-Null
    }
}

$avgLatency = -1
if ($latencies.Count -gt 0) {
    $avgLatency = [math]::Round((($latencies | Measure-Object -Average).Average), 2)
}

$woodcutDispatchEvents = @(
    $nonRollup | Where-Object {
        $_.reason -eq "woodcut_left_click_dispatched" -or $_.reason -eq "woodcut_motor_gesture_in_flight"
    }
)
$woodcutMovementCauseCounts = @{}
foreach ($evt in $woodcutDispatchEvents) {
    $cause = "<missing>"
    if ($null -ne $evt.details -and $null -ne $evt.details.PSObject -and $null -ne $evt.details.PSObject.Properties["movementCause"]) {
        $rawCause = [string]$evt.details.movementCause
        if (-not [string]::IsNullOrWhiteSpace($rawCause)) {
            $cause = $rawCause
        }
    }
    if (-not $woodcutMovementCauseCounts.ContainsKey($cause)) {
        $woodcutMovementCauseCounts[$cause] = 0
    }
    $woodcutMovementCauseCounts[$cause] = [int]$woodcutMovementCauseCounts[$cause] + 1
}

$first = $sessionEvents | Select-Object -First 1
$last = $sessionEvents | Select-Object -Last 1
$windowMs = (Get-LongValue -Value $last.emittedAtMs -Fallback 0) - (Get-LongValue -Value $first.emittedAtMs -Fallback 0)
if ($windowMs -lt 0) {
    $windowMs = 0
}

Write-Host "Session: $SessionId"
Write-Host "File: $Path"
Write-Host "WindowMs: $windowMs"
Write-Host "Events: $($sessionEvents.Count)"
Write-Host "NonRollupEvents: $($nonRollup.Count)"
Write-Host "RollupEvents: $($rollups.Count)"
Write-Host ""
Write-Host "Drop Summary"
Write-Host "  DropClicks: $($dropClicks.Count)"
Write-Host "  DropExactRepeatClicks: $($dropRepeated.Count)"
Write-Host "  DropRepeatBlockedEvents: $($dropRepeatBlockedEvents.Count)"
Write-Host "  DropRepeatBlockedCount: $dropRepeatBlockedCount"
Write-Host ""
Write-Host "Idle Summary"
Write-Host "  IdleCursorMoves: $idleCursorMoves"
Write-Host "  IdleCameraMoves: $idleCameraMoves"
Write-Host ""
Write-Host "Click Latency"
Write-Host "  Samples: $($latencies.Count)"
Write-Host "  AvgMs: $avgLatency"
Write-Host ""

Write-Host "Woodcut Movement Causes"
Write-Host "  DispatchEvents: $($woodcutDispatchEvents.Count)"
if ($woodcutMovementCauseCounts.Count -gt 0) {
    $woodcutMovementCauseCounts.GetEnumerator() |
        Sort-Object -Property Value -Descending |
        ForEach-Object {
            Write-Host ("  {0}`t{1}" -f $_.Value, $_.Key)
        }
} else {
    Write-Host "  (none)"
}
Write-Host ""

Write-Host "Top Reasons"
$nonRollup |
    Group-Object -Property reason |
    Sort-Object -Property Count -Descending |
    Select-Object -First 15 |
    ForEach-Object {
        Write-Host ("  {0}`t{1}" -f $_.Count, $_.Name)
    }

if ($rollups.Count -gt 0) {
    Write-Host ""
    Write-Host "Latest Rollups"
    $rollups |
        Select-Object -Last 3 |
        ForEach-Object {
            $d = $_.details
            $eventsCount = if ($null -ne $d) { Get-IntValue -Value $d.events -Fallback 0 } else { 0 }
            $drops = if ($null -ne $d) { Get-IntValue -Value $d.dropClicks -Fallback 0 } else { 0 }
            $repeat = if ($null -ne $d) { Get-IntValue -Value $d.dropExactRepeatClicks -Fallback 0 } else { 0 }
            $blocked = if ($null -ne $d) { Get-IntValue -Value $d.dropRepeatBlocked -Fallback 0 } else { 0 }
            $idleCursor = if ($null -ne $d) { Get-IntValue -Value $d.idleCursorMoves -Fallback 0 } else { 0 }
            $idleCamera = if ($null -ne $d) { Get-IntValue -Value $d.idleCameraMoves -Fallback 0 } else { 0 }
            Write-Host ("  seq={0} events={1} drop={2} repeat={3} blocked={4} idleCursor={5} idleCamera={6}" -f $_.eventSeq, $eventsCount, $drops, $repeat, $blocked, $idleCursor, $idleCamera)
        }
}
