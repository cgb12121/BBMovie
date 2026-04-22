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

:: 2. Chạy Maven với cờ tối ưu cho máy yếu
echo.
echo [2/2] Building and Running Eureka-Server...
mvn spring-boot:run -DskipTests -Dspring-boot.run.jvmArguments="-Xmx512m -Dspring.output.ansi.enabled=ALWAYS"
endlocal