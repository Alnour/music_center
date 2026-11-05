# Scales: shared-code strategy for more platforms

Written 5 November 2026 against the code as it stood at 15:01. Line counts come from `wc -l` and include comments and blank lines.

**The short version.** Keep two native codebases, Swift for Apple and Kotlin for Android. The whole iOS app already compiles for Apple TV apart from its app shell, so Apple TV needs about 200 new lines once the code is sorted into shared folders. Android gets a full Kotlin port of the logic, kept honest by an answers file generated from the Swift code. Scales, tunings, colours, proportions and screen text move into data files, so most updates touch no code on any platform.

**Read this first.** A scale transformation feature was being added while this was written. Its logic landed between 14:43 and 14:47 and its Transform tab has no screens yet. It lets players edit scales, save their own, and read a chord change report. This changes the strategy: theory now has to run on the device, and Music grew from 589 to 1,250 lines. The refactor below starts only after that feature is finished, and the counts must be taken again at that point.

## 1. Testing the earlier proposal

| Claim | Verdict | Evidence |
|---|---|---|
| Two native codebases with matching layers | **Keep** | Every alternative lost, see below. |
| A linked local package `ScalesKit` | **Change** | About 171 declarations would need `public`, plus hand-written public initialisers. Compiling a shared folder into both Apple apps costs none of that. A small `Package.swift` still gives fast tests: in a scratch copy the Music code and settings built as a macOS package, and all new theory tests passed with `@testable import Scales` unchanged. A cold run took 15 s, a repeat 0.8 s, against 44 s through the simulator. |
| `expected-answers.json` for every key, scale and tuning | **Change** | The full product is 1,734 boards and 675 KB, and only 690 are distinct. Prototype sizes: 289 spellings with chords 35 KB, 360 edit reports in C 161 KB. It must also cover edits, chord reports, settings and board drawing, which the proposal lacked. |
| Colours and proportions in a data file | **Keep and extend** | Add scales, tunings and screen text. |
| Apple TV has no web view | **True** | No WebKit framework in the tvOS 27.0 SDK on this Mac. |
| Roku runs only BrightScript | **Out of date** | SceneGraph with BrightScript is still standard, and Roku now documents a TypeScript SDK in beta. |
| Flutter and Compose Multiplatform do not draw on tvOS | **True for official releases only** | JetBrains declined tvOS on 9 Oct 2026. Community forks exist for both (`flutter-tvos`, `compose-tvos`), and at least one tvOS App Store app is reported to use the Compose fork. |
| About 1,350 of 2,000 lines port to tvOS | **Wrong, too low** | I built a real tvOS target. After removing `AppDelegate.swift`, `TVSceneDelegate.swift` and one `.statusBarHidden()` line, everything compiled. About 2,050 of today's 2,637 non-casting lines are worth sharing. |
| Android covers Fire TV | **Only partly** | See below. |

**Fire TV is two platforms.** Amazon's device table lists the 2025 and 2026 sticks (4K Select, 4K 3rd Gen, HD 2026) on Vega OS, which runs React Native or web view apps and no Android apps. Fire OS devices such as the 4K Plus and 4K Max still take an Android build. Every stick in that table is 32-bit.

**The compile result has a limit.** It proves the code builds for tvOS. No tvOS simulator runtime is installed, so nothing has been run, and whether the forms and menus feel right with a remote is unverified.

### Alternatives rejected

