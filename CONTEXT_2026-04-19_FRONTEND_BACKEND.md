# 1Guntha — Frontend & Backend Change Log (Context for Future Work)

**Date:** 2026-04-19  
**Scope:** Changes made across development sessions to the **1G-Frontend** (Angular) and **1G-Backend** (Spring Boot) codebases under this workspace.

This document is intended for future AI sessions, onboarding, and release notes. It complements existing docs such as `1G-Backend/docs/PROJECT_CONTEXT.md`, `1G-Backend/docs/CONTEXT_2026-01-30.md`, and `1G-Frontend/CONTEXT_2026-04-14.md`.

---

## 1. Backend (`1G-Backend`)

### 1.1 Code changes in these sessions

| Change | Location |
|--------|----------|
| **`GET /sitevisits/my/for-property/{propertyId}`** returns **204 No Content** when the user has no active visit for that property (previously **404**). This matches “optional resource” semantics and avoids red **404** entries in the browser network log for the normal case. | `SiteVisitController.getMyVisitForProperty` |

Other behaviour still relies on existing APIs:

| Capability | Existing endpoint / behaviour |
|--------------|-------------------------------|
| JWT access + refresh | `POST /api/auth/refresh` with body `{ "refreshToken": "..." }` (see `AuthController`, `AuthService.refresh`) |
| Site visits (authenticated) | `POST /api/sitevisits`, `PUT /api/sitevisits/{id}/reschedule`, `GET /api/sitevisits/my`, etc. (`SiteVisitController`) |
| Security | `SecurityConfig`: public paths include `/auth/**` (except authenticated-only auth routes), `/properties/public/**`, etc.; other routes require a valid JWT |

### 1.2 Operational / configuration reminders (unchanged but relevant)

- API base path: **`/api`** (`server.servlet.context-path` in `application.yml` / `application-heroku.yml`).
- Google Drive uploads and public URLs are described in **`1G-Backend/GOOGLE_DRIVE_SETUP.md`**.
- OTP / MSG91: **`1G-Backend/docs/MSG91_CREDENTIALS.md`**.

If future work touches **video storage** (e.g. forcing direct MP4 URLs from your own CDN instead of YouTube/Drive embeds), that would be a **new** backend or storage change and should be appended here.

---

## 2. Frontend (`1G-Frontend`) — Summary

### 2.1 Authentication, HTTP errors, and session refresh

**Problems addressed**

- **401** on authenticated calls (e.g. book/reschedule site visit) when the **access JWT expired** but a **refresh token** was still valid.
- **Login / signup** not showing clear **toast** errors; duplicate toasts from global interceptor vs auth screens.
- **ngx-toastr** sometimes appearing **under** fixed UI (loan calculator, WhatsApp button, modals).

**New / updated files**

| File | Purpose |
|------|---------|
| `src/app/core/interceptors/auth-refresh.interceptor.ts` | On **401**, if the request is not an anonymous auth URL and a **refresh token** exists, calls **`POST /auth/refresh`** via **`HttpBackend`** (bypasses interceptors), updates **`localStorage`** tokens (and user if returned), then **retries the original request once** with header **`X-1g-Auth-Retry: 1`** to avoid loops. On refresh failure: clears auth storage and shows a **session expired** toast. |
| `src/app/core/utils/http-error-message.util.ts` | **`extractHttpErrorMessage(err)`** — parses `message` / `error` from JSON bodies and sensible defaults by HTTP status. |
| `src/app/core/http-context.tokens.ts` | **`SKIP_GLOBAL_ERROR_TOAST`** — `HttpContextToken` so specific flows can show their own message without the global interceptor duplicating. |

**Updated files**

| File | Change |
|------|--------|
| `src/app/app.config.ts` | HTTP interceptors order: **`[authInterceptor, errorInterceptor, authRefreshInterceptor]`** — refresh runs **innermost** on the response chain so it can handle **401** before the generic error handler. |
| `src/app/core/interceptors/error.interceptor.ts` | Uses **`extractHttpErrorMessage`**. **Does not toast on 401** (refresh + auth screens own the copy). Skips toast when **`req.context.get(SKIP_GLOBAL_ERROR_TOAST)`** is true. |
| `src/app/core/services/api.service.ts` | **`post`** / **`put`** accept optional **`HttpContext`** (e.g. skip global error toast). |
| `src/app/core/services/auth.service.ts` | **`login`** / **`signup`** error handlers call **`toast.error(extractHttpErrorMessage(...))`**. Requests pass **`skipGlobalErrorToast`** context. **`resendSignupOtp`** also passes that context. |
| `src/app/features/auth/verify-otp/verify-otp.component.ts` | Verify + resend use **skip-global-toast** context and **`extractHttpErrorMessage`** for user-facing errors. |
| `src/styles.scss` | **`#toast-container` / `.toast-container`** **`z-index: 100050 !important`** so toasts sit above fixed widgets. |

