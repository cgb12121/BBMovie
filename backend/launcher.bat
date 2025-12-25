@echo off
setlocal enabledelayedexpansion
title BBMovie Master Launcher V5 (Delegated)

:: ==========================================
:: 1. CONFIGURATION
:: ==========================================
:: Định nghĩa service (Format: Tên_Folder|Loại_Tech)
set "services[1]=eureka-sever|SPRING"
set "services[2]=gateway|SPRING"
set "services[3]=auth-service|SPRING"
set "services[4]=file-service|SPRING"
set "services[5]=ai-service|SPRING"
set "services[6]=rust-ai-context-refinery|RUST"
set "services[7]=watchlist-quarkus|QUARKUS"
set "services[8]=payment-service|SPRING"
set "services[9]=search-service|SPRING"
set "services[10]=email-service|SPRING"
set "services[11]=media-streaming-service|SPRING"
set "services[12]=media-upload-service|SPRING"
set "services[13]=transcode-worker|SPRING"
set "services[14]=rust-transcode-worker|RUST"
set "services[15]=media-service|SPRING"

:: Màu sắc
set GREEN=[92m
set RED=[91m
set YELLOW=[93m
set CYAN=[96m
set WHITE=[97m
set RESET=[0m

:: ==========================================
:: 2. LOAD GLOBAL ENV (Optional)
:: ==========================================
:: Bác vẫn có thể load .env ở đây để share chung DB_URL, JWT_SECRET...
:: Các file run.bat con sẽ kế thừa được biến này (nếu chạy cùng session)
echo %YELLOW%[SYSTEM] Loading global .env variables...%RESET%
if exist .env (
    for /f "usebackq tokens=*" %%A in (".env") do (
        set "line=%%A"
        if not "!line:~0,1!"=="#" (
            if not "!line!"=="" set "%%A"
        )
    )
    echo %GREEN%OK: Global .env loaded!%RESET%
)

:MAIN_MENU
cls
echo.
echo %CYAN%=============================================================%RESET%
echo %CYAN%   BBMovie Master Launcher V5 (Delegated Mode)              %RESET%
echo %CYAN%=============================================================%RESET%
echo.
echo   [0]  %RED%KILL ALL (Clean up)%RESET%
echo   [A]  %GREEN%Start ALL Services%RESET%
echo.
echo   %YELLOW%--- Select Specific Services ---%RESET%
echo.

:: --- (Đoạn vẽ bảng giữ nguyên như cũ) ---
:: Table header
echo   +----+--------------------------------+-----------+
echo   ^| ID ^| Service Name                   ^| Stack     ^|
echo   +----+--------------------------------+-----------+

set "count=0"
for /L %%i in (1,1,20) do (
    call set "check_svc=%%services[%%i]%%"
    if not "!check_svc!"=="" (
        for /f "tokens=1,2 delims=|" %%a in ("!check_svc!") do (
            set "svc_name=%%a"
            set "svc_type=%%b"
            set "idx=  %%i"
            set "idx=!idx:~-2!"
            set "padded_name=%%a                              "
            set "padded_name=!padded_name:~0,30!"
            set "padded_type=%%b         "
            set "padded_type=!padded_type:~0,9!"
            set "COLOR=%WHITE%"
            if /i "%%b"=="SPRING" set "COLOR=%GREEN%"
            if /i "%%b"=="RUST"   set "COLOR=%YELLOW%"
            if /i "%%b"=="QUARKUS" set "COLOR=%CYAN%"
            echo   ^| !idx! ^| !padded_name! ^| !COLOR!!padded_type!%RESET% ^|
            set "count=%%i"
        )
    )
)
echo   +----+--------------------------------+-----------+
:: ----------------------------------------

echo.
echo %CYAN%Example inputs:%RESET% "1 2 5" or "A"
echo.

set "choices="
set /p choices="Your choice: "

if "%choices%"=="" goto MAIN_MENU
if /i "%choices%"=="0" goto KILL_ALL
if /i "%choices%"=="A" goto START_ALL

echo.
for %%j in (%choices%) do (
    call :PROCESS_SELECTION "%%j"
)

echo.
echo %GREEN%Services launched via delegated scripts.%RESET%
echo Returning to Main Menu in 3 seconds...
timeout /t 3 >nul 2>&1
goto MAIN_MENU

:: ==========================================
:: 3. LOGIC HANDLERS
:: ==========================================

:PROCESS_SELECTION
set "id=%~1"
if "%id%"=="" exit /b
call set "svc_data=%%services[%id%]%%"
if "%svc_data%"=="" (
    echo %RED%Skipping invalid ID: [%id%]%RESET%
    exit /b
)
for /f "tokens=1,2 delims=|" %%a in ("!svc_data!") do (
    call :LAUNCHER "%%a"
)
exit /b

:START_ALL
:: Chạy Eureka trước
call :LAUNCHER "eureka-sever"
timeout /t 10 /nobreak >nul
for /L %%i in (2,1,%count%) do (
    call set "svc_data=%%services[%%i]%%"
    if not "!svc_data!"=="" (
        for /f "tokens=1,2 delims=|" %%a in ("!svc_data!") do (
            call :LAUNCHER "%%a"
        )
    )
)
echo.
echo %GREEN%All services started.%RESET%
timeout /t 3 >nul 2>&1
goto MAIN_MENU

:LAUNCHER
set "folder=%~1"

:: Check xem file run.bat có tồn tại không
if not exist "%folder%\run.bat" (
    echo %RED%Error: %folder%\run.bat not found! Skipping...%RESET%
    exit /b
)

echo %YELLOW%Delegating to %folder%\run.bat...%RESET%

:: 🔥 CORE CHANGE: Gọi run.bat của từng service
:: cmd /k "..." : Mở cửa sổ mới và giữ nó lại
:: cd /d "%folder%" : Nhảy vào thư mục con
:: call run.bat : Chạy script con
start "BBMovie - %folder%" cmd /k "title %folder% && cd /d "%folder%" && call run.bat"

exit /b

:KILL_ALL
echo %RED%Killing all java, cargo, rust processes...%RESET%
taskkill /F /IM java.exe 2>nul
taskkill /F /IM cargo.exe 2>nul
taskkill /F /IM ai-refinery.exe 2>nul
taskkill /F /IM transcode-worker.exe 2>nul
taskkill /F /IM createdump.exe 2>nul
echo %GREEN%Cleaned up.%RESET%
timeout /t 2 >nul
goto MAIN_MENU