# AratKain Android App

> **Discover · Explore · Savor** — Your nearby cafes & restaurant tracker

---

## 📱 About

AratKain is an Android application that connects to the **Supabase** backend to provide full user authentication and profile management. Built with **Kotlin**, **Retrofit**, and **MVP + Vertical Slicing** architecture.

---

## 👥 Developer

| Name | Role |
|---|---|
| Mikel Josh A. Nicer | Mobile Developer |

---

## 🔧 Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| Architecture | MVP (Model-View-Presenter) + Vertical Slicing |
| HTTP Client | Retrofit 2.9.0 |
| JSON Parser | Gson |
| Image Loading | Glide 4.16.0 |
| Backend | Supabase (PostgreSQL + Auth REST API) |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 34 (Android 14) |

---

## 📂 Project Structure

```
com.aratkain/
├── AratKainApp.kt                  ← Custom Application class
├── SplashActivity.kt               ← Entry point
│
├── core/                           ← Shared across all features
│   ├── api/
│   │   └── SupabaseClient.kt       ← Centralized Retrofit API client
│   ├── model/
│   │   └── Models.kt               ← Request & Response data classes
│   └── utils/
│       ├── SessionManager.kt       ← JWT token storage (SharedPreferences)
│       └── Extensions.kt           ← Kotlin extension functions
│
├── login/                          ← Login feature (vertical slice)
│   ├── LoginContract.kt
│   ├── LoginPresenter.kt
│   └── LoginActivity.kt
│
├── register/                       ← Register feature (vertical slice)
│   ├── RegisterContract.kt
│   ├── RegisterPresenter.kt
│   └── RegisterActivity.kt
│
├── dashboard/                      ← Dashboard feature (vertical slice)
│   ├── DashboardContract.kt
│   ├── DashboardPresenter.kt
│   └── DashboardActivity.kt
│
├── profile/                        ← Profile feature (vertical slice)
│   ├── ProfileContract.kt
│   ├── ProfilePresenter.kt
│   └── ProfileActivity.kt
│
├── updateprofile/                  ← Update Profile feature (vertical slice)
│   ├── UpdateProfileContract.kt
│   ├── UpdateProfilePresenter.kt
│   └── UpdateProfileActivity.kt
│
├── changepassword/                 ← Change Password feature (vertical slice)
│   ├── ChangePasswordContract.kt
│   ├── ChangePasswordPresenter.kt
│   └── ChangePasswordActivity.kt
│
└── forgotpassword/                 ← Forgot Password feature (vertical slice)
    ├── ForgotPasswordContract.kt
    ├── ForgotPasswordPresenter.kt
    └── ForgotPasswordActivity.kt
```

---

## ⚙️ Setup & Installation

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Java 17
- Android device or emulator (API 24+)

### Steps

**1. Clone the repository**
```bash
git clone https://github.com/YOUR_USERNAME/aratkain-android.git
cd aratkain-android
```

**2. Open in Android Studio**
- Launch Android Studio
- Click **File → Open**
- Select the project folder
- Click **OK** and wait for Gradle sync

**3. Add dependencies in `build.gradle (Module: app)`**
```gradle
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
implementation 'com.github.bumptech.glide:glide:4.16.0'
implementation 'de.hdodenhof:circleimageview:3.1.0'
```

**4. Sync Gradle**
- Click **Sync Now** when prompted

**5. Run the app**
- Connect a device or start an emulator
- Click **Run ▶**

---

## 🌐 API Documentation

**Base URL:**
```
https://oyohrydkfhsmgwrejvga.supabase.co
```

**Default Headers (all requests):**
```
apikey: <supabase_anon_key>
Content-Type: application/json
```

**Protected routes also include:**
```
Authorization: Bearer <access_token>
```

---

### 🔐 1. Register

**Endpoint:** `POST /auth/v1/signup`

**Description:** Creates a new user account in Supabase Auth, then inserts the user profile into the `users` table.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Success Response `200 OK`:**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer",
  "user": {
    "id": "7f81539d-10e9-4719-aedb-df80856df832",
    "email": "user@example.com"
  }
}
```

**Error Response `400 Bad Request`:**
```json
{
  "error": "User already registered",
  "error_description": "A user with this email address has already been registered"
}
```

**After Auth signup — Insert profile:**

`POST /rest/v1/users`

```json
{
  "user_id": "7f81539d-10e9-4719-aedb-df80856df832",
  "username": "mikeljosh",
  "fullname": "Mikel Josh Niccor",
  "email": "user@example.com",
  "role": "user"
}
```

---

### 🔑 2. Login

**Endpoint:** `POST /auth/v1/token?grant_type=password`

**Description:** Authenticates a user with email and password. Returns a JWT access token.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Success Response `200 OK`:**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer",
  "user": {
    "id": "7f81539d-10e9-4719-aedb-df80856df832",
    "email": "user@example.com"
  }
}
```

**Error Response `400 Bad Request`:**
```json
{
  "error": "invalid_grant",
  "error_description": "Invalid login credentials"
}
```