**Design notes**

- Refresh uses **`HttpBackend`** so the refresh `POST` does not re-enter the interceptor chain (avoids recursion).
- **`sharedRefresh`** / in-flight observable pattern avoids duplicate concurrent refresh calls when multiple requests fail with 401 at once.
- **`AuthService`** is **not** injected from the refresh interceptor (avoids circular DI with `HttpClient`).

---

### 2.2 Home page — service cards (layout / responsiveness)

**File:** `src/app/features/home/home.component.ts`

- Replaced multiple Bootstrap-style **rows/columns** of service tiles with a single **`<section class="services-section">`** driven by a **`services[]`** array (title, image filename, description).
- **CSS Grid:** `repeat(auto-fill, minmax(min(100%, 260px), 1fr))`, **`align-items: stretch`**, card inner **flex** with description **`flex: 1 1 auto`** for equal-height cards in a row.
- Typography uses **`clamp()`** for scaling across breakpoints.
- **Get Quote** calls existing global **`showpopup()`** from legacy scripts via **`openQuotePopup()`** (`window.showpopup?.()`).

---

### 2.3 Property video playback (YouTube, Google Drive, direct files)

**Problems addressed**

- **`iframe [src]`** bound to a raw string was **sanitized away** by Angular — embeds looked broken.
- **YouTube** watch URLs were used with **`<video src>`**, which cannot play HTML pages.
- **Google Drive** **`uc?export=view&id=`** (and similar) links were not parsed as file IDs.
- **Property list cards** only used **`IMAGE`** media for the thumbnail; **video-only** listings showed a broken **`<img>`**.

**File:** `src/app/core/utils/image-url.util.ts`

- **`extractGoogleDriveFileId`**: `/file/d/ID`, `/open?id=ID`, **`/uc`…`id=`**, thumbnail paths; skips **`/folders/`** links.
- **`extractYouTubeVideoId`**: `youtube.com/watch`, **`/embed/`**, **`/shorts/`**, **`/live/`**, **`youtu.be`** (exported).
- **`getPropertyVideoPlayerKind`**: **`'embed'`** (YouTube + Drive file) vs **`'native'`** (direct file / relative upload URL).
- **`resolveVideoEmbedUrl`**: YouTube → **`https://www.youtube.com/embed/{id}?rel=0&modestbranding=1`**; Drive → **`https://drive.google.com/file/d/{id}/preview`**.
- **`resolveNativeVideoUrl`**: same-origin-relative + **http(s)** URLs **except** YouTube/Drive (for **`<video>`**).
- **`resolveVideoCardPosterUrl`**: YouTube **hqdefault** thumbnail; Drive **thumbnail** API; direct **mp4/webm** → placeholder “▶ Video” image.
- **`resolvePropertyImageUrl` / `toDirectImageUrl`**: benefit from expanded Drive ID parsing.
- **`resolvePropertyVideoUrl`**: kept as **deprecated** shim (embed URL else native).

**Files:** `property-detail.component.ts`, `property-form.component.ts`

- Inject **`DomSanitizer`**; for **`embed`** mode use **`bypassSecurityTrustResourceUrl(resolveVideoEmbedUrl(...))`** on **`iframe [src]`**.
- **`native`** mode: **`[src]="resolveNativeVideoUrl(...)"`** on **`<video>`** with **`playsinline`**, **`controls`**.
- **16:9** container styles for gallery / form preview.

**File:** `src/app/shared/property-card/property-card.component.ts`

- **`imgUrl`**: first **IMAGE** as before; if none, first **VIDEO** → **`resolveVideoCardPosterUrl`**.
- **`mediaGalleryLabel`**: **▶** prefix when any media item is a video (for the count badge when `images.length > 1`).

---

