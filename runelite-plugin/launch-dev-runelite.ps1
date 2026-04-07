param(
    [switch]$SkipBuild,
    [switch]$DebugLogs,
    [ValidateSet("plugin", "bridge")]
    [string]$RuntimeMode = "plugin",
    [int]$BridgeIpcPort = 17888,
    [string]$BridgeBindAddress = "127.0.0.1",
    [string]$BridgeAuthToken = "",
    [switch]$BridgeLiveDispatch,
    [string]$BridgeLiveDispatchCommandAllowlist = "FISH_NEAREST_SPOT_SAFE,WOODCUT_CHOP_NEAREST_TREE_SAFE,SET_FISHING_IDLE_MODE_SAFE,WALK_TO_WORLDPOINT_SAFE,CAMERA_NUDGE_SAFE,DROP_START_SESSION,DROP_STOP_SESSION,DROP_ITEM_SAFE,STOP_ALL_RUNTIME,LOGOUT_SAFE,LOGIN_START_TEST",
    [ValidateSet("off", "soft", "strict-no-sig", "strict-sig")]
    [string]$CommandEnvelopeProfile = "strict-no-sig",
    [switch]$EnableCommandEnvelopeValidation,
    [switch]$RequireCommandEnvelope,
    [switch]$VerifyCommandEnvelopeSignature,
    [string]$CommandEnvelopeSigningKey = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$pluginDir = $PSScriptRoot
$jarPath = $null
$commandBusPath = Join-Path $pluginDir "tools\command-bus.ndjson"
$runeliteDir = Join-Path $env:USERPROFILE ".runelite"
$sideloadDir = Join-Path $runeliteDir "sideloaded-plugins"
$telemetryPath = Join-Path $runeliteDir "logs\xptool-telemetry.ndjson"
$clientLogPath = Join-Path $runeliteDir "logs\client.log"
$profilesDir = Join-Path $runeliteDir "profiles2"
$installedClientDir = Join-Path $env:LOCALAPPDATA "RuneLite"
$javaExe = Join-Path $installedClientDir "jre\bin\java.exe"
$repo2 = Join-Path $runeliteDir "repository2"
$jdk21 = "C:\Program Files\Java\jdk-21"

function Resolve-RepositoryClasspath {
    param(
        [string]$RepositoryPath
    )

    $allJars = Get-ChildItem -Path $RepositoryPath -Filter *.jar -ErrorAction SilentlyContinue
    if (-not $allJars -or $allJars.Count -eq 0) {
        throw "No jars found in $RepositoryPath"
    }

    # repository2 can contain multiple historical versions; keep only the newest jar per artifact.
    $latestPerArtifact = @{}
    foreach ($jar in $allJars) {
        $name = $jar.Name
        $parsedVersion = $null
        if ($name -match '^(?<artifact>.+)-(?<version>\d+(?:\.\d+)*)(?<classifier>(?:-[A-Za-z0-9._-]+)?)\.jar$') {
            $artifact = $Matches['artifact']
            $classifier = $Matches['classifier']
            if (-not [string]::IsNullOrWhiteSpace($classifier)) {
                $artifact = "$artifact$classifier"
            }
            $versionText = $Matches['version']
            try {
                $parsedVersion = [version]$versionText
            } catch {
                $parsedVersion = $null
            }
        } else {
            $artifact = [System.IO.Path]::GetFileNameWithoutExtension($name)
        }

        if (-not $latestPerArtifact.ContainsKey($artifact)) {
            $latestPerArtifact[$artifact] = [pscustomobject]@{
                Jar = $jar
                Version = $parsedVersion
            }
            continue
        }

        $existing = $latestPerArtifact[$artifact]
        $existingJar = $existing.Jar
        $pickCandidate = $false
        if ($parsedVersion -ne $null -and $existing.Version -ne $null) {
            if ($parsedVersion -gt $existing.Version) {
                $pickCandidate = $true
            } elseif ($parsedVersion -eq $existing.Version) {
                if ($jar.LastWriteTimeUtc -gt $existingJar.LastWriteTimeUtc) {
                    $pickCandidate = $true
                } elseif ($jar.LastWriteTimeUtc -eq $existingJar.LastWriteTimeUtc -and $jar.Name -gt $existingJar.Name) {
                    $pickCandidate = $true
                }
            }
        } elseif ($jar.LastWriteTimeUtc -gt $existingJar.LastWriteTimeUtc) {
            $pickCandidate = $true
        } elseif ($jar.LastWriteTimeUtc -eq $existingJar.LastWriteTimeUtc -and $jar.Name -gt $existingJar.Name) {
            $pickCandidate = $true
        }

        if ($pickCandidate) {
            $latestPerArtifact[$artifact] = [pscustomobject]@{
                Jar = $jar
                Version = $parsedVersion
            }
        }
    }

    return ($latestPerArtifact.Values | ForEach-Object { $_.Jar } | Sort-Object Name | ForEach-Object FullName) -join ';'
}

function Set-ConfigValue {
    param(
        [string]$FilePath,
        [string]$Key,
        [string]$Value
    )

    if (-not (Test-Path $FilePath)) {
        return
    }

    $escapedKey = [regex]::Escape($Key)
    $lines = Get-Content -Path $FilePath
    $updated = $false

    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match "^$escapedKey=") {
            $lines[$i] = "$Key=$Value"
            $updated = $true
            break
        }
    }

    if (-not $updated) {
        $lines += "$Key=$Value"
    }

    Set-Content -Path $FilePath -Value $lines
}

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

