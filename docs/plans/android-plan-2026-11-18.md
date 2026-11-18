# Scales for Android: implementation plan (18 November 2026, revision 2)

Revision 2 folds in Alnour's seven review points: per-agent git worktrees with pre-wired stubs
and an integration commit per wave; an immutable session snapshot for Compose plus a casting
protocol so `PracticeSession` enters the answers file; a key-value interface under
`SettingsStore`; a streaming `AudioTrack`; a bundled accidentals font from day one;
`android-setup` before the first build and a symlink-based SwiftPM target pinned to Swift 6
mode; Fire OS routing from `PhoneActivity` to `TvActivity`.

## Context

Scales is a SwiftUI app (iPhone, iPad, Mac Catalyst, ~5,500 non-casting lines) that draws any
scale on a guitar neck or piano keyboard, shows the scale's chords, lets the player change one
note in place, keeps lessons and saved scales, and has a drum trainer. Alnour asked for an
Android clone. Decisions taken today:

- Targets: Android phone, tablet, Android TV and Fire OS. One APK, two launcher activities.
- Features: everything the iOS app has today. AirPlay casting, the external-display TV scene
  and capture-state detection have no Android equivalent and are out; Google's built-in Cast
  screen mirrors the full-screen board instead.
- Faithfulness: one `expected/answers.json` written from the Swift code, read by Kotlin tests.
  No `data/` JSON, no code generation, no `Core/` folder move.
- Location: `android/` in this repository, on a branch `android`. Each agent works in its own
  git worktree on a sub-branch; the orchestrator merges and flips `features.json` in one
  integration commit per wave; `android` merges to `main` after each owner gate.
- TV scope: chord chips and in-place note change reach the TV (lesson saved to the TV's own
  library); the Transform tab (lesson list, naming, rename, delete) stays phone and tablet.
  Drums reach the TV with a focusable control row. Keep-awake on TV is a player-started
  30/60/90-minute timer, never saved.
- Governing rule (Alnour, verbatim): "write the code in a way when I request new feature
  it's added and tested across all platforms". Section 1 is the answer and shapes everything.
- Subagents: Opus, as many in parallel as the dependency graph allows (section 7 waves), each
  with `isolation: "worktree"`.

`docs/plans/android-plan.md` (5 Nov 2026) stays the reference for the TV manifest, D-pad focus
rules, 5% overscan, store checklist, Play/Amazon assets and risks. This plan supersedes it where
the app has grown since (keyboard instrument, chord chips with rings, in-place note edits,
toolbar row with popovers, drum trainer, four tabs) and where today's checks disagree. Wave 0
copies this plan into `docs/plans/android-plan-2026-11-18.md`.

Uncommitted work in the tree (toolbar/popover rework of `ContentView.swift`,
`ScaleChordsRow.swift`, UI tests, `CLAUDE.md`) is committed first on `main`; the answers file
and the test tags are taken from committed code.

---

## 1. The cross-platform feature workflow

### 1.1 File-for-file mirroring (the rule)

- Every file in `Scales/Music`, `Scales/Session`, `Scales/Drums` (minus `DrumPlayer.swift`) has
  one Kotlin twin of the same name under
  `android/core/src/main/kotlin/com/alnourkhalifa/scales/core/{music,session,drums}/X.kt`,
  with the same type and member names (`Scale.letterTriads()`, `ScaleDefinition.applying(edit)`,
  `ScaleEdit.menuTitle(aboveRoot)`, `DrumClock.ballPosition(progress, stepsPerBar)`,
  `PracticeSession.transform`, `revertBoardChange`, `currentBoardChange`, `ringedNotes`,
  `toggleRing`). Each Kotlin header docstring names its Swift twin and states only what differs.
- Geometry mirrors too: `Scales/Views/FretboardGeometry.swift`, the new
  `Scales/Views/KeyboardGeometry.swift` and `Scales/Views/DrumStaffLayout.swift` ↔
  `core/board/{FretboardGeometry,KeyboardGeometry,DrumStaffLayout}.kt`. No shape-list
  intermediate: numbers are pinned by `answers.geometry`.
- View files mirror by name into `android/ui/` (shared painters), `android/app/.../phone/` or
  `android/app/.../tv/`. Private helper views Swift keeps inside a file (`BoardToolbar`,
  `BoardSettingsMenu`, `TVMenu` in `ContentView.swift`) get their own Kotlin files whose
  docstring names the Swift source. TV-only files carry a `Tv` prefix.
- `scripts/check_mirror.py` (~30 lines, `python3 -I`): fails when a Swift logic file has no
  same-named `.kt` or vice versa. `SWIFT_ONLY = {"Scales/Drums/DrumPlayer.swift",
  "Scales/Session/BoardCaster.swift"}`; a `PENDING` set shrinks per wave.

### 1.2 `features.json` (repository root, the single source)

```json
{ "platforms": ["ios", "androidPhone", "androidTv"],
  "features": [
    { "id": "chord-chips-rings", "title": "Chord cards under the board; tapping one rings its notes",
      "platforms": { "ios": "required", "androidPhone": "required", "androidTv": "required" },
      "test": "ui" },
    { "id": "transform-lessons", "title": "Transform tab: lessons, chord report, My Scales",
      "platforms": { "ios": "required", "androidPhone": "later", "androidTv": "no" },
      "laterUntil": "wave 3", "reason": "no typing with a remote", "test": "ui" } ] }
```

