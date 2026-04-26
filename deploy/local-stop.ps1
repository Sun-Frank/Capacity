param()

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path $PSScriptRoot -Parent
$tmpDir = Join-Path $projectRoot ".tmp"
$legacyPidDir = Join-Path $tmpDir "pids"

function Stop-ProcessByPort {
    param(
        [string]$Name,
        [int[]]$Ports,
        [string[]]$AllowedProcessNames
    )

    foreach ($port in $Ports) {
        $listeners = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue
        if (-not $listeners) { continue }

        $ownerIds = $listeners | Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($ownerId in $ownerIds) {
            $proc = Get-Process -Id $ownerId -ErrorAction SilentlyContinue
            if ($null -eq $proc) { continue }
            if ($AllowedProcessNames -contains $proc.ProcessName.ToLowerInvariant()) {
                Stop-Process -Id $ownerId -Force -ErrorAction SilentlyContinue
                Write-Host "Stopped $Name process by port ${port}: $ownerId ($($proc.ProcessName))"
            }
        }
    }
}

foreach ($name in @("backend", "frontend")) {
    $pidFiles = @(
        (Join-Path $tmpDir "$name.pid"),
        (Join-Path $legacyPidDir "$name.pid")
    ) | Select-Object -Unique

    $stopped = $false
    $foundPidFile = $false

    foreach ($pidFile in $pidFiles) {
        if (-not (Test-Path $pidFile)) {
            continue
        }
        $foundPidFile = $true
        $rawPid = (Get-Content -Path $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1)
        $procId = if ($null -ne $rawPid) { $rawPid.Trim() } else { "" }

        if (-not $procId) {
            Remove-Item $pidFile -ErrorAction SilentlyContinue
            continue
        }

        $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
        if ($null -ne $proc) {
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            Write-Host "Stopped $name process: $procId"
            $stopped = $true
        } else {
            Write-Host "$name process already stopped: $procId"
        }

        Remove-Item $pidFile -ErrorAction SilentlyContinue
    }

    if (-not $foundPidFile) {
        Write-Host "Skip ${name}: pid file not found."
    } elseif (-not $stopped) {
        Write-Host "Skip ${name}: no running process found from pid file."
    }
}

# Fallback cleanup for stale/missing pid files
Stop-ProcessByPort -Name "backend" -Ports @(8080) -AllowedProcessNames @("java")
Stop-ProcessByPort -Name "frontend" -Ports @(3000, 5173) -AllowedProcessNames @("node")