- **Swift compiled for Android (official since Swift 6.3).** It would save about 500 lines, which makes it the strongest alternative. It is rejected because Fire TV sticks run 32-bit apps, and Swift's 32-bit Android target is undocumented on swift.org's getting-started page and lightly tested. Google also requires native-code TV apps to ship both 32-bit and 64-bit builds, and the runtime adds roughly 15 MB per architecture. A pure Kotlin app avoids all of this with one APK. Reconsider if 32-bit support becomes documented and tested.
- **Kotlin Multiplatform for logic.** It means rewriting 1,250 tested Swift lines in Kotlin and adding Gradle to the Apple build, while Swift export is still alpha and its tvOS support is unconfirmed.
- **Precomputed tables replacing theory code on every platform.** I favoured this until the transformation feature arrived; player-made scales need spelling and chord logic on the device.
- **Skip (SwiftUI to Compose).** Its documentation mentions neither Android TV nor D-pad focus, and the board depends on Canvas, whose support I could not confirm.
- **Flutter, Compose Multiplatform or React Native as one app.** The owner asked for native apps, and tvOS would rest on community forks.
- **A shared web view renderer.** Apple TV has no web view.
- **Theory rules as data.** Spelling and chord stacking are algorithms, and a rule format would be a worse programming language.
- **Generating the settings struct from a schema.** The planned `customScale` field needs custom types, and an answers-file check catches drift for far fewer lines.
- **Reading data files at launch.** Each platform would need a field-by-field model, while generated constants need none and turn a removed key into a compile error.

## 2. Inventory

"Shared" means compiled into both iPhone/iPad and Apple TV. Android ports the same files to Kotlin, except those marked platform-only.

### Shared as-is: 1,184 lines

| File | Lines | File | Lines |
|---|---:|---|---:|
| `Music/ChordChangeReport.swift` | 185 | `Music/KeyChordTable.swift` | 80 |
| `Music/ScaleEdit.swift` | 142 | `Music/ScaleDefinition.swift` | 46 |
| `Music/ScaleChord.swift` | 105 | `Music/ScaleIdentifier.swift` | 44 |
| `Music/Triad.swift` | 102 | `Music/FretboardLayout.swift` | 38 |
| `Music/NoteName.swift` | 94 | `Music/PitchMath.swift` | 19 |
| `Music/TransformationLesson.swift` | 94 | `Views/ScaleNotesRow.swift` | 38 |
| `Music/ScaleDegree.swift` | 87 | `Views/TVDisplayView.swift` | 51 |
| `Music/Scale.swift` | 59 | | |

`NoteName` and `KeyChordTable` can still shrink (section 6), and sharing them does not depend on it.

### Shared after a small change: 1,089 lines

| File | Lines | Change |
|---|---:|---|
| `Views/ContentView.swift` | 265 | Move `ScaleChoiceSection` and `DisplayOptionsSection` (about 50 lines) to a shared file. The rest stays iOS-only. |
| `Views/ChordTablesView.swift` | 237 | Add a third size set for TV. Move footnotes to `text.json`. Drop `spokenName`, which now duplicates `Triad.spokenName`. |
| `Views/FretboardView.swift` | 166 | Split into a pure list of shapes and a thin painter. |
| `Music/ScaleType.swift` | 101 | Generate from `music.json`, keeping a short `degrees` extension. |
| `Views/FretboardGeometry.swift` | 88 | Merge into the shape list, using `Double` in place of `CGFloat`. |
| `Session/PracticeSession.swift` | 80 | Move `caster`, `tvStatus`, `isTVConnected`, `isPhoneMirrored` and `TVStatus` into an iOS-only `CastSession`. |
| `Views/FretboardPalette.swift` | 65 | Generate the two palettes from `design.json`. |
| `Music/Tuning.swift` | 54 | Generate from `music.json`. |
| `Session/PracticeSettings.swift` | 33 | Add a forgiving decoder (see the saved settings trap). Move titles to `text.json`. |

### Platform-only (iPhone and iPad): 1,528 lines

| File | Lines | File | Lines |
|---|---:|---|---:|
| `Cast/AirPlayCaster.swift` | 325 | `App/TVSceneDelegate.swift` | 52 |
| `Cast/BoardVideoWriter.swift` | 325 | `Views/CaptureStateReader.swift` | 50 |
| `Cast/ClipServer.swift` | 250 | `Views/FullScreenBoardView.swift` | 50 |
| `Cast/ClipExtender.swift` | 180 | `Views/RootView.swift` | 15 |
| `Views/AirPlayViews.swift` | 118 | `App/ScalesApp.swift` | 14 |
| `Cast/ClipCache.swift` | 84 | `App/AppDelegate.swift` | 65 |

