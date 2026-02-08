# How to Run the 1Guntha Project

This document describes the complete process of running the real-estate application (1Guntha) — backend, frontend, and database — for local development and using Docker.

---

## Prerequisites

Install the following before running the project:

| Requirement | Version | Notes |
|-------------|---------|--------|
| **JDK** | 17 | Backend runs on Java 17. Check with `java -version`. |
| **Maven** | 3.8+ | For building and running the Spring Boot backend. |
| **Node.js** | 18+ | For the Angular frontend. Check with `node -v`. |
| **npm** | 9+ | Bundled with Node. Check with `npm -v`. |
| **Angular CLI** | 15 | Install globally: `npm install -g @angular/cli@15` |
| **PostgreSQL** | 15 | Required for backend when not using Docker. Optional if you use Docker for the DB. |

---

## Quick Reference

| What | URL |
|------|-----|
| **Frontend (Angular)** | http://localhost:4200 |
| **Backend API** | http://localhost:8080/api |
| **Swagger UI** | http://localhost:8080/api/swagger-ui.html |
| **Default admin login** | **admin@realestate.com** / **admin123** |

---

## Option 1: Run Everything Locally

Use this when you want to run the database, backend, and frontend on your machine (no Docker).

### Step 1: Start PostgreSQL

**If PostgreSQL is installed locally**, create the database:

```bash
createdb realestate
```

Or, if your OS uses a different workflow:

```bash
# Example: macOS with Homebrew PostgreSQL
psql postgres -c "CREATE DATABASE realestate;"
```

**If you prefer PostgreSQL in Docker** (only the DB):

```bash
docker run -d --name postgres \
  -e POSTGRES_DB=realestate \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15-alpine
```

Ensure PostgreSQL is listening on `localhost:5432` with user `postgres` and password `postgres` (or set the matching env vars for the backend).

### Step 2: Run the Backend

From the project root:

```bash
cd realestate-app/backend
java -version   # Must be 17
mvn clean install
mvn spring-boot:run
```

- Backend will run on **http://localhost:8080**.
- Flyway will apply migrations on first run; the default admin user is created by the application.
- Leave this terminal running.

### Step 3: Run the Frontend

Open a **new terminal**:

```bash
cd realestate-app/frontend
npm install
npm start
```

- Frontend will be at **http://localhost:4200**.
- It uses `http://localhost:8080/api` as the API base (see `src/environments/environment.ts`).

### Step 4: Verify

1. Open http://localhost:4200 in a browser.
2. Log in with **admin@realestate.com** / **admin123** (or sign up a new user).
3. API docs: http://localhost:8080/api/swagger-ui.html.

---

## Option 2: Run with Docker Compose

This starts **PostgreSQL** and the **Spring Boot backend** in containers. You still run the **frontend locally** for development.

### Step 1: Start PostgreSQL + Backend

From the project root:

```bash
cd realestate-app
docker-compose up -d
```

- **PostgreSQL**: `localhost:5432`, database `realestate`, user/pass `postgres`/`postgres`.
- **Backend**: http://localhost:8080 (API at http://localhost:8080/api).

Check logs:

```bash
docker-compose logs -f backend
```

### Step 2: Run the Frontend Locally

In another terminal:

```bash
cd realestate-app/frontend
npm install
npm start
```

App URL: http://localhost:4200.

### Step 3: Stop Services

```bash
cd realestate-app
docker-compose down
```

To remove the database volume as well:

```bash
docker-compose down -v
```

---

## Option 3: Build and Run Backend Docker Image Only

If you already have PostgreSQL running and only want the backend in Docker:

```bash
cd realestate-app
docker build -t realestate-backend ./backend
docker run -d --name realestate-backend \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=5432 \
  -e DB_NAME=realestate \
  -e DB_USER=postgres \
  -e DB_PASSWORD=postgres \
  realestate-backend
```

On Linux, use your host IP instead of `host.docker.internal`, or attach the container to the host network as needed.

---

## Environment Configuration

### Backend

Key settings (see `backend/src/main/resources/application.yml` or environment variables):

| Variable | Description | Default |
|----------|-------------|--------|
| `SPRING_PROFILES_ACTIVE` | Profile (dev / test) | dev |
| `DB_HOST` | PostgreSQL host | localhost |
| `DB_PORT` | PostgreSQL port | 5432 |
| `DB_NAME` | Database name | realestate |
| `DB_USER` / `DB_PASSWORD` | DB credentials | postgres / postgres |
| `JWT_SECRET` | JWT signing secret | (dev default in config) |
| `FRONTEND_URL` | Allowed CORS origin | http://localhost:4200 |

For production, set `JWT_SECRET` and other secrets via environment variables.

### Frontend

- **Development**: `src/environments/environment.ts` — `apiUrl: 'http://localhost:8080/api'`.
- **Production**: `src/environments/environment.prod.ts` — typically `apiUrl: '/api'` when the app is served with the backend.

No extra env file is required for the default local run.

---

## Running Tests

### Backend (H2, no PostgreSQL required)

```bash
cd realestate-app/backend
mvn test
```

The test profile uses an in-memory H2 database.

### Frontend

```bash
cd realestate-app/frontend
npm test
```

---

## Troubleshooting

| Issue | What to do |
|-------|------------|
| **Port 8080 already in use** | Stop the process using 8080 or change `server.port` in `application.yml`. |
| **Port 4200 already in use** | Stop the other app using 4200 or run Angular with `ng serve --port 4300`. |
| **Database connection refused** | Ensure PostgreSQL is running and reachable at `DB_HOST:DB_PORT`. For Docker backend, use `DB_HOST=postgres`; for local backend, use `localhost`. |
| **CORS errors in browser** | Ensure backend `FRONTEND_URL` (or CORS config) includes `http://localhost:4200` and the backend is running. |
| **401 on API calls after login** | Check that the frontend is sending the JWT in the `Authorization` header and that `apiUrl` points to the running backend. |
| **Flyway / schema errors** | For a clean DB, drop and recreate the `realestate` database, then restart the backend. |
| **Docker build fails (backend)** | Run `docker-compose build --no-cache backend` and ensure JDK 17 and Maven can resolve dependencies (network access). |

---

## Summary

- **Local full stack**: Start PostgreSQL → run backend with `mvn spring-boot:run` → run frontend with `npm start`.
- **Docker**: `docker-compose up -d` for DB + backend; run frontend with `npm start`.
- **Tests**: Backend `mvn test`, frontend `npm test`.

For API details and project structure, see the main [README.md](README.md).
