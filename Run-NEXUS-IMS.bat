@echo off
setlocal
cd /d "%~dp0"

set "JAR_PATH=target\nexus-ims-1.0.0-shaded.jar"

if not exist "%JAR_PATH%" (
    echo [NEXUS IMS] Shaded executable JAR not found: "%JAR_PATH%"
    echo Please build it first by running:
    echo     .\mvnw.cmd clean package
    echo.
    pause
    exit /b 1
)

start "" javaw -jar "%JAR_PATH%"
endlocal
