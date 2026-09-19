# CampusSync — Multi-College Campus Management App

Kotlin + Jetpack Compose + MVVM + Firebase (Auth/Firestore/Storage).

## Setup

1. Open this folder in Android Studio.
2. Create a Firebase project → add Android app with package `com.yourname.campussync`
   (matches `applicationId` in `app/build.gradle.kts`).
3. Place `google-services.json` at `app/google-services.json`.
4. Enable in Firebase Console:
   - **Authentication** → Google provider
   - **Firestore Database**
   - **Storage** (fee payment proof screenshots)
5. Deploy security rules: `firebase deploy --only firestore:rules`
6. Gradle sync, Run.

## Architecture

- **Auth**: Google Sign-In only. New users must have a pending invite
  (`invites/{email}`) created by a Principal/Admin — invite claiming is validated
  server-side by Firestore rules (`getAfter` checks).
- **Roles**: principal / admin / teacher / student. Role + collegeId live in the root
  `users/{uid}` doc; profiles live under `colleges/{collegeId}/{role}/{uid}`.
- **Tenancy**: every college is an isolated sub-tree (`colleges/{cid}/...`) guarded by
  `firestore.rules`.
- Each feature = Repository (Firestore only) + ViewModel (StateFlow) + Compose Screen.

## Features

- Attendance (teacher mark + student live view), Timetable, Assignments,
  Resources, Notices, Holidays
- Zero-commission fee system: fee types → assign dues → UPI QR payment with
  UTR proof → admin approval (atomic transactions, unique-UTR registry)
- Principal/Admin dashboards with stats, activity logs, invites, events

## Admin Scripts (`scripts/`)

Node.js utilities for custom claims management (`set_principal_claims.js`,
`verify_claims.js`). Requires `firebase-admin` (`npm install`).

> ⚠️ `scripts/serviceAccountKey.json` grants full database access — it is gitignored.
> Never commit it or share it. If exposed, rotate it in Firebase Console →
> Project Settings → Service Accounts.
