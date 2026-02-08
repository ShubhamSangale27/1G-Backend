# Implementation Summary

**Document date:** January 30, 2026

This document lists the main features and fixes implemented in the 1Guntha real estate application.

---

## 1. Auth & Signup Flow

### 1.1 Deferred user creation until OTP verification
- **Backend:** User is **not** created on signup. Signup stores a **pending signup** (email, mobile, password hash, full name, expiry) in table `pending_signups` (Flyway V3).
- **Signup:** `POST /auth/signup` → validate email/mobile not registered → save `PendingSignup` → send email + mobile OTP → return `SignupResponse` (message, email, mobile). No user row is created.
- **Verify signup:** `POST /auth/verify-signup` with `{ email, emailOtp, mobile, mobileOtp }` → verify both OTPs → load `PendingSignup` → create **User** (emailVerified/mobileVerified = true) → delete `PendingSignup` → return **AuthResponse** (tokens + user). User is created only after OTP verification.
- **Frontend:** Signup redirects to `/verify-otp`. After OTP verification, frontend calls `/auth/verify-signup`, receives tokens, and redirects to `/dashboard`.

### 1.2 Redirect after login and signup
- **Login:** On success, user is redirected to **/dashboard** with a “Welcome back, &lt;name&gt;” toast.
- **Verify signup:** On success, user is redirected to **/dashboard** with “Registration complete. You are now logged in.”

### 1.3 Route order fix for List Property
- **Frontend:** Routes reordered so `property/new` and `property/:id/edit` are matched **before** `property/:id`. “List Property” and `/property/new` now show the property form instead of property detail.

---

## 2. Admin Dashboard

### 2.1 Total views metric
- **Backend:** `PropertyAnalyticsRepository.countByType(VIEW)` added. `AdminService.getGlobalMetrics()` includes **totalViews** (total VIEW analytics).
- **Frontend:** Admin metrics grid shows a **Total Views** card (👁).

### 2.2 Assign agent for pending site visits
- **Backend:** `GET /admin/agents` returns users with role AGENT or ADMIN. `PUT /admin/sitevisits/{id}/assign?agentId=` assigns an agent to a pending visit (generates OTP, notifies user).
- **Frontend:** “Pending Site Visit Requests” section lists pending visits with an agent dropdown and “Assign Agent” button.

### 2.3 Manage all site visits
- **Backend:**
  - `GET /admin/sitevisits?page=&size=&from=&to=&agentId=` returns **all** site visits with optional filters:
    - **from** / **to:** ISO date-time (filter by `scheduledAt` range).
    - **agentId:** filter by assigned agent (null = all).
  - Response: `AdminSiteVisitsResponse` with `content`, `totalElements`, `totalPages`, **dueTodayCount** (count of visits due today, status PENDING_ASSIGNMENT or ASSIGNED).
  - Visits are **ordered with due-today first**, then by `scheduledAt` ascending.
- **Frontend:** “Manage All Site Visits” section:
  - **Filters:** From date, To date (datetime-local), Assigned to (dropdown: All agents / specific agent). “Apply filters” loads visits.
  - **Due today:** Badge “Due today: N” at top; visits due today are listed first and rows are highlighted (due-today class).
  - **Table:** Property, User, Scheduled, Assigned to, Status, Reassign (dropdown + Reassign button).
  - **Pagination:** Previous / Next when `totalPages` > 1.

### 2.4 Reassign visit to another agent
- **Backend:** `PUT /admin/sitevisits/{id}/reassign?agentId=` allows reassigning a visit that is **ASSIGNED** or **PENDING_ASSIGNMENT** to another agent. Old visit OTP is replaced; new OTP is generated and user is notified.
- **Frontend:** In “Manage All Site Visits”, each visit that can be reassigned (status PENDING_ASSIGNMENT or ASSIGNED) has an “Assigned to” dropdown and a “Reassign” button.

---

## 3. Seed Users (Local / Dev)

- **DataLoader** (runs when profile is not `test`) creates the following users if they do not exist:
  - **Admin:** `admin@realestate.com` / `admin123`
  - **Agent:** `agent@realestate.com` / `agent123`
  - **Builder (USER):** `builder@realestate.com` / `builder123`
  - **User:** `user@realestate.com` / `user123`  
  All are emailVerified and mobileVerified so they can log in without OTP.

