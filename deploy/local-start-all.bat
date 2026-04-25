@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "PS_SCRIPT=%SCRIPT_DIR%local-start-all.ps1"

if not exist "%PS_SCRIPT%" (
  echo [ERROR] File not found: "%PS_SCRIPT%"
  exit /b 1
)

echo Starting frontend and backend...
powershell -NoProfile -ExecutionPolicy Bypass -File "%PS_SCRIPT%" %*

set "CODE=%ERRORLEVEL%"
if not "%CODE%"=="0" (
  echo.
  echo [ERROR] Startup failed. Exit code: %CODE%
  exit /b %CODE%
)

echo.
echo Done.
exit /b 0