- Status per platform: `required`, `later` (needs `laterUntil`), `no` (needs `reason`).
- `test`: `ui` (tag must be in an interface-test folder), `any` (a logic test is enough; Android
  `core` tests count for both Android columns), `manual` (printed, never fails; needs a reason).
- `make features` prints the table. Only the orchestrator's integration commit edits this file
  during a wave, so parallel agents never collide on it.

Seed (R = required):

| id | ios | phone | tv | test | existing iOS tests to tag |
|---|---|---|---|---|---|
| board | R | R | R | ui | `gMajorMatchesReferenceChart`, `testChoosingAScaleUpdatesTheTitle` |
| roots-scales | R | R | R | ui | `ScaleSpellingTests`, same UI test |
| tunings-frets | R | R | R | any | `dropDLowStringIsTheFifthOfG`, `layoutCoversOpenStringPlusEveryFret` |
| labels | R | R | R | ui | **missing on iOS: add a Notes/Degrees/Dots pill test** |
| left-handed-thin-top | R | R | R | any | **missing: add a `FretboardGeometry.stringIndex` test** |
| notes-row | R | R | R | ui | **missing: assert the `ScaleNotesRow` accessibility label** |
| tv-board (dark theme, 16:9 full-screen) | R | R | R | ui | `testFullScreenBoardOpensInLandscapeAndCloses` |
| settings-persist | R | R | R | any | `settingsSurviveARelaunch`, `freshInstallOpensOnGMajor`, `settingsSavedBeforeCustomScalesStillLoad` |
| keep-awake | R | R | R | manual→ui on tv | iOS idle timer; TV timer row in `TvBoardTest` |
| chord-tables | R | R | R | ui | `testChordTablesShowMajorAndMinorKeys` |
| chord-chips-rings | R | R | R | ui | `pickingAChordRingsItsNotesUntilTheScaleLosesOne`, `testScalesScreenChangesANote` |
| note-edit-revert | R | R | R | ui | `testScalesScreenChangesANote`, `BoardTransformTests` |
| transform-lessons | R | R | no | ui | `testTransformTabExplainsALoweredSeventh`, `testNamingANewScaleAddsItToMyScales` |
| keyboard (instrument, octaves) | R | R | R | ui | `testChoosingTheKeyboardShowsAnOctave`, `KeyboardLayoutTests` |
| drum-trainer | R | R | R | ui | `testDrumsTabPlaysAndPausesTheGroove`, `DrumTrainerTests` |
| accessibility-labels | R | R | R | ui | **missing: assert "Fretboard showing G Major" and a chip label** |
| tv-routing (TV device opens the TV screen) | no | no | R | any | `DeviceKindTest.kt` |
| airplay-cast | R | no | no | ui | `testTVMenuOffersAirPlayAndStatus`, `ClipCacheTests` |

Android cells start as `later` with `laterUntil` naming the wave. The first iOS `make parity`
fails until the four missing iOS tests are written (wave 0).

### 1.3 Tags and `scripts/check_parity.py` (~60 lines, `python3 -I`, standard library)

Tag: `// feature: id1, id2` on the line directly above `@Test func` (Swift Testing),
`func test…` (XCTest) or `@Test fun` (Kotlin). Sources and what they prove:

```
ScalesTests/**/*.swift               ios           logic
ScalesUITests/**/*.swift             ios           ui
android/core/src/test/**/*.kt        phone + tv    logic
android/ui/src/test/**/*.kt          phone + tv    logic
android/app/src/test/**/*.kt         phone + tv    logic
android/app/src/androidTest/.../app/phone/**/*.kt   phone   ui
android/app/src/androidTest/.../app/tv/**/*.kt      tv      ui
```

Fails on: unknown id, a tag not followed by a test within 3 lines, a `required` cell with no
qualifying tag. Warns on a tag for a `no` platform. Prints the `later` count; `--release` fails
if any remain (used by `bundle-android` and `deploy_to_store.sh`).

### 1.4 `expected/answers.json` written by Swift, read by Kotlin

**Generator: `Package.swift` at the root with the executable target at
`scripts/ExpectedAnswers/`, whose folder holds `main.swift` plus git symlinks to the Swift
sources** (`Music -> ../../Scales/Music`, `PracticeSettings.swift`, `PracticeSession.swift`,
`ScaleLibrary.swift`, `BoardCaster.swift`, `DrumBarRenderer.swift`, `FretboardGeometry.swift`,
`KeyboardGeometry.swift`, `DrumStaffLayout.swift`). SwiftPM follows symlinks and never scans
`android/`, `Scales/Views` or `DerivedData`. `swiftLanguageModes: [.v6]` and
`-strict-concurrency=complete` match `project.yml` (`SWIFT_VERSION 6.0`, strict concurrency
complete). Platform `.macOS(.v14)` for Observation. Warm run under 1 s, cold ~15 s, no simulator.
Fallback if symlinked sources misbehave: one `xcrun swiftc -target arm64-apple-macos14` line.

Swift changes required (no behaviour change; `make test` and the store screenshots prove it):
1. `Scales/Session/BoardCaster.swift` (new, ~15 lines): `protocol BoardCaster: AnyObject {
   var playbackError: String? { get }; var isPlayingOnTV: Bool { get }; var receiverName:
   String? { get } }` plus `final class NoCaster: BoardCaster`. `AirPlayCaster` adopts it.
   `PracticeSession.init(defaults:, caster: any BoardCaster = NoCaster())`;
   `shared = PracticeSession(defaults: .standard, caster: AirPlayCaster.shared)`; the four view
   call sites (`player`, `resume`, `cast`, `takeOverTVIfChosen` in `ContentView.swift`) use
   `AirPlayCaster.shared`. `PracticeSession.swift` then imports only Foundation and Observation
   and enters the generator; existing tests keep `PracticeSession(defaults:)`.
