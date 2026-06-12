# OnTect Play Store Release Checklist

This checklist is for publishing the free local Android build of OnTect.

## Current Release Candidate

- App name: OnTect
- Package name: `com.park.ontect`
- Version: `0.3.0-free`
- Version code: `3`
- Minimum SDK: `26`
- Target SDK: `35`
- Release artifact: `app/build/outputs/bundle/freeRelease/app-free-release.aab`
- Local build JDK: 21 or 17. Avoid JDK 25 for this Kotlin Gradle setup.

## Completed In This Repository

- Android App Bundle build command is available: `./gradlew bundleFreeRelease`
- Release signing reads from local `keystore.properties`
- Keystore and signing passwords are excluded from Git
- Placeholder launcher icon has been replaced
- Shared demo accounts have been removed
- First-run local admin setup has been added
- Admin users can create additional local users
- Privacy policy draft is included in `PRIVACY.md`
- Korean Play Store listing draft is included in `STORE_LISTING_KO.md`

## Required Before Production Release

1. Upload the signed AAB to Play Console internal testing first.
2. Host `PRIVACY.md` on a public URL and add that URL in Play Console.
   Suggested URL: `https://qwertyuraura235-rgb.github.io/OnTect/privacy.html`
3. Complete Play Console Data safety using the local-only data handling described in `PRIVACY.md`.
4. Prepare at least 2 phone screenshots that show setup, inventory, and admin dashboard flows.
5. Upload the prepared 1024 x 500 feature graphic from `store-assets/feature-graphic.png`.
6. Test on at least one real Android phone before sending production review.
7. If the Play developer account is a new personal account, complete the required closed testing period before requesting production access.

## Suggested Data Safety Answers

- Data collection: local account info and local inventory records are stored on device.
- Data sharing: no data is shared with third parties in the free build.
- Data encryption in transit: not applicable because the free build does not transmit data.
- Data deletion: users can delete all local data by clearing app storage or uninstalling the app.
- Ads: no.
- Analytics: no.
- In-app purchases: no.

## Manual QA Checklist

- First launch shows the admin setup screen.
- Admin account can be created with a valid email and 8+ character password.
- Login succeeds with the created admin account.
- Invalid login shows an error.
- Admin dashboard opens for admin users.
- Admin can create teacher or assistant users.
- Newly created users can log in.
- Inventory search and filters work.
- New inventory item can be added.
- Stock-in and stock-out actions update quantity and history.
- App still works after force close and relaunch.
