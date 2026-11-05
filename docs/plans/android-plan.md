# Scales for Android: implementation plan

Written 5 November 2026, building on `docs/plans/shared-strategy.md`. Covers phone, tablet, Android TV, Google TV and Fire OS devices. Nothing has been built or installed. Versions were read from Google's Maven and SDK repositories today, and every library claim below that says "read from the package" means I downloaded the published file and looked inside it.

**The short version.** A three-module Gradle project under `android/` produces one app with two launcher activities, one for touch screens and one for TVs. It uses Kotlin 2.4.21, Android Gradle Plugin 9.5.1, Compose BOM 2026.09.00 and tv-material 1.1.0, with `minSdk 28`, `targetSdk 36` and `compileSdk 37`. The whole app comes to **2,570 lines against the strategy's 2,060**, and 1,712 lines without the Transform feature. Agent build time is about 11.5 hours plus about 6 hours of owner review. Google's 14-day closed test then sets the release date, so the plan starts that clock as soon as phone and TV work.

**This Mac is closer to ready than the strategy says.** An Android SDK and JDK 17 are already here, left by another project. Setup costs about 4.2 GB of downloads and 30 minutes.

## 1. Disagreements with the strategy

1. **"This Mac has no Android SDK or Java runtime" is wrong.** `/opt/homebrew/share/android-commandlinetools` holds 8.4 GB: build-tools 36.0.0, platforms 35 and 36, emulator 36.6.11, NDK r28c and one Android 15 image. `openjdk@17` is installed by Homebrew, but it is keg-only, so `/usr/bin/java` still fails and `which adb` finds nothing. `ANDROID_HOME` and `JAVA_HOME` are unset, which explains the mistaken check. `~/.gradle` holds Gradle 9.1.0, and one unrelated emulator (`wasla_e2e`) exists and must be left alone.
2. **The "APK has no `lib/` folder" check cannot pass with Compose.** `ui-graphics` 1.12.1 has a runtime dependency on `androidx.graphics:graphics-path` 1.0.1, whose package contains `libandroidx.graphics.path.so` for all four architectures. Replace the check with an allow-list: the only native file permitted is that library, `armeabi-v7a` and `arm64-v8a` must both be present, and the bundle passes the 16 KB alignment check. I read the arm64 headers and all its load segments are aligned to `0x4000`. The app still has no native code of its own, and the same APK still runs on 32-bit sticks, so the case against Swift on Android stands.
3. **"Screen stays awake" cannot be a plain requirement on Android TV.** Google's checklist item TV-BY and the Ambient Mode guide say an app may hold the screen on only for user-initiated playback, because the screensaver protects OLED panels from static images. A fretboard is a static image, and the screensaver arrives after 10 minutes without remote presses, which is exactly what a player with a guitar in both hands does. Section 5 proposes a player-started practice timer, and decision 3 asks you to choose. How a reviewer will treat it is unverified. Amazon's test criteria only require keeping the screen on for video apps and say nothing against other apps doing so.
4. **The Android total should be 2,570, about 25% over.** Section 7 gives the justification. The Transform estimates are also stale: `ScaleLibrary.swift` is now 199 lines against the planned 90, `TransformView.swift` landed at 310, and `ScalePickerOptions` (25) and `ChordColors` (13) are new.
5. **Android TV margins need 5% on the sides.** `TVDisplayView` uses 4%. Google asks for 48 dp on a 960 dp wide screen, and TV-OV fails anything cut off. Android's TV screen passes 5% to the shared layout as a parameter, so `design.json` and the Apple baselines stay as they are.

The strategy's three remaining Android unknowns are now answered. `minSdk 28` is safe: Compose 1.12.1 and tv-material 1.1.0 both declare 23 in their packages. The compile level had to be raised: Compose 1.12.1 declares `minCompileSdk=37`. Compose on Fire OS has official sample support from Amazon, while its speed on a 1 GB stick is still unmeasured (section 11).

## 2. Toolchain for this Mac

### Versions

| Item | Version | Evidence |
|---|---|---|
| Android Gradle Plugin | 9.5.1 | Google Maven; API reference. 9.6 is still alpha. |
| Gradle wrapper | 9.8.2 | Current stable. 9.5 needs at least 9.6.0, which is the fallback if 9.8.2 misbehaves. The pairing is untested here. |
| JDK | 17 (Homebrew `openjdk@17`, 17.0.19) | AGP 9.5's minimum and default. |
| Kotlin and Compose compiler plugin | 2.4.21 | Kotlin releases page, 8 October 2026. Pairing with AGP 9.5.1 is unverified until the first build. |
| Compose BOM | 2026.09.00 | Maps to ui, foundation, runtime and ui-test 1.12.1, and material3 1.4.0. |
| tv-material | 1.1.0 | Stable, May 2026. Built against Compose 1.10.3 and pulled up to 1.12.1 here, which is normal but unverified on a device. `tv-foundation` is unneeded because standard lazy lists replaced its lists. |
| activity-compose | 1.13.0 | Google Maven, latest stable. |
| compileSdk | 37 (`platforms;android-37.0`) | Compose 1.12.1 refuses anything lower. AGP 9.5 supports up to 37. |
| targetSdk | 36 | Play has required 36 for new phone apps since 31 August 2026. Compose does not force 37, and 36 keeps Android 17's behaviour changes out of the first release. |
| minSdk | 28 | Fire OS 7 is Android 9. This drops Fire OS 5 and 6 sticks, including the 2018 Fire TV Stick 4K (decision 6). TV-PS needs 31 or lower, so 28 passes. |
| Build tools | 36.0.0 | AGP 9.5's default, already installed. |
| Lint | ktlint 1.8.0, detekt 1.23.8 (Homebrew) | ktlint covers format and line length, detekt covers complexity. detekt's reading of Kotlin 2.4 syntax is unverified. |