2. Move `KeyboardGeometry` (private in `KeyboardView.swift`) plus `whiteKeyHeightToWidth` and
   the black-key ratios into `Scales/Views/KeyboardGeometry.swift`, internal.
3. Move `DrumStaffLayout` out of `DrumTrainerView.swift` into `Scales/Views/DrumStaffLayout.swift`.
4. Move into `FretboardGeometry`: `stringIndex(forRow:stringCount:thinStringOnTop:)`, string
   stroke width `max(1, row·0.025)·(1+0.2·bassRank)`, inlay radius `max(2, r·0.16)`, wire
   `max(1, cell·0.02)`, nut `max(3, cell·0.06)`, the three font sizes, `singleInlayFrets`
   `[3,5,7,9,15,17,19,21]`, `doubleInlayFrets` `[12,24]`, `ChordRing.lineWidth` `max(1.5, d·0.1)`.
5. `ScaleLibrary.swift`: `private struct LibraryDocument` becomes internal.

Generator rules: encoder `[.sortedKeys, .prettyPrinted, .withoutEscapingSlashes]`; never write a
`Set`/`Dictionary` in stored order (sort `DrumGroove.voices(atStep:)` by `DrumVoice.allCases`,
`KeyChordRow.alternatives` by step, `KeyboardLayout.blackPitchClasses`); floats rounded to 6
decimals; fixed UUID `00000000-0000-0000-0000-000000000001` and
`Date(timeIntervalSinceReferenceDate: 800_000_000)`; `UserDefaults(suiteName:)` scratch suite
and a temporary library folder for the `session` section.

Sections:

| Section | Pinned output |
|---|---|
| `meta`, `roots` | format version 1; `NoteName.rootChoices` (17): letter, accidental, `displayName`, `pitchClass` |
| `scaleTypes`, `families` | 17 `ScaleType`: `displayName`, `formula`, degree labels; `ScaleFamily.allCases` titles and members |
| `tunings` | 6 tunings: `displayName`, `shortName`, `openStrings` |
| `spellings` (289) | 17 roots × 17 types: `title`, tones (name, degree), `keySignature`; `letterTriads()`: `numeral`, `formulaNumeral`, `triad.name`, `spokenName`, `notes` |
| `fretboards` (102) | in G, 6 tunings × 17 types at 24 frets: `tonesByFret` per string as `"F♯|7"` or null |
| `keyboards` | `KeyboardLayout` for 1 and 2 octaves, 17 types in G and in F♯: `pitchClass`, `isBlack`, tone |
| `edits` (~360) | each type in C: `legalEdits()` → edit JSON, `shortLabel`, `menuTitle`, `sentence(above:)`, working name, formula, `ScaleTransformation.title`, `ScaleIdentifier.match` name or null, `ChordChangeReport` changes (`kind`, before/after numeral+name, `notesGone`, `notesNew`, `summary`); rejected candidates with `ScaleEditError` case |
| `identifier` | matches for every built-in (Ionian→Major, Aeolian→Natural Minor), every edit result, one custom case |
| `settings` | `PracticeSettings()` as sorted-keys bytes, second JSON with every optional set, sorted key list, `fretCountChoices`, `keyboardOctaveChoices`, `NoteLabelStyle` raw values, the "first release" JSON from `ScaleLibraryTests` |
| `session` (new) | scripted scenarios on a fresh `PracticeSession` + empty library: ring V in G Major → `ringedNotes` {D,F♯,A}; choose D Major → still {D,F♯,A}, chip not selected (documents the quirk); lower 7 in G Major → `currentBoardChange` present, library has one lesson named "G Mixolydian", chips after; choose A Minor Pentatonic → `currentBoardChange` nil; revert → scale G Major, lesson kept; `choose(.custom(unknown))` → unchanged; `toggleRing` twice → nil; settings JSON after each step |
| `library` | one `LibraryDocument` with one `CustomScale` and two lessons (`.lower`, `.add`) as Swift writes it: `{"lower":{"_0":{…}}}`, `{"custom":{"_0":"UUID"}}`, dates since 2001, UUIDs upper case |
| `keyChordTable` | `majorRows()`, `minorRows()`: `keyName`, chords, alternatives at steps 4 and 6 |
| `drums` | 5 grooves: name, `beatsPerBar`, `subdivision`, `stepsPerBar`, hits, `countLabel` per step, voices per step; `DrumClock.barDuration` at 40/90/120/220; `ballPosition` at progress k/64 for n=8 and 16 |
| `drumRender` | 7 fixtures (5 grooves at 90 BPM/8 kHz; Basic Rock at 40 BPM/48 kHz and 220 BPM/44.1 kHz): `frames`, `peakIndex` exact; `sum`, `sumAbs`, `peak` tol 1e-3; `head` (first 32) and `probe` (64 at i·frames/64) tol 1e-6; first 16 raw `NoiseSource` states for seeds 7 and 11, exact |
| `geometry` | fretboard at 1920×1080 and 390×300 × 15/24 frets × handedness: `cellWidth`, `rowHeight`, `noteRadius`, `nutX`, `tuningLabelX`, `fretNumberY`, `inlayY`, `noteX[]`, `fretWireX[]`, `stringY[]`, `stringIndex` both orders, stroke widths, font sizes; keyboard at 1200×400 × 1/2 octaves: every key rect, circle radius and centre; `DrumStaffLayout` at 900×300 for an eighth and a sixteenth groove; TV unit sizes at 1920×1080 and 1280×720; the two chord-table metric sets |