### Tests and tooling

`ScalesTests/MusicTheoryTests.swift` (358) splits: theory, settings and session tests move to the shared tests; clip, HTTP and cast tests stay iOS-only. `TransformationTests.swift` (443) moves whole. `ScalesUITests` (199 in three files) stays iOS-only, with feature tags added.

### Dead weight and problems found now

- **`Scales/Info.plist` (62 lines) is generated.** I deleted it in a scratch copy, and xcodegen rebuilt an identical file. Add it to `.gitignore`.
- **`FretboardLayout.frets(withToneOnString:)` (8 lines) has no app callers.** Move it to the tests.
- **Note-name joining is written four times, and the ♭/♯ prefix twice.** One helper each saves about 13 lines.
- **`make archive` and `make upload` duplicate `deploy_to_store.sh`** and skip its build number bump. Remove them.
- **The Makefile's inline Python comprehension breaks the code style.** Move it to `tools/`.
- **Scales has no version control.** Git sees 0 tracked files under `scales/`, and the folder sits untracked inside `personal_projects`. No CI exists either.
- **The saved settings trap.** Adding a non-optional setting makes the whole saved blob fail to decode, and `try?` then silently resets every player's settings. I confirmed the failure with a test. The planned optional `customScale` is safe; the next ordinary setting will not be.
- **Stale board clips.** Changing a colour leaves old clips on phones until someone remembers to bump `clipFormatVersion`, and `GMajorBoard.mp4` has no script to rebuild it.
- **A release trap for Apple TV.** `deploy_to_store.sh` greps `project.yml` for `CFBundleVersion`, and there is exactly one match today. A second target that repeats that line breaks the script's arithmetic.

## 3. Architecture

**The rules.** Anything that is a value lives once in `data/`. Anything that is logic lives in Swift `Core/`, with Kotlin `core/` as a port checked against generated answers. Screens are native on each platform. Board drawing becomes a pure list of shapes (lines, dots, labels with colour names), so all four app families can be checked against the same numbers without comparing pixels.

```
scales/
  project.yml  Makefile  Package.swift  CLAUDE.md     stay at the root, so deploy paths still work
  data/
    music.json       scales, families, tunings, roots, key table roots, fret choices, inlay frets
    design.json      both palettes and every board and TV proportion
    text.json        screen labels and footnotes
    features.json    the parity list, section 5
  expected/
    answers.json     generated from Swift, read by every port's tests
  tools/
    generate.py      data/ to generated Swift and Kotlin
    ExpectedAnswers/ Swift program that writes answers.json
    check_parity.py  fails when a required feature has no test on a platform
    make_icons.py    per-platform icons from the two master layers
  Core/              Swift, no UI imports: Music, Board, Session, Generated
  SharedViews/       SwiftUI used by both Apple apps
  Scales/            iPhone and iPad only: App, Cast, phone screens, Transform screens
  ScalesTV/          Apple TV only
  CoreTests/         shared logic tests, run by swift test
  ScalesTests/  ScalesUITests/                        iOS-only tests
  Store/             Icon/ layers, listing.md, ExportOptions.plist, Screenshots/<device>/
  android/
    core/            pure Kotlin module with no Android imports: music, board, session, generated
    ui/              Compose Foundation only: board, TV layout, notes row, chord table
    app/             one APK for phone, tablet and TV, with Material 3 and TV Material screens
```

**Apple.** `project.yml` lists `Core` and `SharedViews` as sources of both app targets, so no `public` and no `import`. `Package.swift` exists only for testing: it compiles `Core` as a module named `Scales` on macOS, which runs tests in about a second and fails if anyone imports UIKit into `Core`. The apps do not link it. Both apps use the bundle identifier `com.alnourkhalifa.Scales` for one store listing.

