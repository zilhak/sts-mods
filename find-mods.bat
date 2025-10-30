@echo off
echo ======================================
echo   Finding Steam Workshop Mods
echo ======================================
echo.

set "WORKSHOP_DIR=C:\Program Files (x86)\Steam\steamapps\workshop\content\646570"

if not exist "%WORKSHOP_DIR%" (
    echo Workshop folder not found at default location.
    echo Please check your Steam installation directory.
    echo.
    pause
    exit /b 1
)

echo Searching for mods in:
echo %WORKSHOP_DIR%
echo.

echo Looking for ModTheSpire.jar...
for /r "%WORKSHOP_DIR%" %%f in (ModTheSpire.jar) do (
    echo Found: %%f
    set "MODTHESPIRE_PATH=%%f"
)
echo.

echo Looking for BaseMod.jar...
for /r "%WORKSHOP_DIR%" %%f in (BaseMod.jar) do (
    echo Found: %%f
    set "BASEMOD_PATH=%%f"
)
echo.

echo Looking for StSLib.jar...
for /r "%WORKSHOP_DIR%" %%f in (StSLib.jar) do (
    echo Found: %%f
    set "STSLIB_PATH=%%f"
)
echo.

echo ======================================
echo   Copy these paths to build.gradle
echo ======================================
pause
