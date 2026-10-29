@REM =========================================================================
@REM NEXUS IMS - Maven Lightweight Wrapper for Windows
@REM Auto-downloads portable Apache Maven if not installed and delegates commands
@REM =========================================================================

@echo off
setlocal

set "MAVEN_VERSION=3.9.9"
set "WRAPPER_DIR=%~dp0.mvn\wrapper"
set "MAVEN_HOME=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_EXEC=%MAVEN_HOME%\bin\mvn.cmd"

rem 1. Check if global mvn exists
where mvn >nul 2>nul
if %ERRORLEVEL% equ 0 (
    mvn %*
    exit /b %ERRORLEVEL%
)

rem 2. Check if local portable maven is already downloaded
if exist "%MAVEN_EXEC%" (
    call "%MAVEN_EXEC%" %*
    exit /b %ERRORLEVEL%
)

rem 3. Download and extract portable Apache Maven
echo =====================================================================
echo  Maven not detected on system. Bootstrapping Apache Maven %MAVEN_VERSION%...
echo =====================================================================

if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
set "ZIP_FILE=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip"
set "DOWNLOAD_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip"

echo Downloading %DOWNLOAD_URL% ...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; " ^
    "Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%ZIP_FILE%'"

if not exist "%ZIP_FILE%" (
    echo [ERROR] Failed to download Maven binary zip. Please verify internet connection.
    exit /b 1
)

echo Extracting Maven into %WRAPPER_DIR%...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "Expand-Archive -Path '%ZIP_FILE%' -DestinationPath '%WRAPPER_DIR%' -Force"

if exist "%ZIP_FILE%" del "%ZIP_FILE%"

if not exist "%MAVEN_EXEC%" (
    echo [ERROR] Failed to extract Maven executable at %MAVEN_EXEC%
    exit /b 1
)

echo [SUCCESS] Maven %MAVEN_VERSION% configured successfully!
echo Running: %MAVEN_EXEC% %*
echo.

call "%MAVEN_EXEC%" %*
exit /b %ERRORLEVEL%

