@echo off
setlocal enabledelayedexpansion
title MSVC Environment Dumper

echo [1/3] Detecting Visual Studio...
set "vswhere=%ProgramFiles(x86)%\Microsoft Visual Studio\Installer\vswhere.exe"

if not exist "%vswhere%" (
    echo [ERROR] Visual Studio Installer not found!
    pause
    exit /b 1
)

for /f "usebackq tokens=*" %%i in (`"%vswhere%" -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath`) do (
    set "vs_path=%%i"
)

if "%vs_path%"=="" (
    echo [ERROR] C++ Build Tools NOT FOUND!
    pause
    exit /b 1
)

set "vcvars=%vs_path%\VC\Auxiliary\Build\vcvars64.bat"
echo [2/3] Activating MSVC Environment...
call "%vcvars%" >nul

echo [3/3] Exporting variables to 'msvc_env.txt'...

(
    echo ===============================================================================
    echo 1. PATH (Add this to the BEGINNING of your PATH in RustRover)
    echo ===============================================================================
    echo %vs_path%\VC\Tools\MSVC\%VCToolsVersion%\bin\Hostx64\x64
    echo.
    echo ===============================================================================
    echo 2. INCLUDE (Copy full string)
    echo ===============================================================================
    echo %INCLUDE%
    echo.
    echo ===============================================================================
    echo 3. LIB (Copy full string)
    echo ===============================================================================
    echo %LIB%
) > msvc_env.txt

echo.
echo [DONE] Variables saved to: msvc_env.txt
echo Open that file and copy-paste into RustRover!
pause