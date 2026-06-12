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
- login with role-based sessions
- admin dashboard for user and inventory statistics
- dashboard with alert summary
- searchable inventory list
- add and edit item flow
- quick stock-in and stock-out actions
- recent activity timeline
- spreadsheet migration mapping guide

## Demo Accounts

The current free build uses local demo authentication so the app can be tested without a backend.

| Role | Email | Password |
| --- | --- | --- |
| Admin | `admin@ontect.school` | `admin1234` |
| Chemistry teacher | `chem@ontect.school` | `chem1234` |
| Biology teacher | `bio@ontect.school` | `bio1234` |
| Lab assistant | `assistant@ontect.school` | `lab1234` |

Admin users can open the `관리자` tab after login to inspect:

- total, active, and role-based user counts
- total inventory and alert counts
- stock-in, stock-out, and audit activity totals
- lab-level owner, inventory, and alert summaries
- full user roster with last login timestamps

## Build

```bash
./gradlew assembleFreeDebug
```

The generated APK is written to:

`app/build/outputs/apk/free/debug/app-free-debug.apk`

## Free Build Scope

The default `free` flavor is designed to stay usable without paid infrastructure.

- Authentication is local demo authentication.
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

1. Replace the placeholder icon and finalize the visual brand.
2. Add local CSV import/export for existing inventory sheets.
3. Prepare Play Store listing assets for a free app.
4. Create a signed Android App Bundle (`.aab`) for release.
5. Consider optional school-hosted sync later, only if a school wants shared devices.