### Exact setup

```sh
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
BIN=$ANDROID_HOME/cmdline-tools/latest/bin

$BIN/sdkmanager --install "platforms;android-37.0" "emulator" "platform-tools" \
  "system-images;android-36;google_apis;arm64-v8a" \
  "system-images;android-36;google-tv;arm64-v8a" \
  "system-images;android-28;google_apis;arm64-v8a"
$BIN/sdkmanager --licenses

$BIN/avdmanager create avd -n scales_phone  -k "system-images;android-36;google_apis;arm64-v8a" -d pixel_8
$BIN/avdmanager create avd -n scales_tablet -k "system-images;android-36;google_apis;arm64-v8a" -d pixel_tablet
$BIN/avdmanager create avd -n scales_tv     -k "system-images;android-36;google-tv;arm64-v8a"   -d tv_1080p
$BIN/avdmanager create avd -n scales_floor  -k "system-images;android-28;google_apis;arm64-v8a" -d tv_1080p

brew install ktlint detekt

# No standalone Gradle is needed: the copy cached on this Mac writes the wrapper once.
cd android && ~/.gradle/wrapper/dists/gradle-9.1.0-all/*/gradle-9.1.0/bin/gradle \
  wrapper --gradle-version 9.8.2 --distribution-type bin
```

Device names such as `pixel_8` are unverified until `avdmanager list device` runs. Android Studio is unneeded.

**Why these emulators.** All are arm64, the only kind that runs at speed on the M5 Pro. Google publishes arm64 TV images only for API 31 and up, so nothing here reproduces Fire OS 7 on a TV image. `scales_floor` is a stock Android 9 image in a TV-shaped window: launched directly by `adb`, it runs the TV activity at Fire OS 7's Android version and catches missing APIs, though it has no TV launcher. Apple silicon cannot run 32-bit Android processes as far as I know (unverified), so only a real stick proves the 32-bit path.

**Cost.** Downloads total about 4.2 GB: images 1,873, 995 and 857 MB, emulator 416 MB, platform 67 MB. On disk that is about 12 GB at first and up to about 25 GB as the four emulators fill, against 566 GB free. Image sizes unpack at about 2.1 times, measured from the existing Android 15 image; the existing emulator occupies 6 GB. Agent time is about 30 minutes, nearly all download. A Mac starting from nothing would add 8.4 GB and `brew install --cask android-commandlinetools`.

## 3. Project layout

```
android/
  settings.gradle.kts  build.gradle.kts  gradle.properties  gradlew
  gradle/libs.versions.toml       one place for every version in section 2
  version.properties              versionCode and versionName
  detekt.yml  .editorconfig       line length 100, complexity limits
  core/  pure Kotlin JVM module, no Android imports
    com.alnourkhalifa.scales.core.music     theory
    ...core.board                           geometry and the list of shapes
    ...core.session                         settings, state, later the library
    ...core.generated                       committed output of `make data`
  ui/    Android library, Compose Foundation only
    ...ui                                   board painter, TV layout, notes row, chord table
  app/   the one application module, with Material 3 and tv-material
    ...app                                  application class, settings store
    ...app.phone                            touch screens and PhoneActivity
    ...app.phone.transform                  added last
    ...app.tv                               remote screens and TvActivity
```

**Three modules, as the strategy drew them.** Separate modules for theory, session and board would cost about 15 build lines each and enforce nothing that `core` being pure Kotlin does not already enforce. Packages separate them well enough.

**One app module with two launcher activities.** `PhoneActivity` carries `LAUNCHER` and `TvActivity` carries `LEANBACK_LAUNCHER`. Google recommends a single app for phone and TV, and one package name lets one bundle serve both through a dedicated TV track. Two app modules would add about 40 build lines plus a second manifest and shipping rule. One activity that branches at start would save about 20 lines and would put phone Material and TV Material in one call path, which the strategy warns breaks focus. The two activities cost 42 lines. A lint rule in `make lint-android` greps that `app/phone` never imports `androidx.tv` and `app/tv` never imports `androidx.compose.material3`.

**Shared data files.** Android never reads `data/` at build time: `make data` writes generated Kotlin into `core.generated`, and it is committed, as the strategy says. For `expected/answers.json`, I recommend a Gradle resource folder in `core/build.gradle.kts`:

```kotlin
sourceSets["test"].resources.srcDir("../../expected")
```

That is one line, reads the live file, and works on Linux CI. A symlink adds a second path to the same file, a copy task can go stale, and a git subtree has no purpose inside one repository.

**Application id.** `com.alnourkhalifa.scales`, in lower case, is permanent once published (decision 7).

## 4. Drawing and screens

### The list of shapes on a Compose Canvas

`core.board.boardShapes(layout, options, width, height)` returns a flat `List<BoardShape>`, where `BoardShape` is a sealed type with `Line`, `Dot` and `Label`, each carrying a `BoardColour` name rather than a colour value. `ui.BoardCanvas` paints it, and the painter contains no music:

```kotlin
for (shape in shapes) {
    when (shape) {
        is BoardShape.Line -> drawBoardLine(shape, palette)
        is BoardShape.Dot -> drawBoardDot(shape, palette)
        is BoardShape.Label -> drawBoardLabel(shape, palette, textMeasurer)
    }
}
```

Four details decide whether Android matches Apple:

- **Work in dp and scale at the end.** The Swift code has minimums in points (`max(1, ...)`, `max(3, ...)`). Canvas sizes arrive in pixels, so the painter divides by density, builds shapes in dp, and multiplies when drawing. Otherwise thin lines lose their minimums on a 4K panel and the shapes stop matching `answers.json`.
- **Ignore the system font scale on the board.** SwiftUI's fixed sizes do. Compose `sp` does not, so a large-text setting would burst the circles. The painter measures with `Density(density, fontScale = 1f)`.
- **Keep measured text.** `rememberTextMeasurer(cacheSize = 64)` holds a board's labels, and `Modifier.drawWithCache` rebuilds only when settings or size change, so moving focus in a menu over the board never re-measures.
- **Centre on the text box,** with `includeFontPadding = false`, to match SwiftUI's `.center` anchor. A side-by-side screenshot confirms this.

