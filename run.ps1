# =============================================================================
# NEXUS IMS - Local Build & Run Helper Script (PowerShell)
# Use this when 'mvn' is not globally configured in PATH.
# =============================================================================

$ErrorActionPreference = "Stop"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "   NEXUS IMS - Build & Execution Helper   " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. Verify Java Installation
try {
    $javaVersion = & java -version 2>&1 | Select-Object -First 1
    Write-Host "[✓] Java runtime detected: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "[✗] Java is not detected in PATH. Please install JDK 17+ or configure JAVA_HOME." -ForegroundColor Red
    exit 1
}

# 2. Check if local Maven wrapper exists
$wrapperCmd = Join-Path $PSScriptRoot "mvnw.cmd"
if (Test-Path $wrapperCmd) {
    Write-Host "[✓] Utilizing Maven Wrapper: $wrapperCmd" -ForegroundColor Green
    & $wrapperCmd clean compile
    & $wrapperCmd exec:java
    exit 0
}

# 3. Check common Maven installation paths on Windows
$commonPaths = @(
    "$env:LOCALAPPDATA\Programs\Common\Apache-Maven\bin\mvn.cmd",
    "C:\Program Files\Apache\maven\bin\mvn.cmd",
    "C:\Program Files\apache-maven-*\bin\mvn.cmd",
    "C:\apache-maven-*\bin\mvn.cmd",
    "C:\ProgramData\chocolatey\bin\mvn.cmd",
    "$env:USERPROFILE\scoop\apps\maven\current\bin\mvn.cmd"
)

$discoveredMvn = $null
foreach ($pathPattern in $commonPaths) {
    $matches = Resolve-Path $pathPattern -ErrorAction SilentlyContinue
    if ($matches) {
        $discoveredMvn = $matches[0].Path
        break
    }
}

if ($discoveredMvn) {
    Write-Host "[✓] Found local Maven installation at: $discoveredMvn" -ForegroundColor Green
    & $discoveredMvn clean compile
    & $discoveredMvn exec:java
    exit 0
}

# 4. Fallback: Prompt user with automated installation command
Write-Host "`n[!] Maven ('mvn') is not yet installed or not registered in your Windows PATH." -ForegroundColor Yellow
Write-Host "    You can install it instantly via Windows Package Manager by running:" -ForegroundColor White
Write-Host "    winget install Apache.Maven" -ForegroundColor Cyan
Write-Host "`n    Or run directly from VS Code by pressing F5 or clicking 'Run' above main() in Main.java." -ForegroundColor White

