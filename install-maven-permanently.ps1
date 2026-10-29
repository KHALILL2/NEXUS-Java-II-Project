# =============================================================================
# NEXUS IMS - Permanent Apache Maven Installer for Windows
# Run this script once in PowerShell to permanently configure 'mvn' on your PC.
# =============================================================================

$ErrorActionPreference = "Stop"

Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "   Permanent Apache Maven Installation & Setup        " -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan

# Configuration
$mavenVersion = "3.9.9"
$targetDir = "C:\apache-maven"
$zipUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
$tempZip = "$env:TEMP\apache-maven-$mavenVersion-bin.zip"

# Step 1: Download Official Apache Maven
Write-Host "`n[1/4] Downloading Apache Maven $mavenVersion from official archive..." -ForegroundColor Yellow
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Invoke-WebRequest -Uri $zipUrl -OutFile $tempZip -UseBasicParsing

# Step 2: Extract to permanent directory
Write-Host "[2/4] Extracting to $targetDir..." -ForegroundColor Yellow
if (Test-Path $targetDir) {
    Remove-Item -Path $targetDir -Recurse -Force
}
Expand-Archive -Path $tempZip -DestinationPath "$env:TEMP\mvn-extract" -Force
$extractedSubfolder = Get-ChildItem "$env:TEMP\mvn-extract" | Select-Object -First 1
Move-Item -Path $extractedSubfolder.FullName -Destination $targetDir -Force

# Cleanup temp files
Remove-Item $tempZip -Force -ErrorAction SilentlyContinue
Remove-Item "$env:TEMP\mvn-extract" -Recurse -Force -ErrorAction SilentlyContinue

# Step 3: Configure Permanent Environment Variables (User-Level: No Admin needed)
Write-Host "[3/4] Registering permanent environment variables in Windows Registry..." -ForegroundColor Yellow

# Set MAVEN_HOME
[Environment]::SetEnvironmentVariable("MAVEN_HOME", $targetDir, [EnvironmentVariableTarget]::User)
[Environment]::SetEnvironmentVariable("M2_HOME", $targetDir, [EnvironmentVariableTarget]::User)

# Append to User PATH permanently if not present
$currentPath = [Environment]::GetEnvironmentVariable("Path", [EnvironmentVariableTarget]::User)
$binPath = "$targetDir\bin"
if ($currentPath -split ';' -notcontains $binPath) {
    $newPath = if ([string]::IsNullOrWhiteSpace($currentPath)) { $binPath } else { "$currentPath;$binPath" }
    [Environment]::SetEnvironmentVariable("Path", $newPath, [EnvironmentVariableTarget]::User)
}

# Update the active session immediately
$env:MAVEN_HOME = $targetDir
$env:M2_HOME = $targetDir
$env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [Environment]::GetEnvironmentVariable("Path", "User")

# Step 4: Verification
Write-Host "[4/4] Verifying installation..." -ForegroundColor Green
Write-Host "------------------------------------------------------" -ForegroundColor DarkGray
& "$binPath\mvn.cmd" -version
Write-Host "------------------------------------------------------" -ForegroundColor DarkGray
Write-Host "[SUCCESS] Apache Maven is now PERMANENTLY installed at $targetDir." -ForegroundColor Green
Write-Host "The 'mvn' command will now work in any terminal, VS Code, or future reboot." -ForegroundColor Green