### How the SwiftUI screens map

| SwiftUI | Android | Notes |
|---|---|---|
| `RootView` | `PhoneApp` | Material `NavigationBar` driven by a list of tabs. A third tab is one entry. |
| `ContentView` | `PhoneScalesScreen`, `PhoneOptions` | Scrolling column. The TV section shrinks to Dark board and Full-screen board; there is no casting status. |
| `PhoneFretboard` | Part of `PhoneScalesScreen` | `horizontalScroll`, 46 dp minimum cell, scrolled to the end for left-handed players. |
| `ScaleNotesRow` | `ui.ScaleNotesRow` | Used by phone and TV. |
| `TVDisplayView` | `ui.TvBoardLayout` | Same fractions of screen height. An optional close action, and a margin parameter. |
| `FullScreenBoardView` | `FullScreenBoard` | Landscape on phones. On tablets, see below. |
| `ChordTablesView` | `ui.ChordTable`, plus `PhoneChordsScreen` and `TvChordsScreen` | Three size sets, as the strategy asked, written out in full. Colours come in as a parameter because each theme has its own primary colour. |
| `CaptureStateReader`, `AirPlayViews`, `Cast/*` | Nothing | No casting in the first release. Google's built-in screen casting still mirrors the full-screen board to a TV, at no code cost. |

**Size classes.** `isWide` is true when the window is at least 600 dp wide and 480 dp tall, read from `LocalConfiguration`, so no extra library is needed. The height test keeps a phone on its side from counting as a tablet. Wide windows get the 420 dp board, 20 dp note chips and the roomy chord sizes, and a tablet sharing its screen falls back to phone sizes, matching the iPad.

**Tablets cannot be forced sideways.** At `targetSdk 36` on Android 16, `setRequestedOrientation` is ignored on screens 600 dp wide or more, the same as iPadOS. `FullScreenBoard` reuses the iPad logic: an upright large screen keeps the 16:9 board and shows the hint to turn the device. On phones it locks landscape, and the opt-out is left unused because it disappears at API 37.

**Back and edge-to-edge.** At target 36 the app cannot opt out of edge-to-edge, so `PhoneActivity` calls `enableEdgeToEdge()` and `Scaffold` handles the insets. `KEYCODE_BACK` is no longer delivered on Android 16 and up, so all back behaviour uses Compose `BackHandler`.

### Phone, upright

```
+-----------------------------+
| G Major                     |
|  (G) (A) (B) (C) (D) (E)(F#)|  notes row
|   1   2   3   4   5   6   7 |
| 1 2 3 4 5 ....        >>>   |  board, scrolls sideways
| E--o---o-(G)-o---o---o--    |
| B--o-(G)-o---o---o---o--    |
| ...                         |
|-----------------------------|
| Scale                       |
|   Root               G    v |
|   Scale          Major    v |
| Display                     |
|  [ Notes | Degrees | Dots ] |
|   Frets         15 frets  v |
|   Tuning        Standard  v |
|   Thin string on top   (on) |
|   Left-handed         (off) |
| TV                          |
|   Dark board           (on) |
|   Full-screen board       > |
|-----------------------------|
|   Scales  Chords  Transform |  third tab added last
+-----------------------------+
```

### Phone, Chords tab

```
+-----------------------------+
| Chords                      |
| [ Major keys | Minor keys ] |
| Key  I  ii iii IV  V  vi vii|
| C    C  Dm Em  F   G  Am B° |
| G    G  Am Bm  C   D  Em F#°|
| ...                         |
| Each chord stacks every ... |
+-----------------------------+
```

### Tablet, sideways

```
+----------------------------------------------------------+
| G Major                                                  |
|        (G) (A) (B) (C) (D) (E) (F#)                      |
| full board, 420 dp tall, every fret visible              |
|----------------------------------------------------------|
| Scale                 | Display                          |
|  options in one column up to 720 dp wide, centred        |
|----------------------------------------------------------|
|              Scales      Chords      Transform           |
+----------------------------------------------------------+
```

### TV, the start screen

```
+--------------------------------------------------------------+
|                                                              |
|   G Major                              Standard tuning       |
|   (G) (A) (B) (C) (D) (E) (F#)                               |
|    1   2   3   4   5   6   7                                 |
|      1  2  3  4  5  6  7  8  9 10 11 12 13 14 15             |
|   E -o--o--G--o--o--o--o--G--o--o--o--o--G--o--o             |
|   B -o--G--o--o--o--o--G--o--o--o--o--G--o--o--o             |
|   ... six strings filling the screen ...                     |
|              .     .     .     .        :                    |
|                                         Press OK for options |  fades after 5 s
+--------------------------------------------------------------+
  Outer 5% holds background colour only. OK opens the panel. Back leaves the app.
```

### TV, options panel over the board

```
+------------------------------------+-------------------------+
|   G Major                          | Root            G       |
|   (G) (A) (B) (C) (D) (E) (F#)     | Scale           Major   |  <- focused row
|                                    | Tuning          Standard|
|   board stays visible and          | Frets           15      |
|   redraws as choices are made      | Labels          Notes   |
|                                    | Left-handed     off     |
|                                    | Thin string on top  on  |
|                                    | Dark board      on      |
|                                    | Keep board on   60 min  |
|                                    | Chord tables          > |
+------------------------------------+-------------------------+
  OK on Root, Scale, Tuning, Frets or Labels swaps the panel for a list of choices,
  with focus on the current value. OK applies and returns. Back closes one level.
```