**Android.** One Kotlin APK with **no native code**. A build check asserts the APK has no `lib/` folder. That keeps it outside Google's dual-architecture rule and lets the same file run on 32-bit sticks and phones. Use `minSdk 28` (Fire OS 7, Android 9). Keep phone and TV screens in separate files, because Android's TV components use `tv-material` and third-party guides warn that mixing it with phone Material breaks focus. Keep `android/core` free of Android imports so a later web TV port has a clean starting point.

**Generated code.** `make data` runs both generators. Outputs are committed, so Android builds need neither Python nor Swift.

### Where each thing lives

| Kind of thing | Single source | Reaches each platform by |
|---|---|---|
| Theory rules | `Core/Music/*.swift` | Apple compiles it. Android ports it and is checked by `answers.json`. |
| Scale and tuning definitions | `data/music.json` | Generated enums, keeping `.major` and the saved format. |
| Colours and proportions | `data/design.json` | Generated constants. The file's hash joins the clip cache key. |
| Screen text | `data/text.json` | Generated constants, including accessibility labels. |
| Theory sentences | `Core/Music` code | Android ports them, and `answers.json` pins every word. They stay in code because they are assembled by logic. |
| Settings schema | `Core/Session/PracticeSettings.swift` | Hand-written on each platform. `answers.json` holds every key and default. |
| App icon | `Store/Icon/` (background and foreground layers) | `make_icons.py`. Today's icon is flat shapes on a flat colour, so separating the layers is simple. tvOS sizes are unverified. |
| Store text and screenshots | `Store/listing.md`, `Store/Screenshots/` | Copied into each store by hand. |

## 4. The update workflow

Run `make data` after any change in `data/` or `Core/`. "Android UI" means `android/ui` or `android/app`.

| Update | Apple files | Android files | What catches a platform left behind |
|---|---|---|---|
| **Add a scale** | `data/music.json` (one line plus its family) | None | The freshness check fails if `make data` was skipped. |
| **Add a tuning** | `data/music.json` (one line) | None | Same. |
| **Change a colour** | `data/design.json`, then `make board-clip` if a default-board colour changed | None | A test compares the design hash stored with `GMajorBoard.mp4`. Cached clips rebuild on their own. |
| **Fix a theory bug** | `Core/Music/<file>.swift` and a test in `CoreTests/` | The matching file in `android/core` | `answers.json` regenerates, so the Kotlin test fails at the exact case until it is ported. |
| **Add a setting** | `PracticeSettings.swift` (field and decoder line), `data/text.json`, the shared options view | `PracticeSettings.kt`, phone options, TV options | The settings section of `answers.json` fails Kotlin's key check. A feature entry fails `check_parity.py` until each platform's UI test is tagged. |
| **Add a screen** | Logic in `Core/`, views in `SharedViews/` or the app folder, `data/text.json`, `data/features.json` | Same logic in `android/core`, screen in Android UI | `check_parity.py` fails until every required platform has a tagged test. |

**`make verify` runs everything:** data freshness, `swift test`, iOS and tvOS builds, Kotlin tests, parity check, formatters and linters (swift-format, ruff, ktlint) with line-length and complexity rules, and the APK native-code check.

**The limit of the parity check.** It proves a tagged test exists, and a reviewer still has to judge that the test means something. Visual differences between platforms are caught only by looking at screenshots.

## 5. Feature parity

`data/features.json` is the live version of this table. "Yes" means required.

