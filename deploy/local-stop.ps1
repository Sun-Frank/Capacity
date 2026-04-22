param()

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path $PSScriptRoot -Parent
$pidDir = Join-Path $projectRoot ".tmp\pids"

if (-not (Test-Path $pidDir)) {
    Write-Host "No pid directory found: $pidDir"
    exit 0
}

foreach ($name in @("backend", "frontend")) {
    $pidFile = Join-Path $pidDir "$name.pid"
    if (-not (Test-Path $pidFile)) {
        Write-Host "Skip ${name}: pid file not found."
        continue
    }
    $procId = (Get-Content -Path $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1).Trim()
    if (-not $procId) {
        Remove-Item $pidFile -ErrorAction SilentlyContinue
        Write-Host "Skip ${name}: empty pid file."
        continue
    }
    $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
    if ($null -ne $proc) {
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped $name process: $procId"
    } else {
        Write-Host "$name process already stopped: $procId"
    }
    Remove-Item $pidFile -ErrorAction SilentlyContinue
}
