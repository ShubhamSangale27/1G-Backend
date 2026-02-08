# Environment & Config Placeholders

Use this file as a reference for where to set values for **PostgreSQL**, **SMTP (email OTP)**, **Firebase (mobile OTP)**, and related config. Set these as environment variables or in `backend/src/main/resources/application.yml` (or profile-specific YAML). Never commit real secrets to git.

---

## 1. PostgreSQL

| Placeholder   | Description              | Example / Where to set |
|---------------|--------------------------|-------------------------|
| `DB_HOST`     | Database host            | `localhost` or your DB host |
| `DB_PORT`     | Database port            | `5432` |
| `DB_NAME`     | Database name            | `realestate` |
| `DB_USER`     | Database user            | `postgres` or your DB user |
| `DB_PASSWORD` | Database password        | Your DB password |

**Heroku:** Use `DATABASE_URL` (set automatically by Heroku Postgres). The app parses it in `HerokuDataSourceConfig`; no need to set `DB_*` separately.

**If you see "Column USER_ID not found" (properties table):** The schema uses `owner_id`. Flyway V1 creates `owner_id`. If your DB was created by an older Hibernate run with `user_id`, run manually: **H2:** `ALTER TABLE properties ALTER COLUMN user_id RENAME TO owner_id;` **PostgreSQL:** `ALTER TABLE properties RENAME COLUMN user_id TO owner_id;`

**Local / Docker:** Set in env or in `application.yml` under `spring.datasource` (dev profile):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:realestate}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
```

---

## 2. SMTP (Email OTP)

Used to send OTP emails. Set these so the app can send mail (e.g. Gmail, SendGrid, Mailgun).

| Placeholder    | Description           | Example / Where to set |
|----------------|-----------------------|-------------------------|
| `MAIL_HOST`    | SMTP server           | `smtp.gmail.com` |
| `MAIL_PORT`    | SMTP port             | `587` (TLS) or `465` (SSL) |
| `MAIL_USER`    | SMTP username (email) | `your-email@gmail.com` |
| `MAIL_PASSWORD`| SMTP password         | App password or account password |

**Gmail:** Use an [App Password](https://support.google.com/accounts/answer/185833) (2FA required). Do not use your normal Gmail password.

**In code:** Used in `application.yml` / `application-heroku.yml` under `spring.mail`:

```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USER:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

If `MAIL_USER` / `MAIL_PASSWORD` are empty, the app still starts but email OTP is not sent (backend logs: "Mail sender not available in local mode.").

---

## 3. Firebase (Mobile OTP – placeholder for future use)

You can replace Twilio with **Firebase Phone Authentication** for sending OTP to mobile numbers. Firebase uses its own infrastructure (reCAPTCHA + SMS) and has a free tier.

**Placeholders to add when you implement Firebase:**

| Placeholder              | Description                    | Example / Where to set |
|--------------------------|--------------------------------|-------------------------|
| `FIREBASE_PROJECT_ID`    | Firebase project ID            | From Firebase Console → Project settings |
| `FIREBASE_PRIVATE_KEY`   | Service account private key    | From Firebase Console → Service accounts → Generate key (JSON) |
| `FIREBASE_CLIENT_EMAIL`  | Service account client email   | From the same JSON key file |

**Implementation options:**

- **Option A (client-side):** Use Firebase JS SDK in the frontend for Phone Auth (reCAPTCHA + SMS). After successful sign-in, send the Firebase ID token to your backend; verify the token with Firebase Admin SDK and then mark mobile as verified.
- **Option B (backend):** Use Firebase Admin SDK on the backend to send/verify custom OTP; this would require replacing or extending the current `OtpService` SMS path with Firebase.

Until Firebase is implemented, the app uses the existing SMS path (e.g. Twilio or log-only in local mode). Add the above placeholders in `application.yml` under a dedicated `app.firebase` section when you integrate Firebase.

---

## 4. Twilio (current mobile SMS – optional)

If you keep Twilio for SMS OTP:

| Placeholder           | Description        | Example / Where to set |
|-----------------------|--------------------|-------------------------|
| `TWILIO_ACCOUNT_SID`  | Twilio account SID | From Twilio Console |
| `TWILIO_AUTH_TOKEN`   | Twilio auth token | From Twilio Console |
| `TWILIO_FROM_NUMBER`  | Twilio phone number| e.g. `+1234567890` |

Used in `application.yml` under `app.twilio`. If not set, SMS OTP is only logged (e.g. "SMS to 8408972717: Your verification OTP is: 615213").

---

## 5. Other app config

| Placeholder           | Description              | Example |
|-----------------------|--------------------------|---------|
| `JWT_SECRET`          | JWT signing secret (256+ bits) | Long random string |
| `JWT_ACCESS_TTL`      | Access token TTL (ms)    | `3600000` |
| `JWT_REFRESH_TTL`     | Refresh token TTL (ms)   | `604800000` |
| `FRONTEND_URL`        | Allowed CORS origin      | `http://localhost:4200` or your frontend URL |
| `STRIPE_API_KEY`      | Stripe API key (payments)| From Stripe Dashboard |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook secret  | From Stripe Webhooks |
| `GOOGLE_MAPS_API_KEY` | Google Maps (optional)   | From Google Cloud Console. **Frontend:** Set `googleMapsApiKey` in `frontend/src/environments/environment.ts` and `environment.prod.ts` for property map and drop-pin. |

---

## Quick copy-paste (env file style)

Use these as placeholders; replace with your real values only in a local `.env` or in your host’s env config (never commit secrets).

```bash
# PostgreSQL
DB_HOST=localhost
DB_PORT=5432
DB_NAME=realestate
DB_USER=postgres
DB_PASSWORD=your_db_password

# SMTP (email OTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USER=your-email@gmail.com
MAIL_PASSWORD=your_app_password

# Firebase (mobile OTP – when implemented)
# FIREBASE_PROJECT_ID=your-project-id
# FIREBASE_PRIVATE_KEY=your-private-key
# FIREBASE_CLIENT_EMAIL=firebase-adminsdk-xxx@your-project.iam.gserviceaccount.com

# Twilio (optional, current SMS)
# TWILIO_ACCOUNT_SID=your-sid
# TWILIO_AUTH_TOKEN=your-token
# TWILIO_FROM_NUMBER=+1234567890

# App
JWT_SECRET=your-256-bit-secret
FRONTEND_URL=http://localhost:4200
```