function Resolve-AttachJavaExecutable {
    param(
        [string]$FallbackJavaExe
    )

    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $javaHomeExe = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (Test-Path $javaHomeExe) {
            return $javaHomeExe
        }
    }

    return $FallbackJavaExe
}

function Resolve-AttachTargetPid {
    param(
        [int]$PreferredPid
    )

    if ($PreferredPid -le 0) {
        return $PreferredPid
    }

    $direct = Get-Process -Id $PreferredPid -ErrorAction SilentlyContinue
    if ($direct -and ($direct.ProcessName -ieq "java" -or $direct.ProcessName -ieq "javaw")) {
        return $PreferredPid
    }

    try {
        $child = Get-CimInstance -ClassName Win32_Process -Filter "ParentProcessId = $PreferredPid" -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -ieq "java.exe" -or $_.Name -ieq "javaw.exe" } |
            Select-Object -First 1
        if ($child) {
            return [int]$child.ProcessId
        }
    } catch {
        # Best-effort child pid discovery.
    }

    return $PreferredPid
}

function Invoke-BridgeAgentAttach {
    param(
        [string]$AttachJavaExe,
        [int]$TargetPid,
        [string]$AgentJarPath,
        [string]$AgentArgs = "",
        [int]$MaxAttempts = 40,
        [int]$RetryDelayMs = 250
    )

    if ($TargetPid -le 0) {
        throw "Invalid target JVM pid: $TargetPid"
    }
    if (-not (Test-Path $AttachJavaExe)) {
        throw "Attach Java runtime not found: $AttachJavaExe"
    }
    if (-not (Test-Path $AgentJarPath)) {
        throw "Agent JAR not found: $AgentJarPath"
    }

    $resolvedTargetPid = Resolve-AttachTargetPid -PreferredPid $TargetPid
    if ($resolvedTargetPid -ne $TargetPid) {
        Write-Host "Resolved attach target pid $resolvedTargetPid (launcher pid $TargetPid)."
    }

    $lastError = ""
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        $currentTargetProcess = Get-Process -Id $resolvedTargetPid -ErrorAction SilentlyContinue
        if (-not $currentTargetProcess) {
            $lastError = "target_process_not_running pid=$resolvedTargetPid"
            break
        }

        $attachArgs = @(
            "--add-modules=jdk.attach",
            "-cp",
            $AgentJarPath,
            "com.xptool.bridge.BridgeAgentAttacher",
            "--pid",
            "$resolvedTargetPid",
            "--agent-jar",
            $AgentJarPath
        )
        if (-not [string]::IsNullOrWhiteSpace($AgentArgs)) {
            $attachArgs += @("--agent-args", $AgentArgs)
        }

        $stdoutPath = [System.IO.Path]::GetTempFileName()
        $stderrPath = [System.IO.Path]::GetTempFileName()
        try {
            $attachProcess = Start-Process `
                -FilePath $AttachJavaExe `
                -ArgumentList $attachArgs `
                -PassThru `
                -NoNewWindow `
                -Wait `
                -RedirectStandardOutput $stdoutPath `
                -RedirectStandardError $stderrPath

            $stdoutText = ""
            $stderrText = ""
            if (Test-Path $stdoutPath) {
                $stdoutText = [System.Convert]::ToString((Get-Content -Path $stdoutPath -Raw -ErrorAction SilentlyContinue))
                $stdoutText = $stdoutText.Trim()
            }
            if (Test-Path $stderrPath) {
                $stderrText = [System.Convert]::ToString((Get-Content -Path $stderrPath -Raw -ErrorAction SilentlyContinue))
                $stderrText = $stderrText.Trim()
            }
            $attachOutput = @($stdoutText, $stderrText) |
                Where-Object { -not [string]::IsNullOrWhiteSpace($_) }

            if ($attachProcess.ExitCode -eq 0) {
                if ($attachOutput) {
                    $attachOutput | ForEach-Object { Write-Host $_ }
                }
                return
            }

            $lastError = ($attachOutput | Out-String).Trim()
            if ($lastError -match "jvm\\.dll not loaded by target process") {
                $candidatePid = Resolve-AttachTargetPid -PreferredPid $TargetPid
                if ($candidatePid -gt 0 -and $candidatePid -ne $resolvedTargetPid) {
                    $resolvedTargetPid = $candidatePid
                    Write-Host "Retrying attach with resolved target pid $resolvedTargetPid."
                }
            }
        } finally {
            Remove-Item -Path $stdoutPath -Force -ErrorAction SilentlyContinue
            Remove-Item -Path $stderrPath -Force -ErrorAction SilentlyContinue
        }

        if ($attempt -lt $MaxAttempts) {
            Start-Sleep -Milliseconds $RetryDelayMs
        }
    }

    if ([string]::IsNullOrWhiteSpace($lastError)) {
        $lastError = "unknown"
    }
    throw "Bridge agent attach failed after $MaxAttempts attempts for pid $TargetPid. Last error: $lastError"
}

function Normalize-BridgeLiveDispatchAllowlist {
    param(
        [string]$Csv
    )

    $required = @("LOGOUT_SAFE", "LOGIN_START_TEST")
    $ordered = New-Object System.Collections.Generic.List[string]
    $seen = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::OrdinalIgnoreCase)

    foreach ($token in ($Csv -split ",")) {
        $normalized = [string]$token
        $normalized = $normalized.Trim().ToUpperInvariant()
        if ([string]::IsNullOrWhiteSpace($normalized)) {
            continue
        }
        if ($seen.Add($normalized)) {
            [void]$ordered.Add($normalized)
        }
    }

    foreach ($cmd in $required) {
        if ($seen.Add($cmd)) {
            [void]$ordered.Add($cmd)
        }
    }

    return ($ordered -join ",")
}

if (Test-Path (Join-Path $jdk21 "bin\java.exe")) {
    $env:JAVA_HOME = $jdk21
    $env:Path = "$($env:JAVA_HOME)\bin;$($env:Path)"
    Write-Host "Using build JDK: $($env:JAVA_HOME)"
} else {
    Write-Warning "JDK 21 not found at $jdk21. Gradle may fail if JAVA_HOME points to an unsupported JDK."
}

if (-not $SkipBuild) {
    Write-Host "Building plugin JAR..."
    Push-Location $pluginDir
    try {
        & .\gradlew.bat build
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle build failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

$jarCandidates = Get-ChildItem -Path (Join-Path $pluginDir "build\libs") -Filter *.jar -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending
if ($jarCandidates -and $jarCandidates.Count -gt 0) {
    $jarPath = $jarCandidates[0].FullName
}
if ([string]::IsNullOrWhiteSpace($jarPath) -or -not (Test-Path $jarPath)) {
    throw "Plugin JAR not found in $(Join-Path $pluginDir 'build\\libs')"
}

$normalizedRuntimeMode = $RuntimeMode.Trim().ToLowerInvariant()
$isPluginMode = $normalizedRuntimeMode -eq "plugin"
$isBridgeMode = $normalizedRuntimeMode -eq "bridge"
if (-not $isPluginMode -and -not $isBridgeMode) {
    throw "Unsupported runtime mode: $RuntimeMode"
}
if ($BridgeIpcPort -lt 1 -or $BridgeIpcPort -gt 65535) {
    throw "Bridge IPC port must be between 1 and 65535."
}

Write-Host "Runtime mode: $normalizedRuntimeMode"

New-Item -ItemType Directory -Path (Split-Path -Parent $commandBusPath) -Force | Out-Null
if (-not (Test-Path $commandBusPath)) {
    New-Item -ItemType File -Path $commandBusPath -Force | Out-Null
}
if ($isPluginMode) {
    New-Item -ItemType Directory -Path $sideloadDir -Force | Out-Null
    # Keep only the latest XPTool sideload jar to avoid duplicate plugin loads.
    Get-ChildItem -Path $sideloadDir -Filter "xptool*.jar" -ErrorAction SilentlyContinue | ForEach-Object {
        Remove-Item -Path $_.FullName -Force -ErrorAction SilentlyContinue
    }
    Copy-Item -Path $jarPath -Destination (Join-Path $sideloadDir (Split-Path $jarPath -Leaf)) -Force
    Write-Host "Copied plugin to $sideloadDir"
} else {
    Write-Host "Bridge mode selected; skipping sideload plugin copy."
}
Write-Host "Using command bus path: $commandBusPath"
Write-Host "Using telemetry path: $telemetryPath"

if (-not (Test-Path $javaExe)) {
    throw "RuneLite Java runtime not found: $javaExe"
}
if (-not (Test-Path $repo2)) {
    throw "RuneLite dependency cache not found: $repo2"
}

$classpath = Resolve-RepositoryClasspath -RepositoryPath $repo2

if (Test-Path $profilesDir) {
    $xptoolPluginConfigValue = if ($isPluginMode) { "true" } else { "false" }
    Get-ChildItem -Path $profilesDir -Filter *.properties -ErrorAction SilentlyContinue | ForEach-Object {
        Set-ConfigValue -FilePath $_.FullName -Key "runelite.xptoolplugin" -Value $xptoolPluginConfigValue
        # RuneLite 1.12.19 developer-mode classpath can fail to load DevTools.
        Set-ConfigValue -FilePath $_.FullName -Key "runelite.devtoolsplugin" -Value "false"
    }
    Write-Host "Ensured runelite.xptoolplugin=$xptoolPluginConfigValue and runelite.devtoolsplugin=false across RuneLite profiles."
}
$env:XPTOOL_COMMAND_FILE_PATH = $commandBusPath

$normalizedEnvelopeProfile = $CommandEnvelopeProfile.Trim().ToLowerInvariant()
$profileEnableEnvelopeValidation = $false
$profileRequireEnvelope = $false
$profileVerifyEnvelopeSignature = $false
$effectiveEnableEnvelopeValidation = $false
$effectiveRequireEnvelope = $false
$effectiveVerifyEnvelopeSignature = $false

if ($isPluginMode) {
    switch ($normalizedEnvelopeProfile) {
        "soft" {
            $profileEnableEnvelopeValidation = $true
        }
        "strict-no-sig" {
            $profileEnableEnvelopeValidation = $true
            $profileRequireEnvelope = $true
        }
        "strict-sig" {
            $profileEnableEnvelopeValidation = $true
            $profileRequireEnvelope = $true
            $profileVerifyEnvelopeSignature = $true
        }
        default {
            # "off" keeps envelope validation disabled unless explicit switches are provided.
        }
    }

    $effectiveEnableEnvelopeValidation = $profileEnableEnvelopeValidation -or $EnableCommandEnvelopeValidation
    $effectiveRequireEnvelope = $profileRequireEnvelope -or $RequireCommandEnvelope
    $effectiveVerifyEnvelopeSignature = $profileVerifyEnvelopeSignature -or $VerifyCommandEnvelopeSignature

    if ($effectiveRequireEnvelope -or $effectiveVerifyEnvelopeSignature) {
        $effectiveEnableEnvelopeValidation = $true
    }
    if ($effectiveVerifyEnvelopeSignature -and -not $effectiveRequireEnvelope) {
        Write-Warning "Signature verification is enabled without required envelopes. Commands without commandEnvelope may still be accepted."
    }

    Write-Host "Command envelope profile: $normalizedEnvelopeProfile"
} else {
    Write-Host "Command envelope profile ignored in bridge mode."
}

$launchArgs = @(
    "-ea",
    "-Dxptool.idleRuntimeEnabled=true",
    "-Dxptool.loginBreakRuntimeEnabled=true",
    "-Dxptool.verboseExecutionLogs=$($DebugLogs.IsPresent.ToString().ToLowerInvariant())",
    "-Dxptool.telemetryFileEnabled=true",
    "-Dxptool.telemetryFilePath=$telemetryPath",
    "-cp",
    $classpath,
    "net.runelite.client.RuneLite"
)

if ($isPluginMode) {
    Write-Host "Launching RuneLite in plugin mode (developer sideload flow)..."
    $launchArgs += "--developer-mode"
} else {
    Write-Host "Launching RuneLite in bridge mode (post-launch attach, no developer mode)..."
    if (-not (Test-LoopbackBindAddress -Address $BridgeBindAddress)) {
        throw "Bridge bind address must resolve to loopback only. Provided: $BridgeBindAddress"
    }
    $effectiveBridgeAuthToken = $BridgeAuthToken
    if ([string]::IsNullOrWhiteSpace($effectiveBridgeAuthToken)) {
        $effectiveBridgeAuthToken = $env:XPTOOL_BRIDGE_AUTH_TOKEN
    }
    if ([string]::IsNullOrWhiteSpace($effectiveBridgeAuthToken)) {
        throw "Bridge auth token is required. Set -BridgeAuthToken or XPTOOL_BRIDGE_AUTH_TOKEN."
    }
    $bridgeLiveDispatchEnabled = $BridgeLiveDispatch.IsPresent
    $bridgeCommandExecutorShadowOnly = -not $bridgeLiveDispatchEnabled
    $effectiveBridgeLiveDispatchAllowlist = Normalize-BridgeLiveDispatchAllowlist -Csv $BridgeLiveDispatchCommandAllowlist
    $launchArgs = @(
        "-Dxptool.bridge.enabled=true",
        "-Dxptool.bridge.bindAddress=$BridgeBindAddress",
        "-Dxptool.bridge.ipcPort=$BridgeIpcPort",
        "-Dxptool.commandExecutorShadowOnly=$($bridgeCommandExecutorShadowOnly.ToString().ToLowerInvariant())",
        "-Dxptool.bridge.liveDispatch=$($bridgeLiveDispatchEnabled.ToString().ToLowerInvariant())",
        "-Dxptool.bridge.liveDispatchCommandAllowlist=$effectiveBridgeLiveDispatchAllowlist"
    ) + $launchArgs
    $launchArgs = @("-Dxptool.bridge.authToken=$effectiveBridgeAuthToken") + $launchArgs
    Write-Host "Bridge bind address enforced: $BridgeBindAddress"
    Write-Host "Bridge IPC auth token enabled."
    Write-Host "Bridge command executor shadow-only: $($bridgeCommandExecutorShadowOnly.ToString().ToLowerInvariant())"
    Write-Host "Bridge live dispatch enabled: $($bridgeLiveDispatchEnabled.ToString().ToLowerInvariant())"
    Write-Host "Bridge live dispatch allowlist: $effectiveBridgeLiveDispatchAllowlist"
}

if ($isPluginMode -and $effectiveEnableEnvelopeValidation) {
    $launchArgs = @("-Dxptool.commandEnvelopeValidationEnabled=true") + $launchArgs
    Write-Host "Command envelope validation enabled."
}
if ($isPluginMode -and $effectiveRequireEnvelope) {
    $launchArgs = @("-Dxptool.commandEnvelopeRequire=true") + $launchArgs
    Write-Host "Command envelope requirement enabled."
}
if ($isPluginMode -and $effectiveVerifyEnvelopeSignature) {
    $launchArgs = @("-Dxptool.commandEnvelopeVerifySignature=true") + $launchArgs
    $effectiveSigningKey = $CommandEnvelopeSigningKey
    if ([string]::IsNullOrWhiteSpace($effectiveSigningKey)) {
        $effectiveSigningKey = $env:XPTOOL_REMOTE_PLANNER_SIGNING_KEY
    }
    if ([string]::IsNullOrWhiteSpace($effectiveSigningKey)) {
        throw "Command envelope signature verification requested but no signing key provided. Set -CommandEnvelopeSigningKey or XPTOOL_REMOTE_PLANNER_SIGNING_KEY."
    }
    $launchArgs = @("-Dxptool.commandEnvelopeSigningKey=$effectiveSigningKey") + $launchArgs
    Write-Host "Command envelope signature verification enabled."
}
if ($DebugLogs) {
    Write-Host "xptool verbose execution logs enabled."
}
$launchedProcess = Start-Process -FilePath $javaExe -ArgumentList $launchArgs -PassThru
if ($isBridgeMode) {
    $attachJavaExe = Resolve-AttachJavaExecutable -FallbackJavaExe $javaExe
    Write-Host "Attaching bridge agent to RuneLite pid $($launchedProcess.Id) using $attachJavaExe"
    try {
        Invoke-BridgeAgentAttach `
            -AttachJavaExe $attachJavaExe `
            -TargetPid $launchedProcess.Id `
            -AgentJarPath $jarPath `
            -AgentArgs "enabled=true"
    } catch {
        if ($launchedProcess -and -not $launchedProcess.HasExited) {
            Stop-Process -Id $launchedProcess.Id -Force -ErrorAction SilentlyContinue
        }
        throw
    }
    Write-Host "Bridge agent attached (bridge IPC-only mode active; runtime snapshot stream not expected)."
}
