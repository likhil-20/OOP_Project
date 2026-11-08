# Usage:  .\backup-db.ps1                    (backs up the DB in .env)
#         .\backup-db.ps1 -EnvFile .env.local (backs up the local DB)
param([string]$EnvFile = ".env", [int]$Keep = 10)
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

$stamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$file = "itsm_$stamp.dump"
New-Item -ItemType Directory -Force -Path "backups" | Out-Null

Write-Host "Backing up '$dbName' on '$dbHost' ..."
docker run --rm -v "${PWD}\backups:/backups" -e "PGPASSWORD=$dbPass" -e "PGSSLMODE=$ssl" postgres:18-alpine pg_dump -h $dbHost -p $dbPort -U $dbUser -d $dbName -Fc --no-owner --no-privileges -f "/backups/$file"
if ($LASTEXITCODE -ne 0) { throw "Backup FAILED" }

Get-ChildItem "backups" -Filter "itsm_*.dump" | Sort-Object LastWriteTime -Descending | Select-Object -Skip $Keep | Remove-Item
Write-Host "Done: backups\$file (keeping the newest $Keep)"
