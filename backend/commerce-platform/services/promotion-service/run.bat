@echo off
setlocal enabledelayedexpansion

echo [1/2] Loading .env variables...
if exist .env (
    for /f "usebackq tokens=*" %%A in (".env") do (
        set "line=%%A"
        if not "!line:~0,1!"=="#" (
            if not "!line!"=="" (
                set "%%A"
                for /f "tokens=1 delims==" %%i in ("!line!") do echo Loaded: %%i
            )
        )
    )
) else (
    echo .env file not found! Skipping...
)

echo.
echo [2/2] Building and Running Promotion-Service...
mvn spring-boot:run -DskipTests -Dspring-boot.run.jvmArguments="-Xmx512m -Dspring.output.ansi.enabled=ALWAYS"
endlocal
