# MSG91 OTP Setup – Where to Add Credentials

OTP for mobile verification is sent via **MSG91** only. Add your MSG91 credentials in the places below.

---

## How to create MSG91 Auth Key

1. **Sign up / Log in** at [msg91.com](https://msg91.com).
2. In the **MSG91 Dashboard**, go to **Flow and APIs** (or **Developers** → **API**).
3. Under **Configure**, click **Create New** (or **Add Key**).
4. Enter a name for the key (e.g. "Real Estate App") and create it.
5. **Copy the Authentication Key** – this is your `MSG91_AUTHKEY`. Paste it in `application.yml` or set the `MSG91_AUTHKEY` environment variable.

You can also find an existing key under **Flow and APIs** → **Configure** → list of keys.

---

## Testing without DLT template (no real SMS)

In India, sending OTP SMS usually needs a **DLT-approved template**. Until you have DLT set up, you can test in two ways:

### Option A: Use test OTP (no SMS, no MSG91 key needed)

1. In **backend** `application.yml`, under `app.otp`, set a fixed test OTP:
   ```yaml
   app:
     otp:
       test-otp: "123456"   # Only use in dev – any signup can use this OTP to verify
   ```
   Or set environment variable: `OTP_TEST_OTP=123456`
2. **Do not set** `MSG91_AUTHKEY` (leave it empty).
3. Sign up with any email and mobile → backend will use the test OTP (no SMS sent).
4. On the verify-otp screen, enter **123456** → verification will succeed.

Remove `test-otp` (or leave it blank) when you go to production or when using real MSG91 + DLT.

### Option B: Read OTP from backend logs (no MSG91 key)

1. Leave `MSG91_AUTHKEY` empty and do **not** set `test-otp`.
2. Sign up → backend generates a random OTP and **logs** it (e.g. `OTP for 9876543210 would be: 482917`).
3. Check your **backend console/logs** for the line containing the OTP.
4. Enter that OTP on the verify-otp page.

Use **Option A** for quick local testing; use **Option B** if you prefer not to use a fixed test OTP.

---

## 1. Backend – `application.yml` (or environment variables)

**File:** `backend/src/main/resources/application.yml`

Under `app.msg91` you have placeholders. **Add your values** either here or via environment variables:

```yaml
app:
  msg91:
    # REQUIRED: Your MSG91 auth key. Get from MSG91 Dashboard → API Keys.
    authkey: ${MSG91_AUTHKEY:}        # ← Add your key here, e.g. "YOUR_MSG91_AUTHKEY"
    # OPTIONAL: Sender ID (6 chars). Leave empty to use MSG91 default.
    sender: ${MSG91_SENDER:}          # ← e.g. "SMSIND" or your approved sender
```

**Exact placeholders to replace:**

| Placeholder / Env var | Where to get it | Example value |
|-----------------------|-----------------|---------------|
| `MSG91_AUTHKEY` or `app.msg91.authkey` | MSG91 Dashboard → API Keys (or SendOTP auth key) | `"abc123YourAuthKey"` |
| `MSG91_SENDER` or `app.msg91.sender` | Your approved 6‑character Sender ID (optional) | `"SMSIND"` |

So in `application.yml`, replace the empty defaults with your values, for example:

```yaml
  msg91:
    authkey: ${MSG91_AUTHKEY:YOUR_MSG91_AUTHKEY_HERE}
    sender: ${MSG91_SENDER:SMSIND}
```

Or set environment variables and leave the file as-is:

- `MSG91_AUTHKEY=your_actual_authkey`
- `MSG91_SENDER=your_sender_id` (optional)

---

## 2. Backend – Heroku / production

**File:** `backend/src/main/resources/application-heroku.yml`

The same `app.msg91` section is used. Set these **config vars** in Heroku (or your host):

- **`MSG91_AUTHKEY`** = your MSG91 auth key  
- **`MSG91_SENDER`** = sender ID (optional)

No Firebase or other OTP config is used.

---

## 3. Frontend – no MSG91 credentials

The frontend **does not** use any MSG91 or Firebase config. OTP is sent by the **backend** when the user signs up; the user only enters the OTP on the verify-otp screen.

---

## 4. How to get MSG91 credentials

1. Sign up at [msg91.com](https://msg91.com).
2. **Auth key:** MSG91 Dashboard → **API Keys** (or **SendOTP** section) → copy your **Authentication Key**.
3. **Sender ID:** Use the default (e.g. SMSIND) or get your own 6‑character Sender ID from MSG91 (may require approval).

---

## 5. Summary – exact places to add credentials

| Location | What to add |
|----------|-------------|
| **Backend** `application.yml` → `app.msg91.authkey` | Your MSG91 auth key (or set env `MSG91_AUTHKEY`) |
| **Backend** `application.yml` → `app.msg91.sender` | Optional sender ID (or set env `MSG91_SENDER`) |
| **Production/Heroku** | Config vars: `MSG91_AUTHKEY`, `MSG91_SENDER` |
| **Frontend** | Nothing – no OTP credentials in frontend |

After setting `authkey` (and optionally `sender`), restart the backend. Signup will send OTP via MSG91 and verification will work with the 6‑digit OTP only.