Kotlin side: `core/build.gradle.kts` adds `sourceSets["test"].resources.srcDir("../../expected")`;
`Answers.kt` loads once via `Json.parseToJsonElement`; one reader test per section failing with
the exact case name; one test fails on any top-level key no reader handles. Library round trip
compares parsed JSON trees, not bytes.

Freshness: `make answers` writes `expected/answers.json`; `make answers-check` writes to
`DerivedData/answers.json` and `cmp`s.

### 1.5 `make verify`

```make
PY := python3 -I
answers:        swift run --quiet ExpectedAnswers expected/answers.json
answers-check:  swift run --quiet ExpectedAnswers $(DERIVED)/answers.json && cmp -s … || fail
parity:         $(PY) scripts/check_parity.py
mirror:         $(PY) scripts/check_mirror.py
test-unit:      xcodebuild … -only-testing:ScalesTests
test-ui:        xcodebuild … -only-testing:ScalesUITests
verify:     parity mirror lint lint-android answers-check test-unit mac-test test-android build-android   # ~4 min warm
verify-ui:  test-ui; test-ui SIM="iPad Pro 13-inch (M5)"; test-android-ui                              # ~13 min
verify-all: verify verify-ui
```
Cheapest first. `verify` runs on every agent loop (in the agent's worktree; the shared
`~/.gradle` cache is safe for concurrent builds) and in `.githooks/pre-push` (owner runs
`git config core.hooksPath .githooks` once). `verify-all` runs on the integration branch before
each owner gate; `scripts/deploy_to_store.sh` and `bundle-android` run it first. Linux CI
(`.github/workflows/android.yml`): parity, mirror, `:core:test`, `:app:testDebugUnitTest`, ktlint,
detekt, `assembleDebug`.

### 1.6 `CLAUDE.md` "Adding a feature" recipe (written in wave 0)

1. Register the id in `features.json` (statuses per platform; `later` needs `laterUntil`, `no`
   needs a reason). `make parity` now lists the missing tests.
2. Logic in `Scales/Music|Session|Drums/X.swift` (no UI imports) and the Kotlin twin
   `android/core/.../X.kt`, same names. `make mirror`.
3. If the logic produces something players see, add a section/cases to
   `scripts/ExpectedAnswers/main.swift`, `make answers`, commit `expected/answers.json`, teach
   the matching `Answers…Test.kt`. Write sets sorted.
4. A new setting: optional and nil for its default in `PracticeSettings.swift`; mirror in
   `PracticeSettings.kt` and one read + one write line in `core/session/SettingsStore.kt`;
   `make answers`.
5. iPhone/iPad/Mac: SwiftUI view with accessibility labels; tagged tests in `ScalesTests` and
   `ScalesUITests`. Identifiers shared with Android test tags: `tvMenu`, `boardSettings`,
   `changeBoardNote`, `boardChords`, `revertBoardChange`, `tempoLabel`, `addNote`,
   `editNote-<label>`, `myScale-<name>`, `saveLessonButton`, `scaleNameField`.
6. Android phone/tablet: Compose screen in `android/app/.../app/phone/` (Material 3, never
   `androidx.tv`); tagged test in the screen's own file under `androidTest/.../app/phone/`.
7. Android TV: screen or panel row in `android/app/.../app/tv/` (tv-material, never material3),
   D-pad only; key-press test in the screen's own file under `androidTest/.../app/tv/`. Or
   `androidTv: "no"` with a reason.
8. `make verify`, then `make verify-all`. 9. If looks changed: `make screenshots`,
   `screenshots-ipad`, `screenshots-android`, compare side by side. 10. A line in `CLAUDE.md`.

---

## 2. Toolchain and scaffolding (`android/`)

Verified 18 Nov 2026: AGP 9.5.1, Kotlin 2.4.21, Compose BOM 2026.09.00 (ui/foundation 1.12.1,
material3 1.4.0), tv-material 1.1.0, activity-compose 1.13.0, lifecycle 2.11.0, coroutines
1.11.0, kotlinx-serialization 1.13.0, test runner/rules 1.7.0, ext-junit 1.3.0, Gradle 9.9.1
(sha256 `d8a18a7a841dd5df825d35a685fad6f2d7f12ac0c280e57b67c27b4a520ba228`; fallback 9.8.2
`21bd5da2de49b642904077f38c0efdc7778e92b3ed0889392dca61b97e389607`, then 9.6.0 = AGP minimum),
ktlint-gradle 14.2.0 with ktlint 1.8.0, detekt `dev.detekt` 2.0.0-alpha.6 (Homebrew detekt
1.23.8 cannot parse Kotlin 2.4; Gradle plugins only). SDK at
`/opt/homebrew/share/android-commandlinetools` has platforms 35/36 only, build-tools 36.0.0,
emulator 36.6.11; JDK 17.0.19 at `/opt/homebrew/opt/openjdk@17`; `~/.gradle` has 9.1.0 to
bootstrap the wrapper; device ids `pixel_8`, `pixel_tablet`, `tv_1080p` exist; `timeout` absent
on macOS; `rsvg-convert` and `hb-subset` installed, Pillow and fontTools not.

