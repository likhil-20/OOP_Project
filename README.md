# OOP_Project

**Frameworks Used:** React, Spring Boot and PostgreSQL

## Setting up

1. Make a folder `App`. Enter `App` and open the terminal.

2. Clone the repository:
```bash
git clone https://github.com/likhil-20/OOP_Project
```

3. Change the directory to `OOP_Project` and use:
```bash
git pull
```
It should display `Already up to date.`

4. Install Docker Desktop.

5. Choose ONE of the two ways to run the app (A is recommended for testing).

### A) Local database (recommended for testing, does not touch the shared Neon DB)

Create a file named exactly `.env.local` in the main folder (next to `docker-compose.yml`). Run this in PowerShell:
```powershell
Set-Content .env.local "DB_URL=jdbc:postgresql://db:5432/itsmdb`nDB_USER=itsm`nDB_PASSWORD=itsm123"
```
These are only local test values, not secrets.

Start everything (first time takes a few minutes):
```bash
docker compose --env-file .env.local --profile local up -d --build db backend frontend
```
Wait about 40 seconds, then open http://localhost:3000

## Default Login Details (Demo Accounts)

| Role          | Name            | Email                      | Password   | Skills                                         |
|---------------|-----------------|----------------------------|------------|------------------------------------------------|
| Administrator | System Admin    | admin@gmail.com            | `admin123` | -                                              |
| Technician    | Alice Tech      | tech.alice@gmail.com       | `tech123`  | APPLICATION, DATABASE, NETWORK, SECURITY, HARDWARE |
| Technician    | Bob Loadbalancer| tech.bob@gmail.com         | `tech123`  | SOFTWARE, CLOUD, STORAGE                       |
| Employee      | John Employee   | employee@gmail.com         | `emp123`   | -                                              |
| Employee      | Sarah Employee  | sarah.employee@gmail.com   | `emp123`   | -                                              |
| Employee      | Rahul Employee  | rahul.employee@gmail.com   | `emp123`   | -                                              |
| Employee      | Meera Employee  | meera.employee@gmail.com   | `emp123`   | -                                              |

> These are demo accounts for testing only. Change the passwords before any real deployment.

Your login is remembered for 8 hours, even after rebuilding Docker.

### B) Shared Neon database (for final demos and shared data)

Create a file named exactly `.env` in the main folder with these 3 lines.
Ask a teammate PRIVATELY for the real values (never put them in the README or commit `.env`):
```bash
DB_URL=jdbc:postgresql://<neon-host>/neondb?sslmode=require
DB_USER=<user>
DB_PASSWORD=<password>
```
Run (not plain `docker compose up`):
```bash
docker compose up -d --build backend frontend
```
Then open http://localhost:3000

## Optional: AI triage and P1 alerts

Add these lines to your `.env.local` (or `.env`). Never commit this file.
```bash
GEMINI_API_KEY=<key from https://aistudio.google.com/apikey>
ALERT_WEBHOOK_URL=<Slack or Discord incoming-webhook URL>
```
Without a key, the form still works: a keyword-based fallback suggests the category.
Voice input records audio in the browser and Gemini turns it into text, so it needs the Gemini key and a microphone (works in Chrome, Edge and Brave on localhost).

## Day-to-day commands

For the local database, add `--env-file .env.local --profile local` to each command.

| What | Local DB command |
|---|---|
| Start (no code changes) | `docker compose --env-file .env.local --profile local up -d` |
| Backend code changed | `docker compose --env-file .env.local --profile local up -d --build backend` |
| Frontend code changed | `docker compose --env-file .env.local --profile local up -d --build frontend` |
| Stop (keeps data) | `docker compose --env-file .env.local --profile local down` |
| Stop and WIPE local data | `docker compose --env-file .env.local --profile local down -v` |

For the Neon database, use the same commands without `--env-file .env.local --profile local`.

## Occasionally free disk space
Run these two commands one at a time:
```bash
docker image prune -f
docker builder prune -f
```

## Backup and restore
Back up the shared Neon database (do this before demos or risky changes):
```bash
powershell -ExecutionPolicy Bypass -File .\backup-db.ps1
```
Back up your local database:
```bash
powershell -ExecutionPolicy Bypass -File .\backup-db.ps1 -EnvFile .env.local
```
Restore (stop the backend first; add `-EnvFile .env.local` for the local database):
```bash
docker compose stop backend
powershell -ExecutionPolicy Bypass -File .\restore-db.ps1
docker compose up -d backend
```
Backups are saved in `backups/` (ignored by git). Restoring takes a safety backup of the current state first.

## Troubleshooting
- **"Login failed. Please check credentials or approval status." on a correct password:** the backend is not running yet. Wait 40 seconds, or check `docker compose --env-file .env.local --profile local logs backend --tail 30` for a line saying `Started ItsmApplication`.
- **Backend log says `UnknownHostException: db`:** the database container is crashing. Run `down -v` (see table above), then start again.
- **Too many wrong passwords:** an account locks for 15 minutes. `docker compose restart backend` clears it.

## Run Tests
```bash
cd i-itsm-project\backend
docker run --rm -v "${PWD}:/app" -v maven-repo:/root/.m2 -w /app maven:3.9-eclipse-temurin-17 mvn test
```

## Shared Google Sheet
[Checklist](https://docs.google.com/spreadsheets/d/1SebWLuT8Mb3m15Qeci5hlAUBrBwvPxWydhXxizJZbao/edit?usp=sharing)