### TV, chord tables

```
+--------------------------------------------------------------+
|   [ Major keys ]   Minor keys                                |  left and right switch
|   Key    I     ii    iii    IV     V     vi    vii°          |
|   C      C     Dm    Em     F      G     Am    B°            |
|   G      G     Am    Bm     C      D     Em    F#°           |
|   ... all twelve rows fit, nothing scrolls ...               |
|   Each chord stacks every other note of the key's scale.     |
+--------------------------------------------------------------+
```

The TV size set uses fractions of screen height (rows about 6.4% tall) so all twelve keys fit without scrolling. That matters because the table has nothing focusable, and Compose has no documented way to scroll focus-free content with a D-pad. Whether minor-key stand-in chords read well from 10 feet at that size is unverified, and the review after step 6 decides.

## 5. TV specifics

**Manifest, shared by Android TV, Google TV and Fire OS:**

```xml
<uses-feature android:name="android.software.leanback" android:required="false" />
<uses-feature android:name="android.hardware.touchscreen" android:required="false" />

<application android:banner="@drawable/banner" ...>
  <activity android:name=".phone.PhoneActivity" android:exported="true"> LAUNCHER </activity>
  <activity android:name=".tv.TvActivity" android:exported="true"
            android:screenOrientation="landscape"> LEANBACK_LAUNCHER </activity>
</application>
```

- `required="false"` on leanback keeps the app installable on phones, and touchscreen `false` is what makes it a TV app on both stores. Amazon classifies Fire TV support from the touchscreen entry and needs the devices ticked by hand.
- The banner is 320×180 px in `drawable-xhdpi` and must contain the app's name (TV-BN). The launcher icon must be at least 160×160 at xhdpi (TV-LB).
- The app requests **no permissions**, so no permission can imply a hardware requirement. Fire OS needs no extra flag that I could find; `amazon.hardware.fire_tv` is for checking the device at runtime, and the app has no reason to.

**D-pad focus.**

- The board screen is one focusable box that takes focus at launch through a `FocusRequester`. OK opens the panel, so Amazon's "Select with nothing highlighted" test has a logical result.
- Every `LazyColumn` uses `Modifier.focusRestorer`. Plain `Row` and `Column` groups also need `focusGroup`. On first opening, focus goes to the current value; on returning, to the row just left.
- The panel traps left and right so focus cannot fall onto the board underneath.
- Controls come from tv-material (`ListItem`, `Switch`, `Tab`) for their focus styling.
- Back goes panel list, panel, board, then out of the app, with no exit confirmation and no menu loop (TV-DB). Nothing depends on a Menu button (TV-DM).

**Overscan.** `TvBoardLayout` gets 5% on the sides and top and bottom, which is Google's 48 dp and 27 dp and Amazon's 90% fill target. The background colour runs to the screen edge, as Google asks.

**Keeping the board on, if you accept decision 3.** A row in the panel, Keep board on, offers Off, 30, 60 and 90 minutes. Choosing a time sets `FLAG_KEEP_SCREEN_ON` for that activity, a coroutine clears it when the time ends, and the choice is never saved, so each session is one the player started. `TvPracticeTimer` is 30 lines. Without it, the Android TV screensaver arrives after 10 minutes, and after 30 more the player lands on the Home screen.

**What commonly fails review, and how this app answers it.**

| Item | Risk here | Answer |
|---|---|---|
| TV-MT, TV-ML: touchscreen and launcher entries | App is not treated as a TV app at all | Manifest above, asserted by a check on the built manifest |
| TV-LB, TV-BN: banner | Missing the name, or a banner with no text | Generated by `make_icons.py` with the name in it |
| TV-DP: everything reachable by D-pad | Dead ends in lists | Full menu walk in the TV UI test using only key presses |
| TV-DB: Back | Back reopening the panel | Asserted in the same test |
| TV-OV: text cut off | Long scale titles at 8% height | "Phrygian Dominant (Hijaz)" and "Half Step Down" tested at 720p and 1080p |
| TV-BY: ambient mode | Static board | Decision 3 |
| TV-G1, TV-G6: bundle, 32 and 64 bit, 16 KB | Wrong artefact | `make bundle-android` and the native file check |
| TV-G4 and listing | No TV screenshot, or "Android TV" missing from the description | Section 10 checklist |

## 6. Settings

**Use SharedPreferences, read and written key by key.** It costs about 55 lines in `SettingsStore`, against about 75 for Preferences DataStore. Three further reasons decide it. Reads are synchronous, so the TV never paints the default board for a frame before the saved one. Each key has its own default, which gives Android the strategy's forgiving decoder for nothing: a missing or unknown value falls back alone. DataStore 1.3.0 would add a second native library (`libdatastore_shared_counter.so`, read from the package) and a coroutine stream for nine small values.

`PracticeSettings` stays a pure data class in `core`, and a test checks its keys and defaults against `answers.json`. `SettingsStore` writes in one place when settings change. Current state lives in `ScalesApplication` in a Compose `mutableStateOf`, the Android equivalent of `PracticeSession.shared`, which also survives rotation without a ViewModel library. Android's Auto Backup restores the file on a new device.

**No sync with the iPhone in the first release.** The strategy does not ask for it, Android and Apple would need a shared account or server, and a TV has no scales of its own to carry until Transform reaches TVs.

## 7. File budget

Counts include blank lines and comments, as the strategy's do. Generated files, Gradle, XML and tests are listed apart. "Later" means added in the Transform steps.

### `core` (1,179)

