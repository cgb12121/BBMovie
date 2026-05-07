@echo off
setlocal
title Transcode Java Cluster Launcher

set "ROOT_DIR=%~dp0"

echo =========================================
echo  Transcode Java Cluster Launcher
echo  Root: %ROOT_DIR%
echo  Validation worker: vvs
echo  Quality worker: vqs
echo =========================================
echo.

call :launch "cas"
timeout /t 2 /nobreak >nul
call :launch "ves"
timeout /t 2 /nobreak >nul
call :launch "lgs"
timeout /t 2 /nobreak >nul
call :launch "vis"
timeout /t 2 /nobreak >nul
call :launch "vvs"
timeout /t 2 /nobreak >nul
call :launch "vqs"
timeout /t 2 /nobreak >nul
call :launch "temporal-orchestrator"

echo.
echo Cluster launch commands dispatched.
echo Use one terminal per service window.
exit /b 0

:launch
set "SERVICE=%~1"
if not exist "%ROOT_DIR%%SERVICE%\run.bat" (
    echo [WARN] Missing script: %SERVICE%\run.bat
    exit /b 0
)
echo [START] %SERVICE%
start "transcode-%SERVICE%" cmd /k "cd /d "%ROOT_DIR%%SERVICE%" && call run.bat"
exit /b 0
