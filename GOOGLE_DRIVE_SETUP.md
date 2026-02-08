# Google Drive Image Upload Setup

Property images can be uploaded to a Google Drive folder and stored as publicly accessible URLs. When configured, the backend uploads resized images and thumbnails to Drive instead of the local filesystem.

**Folder ID** is already set in all environments to `1wYcQ625QmwveUa1h4VoQQFZkLitAc0bx`. That folder **must be inside a Shared Drive** (not My Drive), with the service account added as a member—see **§3**. You also need to configure **credentials** (see below).

## 1. Google Cloud Project & Drive API

1. Go to [Google Cloud Console](https://console.cloud.google.com/).
2. Create a project or select an existing one.
3. Enable the **Google Drive API**:
   - APIs & Services → Library → search "Google Drive API" → Enable.

## 2. Service Account

1. APIs & Services → **Credentials** → **Create credentials** → **Service account**.
2. Give it a name (e.g. `realestate-upload`) and create.
3. (Optional) Add a role; the service account will use its own key to access Drive.
4. Open the service account → **Keys** tab → **Add key** → **Create new key** → **JSON**. Download the JSON file.
5. Keep this file secure; do not commit it to version control.

## 3. Google Drive folder (must be in a Shared Drive)

**Important:** Service accounts have **no storage quota**. You cannot use a folder in **My Drive** (even if shared with the service account). You must use a folder inside a **Shared Drive** (Team Drive).

### 3.1 Create a Shared Drive (requires Google Workspace)

1. In Google Drive, click **Shared drives** in the left sidebar (or go to [drive.google.com](https://drive.google.com) and open **Shared drives**).
2. Click **New** → name it (e.g. "Real Estate Images") → **Create**.
3. Add the **service account** as a member: click the Shared drive name → **Manage members** → **Add members** → paste the **service account email** (`client_email` from your JSON key) → set role **Content manager** or **Manager** → **Send** (you can uncheck "Notify people").

### 3.2 Create a folder and get its ID

1. Open the Shared Drive, then create a folder inside it (e.g. "Property Images").
2. Open that folder and copy its **Folder ID** from the URL:
   - URL format: `https://drive.google.com/drive/folders/FOLDER_ID`
   - The `FOLDER_ID` is the long string between `/folders/` and the next `?` or end of URL.
3. Set `GOOGLE_DRIVE_FOLDER_ID` to this folder ID (or put it in `app.google-drive.folder-id` in config). The app is already configured to use `supportsAllDrives=true`.

**If you don’t have Google Workspace:** Shared Drives are a Workspace feature. With a personal Gmail account you can’t create them. Options: use **Google Workspace** (trial or paid), or use another storage (e.g. **AWS S3**, **Cloudinary**) for uploads instead of Drive.

## 4. Backend Configuration

Folder ID is already configured. Set **only** the credentials:

### Option A: Local / dev (file path)

Set the path to your service account JSON (env or `application.yml`):

- **Credentials file:** `GOOGLE_DRIVE_CREDENTIALS_PATH` or `app.google-drive.credentials-path`  
  Path is relative to the process working directory. Examples:  
  - Key in **project root**, app run from repo root: `./guntha-f6ee4-3264caae9768.json`  
  - Key in project root, app run from **backend** folder: `../guntha-f6ee4-3264caae9768.json`  
  - Or use an absolute path: `C:\path\to\guntha-f6ee4-3264caae9768.json`

### Option B: Heroku / production (JSON string)

Set Heroku config var (folder ID is already the default; override only if needed):

- `GOOGLE_DRIVE_CREDENTIALS_JSON` = the **entire** contents of the service account JSON file (single line or multi-line string).
- Optional: `GOOGLE_DRIVE_FOLDER_ID` to override the default folder (already set in app config).

To set the JSON on Heroku (PowerShell, one line):

```powershell
heroku config:set GOOGLE_DRIVE_CREDENTIALS_JSON="$(Get-Content path\to\key.json -Raw)"
```

Or copy the JSON content and set in Heroku Dashboard → Settings → Config Vars.

## 5. Behavior

- When **both** folder ID and credentials are set, the backend uploads property images to Google Drive and returns public view URLs (`https://drive.google.com/uc?export=view&id=FILE_ID`). These URLs are stored as the property image URL and thumbnail URL.
- When Google Drive is **not** configured, the backend falls back to local file storage under `app.upload.dir` (e.g. `uploads/`) and returns paths like `/uploads/xxx_main.png`.

## 6. Security

- Do not commit the service account JSON to git. The repo root `.gitignore` already ignores `guntha-f6ee4-3264caae9768.json` and `*credentials*.json`; add any other key filenames there if you use a different path.
- The app uses **Drive API** scope `https://www.googleapis.com/auth/drive` so the service account can create files in a folder shared with it. Each uploaded file is set to "anyone with the link can view" so the stored URLs work in the frontend without auth.

## 7. Troubleshooting (e.g. 400 on Heroku upload)

If `POST /api/upload` returns **400** and logs show "Google Drive API error", check Heroku logs for the **exact status and message** (e.g. `403 Forbidden`, `404 Not Found`). Then:

| Log / status | Cause | Fix |
|--------------|--------|-----|
| **403** **"Service Accounts do not have storage quota"** / **storageQuotaExceeded** | Folder is in **My Drive**. Service accounts have no quota; the folder must be in a **Shared Drive**. | Create a **Shared Drive** (Google Workspace required), add the **service account email** as Content manager, create a folder inside it, and set `GOOGLE_DRIVE_FOLDER_ID` to that folder’s ID. See **§3** above. |
| **403 Forbidden** or "insufficient permissions" | Folder not shared / SA not member of Shared Drive, or wrong folder ID | Put the folder in a **Shared Drive** and add the **service account email** as a member (Content manager). If using a Shared Drive, confirm the folder ID matches `GOOGLE_DRIVE_FOLDER_ID`. |
| **404 Not Found** | Folder ID wrong or folder deleted | Copy the folder ID again from the Drive URL and set `GOOGLE_DRIVE_FOLDER_ID` (or ensure the default in config is correct). |
| **401 Unauthorized** or "invalid credentials" | Invalid or mangled JSON on Heroku | Use **Base64**: encode the JSON file (e.g. `[Convert]::ToBase64String([IO.File]::ReadAllBytes("key.json"))` in PowerShell) and set `GOOGLE_DRIVE_CREDENTIALS_BASE64` instead of `GOOGLE_DRIVE_CREDENTIALS_JSON`. |
| "Google Drive is not configured" | Credentials not loaded (env var empty or init failed) | Set `GOOGLE_DRIVE_CREDENTIALS_JSON` or `GOOGLE_DRIVE_CREDENTIALS_BASE64` on Heroku. Check app startup logs for "Google Drive upload configured" or "credentials could not be loaded". |

After changing config vars, redeploy or restart the dyno so the app picks them up.
