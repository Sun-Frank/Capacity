param(
    [string]$EnvFile = (Join-Path $PSScriptRoot ".env.local"),
    [switch]$Foreground
)

$ErrorActionPreference = "Stop"

function Read-EnvFile {
    param([string]$Path)
    if (-not (Test-Path $Path)) {
        throw "Missing env file: $Path`nCopy deploy/.env.local.example to deploy/.env.local first."
    }

    $map = @{}
    foreach ($rawLine in Get-Content -Path $Path) {
        $line = $rawLine.Trim()
        if (-not $line -or $line.StartsWith("#")) { continue }
        $idx = $line.IndexOf("=")
        if ($idx -lt 1) { continue }
        $key = $line.Substring(0, $idx).Trim()
        $val = $line.Substring($idx + 1).Trim()
        $map[$key] = $val
    }
    return $map
}

function Expand-EnvValue {
    param([string]$Value, [hashtable]$Map)
    if ($null -eq $Value) { return $null }
    $result = $Value
    $guard = 0
    while ($result -match '\$\{([A-Za-z_][A-Za-z0-9_]*)\}' -and $guard -lt 10) {
        $name = $matches[1]
        $replacement = ""
        if ($Map.ContainsKey($name)) {
            $replacement = [string]$Map[$name]
        } elseif (Test-Path "Env:$name") {
            $replacement = [string](Get-Item "Env:$name").Value
        }
        $result = $result.Replace('${' + $name + '}', $replacement)
        $guard++
    }
    return $result
}

function Ensure-Value {
    param([hashtable]$Cfg, [string]$Key)
    if (-not $Cfg.ContainsKey($Key) -or [string]::IsNullOrWhiteSpace([string]$Cfg[$Key])) {
        throw "Required config is empty: $Key"
    }
}

function Escape-PsLiteral {
    param([string]$Value)
    return ($Value -replace "'", "''")
}

$projectRoot = Split-Path $PSScriptRoot -Parent
$backendDir = Join-Path $projectRoot "backend"
$tmpDir = Join-Path $projectRoot ".tmp"
$logDir = Join-Path $tmpDir "logs"
$runScriptPath = Join-Path $tmpDir "run-backend-only.ps1"
$pidFile = Join-Path $tmpDir "backend.pid"
$outLog = Join-Path $logDir "backend.out.log"
$errLog = Join-Path $logDir "backend.err.log"

New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$cfg = Read-EnvFile -Path $EnvFile
foreach ($k in @("SPRING_PROFILES_ACTIVE", "SERVER_PORT", "DB_URL", "DB_HOST", "DB_PORT", "DB_NAME", "DB_USERNAME", "DB_PASSWORD", "JWT_SECRET", "JWT_EXPIRATION", "APP_CORS_ALLOWED_ORIGINS", "LOG_LEVEL_APP", "LOG_LEVEL_SECURITY")) {
    if (-not $cfg.ContainsKey($k)) { $cfg[$k] = "" }
}

$cfg["DB_URL"] = Expand-EnvValue -Value ([string]$cfg["DB_URL"]) -Map $cfg
$cfg["APP_CORS_ALLOWED_ORIGINS"] = Expand-EnvValue -Value ([string]$cfg["APP_CORS_ALLOWED_ORIGINS"]) -Map $cfg

if ([string]::IsNullOrWhiteSpace([string]$cfg["SPRING_PROFILES_ACTIVE"])) { $cfg["SPRING_PROFILES_ACTIVE"] = "prod" }
if ([string]::IsNullOrWhiteSpace([string]$cfg["SERVER_PORT"])) { $cfg["SERVER_PORT"] = "8080" }
if ([string]::IsNullOrWhiteSpace([string]$cfg["DB_URL"])) {
    $cfg["DB_URL"] = "jdbc:postgresql://$($cfg["DB_HOST"]):$($cfg["DB_PORT"])/$($cfg["DB_NAME"])"
}
if ([string]::IsNullOrWhiteSpace([string]$cfg["JWT_EXPIRATION"])) { $cfg["JWT_EXPIRATION"] = "86400000" }
if ([string]::IsNullOrWhiteSpace([string]$cfg["LOG_LEVEL_APP"])) { $cfg["LOG_LEVEL_APP"] = "INFO" }
if ([string]::IsNullOrWhiteSpace([string]$cfg["LOG_LEVEL_SECURITY"])) { $cfg["LOG_LEVEL_SECURITY"] = "WARN" }

foreach ($k in @("DB_URL", "DB_USERNAME", "DB_PASSWORD", "JWT_SECRET", "APP_CORS_ALLOWED_ORIGINS")) {
    Ensure-Value -Cfg $cfg -Key $k
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "Command not found: mvn"
}

$serverPort = [int]$cfg["SERVER_PORT"]

Get-NetTCPConnection -State Listen -LocalPort $serverPort -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique |
    ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }

$runScript = @()
$runScript += "`$env:SPRING_PROFILES_ACTIVE='" + (Escape-PsLiteral $cfg["SPRING_PROFILES_ACTIVE"]) + "'"
$runScript += "`$env:SERVER_PORT='" + (Escape-PsLiteral $cfg["SERVER_PORT"]) + "'"
$runScript += "`$env:DB_URL='" + (Escape-PsLiteral $cfg["DB_URL"]) + "'"
$runScript += "`$env:DB_USERNAME='" + (Escape-PsLiteral $cfg["DB_USERNAME"]) + "'"
$runScript += "`$env:DB_PASSWORD='" + (Escape-PsLiteral $cfg["DB_PASSWORD"]) + "'"
$runScript += "`$env:JWT_SECRET='" + (Escape-PsLiteral $cfg["JWT_SECRET"]) + "'"
$runScript += "`$env:JWT_EXPIRATION='" + (Escape-PsLiteral $cfg["JWT_EXPIRATION"]) + "'"
$runScript += "`$env:APP_CORS_ALLOWED_ORIGINS='" + (Escape-PsLiteral $cfg["APP_CORS_ALLOWED_ORIGINS"]) + "'"
$runScript += "`$env:LOG_LEVEL_APP='" + (Escape-PsLiteral $cfg["LOG_LEVEL_APP"]) + "'"
$runScript += "`$env:LOG_LEVEL_SECURITY='" + (Escape-PsLiteral $cfg["LOG_LEVEL_SECURITY"]) + "'"
$runScript += "mvn spring-boot:run"

Set-Content -Path $runScriptPath -Value $runScript -Encoding UTF8

if ($Foreground) {
    Write-Host "Starting backend in foreground on port $serverPort ..."
    Push-Location $backendDir
    try {
        & powershell -NoProfile -ExecutionPolicy Bypass -File $runScriptPath
    } finally {
        Pop-Location
    }
    exit 0
}

$proc = Start-Process -FilePath "powershell" -WorkingDirectory $backendDir -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $runScriptPath) -RedirectStandardOutput $outLog -RedirectStandardError $errLog -PassThru
Set-Content -Path $pidFile -Value $proc.Id

$ok = $false
for ($i = 0; $i -lt 20; $i++) {
    Start-Sleep -Seconds 1
    try {
        $resp = Invoke-WebRequest -Uri ("http://127.0.0.1:{0}/api/health" -f $serverPort) -UseBasicParsing -TimeoutSec 3
        if ($resp.StatusCode -eq 200) {
            $ok = $true
            break
        }
    } catch {}
}

if ($ok) {
    Write-Host "Backend started."
    Write-Host "Health: http://127.0.0.1:$serverPort/api/health"
} else {
    Write-Warning "Backend start timeout. Check logs:"
    Write-Host $outLog
    Write-Host $errLog
}

