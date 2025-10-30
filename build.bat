@echo off
REM Slay the Spire Mods Build Script for Windows (Gradle)
REM Usage: build.bat [all|ascension|relics]

setlocal enabledelayedexpansion

echo ======================================
echo   Slay the Spire Mods Build Script
echo   (Using Gradle)
echo ======================================
echo.

REM Check if Gradle wrapper exists, otherwise use system gradle
if exist "gradlew.bat" (
    set GRADLE_CMD=gradlew.bat
) else (
    set GRADLE_CMD=gradle
)

set "BUILD_TARGET=%~1"
if "%BUILD_TARGET%"=="" set "BUILD_TARGET=all"

goto :%BUILD_TARGET%

:all
    echo Building all modules...
    echo.
    call %GRADLE_CMD% clean build -q
    if %ERRORLEVEL% EQU 0 (
        echo [32m√ All modules built successfully[0m
        echo   Ascension 100: ascension-100\build\libs\Ascension100.jar
        echo   Custom Relics: custom-relics\build\libs\CustomRelics.jar
    ) else (
        echo [31mX Build failed[0m
        exit /b 1
    )
    goto :end

:ascension
    call :build_module "ascension-100" "Ascension 100"
    goto :end

:relics
    call :build_module "custom-relics" "Custom Relics"
    goto :end

:build_module
    echo Building %~2...
    call %GRADLE_CMD% :%~1:clean :%~1:build -q
    if %ERRORLEVEL% EQU 0 (
        echo [32m√ %~2 build successful[0m
        echo   Output: %~1\build\libs\*.jar
    ) else (
        echo [31mX %~2 build failed[0m
        exit /b 1
    )
    echo.
    goto :eof

:help
    echo Usage: build.bat [all^|ascension^|relics]
    echo.
    echo Options:
    echo   all       - Build all modules (default)
    echo   ascension - Build Ascension 100 only
    echo   relics    - Build Custom Relics only
    goto :end

:end
    echo.
    echo ======================================
    echo   Build Complete!
    echo ======================================
    endlocal
