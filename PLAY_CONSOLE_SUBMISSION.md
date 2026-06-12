# OnTect Play Console Submission Notes

Use this file while filling out Google Play Console.

## Privacy Policy URL

Use this URL after GitHub Pages is enabled:

```text
https://qwertyuraura235-rgb.github.io/OnTect/privacy.html
```

## App Category

- App type: App
- Category suggestion: Productivity or Education
- Price: Free
- Contains ads: No

## Contact

Use the contact details from the Google Play developer account.
For the privacy policy page, repository issues are listed as the public support channel:

```text
https://github.com/qwertyuraura235-rgb/OnTect/issues
```

## Data Safety Draft

- Does your app collect or share any required user data types? The free build stores local account and inventory data on device only and does not transmit it to a server.
- Is data shared with third parties? No.
- Is data encrypted in transit? Not applicable for the free build because no data is transmitted.
- Can users request data deletion? Users can delete all local data by clearing app storage or uninstalling the app.
- Does the app use advertising ID? No.
- Does the app contain ads? No.
- Does the app use analytics SDKs? No.
- Does the app have in-app purchases? No.

## Release Artifact

Upload this signed Android App Bundle:

```text
app/build/outputs/bundle/freeRelease/app-free-release.aab
```

## Feature Graphic

Upload:

```text
store-assets/feature-graphic.png
```

Size: 1024 x 500 PNG.

## Release Name

```text
OnTect 0.3.0-free
```

## Release Notes

```text
첫 실행 관리자 계정 생성 기능을 추가했습니다.
공용 데모 계정을 제거하고 로컬 계정 방식으로 변경했습니다.
관리자 사용자 추가 기능과 재고 통계 대시보드를 개선했습니다.
무료 로컬 빌드 기준으로 앱 아이콘, 릴리즈 서명, 개인정보처리방침을 준비했습니다.
```

## Closed Testing Plan

If this is a new personal developer account, prepare at least 12 testers and keep them opted in for 14 continuous days before requesting production access.

Suggested tester tasks:

- Install the app from the closed testing link.
- Create the first admin account.
- Add one teacher or assistant user.
- Add a reagent inventory item.
- Record one stock-in event.
- Record one stock-out event.
- Open the admin dashboard and check statistics.
- Relaunch the app and confirm data is preserved.
