# GitHub Integration and CI/CD

This document explains how to connect the 1Guntha project to GitHub and use the included CI/CD workflows.

---

## Overview

| Workflow | File | Trigger | What it does |
|----------|------|---------|--------------|
| **CI** | `.github/workflows/ci.yml` | Push / PR to `main` or `master` | Builds and tests backend (Maven) and frontend (Angular) |
| **Deploy** | `.github/workflows/deploy.yml` | After CI succeeds, or manual | Optional: triggers Render deploy hooks (or extend for Fly.io) |

---

## 1. Push the project to GitHub

### Create a new repository on GitHub

1. Go to [github.com/new](https://github.com/new).
2. Create a repository (e.g. `1guntha` or `realestate-app`). Do **not** initialize with a README if you already have one.
3. Note the repository URL: `https://github.com/YOUR_USERNAME/YOUR_REPO.git`.

### Push from your machine

From your project root (the folder that contains `backend/`, `frontend/`, and `.github/`):

```bash
cd /path/to/realestate-app

git init
git add .
git commit -m "Initial commit: Angular + Spring Boot + CI/CD"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git push -u origin main
```

If the repo already exists locally with a different remote, just update and push:

```bash
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git   # only if not already added
git push -u origin main
```

---

## 2. Enable GitHub Actions

- Actions are **on** by default for public repos.
- For a **private** repo: **Settings → Actions → General** → allow "Read and write permissions" for the default GITHUB_TOKEN if you need it for deploy.
- Workflows run automatically on every **push** and **pull_request** to `main` (or `master`).

---

## 3. What CI does

On every **push** or **pull request** to `main`/`master`:

1. **Backend**
   - JDK 17, Maven cache
   - `mvn -B clean verify` (compile + tests with H2; no PostgreSQL required)

2. **Frontend**
   - Node 18, npm cache
   - `npm install` → `npm run build` → `ng test --no-watch --browsers=ChromeHeadlessCI`

If either job fails, the workflow fails. Fix the failing step and push again.

---

## 4. CD options

### Option A: Render (recommended for free tier)

Render can **auto-deploy on push** when you connect GitHub — no GitHub Actions deploy step required.

1. Sign up at [render.com](https://render.com) and connect your GitHub account.
2. **New → Web Service** for the backend:
   - Connect the repo, select branch `main`.
   - Root directory: `backend` (or build command: `mvn -DskipTests package`, start: `java -jar target/*.jar`).
   - Add a **PostgreSQL** database in the same account and attach it (env vars: `DB_HOST`, `DB_NAME`, etc.).
3. **New → Static Site** for the frontend:
   - Same repo, branch `main`, root: `frontend`.
   - Build: `npm install && npm run build`, publish directory: `dist/realestate-frontend`.
   - Add env var for API URL, e.g. `NG_APP_API_URL` or use Angular environment at build time pointing to your backend URL.

**Optional: Deploy only after CI (GitHub Actions trigger)**  
- In Render, open your **Web Service** → **Settings** → **Deploy Hook**; copy the URL.
- In GitHub: **Settings → Secrets and variables → Actions** → **New repository secret**:
  - Name: `RENDER_DEPLOY_HOOK_BACKEND`, Value: (paste deploy hook URL).
- Do the same for the static site → `RENDER_DEPLOY_HOOK_FRONTEND`.
- Then the **Deploy** workflow will call these URLs after CI succeeds (and on manual run). You can still use Render’s native “Deploy on push” if you prefer.

### Option B: Fly.io

1. Install [flyctl](https://fly.io/docs/hands-on/install-flyctl/) and log in.
2. From the repo root, create an app and deploy the backend, e.g.:
   - `cd backend && fly launch` (follow prompts; add Postgres if needed).
   - Or add a `fly.toml` in `backend/` and use `fly deploy`.
3. To deploy from GitHub Actions:
   - **Settings → Secrets and variables → Actions** → add `FLY_API_TOKEN` (from `fly tokens create deploy`).
   - Uncomment the `fly-deploy` job in `.github/workflows/deploy.yml` and set your app name / path (e.g. `backend/fly.toml`).

### Option C: Railway

1. Sign up at [railway.app](https://railway.app), connect GitHub.
2. **New Project → Deploy from GitHub** → select repo and branch.
3. Add a **PostgreSQL** plugin; Railway sets `DATABASE_URL`.
4. Configure build/start for the backend (e.g. root `backend`, build `mvn -DskipTests package`, start `java -jar target/*.jar`).
5. Railway deploys on every push; no extra secrets in GitHub for basic CD.

---

## 5. GitHub Secrets (for CD from Actions)

If you use the **Deploy** workflow to trigger Render:

| Secret | Required for | Description |
|--------|----------------|-------------|
| `RENDER_DEPLOY_HOOK_BACKEND` | Render backend | Deploy hook URL from Render Web Service → Settings |
| `RENDER_DEPLOY_HOOK_FRONTEND` | Render frontend | Deploy hook URL from Render Static Site → Settings |

For Fly.io deploy from Actions:

| Secret | Required for | Description |
|--------|----------------|-------------|
| `FLY_API_TOKEN` | Fly.io | Create with `fly tokens create deploy` |

Add secrets: repo **Settings → Secrets and variables → Actions → New repository secret**.

---

## 6. Branch protection (optional)

To ensure only tested code is merged:

1. **Settings → Branches → Add branch protection rule** for `main` (or `master`).
2. Enable **Require status checks to pass before merging** and select **Backend** and **Frontend** (the CI job names).
3. Optionally enable **Require pull request reviews**.

---

## 7. File summary

| Path | Purpose |
|------|---------|
| `.github/workflows/ci.yml` | Build and test backend + frontend on push/PR |
| `.github/workflows/deploy.yml` | Optional deploy (Render hooks; Fly.io commented) |
| `frontend/karma.conf.js` | Karma config with ChromeHeadlessCI for CI tests |
| `frontend/angular.json` | Test options point to `karma.conf.js` |

---

## 8. Quick checklist

- [ ] Create GitHub repo and push project (including `.github/`).
- [ ] Confirm CI runs on push/PR (Actions tab).
- [ ] Choose CD: Render (connect repo) **or** Render deploy hooks + secrets **or** Fly.io/Railway.
- [ ] Add secrets if using deploy workflow (Render hooks or FLY_API_TOKEN).
- [ ] Optionally add branch protection for `main`.

For run instructions (local and Docker), see [RUNNING.md](RUNNING.md). For hosting options, see [HOSTING.md](HOSTING.md).