| File | Lines | File | Lines |
|---|---:|---|---:|
| `music/PitchMath.kt` | 18 | `board/BoardGeometry.kt` | 65 |
| `music/NoteName.kt` | 58 | `board/BoardShapes.kt` | 105 |
| `music/ScaleDegree.kt` | 65 | `session/PracticeSettings.kt` | 50 |
| `music/ScaleDefinition.kt` | 34 | `session/PracticeState.kt` | 22 |
| `music/CustomScale.kt` | 16 | Later: `music/ScaleChord.kt` | 75 |
| `music/Scale.kt` | 50 | Later: `music/ScaleEdit.kt` | 105 |
| `music/FretboardLayout.kt` | 28 | Later: `music/ScaleIdentifier.kt` | 30 |
| `music/Triad.kt` | 80 | Later: `music/TransformationLesson.kt` | 48 |
| `music/KeyChordTable.kt` | 60 | Later: `music/ChordChangeReport.kt` | 140 |
| | | Later: `session/ScaleLibrary.kt` | 130 |

### `ui` (335)

| File | Lines | File | Lines |
|---|---:|---|---:|
| `BoardCanvas.kt` | 55 | `TvBoardLayout.kt` | 50 |
| `BoardColors.kt` | 15 | `ChordTable.kt` | 175 |
| `ScaleNotesRow.kt` | 40 | | |

### `app` (1,056)

| File | Lines | File | Lines |
|---|---:|---|---:|
| `ScalesApplication.kt` | 14 | `tv/TvActivity.kt` | 22 |
| `SettingsStore.kt` | 55 | `tv/TvApp.kt` | 35 |
| `phone/PhoneActivity.kt` | 20 | `tv/TvBoardScreen.kt` | 50 |
| `phone/PhoneApp.kt` | 35 | `tv/TvOptionsPanel.kt` | 90 |
| `phone/PhoneScalesScreen.kt` | 105 | `tv/TvChoiceList.kt` | 45 |
| `phone/PhoneOptions.kt` | 100 | `tv/TvChordsScreen.kt` | 45 |
| `phone/PhoneChordsScreen.kt` | 40 | `tv/TvPracticeTimer.kt` | 30 |
| `phone/FullScreenBoard.kt` | 40 | Later: `LibraryStore.kt` | 30 |
| Later: `phone/transform/TransformScreen.kt` | 200 | Later: `phone/transform/TransformParts.kt` | 100 |

**Total: 2,570. Without the Transform feature: 1,712.**

### Against the strategy

| Module | Strategy | This plan | Why it differs |
|---|---:|---:|---|
| Theory | 700 | 807 | See below |
| Board geometry and drawing | 165 | 225 | The painter is 55 lines on its own, and Compose has no one-line text draw |
| Palette | 12 | 15 | |
| Settings and session | 70 | 127 | The store was uncounted |
| Scale library | 60 | 160 | The Swift file is 199 lines, more than twice the plan |
| TV layout and notes row | 80 | 90 | |
| Chord tables | 170 | 175 | Three written-out size sets |
| Practice options | 180 | 235 | Compose has no `Form` or `Picker`, so menus and rows are built |
| Phone screen | 130 | 145 | |
| Transform screens | 300 | 300 | Tight, since Swift is already at 310 and still changing |
| Root and full-screen | 30 | 75 | Swift is 95 and the logic is the same |
| App shell | 40 | 56 | Two activities |
| TV-only screens | 120 | 160 | Includes the 30-line timer |
| **Total** | **2,060** | **2,570** | **+510** |

**Why theory is 807.** I measured the Swift files: the theory that gets ported holds 808 lines of code with blanks and comments removed, and the planned cuts remove about 60. Kotlin data classes save about 40 more, giving about 705 lines of bare code. The strategy's 700 therefore had no room for a single blank line or docstring. About 100 lines of those are what your style asks for.

**Where I refused to cut.**

- **Theory files keep blank lines and a short "why" docstring,** even with the longer reasoning left in Swift.
- **`ChordChangeReport` stays at 140.** Its sentences are pinned word for word by `answers.json`, and plain string building is the readable form.
- **`BoardGeometry` keeps every named property,** matching the strategy's rule.
- **`ChordTable` keeps three explicit size sets.**
- **`SettingsStore` keeps one line per setting in each direction,** with no reflection and no loop over keys.
- **TV menus stay in two files.** Merging the panel and the list would save about 10 lines and mix two focus behaviours.

### Outside the budget

| Item | Lines |
|---|---:|
| Gradle files and version catalogue | about 170 |
| Manifest and XML resources | about 70 |
| Makefile additions and `tools/android_boot.sh` | about 70 |
| `tools/check_apk.sh` (native allow-list, manifest entries, 16 KB) | about 35 |
| Kotlin output in `generate.py` | within the strategy's 140 |
| Unit tests | about 150 |
| UI tests | about 140 |

Tests come to 290 against the strategy's 150, because the strategy's figure did not include Compose UI tests.

## 8. Testing

**Answers file.** `core/src/test/.../AnswersTest.kt` reads `answers.json` through `kotlinx-serialization-json` 1.12.0's `parseToJsonElement`, which needs no compiler plugin. It walks every section and names the exact case on failure: 289 spellings with chords, all scales across all tunings, default settings and keys, and the shapes of six boards, with doubles compared to a tolerance of 0.001. Edit sections switch on in the Transform step. This runs on the JVM with no emulator, so Linux CI can run it.

**Compose UI tests**, in `app/src/androidTest`, following `ScalesUITests.swift`:

- `PhoneFlowTest`: choose A and Minor Pentatonic and see the title change; open and close the full-screen board; relaunch and see settings kept.
- `TvFlowTest`: key presses only. Launch has focus; OK opens the panel; every row and every list is reachable; Back climbs out in the right order; the chord tables open and switch.
- `SettingsStoreTest`: round trip, plus a corrupted value falling back alone.
- `GlyphTest`: `Paint().hasGlyph` on ♭, ♯, 𝄪 and 𝄫, run on every emulator and on the real stick.

