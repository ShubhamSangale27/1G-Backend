# Development Context – Conversation Summary

**Last discussion:** Gitignore setup (node_modules, package-lock.json, .idea, .angular)  
**Date:** 2025-01-30

This document summarizes conversation context and changes made during development for future reference.

---

## 1. Site Visit Booking & Reschedule

- **Property detail:** Reschedule option for site visits; prevent duplicate bookings for the same property.
- **Files:** `frontend/src/app/property-detail/property-detail.component.ts`, `.html`, `.scss`
- **Behaviour:** `loadMyVisitForProperty`, `openReschedule`, `submitReschedule`; conditional display of booking vs reschedule.

---

## 2. Price Range Slider & UI Alignment

- **Search:** Price filter inputs replaced with a dual-thumb HTML range slider.
- **Files:** `frontend/src/app/features/search/search.component.ts` and template; `frontend/src/styles.scss` and component SCSS.
- **Note:** Resolved TS1206 by placing constants correctly; CSS alignment fixes for slider and layout.

---

## 3. Auth: Firebase Removed, MSG91 OTP Only

- **Backend:** Removed Firebase (FirebaseService, FirebaseConfig, dependencies). OTP sent via **MSG91** only.
- **Frontend:** Removed Firebase package and `firebase.service.ts`. Verify-OTP flow is mobile OTP only (no Firebase/Recaptcha).
- **Config:** `app.msg91` in `application.yml`; see `MSG91_CREDENTIALS.md` for setup.

---

## 4. Backend Configuration Consolidation

- Single config: `backend/src/main/resources/application.yml` and `application-heroku.yml`.
- Database, JWT, OTP (MSG91), Stripe, Google Maps, SMTP, etc. in one place; env vars supported.

---

## 5. Email Verification (Optional, After Signup)

- **Backend:** `EmailVerificationToken` entity, Flyway `V6__email_verification_tokens.sql` (use `TIMESTAMP` not `TIMESTAMPTZ`), `AuthService.sendEmailVerificationLink` / `verifyEmailByToken`; SMTP via `JavaMailSender` (optional).
- **Frontend:** `/verify-email?token=...` route and component; dashboard banner to “Send verification link” when `!auth.user()?.emailVerified`.

---

## 6. NG0100 (ExpressionChangedAfterItHasBeenCheckedError) Fixes

- **Cause:** State and DOM updates (toast, navigation, loading flags) in the same change-detection cycle.
- **AuthService:** `login` and `signup` – toast and `router.navigate` wrapped in `setTimeout(..., 0)`; error toasts deferred similarly.
- **DashboardComponent:** `sendEmailVerificationLink()` – `sendingEmailVerification` and toasts updated inside `setTimeout(..., 0)`; `loadVisits()` and `loadRecentProperties()` – assignments to `visits`, `loadingVisits`, `recentProperties`, `loadingProperties` (and error resets) deferred with `setTimeout(..., 0)`.
- **VerifyOtpComponent:** Already used `setTimeout` for toast and navigate after OTP success.

---

## 7. Send Email Verification 500 Fix

- **Backend:** When mail was not configured, a `log.warn` in `AuthService.sendEmail()` used `body.substring(...)` in a way that could throw (e.g. indexOf ordering). Replaced with a safe snippet (e.g. from `"http"` to next newline) so logging never throws.
- **AuthController:** Null check for `principal` on `/send-email-verification`; return `401 Unauthorized` instead of NPE/500 when not authenticated.

---

## 8. MSG91 Test OTP (No DLT)

- **Config:** `app.otp.test-otp` in `application.yml` – when set (e.g. `"123456"`), backend accepts that OTP and skips MSG91 API for testing.
- **OtpService:** Sends via MSG91 when configured; fallback/log when auth key missing; test-otp bypass when `app.otp.test-otp` is set.

---

## 9. Other Frontend Fixes

- **config.json 404:** Added `frontend/src/assets/config.json` and ensured `angular.json` serves it.
- **TypeScript/Zone:** `typescript` ~5.9.0, `zone.js` ~0.16.0; `tsconfig` `moduleResolution: "bundler"`; removed unnecessary optional chaining where it caused issues (e.g. admin, agent-visit-detail).

---

## 10. Git & Repo Setup (Last Discussion – 2025-01-30)

- **Git:** Repo initialized in the folder containing `frontend` and `backend`.
- **.gitignore added/updated at repo root:**
  - `node_modules/`
  - `package-lock.json`
  - `.idea/` (JetBrains IDE – frontend & backend)
  - `.angular/` (Angular CLI cache – e.g. frontend)

If `node_modules` or `package-lock.json` were already committed, remove from index (keep on disk) then commit:

```bash
git rm -r --cached frontend/node_modules
git rm --cached frontend/package-lock.json
# add other paths if needed
git commit -m "Stop tracking node_modules and package-lock.json"
```

---

## Reference Files

| Topic              | Key files / docs |
|--------------------|------------------|
| MSG91 / OTP        | `MSG91_CREDENTIALS.md`, `application.yml` (app.msg91, app.otp) |
| Backend config     | `backend/src/main/resources/application.yml` |
| Auth / email verify| `AuthService.java`, `AuthController.java`, dashboard component |
| NG0100             | `auth.service.ts`, `dashboard.component.ts` |
| Gitignore          | Root `.gitignore` |

---

*Generated for future development context. Update this file when adding major features or resolving recurring issues.*
