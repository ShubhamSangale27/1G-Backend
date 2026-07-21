# 1Guntha Project Context — Source of Truth

Last updated: 2026-07-21 (Devanagari logo rendering — composite lockup web + mobile)

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
- Workspace (`D:\DevSoftwares\1Guntha`) contains three active codebases sharing one backend API:
  - **`1G-Backend`** — Spring Boot API (primary backend)
  - **`1G-Frontend`** — Angular web app (primary web client)
  - **`1G-Mobile`** — Flutter Android/iOS app (native mobile client)
- Legacy/alternate folders may exist as `frontend` + `backend`; prefer **`1G-*`** paths for active development.
- Target feature branch for recent work: **`update/31052026`**
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

Mobile docs (under `1G-Mobile/`):

- `1G-Mobile/PROJECT_CONTEXT.md` — mobile source-of-truth
- `1G-Mobile/README.md` — MVP scope and setup
- `1G-Mobile/01_Project_Analysis.md` — full Angular→Flutter API/feature inventory
- `1G-Mobile/02_Flutter_Migration_Plan.md` — phased migration plan
- `1G-Mobile/03_Flutter_Architecture.md` — Clean Architecture + Riverpod spec
- `1G-Mobile/BUILD_COMMANDS.md` — APK build/install
- `1G-Mobile/ANDROID_DEVICE_SETUP.md` — physical device setup

---

## Full Workspace Map (2026-07-21 scan)

### 1G-Backend (`1G-Backend/`)

| Area | Details |
|------|---------|
| Stack | Spring Boot 3.x, JDK 17, Maven, PostgreSQL, Flyway, JWT, MSG91 OTP |
| API base | `/api` context path |
| Migrations | V1–V14 (latest: `V14__market_statistics.sql`) |
| Public paths | auth, properties search/public, carousel, blogs published, FAQ, market-stats, uploads |
| Admin | metrics, properties, users, site visits, carousel, FAQs, market statistics |
| Seed users | admin@realestate.com, agent@realestate.com, user@realestate.com, blogger@realestate.com |

**Flyway sequence (V1–V14):** init → seed → pending_signups → site_visit_comments → featured → email_verification → media_type → otp_throttling → blog → blog_seo → profile_image → carousel → **faqs** (V13) → **market_statistics** (V14).

### 1G-Frontend (`1G-Frontend/`)

| Area | Details |
|------|---------|
| Stack | Angular 15+ (standalone components), ngx-toastr, Leaflet, Bootstrap-inspired CSS vars |
| Brand | 1Guntha (99acres-inspired UI) |
| Key features | Auth (JWT refresh), search, property CRUD, site visits, admin panel, blog, carousel, FAQ chatbot floater, property growth SIP calculator |
| Location data | `core/data/indian-locations.ts` — State→City only (36 states/UTs) |
| Patterns | `NgZone.run` + `cdr.detectChanges()` after HTTP; `ApiService` + `ConfigService`; guards for auth/admin/agent/blog |

### 1G-Mobile (`1G-Mobile/`)

| Area | Details |
|------|---------|
| Package | `com.oneguntha.one_guntha` |
| Stack | Flutter 3.44+, Dart 3.12+, Riverpod, Dio, GoRouter, flutter_secure_storage |
| API | Same Heroku/backend as web: `https://og-backend-ec80a37e82c0.herokuapp.com/api` (prod domain target: `https://1guntha.com/api`) |
| Architecture | Feature-first Clean Architecture |
| Font | Poppins (mobile); web uses DM Sans + Space Grotesk |
| Roles on mobile | **USER** and **AGENT** only — **ADMIN** and **BLOG blocked** (web-only) |

**Mobile MVP includes:** auth (login/signup/OTP/forgot password), persistent session, home, search, property detail, favorites/watchlist, profile, dashboard, list/edit properties (URL media), site visits, agent panel, blog read-only, caching (stale-while-revalidate).

**Mobile excludes:** admin panel, blog studio, premium payments.

**Key mobile paths:**

```
1G-Mobile/lib/
├── config/          env_config, app_router, route_paths
├── core/            auth, dio, cache, theme, navigation, utils
├── features/        auth, home, search, property, blog, agent, profile, ...
├── presentation/    main_shell (bottom nav)
└── shared/          widgets, models
```

**Mobile test accounts:** user@realestate.com/user123, agent@realestate.com/agent123 (admin/blogger blocked on mobile).

---

## Cross-Platform Feature Matrix

