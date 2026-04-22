param(
    [string]$EnvFile = (Join-Path $PSScriptRoot ".env.local"),
    [switch]$NoStart
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

function Require-Command {
    param([string]$Cmd)
    if (-not (Get-Command $Cmd -ErrorAction SilentlyContinue)) {
        throw "Command not found: $Cmd"
    }
}

function Get-PsqlPath {
    param([hashtable]$Cfg)
    $candidates = @()
    if ($Cfg.ContainsKey("PSQL_BIN")) { $candidates += $Cfg["PSQL_BIN"] }
    $candidates += "C:\Program Files\PostgreSQL\18\bin\psql.exe"
    $candidates += "C:\Program Files\PostgreSQL\17\bin\psql.exe"
    $candidates += "psql"

    foreach ($c in $candidates) {
        if ($c -eq "psql") {
            $cmd = Get-Command psql -ErrorAction SilentlyContinue
            if ($cmd) { return $cmd.Source }
            continue
        }
        if (Test-Path $c) { return $c }
    }
    throw "psql not found. Set PSQL_BIN in deploy/.env.local."
}

function Ensure-Value {
    param([hashtable]$Cfg, [string]$Key)
    if (-not $Cfg.ContainsKey($Key) -or [string]::IsNullOrWhiteSpace([string]$Cfg[$Key])) {
        throw "Required config is empty: $Key"
    }
}

function Resolve-Secret {
    param([hashtable]$Cfg, [string]$Key)
    $v = [string]$Cfg[$Key]
    if ([string]::IsNullOrWhiteSpace($v) -or $v -like "CHANGE_ME*") {
        $secure = Read-Host -Prompt "$Key (input hidden)" -AsSecureString
        $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        try {
            $v = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
        } finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
        }
        if ([string]::IsNullOrWhiteSpace($v)) {
            throw "$Key is required."
        }
        $Cfg[$Key] = $v
    }
}

function Invoke-Psql {
    param(
        [string]$Psql,
        [string]$DbHost,
        [string]$DbPort,
        [string]$User,
        [string]$Db,
        [string]$Password,
        [string]$Sql,
        [switch]$Quiet
    )
    $env:PGPASSWORD = $Password
    try {
        $args = @("-h", $DbHost, "-p", $DbPort, "-U", $User, "-d", $Db, "-v", "ON_ERROR_STOP=1")
        if ($Quiet) { $args += @("-tA") }
        $args += @("-c", $Sql)
        $output = & $Psql @args
        if ($LASTEXITCODE -ne 0) {
            throw "psql failed: $Sql"
        }
        return $output
    } finally {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    }
}

function Invoke-PsqlFile {
    param(
        [string]$Psql,
        [string]$DbHost,
        [string]$DbPort,
        [string]$User,
        [string]$Db,
        [string]$Password,
        [string]$FilePath
    )
    $env:PGPASSWORD = $Password
    try {
        & $Psql "-h" $DbHost "-p" $DbPort "-U" $User "-d" $Db "-v" "ON_ERROR_STOP=1" "-f" $FilePath
        if ($LASTEXITCODE -ne 0) {
            throw "psql failed for file: $FilePath"
        }
    } finally {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    }
}

function Escape-PsLiteral {
    param([string]$s)
    return ($s -replace "'", "''")
}

$projectRoot = Split-Path $PSScriptRoot -Parent
$backendDir = Join-Path $projectRoot "backend"
$frontendDir = Join-Path $projectRoot "capics-frontend"
$schemaFile = Join-Path $backendDir "src\main\resources\schema.sql"
$tmpDir = Join-Path $projectRoot ".tmp"
$pidDir = Join-Path $tmpDir "pids"
$logDir = Join-Path $tmpDir "logs"

New-Item -ItemType Directory -Force -Path $pidDir | Out-Null
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$cfg = Read-EnvFile -Path $EnvFile
foreach ($k in @("SPRING_PROFILES_ACTIVE","SERVER_PORT","DB_HOST","DB_PORT","DB_NAME","DB_URL","DB_USERNAME","DB_PASSWORD","DB_SCHEMA_MODE","POSTGRES_ADMIN_USER","POSTGRES_ADMIN_PASSWORD","JWT_SECRET","JWT_EXPIRATION","APP_CORS_ALLOWED_ORIGINS","LOG_LEVEL_APP","LOG_LEVEL_SECURITY","FRONTEND_PORT")) {
    if (-not $cfg.ContainsKey($k)) { $cfg[$k] = "" }
}

foreach ($k in @("DB_URL","APP_CORS_ALLOWED_ORIGINS")) {
    $cfg[$k] = Expand-EnvValue -Value ([string]$cfg[$k]) -Map $cfg
}

