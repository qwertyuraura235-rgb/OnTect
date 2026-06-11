# OnTect

`OnTect` is a native Android app for managing school reagents, lab supplies, and science classroom equipment.

It is built to replace spreadsheet-only workflows with a faster field-friendly flow:

- quick search across reagents, supplies, and equipment
- low-stock alerts and expiry warnings
- storage location and manager tracking
- stock-in and stock-out history
- offline-first local persistence for classroom and lab use

## Current MVP

- dashboard with alert summary
- searchable inventory list
- add and edit item flow
- quick stock-in and stock-out actions
- recent activity timeline
- spreadsheet migration mapping guide

## Build

```bash
./gradlew assembleDebug
```

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
2. Add CSV or Google Sheets import for existing inventory sheets.
3. Add cloud sync for shared management across multiple teachers.
4. Prepare a privacy policy and Play Store listing assets.
5. Create a signed Android App Bundle (`.aab`) for release.
