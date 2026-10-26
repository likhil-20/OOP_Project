# OOP_Project

**Frameworks Used:** React, Spring Boot and PostgreSQL

### Setting up
1. Make a folder `App`. Enter `App` and open the terminal.
2. Clone the repository
```bash
git clone https://github.com/likhil-20/OOP_Project
```
3. Change the directory to `OOP_Project` and use `git pull`. It should display `Already up to date.`
4. Install docker
5. Do the Following:
  - In the main folder (next to docker-compose.yml) create a file named exactly ".env" with these 3 lines:

    DB_URL=jdbc:postgresql://ep-muddy-poetry-b537ul7o.c-7.us-east-2.aws.neon.tech/neondb?sslmode=require
    
    DB_USER=neondb_owner
    
    DB_PASSWORD=npg_xbyXZTeU5V9J

6. Run: docker compose up --build backend frontend (not plain "docker compose up")
7. Open http://localhost:3000