if ([string]::IsNullOrWhiteSpace([string]$cfg["SPRING_PROFILES_ACTIVE"])) { $cfg["SPRING_PROFILES_ACTIVE"] = "prod" }
if ([string]::IsNullOrWhiteSpace([string]$cfg["SERVER_PORT"])) { $cfg["SERVER_PORT"] = "8080" }
if ([string]::IsNullOrWhiteSpace([string]$cfg["DB_SCHEMA_MODE"])) { $cfg["DB_SCHEMA_MODE"] = "reset" }
if ([string]::IsNullOrWhiteSpace([string]$cfg["FRONTEND_PORT"])) { $cfg["FRONTEND_PORT"] = "3000" }
if ([string]::IsNullOrWhiteSpace([string]$cfg["LOG_LEVEL_APP"])) { $cfg["LOG_LEVEL_APP"] = "INFO" }
if ([string]::IsNullOrWhiteSpace([string]$cfg["LOG_LEVEL_SECURITY"])) { $cfg["LOG_LEVEL_SECURITY"] = "WARN" }

foreach ($k in @("DB_HOST","DB_PORT","DB_NAME","DB_URL","DB_USERNAME","POSTGRES_ADMIN_USER","JWT_EXPIRATION","APP_CORS_ALLOWED_ORIGINS")) {
    Ensure-Value -Cfg $cfg -Key $k
}

Resolve-Secret -Cfg $cfg -Key "DB_PASSWORD"
Resolve-Secret -Cfg $cfg -Key "POSTGRES_ADMIN_PASSWORD"
Resolve-Secret -Cfg $cfg -Key "JWT_SECRET"

Require-Command "mvn"
Require-Command "node"
Require-Command "npm"
Require-Command "java"
$psql = Get-PsqlPath -Cfg $cfg

Write-Host "[1/5] Initialize database role/database..."
$adminHost = $cfg["DB_HOST"]
$adminPort = $cfg["DB_PORT"]
$adminUser = $cfg["POSTGRES_ADMIN_USER"]
$adminPass = $cfg["POSTGRES_ADMIN_PASSWORD"]
$dbUser = $cfg["DB_USERNAME"]
$dbPass = $cfg["DB_PASSWORD"]
$dbName = $cfg["DB_NAME"]
$schemaMode = $cfg["DB_SCHEMA_MODE"].ToLower()

$roleExists = (Invoke-Psql -Psql $psql -DbHost $adminHost -DbPort $adminPort -User $adminUser -Db "postgres" -Password $adminPass -Sql "SELECT 1 FROM pg_roles WHERE rolname='$dbUser';" -Quiet).Trim()
if ($roleExists -ne "1") {
    Invoke-Psql -Psql $psql -DbHost $adminHost -DbPort $adminPort -User $adminUser -Db "postgres" -Password $adminPass -Sql "CREATE USER $dbUser WITH PASSWORD '$dbPass';" | Out-Null
} else {
    Invoke-Psql -Psql $psql -DbHost $adminHost -DbPort $adminPort -User $adminUser -Db "postgres" -Password $adminPass -Sql "ALTER USER $dbUser WITH PASSWORD '$dbPass';" | Out-Null
}

$dbExists = (Invoke-Psql -Psql $psql -DbHost $adminHost -DbPort $adminPort -User $adminUser -Db "postgres" -Password $adminPass -Sql "SELECT 1 FROM pg_database WHERE datname='$dbName';" -Quiet).Trim()
if ($dbExists -ne "1") {
    Invoke-Psql -Psql $psql -DbHost $adminHost -DbPort $adminPort -User $adminUser -Db "postgres" -Password $adminPass -Sql "CREATE DATABASE $dbName OWNER $dbUser;" | Out-Null
} else {
    Invoke-Psql -Psql $psql -DbHost $adminHost -DbPort $adminPort -User $adminUser -Db "postgres" -Password $adminPass -Sql "ALTER DATABASE $dbName OWNER TO $dbUser;" | Out-Null
}

Write-Host "[2/5] Apply schema mode: $schemaMode"
switch ($schemaMode) {
    "reset" {
        Invoke-PsqlFile -Psql $psql -DbHost $adminHost -DbPort $adminPort -User $dbUser -Db $dbName -Password $dbPass -FilePath $schemaFile
    }
    "bootstrap" {
        $tableCount = (Invoke-Psql -Psql $psql -DbHost $adminHost -DbPort $adminPort -User $dbUser -Db $dbName -Password $dbPass -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE';" -Quiet).Trim()
        if ($tableCount -eq "0") {
            Invoke-PsqlFile -Psql $psql -DbHost $adminHost -DbPort $adminPort -User $dbUser -Db $dbName -Password $dbPass -FilePath $schemaFile
        } else {
            Write-Host "Skip schema apply, existing tables: $tableCount"
        }
    }
    "incremental" {
        Write-Host "Incremental mode on local: no migration runner in this script. Keep existing schema."
    }
    default {
        throw "Invalid DB_SCHEMA_MODE: $schemaMode (use reset/bootstrap/incremental)"
    }
}

Write-Host "[3/5] Build backend..."
Push-Location $backendDir
try {
    & mvn "-DskipTests" "clean" "package"
    if ($LASTEXITCODE -ne 0) { throw "Backend build failed." }
} finally {
    Pop-Location
}

Write-Host "[4/5] Build frontend..."
Push-Location $frontendDir
try {
    & npm "install"
    if ($LASTEXITCODE -ne 0) { throw "npm install failed." }
    & npm "run" "build"
    if ($LASTEXITCODE -ne 0) { throw "Frontend build failed." }
} finally {
    Pop-Location
}

if ($NoStart) {
    Write-Host "[5/5] Skip start because -NoStart was provided."
    Write-Host "Local prepare completed."
    exit 0
}

Write-Host "[5/5] Start backend + frontend..."
$backendJar = Get-ChildItem -Path (Join-Path $backendDir "target") -Filter "capics-backend-*.jar" | Where-Object { $_.Name -notlike "*.original" } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $backendJar) { throw "Cannot find backend jar in backend/target." }