| Feature | Web (1G-Frontend) | Mobile (1G-Mobile) | Backend |
|---------|-------------------|--------------------|---------|
| Auth + JWT refresh | Yes | Yes | `/auth/**` |
| Property search/detail | Yes | Yes | `/properties/**` |
| Site visits + OTP | Yes | Yes | `/sitevisits/**`, `/agent/**` |
| Watchlist/favorites | Toggle on detail | Dedicated tab | `/properties/*/watchlist` |
| Blog read | Yes | Yes | `/blogs/published/**` |
| Blog editor | Yes (BLOG role) | No | `/blogs/editor/**` |
| Admin panel | Yes | No | `/admin/**` |
| Homepage carousel | Yes | Yes | `/carousel/slides` |
| FAQ chatbot floater | Yes (all pages) | Not yet | `/faq/**` |
| SIP growth calculator | Yes (home) | Not yet | `/market-stats/**` |
| Market stats admin | Yes | No | `/admin/market-stats/**` |
| Indian price format | `IndianPricePipe` | Mobile formatter | — |
| Media (YouTube/Drive) | embed + native | webview + resolver | URL stored as-is |

---

## Data & Integration Notes

- **OTP:** MSG91 (not Twilio/Firebase in current direction); signup uses mobile OTP via `POST /auth/verify-signup`.
- **Media:** Property media is URL-based on web; profile photo uses `POST /upload` on web. On mobile, profile photo is **device-local only** (gallery pick → app documents; no `/upload`). YouTube + Google Drive URL resolution in `image-url.util.ts` (web) and `media_url_resolver.dart` (mobile).
- **Search gap:** Frontend state dropdown exists but `state` param is **not sent** to search API (only `city` and other filters).
- **Market statistics:** Admin-managed primary source; optional RBI-style seed for major cities; public endpoints drive SIP calculator on home page.
- **FAQ chatbot:** Keyword matching against admin FAQs; unmatched questions logged for admin; fallback: **"Just Missedcall on 9134913491, will help you!"**

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

### 2026-06-09 — Header shows auth tabs when not logged in (fix)

- **Problem:** My Properties, List Property, Agent, Admin, and Blog Studio sometimes appeared for logged-out users due to stale `user` in `localStorage` without a valid `accessToken`, and session expiry cleared storage without resetting `AuthService` in-memory state.
- **Frontend:**
  - `auth.service.ts` — `hydrateSessionFromStorage()` only restores session when token + user both exist; `clearSession()` centralizes logout/expiry cleanup; `getRole()` reads from user signal.
  - `auth-refresh.interceptor.ts` — calls `auth.clearSession()` on refresh failure; syncs user signal after successful refresh.
  - `header.component.ts` — nav gated on `auth.isLoggedIn()`; role links use `u.role` from user object.
  - `auth.guard.ts`, `admin.guard.ts`, `agent.guard.ts`, `blog.guard.ts` — use `AuthService` instead of raw `localStorage`.
  - `auth.service.spec.ts` — unit tests for hydration and clearSession.
- **Verification:** `npx tsc -p tsconfig.app.json --noEmit` succeeded.

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

### 2026-07-21 — Full workspace context scan (web + mobile)

- Scanned all `.md` files under `1Guntha/` including newly cloned **`1G-Mobile/`** Flutter app.
- Consolidated mobile context into this file: stack (Flutter/Riverpod/Dio/GoRouter), MVP scope, role policy (USER/AGENT only), caching, navigation, test accounts, build commands.
- Documented three-codebase workspace map and cross-platform feature matrix.
- No runtime code changed by this documentation action.

### 2026-07-21 — FAQ chatbot (V13) + unmatched question admin

- **Backend:**
  - Flyway `V13__faqs.sql` — `faqs`, `unmatched_faq_questions` tables.
  - `FaqService`, `FaqController` (public `/faq/**`), `AdminFaqController` (admin CRUD + resolve/promote unmatched).
  - Keyword scoring match; unmatched questions stored with user (or Unknown).
  - Fallback answer: **"Just Missedcall on 9134913491, will help you!"**
  - `SecurityConfig` — `/faq/**` public; admin metrics include `unmatchedFaqPending`.
- **Frontend:**
  - `FaqChatbotComponent` — right-bottom floater on all pages via `app.component.ts`.
  - Admin sections: Manage FAQs, Out-of-scope Questions (resolve/promote).
  - Admin modal z-index raised above chatbot floater.

### 2026-07-21 — Area-based SIP property growth calculator (V14 market stats)

