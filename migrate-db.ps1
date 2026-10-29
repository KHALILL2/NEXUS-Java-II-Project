# =============================================================================
# NEXUS IMS - Database Migration Runner
# Applies schema.sql and data.sql to MySQL
# =============================================================================

$ErrorActionPreference = "Stop"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "   NEXUS IMS - Database Migration        " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. Search for mysql.exe on system
$mysqlCmd = Get-Command mysql -ErrorAction SilentlyContinue
$mysqlPath = if ($mysqlCmd) { $mysqlCmd.Source } else { $null }

if (-not $mysqlPath) {
    $searchPatterns = @(
        "C:\Program Files\MySQL\MySQL Server *\bin\mysql.exe",
        "C:\Program Files (x86)\MySQL\MySQL Server *\bin\mysql.exe",
        "C:\xampp\mysql\bin\mysql.exe",
        "C:\wamp64\bin\mysql\mysql*\bin\mysql.exe",
        "C:\laragon\bin\mysql\mysql*\bin\mysql.exe"
    )

    foreach ($pattern in $searchPatterns) {
        $found = Resolve-Path $pattern -ErrorAction SilentlyContinue
        if ($found) {
            $mysqlPath = $found[0].Path
            $binFolder = Split-Path $mysqlPath -Parent
            
            # Permanently add MySQL bin to User PATH
            $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
            if ($userPath -notlike "*$binFolder*") {
                Write-Host "[*] Registering $binFolder in Windows PATH..." -ForegroundColor Yellow
                [Environment]::SetEnvironmentVariable("Path", "$userPath;$binFolder", "User")
                $env:Path += ";$binFolder"
            }
            break
        }
    }
}

if ($mysqlPath) {
    Write-Host "[✓] Found MySQL CLI at: $mysqlPath" -ForegroundColor Green
    Write-Host "[*] Applying schema.sql..." -ForegroundColor Cyan
    Get-Content "src\main\resources\database\schema.sql" | & $mysqlPath -u root -p
    Write-Host "[*] Applying data.sql..." -ForegroundColor Cyan
    Get-Content "src\main\resources\database\data.sql" | & $mysqlPath -u root -p nexus_ims_db
    Write-Host "[SUCCESS] Database initialized successfully!" -ForegroundColor Green
    exit 0
}

# 2. If mysql.exe not found on disk, execute via Java Migrator (zero external dependency)
Write-Host "[*] 'mysql.exe' CLI not found on disk. Executing via Java JDBC Migrator..." -ForegroundColor Yellow
mvn clean compile
mvn exec:java -Dexec.mainClass="com.nexus.ims.util.DatabaseMigrator"

