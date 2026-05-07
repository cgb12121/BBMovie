@echo off
setlocal

echo [CONTRACTS] Building transcode-contracts once...
mvn -pl transcode-contracts -am install -DskipTests
exit /b %errorlevel%
