# Context Update — 2026-04-14

This document captures the latest backend + frontend changes implemented for 1Guntha so future developers/agents can continue work without losing context.

---

## Scope of this update

Implemented the following requested enhancements across `1G-Backend` and `1G-Frontend`:

1. Admin authority to manage all properties (edit/delete already present and preserved).
2. Property media support extended from image-only to image + video URL support.
3. Verified badge on property image sections in frontend.
4. Admin user lifecycle controls: suspend/activate and delete users with related data cleanup.

Also added `target/` ignore in backend gitignore.

---

## Backend changes (`1G-Backend`)

### 1) Property media type support (image/video)

- `PropertyImage` entity now includes `mediaType`.
  - File: `src/main/java/com/realestate/entity/PropertyImage.java`
  - Enum: `IMAGE`, `VIDEO`
  - Default: `IMAGE`
- `PropertyImageDto` now includes `mediaType`.
  - File: `src/main/java/com/realestate/dto/PropertyImageDto.java`
- `PropertyService.create()` and `PropertyService.update()` now persist `mediaType`.
  - File: `src/main/java/com/realestate/service/PropertyService.java`
  - Backward-compatible default to `IMAGE` when media type is missing.
- Flyway migration added:
  - File: `src/main/resources/db/migration/V7__property_image_media_type.sql`
  - Adds `media_type` column to `property_images` with default `IMAGE`.

### 2) Admin user management APIs

Added new admin endpoints in `AdminController`:

- `GET /admin/users` -> list all users
- `PUT /admin/users/{id}/status?active=true|false` -> activate/suspend user
- `DELETE /admin/users/{id}` -> delete user and related data

Files:

- `src/main/java/com/realestate/controller/AdminController.java`
- `src/main/java/com/realestate/service/AdminService.java`

Service methods added:

- `getAllUsers()`
- `setUserActive(Long userId, boolean active, UserPrincipal principal)`
- `deleteUser(Long userId, UserPrincipal principal)`

Safety constraints:

- Admin cannot suspend own account.
- Admin cannot delete own account.

### 3) User DTO and login suspension message

- `UserDto` now includes `active`.
  - File: `src/main/java/com/realestate/dto/UserDto.java`
- `AuthService.login()` deactivated account message updated to suspension-specific wording:
  - `"Suspended user: your account has been deactivated. Please contact admin."`
  - File: `src/main/java/com/realestate/service/AuthService.java`

### 4) Property management rights consistency

- Admin property management flow remains aligned:
  - Admin edit/create endpoints already used for admin actions.
  - Property update/delete owner checks still allow `ADMIN` override where intended.

### 5) Backend gitignore

- Added file `1G-Backend/.gitignore` with:
  - `target/`

---

## Frontend changes (`1G-Frontend`)

### 1) Property media model update

- `PropertyImage` model now supports media type.
  - File: `src/app/core/models/property.model.ts`
  - Added: `mediaType?: 'IMAGE' | 'VIDEO'`

### 2) Media URL utilities

Updated utility file:

- `src/app/core/utils/image-url.util.ts`

Added:

- `isGoogleDriveUrl(url)` helper
- `resolvePropertyVideoUrl(url, baseUrl?)`
  - Google Drive video links are mapped to embeddable preview URL:
    - `https://drive.google.com/file/d/<FILE_ID>/preview`
  - Relative URLs resolved with backend base URL like image logic.

### 3) Property form now supports image + video inputs

File:

- `src/app/features/property-form/property-form.component.ts`

Changes:

- Section renamed to "Property Media (Images/Videos)".
- Added media type selector (`IMAGE` or `VIDEO`) when adding URL.
- Added video preview handling (`<video>` or Google Drive `<iframe>` preview).
- Payload now sends `images[]` entries with:
  - `imageUrl`
  - `mediaType`
  - `displayOrder`

This keeps the existing backend field name `images` while supporting mixed media.

### 4) Property detail page media rendering + verified badge

File:

- `src/app/features/property-detail/property-detail.component.ts`

Changes:

- Added verified badge on main image section.
- Image gallery now uses image-only subset (`mediaType=IMAGE` or missing mediaType).
- Added video grid for entries with `mediaType=VIDEO`.
- Google Drive videos use embedded preview iframe.
- Image carousel/index logic updated to use image-only list.

### 5) Property card verified badge + image filtering

File:

- `src/app/shared/property-card/property-card.component.ts`

Changes:

- Added `✔ Verified` badge in card image overlay.
- Card thumbnail now picks first image media only (ignores videos).

### 6) Admin dashboard user management UI

File:

- `src/app/features/admin/admin.component.ts`

Changes:

- Added "User Management" section.
- Added user list table with:
  - Name, email, role, status
  - Suspend/activate action
  - Delete action (with confirmation)
- Calls new backend APIs:
  - `GET /admin/users`
  - `PUT /admin/users/{id}/status?active=...`
  - `DELETE /admin/users/{id}`

### 7) Login suspension UX

File:

- `src/app/core/services/auth.service.ts`

Changes:

- Added optional `active` to frontend `User` interface.
- Login error handling now surfaces suspension-specific toast:
  - "Suspended user: your account is inactive. Please contact admin."

### 8) Dashboard image resolution consistency

File:

- `src/app/features/dashboard/dashboard.component.ts`

Changes:

- Reused centralized URL resolver for image URLs.
- Added helper to pick first image media item instead of blindly using first media.

---

## Verification summary from this session

### Completed

- Backend packaging without tests: `mvn -DskipTests package` -> success.
- Frontend type-check: `npx tsc -p tsconfig.app.json --noEmit` -> success.
- No linter diagnostics on edited files.

### Blockers (pre-existing environment/project issues)

1. Backend tests and runtime failed due missing Firebase class dependency:
   - `ClassNotFoundException: com.google.firebase.auth.FirebaseAuthException`
   - Affects `mvn test` and `spring-boot:run` in this environment.
2. Angular production build blocked by local Node version:
   - Installed Node: `v20.11.0`
   - Angular CLI in this project requires at least `v20.19.0` (or `v22.12+`).

---

## Suggested next actions

1. Resolve Firebase classpath/bean mismatch in backend (or cleanly disable Firebase bean path).
2. Upgrade Node to `>=20.19.0` and rerun frontend `npm run build`.
3. Execute full end-to-end dry run:
   - Admin suspend user -> user login blocked with suspension message.
   - Admin activate user -> login restored.
   - Admin delete user -> verify user and related data removal.
   - Create/edit property with mixed image/video URLs.
   - Verify image/video rendering and verified badges.

---

## Edited file list (quick reference)

### Backend

- `src/main/java/com/realestate/entity/PropertyImage.java`
- `src/main/java/com/realestate/dto/PropertyImageDto.java`
- `src/main/java/com/realestate/service/PropertyService.java`
- `src/main/java/com/realestate/dto/UserDto.java`
- `src/main/java/com/realestate/service/AdminService.java`
- `src/main/java/com/realestate/controller/AdminController.java`
- `src/main/java/com/realestate/service/AuthService.java`
- `src/main/resources/db/migration/V7__property_image_media_type.sql`
- `.gitignore` (added with `target/`)

### Frontend

- `src/app/core/models/property.model.ts`
- `src/app/core/services/auth.service.ts`
- `src/app/core/utils/image-url.util.ts`
- `src/app/shared/property-card/property-card.component.ts`
- `src/app/features/property-form/property-form.component.ts`
- `src/app/features/property-detail/property-detail.component.ts`
- `src/app/features/admin/admin.component.ts`
- `src/app/features/dashboard/dashboard.component.ts`