Order inside wave 0 C, because `compileSdk 37` is not installed yet: (1) `make android-setup`
(installs `platforms;android-37.0`, build-tools 36.0.0, platform-tools, emulator, the three
system images, creates the four AVDs; ~4.2 GB; `wasla_e2e` untouched); (2) generate the wrapper
with the cached Gradle 9.1.0 before any AGP file exists; (3) write the build files; (4) first
build; (5) `./gradlew buildEnvironment | grep kotlin-gradle-plugin` must show `2.2.10 -> 2.4.21`.

Findings that change the Nov 5 plan: AGP 9.5.1 compiles with Kotlin 2.2.10 unless `kotlin-jvm`
2.4.21 is declared `apply false` at the root; AGP 9 defaults `targetSdk` to `compileSdk` (set 36
explicitly); built-in Kotlin (no `org.jetbrains.kotlin.android`), `kotlin { jvmToolchain(17) }`,
`compileSdk { version = release(37) }`, no `kotlinOptions`, no `proguard-rules.pro`; activity
names `.app.ScalesApplication`, `.app.phone.PhoneActivity`, `.app.tv.TvActivity`; AndroidX
merges `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (allowed by `check_apk.sh`); release renames
resource files so the banner check reads the manifest xmltree; iOS icon is one flat PNG, redrawn
as a VectorDrawable adaptive icon (nut `#D3D7E0`, frets `#5C6475`, strings `#7D8596`, dots
`#9FABC2`/`#F5CB60`, background `#171C25`) and an SVG banner `Store/Icon/android-tv-banner.svg`.

**Accidentals font (day one).** `android/ui/src/main/res/font/noto_music_accidentals.ttf`:
Noto Music (SIL OFL 1.1, bundling with GPL software is allowed; the OFL text goes in
`android/ui/src/main/assets/licenses/`) subset with
`hb-subset --unicodes=U+266D,U+266E,U+266F,U+1D12A,U+1D12B --output-file=…` (a few KB).
`ui/MusicFont.kt` exposes `MusicFontFamily = FontFamily(Font(R.font.noto_music_accidentals))`;
Android falls back per glyph to the system font for every other character, so all note labels,
chips and titles use this family and ♭ ♯ ♮ 𝄪 𝄫 render identically on every device. The
download goes into a fresh scratch directory; `GlyphTest` still runs on every emulator and the
stick as a second check.

Files (≈700 lines): `settings.gradle.kts`, root `build.gradle.kts` (plugins `apply false`, one
root detekt task), `gradle.properties` (`-Xmx4g`, parallel, caching, configuration cache),
`gradle/libs.versions.toml`, `version.properties`, `core/build.gradle.kts` (kotlin-jvm,
toolchain 17, test resources `../../expected`, serialization-json), `ui/build.gradle.kts`
(android-library, Compose Foundation only, `api(project(":core"))`, font resource),
`app/build.gradle.kts` (id `com.alnourkhalifa.scales`, minSdk 28, targetSdk 36, compileSdk 37,
version from `version.properties`, signing from `~/.gradle/gradle.properties`
`scalesUpload*` keys with unsigned fallback, R8, lint `abortOnError`), `AndroidManifest.xml`
(leanback and touchscreen not required, banner, `enableOnBackInvokedCallback`, no permissions,
`allowBackup` + `data_extraction_rules`, two activities, TV one landscape), `res/` (strings,
colors, themes light/night/TV `NoActionBar`, adaptive icon, banner), `detekt.yml`
(Cognitive 10, Cyclomatic 10, LongMethod 50, NestedBlockDepth 3, MaxLineLength 100,
ReturnCount 3, `UndocumentedPublicClass`), `.editorconfig` (ktlint_official, 100 cols, chain
rule at 3 calls, Composable naming), `.gitignore` additions, `scripts/android_boot.sh`,
`scripts/check_apk.sh` (native allow-list `libandroidx.graphics.path.so`, both ABIs,
`zipalign -c -P 16 -v 4`, badging expectations, banner), Makefile targets `android-setup
build-android test-android test-android-ui run-android run-android-tv run-fire lint-android
format-android bundle-android screenshots-android`, `.github/workflows/android.yml`.

---

## 3. Module and file map (Kotlin production ≈ 5,400 lines)

Package root `com.alnourkhalifa.scales`.

**`core`** (pure Kotlin JVM, ≈2,200): `music/` mirrors `Scales/Music` file for file (PitchMath 20,
NoteName 70, ScaleDegree 70, ScaleType 95, ScaleDefinition 40, Scale 55, ScaleEdit 115,
ScaleEditMenus 40, ScaleIdentifier 40, ScaleChord 100, Triad 140, Tuning 50, FretboardLayout 35,
KeyboardLayout 45, Instrument 18, KeyChordTable 70, ChordChangeReport 150, TransformationLesson
85, DrumGroove 105, DrumClock 38); `session/`: PracticeSettings 70, **PracticeSession 150**
(see below), **SessionState 25**, ScaleLibrary 150, ScaleLibraryJson 70, **KeyValueStore 20**
(interface `getString/putString/remove/keys` + `InMemoryKeyValueStore`), **SettingsStore 75**
(reads and writes `PracticeSettings` through `KeyValueStore`, one line per key each way; fully
JVM-tested); `board/` (FretboardGeometry 75, KeyboardGeometry 40, DrumStaffLayout 45);
`drums/` (DrumBarRenderer 100: `FloatArray`, `Double` maths, `NoiseSource` on `Int` with
`toUInt().toDouble() / 4_294_967_295.0 * 2 - 1`, voices rendered once per call, `kotlin.math`);
`DeviceKind.kt` 30 (phone, tablet, tv; `fromFeatures(hasLeanback, hasFireTv, smallestWidthDp)`).

