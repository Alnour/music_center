# Releasing Scales on Google Play and the Amazon Appstore, from no account

Every step with its link, in order. Text to paste is in `listing.md`; the tester
message is in `tester-invite.md`; release notes in `release-notes-1.0.txt`.

Privacy policy URL for both stores: https://alnouralharin.com/apps/scales/#privacy
(the page lives in `personal_website/client/public/apps/scales/index.html`; publish the
site from Replit after editing it).

## 1. Accounts (about an hour, then a wait for identity checks)

1. Google account with two-step verification on: https://myaccount.google.com/signinoptions/two-step-verification
2. Register a Play developer account (Personal, 25 USD once): https://play.google.com/console/signup
3. Identity verification when the console asks (passport or licence photo, selfie):
   https://play.google.com/console/ → Account details. Takes 1 to 7 days.
4. Amazon developer account (free): https://developer.amazon.com/apps-and-games
   Then the tax interview and payment profile under Settings, even for a free app.
5. Recruit 15 or more Android testers and collect their Gmail addresses.

## 2. Build (5 minutes)

6. `make verify`, commit.
7. `make bundle-android` → `android/app/build/outputs/bundle/release/app-release.aab` (Play)
   and `android/app/build/outputs/apk/release/app-release.apk` (Amazon).
8. Back up `~/.keystores/scales-upload.jks` and the four `scalesUpload*` lines of
   `~/.gradle/gradle.properties` to the password manager.

## 3. Google Play, first session (about 90 minutes)

9. Create app: https://play.google.com/console/ → All apps → Create app.
   Name Scales, English, App, Free. Tick the declarations.
10. Dashboard → "Set up your app" checklist. Each item below is one row of it.
11. App access: all functionality available without special access.
12. Ads: No.
13. Content rating: Reference, News, or Educational; every answer No. Result: Everyone.
14. Target audience: 13 and over. News app: No.
15. Data safety: does the app collect or share user data → No.
16. Government apps No, Financial features none, Health none.
17. Privacy policy: https://alnouralharin.com/apps/scales/#privacy
18. Store settings: category Music & Audio; tags music education, guitar; contact email.
19. Main store listing: short and full description from `listing.md`; `icon-512.png`;
    `feature-graphic.png`; phone screenshots `screenshots/phone/01…07` (04 is already
    cropped to 2:1); tablet screenshots `screenshots/tablet/01…07` in both the 7-inch
    and 10-inch slots.
20. Android TV: Setup → Advanced settings → Form factors → Add form factor → Android TV.
    Then in the listing's Android TV section: `screenshots/tv/01…04` and `tv-banner-1280.png`.
21. Setup → App signing: Play App Signing on, Google-generated key (the default).
22. Testing → Closed testing → Create track "Testers" → Create release → upload
    `app-release.aab` → release notes from `release-notes-1.0.txt` → Save → Review → Start rollout.
23. Same track, Testers tab: create an email list with the addresses, save, copy the
    opt-in link, send `tester-invite.md` with that link. Note today's date.
24. Keep 12 or more testers opted in for 14 unbroken days. New builds during that time:
    `make android-bump`, `make bundle-android`, upload to the same track.

## 4. Amazon Appstore, same day (about 45 minutes, no test period)

25. https://developer.amazon.com/apps-and-games/console/apps/new → Android app.
    Title Scales, category Music, no in-app purchases.
26. APK files: upload `app-release.apk`. Device support: Fire TV devices only.
27. Description: Amazon description and product-feature bullets from `listing.md`.
    Keywords: guitar, scales, piano, music theory.
28. Images: `icon-114.png`, `icon-512.png`, `tv-banner-1280.png`,
    `fire-tv-background-1920.png`, `screenshots/tv/01…04`.
29. Content rating: every answer No. Privacy policy URL as above. Submit.

## 5. Google production, after the 14 days

30. Dashboard → Apply for production access: https://play.google.com/console/
    Answer the questions in a few sentences each (who tested, what they found, what changed).
31. On approval: Production → Create release → Add from library → the tested bundle →
    release notes → Review → Start rollout at 100 %. Review takes up to 7 days.
32. Check the Android TV review under Publishing overview; it is reviewed separately.

## Every later release

```sh
make android-bump
make bundle-android
git commit -am "Android build N"
```

Upload the new `.aab` to Play (Production → Create release) and the new `.apk` to Amazon
(Upcoming version).

Reference: Play closed-test rule https://support.google.com/googleplay/android-developer/answer/14151465
Play asset rules https://support.google.com/googleplay/android-developer/answer/9866151
Amazon asset rules https://developer.amazon.com/docs/app-submission/asset-guidelines.html
