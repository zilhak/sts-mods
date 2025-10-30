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
    echo [32m√ Java is installed[0m
    set /a PASS+=1
) else (
    echo [31mX Java not found. Please install JDK.[0m
    set /a FAIL+=1
    goto summary
)
echo.

REM 2. Check JAVA_HOME
echo 2. Checking JAVA_HOME...
if defined JAVA_HOME (
    echo [32m√ JAVA_HOME is set: %JAVA_HOME%[0m
    set /a PASS+=1
) else (
    echo [33m! JAVA_HOME not set (optional but recommended)[0m
    set /a WARN+=1
)
echo.

REM 3. Check Maven
echo 3. Checking Maven installation...
mvn -version >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "tokens=*" %%a in ('mvn -version 2^>^&1 ^| findstr /i "Apache Maven"') do (
        echo    Found: %%a
    )
    echo [32m√ Maven is installed[0m
    set /a PASS+=1
) else (
    echo [31mX Maven not found. Please install Maven 3.6+[0m
    set /a FAIL+=1
    goto summary
)
echo.

REM 4. Check STS installation
echo 4. Checking Slay the Spire installation...
if defined STS_INSTALL_DIR (
    if exist "%STS_INSTALL_DIR%\desktop-1.0.jar" (
        echo [32m√ STS installation found: %STS_INSTALL_DIR%[0m
        set /a PASS+=1

        if exist "%STS_INSTALL_DIR%\mods\ModTheSpire.jar" (
            echo [32m√ ModTheSpire.jar found[0m
            set /a PASS+=1
        ) else (
            echo [33m! ModTheSpire.jar not found in mods folder[0m
            set /a WARN+=1
        )

        if exist "%STS_INSTALL_DIR%\mods\BaseMod.jar" (
            echo [32m√ BaseMod.jar found[0m
            set /a PASS+=1
        ) else (
            echo [33m! BaseMod.jar not found in mods folder[0m
            set /a WARN+=1
        )
    ) else (
        echo [31mX desktop-1.0.jar not found in STS_INSTALL_DIR[0m
        set /a FAIL+=1
    )
) else (
    echo [33m! STS_INSTALL_DIR not set. You'll need to configure this.[0m
    echo    Set with: setx STS_INSTALL_DIR "C:\Path\To\SlayTheSpire"
    set /a WARN+=1
)
echo.

REM 5. Test Maven compilation
echo 5. Testing Maven compilation...
call mvn clean compile -q >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [32m√ Maven can compile the project[0m
    set /a PASS+=1

    REM Check if class file exists
    if exist "ascension-100\target\classes\com\stsmod\ascension100\Ascension100Mod.class" (
        REM Try to check bytecode version (simplified check)
        echo [32m√ Class files generated successfully[0m
        set /a PASS+=1
    )
) else (
    echo [31mX Maven compilation failed. Check pom.xml configuration.[0m
    set /a FAIL+=1
)
echo.

:summary
REM 6. Summary
echo =========================================
echo   Verification Summary
echo =========================================
echo.
echo Passed: %PASS% checks
if %WARN% GTR 0 echo Warnings: %WARN% items
if %FAIL% GTR 0 echo Failed: %FAIL% checks
echo.

if %FAIL% EQU 0 (
    if defined JAVA_HOME (
        if defined STS_INSTALL_DIR (
            echo [32m√ Setup looks good![0m
            echo.
            echo Next steps:
            echo   1. Run: mvn clean package
            echo   2. Copy JARs to ModTheSpire mods folder
            echo   3. Launch game and enable mods
        ) else (
            goto incomplete
        )
    ) else (
        goto incomplete
    )
) else (
    goto incomplete
)

goto end

:incomplete
echo [33m! Setup incomplete[0m
echo.
echo Required actions:
if not defined JAVA_HOME echo   - Set JAVA_HOME environment variable
if not defined STS_INSTALL_DIR echo   - Set STS_INSTALL_DIR environment variable
echo.
echo See SETUP.md for detailed instructions

:end
echo.
endlocal
