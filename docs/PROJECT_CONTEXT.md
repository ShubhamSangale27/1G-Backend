# Project Context — 1Guntha (Real Estate App)

This file stores the **conversation and project context** for this codebase so that when you (or an AI assistant) return to the project, you have a single place to understand what was built, why, and how to work with it.

---

## 1. Project overview

- **Name:** 1Guntha (rebranded from “Real Estate Ecommerce Application”).
- **Purpose:** Full-stack real-estate platform similar to 99acres.com — buy, sell, rent, search properties, with auth, dashboards, admin, site visits, and payments.
- **Repo layout:** Monorepo with `backend/` (Spring Boot) and `frontend/` (Angular). Root contains docs and Docker/CI config.

---

## 2. Tech stack

| Layer      | Technology |
|-----------|------------|
| Frontend  | Angular 15+, Node 18, Leaflet, ngx-toastr |
| Backend   | Spring Boot 3.x, JDK 17, Maven |
| Database  | PostgreSQL (prod/dev), H2 (tests) |
| Auth      | JWT (access + refresh), OTP (email/SMS) |
| Migrations| Flyway |
| API docs  | Swagger/OpenAPI |
| Hosting   | Heroku (CLI), or Render/Fly.io/Oracle/Railway (see HOSTING.md) |

---

## 3. Conversation / session history (what was done)

Summary of what was requested and implemented across sessions:

1. **Initial build**
   - Full Spring Boot backend: entities, repos, services, REST APIs, JWT, Flyway, Swagger, Stripe/Twilio placeholders.
   - Full Angular frontend: auth, search, property listing/detail, dashboard, admin, my properties, property form, site visits, payments UI.
   - PostgreSQL + H2 (tests), JDK 17.

2. **UI/UX and rebranding**
   - 99acres-inspired UI (hero, search, cards, dashboards, skeleton loaders).
   - Rebrand to **1Guntha** in the frontend (logo, footer, meta, copy). Backend APIs unchanged.

3. **API sync and validation**
   - Aligned frontend calls with backend endpoints; fixed `Watchlist` entity, `SiteVisitService`, `PropertyController` (getById, DELETE), test context path.
   - Documented in API_VALIDATION.md, SYNC_STATUS.md.

4. **Run and deploy docs**
   - **RUNNING.md:** How to run locally (PostgreSQL → backend → frontend) and with Docker Compose.
   - **HOSTING.md:** Low-cost hosting (Render, Fly.io, Oracle Always Free, Railway, DigitalOcean).

5. **GitHub and CI/CD**
   - **CI:** `.github/workflows/ci.yml` — build and test backend (Maven) and frontend (Angular + Karma ChromeHeadlessCI) on push/PR to `main` or `master`.
   - **CD:** `.github/workflows/deploy.yml` — optional Render deploy hooks after CI; Fly.io deploy commented.
   - **CI_CD.md:** Push to GitHub, enable Actions, set secrets, optional branch protection.

6. **Heroku (CLI-only)**
   - **Backend:** Procfile, `system.properties` (JDK 17), `application-heroku.yml` (PORT, exclude DataSource auto-config), `HerokuDataSourceConfig` (parses `DATABASE_URL` from Heroku Postgres).
   - **Frontend:** Node server (`server.js`) serves built Angular and `/config.json`; `ConfigService` loads API URL at runtime; `ApiService` uses `ConfigService.apiUrl`; Procfile, `heroku-postbuild`, `engines.node`.
   - **HEROKU.md:** Step-by-step Heroku CLI (create two apps, add Postgres, set config vars, deploy with `git subtree push --prefix backend heroku-backend main` and `--prefix frontend heroku-frontend main`).

7. **This file**
   - **PROJECT_CONTEXT.md:** Single place for project and conversation context for future sessions.

---

## 4. Key decisions and configurations

- **Backend context path:** `/api` (all REST under `/api`).
- **Default admin:** `admin@realestate.com` / `admin123` (created by app/Flyway/DataLoader).
- **Frontend API URL:** Build-time default in `environment.ts`/`environment.prod.ts`; on Heroku (and any host with a Node server), runtime `/config.json` overrides via `ConfigService` and `APP_INITIALIZER`.
- **Heroku:** Two apps (backend + frontend). Backend uses profile `heroku` and `DATABASE_URL`; frontend sets `API_URL` and serves `/config.json`.
- **CI:** Frontend tests use Karma with `ChromeHeadlessCI` (`karma.conf.js`); backend tests use H2 and `application-test.yml`.

---

## 5. Important files and docs

| File / folder | Purpose |
|---------------|--------|
| **README.md** | Overview, features, prerequisites, quick setup, links to other docs |
| **RUNNING.md** | How to run locally and with Docker (PostgreSQL, backend, frontend) |
| **HEROKU.md** | Heroku deployment using only Heroku CLI (two apps, config vars, subtree push) |
| **HOSTING.md** | Low-cost hosting options (Render, Fly.io, Oracle, Railway, DigitalOcean) |
| **CI_CD.md** | GitHub setup, Actions, secrets, CD options (Render/Fly.io/Railway) |
| **API_VALIDATION.md** | Frontend–backend API alignment and fixes |
| **REBRANDING_1GUNTHA.md** | Rebrand from “Real Estate” to “1Guntha” |
| **UI_ENHANCEMENTS.md** | UI/UX changes (99acres-style) |
| **SYNC_STATUS.md** | API and UI sync status |
| **PROJECT_CONTEXT.md** | This file — project and conversation context |
| **.github/workflows/ci.yml** | CI: build and test backend + frontend |
| **.github/workflows/deploy.yml** | CD: optional Render deploy hooks |
| **backend/Procfile** | Heroku backend process (run JAR) |
| **backend/system.properties** | JDK 17 for Heroku |
| **backend/.../application-heroku.yml** | Heroku profile (PORT, CORS, exclude DataSource auto-config) |
| **backend/.../HerokuDataSourceConfig.java** | DataSource from `DATABASE_URL` on Heroku |
| **frontend/Procfile** | Heroku frontend process (`node server.js`) |
| **frontend/server.js** | Serves built Angular + `/config.json` |
| **frontend/.../config.service.ts** | Loads `/config.json` for API URL |
| **frontend/.../api.service.ts** | Uses `ConfigService.apiUrl` |
| **frontend/karma.conf.js** | Karma + ChromeHeadlessCI for CI |

---

## 6. Quick reference for next session

- **Run locally:** See RUNNING.md (PostgreSQL → `cd backend && mvn spring-boot:run` → `cd frontend && npm install && npm start`).
- **Deploy to Heroku:** See HEROKU.md (two apps, `git subtree push --prefix backend/ frontend`).
- **CI/CD:** See CI_CD.md; workflows in `.github/workflows/`.
- **Hosting alternatives:** See HOSTING.md.
- **Backend API base:** `http://localhost:8080/api` (local); Swagger at `/api/swagger-ui.html`.
- **Frontend:** `http://localhost:4200` (local); uses `ConfigService` for API URL when `/config.json` is present.

Use **PROJECT_CONTEXT.md** as the main context file when continuing work or onboarding an AI assistant on this project.
