# OnTect Free Build

This repository is configured around a free Android build of OnTect.

## Included

- Local demo login for admin, teacher, and lab assistant roles
- Device-local reagent and lab inventory storage
- Device-local admin dashboard statistics
- Stock-in, stock-out, and audit history
- Low-stock, expiry, and high-hazard visibility

## Excluded From The Free Build

- Paid backend services
- Paid authentication providers
- Ads
- In-app purchases
- Paid analytics SDKs
- Required cloud sync

## Build Command

```bash
./gradlew assembleFreeDebug
```

APK output:

```text
app/build/outputs/apk/free/debug/app-free-debug.apk
```

## Release Notes

The free build is intended for small school labs that need a working inventory tool before deciding whether shared cloud sync is necessary. Because all current data is stored on the device, schools should plan manual backup or local export before using it as the only source of record.
