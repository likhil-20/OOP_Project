# OOP_Project

**Frameworks Used:** React, Spring Boot and PostgreSQL

### Setting up
1. Make a folder `App`. Enter `App` and open the terminal.
   
2. Clone the repository
  ```bash
  git clone https://github.com/likhil-20/OOP_Project
  ```

3. Change the directory to `OOP_Project` and use:
  ```bash
  git pull
  ```
  It should display `Already up to date.`
   
4. Install docker
   
5. Do the Following:
  - In the main folder (next to docker-compose.yml) create a file named exactly ".env" and insert these 3 lines into ".env":

 ```bash
   DB_URL=jdbc:postgresql://ep-muddy-poetry-b537ul7o.c-7.us-east-2.aws.neon.tech/neondb?sslmode=require
   DB_USER=neondb_owner
   DB_PASSWORD=npg_xbyXZTeU5V9J
 ```

6. Run:
  ```bash
  docker compose up --build backend frontend
  ```
   (not plain "docker compose up")
   
7. Open
  ```bash
  http://localhost:3000
  ```

### Checklist:
`https://docs.google.com/spreadsheets/d/1SebWLuT8Mb3m15Qeci5hlAUBrBwvPxWydhXxizJZbao/edit?usp=sharing`

8. To Close everything, use:
```bash
docker compose down
```