**After login — Fetch profile:**

`GET /rest/v1/users?user_id=eq.{userId}&select=user_id,username,fullname,email,photo_url,role`

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response `200 OK`:**
```json
[
  {
    "user_id": "7f81539d-10e9-4719-aedb-df80856df832",
    "username": "Mikel",
    "fullname": "Mikol Alariao",
    "email": "mikeljosh@test.com",
    "photo_url": null,
    "role": "user"
  }
]
```

---

### 📊 3. Dashboard

**Description:** Loads the currently logged-in user's data from `SessionManager` (SharedPreferences). No new API call is needed — the data is already saved after login.

**Data displayed:**
- Username
- Full Name
- Email
- Profile photo (if available)

---

### 👤 4. Profile

**Description:** Reads user data from `SessionManager`. Shows username, fullname, email, and profile photo. Provides navigation to Update Profile and Change Password.

---

### ✏️ 5. Update Profile

**Endpoint:** `PATCH /rest/v1/users?user_id=eq.{userId}`

**Description:** Updates the username and fullname of the logged-in user in the `users` table.

**Headers:**
```
Authorization: Bearer <access_token>
Prefer: return=minimal
```

**Request Body:**
```json
{
  "username": "newusername",
  "fullname": "New Full Name"
}
```

**Success Response `204 No Content`**
*(empty body — success)*

**Error Response `409 Conflict`:**
```json
{
  "code": "23505",
  "details": "Key (username)=(newusername) already exists.",
  "message": "duplicate key value violates unique constraint \"users_username_key\""
}
```

---

### 🔒 6. Change Password

**Endpoint:** `PUT /auth/v1/user`

**Description:** Updates the password of the currently authenticated user via Supabase Auth.

**Headers:**
```
Authorization: Bearer <access_token>
```

**Request Body:**
```json
{
  "password": "newpassword123"
}
```

**Success Response `200 OK`:**
```json
{
  "id": "7f81539d-10e9-4719-aedb-df80856df832",
  "email": "mikeljosh@test.com"
}
```

**Error Response `401 Unauthorized`:**
```json
{
  "error": "invalid_token",
  "message": "JWT expired"
}
```

---

### 🔓 7. Forgot Password

**Step 1 — Check if email exists:**

`GET /rest/v1/users?email=eq.{email}&select=email`

**Headers:**
```
Authorization: Bearer <anon_key>
```

**Response if found `200 OK`:**
```json
[
  { "email": "mikeljosh@test.com" }
]
```

**Response if not found `200 OK`:**
```json
[]
```

---

**Step 2 — Reset password via RPC:**

`POST /rest/v1/rpc/reset_user_password`

**Request Body:**
```json
{
  "user_email": "mikeljosh@test.com",
  "new_password": "newpassword123"
}
```

**Success Response `200 OK`**
*(empty body)*

---

## ⚠️ Error Handling

| HTTP Code | Meaning | App Response |
|---|---|---|
| `200` | Success | Proceed normally |
| `204` | No Content (update OK) | Show success message |
| `400` | Bad Request / Invalid credentials | Show error message |
| `401` | Unauthorized / Token expired | Redirect to Login |
| `409` | Conflict (duplicate username/email) | Show "already exists" error |
| `422` | Unprocessable (invalid data) | Show validation error |
| `500` | Server Error | Show "try again later" |
| No network | Connection failed | Show "No internet connection" |

---

## 🔐 Authentication Flow

```
App Launch
    ↓
SplashActivity (2 seconds)
    ↓
Token saved in SharedPreferences?
    ├── YES → DashboardActivity
    └── NO  → LoginActivity
                  ↓
            Enter email + password
                  ↓
            POST /auth/v1/token
                  ↓
            GET /rest/v1/users (fetch profile)
                  ↓
            Save token + profile to SharedPreferences
                  ↓
            DashboardActivity
```

---

## 📸 Screenshots

| Screen | Description |
|---|---|
| Splash | AratKain logo loading screen |
| Login | Email + password login form |
| Register | Full registration form with validation |
| Dashboard | Home screen with user info |
| Profile | View profile details |
| Update Profile | Edit username and full name |
| Change Password | Set new password |
| Forgot Password | 2-step password reset |

---

## 🏗️ MVP Architecture

Each feature follows the MVP pattern:

```
LoginContract.kt     ← Defines View + Presenter interfaces
LoginPresenter.kt    ← All logic + API calls (no Android imports)
LoginActivity.kt     ← Implements View interface, updates UI only
```

**Rules:**
- ✅ All validation logic is in the **Presenter**
- ✅ All API calls are in the **Presenter**
- ✅ The **Activity (View)** only calls presenter methods and updates UI
- ✅ Presenter holds a nullable reference to View to prevent memory leaks
- ✅ `presenter.onDestroy()` called in `Activity.onDestroy()`

---

*© 2025 AratKain · Built with Kotlin + Supabase*
