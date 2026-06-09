# 1Guntha Project Context — Source of Truth

Last updated: 2026-06-09 (carousel taller height + auto-cycle)

This is the canonical context document for this workspace. It merges the most relevant information from historical markdown notes across `frontend`, `backend`, `1G-Frontend`, and `1G-Backend`.

---

## Mandatory Maintenance Rule

**This file is the single changelog for the entire 1Guntha workspace.** Every change made to `1G-Frontend`, `1G-Backend`, config, infra, or docs must be recorded here in the same session — no exceptions.

Applies to all work going forward (from 2026-06-09 onward), including:
- Features, bug fixes, refactors, and UX tweaks
- API, schema, or migration changes
- Environment, deployment, and CI/CD updates
- Dependency or tooling upgrades that affect behavior

**Do not** create separate one-off context `.md` files for new work; append to `## Change Log` below instead.

### Update checklist (required after every change)

1. Set **Last updated** at the top of this file (date + short summary).
2. Add a dated entry under `## Change Log` (newest last).
3. Include **what changed and why** (problem → solution).
4. List **impacted files/modules** (paths under `1G-Frontend/` and `1G-Backend/`).
5. Add **verification notes** (build, type-check, tests, or manual steps run).
6. Note **follow-up items** if anything is partial or deferred.

Do not leave this file stale after code or config changes. If a session ends without updating this file, the work is incomplete.

---

## Project Overview

- Product name: `1Guntha` (rebranded from RealEstate naming in older files).
- Purpose: full-stack real-estate platform (buy/sell/rent, property management, search, auth, admin, site visits, analytics-like metrics).
- Workspace has parallel app folders:
  - `frontend` + `backend`
  - `1G-Frontend` + `1G-Backend`
- Primary stack:
  - Frontend: Angular
  - Backend: Spring Boot (Java 17, Maven)
  - Database: PostgreSQL (H2 in tests in some flows)
  - Auth: JWT access + refresh
  - Migrations: Flyway
  - Notifications/OTP: MSG91 is the current direction in newer docs; older docs reference Twilio/Firebase placeholders.

---

## Current Cross-Stack Baseline

### Backend

- API context path is `/api`.
- Security expects JWT on protected routes; public routes include selected auth/public property paths.
- Site visit optional-resource endpoint behavior updated:
  - `GET /sitevisits/my/for-property/{propertyId}` returns `204` when no active visit exists (instead of `404`) in newer context.
- Admin/user/property/site-visit operations are documented and generally aligned with frontend usage.

### Frontend

- Interceptor strategy includes:
  - Auth token attachment
  - Global error handling
  - Auth refresh handling on `401` with retry guard
- Refresh flow uses non-recursive pattern (refresh call isolated from normal interceptor recursion).
- Error handling improvements include utility-based message extraction and optional suppression of duplicate global toasts.
- Toast z-index and modal layering were adjusted in past sessions to avoid hidden UI overlays.

---

## Key Functional Areas and Status

### Authentication and Session

- Login/signup/verify OTP flows exist and have had multiple rounds of UX and error-surface improvements.
- Refresh token flow and 401 retry behavior were hardened in recent context notes.
- Deactivated/suspended account handling is explicitly surfaced in auth messaging.

### Property Media

- Media model expanded from image-only to image + video.
- URL handling supports:
  - YouTube variants
  - Google Drive file links
  - Direct/native file URLs
- Rendering paths distinguish embed vs native playback and sanitize embed sources where required.
- Card/detail/form behavior includes video-aware fallbacks and poster/thumbnail handling.

### Admin Capabilities

- Admin can manage users (list, activate/suspend, delete with safety constraints).
- Admin property management and moderation flows are present.
- Site-visit assignment/reassignment and dashboard-style metrics workflows are documented.

### UI/UX and Branding

- Rebranding to `1Guntha` has been applied across major frontend copy and metadata in the corresponding tracks.
- Multiple design enhancement passes (99acres-inspired visual system) are documented.

---

## Run and Deployment Context

- Local run and Docker flow documented under backend docs.
- CI/CD workflows documented for GitHub Actions (`ci` + optional deploy strategy).
- Hosting guidance exists for Render/Fly/Oracle/Railway/DigitalOcean.
- Heroku deployment documentation includes subtree deployment approach for split frontend/backend app deployment.

---

## Configuration and Secrets Context

