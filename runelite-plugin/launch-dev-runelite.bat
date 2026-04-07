@echo off
setlocal EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "PS_SCRIPT=%SCRIPT_DIR%launch-dev-runelite.ps1"
set "BUILD_ARG=-SkipBuild"
set "DEBUG_ARG="
set "FORWARD_ARGS="

for %%A in (%*) do (
  if /I "%%~A"=="build" (
    set "BUILD_ARG="
  ) else if /I "%%~A"=="debug" (
    set "DEBUG_ARG=-DebugLogs"
  ) else (
    set "FORWARD_ARGS=!FORWARD_ARGS! %%~A"
  )
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%PS_SCRIPT%" %BUILD_ARG% %DEBUG_ARG% !FORWARD_ARGS!
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
  echo.
  echo Launch failed with exit code %EXIT_CODE%.
  pause
)

exit /b %EXIT_CODE%
