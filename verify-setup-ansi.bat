@echo off
setlocal enabledelayedexpansion

REM Setup Verification Script for Slay the Spire Mods (Windows)
REM Checks JDK version, Maven, and bytecode compatibility

echo =========================================
echo   STS Mods Setup Verification
echo =========================================
echo.

set PASS=0
set FAIL=0
set WARN=0

REM 1. Check Java version
echo 1. Checking Java installation...
java -version >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        set JAVA_VERSION=%%g
    )
    echo    Found: !JAVA_VERSION!
    echo [32miconv: illegal input sequence at position 613
