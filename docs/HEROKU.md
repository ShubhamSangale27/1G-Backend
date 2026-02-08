# Deploying 1Guntha to Heroku (CLI)

This guide uses **Heroku CLI** only. You will create two Heroku apps: one for the backend (Spring Boot + Postgres) and one for the frontend (Angular served by Node).

---

## Prerequisites

- [Heroku CLI](https://devcenter.heroku.com/articles/heroku-cli) installed and logged in: `heroku login`
- Git repository with the project (e.g. pushed to GitHub)
- Project root: the folder that contains `backend/` and `frontend/`

---

## 1. Backend app (Spring Boot + Postgres)

### 1.1 Create app and add Postgres

```bash
cd /path/to/realestate-app

# Create backend app (use a unique name or let Heroku generate one)
heroku create your-backend-app-name --remote heroku-backend

# Or without a name (Heroku assigns one):
heroku create --remote heroku-backend

# Add Heroku Postgres (essential)
heroku addons:create heroku-postgresql:essential-0 -a $(heroku apps:info -a $(git remote get-url heroku-backend 2>/dev/null | sed 's/.*\///;s/\.git//') 2>/dev/null || echo "your-backend-app-name")
```

If you used a named app:

```bash
heroku addons:create heroku-postgresql:essential-0 -a your-backend-app-name
```

This sets `DATABASE_URL` automatically.

### 1.2 Set config vars

```bash
# Replace your-backend-app-name with your actual backend app name
heroku config:set SPRING_PROFILES_ACTIVE=heroku -a your-backend-app-name
heroku config:set JWT_SECRET="your-256-bit-secret-key-here" -a your-backend-app-name
heroku config:set FRONTEND_URL="https://your-frontend-app-name.herokuapp.com" -a your-backend-app-name
```

Use a strong value for `JWT_SECRET` (e.g. 32+ random characters). Set `FRONTEND_URL` to your frontend Heroku URL (you can set it after creating the frontend app).

### 1.3 Deploy backend (subtree from `backend/`)

Heroku builds from the **root** of what you push. To deploy only the backend, push the `backend/` directory as the root using `git subtree`:

```bash
cd /path/to/realestate-app

# One-time: add Heroku backend remote (if not already done)
# heroku create your-backend-app-name --remote heroku-backend

# Deploy: push backend/ as the repo root
git subtree push --prefix backend heroku-backend main
```

If your default branch is `master`:

```bash
git subtree push --prefix backend heroku-backend master
```

Heroku will:

1. Detect the Java app (from `pom.xml` in the pushed root)
2. Run Maven build
3. Start with `Procfile`: `web: java -Dserver.port=$PORT ... -jar target/realestate-backend-1.0.0.jar`

### 1.4 Verify backend

```bash
heroku open -a your-backend-app-name
```

You should see the API (e.g. Swagger at `https://your-backend-app-name.herokuapp.com/api/swagger-ui.html`). Default admin: **admin@realestate.com** / **admin123**.

---

## 2. Frontend app (Angular + Node server)

### 2.1 Create app

```bash
cd /path/to/realestate-app

heroku create your-frontend-app-name --remote heroku-frontend
# Or: heroku create --remote heroku-frontend
```

### 2.2 Set API URL

Point the frontend to your backend API:

```bash
heroku config:set API_URL="https://your-backend-app-name.herokuapp.com/api" -a your-frontend-app-name
```

Replace `your-backend-app-name` with your real backend app name.

### 2.3 Deploy frontend (subtree from `frontend/`)

```bash
git subtree push --prefix frontend heroku-frontend main
```

For `master`:

```bash
git subtree push --prefix frontend heroku-frontend master
```

Heroku will:

1. Detect Node (from `package.json` in the pushed root)
2. Run `npm install`
3. Run `heroku-postbuild` → `npm run build` (Angular production build)
4. Start with `Procfile`: `web: node server.js`

The Node server serves the built app and `/config.json` (from `API_URL`).

### 2.4 Verify frontend

```bash
heroku open -a your-frontend-app-name
```

Open the app and log in (use the same admin user as backend).

### 2.5 Update backend CORS

Ensure the backend allows your frontend origin:

```bash
heroku config:set FRONTEND_URL="https://your-frontend-app-name.herokuapp.com" -a your-backend-app-name
```

---

## 3. Summary of Heroku config

### Backend app

| Config var | Description |
|------------|-------------|
| `DATABASE_URL` | Set automatically by Heroku Postgres add-on |
| `SPRING_PROFILES_ACTIVE` | `heroku` |
| `JWT_SECRET` | Your secret (256+ bits recommended) |
| `FRONTEND_URL` | `https://your-frontend-app-name.herokuapp.com` |

### Frontend app

| Config var | Description |
|------------|-------------|
| `API_URL` | `https://your-backend-app-name.herokuapp.com/api` |
| `NODE_ENV` | Set by Heroku to `production` |

---

## 4. Useful Heroku CLI commands

```bash
# List apps
heroku apps

# Backend logs
heroku logs -t -a your-backend-app-name

# Frontend logs
heroku logs -t -a your-frontend-app-name

# Run command on backend (e.g. Maven)
heroku run mvn -v -a your-backend-app-name

# Open backend Swagger
heroku open -a your-backend-app-name
# Then go to /api/swagger-ui.html

# Scale (if on paid dynos)
heroku ps -a your-backend-app-name
heroku ps:scale web=1 -a your-backend-app-name
```

---

## 5. Project layout for Heroku

| Path | Purpose |
|------|---------|
| `backend/Procfile` | Backend process: run the JAR |
| `backend/system.properties` | JDK 17 for Heroku |
| `backend/src/main/resources/application-heroku.yml` | Heroku profile (PORT, exclude datasource auto-config) |
| `backend/.../config/HerokuDataSourceConfig.java` | DataSource from `DATABASE_URL` |
| `frontend/Procfile` | Frontend process: `node server.js` |
| `frontend/server.js` | Serves built Angular + `/config.json` |
| `frontend/package.json` | `start`, `heroku-postbuild`, `engines.node` |
| `frontend/.../config.service.ts` | Loads `/config.json` for API URL |
| `frontend/.../api.service.ts` | Uses `ConfigService.apiUrl` |

---

## 6. Redeploy after changes

**Backend:**

```bash
git subtree push --prefix backend heroku-backend main
```

**Frontend:**

```bash
git subtree push --prefix frontend heroku-frontend main
```

---

## 7. Troubleshooting

| Issue | What to do |
|-------|------------|
| Backend build fails | Check `heroku buildpacks -a your-backend-app-name` (should include `heroku/java`). Ensure `backend/pom.xml` and `backend/Procfile` are at the root of what you push (subtree from `backend/`). |
| Backend: “No DataSource” | Set `SPRING_PROFILES_ACTIVE=heroku` and ensure Postgres add-on is attached (`heroku addons -a your-backend-app-name`). |
| Frontend: API calls fail / CORS | Set `FRONTEND_URL` on the backend to `https://your-frontend-app-name.herokuapp.com`. Set `API_URL` on the frontend to `https://your-backend-app-name.herokuapp.com/api`. |
| Frontend: blank page | Check `heroku logs -a your-frontend-app-name`. Ensure `heroku-postbuild` ran and `dist/realestate-frontend` exists. |
| Subtree push rejected | Run `git subtree split --prefix backend main` (or `frontend`) and inspect the result. Ensure the prefix path is correct and the branch is `main` or `master`. |

For local run and Docker, see [RUNNING.md](RUNNING.md).
