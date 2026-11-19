# Scales on Google Play and the Amazon Appstore

Everything the two store listings need, in the order the consoles ask for it. Text in the
quoted blocks is ready to paste. Sizes and rules come from the Android plan
(`docs/plans/android-plan.md` §5 and §10); the files sit next to this one.

## Release build

```sh
make android-bump                # versionCode + 1 in android/version.properties
make bundle-android              # parity --release, R8 release AAB and APK, scripts/check_apk.sh
make android-release-notes SINCE=<last uploaded tag or commit>   # start of "What's new"
```

- Play: `android/app/build/outputs/bundle/release/app-release.aab`
- Amazon: `android/app/build/outputs/apk/release/app-release.apk` (named
  `app-release-unsigned.apk` on a Mac without the upload key; that file cannot be uploaded)
- First build (18 Nov 2026, versionCode 1): APK 3.0 MB, AAB 4.4 MB; `check_apk.sh` passed
  (native allow-list, armeabi-v7a and arm64-v8a, 16 KB alignment, leanback and touchscreen
  not required, no required features, no permissions beyond AndroidX's own, banner,
  targetSdk 36).

Agents never upload. `make bundle-android` builds and checks; the uploads below are done by
hand.

## Google Play

**App name** (30 characters at most)

> Scales

**Short description** (80 characters at most; this one is 77)

> Any scale across the guitar neck or a piano keyboard, big enough for your TV.

**Full description** (4,000 characters at most). It names Android TV, which Google's TV
review looks for before it accepts the TV release.

> Scales draws any musical scale across the guitar neck or a piano keyboard, so you can see
> the whole shape at once and practise it from across the room.
>
> ANY SCALE, ANY KEY
> Pick a root and a scale: major and minor, the modes, pentatonics, blues, harmonic and
> melodic minor and more. Every note appears on every string and fret, or on one or two
> piano octaves. Circles show note names, scale degrees, or nothing at all, and the root
> stands out. Sharps and flats are spelled the way the key spells them.
>
> CHORDS OF THE SCALE
> The chords the scale makes sit under the board. Tap one and its notes are ringed on the
> board while the rest of the scale stays in view.
>
> CHANGE ONE NOTE
> Lower, raise, add or remove a note straight from the board. The chords update in place
> and show what each one was, which note is new and which chords the scale lost. The
> Transform tab explains the same change as a lesson, names the scale you made when it has
> a name, and lets you save new ones under your own name to use like any built-in scale.
>
> CHORD TABLES
> The chords of every major and minor key, in one table.
>
> DRUM TRAINER
> A groove written on the drum staff, with a ball that bounces from count to count, to read
> and play along with at any tempo.
>
> ON YOUR TV
> Scales runs on Android TV and Google TV, with its own screen made for the remote: the
> board fills the TV, the options open from the OK button, and the chord tables and drum
> trainer are one press away. A "Keep board on" timer stops the screensaver while you
> practise. Tablets get a larger board, and phones open a full-screen board in landscape.
>
> Left-handed and right-handed necks, standard tuning plus half step down, drop D, DADGAD,
> open G and open D, and 12 to 24 frets.
> No account, no ads, no tracking; nothing leaves your device.

**Category**: Music & Audio. **Tags**: music education, guitar.

**Contact email**: alnour.ahmed@gmail.com. **Privacy policy URL**:
https://alnouralharin.com/apps/scales/#privacy (the page is in the personal_website repo).

### Data safety

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | No |
| Is all of the user data collected by your app encrypted in transit? | Not applicable (no data collected) |
| Do you provide a way for users to request that their data is deleted? | Not applicable (no data collected) |
| Data shared with third parties | None |

Why "no": the app has no network code and requests no permissions. Settings and saved
scales stay in the app's own storage on the device. Android's own backup may copy them to
the player's Google account (`allowBackup`), which Google does not count as the developer
collecting data.

### Other forms

- **Content rating**: questionnaire category "Reference, News, or Educational"; every
  content question is No (no violence, no user content, no sharing, no purchases, no
  location). Expected rating: Everyone / PEGI 3.
- **Target audience**: 13 and over (the app is not designed for children; choosing an
  under-13 group triggers the Families policy for no benefit).
- **Ads**: No. **App access**: all functionality available without login.
- **Government, financial, health features**: none.

### Play assets

| Asset | Rule | File | Status |
| --- | --- | --- | --- |
| App icon | 512×512, 32-bit PNG, at most 1 MB | `icon-512.png` (from `icon-512.svg`) | ready |
| Feature graphic | 1024×500, JPEG or 24-bit PNG, no alpha | `feature-graphic.png` (from `feature-graphic.svg`) | ready |
| Phone screenshots | 2 to 8; 4 or more at 1080 px or more; long side at most twice the short | `screenshots/phone/01…07` (1080×2400; 04 cropped to 2160×1080) | ready |
| Tablet screenshots | 4 or more, 1080 to 7680 px | `screenshots/tablet/01…07` (2560×1600) | ready |
| TV screenshots | at least 1; plan on 4 at 1920×1080 | `screenshots/tv/01…04` | ready |
| TV banner (listing) | 1280×720, no alpha | `tv-banner-1280.png` (from `Store/Icon/android-tv-banner.svg`) | ready |
| TV banner (in the app) | 320×180, xhdpi, with the name | `android/app/src/main/res/drawable-xhdpi/banner.png` | in the APK |

