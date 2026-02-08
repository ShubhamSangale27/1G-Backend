# App configuration guide

This document describes how to configure the app: **Firebase (Phone OTP)**, **backend single config**, and **which variables to set** so everything works.

---

## 1. Backend: single config file

All backend configuration lives in **one place**:

- **`backend/src/main/resources/application.yml`** – main config (database, SMTP, JWT, OTP, Firebase, Stripe, Google Maps, etc.)
- Profile-specific overrides: `application-heroku.yml` for Heroku.

You can override any value with **environment variables** (recommended for production). The table below lists the variables and where to set them.

---

## 2. Firebase setup (Phone OTP – replace Twilio)

Phone OTP is sent via **Firebase Phone Authentication** (no Twilio). Email OTP is still sent by the backend (SMTP).

### 2.1 Create / use a Firebase project (Google account)

1. Go to [Firebase Console](https://console.firebase.google.com/) and sign in with your Google account.
2. Click **Add project** (or select an existing project).
3. Enter a project name (e.g. `realestate-app`) and follow the steps (Analytics optional).
4. Once the project is created, open it.

### 2.2 Enable Phone Authentication

1. In the left menu: **Build** → **Authentication**.
2. Open the **Sign-in method** tab.
3. Click **Phone**, turn **Enable** on, and save.

### 2.3 Register a web app and get config

1. In Project overview (gear icon) → **Project settings**.
2. Under **Your apps**, click the **Web** icon (`</>`).
3. Register an app (e.g. "Real Estate Web").
4. Copy the **firebaseConfig** object (apiKey, authDomain, projectId, storageBucket, messagingSenderId, appId). You will put these in the frontend.

### 2.4 Get the service account key (for backend)

1. In **Project settings** → **Service accounts**.
2. Click **Generate new private key** (confirm).
3. A JSON file is downloaded. **Keep it secret** (do not commit to git).
4. Use it for the backend in one of two ways:
   - **Option A:** Save the file (e.g. `backend/firebase-service-account.json`) and set the path in config.
   - **Option B:** Encode the JSON as base64 and set the base64 string in an env var (e.g. `FIREBASE_SERVICE_ACCOUNT_JSON`).

---

## 3. Where to configure what

### 3.1 Frontend

| What | File | Variables / values |
|------|------|--------------------|
| API URL | `frontend/src/environments/environment.ts` (dev) / `environment.prod.ts` (prod) | `apiUrl`: e.g. `http://localhost:8080/api` (dev), `/api` (prod) |
| Firebase (Phone OTP) | Same files | `firebase`: object with `apiKey`, `authDomain`, `projectId`, `storageBucket`, `messagingSenderId`, `appId` from Firebase Console (see 2.3) |
| Google Maps | Same files | `googleMapsApiKey`: your Google Maps API key (optional) |

**Example** – `frontend/src/environments/environment.ts`:

```ts
export const firebaseConfig = {
  apiKey: 'AIza...',
  authDomain: 'your-app.firebaseapp.com',
  projectId: 'your-app',
  storageBucket: 'your-app.appspot.com',
  messagingSenderId: '123456789',
  appId: '1:123456789:web:abc123',
};

export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  googleMapsApiKey: '',
  firebase: firebaseConfig,
};
```

Replace the placeholder values with the ones from your Firebase project (step 2.3).

### 3.2 Backend – single config and env vars

| Area | Config key / env var | Description |
|------|----------------------|-------------|
| **Database** | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | PostgreSQL (or use `spring.datasource.*` in YAML). |
| **SMTP (email OTP)** | `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD`, `MAIL_FROM` | Gmail: use [App Password](https://support.google.com/accounts/answer/185833). |
| **JWT** | `JWT_SECRET`, `JWT_ACCESS_TTL`, `JWT_REFRESH_TTL` | Secret key and token TTLs. |
| **OTP** | `OTP_EXPIRY_MINUTES`, `OTP_LENGTH` | Email OTP expiry and length. |
| **Firebase** | `FIREBASE_ENABLED` | Set to `true` to use Firebase for phone verification. |
| | `FIREBASE_PROJECT_ID` | Firebase project ID. |
| | `FIREBASE_SERVICE_ACCOUNT_PATH` | Path to service account JSON file (e.g. `./firebase-service-account.json`). |
| | `FIREBASE_SERVICE_ACCOUNT_JSON` | **Or** base64-encoded JSON (alternative to path). |
| **Stripe** | `STRIPE_API_KEY`, `STRIPE_WEBHOOK_SECRET` | Payments (optional). |
| **Google Maps** | `GOOGLE_MAPS_API_KEY` | Optional. |
| **App** | `FRONTEND_URL`, `UPLOAD_DIR`, `MAIL_FROM` | CORS, uploads, email “from” address. |

All of these are read from **`application.yml`** (and profile-specific files). You can set them there or override with environment variables.

**Example** – local dev with Firebase:

1. Save the service account JSON as `backend/firebase-service-account.json` (and add it to `.gitignore`).
2. Set in `application.yml` under the `dev` profile or via env:

```yaml
app:
  firebase:
    enabled: true
    project-id: your-app
    service-account-path: ./firebase-service-account.json
```

Or with env vars (no path in repo):

```bash
export FIREBASE_ENABLED=true
export FIREBASE_PROJECT_ID=your-app
export FIREBASE_SERVICE_ACCOUNT_JSON="<base64-encoded JSON>"
```

---

## 4. Quick checklist

**Firebase (Phone OTP):**

- [ ] Firebase project created, Phone auth enabled.
- [ ] Web app registered; `apiKey`, `authDomain`, `projectId`, etc. copied into frontend `environment.ts` / `environment.prod.ts` under `firebase`.
- [ ] Service account JSON downloaded; path or base64 set in backend (`FIREBASE_SERVICE_ACCOUNT_PATH` or `FIREBASE_SERVICE_ACCOUNT_JSON`).
- [ ] Backend: `FIREBASE_ENABLED=true` (and `FIREBASE_PROJECT_ID` if not in JSON).

**Backend:**

- [ ] Database: `DB_*` or `spring.datasource.*` set.
- [ ] SMTP: `MAIL_*` set (for email OTP).
- [ ] JWT: `JWT_SECRET` set (and TTLs if needed).
- [ ] Optional: Stripe, Google Maps, `FRONTEND_URL`, `MAIL_FROM`, `UPLOAD_DIR`.

**Frontend:**

- [ ] `apiUrl` points to your backend.
- [ ] `firebase` object filled from Firebase Console (step 2.3).

After this, signup sends **email OTP** via SMTP and **phone OTP** via Firebase (user clicks “Send verification code” on the verify-otp page, gets SMS from Firebase, enters code, and backend verifies the Firebase ID token).
