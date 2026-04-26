param(
    [string]$EnvFile = (Join-Path $PSScriptRoot ".env.local"),
    [int]$FrontendPort = 3000,
    [switch]$ForegroundFrontend
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

if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    throw "Command not found: npm"
}

$npmCmd = (Get-Command npm.cmd -ErrorAction SilentlyContinue)
if (-not $npmCmd) {
    throw "Command not found: npm.cmd"
}

if (-not (Test-Path $frontendDir)) {
    throw "Frontend directory not found: $frontendDir"
}

Write-Host "Starting backend..."
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "local-start-backend.ps1") -EnvFile $EnvFile

$frontendListeners = Get-NetTCPConnection -State Listen -LocalPort $FrontendPort -ErrorAction SilentlyContinue
if ($frontendListeners) {
    $owners = ($frontendListeners | Select-Object -ExpandProperty OwningProcess -Unique) -join ", "
    Write-Warning "Frontend port $FrontendPort is already in use (PID: $owners). Skip starting frontend."
    Write-Host "If frontend is already running, you can open: http://127.0.0.1:$FrontendPort"
    exit 0
}

if ($ForegroundFrontend) {
    Write-Host "Starting frontend in foreground..."
    Write-Host "Frontend URL: http://127.0.0.1:$FrontendPort"
    Push-Location $frontendDir
    try {
        npm run dev -- --host 0.0.0.0 --port $FrontendPort
    } finally {
        Pop-Location
    }
    exit 0
}

Write-Host "Starting frontend in background..."
$frontendProc = Start-Process -FilePath $npmCmd.Source `
    -WorkingDirectory $frontendDir `
    -ArgumentList @("run", "dev", "--", "--host", "0.0.0.0", "--port", "$FrontendPort") `
    -RedirectStandardOutput $frontendOutLog `
    -RedirectStandardError $frontendErrLog `
    -WindowStyle Hidden `
    -PassThru

Set-Content -Path $frontendPidFile -Value $frontendProc.Id

$frontendReady = $false
for ($i = 0; $i -lt 20; $i++) {
    Start-Sleep -Seconds 1
    try {
        $resp = Invoke-WebRequest -Uri ("http://127.0.0.1:{0}" -f $FrontendPort) -UseBasicParsing -TimeoutSec 3
        if ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 500) {
            $frontendReady = $true
            break
        }
    } catch {}
}

if ($frontendReady) {
    Write-Host "Frontend started."
    Write-Host "Frontend URL: http://127.0.0.1:$FrontendPort"
} else {
    Write-Warning "Frontend start timeout. Check logs:"
    Write-Host $frontendOutLog
    Write-Host $frontendErrLog
}

Write-Host ""
Write-Host "All done."
Write-Host "Backend health: http://127.0.0.1:8080/api/health"
Write-Host "Frontend:      http://127.0.0.1:$FrontendPort"