The phone screenshot `04-tv-board-landscape.png` is cropped to 2160×1080 because Play allows
a long side of at most twice the short; `make screenshots-android` writes it at 2400×1080 again,
so crop it after a rerun (`ffmpeg -i in.png -vf crop=2160:1080:120:0 out.png`).

### Closed test before production

Personal developer accounts created after 13 November 2023 need **12 testers opted in for
14 continuous days** in a closed test before Play allows a production release. A tester who
leaves and rejoins restarts their own 14 days, so recruit 15 or more. Builds can be updated
during the test. Afterwards Google asks written questions about the test; review usually
takes up to 7 days. Organisation accounts and older personal accounts are exempt.

### Android TV release

After the bundle is uploaded with the assets above: Setup › Advanced settings › Form factors
› Add release type › Android TV, opt in, and add the TV screenshots and TV banner. Google
reviews the TV release separately against its TV quality list (TV-MT, TV-ML, TV-LB, TV-BN,
TV-DP, TV-DB, TV-OV, TV-BY, TV-G1, TV-G4, TV-G6; the answers are in plan §5).

## Amazon Appstore

Upload the signed release APK (the same file tested on the Fire TV stick). Tick Fire TV
devices by hand and leave Fire tablets unticked until they are tested. Vega devices cannot
take an APK, so the console marks them unsupported.

**Title**: Scales. **Short description**: as on Play.

**Description**

> Scales draws any musical scale across the guitar neck or a piano keyboard, big enough to
> read from the sofa. On Fire TV the board fills the screen and everything works from the
> remote: OK opens the options, where you choose the key, the scale, guitar or keyboard,
> note names or scale degrees, and a dark or light board.
>
> The chords of the scale sit under the board; pick one to ring its notes. Change one note
> of the scale (lower, raise, add or remove it) and see what each chord becomes. Chord
> tables list every major and minor key, and the drum trainer plays a groove written on
> the staff at any tempo. A "Keep board on" timer holds off the screensaver while you
> practise.
>
> No account, no ads, no tracking; nothing leaves your device.
>
> Works on Fire TV devices running Fire OS, including Fire TV Stick 4K Max and 4K Plus. Not
> available on Fire TV Stick 4K Select, 4K (3rd Gen) or HD (2026).

**Product features** (bullets): Any scale on guitar or piano · Chords of the scale, ringed on
the board · Change one note and see what each chord becomes · Chord tables for every key ·
Drum trainer · Made for the Fire TV remote.

| Amazon asset | Rule | File |
| --- | --- | --- |
| Small icon | 114×114 PNG, transparency allowed | `icon-114.png` |
| Large icon | 512×512 PNG, transparency allowed | `icon-512.png` |
| Fire TV icon | 1280×720 PNG, no transparency | `tv-banner-1280.png` |
| Screenshots | 3 to 10 at 1920×1080 | `screenshots/tv/01…04` |
| Fire TV background | 1920×1080, no text | `fire-tv-background-1920.png` (the fretboard band of `screenshots/tv/01-board.png`, padded) |

There is no closed-test requirement on Amazon.

## Steps only Alnour can take

1. **Upload key** (once): `keytool -genkeypair -keystore ~/.keystores/scales-upload.jks
   -keyalg RSA -keysize 4096 -validity 10000 -alias upload`. Back the file and both
   passwords up in the password manager. Add to `~/.gradle/gradle.properties`:
   `scalesUploadStoreFile`, `scalesUploadStorePassword`, `scalesUploadKeyAlias`,
   `scalesUploadKeyPassword`. `make bundle-android` then writes a signed
   `app-release.apk`. Done on this Mac.
2. **Play Console**: create the app (name Scales, free, app not game), accept Play App
   Signing, finish developer verification, the data safety form, content rating, target
   audience and ads forms as above, and paste the privacy policy URL above. The step-by-step list with links is in
   `release-steps.md`.
3. **Closed test**: upload `app-release.aab` to a closed track, invite 15 or more testers,
   keep 12 opted in for 14 days, then apply for production.
4. **Android TV**: add the TV release type, TV screenshots and TV banner (above).
5. **Amazon Developer Console**: create the app, upload the signed `app-release.apk`, tick
   the Fire TV devices, paste the description with its Fire OS sentence, upload the assets.
6. **Real devices**: run the release APK on the Fire TV stick (`make run-fire FIRE=<ip>`
   runs the debug build; install the release APK with `adb install` for the final check)
   before each upload.
7. Before each later upload: `make android-bump`, `make bundle-android`, then upload.
