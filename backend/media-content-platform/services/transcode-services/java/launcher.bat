@echo off
setlocal enabledelayedexpansion
title Transcode Java Launcher

set "ROOT_DIR=%~dp0"

:MENU
cls
echo =========================================
echo   Transcode Java Launcher
echo   Root: %ROOT_DIR%
echo =========================================
echo.
echo   [1] Start full cluster (CAS + VES + LGS + VIS + VVS + VQS + Orchestrator)
echo   [2] Start single service
echo   [0] Exit
echo.
set /p CHOICE="Choose option: "

if "%CHOICE%"=="1" goto START_CLUSTER
if "%CHOICE%"=="2" goto START_SINGLE
if "%CHOICE%"=="0" goto END
goto MENU

:START_CLUSTER
echo.
echo Launching full transcode cluster...
call "%ROOT_DIR%run-cluster.bat"
echo.
pause
goto MENU

:START_SINGLE
cls
echo =========================================
echo   Start Single Service
echo =========================================
echo.
echo   [1] cas
echo   [2] ves
echo   [3] lgs
echo   [4] vis
echo   [5] vvs
echo   [6] vqs
echo   [7] temporal-orchestrator
echo   [0] Back
echo.
set /p SERVICE_CHOICE="Choose service: "

if "%SERVICE_CHOICE%"=="1" set "SERVICE=cas"
if "%SERVICE_CHOICE%"=="2" set "SERVICE=ves"
if "%SERVICE_CHOICE%"=="3" set "SERVICE=lgs"
if "%SERVICE_CHOICE%"=="4" set "SERVICE=vis"
if "%SERVICE_CHOICE%"=="5" set "SERVICE=vvs"
if "%SERVICE_CHOICE%"=="6" set "SERVICE=vqs"
if "%SERVICE_CHOICE%"=="7" set "SERVICE=temporal-orchestrator"
if "%SERVICE_CHOICE%"=="0" goto MENU

if not defined SERVICE (
    echo Invalid selection.
    timeout /t 1 >nul
    goto START_SINGLE
)

call :LAUNCH_SERVICE "%SERVICE%"
set "SERVICE="
echo.
pause
goto MENU

:LAUNCH_SERVICE
set "TARGET=%~1"
if not exist "%ROOT_DIR%%TARGET%\run.bat" (
    echo [WARN] Missing script: %TARGET%\run.bat
    exit /b 0
)
echo Starting %TARGET%...
start "transcode-%TARGET%" cmd /k "cd /d "%ROOT_DIR%%TARGET%" && call run.bat"
exit /b 0

:END
echo Bye.
endlocal