Each test carries a feature tag that `check_parity.py` reads, written as a `// feature: <id>` comment to match however the Swift tags land.

**Screenshot tests: not in the first release.** Google's tool is at `0.0.1-alpha16`, Paparazzi has no stable release for AGP 9, and Roborazzi (1.77.0) is unconfirmed on Compose 1.12. The shape list already checks geometry against Apple. `make screenshots-android` captures fixed states with `adb exec-out screencap -p` for store images and side-by-side review, and the painter is small enough for a person to judge. Revisit once Roborazzi states Compose 1.12 support.

**Makefile additions:**

```make
ANDROID_HOME ?= /opt/homebrew/share/android-commandlinetools
JAVA_HOME    ?= /opt/homebrew/opt/openjdk@17
export ANDROID_HOME JAVA_HOME
GRADLE     := cd android && ./gradlew --console=plain
ADB        := $(ANDROID_HOME)/platform-tools/adb
ANDROID_ID := com.alnourkhalifa.scales

build-android:
	$(GRADLE) assembleDebug

test-android:
	$(GRADLE) :core:test

test-android-ui:
	tools/android_boot.sh scales_phone 5554
	tools/android_boot.sh scales_tv 5556
	$(GRADLE) connectedDebugAndroidTest

run-android: build-android
	tools/android_boot.sh scales_phone 5554
	$(ADB) -s emulator-5554 install -r android/app/build/outputs/apk/debug/app-debug.apk
	$(ADB) -s emulator-5554 shell am start -n $(ANDROID_ID)/.phone.PhoneActivity

run-android-tv: build-android
	tools/android_boot.sh scales_tv 5556
	$(ADB) -s emulator-5556 install -r android/app/build/outputs/apk/debug/app-debug.apk
	$(ADB) -s emulator-5556 shell am start -n $(ANDROID_ID)/.tv.TvActivity

# make run-fire FIRE=192.168.1.40   (the stick needs ADB debugging on)
run-fire: build-android
	$(ADB) connect $(FIRE):5555
	$(ADB) -s $(FIRE):5555 install -r android/app/build/outputs/apk/debug/app-debug.apk
	$(ADB) -s $(FIRE):5555 shell am start -n $(ANDROID_ID)/.tv.TvActivity

lint-android:
	ktlint "android/**/src/**/*.kt" "!android/core/**/generated/**"
	detekt --input android --config android/detekt.yml --excludes "**/generated/**"
	! grep -rl "androidx.tv" android/app/src/main/kotlin/com/alnourkhalifa/scales/app/phone
	! grep -rl "androidx.compose.material3" android/app/src/main/kotlin/com/alnourkhalifa/scales/app/tv

bundle-android:
	$(GRADLE) bundleRelease assembleRelease
	tools/check_apk.sh
```

`tools/android_boot.sh <avd> <port>` starts the emulator with `-no-boot-anim -no-snapshot-save` if it is not running, then polls `sys.boot_completed`. The strategy's `make verify` gains `test-android`, `lint-android` and `check_apk.sh`. UI tests stay out of it because they take minutes, and they run before each owner review.

## 9. Build order

**Before step 1:** the strategy's refactor through its step 14, so `expected/answers.json` and `data/` exist, and scales has its own repository. Agents do not touch `android/` until then. Line totals count only the budgeted Kotlin. Every step ends with `make build-android`, `make test-android`, the lint targets and a commit.

| Step | Work | New lines | Total | Agent hours | Owner reviews |
|---:|---|---:|---:|---:|---|
| 1 | Toolchain, wrapper, three modules, Makefile, boot script, manifest, two empty activities, `.gitignore` and `CLAUDE.md` updates | 56 | 56 | 1.0 | An empty app opens on the phone and TV emulators (10 min) |
| 2 | Kotlin output from `generate.py`, base theory, `AnswersTest` for spellings, chords and tunings | 409 | 465 | 1.0 | Test count and any place Kotlin needed a change in Swift (10 min) |
| 3 | Settings, state, shapes, painter, colours, TV layout, notes row. Both activities show the real default board. `GlyphTest`. | 402 | 867 | 1.5 | **The first check on a real TV.** `make run-fire`: launch time, board sharpness, C♯ Lydian's double sharp. Compare against iPhone. This is the moment to stop if Compose is too slow on a stick (1 hr) |
| 4 | `SettingsStore`, then the phone and tablet Scales screen, options and full-screen board, with `PhoneFlowTest` | 335 | 1,202 | 1.5 | Use it on a phone and a tablet emulator, with TalkBack and large text (45 min) |
| 5 | Chord tables and the Chords tab | 215 | 1,417 | 0.75 | Compare the tables with iPhone (15 min) |
| 6 | TV app: routing, board screen, panel, lists, chords, timer, `TvFlowTest` | 295 | 1,712 | 2.0 | With a real remote on Fire and on Google TV if you have one. Readability from the sofa. Long titles (1 hr) |
| 7 | Icons, banners, R8 shrinking, signing setup, `check_apk.sh`, screenshots, accessibility labels, store text | 0 | 1,712 | 1.0 | Create the key, upload to Play and Amazon, **start the closed test** (1.5 hrs, then 14 days) |
| 8 | Transform logic and the library, with every `answers.json` edit section switched on | 528 | 2,240 | 1.25 | Test count and report wording spot checks (10 min) |
| 9 | Transform tab, My Scales in the pickers, library storage | 330 | 2,570 | 1.5 | Full Transform walkthrough against iPhone. Ship as an update inside the closed test (45 min) |

**Total: about 11.5 agent hours and about 6 owner hours,** plus the 14-day test and up to 7 days of Google's review. These are estimates: no Kotlin has been built in this project. If the Swift logic is still changing when step 8 starts, the counts need taking again.

### Adding Transform without restructuring

Steps 1 to 7 are written so that steps 8 and 9 only add files:

- `PracticeSettings.customScale` and `CustomScale` exist in Kotlin from steps 2 and 3, nullable and unused. `PracticeState` always builds a `Scale` from a `ScaleDefinition`, as Swift does.
- Scale pickers on phone take a list of groups, so My Scales is one more group, and TV pickers ignore custom scales.
- Phone tabs are a list.
- `SettingsStore` adds one key holding the custom scale as JSON. `kotlinx-serialization` and its compiler plugin arrive with the library in step 8, and until then `core` has no dependency beyond Kotlin.
- `features.json` marks Transform as "later" for Android until step 9, so `check_parity.py` passes in the meantime and fails if someone forgets. This needs a third state in that script (about 3 lines).
- TV code never touches any of it, keeping decision 2 of the strategy free to change.

## 10. Stores

**Agents never upload.** The rule for `deploy_to_store.sh` extends here: `make bundle-android` builds, and you upload by hand.

### Signing

You create one upload key: `keytool -genkeypair -keystore ~/.keystores/scales-upload.jks -keyalg RSA -keysize 4096 -validity 10000 -alias upload`. It lives outside the repository, backed up in your password manager, with its passwords in `~/.gradle/gradle.properties`. New Play apps use Play App Signing, where Google holds the final key. Amazon strips your signature and applies its own, so a Play and an Amazon install cannot update each other, which is harmless because Fire devices have no Play Store. `android/version.properties` holds `versionCode`, increased by a small script before each release.

### Google Play

Verified against Google's help pages today:

- **Closed test.** Personal accounts created after 13 November 2023 need at least **12 testers opted in for 14 continuous days** before applying for production. Accounts from before that date and organisation accounts are exempt. Dropping out and rejoining restarts that tester's 14 days, and third-party guides disagree on what happens if the count falls below 12, so recruit 15 or more. Builds can be updated during the test. Google asks written questions about the test, and review usually takes 7 days or less.
- **TV form factor.** Upload one bundle with TV support, add the assets below, mention "Android TV" in the description, then Setup › Advanced Settings › Form Factors › Add Release Type › Android TV. Google reviews it separately against the TV quality list. Whether the 14-day rule also binds the TV track is unverified, and I expect it is settled once the app has production access.
- **Also needed:** bundle format (TV-G1), data safety form (declare no data collected), content rating questionnaire, target audience, privacy policy URL (unverified for an app with no data, so plan on one), and developer verification. The Android developer verification rollout is automatic for verified Play accounts.

| Play asset | Size |
|---|---|
| App icon | 512×512, 32-bit PNG, up to 1 MB |
| Feature graphic | 1024×500, JPEG or 24-bit PNG, no alpha |
| Phone screenshots | 2 to 8; at least 4 at 1080 px or more recommended; long side at most twice the short side |
| Tablet screenshots | 4 or more recommended, 1080 to 7680 px |
| TV screenshots | At least 1; plan on 4 at 1920×1080 |
| TV banner (listing) | 1280×720, no alpha |
| TV banner (inside the app) | 320×180, xhdpi, with the name |

### Amazon Appstore

Upload the signed release APK from `make bundle-android`. Amazon accepts bundles too, but the APK is the same file you tested on the stick, and its 2021 notes on bundles and own-signature apps may be dated. Check Fire TV devices by hand and leave Fire tablets unticked until tested. Vega devices cannot take an APK, so the console marks them unsupported. `minSdk 28` removes Fire OS 5 and 6. Amazon's minimum target for Fire OS 8 is 30, which 36 meets, and its filter page describes target level as a submission rule, with no device filtering by it.

Listing assets: 114×114 and 512×512 PNG icons with transparency, a 1280×720 Fire TV icon without transparency, 3 to 10 screenshots at 1920×1080, and a 1920×1080 background image. The listing says plainly: *Works on Fire TV devices running Fire OS, including Fire TV Stick 4K Max and 4K Plus. Not available on Fire TV Stick 4K Select, 4K (3rd Gen) or HD (2026).* There is no closed test requirement that I could find.

## 11. Risks

| Risk | Mitigation |
|---|---|
| **Compose is slow on a 1 GB, 32-bit stick.** No benchmarks found, though Amazon publishes a Compose sample. | Measured in step 3 with 1,400 lines still unwritten. R8 and the baseline profiles that ship with the libraries help. If it fails, the TV screens could move to a plain Android `View` canvas, reusing `core`. |
| **New sticks are the wrong platform.** Buying a current cheap stick gets Vega, where nothing runs. | Decision 2 names the model. |
| **Double sharp and double flat glyphs are missing** on Fire OS or Android TV. | `GlyphTest` answers it in step 3. The fix is a subset of Noto Music holding those two glyphs plus a helper that puts them in their own text span, about 25 lines and an open licence entry. |
| **Keep-awake is rejected** by TV review. | The timer is one file and one row, so remove it and resubmit. Google warns that a rejected form factor can hold up listing updates, which makes a quick fix matter. |
| **Fewer than 12 lasting testers.** | Recruit 15 or more before step 7. Paid tester services exist, and I would not rely on them for a first application. |
| **Kotlin and AGP pairing fails on first build.** | Found in hour one. AGP's own Kotlin or Gradle 9.6.0 are the fallbacks. |
| **tv-material 1.1.0 misbehaves on Compose 1.12.** | The TV screens use few components. A focus bug would show in `TvFlowTest` and on the real remote. Compose 1.10 would mean an older BOM and `compileSdk`. |
| **Compose raises its compile level again,** as it says it always will. | Only `compileSdk` moves. Target stays under your control until Play's yearly rule. Next August's requirement is likely 37 (unverified), and the app has no opt-outs to lose. |
| **Phone and TV Material mix by accident.** | The grep in `make lint-android`. |
| **Library native files change.** | `check_apk.sh` fails on anything off the allow-list, and a person then checks its architectures and alignment. |
| **Swift theory changes during the port.** | `answers.json` regenerates and the Kotlin test names the failing case, which is the design working. |
| **Text differs from iOS.** Android has no system rounded font. | Accepted by the parity rule. Reviewed in step 3. |