| Feature | iPhone, iPad | Apple TV | Android phone, tablet | Android TV, Fire OS |
|---|---|---|---|---|
| Board with every scale tone, roots highlighted | Yes | Yes | Yes | Yes |
| 17 roots with correct spelling, all scales grouped by family | Yes | Yes | Yes | Yes |
| Six tunings; 12, 15, 17, 22 or 24 frets | Yes | Yes | Yes | Yes |
| Labels as notes, degrees or dots | Yes | Yes | Yes | Yes |
| Left-handed, and thin string on top | Yes | Yes | Yes | Yes |
| Spelled notes row with degrees | Yes | Yes | Yes | Yes |
| Light and dark board; TV layout filling a 16:9 screen | Yes | Yes | Yes | Yes |
| Settings saved between launches; screen stays awake | Yes | Yes | Yes | Yes |
| Chord tables, including harmonic minor stand-ins | Yes | Yes | Yes | Yes |
| Accessibility labels on board, notes and chords | Yes | Yes | Yes | Yes |
| Edit a scale, read the chord report, name and save it, keep lessons | Yes | No | Yes | No |

These are legitimately platform-only:

- **iPhone and iPad:** AirPlay casting, the clip pipeline and clip server, the TV scene during mirroring, full-screen fallback, orientation lock, and the iPad "turn sideways" hint.
- **Apple TV, Android TV, Fire OS:** remote and D-pad focus handling.
- **Android phone:** casting is not required in the first release (decision 3).

Rules for the planners: English only, the board looks the same everywhere apart from the system font, and each platform should use its own native controls. One check to make early: the double sharp and double flat glyphs (𝄪 𝄫) are unverified on Android TV and Fire OS fonts. C♯ Lydian already needs a double sharp. If they fail, ship a small subset font.

## 6. Line budgets

Treat each target as a ceiling. Apple figures are measured or derived from the code. Kotlin figures and anything marked "planned" are estimates. Generated files and data files do not count.

| Module | Now | Apple target | Android target |
|---|---:|---:|---:|
| Theory | 1,250 | 1,030 shared | 700 |
| Board geometry and drawing | 254 | 185 shared | 165 |
| Palette | 65 | 20 shared | 12 |
| Settings and session | 113 | 85 shared, 45 iOS | 70 |
| Scale library (planned, about 90) | 0 | 90 shared | 60 |
| TV layout and notes row | 89 | 89 shared | 80 |
| Chord tables | 237 | 215 shared | 170 |
| Practice options | 50 | 50 shared | 180 (phone 70, TV 110) |
| Phone screen, rest of `ContentView` | 215 | 205 iOS | 130 |
| Transform screens (planned, about 400) | 0 | 400 iOS | 300 |
| Root and full-screen views | 65 | 65 iOS | 30 |
| Casting, with AirPlay views and capture reader | 1,332 | 1,332 iOS | 0 |
| App shell | 131 | 131 iOS | 40 |
| TV-only screens | 0 | 200 tvOS | 120 |

| Total | Lines |
|---|---:|
| Today, iPhone and iPad | 3,801 |
| Today plus planned Transform work, with no refactor | about 4,290 |
| **iPhone and iPad after refactor** (1,764 shared, 2,178 iOS-only) | **3,942** |
| **Apple TV** (the same 1,764 shared, 200 new) | **1,964** |
| **Android** (about 950 logic, 1,110 UI) | **2,060** |
| Tools, once for all platforms | 330 |
| Data files | about 215 |

The Android total adds about 300 if casting is wanted. Kotlin tests should stay near 150 lines, because most checks live in `answers.json`. Tool budget: `generate.py` 140, `ExpectedAnswers` 110, `check_parity.py` 40, `make_icons.py` 40.

### How the cuts stay readable

- **Scales and tunings as data** remove 155 lines of parallel switches, and one scale becomes one line.
- **Lookup arrays** replace the two nine-line switches on `Letter`, and generated root lists remove about 50 lines from `NoteName` and `KeyChordTable`.
- **Small shared helpers** remove the duplicates listed in section 2.
- **The shape list** cuts drawing by about 70 lines and gives tests something to check.
- **Kotlin data classes and shorter docstrings.** Each Kotlin file carries a short "why" and points to the Swift file for the full reasoning. All Kotlin follows the owner's code style, with plain loops and one action per line.

