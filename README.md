# Wiom CSP App

Wiom Channel Service Partner (CSP) platform — a mobile app for field partners + a web admin portal for operations.

## Architecture

- **Android App** (`android/`) — Kotlin + Jetpack Compose, native mobile app for CSPs and technicians
- **Admin Portal** (`src/`) — Next.js 15 web app for operations management (task creation, event simulation, registration review)
- **API Layer** (`src/app/api/`) — Next.js API routes with in-memory data store

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Node.js | 18+ | Admin portal |
| npm | 9+ | Package management |
| Android Studio | Latest | Android app |
| JDK | 17+ | Android build |
| Git | Any | Version control |

## Quick Start

### 1. Clone the repo

```bash
git clone https://github.com/ashishagrawal-iam/satyamYfeb20.git
cd satyamYfeb20
git checkout wioms-way-design-fixes-2026-03-12
```

### 2. Run the Admin Portal (Web)

```bash
npm install
npm run dev
```

Open in browser:
- **Admin Portal:** http://localhost:3456/admin
- **CSP Dashboard (web):** http://localhost:3456
- **Login:** http://localhost:3456/login

### 3. Run the Android App

1. Open the `android/` folder in **Android Studio**
2. Wait for Gradle sync to complete
3. Select an emulator or connected device
4. Click **Run** (green play button)

The app will build and install automatically.

### 4. Build an APK

```bash
cd android
./gradlew assembleDevDebug
```

The APK will be at:
```
android/app/build/outputs/apk/dev/debug/app-dev-debug.apk
```

## Connecting the Android App to the Admin Portal

Both the admin portal and Android app use the same in-memory data store via the Next.js API server.

- **Emulator:** The app connects to `10.0.2.2:3456` (Android emulator's alias for `localhost`)
- **Physical device:** Both the phone and computer must be on the same Wi-Fi network. Update the API base URL to your computer's local IP (e.g. `http://192.168.x.x:3456`)

To find your local IP:
```bash
# macOS
ipconfig getifaddr en0

# Linux
hostname -I
```

## Project Structure

```
satyamYfeb20/
├── android/                    # Android native app (Kotlin + Compose)
│   └── app/src/main/java/com/wiom/csp/
│       ├── ui/auth/            # Login screens
│       ├── ui/home/            # Dashboard, task feed, assurance strip
│       ├── ui/taskdetail/      # Task detail & actions
│       ├── ui/profile/         # CSP profile
│       ├── ui/wallet/          # Wallet hub
│       ├── ui/team/            # Team management
│       ├── ui/support/         # Support hub
│       ├── ui/netbox/          # NetBox hub
│       ├── ui/sla/             # SLA hub
│       ├── ui/policies/        # Policies
│       ├── ui/technician/      # Technician app screens
│       ├── ui/onboarding/      # Partner registration
│       ├── ui/theme/           # Design system (Color, Type, Theme)
│       └── ui/common/          # Shared components
├── src/                        # Next.js web app
│   ├── app/                    # App Router pages
│   │   ├── admin/              # Admin portal
│   │   ├── login/              # OTP login
│   │   ├── onboarding/         # Partner onboarding
│   │   ├── technician/         # Technician web view
│   │   └── api/                # API routes
│   └── lib/                    # Shared utilities, types, data
└── package.json
```

## Design System

The Android app follows the **Wiom Design System**:

- **Font:** Noto Sans (weights: 400, 600, 700)
- **Type scale:** 12, 14, 16, 20, 24, 32, 48sp
- **Spacing grid:** 4, 8, 12, 16, 20, 24, 32, 40, 48, 64, 72, 84dp
- **Border radius:** 4, 8, 12, 16, 24, 888dp
- **Primary brand:** #D9008D
- **Cards:** 16dp radius, 16dp padding, white bg
- **CTAs:** 24dp radius, 48dp height, Bold weight
- **Language:** Hindi-first with English fallback

## Admin Portal Features

- **Registration Review** — Approve/reject CSP partner applications
- **Task Creation** — Create installation, restore, and pickup tasks
- **Event Simulation** — Trigger capability resets, wallet freezes, SLA breaches
- **Dashboard Stats** — View platform metrics

## Environment

- **QA Backend:** services.qa.i2e1.in (OTP auth)
- **Default port:** 3456
- **Auth:** OTP-based login, JWT token stored in localStorage