- Core env areas:
  - DB: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
  - JWT: `JWT_SECRET` and TTL controls
  - CORS/front URL: `FRONTEND_URL`
  - Mail: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD`
  - OTP (current): MSG91 (`MSG91_AUTHKEY`, optional sender)
  - Optional/legacy references: Twilio/Firebase placeholders appear in older docs
  - Storage/media: Google Drive credentials/folder envs where configured
- Rule: never commit real secret values into repo docs or source.

---

## Known Historical Issues and Lessons

- Angular UI state sometimes needed explicit zone/change-detection patterns in older sessions.
- Node version mismatches have blocked frontend production builds in some environments.
- Firebase dependency/classpath mismatch was noted in at least one backend test/runtime context.
- Google Drive service-account uploads require Shared Drive membership and correct credentials/folder configuration.

---

## Canonical Reference Documents

Use these first when reconstructing recent context:

- `CONTEXT_2026-04-19_FRONTEND_BACKEND.md`
- `1G-Backend/docs/CONTEXT_2026-04-14_ADMIN_MEDIA_USER_LIFECYCLE.md`
- `1G-Backend/docs/PROJECT_CONTEXT.md`
- `1G-Backend/docs/CONTEXT_2026-01-30.md`

Supporting operational docs:

- `1G-Backend/docs/RUNNING.md`
- `1G-Backend/docs/CI_CD.md`
- `1G-Backend/docs/HEROKU.md`
- `1G-Backend/docs/HOSTING.md`
- `1G-Backend/GOOGLE_DRIVE_SETUP.md`
- `1G-Backend/docs/MSG91_CREDENTIALS.md`

---

## Change Log

### 2026-04-29

- Created this source-of-truth context by consolidating all markdown context docs under the workspace recursively.
- Established mandatory rule to update this document after every meaningful project change.
- No code/runtime behavior changed by this documentation action.

### 2026-04-29 (Indian price formatting)

- Added a reusable Angular standalone pipe `IndianPricePipe` at `1G-Frontend/src/app/shared/pipes/indian-price.pipe.ts`.
- Updated property value displays to Indian compact style (Lakhs/Crores) across:
  - `1G-Frontend/src/app/shared/property-card/property-card.component.ts`
  - `1G-Frontend/src/app/features/property-detail/property-detail.component.ts`
  - `1G-Frontend/src/app/features/agent/agent-visit-detail.component.ts`
  - `1G-Frontend/src/app/features/admin/admin.component.ts`
  - `1G-Frontend/src/app/features/dashboard/dashboard.component.ts`
- Display behavior:
  - `>= 1,00,00,000` -> `Cr`
  - `>= 1,00,000` -> `L`
  - below lakh -> Indian comma format via `en-IN`

### 2026-05-08 (Blog feature + blog role)

- Implemented full Blog module across backend and frontend with WordPress-style editor/dashboard.
- Backend:
  - Added blog schema migration `V9__blog_posts.sql`:
    - `blog_posts`
    - `blog_content_blocks`
  - Added entities:
    - `BlogPost`
    - `BlogContentBlock` (supports `TEXT`, `IMAGE`, `VIDEO`, `LINK`)
  - Added repository:
    - `BlogPostRepository`
  - Added DTOs:
    - `BlogPostDto`
    - `BlogContentBlockDto`
    - `BlogPostCreateUpdateRequest`
  - Added service + controller:
    - `BlogService`
    - `BlogController`
  - Added role support:
    - `User.Role.BLOG`
    - Secured editor APIs with `BLOG` or `ADMIN`
    - Admin role-change now supports `ADMIN`, `AGENT`, `BLOG`, `USER`
  - Added seeded dummy blog user:
    - `blogger@realestate.com / blog123`
  - Extended existing upload flow (`/upload`) to support video MIME types, reusing same storage/Drive pipeline.
- Frontend:
  - Added blog models and guard:
    - `core/models/blog.model.ts`
    - `core/guards/blog.guard.ts`
  - Added public blog UI:
    - `features/blog/blog-list.component.ts`
    - `features/blog/blog-detail.component.ts`
  - Added editor dashboard:
    - `features/blog-editor/blog-editor-dashboard.component.ts`
    - create/edit/delete/publish/unpublish and ordered mixed blocks
    - media uploads via existing `api.uploadFile('/upload', file)` flow
  - Routing + nav integration:
    - public routes: `/blog`, `/blog/:slug`
    - editor route: `/blog-editor` (guarded)
    - header links for Blog and Blog Studio
  - Admin user-role UI updated to include `USER` and `BLOG`.
- Verification:
  - Backend build: `mvn -DskipTests package` succeeded
  - Frontend type-check: `npx tsc -p tsconfig.app.json --noEmit` succeeded

### 2026-05-31 — User profile management and OTP password reset

- Backend:
  - Migration `V11__user_profile_image.sql` adds `profile_image_url` on `users`.
  - Profile API: `GET /users/me`, `PUT /users/me` with `fullName`, `email`, `profileImageUrl` (mobile is not editable).
  - Password reset (public): `POST /auth/forgot-password` (OTP to registered mobile), `POST /auth/reset-password` (email + OTP + new password).
  - Password change (authenticated): `POST /auth/change-password/send-otp`, `POST /auth/change-password`.
  - `OtpService.verifyOtpCode()` validates OTP without toggling email/mobile verification flags.
  - Invalidates refresh tokens after password reset/change.
- Frontend:
  - New `/profile` page: photo upload, edit name/email, read-only mobile, in-profile password change with OTP.
  - New `/forgot-password` page linked from login.
  - Header avatar links to profile and shows uploaded photo when set.
  - `AuthService` extended with profile and password APIs; `User.profileImageUrl` added.
- Verification:
  - Backend build: `mvn -DskipTests package` succeeded
  - Frontend type-check: `npx tsc -p tsconfig.app.json --noEmit` succeeded

### 2026-05-31 — Blog UI fixes and site visit OTP flow

- Blog: blank detail page fixed (loading/error states, param subscription); stable slugs on edit; production styling on list/studio; save feedback banner + toast; global rich-text CSS.
- Site visits: SMS OTP on agent assignment; `GET/POST /sitevisits/{id}/otp|resend-otp`; OTP shown on property detail; agent list upcoming-first with inline OTP complete and filter tabs.
- Verification: backend build and frontend type-check succeeded.

### 2026-06-09 — Admin-configurable homepage carousel (URL-based)

- **Problem:** Homepage carousel used 3 hardcoded static images; admins could not manage banners without a code deploy.
- **Backend:**
  - Flyway `V12__carousel_slides.sql` — table `carousel_slides` (image_url, link_url, alt_text, display_order, active).
  - Entity `CarouselSlide`, `CarouselSlideRepository`, DTOs (`CarouselSlideDto`, create/update requests).
  - `CarouselService` — URL validation, CRUD, ordered listing.
  - Public `GET /carousel/slides` (`CarouselController`) — active slides only.
  - Admin `GET/POST/PUT/DELETE /admin/carousel/slides` (`AdminController`).
  - `SecurityConfig` — `/carousel/**` added to public paths.
- **Frontend:**
  - `core/models/carousel.model.ts`
  - `home.component.ts` — loads slides from API; `resolvePropertyImageUrl` for display; falls back to `assets/images/carousel/1–3.jpg` when none configured; optional click-through links; CSS `object-fit: cover` auto-fits any image size.
  - `admin.component.ts` — **Homepage Carousel** section: add/edit/delete slides via image URL (no upload), live preview, reorder, active toggle.
- **Verification:**
  - Backend build: `mvn -DskipTests package` succeeded
  - Frontend type-check: `npx tsc -p tsconfig.app.json --noEmit` succeeded

### 2026-06-09 — Homepage carousel taller height + auto-cycle

- **Problem:** Carousel felt too short (220px) and did not auto-advance; slides load asynchronously so ngx-bootstrap interval never started.
- **Frontend** (`1G-Frontend/src/app/features/home/home.component.ts`):
  - Increased max height to **330px** (1.5×) via `--home-carousel-max-height` CSS variable.
  - Configured carousel: `interval` 5000ms, `noWrap` false (cyclic), `noPause` true, `isAnimated` true, indicators when 2+ slides.
  - Wrapped `<carousel>` in `*ngIf="displayCarouselSlides.length"` so auto-play starts after slides load.
- **Verification:** Frontend type-check succeeded.

### 2026-06-09 — Mandatory changelog rule (all future work)

- **Policy:** All workspace changes must be logged in this file (`PROJECT_CONTEXT_SOURCE_OF_TRUTH.md`) in the same session; no separate ad-hoc context `.md` files for routine work.
- Strengthened `## Mandatory Maintenance Rule` with explicit scope, checklist, and “work incomplete if changelog missing” guidance.
- Added Cursor rule `.cursor/rules/update-context-source-of-truth.mdc` (`alwaysApply: true`) so agents update this file after every code/config change.

### 2026-06-09 — Property form Add URL and multiple media fix

- **Problem:** Add URL appeared broken after the first media item; users could not reliably add multiple images/videos. Root causes: ngx-toastr `preventDuplicates` suppressed repeated success toasts, strict URL validation rejected common CDN links, and backend did not persist `mediaType` on save.
- **Frontend** (`1G-Frontend/src/app/features/property-form/property-form.component.ts`):
  - Unique success toasts with count (`Image added (2 total)`) so duplicate prevention does not hide feedback.
  - URL normalization: auto-prepend `https://`, `upgradeInsecureMediaUrl()`; relaxed image validation accepts any valid `http(s)://` URL.
  - Media URL input changed from `type="url"` to `type="text"`.
  - Immutable `mediaItems` updates + `ChangeDetectorRef.markForCheck()` on add/remove.
  - Duplicate URL guard; inline media count; `onMediaUrlEnter()` prevents accidental form submit on Enter.
  - Video preview fallback placeholder when embed URL cannot be resolved.
- **Backend** (`1G-Backend/src/main/java/com/realestate/service/PropertyService.java`):
  - `create()` and `update()` now persist `mediaType` from request (defaults to `IMAGE` when omitted).
- **Verification:**
  - Backend build: `mvn -DskipTests package` succeeded
  - Frontend type-check: `npx tsc -p tsconfig.app.json --noEmit` succeeded

