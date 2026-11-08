# Usage:  .\restore-db.ps1                          (restores the NEWEST backup into the DB in .env)
#         .\restore-db.ps1 -File itsm_2026-11-08_10-00-00.dump
#         .\restore-db.ps1 -EnvFile .env.local
param([string]$File = "", [string]$EnvFile = ".env")
$ErrorActionPreference = "Stop"

if (-not (Test-Path $EnvFile)) { throw "Cannot find $EnvFile" }
$cfg = @{}
foreach ($line in Get-Content $EnvFile) {
  if ($line -match '^\s*([A-Za-z_]+)\s*=\s*(.*?)\s*$') { $cfg[$Matches[1]] = $Matches[2] }
}
if (-not ($cfg["DB_URL"] -match 'jdbc:postgresql://([^/:]+)(?::(\d+))?/([^?]+)')) { throw "Cannot read DB_URL in $EnvFile" }
$dbHost = $Matches[1]
$dbPort = if ($Matches[2]) { $Matches[2] } else { "5432" }
$dbName = $Matches[3]
$dbUser = $cfg["DB_USER"]
$dbPass = $cfg["DB_PASSWORD"]
if ($dbHost -eq "db") { $dbHost = "host.docker.internal" }
$ssl = if ($cfg["DB_URL"] -match 'sslmode=require') { "require" } else { "prefer" }

if (-not $File) {
  $latest = Get-ChildItem "backups" -Filter "itsm_*.dump" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
  if (-not $latest) { throw "No backups found in .\backups" }
  $File = $latest.Name
}
if (-not (Test-Path "backups\$File")) { throw "backups\$File not found" }

Write-Host "This will OVERWRITE database '$dbName' on '$dbHost' with $File"
Write-Host "Tip: run 'docker compose stop backend' first."
$ok = Read-Host "Type YES to continue"
if ($ok -ne "YES") { Write-Host "Cancelled."; exit }

# Safety copy of the current (possibly broken) state first
& "$PSScriptRoot\backup-db.ps1" -EnvFile $EnvFile

docker run --rm -v "${PWD}\backups:/backups" -e "PGPASSWORD=$dbPass" -e "PGSSLMODE=$ssl" postgres:18-alpine pg_restore -h $dbHost -p $dbPort -U $dbUser -d $dbName --clean --if-exists --no-owner --no-privileges "/backups/$File"
if ($LASTEXITCODE -ne 0) { Write-Host "Restore finished with warnings (often harmless). Check the app." } else { Write-Host "Restore complete." }
Write-Host "Now run: docker compose up -d backend"
