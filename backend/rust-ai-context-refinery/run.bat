@echo off
setlocal enabledelayedexpansion
title Rust Worker (Auto-Detect MSVC)

:: ==========================================
:: 1. SETUP ENV CHO RUST
:: ==========================================
:: Nếu đã cài LLVM thì giữ nguyên
set LIBCLANG_PATH=C:\Program Files\LLVM\bin
set RUST_LOG=info

echo [1/2] Loading .env variables...
if exist .env (
    for /f "usebackq tokens=*" %%A in (".env") do (
        set "line=%%A"
        :: Bỏ qua dòng comment # và dòng trống
        if not "!line:~0,1!"=="#" (
            if not "!line!"=="" (
                set "%%A"
                echo Loaded: %%A
            )
        )
    )
) else (
    echo .env file not found! Skipping...
)

:: Biến môi trường "bóp họng" Whisper để tránh treo (Deadlock)
set OMP_NUM_THREADS=1
set MKL_NUM_THREADS=1

:: ==========================================
:: 2. DÙNG vswhere ĐỂ TÌM VISUAL STUDIO (Chuẩn 100%)
:: ==========================================
echo [RUST] Auto-detecting Visual Studio C++ installation...

set "vswhere=%ProgramFiles(x86)%\Microsoft Visual Studio\Installer\vswhere.exe"

:: Kiểm tra vswhere có tồn tại không
if not exist "%vswhere%" (
    echo [ERROR] Visual Studio Installer not found!
    echo [HINT] Please install "Visual Studio Build Tools" via Winget or Microsoft website.
    pause
    exit /b 1
)

:: Hỏi vswhere đường dẫn cài đặt của gói C++ (VC.Tools.x86.x64)
set "vs_path="
for /f "usebackq tokens=*" %%i in (`"%vswhere%" -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath`) do (
    set "vs_path=%%i"
)

if "%vs_path%"=="" (
    echo [ERROR] C++ Build Tools NOT FOUND!
    echo [HINT] Open Visual Studio Installer -> Modify -> Check "Desktop development with C++".
    pause
    exit /b 1
)

echo [RUST] Found Visual Studio at: "%vs_path%"

:: Đường dẫn file kích hoạt môi trường
set "vcvars=%vs_path%\VC\Auxiliary\Build\vcvars64.bat"

if not exist "%vcvars%" (
    echo [ERROR] Found VS path but 'vcvars64.bat' is missing!
    echo [CHECK] Looked in: "%vcvars%"
    pause
    exit /b 1
)

:: ==========================================
:: 3. KÍCH HOẠT VÀ CHẠY
:: ==========================================
echo [RUST] Activating x64 environment...
call "%vcvars%" >nul

echo.
echo [RUST] Environment Ready! Building and Running...
cargo run --release

:: Giữ màn hình nếu crash
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Rust crashed!
    pause
)