### Where I refused to cut

- **Casting stays at 1,332 lines.** Its oddities are hard-won fixes for real Roku failures, recorded in `CLAUDE.md`, and only a real TV can test them.
- **`AppDelegate`'s scene method stays.** It may be redundant now that the scene is declared in `Info.plist`, but only a real TV can prove that.
- **The 190 comment lines in Music stay.** They explain the musical reasons, which the owner's style requires.
- **`ChordTableMetrics` stays explicit,** and it grows a TV set. Merging the sets into formulas would save lines and hide the design.
- **Geometry keeps its named properties** after merging, because each name states a piece of board anatomy.
- **Theory sentences stay as plain string building.** A template system would shorten nothing.

## 7. Refactor before any port

This starts after the Transform tab lands. Steps are ordered, and each ends with `make test` on an iPhone and an iPad simulator, plus a commit. Expect about 3 to 4 hours of agent work, plus about an hour of the owner's review and real-device testing.

1. **Put Scales under version control** (decision 1) and add `Scales/Info.plist` to `.gitignore`.
2. **Capture the baseline.** Save `make screenshots` and `make screenshots-ipad` output. Add tests that pin a frozen saved-settings JSON, the `ClipCache.key` hex for default settings, and a hash of `TVDisplayView` rendered at six settings combinations.
3. **Make settings forgiving.** Add `PracticeSettings.init(from:)` so a missing or unknown field falls back to its default alone.
4. **Separate casting state.** Move the cast-related members of `PracticeSession` into `CastSession`.
5. **Split `ContentView`.** Move the two options sections to their own file.
6. **Remove the small dead weight:** test-only helper, duplicated helpers, Makefile targets.
7. **Create the folders** and move files into `Core/` and `SharedViews/`. Update `project.yml`, the Makefile lint paths and `CLAUDE.md`. No code changes in this step.
8. **Add `Package.swift` and `CoreTests/`,** and move shared tests there. Confirm `swift test` passes.
9. **Add `generate.py` with colours and proportions first.** The design hash joins the clip cache key, deliberately changing cached clip names once, and `make board-clip` is scripted here. The clip key test from step 2 updates in this step only.
10. **Generate scales, tunings and roots** from `music.json`.
11. **Move screen text** to `text.json`.
12. **Extract the board shape list** with its painter.
13. **Add `ExpectedAnswers` and `expected/answers.json`:** all 289 spellings with chords, all 17 scales across all six tunings in one key, every legal edit in C with sentences and reports, default settings, and the shapes for six boards at a fixed size.
14. **Add `features.json`, `check_parity.py`, `make verify` and CI** (decision 1). Tag the iOS UI tests.
15. **Prove the Apple TV target:** add an empty tvOS target that compiles `Core` and `SharedViews`. Before building it, make the deploy script safe: one shared build number, not a second `CFBundleVersion` line. The Apple TV plan still has to add a tvOS archive, because the script builds for iOS only.

### Proving nothing changed for iPhone and iPad users

- The new and existing unit and UI tests pass on both simulators.
- `TVDisplayView` render hashes match the baseline at every step.
- Store screenshots match the baseline pixel for pixel, compared at the end.
- A saved-settings file from the current release decodes to the same values.
- Generated enums produce identical `displayName`, `formula` and open strings for every case. Compare this once against the old code before deleting it.
- **The owner installs a build over the current store version on a real iPhone,** then checks saved settings, casting to the Roku, taking the TV back from another app, and a real iPad. Casting has no automated test.

## 8. Decisions for the owner

