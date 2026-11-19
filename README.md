# Scales

Scales draws any musical scale across the guitar neck or a piano keyboard and puts it on
the TV, so a player can read the shape from across the room while practising.

<p align="center">
  <img src="Store/Screenshots/6.3-inch/01-g-major.png" width="200" alt="G major on the fretboard">
  <img src="Store/Screenshots/6.3-inch/03-e-blues-degrees.png" width="200" alt="E blues shown as scale degrees">
  <img src="Store/Screenshots/6.3-inch/05-chord-tables.png" width="200" alt="Chord tables for a key">
</p>

## What it does

- **Any scale, any key.** Pick a root and a scale; the notes appear on every string and
  fret, or on one or two piano octaves. Labels show note names, scale degrees, or nothing.
- **Chords of the scale.** The chords the scale makes sit under the board. A pentatonic or
  blues scale shows the chords it is played over instead: the chords of its major or minor
  key, or the I7, IV7 and V7 of a twelve-bar blues, with the notes the scale lacks shown
  faint. Tap one and its notes are ringed on the board, the notes row and the TV while the
  rest of the scale stays in view.
- **Change one note.** Lower, raise, add or remove a note straight from the Scales screen.
  The chords update in place and show what each one was, which note is new, and which
  chords the scale lost. The Transform tab explains the same change as a lesson.
- **Your own scales.** Changed scales can be saved under a name and chosen later like any
  built-in one.
- **Chord tables.** The Chords tab lists the chords of every major and minor key.
- **Drum trainer.** A groove written on the drum staff with a ball that bounces from count
  to count, for reading and playing along.
- **TV output.** Start AirPlay screen mirroring from Control Center and the app replaces
  the mirrored phone with its own full-screen board (Apple TV, Roku, AirPlay smart TVs).
  The AirPlay button in the app casts the board as video instead. A full-screen board on
  the phone or iPad covers the case where no TV is around.
- **iPhone, iPad and Mac.** One codebase; the Mac version is built with Mac Catalyst and
  handed out as a download.
- **Android phones, tablets and TVs.** A Kotlin twin of the same app, with its own screen
  for the TV remote on Android TV, Google TV and Fire TV (see [Android](#android)).

## Requirements

- iOS or iPadOS 26 or later; macOS for the Catalyst build.
- Xcode 26 or later with Swift 6.
- [xcodegen](https://github.com/yonaskolb/XcodeGen) to generate the Xcode project.
- Android: runs on Android 9 (Fire OS 7) or later; building needs JDK 17 and the Android
  command-line tools (Homebrew's `openjdk@17` and `android-commandlinetools`).

## Building

The Xcode project is generated from `project.yml`; edit the yml rather than the
`.xcodeproj`, which is git-ignored.

```sh
make build                                  # iPhone simulator build
make run                                    # build, then open it in the simulator
make test                                   # unit and UI tests
make test SIM="iPad Pro 13-inch (M5)"       # the same on an iPad
make run-device DEVICE="<your iPhone>"      # install on a plugged-in phone
make lint                                   # swift-format, strict
make format
```

Mac:

```sh
make mac            # Catalyst build, signed for this Mac only
make run-mac
make install-mac    # Release copy into /Applications/Scales.app
```

`scripts/build_mac_dmg.sh` and `scripts/notarize_mac_dmg.sh` produce a notarised disk
image, and `scripts/deploy_to_store.sh` uploads to App Store Connect; both need the
owner's certificates.

## Android

The Android app lives in `android/`: one APK for phones, tablets, Android TV, Google TV and
Fire TV, with a touch screen (`PhoneActivity`, Material 3) and a remote-control screen
(`TvActivity`, TV Material). It mirrors the Swift code file for file: `android/core` is the
pure-Kotlin twin of `Scales/Music`, `Scales/Session` and `Scales/Drums`, `android/ui` holds the
board painters both screens share, and `android/app` holds the screens. Its make targets live
in `scripts/android.mk`, which the root Makefile includes.

```sh
make android-setup         # once: SDK 37, build tools, the four emulators (~4.2 GB)
make build-android         # debug APK
make test-android          # JVM tests of core, ui and app (no emulator)
make test-android-ui       # Compose interface tests on the phone and TV emulators
make run-android           # install and open on the phone emulator
make run-android-tv        # the same on the TV emulator
make run-fire FIRE=<ip>    # a Fire TV stick with ADB debugging on
make lint-android          # ktlint, detekt and Android lint
make screenshots-android   # store screenshots into Store/android/screenshots/
make bundle-android        # release AAB and APK, checked by scripts/check_apk.sh
make android-bump          # versionCode + 1 before a store upload
```

Emulators, all arm64 and booted on demand by `scripts/android_boot.sh`: `scales_phone`
(Pixel 8, port 5554), `scales_tv` (Google TV 1080p, 5556), `scales_tablet` (Pixel Tablet,
5558) and `scales_floor` (Android 9, standing in for Fire OS 7, 5560). Homebrew's
command-line tools and JDK 17 are expected; `ANDROID_HOME` and `JAVA_HOME` override them.

Build outputs land under `android/app/build/outputs/`: `apk/debug/app-debug.apk`,
`bundle/release/app-release.aab` (for Play) and `apk/release/app-release.apk` (for Amazon;
`app-release-unsigned.apk` without the owner's upload key). Store text, assets and the
owner's upload steps are in `Store/android/listing.md`.

**One feature list for every platform.** `features.json` registers each feature with a
status for iOS, Android phone and Android TV. A test proves a feature by carrying a
`// feature: <id>` comment just above it, and `make parity` checks that every required cell
has such a test (`make features` prints the table). `expected/answers.json`, written from the
Swift code by `make answers`, is what the Kotlin tests compare against, so both apps spell
and place every note the same way. `make verify` runs parity, the twin check, both lints,
the answers check and every unit test and build (~4 min); `make verify-all` adds the
interface tests on the iOS simulators and the Android emulators.

## Layout

| Folder | Holds |
| --- | --- |
| `Scales/Music` | Pure theory with no UI: note spelling, scale formulas, tunings, fret and key positions, chord stacking, scale recognition. |
| `Scales/Session` | The practice settings shared by the phone screen and the TV screen, and the saved-scale library. |
| `Scales/Views` | SwiftUI views: fretboard, keyboard, chord chips, edit menus, the Transform and Chords tabs, the TV scene. |
| `Scales/Cast` | AirPlay video casting: the board rendered to a short H.264 clip and served to the TV. |
| `Scales/Drums` | The drum groove player and staff renderer. |
| `ScalesTests`, `ScalesUITests` | Unit tests of the theory and session code; UI tests that drive each tab and take the store screenshots. |

## Notes on the TV path

An app cannot start screen mirroring by itself; the player starts it from Control Center.
Once mirroring runs, iOS offers the foreground app a scene for the external display, and
Scales attaches its board to it. Capture tools such as QuickTime never offer that scene.
The in-app AirPlay button takes a different route: the board is encoded once as a looping
video and streamed to the TV over the local network, which needs the Local Network
permission.

## License

Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>.

Scales is free software under the [GNU General Public License v3.0 or later](LICENSE).
You may use, study, change and redistribute it, provided any copy or derived work is
shared under the same license with its source available. See [COPYRIGHT.md](COPYRIGHT.md).