- **Backend:**
  - Flyway `V14__market_statistics.sql` — `market_areas`, `market_stat_snapshots`, `market_data_refresh_runs`.
  - Entities/repos/services: `MarketArea`, `MarketStatSnapshot`, `MarketStatsService`.
  - Public: `GET /market-stats/areas`, `GET /market-stats`, `POST /market-stats/projection`.
  - Admin: `/admin/market-stats/**` (area/snapshot CRUD, RBI-style seed import).
  - `RbiHpiSeedProvider` — embedded free/open-style seed data (no paid API); `MarketStatsSeedRunner` auto-seeds on startup (non-test profile).
  - Projection engine: CAGR from snapshots, historical + forecast lines.
- **Frontend:**
  - `MarketStatsService` + upgraded `property-growth-calculator.component.ts` on home page.
  - State/City from `indian-locations.ts`; Location (LOCALITY) from backend admin catalog.
  - Range tabs: YTD, 1Y, 3Y, 5Y, 10Y, MAX; SVG chart with history/forecast split.
  - Admin: Market Statistics section (areas, snapshots, seed import).
- **Tests:** `MarketStatsServiceTest`, `MarketStatsControllerTest`, calculator component spec (4/4 passed).

### 2026-07-21 — Mobile local profile photo (device-only)

- **Mobile (`1G-Mobile`):**
  - Added `path_provider` and `LocalProfilePhotoService` — gallery pick copies image to `{appDocuments}/profile_photos/{userId}.jpg`; path stored in secure storage per user.
  - `ProfileScreen` — no `POST /upload`; avatar priority: local file → server `profileImageUrl` → initials; remove deletes local file only; **Save profile** sends name/email only (no `profileImageUrl`).
  - Local photos persist across logout/re-login on same device (scoped by `userId`).
- **Tests:** `local_profile_photo_service_test.dart` (save/get/remove).
- **Out of scope:** Backend/web profile photo flow unchanged; no sync to other devices.

### 2026-07-21 — Admin push notifications (FCM) for mobile

- **Backend (V15):** `device_tokens`, `push_campaigns` tables; `DeviceTokenController` (`POST/DELETE /devices/fcm-token`); `AdminPushNotificationController` (`POST/GET /admin/push-notifications`); `PushNotificationService` via Firebase Admin SDK (`FIREBASE_ENABLED`, `FIREBASE_CREDENTIALS_PATH`).
- **Web admin:** Push Notifications (Mobile) section — title, message, image URL, link URL, in-app vs browser, target role (ALL/USER/AGENT), campaign history.
- **Mobile:** `firebase_core`, `firebase_messaging`, `flutter_local_notifications`; token register on login, unregister on logout; tap routes to go_router or external browser.
- **Docs:** `1G-Mobile/PUSH_NOTIFICATIONS_SETUP.md` — Firebase project, google-services.json, service account, testing steps.
- **FCM cost:** Free for standard message delivery.

### 2026-07-21 — Delete account (self-service + admin)

- **Problem:** Users could not close their own accounts; admin delete lacked shared cleanup and last-admin protection.
- **Backend:**
  - `DELETE /users/me` — authenticated self-delete; body `{ "password": "..." }`; verifies password, clears refresh/device/email-verification tokens, deletes user and cascaded data; returns `204 No Content`. Admin role cannot self-delete.
  - `DELETE /admin/users/{id}` — existing admin delete extended via `UserAccountDeletionService`; no password; blocks self-delete and deleting the last admin; returns `{ "deleted": true }`.
  - New: `DeleteAccountRequest`, `UserAccountDeletionService`; `UserRepository.countByRole`, `DeviceTokenRepository.deleteByUserId`.
- **Web:** Profile page “Delete account” danger card (non-admin) with password + confirm dialog; `AuthService.deleteAccount()`.
- **Mobile:** Profile “Delete account” card with password + confirm dialog; `AuthRemoteDatasource.deleteAccount()` + repository clears session and local profile photo.
- **Tests:** `UserAccountDeletionControllerTest`, `AdminUserDeletionControllerTest`.
- **Migration:** None (existing FK cascades sufficient).
- **Verification:** `mvn test` (new deletion tests).

### 2026-07-21 — Unified 1Guntha brand logo (web + mobile)

