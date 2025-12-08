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
                echo Loaded: %%A
            )
        )
    )
) else (
    echo .env file not found! Skipping...
)

echo Debug: DB_URL is !DB_URL!
:: 2. Chạy Maven với cờ tối ưu cho máy yếu
echo.
echo [2/2] Building and Running Ai-Service...
:: -o: Offline mode (đỡ check update trên mạng, chạy nhanh hơn)
:: -DskipTests: Bỏ qua test cho nhẹ
:: -pl: Tên module service (SỬA LẠI TÊN NÀY CHO ĐÚNG)
:: -am: Tự động build module common
:: mvn -o -pl ai-service -am spring-boot:run -DskipTests -Dspring-boot.run.jvmArguments="-Xmx512m": dùng ở root project để tự build common
:: QUAN TRỌNG: Truyền biến môi trường vào thành System Property cho Maven Plugin
:: Maven Plugin (jOOQ) sẽ đọc được qua ${DB_URL} thay vì ${env.DB_URL} (cần sửa pom.xml một xíu)
mvn spring-boot:run
    -DskipTests ^
    -Dspring-boot.run.jvmArguments="-Xmx512m -Dspring.output.ansi.enabled=ALWAYS" ^
    -Ddb.url="!DB_URL!" ^
    -Ddb.username="!DB_USERNAME!" ^
    -Ddb.password="!DB_PASSWORD!" ^
    -Ddb.driver="!DB_DRIVER!"
endlocal