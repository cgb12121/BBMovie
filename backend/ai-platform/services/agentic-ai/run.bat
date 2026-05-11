@echo off
setlocal enabledelayedexpansion

:: 1. Đọc file .env và set biến môi trường
echo [1/2] Loading .env variables...
if exist .env (
    for /f "usebackq tokens=*" %%A in (".env") do (
        set "line=%%A"
        :: Bỏ qua dòng comment # và dòng trống
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
echo [2/2] Building and Running Agentic AI...
mvn spring-boot:run
    -DskipTests ^
    -Dspring-boot.run.jvmArguments="-Xmx512m -Dspring.output.ansi.enabled=ALWAYS" ^
    -Dspring-boot.run.arguments="ai info" ^
    -Dpicocli.ansi=always
endlocal