---

## 4. Property Images

### 4.1 Upload file instead of URL
- **Backend:**
  - **Thumbnailator** dependency for image processing.
  - **ImageUploadService:** Accepts multipart image (JPEG, PNG, WebP, GIF, max 10 MB). Produces main image (max 1200×1200, quality 0.85) and thumbnail (300×300, quality 0.8). Saves under `app.upload.dir` (default `uploads/`). Returns `{ url, thumbnailUrl }` (paths like `/uploads/xxx_main.jpg`, `/uploads/xxx_thumb.jpg`).
  - **UploadController:** `POST /upload` (multipart `file`), auth required.
  - **UploadedFileController:** `GET /uploads/{filename}` serves files (public). Security allows `GET /uploads/**` without auth.
- **Frontend:** Property form (list/edit) uses **file upload** per image slot: preview, “Upload” / “Replace”, “Remove”, “+ Add another image”. On file select, `POST /upload` is called; returned URL is stored and sent with the property payload.

### 4.2 Zoomable images on property detail
- **Frontend:** On property detail page, clicking the main image or double-clicking a thumbnail opens a **full-screen overlay** (dark background) with the image. Click overlay, “×” button, or **Escape** closes it. Image URLs are resolved using `ConfigService.apiUrl` for relative paths (e.g. `/uploads/...`).

---

## 5. E2E Tests (Playwright)

- **Location:** Project root `e2e/` and `playwright.config.ts`.
- **Root package.json:** Scripts `e2e`, `e2e:ui`, `e2e:headed`; devDependency `@playwright/test`.
- **Specs:**
  - **auth.spec.ts:** Home load, login/signup navigation, login as user and admin (redirect to dashboard).
  - **property.spec.ts:** My properties, list new property form, search (after login).
  - **admin.spec.ts:** Admin panel load, total views metric, pending site visits section, pending approvals (after admin login).
- **Run:** From project root: `npm install` then `npm run e2e`. Backend and frontend must be running (or use Playwright’s frontend webServer). See `e2e/README.md`.

---

## 6. Database & Migrations

- **V2__seed_data.sql:** Premium plans seed.
- **V3__pending_signups.sql:** Table `pending_signups` for deferred signup.
- **V4** migration was removed (PostgreSQL-only `DO $$` block not supported on H2). If you see “Column USER_ID not found” on an old DB, see ENV_PLACEHOLDERS.md for manual fix.

---

## 7. Configuration Placeholders

- **ENV_PLACEHOLDERS.md** lists placeholders for:
  - PostgreSQL (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD).
  - SMTP (MAIL_HOST, MAIL_PORT, MAIL_USER, MAIL_PASSWORD) for email OTP.
  - Firebase (FIREBASE_PROJECT_ID, FIREBASE_PRIVATE_KEY, FIREBASE_CLIENT_EMAIL) for future mobile OTP.
  - Twilio (current SMS OTP).
  - App (JWT_SECRET, FRONTEND_URL, STRIPE_*, GOOGLE_MAPS_API_KEY, UPLOAD_DIR).

---

## 8. API Summary (Admin & Visits)

| Method | Path | Description |
|--------|------|-------------|
| GET | /admin/metrics | Global metrics (totalProperties, totalViews, pendingProperties, pendingSiteVisits, revenueLast30Days, etc.) |
| GET | /admin/agents | List agents (AGENT + ADMIN users) for assigning visits |
| GET | /admin/sitevisits/pending | Pending site visits (no agent assigned yet) |
| GET | /admin/sitevisits | All visits with filters: `from`, `to` (ISO date-time), `agentId`. Due today first; response includes `dueTodayCount` |
| PUT | /admin/sitevisits/{id}/assign?agentId= | Assign agent (pending only) |
| PUT | /admin/sitevisits/{id}/reassign?agentId= | Reassign visit to another agent (ASSIGNED or PENDING_ASSIGNMENT) |
| POST | /upload | Upload property image (multipart `file`); returns `{ url, thumbnailUrl }` |
| GET | /uploads/{filename} | Serve uploaded file (public) |

---

*Last updated: January 30, 2026*