**`PracticeSession` and Compose.** `SessionState` is an immutable data class (`settings`,
`boardChange`, `ringedChord`, plus derived `scale`, `ringedNotes`, `currentBoardChange` as
functions). `PracticeSession` holds `var state: SessionState` privately, exposes it read-only,
and calls `onChange(state)` with the new snapshot after every mutation (`choose`, `transform`,
`revertBoardChange`, `toggleRing`, `refreshCustomScale`, `updateSettings`). `ScalesApplication`
holds `val sessionState = mutableStateOf(session.state)` and the listener assigns it, so every
mutation replaces the reference and Compose recomposes. Composables read
`ScalesApplication.sessionState.value` only. `SettingsStore` saves on every `onChange` whose
`settings` differ, mirroring Swift's `didSet`.

**`ui`** (Compose Foundation only, ≈1,030): MusicFont 15, FretboardPalette 60, ChordColors 20,
FretboardView 170 (+`ChordRing`, dp drawing with `fontScale = 1f`, `drawWithCache`,
`rememberTextMeasurer(cacheSize = 64)`), KeyboardView 130, ScaleNotesRow 50, TVDisplayView 80
(`unit = min(h/100, w/178)`, margin parameter, optional bottom slot), ChordTablesView 170,
ScaleChordsRow 170 (chip anatomy, ring = 2 dp orange border + 12% fill, accessibility label
"V, D major: D, F♯, A. Was D major.", `BoardChangeRow`), DrumStaffView 170, DrumIcons 25.

**`app`** (≈2,170): ScalesApplication 35, `SharedPreferencesKeyValueStore` 20 (the only
Android-specific storage code), PhoneActivity 35 (edge-to-edge, `configChanges`; **on create, if
`PackageManager.FEATURE_LEANBACK` or `amazon.hardware.fire_tv` is present it starts
`TvActivity` and finishes**, so a TV launcher that lists `CATEGORY_LAUNCHER` never shows the phone
layout), `drums/DrumPlayer` 90 (plain class, `DrumSound` interface, injectable clock),
`drums/StreamingDrumSound` 120 (see below), `rememberDrumPlayer` 30;
`phone/`: RootView 45, ContentView 180, BoardToolbar 90, ScaleMenus 70, ChangeNoteMenu 55,
BoardSettingsMenu 80, TVMenu 40, PhoneBoard 60, BoardChordGrid 75, FullScreenBoardView 70,
ChordTablesScreen 50, TransformView 200, LessonViews 380, DrumTrainerView 110;
`tv/`: TvActivity 22, TvApp 50, TvBoardScreen 100, TvOptionsPanel 140, TvChoiceList 65,
TvEditNoteLists 70, TvChordsScreen 45, TvDrumsScreen 140, TvPracticeTimer 30.

**Drum sound: one streaming `AudioTrack` per screen.** `StreamingDrumSound` opens one
`AudioTrack` (`MODE_STREAM`, `ENCODING_PCM_16BIT`, mono, native output rate, buffer = 4 × the
minimum) when the drums screen appears and releases it on dispose. A writer coroutine on
`Dispatchers.Default` feeds 20 ms chunks from the current bar array, wrapping at the bar end, so
the loop is gapless and no 576 KB static buffer exists (removes the 1 GB-stick risk). On
`start()`/change: render the new bar on `Dispatchers.Default` (one bar of mixed `Double` synthesis,
a few ms), then `pause(); flush(); swap the bar; play()`, and set `startNanos` when the first
chunk is written, so the ball restarts from beat one as on iOS. Phone slider: the label follows
the drag live, tempo is applied in `onValueChangeFinished` (iOS applies continuously; accepted
divergence that avoids 179 restarts). TV ±1 presses apply at once. Audio focus: iOS uses
`AVAudioSession` category `.playback` with no `.mixWithOthers`, which silences other apps, so
Android requests `AUDIOFOCUS_GAIN` to match; loss → `stop()`. If Alnour wants practice over a
backing track, both platforms change together (iOS `.mixWithOthers`, Android no focus request)
as one registered feature. Any `AudioTrack` failure → silent, ball still runs.

Back order on TV: sublist → list → panel → chips/change button → board → exit, no confirmation.
Grep rule: no `androidx.compose.material3` under `app/tv`, no `androidx.tv` under `app/phone`.

Outside production: Kotlin unit tests ≈750 (answers readers 320 incl. `AnswersSessionTest`,
`SettingsStoreTest` on `InMemoryKeyValueStore`, `ScaleLibraryTests`, `PracticeSessionTest`,
`DrumPlayerTest` with fake sound and clock, `DeviceKindTest`), Compose UI tests ≈550 split per
screen (`phone/PhoneScalesTest`, `PhoneChordsTest`, `PhoneTransformTest`, `PhoneDrumsTest`,
`PhoneSettingsPersistTest` [the one instrumented `SharedPreferencesKeyValueStore` round trip];
`tv/TvBoardTest`, `TvPanelTest`, `TvChordsTest`, `TvDrumsTest`, `TvBackOrderTest`; `GlyphTest`),
Swift additions ≈500, scaffolding ≈700, scripts ≈150.

