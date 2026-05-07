@echo off
setlocal enabledelayedexpansion

echo [CAS] Loading environment variables...
call :load_env ".env"
call :load_env "..\.env"

echo [CAS] Starting service...
mvn spring-boot:run -DskipTests
exit /b %errorlevel%

:load_env
set "ENV_FILE=%~1"
if not exist "%ENV_FILE%" exit /b 0
for /f "usebackq tokens=*" %%A in ("%ENV_FILE%") do (
    set "line=%%A"
    if not "!line!"=="" if not "!line:~0,1!"=="#" set "%%A"
)
exit /b 0