- Replaced `1G_logo.png` in `1G-Frontend/src/assets/images/` and `1G-Mobile/assets/images/` with the official vertical logo (roof + G/1 mark, 1GUNTHA.COM, Marathi tagline).
- **Web:** `BrandLogoComponent` responsive variants (`compact`, `auth`, `footer`, `splash`) using `clamp()` / `min()` for header, auth, footer; favicon and apple-touch-icon updated in `index.html`.
- **Mobile:** `AppLogo` height/width scaling by screen size; regenerated Android launcher + adaptive icons (`flutter_launcher_icons`, white adaptive background).

### 2026-07-21 — Account delete: owned properties and associated data

- **Problem:** Deleting a user who owned listed properties caused FK/SQL errors because JPA does not cascade-delete uninitialized lazy collections (`User.properties`, `Property.siteVisits`, etc.).
- **Backend:**
  - Expanded `UserAccountDeletionService` with explicit ordered deletion: owned properties (and each property’s site-visit comments, OTPs, site visits, watchlist entries, images, analytics) → remaining user-scoped rows (comments, site visits, agent assignments cleared, watchlist, alerts, payments, blog posts) → tokens → user.
  - New repository bulk methods: `PropertyRepository.findAllByOwnerId`, `SiteVisitRepository.deleteByPropertyId`, `WatchlistRepository.deleteByPropertyId`, `PropertyAnalyticsRepository.deleteByPropertyId`, `SiteVisitCommentRepository.deleteBySiteVisitPropertyId`, `VisitOTPRepository.deleteBySiteVisitPropertyId`, plus existing `deleteByUserId` / `deleteByAuthorId` helpers.
  - `SiteVisit` entity: `@OneToMany(cascade = ALL, orphanRemoval = true)` on `comments` for JPA consistency.
- **Tests:** `UserAccountDeletionServiceTest.deleteUser_withOwnedPropertyAndDependents_removesAllRows`, `UserAccountDeletionControllerTest.deleteMyAccount_withOwnedProperty_deletesUserAndProperty`.
- **Migration:** None (application-layer fix; existing schema FK rules unchanged).
- **Verification:** `mvn test "-Dtest=UserAccountDeletionServiceTest,UserAccountDeletionControllerTest,AdminUserDeletionControllerTest"` — 11/11 passed.

### 2026-07-21 — Market statistics admin fixes + production hardening

- **Problem:** Admin panel location/statistics updates appeared not to persist or reflect on the public growth calculator; parent ID was a raw number (easy misconfiguration); calculator used static `indian-locations.ts` while admin edits DB areas; duplicate snapshot dates caused opaque 500 errors; mobile had truncated location list and silent dashboard failures.
- **Backend:**
  - `MarketStatsService` — multi-level snapshot fallback (locality → city → state); duplicate snapshot guard with clear `BadRequestException`; RBI seed re-import now refreshes existing area metadata.
  - `GlobalExceptionHandler` — friendly message for `DataIntegrityViolationException` on snapshot conflicts.
- **Web admin:** Parent dropdown (STATE/CITY filtered by level), auto-filled state/city names, sort order field, active checkbox hint, snapshot panel shows area name, validation before save.
- **Web calculator:** State/city lists loaded from `/market-stats/areas` API (admin data is source of truth) with static fallback when API empty.
- **Mobile:** Synced `indian_locations.dart` to full 36-state web dataset via `scripts/sync-indian-locations.js`; dashboard shows `ErrorView` + retry on load failures instead of silent catch.
- **Verification:** `mvn test MarketStatsServiceTest,MarketStatsControllerTest`; Angular growth calculator spec 4/4; Flutter 30/30.
- **Follow-up:** Mobile property growth projector widget not yet ported (web-only today); admin market-stats CRUD remains web-only by design.

### 2026-07-21 — Devanagari logo rendering (web + mobile)

- **Problem:** Logo PNG had black background and embedded Devanagari tagline that became illegible when scaled down in headers (28–36px).
- **Solution:** Composite brand lockup — transparent `1G_logo_lockup.png` (icon + wordmark) + live Devanagari text **घर प्रत्येकासाठी** via Noto Sans Devanagari; enlarged compact/header sizes.
- **Assets:** `scripts/prepare-brand-logo.ps1` generates `1G_logo_full.png`, `1G_logo_lockup.png` (+@2x/@3x), `1G_logo_mark.png` for favicon/launcher.
- **Web:** `BrandLogoComponent` stacked lockup; `index.html` Noto Sans Devanagari font + mark favicon; header min-height increased.
- **Mobile:** `AppLogo` Column with lockup + tagline; `SliverAppBar` toolbarHeight 72; launcher icons use mark-only crop.
- **Verification:** Flutter branding + app_logo + widget tests passed; launcher icons regenerated.