## 3. File checklist (frontend)

Use this as a quick audit list when porting changes to another branch or duplicate app:

```
1G-Backend/src/main/java/com/realestate/controller/SiteVisitController.java
1G-Frontend/src/app/app.config.ts
1G-Frontend/src/app/core/http-context.tokens.ts (includes `SILENT_NOT_FOUND` for optional `GET` 404s)
1G-Frontend/src/app/core/interceptors/auth-refresh.interceptor.ts
1G-Frontend/src/app/core/interceptors/error.interceptor.ts
1G-Frontend/src/app/core/services/api.service.ts
1G-Frontend/src/app/core/services/auth.service.ts
1G-Frontend/src/app/core/utils/http-error-message.util.ts
1G-Frontend/src/app/core/utils/image-url.util.ts
1G-Frontend/src/app/features/auth/verify-otp/verify-otp.component.ts
1G-Frontend/src/app/features/home/home.component.ts
1G-Frontend/src/app/features/property-detail/property-detail.component.ts
1G-Frontend/src/app/features/property-form/property-form.component.ts
1G-Frontend/src/app/shared/property-card/property-card.component.ts
1G-Frontend/src/styles.scss
```

---

## 4. Suggested manual tests

1. **Auth:** Log in with wrong password → single **error** toast with backend message when present.  
2. **Session:** Log in, wait until access token expires (or revoke server-side), trigger **book visit** → refresh should run once and succeed, or show **session expired** and clear storage.  
3. **Home:** Resize viewport — service cards stay aligned and equal height within each row.  
4. **Video:** Add **YouTube** + **Drive file** + **.mp4** URLs as **VIDEO** media — preview in form, playback on property detail, poster on search/list card when there is no image.

---

## 5. Related documentation (pre-existing)

| Document | Topic |
|----------|--------|
| `1G-Backend/docs/PROJECT_CONTEXT.md` | Architecture, API overview |
| `1G-Backend/docs/CONTEXT_2026-01-30.md` | Context path `/api`, JWT, CD fixes history |
| `1G-Backend/GOOGLE_DRIVE_SETUP.md` | Drive uploads, public URLs |
| `1G-Frontend/CONTEXT_2026-04-14.md` | Frontend session notes |

---

## 6. Follow-up (same day / production hardening)

### 6.1 Site visit “404” in DevTools

- **Cause (legacy):** `GET .../for-property/{id}` used **404** when no pending/assigned visit existed; browsers log that as a failed request even though the UI treats it as “no visit”.
- **Fix:** Backend **204** when empty; frontend **`SILENT_NOT_FOUND`** `HttpContext` on that `GET` so the global error interceptor does not toast on **404** if an older API is still deployed.

### 6.2 Mixed content (`http://youtube.com/...` on HTTPS frontend)

- **Cause:** Stored or pasted URLs used **`http://`**; `<img>` / `<video>` / embed upgrades expect **HTTPS** on production.
- **Fix:** **`upgradeInsecureMediaUrl()`** in `image-url.util.ts` rewrites `http://` → `https://` for YouTube, Google Drive, `img.youtube.com`, etc. YouTube links wrongly stored as **IMAGE** now resolve to **`img.youtube.com` thumbnails** in `toDirectImageUrl`.

### 6.3 Book / Reschedule modal: blur only, no dialog

- **Cause (initial):** Sticky header **`z-index: 1000`** tied with an older modal layer.
- **Cause (persistent):** Global **`.card { overflow: hidden }`** on **`modal card`** could clip or prevent the dialog panel from painting correctly in some stacking contexts.
- **Fix (final):** **`pv-dialog-root` / backdrop / panel`** pattern (no **`card`** class on the dialog), **`z-index: 600000`**, **`signal` + `@if`**, **`NgZone.run`** when opening, Escape closes dialogs before zoom. Toasts at **`800000`**.

### 6.4 Home “Get Quote” alignment

- **Fix:** Service card CTA uses **`align-self: center`** and **`min-width`** so the button is centered in each card row.

### 6.5 Login / signup error visibility

- **Fix:** **`AuthService.login` / `signup`** return **`Observable`**; **Login** and **Signup** components **`subscribe`** with **`NgZone.run`** for **toastr** and an **`auth-inline-error`** banner for API errors.

---

*End of document. When you make further changes, append a dated subsection or link to PRs/commits.*