---

## 4. Storage

- **Settings**: `SettingsStore` (core) over `KeyValueStore`; Android adapter wraps
  `SharedPreferences`. Keys `root.letter`, `root.accidental`, `scaleType`, `tuning`,
  `fretCount`, `labelStyle`, `isLeftHanded`, `thinStringOnTop`, `tvUsesDarkTheme`, `customScale`
  (Swift-shaped JSON, absent for nil), `instrument`, `keyboardOctaves` (absent for defaults).
  JVM `SettingsStoreTest` round-trips both `answers.settings` JSONs and checks one corrupt value
  falls back alone; `PhoneSettingsPersistTest` relaunches the activity and reads the saved scale.
- **Library**: Swift-shaped JSON (`ScaleLibraryJson.kt`) at `filesDir/Scales/ScaleLibrary.json`,
  shared by both activities; unreadable → `ScaleLibrary-unreadable.json`.

---

## 5. Decisions taken and still open

Taken today: targets, full feature set, answers-file-only, `android/` on branch `android` with
worktrees, chips + note change on TV with Transform tab off TV, drums on TV with a control row,
keep-awake timer, Opus subagents in parallel, `scripts/` not `tools/`, mirrored geometry instead
of a shape list, Swift-shaped library JSON, Gradle lint plugins, streaming AudioTrack, bundled
accidentals font, Fire OS routing in PhoneActivity, audio focus matching iOS.

Still open, needed only at wave 5 (store): Play account age, test devices (Fire TV Stick 4K
Plus recommended), Amazon at the same time, minSdk 28, price. None blocks building.

---

## 6. Risks (new since Nov 5)

- Kotlin 2.4.21 under AGP 9.5.1's built-in Kotlin: first-build check; fallback 2.3.x.
- detekt 2.0 alpha: config key rejected or no root task → drop key / register task / ktlint only.
- Streaming writer underruns on a 1 GB stick (audible gap): buffer 4× minimum, chunk 20 ms;
  measure on the stick in wave 2.
- `sin`/`exp` 1-ulp differences: tolerances in `drumRender`.
- Nested `DropdownMenu` levels; `LazyVerticalGrid` inside a height cap (must not sit in a
  vertical scroll parent); TV chip focus after a transform (always re-request the board).
- TV board loses ~14% height to chips: check readability on the stick.
- Unsorted set in the generator breaks `answers-check` forever: rule in recipe.
- Symlinked sources in SwiftPM and `swift run` with `@MainActor` top-level code: wave 0 A check;
  `swiftc` fallback.
- The parity check proves a tagged test exists, not that it is meaningful: owner gates.

---

## 7. Build order: waves of parallel Opus agents in worktrees, with owner gates

Mechanics for every wave: the orchestrator creates stubs and the file ownership list first;
each agent gets `isolation: "worktree"` on a branch `android/w<N>-<letter>` cut from `android`,
owns only the files listed, never edits `features.json`, `RootView.kt`, `TvApp.kt` or another
agent's test file, and commits when `make verify` passes in its worktree (Android targets are
skipped until they exist). The orchestrator then merges the sub-branches into `android`,
resolves nothing but import lists, flips `features.json`, runs `make verify-all`, and commits
"Wave N integration". Each wave ends with an owner gate; `android` merges to `main` after it.

