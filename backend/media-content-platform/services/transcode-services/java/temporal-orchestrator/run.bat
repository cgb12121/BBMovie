@echo off
setlocal enabledelayedexpansion

echo [ORCHESTRATOR] Loading environment variables...
call :load_env ".env"
call :load_env "..\.env"

set "NATS_BRIDGE_ENABLED=true"

echo [ORCHESTRATOR] Starting service...
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