1. **Where does the code live, and where does CI run?** Scales needs its own private GitHub repository. I recommend running `make verify` from a local pre-push hook, with GitHub Actions on Linux for the data freshness check and Android tests. macOS runners cost more on private repositories (rates unverified), so Apple checks stay on your Mac.
2. **Does Transform go on TVs?** I recommend phones and tablets only, as the parity table assumes. Editing with a remote is clumsy, and saved scales would need sync to reach a TV. Adding it would cost about 200 extra Android lines, and the tvOS cost is unverified.
3. **Does Android cast from phone to TV in the first release?** I recommend no. The TV app is the TV path on Android, and AirPlay is unavailable there, so Roku owners on Android would miss out. Adding it later costs about 300 lines, with the mechanism left to that plan.
4. **What about Vega Fire TV sticks?** I recommend skipping them for now, and saying so plainly in the Amazon listing. Fire OS devices get the Android build. Newer sticks would need a separate web or React Native app.
5. **What order for later platforms?** I recommend Apple TV, then Android phone and tablet, then Android TV and Fire OS. Decide on Samsung, LG and Roku after that. A single TypeScript web app could reach Samsung, LG and Vega's web view, and possibly Roku through its beta SDK. That would be one more theory port, checked by the same answers file. How well it runs on each is unverified.
6. **One Apple store listing?** I recommend yes: the same app record and bundle identifier for iPhone, iPad and Apple TV.
7. **Other languages?** I recommend English only for now. `text.json` can grow per-language tables later, but theory sentences would need work.

## Unverified

- Nothing has run on tvOS, and no Android or Kotlin code has been built. This Mac has no Android SDK or Java runtime.
- Swift for Android was assessed from documentation and was not compiled.
- Kotlin budgets and the planned Transform line counts are estimates.
- Jetpack Compose on Fire OS, and the `minSdk 28` floor against Compose's own minimum.
- Double sharp and flat glyphs on Android TV and Fire OS.
- tvOS icon layers and sizes.
- Swift export's tvOS support.
- Kotlin's tvOS support tier, which came from a secondary source.
- `flutter-webos` public availability, which only matters if Flutter is revisited.

## Sources

- [Swift 6.3 release](https://www.swift.org/blog/swift-6.3-released/), [Swift SDK for Android guide](https://www.swift.org/documentation/articles/swift-sdk-for-android-getting-started.html), [SDK announcement thread, on armv7](https://forums.swift.org/t/announcing-the-swift-sdk-for-android/82845), [Swift forums on Android app size](https://forums.swift.org/t/android-app-size-and-lib-foundationicu-so/78399), [runtime size report](https://ignit.group/blog/swift-on-android-a-practical-use-case)
- [Google: 64-bit TV apps](https://android-developers.googleblog.com/2025/08/64-bit-app-compatibility-for-google-tv-android-tv.html), [Compose on Android TV](https://developer.android.com/training/tv/playback/compose)
- [Amazon: Fire TV device specifications](https://developer.amazon.com/docs/device-specs/device-specifications-fire-tv-streaming-media-player.html), [Vega SDK overview](https://developer.amazon.com/docs/vega/0.24/vega-overview), [Vega web apps](https://developer.amazon.com/docs/vega/0.24/develop-your-app-with-webview)
- [Roku TypeScript SDK (beta)](https://developer.roku.com/dev/docs/rsg-sdk-standing-docs-getting-started-overview), [Roku development overview](https://developer.roku.com/dev/docs/roku-overview)
- [JetBrains on tvOS in Compose](https://github.com/JetBrains/compose-multiplatform/pull/5512), [compose-tvos](https://github.com/sajidalidev/compose-tvos), [flutter-tvos](https://github.com/fluttertv/flutter-tvos/releases/tag/v3.47.5-tvos.1.10.4), [Kotlin 2.4 notes](https://kotlinlang.org/docs/whatsnew24.html), [Kotlin 2.3 notes](https://kotlinlang.org/docs/whatsnew23.html)
- [Skip documentation](https://skip.dev/docs/faq/), [Samsung Flutter for Tizen](https://developer.samsung.com/smarttv/develop/native/flutter.html)