| Wave | Pre-wiring by orchestrator | Parallel agents (Opus, own worktrees, disjoint files) | Done when | Owner gate |
|---|---|---|---|---|
| **0** | Commit pending work on `main`; create branch `android`; copy this plan to `docs/plans/` | **A** Swift: `BoardCaster` protocol, the four geometry/library moves, `Package.swift` + `scripts/ExpectedAnswers/` (symlinks, `main.swift`), `make answers`/`answers-check`, four missing iOS tests, tags on every Swift test. **B** `features.json`, `check_parity.py`, `check_mirror.py`, Makefile `verify`/`verify-ui`/`verify-all`, pre-push hook, CLAUDE.md recipe + Android bullet. **C** `make android-setup`, wrapper, `android/` scaffolding (§2), accidentals font + OFL text, icon, banner, lint, CI, `android_boot.sh`, `check_apk.sh`, step-1 activities showing text, Fire OS routing stub in `PhoneActivity` | iOS `make test` green; `answers-check` passes; `parity` passes with Android `later`; both empty activities open on phone and TV emulators; Kotlin 2.4.21 confirmed in `buildEnvironment`; font renders 𝄪 on the TV emulator | Read a few spellings, one edit report and the `session` section of `expected/answers.json`; approve registry ids and recipe wording; see the empty app on both emulators (30 min) |
| **1** core | Empty package folders; `Answers.kt` loader with the unknown-section test | **A** `music/` base (PitchMath…KeyChordTable, Instrument, KeyboardLayout) + `AnswersSpelling/Fretboard/Keyboard/KeyTableTest`. **B** `music/` edits (ScaleEdit, ScaleEditMenus, ScaleIdentifier, ChordChangeReport, TransformationLesson) + `AnswersEditTest`, `AnswersIdentifierTest`. **C** `session/` (PracticeSettings, SessionState, PracticeSession, KeyValueStore, SettingsStore, ScaleLibrary, ScaleLibraryJson) + `AnswersSettings/Session/LibraryTest`, `SettingsStoreTest`, `ScaleLibraryTests`, `PracticeSessionTest`; `DeviceKind` + test. **D** `board/` + `drums/` (three geometry files, DrumGroove, DrumClock, DrumBarRenderer) + `AnswersGeometryTest`, `AnswersDrumTest`, `DrumTrainerTest` | `make test-android` green; every answers section has a reader; `mirror` passes with `PENDING` emptied | Test counts; any place Kotlin questioned Swift behaviour (15 min) |
| **2** ui | `ScalesApplication` with `sessionState`, `SharedPreferencesKeyValueStore`; both activities wired to show a board composable slot | **A** MusicFont, FretboardPalette, ChordColors, FretboardView, KeyboardView, ScaleNotesRow, TVDisplayView; both activities draw the real saved board; `GlyphTest`. **B** ChordTablesView, ScaleChordsRow. **C** DrumStaffView, DrumIcons, `DrumPlayer`, `StreamingDrumSound`, `rememberDrumPlayer`, `DrumPlayerTest` | Boards render on phone and TV emulators; glyphs present; drums loop gaplessly on the phone emulator | **First real TV check** `make run-fire`: launch time, sharpness, C♯ Lydian, drum loop at 40 and 220 BPM, side by side with the iPhone. Stop here if Compose is too slow on the stick (1 h) |
| **3** phone | `RootView` with four tab slots pointing at empty stub composables (`ScalesTabStub`…), `PhoneActivity` routing final, shared test helpers (`PhoneTestRule`, clear prefs and library) | **A** ContentView, BoardToolbar, ScaleMenus (families + My Scales group fed by a `List<ScaleGroup>` parameter), ChangeNoteMenu, BoardSettingsMenu, TVMenu, PhoneBoard, BoardChordGrid, FullScreenBoardView + `PhoneScalesTest`, `PhoneSettingsPersistTest`. **B** ChordTablesScreen + `PhoneChordsTest`. **C** TransformView, LessonViews + `PhoneTransformTest`. **D** DrumTrainerView + `PhoneDrumsTest` | After integration (stubs → real screens, My Scales group plugged in): phone `test-android-ui` green; all `androidPhone` cells `required` | Phone and tablet emulators, TalkBack, large text; full Transform walkthrough against the iPhone; drums beside the iPhone at 90 and 180 BPM (1.5 h) |
| **4** tv | `TvApp` with routes to stub screens, `TvOptionsPanel` skeleton with the agreed row list and a `TvNavigation` state, shared `TvTestRule` with key helpers, the focus/Back table in a doc comment | **A** TvBoardScreen (chips, change row), TvOptionsPanel rows for board/instrument/labels/theme/timer, TvChoiceList, TvPracticeTimer + `TvBoardTest`, `TvPanelTest`. **B** TvEditNoteLists + its panel rows + `TvEditNoteTest`. **C** TvChordsScreen, TvDrumsScreen + `TvChordsTest`, `TvDrumsTest`. **D** `TvBackOrderTest` and the TV-DP full walk, written against the agreed table and pre-wired stubs; runs green only after integration | `make verify-all` green end to end; every `androidTv` cell `required` or `no` | Real remote on Fire TV (and Google TV if owned): readability, long titles at 720p/1080p, chips focus, note change, drums from the sofa; confirm a Fire stick opens the TV layout from every launcher surface (1 h) |
| **5** store | — | **A** `make screenshots-android` fixed states, accessibility pass. **B** R8 release, signing docs, `check_apk.sh`, `bundle-android` with `parity --release`, store text per Nov 5 §10 | Release APK and AAB pass `check_apk.sh` | Create upload key, upload to Play and Amazon by hand, start the closed test (1.5 h, then 14 days) |

Estimated agent work ≈28 h spread over the waves (wall time far less with 3–4 agents per
wave); owner time ≈6 h plus the real-TV sessions.

---

## 8. Verification (end to end)

- `make verify` on every agent loop in its worktree; `make verify-all` on `android` after each
  integration and before each gate.
- Logic: answers readers per section including `session`; mirrored behaviour tests for what
  answers cannot express (library set-aside, atomic commit, settings fallback per key, player
  restart rules, focus loss stops drums, `DeviceKind` routing).
- UI: per-screen Compose tests mirroring the 9 iOS UI tests with the same tags;
  `PhoneSettingsPersistTest` for real `SharedPreferences`; TV tests keys only, including the
  TV-DP full walk and TV-DB Back order.
- By eye: `make screenshots-android` beside `Store/Screenshots`; real-TV sessions in waves 2 and 4.
- Release: `check_apk.sh` and `parity --release`.

## 9. Unverified until first build

Kotlin 2.4.21 × AGP 9.5.1 × Gradle 9.9.1; tv-material 1.1.0 on Compose 1.12.1; detekt 2.0 alpha
config; SwiftPM symlinked sources and `swift run` of `@MainActor` code; streaming `AudioTrack`
headroom on a 1 GB stick; Compose speed there; `release(37)` mapping; `tv_1080p` with the
Android 9 image; Fire OS launcher behaviour with two launcher categories; Play TV reviewers' view
of the keep-awake timer; privacy policy requirement for a no-data app. All counts and hours are
estimates.
