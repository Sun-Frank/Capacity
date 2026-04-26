param(
    [string]$EnvFile = (Join-Path $PSScriptRoot ".env.local"),
    [int]$FrontendPort = 3000
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path $PSScriptRoot -Parent
$frontendDir = Join-Path $projectRoot "capics-frontend"
$tmpDir = Join-Path $projectRoot ".tmp"
$logDir = Join-Path $tmpDir "logs"
$frontendPidFile = Join-Path $tmpDir "frontend.pid"
$frontendOutLog = Join-Path $logDir "frontend.out.log"
$frontendErrLog = Join-Path $logDir "frontend.err.log"

New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

# Start backend first (background mode handled inside script)
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "local-start-backend.ps1") -EnvFile $EnvFile | Out-Null

$frontendListeners = Get-NetTCPConnection -State Listen -LocalPort $FrontendPort -ErrorAction SilentlyContinue
if ($frontendListeners) {
    exit 0
}

$npmCmd = (Get-Command npm.cmd -ErrorAction SilentlyContinue)
if (-not $npmCmd) {
    throw "Command not found: npm.cmd"
}
if (-not (Test-Path $frontendDir)) {
    throw "Frontend directory not found: $frontendDir"
}

$frontendProc = Start-Process -FilePath $npmCmd.Source `
    -WorkingDirectory $frontendDir `
    -ArgumentList @("run", "dev", "--", "--host", "0.0.0.0", "--port", "$FrontendPort") `
    -RedirectStandardOutput $frontendOutLog `
    -RedirectStandardError $frontendErrLog `
    -WindowStyle Hidden `
    -PassThru

Set-Content -Path $frontendPidFile -Value $frontendProc.Id

# Best-effort readiness wait
for ($i = 0; $i -lt 20; $i++) {
    Start-Sleep -Seconds 1
    try {
        $resp = Invoke-WebRequest -Uri ("http://127.0.0.1:{0}" -f $FrontendPort) -UseBasicParsing -TimeoutSec 3
        if ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 500) {
            break
        }
    } catch {}
}

