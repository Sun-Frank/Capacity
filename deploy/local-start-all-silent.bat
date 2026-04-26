@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "VBS_SCRIPT=%SCRIPT_DIR%local-start-all-silent.vbs"

if not exist "%VBS_SCRIPT%" (
  echo [ERROR] File not found: "%VBS_SCRIPT%"
  exit /b 1
)

wscript //nologo "%VBS_SCRIPT%"
exit /b 0