## 12. Decisions for the owner

1. **Which Play account?** Do you have one, and when was it created? If it is personal and from after 13 November 2023, or new, the 14-day test applies. I recommend a personal account and 15 testers lined up before step 7, since an organisation account needs a D-U-N-S number and a legal entity.
2. **Which test devices?** I recommend buying a **Fire TV Stick 4K Plus**, the one current stick still on Fire OS (Fire OS 8), before step 3. A used Fire OS 7 stick adds the true floor device, and is worth buying only if the 4K Plus feels slow. Tell me if you own a Google TV device or an Android phone; otherwise emulators cover them, and a real phone may be needed to verify a new Play account.
3. **How should the board stay on with Android TV's screensaver?** I recommend the Keep board on timer: 30, 60 or 90 minutes, started by the player, never saved. It is the honest reading of "user-initiated", it protects OLED sets when the player forgets, and it costs 30 lines. The alternatives are always on, which risks rejection, and following the rules strictly, which breaks the app's main use every 10 minutes.
4. **Does the first release wait for Transform?** I recommend starting the closed test after step 7 without it and adding it as an update during the 14 days, so production launches with parity and the clock starts about 3 agent hours sooner.
5. **Amazon at the same time?** I recommend yes, at step 7. No test period applies, so Fire OS users may get the app weeks before Play users, and it gives real TV feedback.
6. **Keep `minSdk 28`?** I recommend yes for now. Lowering it to 25 would bring in the popular 2018 Fire TV Stick 4K, and Compose allows that, but we would need that stick to test it and the 1.5 GB of RAM is unproven. It is a one-number change later.
7. **Application id:** `com.alnourkhalifa.scales`. It can never change after publishing. I recommend it.
8. **Accept 2,570 lines?** I recommend yes. Reaching 2,060 would mean stripping blank lines and docstrings from theory and merging screens, which breaks your style rules.
9. **Price on Android?** Match the App Store, and if it is paid, Play needs a merchant account set up before release.

## Unverified

- Nothing has been built or run. Kotlin 2.4.21 with AGP 9.5.1, Gradle 9.8.2 with AGP 9.5.1, tv-material on Compose 1.12.1, and detekt on Kotlin 2.4 syntax are all first-build checks.
- Compose speed on Fire OS, and the glyphs on Fire OS and Android TV.
- Whether Play TV reviewers accept a player-started keep-awake timer.
- Whether the TV track needs its own closed test, and whether a no-data app needs a privacy policy today.
- The TV chord table's legibility, and AVD device names.
- That Apple silicon cannot run 32-bit Android code in the emulator.
- Agent hours are estimates, and every Kotlin line count is one too. Theory numbers rest on measured Swift; screen numbers rest on judgement.

## Sources

- **Build:** [AGP 9.5 notes](https://developer.android.com/build/releases/agp-9-5-0-release-notes), [AGP API reference](https://developer.android.com/reference/tools/gradle-api), [Compose BOM](https://developer.android.com/develop/ui/compose/bom/bom-mapping), [Compose for TV releases](https://developer.android.com/jetpack/androidx/releases/tv), [Kotlin releases](https://kotlinlang.org/docs/releases.html), [Compose August '26 release](https://developer.android.com/blog/posts/what-s-new-in-the-jetpack-compose-august-26-release), [Google Maven](https://dl.google.com/android/maven2/), [SDK repository](https://dl.google.com/android/repository/repository2-3.xml), [Gradle versions](https://services.gradle.org/versions/current)
- **Android behaviour:** [Android 16 behaviour changes](https://developer.android.com/about/versions/16/behavior-changes-16), [target API rules](https://developer.android.com/google/play/requirements/target-sdk), [16 KB page sizes](https://developer.android.com/guide/practices/page-sizes), [focus restoration](https://developer.android.com/develop/ui/compose/touch-input/focus/focus-restoration)
- **TV:** [TV app quality](https://developer.android.com/docs/quality-guidelines/tv-app-quality), [create a TV app](https://developer.android.com/training/tv/get-started/create), [TV navigation](https://developer.android.com/training/tv/get-started/navigation), [TV layouts](https://developer.android.com/training/tv/playback/compose/layouts), [Ambient Mode](https://developer.android.com/training/tv/playback/ambient-mode), [TV publishing checklist](https://developer.android.com/training/tv/publishing/checklist), [distribute to Android TV](https://developer.android.com/training/tv/publishing/distribute)
- **Stores:** [Play testing requirement](https://support.google.com/googleplay/android-developer/answer/14151465), [Play preview assets](https://support.google.com/googleplay/android-developer/answer/9866151), [developer verification](https://android-developers.googleblog.com/2026/03/android-developer-verification-rolling-out-to-all-developers.html), [Amazon device filtering](https://developer.amazon.com/docs/app-submission/device-filtering-and-compatibility.html), [Fire TV device table](https://developer.amazon.com/docs/device-specs/device-specifications-fire-tv-streaming-media-player.html), [Amazon upload](https://developer.amazon.com/docs/app-submission/upload-app-file.html), [Amazon test criteria](https://developer.amazon.com/docs/app-testing/test-criteria.html), [Fire TV images](https://community.amazondeveloper.com/t/what-images-and-videos-are-needed-for-fire-tv-apps/3749), [Amazon app bundles](https://developer.amazon.com/apps-and-games/blogs/2021/07/appstore-support-for-android-app-bundle)
- **Testing:** [Compose Preview Screenshot Testing notes](https://developer.android.com/studio/preview/compose-screenshot-testing-release-notes)
