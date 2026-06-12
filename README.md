# OnTect

`OnTect` is a free native Android app for managing school reagents, lab supplies, and science classroom equipment.

It is built to replace spreadsheet-only workflows with a faster field-friendly flow:

- no paid backend, subscription, ads, or cloud requirement in the default build
- quick search across reagents, supplies, and equipment
- low-stock alerts and expiry warnings
- storage location and manager tracking
- stock-in and stock-out history
- offline-first local persistence for classroom and lab use

## Current MVP

- free local-only Android build flavor
- first-run admin account setup
- local login with role-based sessions
- admin user creation for teacher, assistant, and additional admin roles
- admin dashboard for user and inventory statistics
- dashboard with alert summary
- searchable inventory list
- add and edit item flow
- quick stock-in and stock-out actions
- recent activity timeline
- Play Store release checklist and Korean listing draft

## First-Run Setup

The free build no longer ships with shared demo accounts.
On first launch, the app asks the school to create a local administrator account.
After that, administrators can add teacher, lab assistant, or additional admin accounts from the admin dashboard.

Authentication remains device-local in the free build:

- account records are stored in Android local preferences
- passwords are stored as PBKDF2 hashes with random salts
- no authentication data is sent to an external server
- clearing app storage removes local accounts and inventory data

Admin users can open the `관리자` tab after login to inspect:

- total, active, and role-based user counts
- total inventory and alert counts
- stock-in, stock-out, and audit activity totals
- lab-level owner, inventory, and alert summaries
- full user roster with last login timestamps

## Build

Use JDK 21 or JDK 17 for local Android builds. JDK 25 is currently too new for this Kotlin Gradle setup.

```bash
./gradlew assembleFreeDebug
```

The generated APK is written to:

`app/build/outputs/apk/free/debug/app-free-debug.apk`

## Release Bundle

Google Play requires Android App Bundles for new apps. Create a signed free release bundle with:

```bash
./gradlew bundleFreeRelease
```

The generated AAB is written to:

```text
app/build/outputs/bundle/freeRelease/app-free-release.aab
```

Release signing is configured through an untracked `keystore.properties` file.
Use `keystore.properties.example` as the template and keep the real upload key and passwords private.

## Free Build Scope

The default `free` flavor is designed to stay usable without paid infrastructure.

- Authentication is local account authentication.
- Inventory data is stored on the device with Android local preferences.
- Admin statistics are calculated on device.
- No analytics SDK, ad SDK, paid API, server, Firebase project, or Supabase project is required.
- Network permissions are not required by the current free build.

See [FREE_BUILD.md](FREE_BUILD.md) and [PRIVACY.md](PRIVACY.md) for the release assumptions.

## Vision

The app is designed for schools that currently manage chemicals and lab tools in shared spreadsheets.
OnTect keeps the familiar structure of category, item name, model or volume, and quantity while adding the practical controls schools usually need next:

- storage location
- minimum stock threshold
- expiry date tracking
- hazard level visibility
- movement history for audits

## Next Release Steps

1. Upload the signed `.aab` to Play Console internal testing.
2. Host `PRIVACY.md` as a public privacy policy URL.
3. Prepare phone screenshots and a 1024 x 500 feature graphic.
4. Complete Play Console Data safety with the local-only data handling described in `PRIVACY.md`.
5. Add local CSV import/export for existing inventory sheets.
6. Consider optional school-hosted sync later, only if a school wants shared devices.