$backendLog = Join-Path $logDir "backend.out.log"
$backendErrLog = Join-Path $logDir "backend.err.log"
$frontendLog = Join-Path $logDir "frontend.out.log"
$frontendErrLog = Join-Path $logDir "frontend.err.log"
$backendPidFile = Join-Path $pidDir "backend.pid"
$frontendPidFile = Join-Path $pidDir "frontend.pid"

$backendScriptPath = Join-Path $tmpDir "run-backend.ps1"
$frontendScriptPath = Join-Path $tmpDir "run-frontend.ps1"

$backendScript = @()
$backendScript += "`$env:SPRING_PROFILES_ACTIVE='" + (Escape-PsLiteral $cfg["SPRING_PROFILES_ACTIVE"]) + "'"
$backendScript += "`$env:SERVER_PORT='" + (Escape-PsLiteral $cfg["SERVER_PORT"]) + "'"
$backendScript += "`$env:DB_URL='" + (Escape-PsLiteral $cfg["DB_URL"]) + "'"
$backendScript += "`$env:DB_USERNAME='" + (Escape-PsLiteral $cfg["DB_USERNAME"]) + "'"
$backendScript += "`$env:DB_PASSWORD='" + (Escape-PsLiteral $cfg["DB_PASSWORD"]) + "'"
$backendScript += "`$env:JWT_SECRET='" + (Escape-PsLiteral $cfg["JWT_SECRET"]) + "'"
$backendScript += "`$env:JWT_EXPIRATION='" + (Escape-PsLiteral $cfg["JWT_EXPIRATION"]) + "'"
$backendScript += "`$env:APP_CORS_ALLOWED_ORIGINS='" + (Escape-PsLiteral $cfg["APP_CORS_ALLOWED_ORIGINS"]) + "'"
$backendScript += "`$env:LOG_LEVEL_APP='" + (Escape-PsLiteral $cfg["LOG_LEVEL_APP"]) + "'"
$backendScript += "`$env:LOG_LEVEL_SECURITY='" + (Escape-PsLiteral $cfg["LOG_LEVEL_SECURITY"]) + "'"
$backendScript += "java -jar '" + (Escape-PsLiteral $backendJar.FullName) + "'"

$frontendScript = @()
$frontendScript += "`$env:PORT='" + (Escape-PsLiteral $cfg["FRONTEND_PORT"]) + "'"
$frontendScript += "npm run dev -- --host 0.0.0.0 --port " + $cfg["FRONTEND_PORT"]

Set-Content -Path $backendScriptPath -Value $backendScript -Encoding UTF8
Set-Content -Path $frontendScriptPath -Value $frontendScript -Encoding UTF8

$backendProc = Start-Process -FilePath "powershell" -WorkingDirectory $backendDir -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $backendScriptPath) -RedirectStandardOutput $backendLog -RedirectStandardError $backendErrLog -PassThru
$frontendProc = Start-Process -FilePath "powershell" -WorkingDirectory $frontendDir -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $frontendScriptPath) -RedirectStandardOutput $frontendLog -RedirectStandardError $frontendErrLog -PassThru

Set-Content -Path $backendPidFile -Value $backendProc.Id
Set-Content -Path $frontendPidFile -Value $frontendProc.Id

Start-Sleep -Seconds 8
try {
    $health = Invoke-RestMethod -Uri ("http://127.0.0.1:{0}/api/health" -f $cfg["SERVER_PORT"]) -Method Get -TimeoutSec 10
    Write-Host "Backend health: $($health | ConvertTo-Json -Compress)"
} catch {
    Write-Warning "Backend health check failed. See $backendLog"
}

Write-Host "Local deploy completed."
Write-Host "Frontend: http://127.0.0.1:$($cfg["FRONTEND_PORT"])/login"
Write-Host "Backend : http://127.0.0.1:$($cfg["SERVER_PORT"])/api/health"
Write-Host "Logs    : $logDir"
