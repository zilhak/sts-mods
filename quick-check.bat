@echo off
echo === STS Mods Quick Check ===
echo.

echo 1. Java Version:
java -version
echo.

echo 2. JAVA_HOME:
echo %JAVA_HOME%
echo.

echo 3. Maven Version:
mvn -version
echo.

echo 4. STS Install Directory:
echo %STS_INSTALL_DIR%
echo.

echo === Test Build ===
cd /d E:\workspace\sts-mods
mvn clean compile -q
if %ERRORLEVEL% EQU 0 (
    echo Build SUCCESS!
) else (
    echo Build FAILED!
)
echo.

pause
