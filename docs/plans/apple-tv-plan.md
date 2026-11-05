# Scales on Apple TV: implementation plan

Written 5 November 2026. Builds on `shared-strategy.md` and starts after that refactor finishes.

**The short version.** A working Apple TV prototype now exists in a scratch copy, and it ran on the tvOS 27.0 simulator driven by simulated remote presses. The board fills the screen sharply at 4K, left/right changes the root, up/down changes the scale, a click opens options beside a live board, and Menu backs out one level at a time. The plan adds 291 lines of app code and brings the Apple TV total to about 1,944, inside the strategy's 1,964 ceiling. That is 91 more new lines than the strategy's 200, paid for by compiling 140 fewer shared lines. Expect about 4 hours of agent work plus about 2 hours of the owner's review. Nothing has run on a real Apple TV.

## What was actually tested

The tvOS 27.0 simulator runtime was missing, so I installed it (3.5 GB; `xcrun simctl runtime delete 870D81A7-2F41-489D-9E0C-E243FEB9B832` removes it). All code below ran in scratch copies. Every result here is from this Mac, with Xcode 27.1 and tvOS SDK 27.0.

| Check | Result |
|---|---|
| App launches and draws on 1080p and 4K Apple TV simulators | Passed. The window is 1920×1080 points; the 4K simulator's screenshots are 3840×2160 |
| Board takes focus at launch; left, right, up, down, play/pause change it | Passed |
| Options panel takes focus, walks every row, closes back to the board | Passed, after two fixes below |
| Menu on the board goes to the Home screen | Passed |
| Settings survive quitting and relaunching | Passed: each run opened on the previous run's scale |
| Chord tables reachable, Major/Minor switch works, focus scrolls the table | Passed, after fixes below. The test went down to the eighth of 12 keys |
| Light board theme, including options and chord tables | Passed |
| 39 existing theory tests plus 3 new ones, inside a tvOS unit test bundle | 42 of 42 passed, using `@testable import Scales` unchanged |
| Proposed `project.yml` and `make build-tv` in the strategy's `Core/` and `SharedViews/` layout, with real team signing settings | Passed: 6 s build. `xcodebuild test` with the same destination as `make test-tv` passed in 36 s |
| Release archive for a real Apple TV (unsigned, not uploaded) | Passed: 352 KB app, 6 s |
| Placeholder icon and top-shelf catalog through Apple's asset compiler | Compiled with no warnings; a wrong-sized back layer is an error |
| `iOS` target in the same project | Not rebuilt in the new layout, because the scratch copy removed casting from the session. The refactor's own tests cover it |

Four problems appeared only by running, which a compile could never show:

1. **`.focusable(false)` and `.disabled(true)` leave focus on the board.** The remote kept changing the scale behind the open panel, and Menu quit to Home. Removing the focus modifiers from the view while the panel is open fixes it.
2. **A Picker's pushed page is empty on tvOS 27.** Even a bare `Form` with a two-string `Picker` shows a black page, and the accessibility tree reports "Empty list". The `.menu` style works but drops the row labels. `.inline` works but would list 17 roots in the panel. A small hand-written row replaces it.
3. **`onExitCommand` around the navigation stack swallows Back.** Menu on a pushed list closed the whole panel. Attaching it to the panel's first page, inside the stack, fixes it.
4. **The chord table needed more than a size set.** It was transparent, oversized, could not scroll and had its title over the rows. tvOS scrolls only by moving focus, so each row must hold focus.

## Disagreements with the strategy

- **The practice options cannot be shared with tvOS.** `ScaleChoiceSection` and half of `DisplayOptionsSection` rely on the Picker's pushed page. The two sections stay iOS-only, and the 50 lines leave the Apple TV's count.
- **`ScaleLibrary.swift` is 199 lines, against 90 planned, and Apple TV does not need it.** Without Transform on TV, the file and the two `PracticeSession` methods that take a library (`choose`, `refreshCustomScale`) are dead there. The scratch build excluded the file and passed. Move those methods into a `PracticeSession+Library.swift` extension, excluded from the tvOS target. Its doc comment says Application Support is "never cleared by the system", which is false on tvOS, another reason to keep it out.
- **Settings and session come to about 114 shared lines, against 85.** Measured without casting and library methods: `PracticeSession` 45, `PracticeSettings` 54, plus about 15 for the forgiving decoder.
- **New lines come to 291, against about 200.** The total still fits (see the budget).
- **The Mac has Xcode 27.1,** which the plan's brief called Xcode 26. `project.yml` already says `xcodeVersion: "26.0"`, which caused no problem.

The strategy's compile claim holds: `Music`, settings and all board and TV views ran on tvOS with no code changes beyond those listed here.

## The target

**Prerequisite.** Strategy steps 1 to 15 are done. If Apple TV must start sooner, steps 1, 4, 7 and 8 are the true dependencies, though line totals would run higher until the data work lands.

**Bundle identifier and listing.** The target uses `com.alnourkhalifa.Scales`, the iPhone app's identifier. Apple's rule is that the tvOS app shares the Apple ID, SKU and bundle ID of the iOS app. In App Store Connect, open Scales, click Add Platform in the sidebar and pick tvOS. Name and other metadata carry over; promotional text, description and screenshots must be supplied again. It becomes a universal purchase once review approves both platforms, and after that **a platform cannot be removed** (decision 1).

**`project.yml`.** Tested as written, in the strategy's layout. The top block is new; delete the two version lines from the iOS target and put `<<: *versionKeys` there. Xcodegen ignores the unknown top-level key, the merge works, and the deploy script's two `grep`s each still match one line, so its build number bump keeps working with no change to that logic.

```yaml
# One version and build number for every platform. Both app targets merge this block, so
# scripts/deploy_to_store.sh still finds exactly one CFBundleVersion line to bump.
versionKeys: &versionKeys
  CFBundleShortVersionString: "1.0"
  CFBundleVersion: "3"

options:
  deploymentTarget:
    iOS: "26.0"
    tvOS: "26.0"          # add this line

targets:
  ScalesTV:
    type: application
    platform: tvOS
    sources:
      - path: Core
        excludes:
          # Saved scales belong to Transform, which stays on phones and tablets.
          - Session/ScaleLibrary.swift
          - Session/PracticeSession+Library.swift
      - path: SharedViews
      - path: ScalesTV
    info:
      path: ScalesTV/Info.plist
      properties:
        <<: *versionKeys
        CFBundleDisplayName: Scales
        UILaunchScreen: {}
        ITSAppUsesNonExemptEncryption: false
    settings:
      base:
        # The same bundle identifier as the iPhone app puts both on one store listing.
        PRODUCT_BUNDLE_IDENTIFIER: com.alnourkhalifa.Scales
        # Keeps the product Scales.app and lets shared tests say @testable import Scales.
        PRODUCT_NAME: Scales
        PRODUCT_MODULE_NAME: Scales
        TARGETED_DEVICE_FAMILY: "3"
        ASSETCATALOG_COMPILER_APPICON_NAME: "App Icon & Top Shelf Image"
        ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME: AccentColor
  ScalesTVTests:
    type: bundle.unit-test
    platform: tvOS
    sources:
      - path: CoreTests
    dependencies:
      - target: ScalesTV
    settings:
      base:
        PRODUCT_BUNDLE_IDENTIFIER: com.alnourkhalifa.ScalesTVTests
        TEST_HOST: "$(BUILT_PRODUCTS_DIR)/Scales.app/$(BUNDLE_EXECUTABLE_FOLDER_PATH)/Scales"
        BUNDLE_LOADER: "$(TEST_HOST)"
        GENERATE_INFOPLIST_FILE: true
        TARGETED_DEVICE_FAMILY: "3"
  ScalesTVUITests:
    type: bundle.ui-testing
    platform: tvOS
    sources:
      - path: ScalesTVUITests
    dependencies:
      - target: ScalesTV
    settings:
      base:
        PRODUCT_BUNDLE_IDENTIFIER: com.alnourkhalifa.ScalesTVUITests
        GENERATE_INFOPLIST_FILE: true
        TEST_TARGET_NAME: ScalesTV
        TARGETED_DEVICE_FAMILY: "3"

schemes:
  ScalesTV:
    build:
      targets:
        ScalesTV: all
        ScalesTVTests: [test]
        ScalesTVUITests: [test]
    run:
      config: Debug
    test:
      config: Debug
      targets:
        - ScalesTVTests
        - ScalesTVUITests
```

Tested with three differences. The app icon line was checked separately with the asset compiler. `CoreTests` does not exist yet, so the test target used `ScalesTests/TransformationTests.swift`. `PracticeSession+Library.swift` does not exist yet either, so only the `ScaleLibrary.swift` exclusion ran. Any test touching `ScaleLibrary` must also be excluded from the tvOS test target.

**Info.plist.** Only the keys above. None of the iOS keys for casting, background audio, Local Network or external-display scenes apply. Add `ScalesTV/Info.plist` to `.gitignore`, because it is generated.

**Deployment target.** tvOS 26.0 matches the iPhone floor, but everything ran on 27.0 only (decision 3).

## Screens

The player holds a guitar, with at most one hand free for a remote they cannot look at. So the most common changes take one press and no menu, and everything else takes two clicks at most, with the board staying visible.

### Remote map

| Where | Press | Result |
|---|---|---|
| Board | Left, right | Previous or next root, wrapping around |
| Board | Up, down | Previous or next scale, wrapping, in picker order |
| Board | Play/pause | Notes, degrees, dots |
| Board | Click | Open options |
| Board | Menu | Home screen, as Apple expects from an app's top level |
| Options | Menu | Close options, focus back on the board |
| Choice list | Click | Choose and stay on the list, so several can be tried against the board |
| Choice list | Menu | Back one page, focus on the row it came from |
| Chord tables | Menu | Back to options |

### Board, first six seconds

The hint fades by itself, in the empty corner beside the notes.

```
+------------------------------------------------------------------+
| G Major                                         Standard tuning  |
| (G)(A)(B)(C)(D)(E)(F#)                                           |
|  1  2  3  4  5  6  7    [Click for options  <> root  ^v scale]   |
|     1   2   3   4   5   6   7   8   9  10  11  12  13  14  15     |
| E  (E)     (F#)(G)     (A)     (B)(C)     (D)     (E)     (F#)(G)|
| ...                     all six strings                          |
+------------------------------------------------------------------+
```

### Board, playing

This is `TVDisplayView`, unchanged, and shows no chrome and no focus ring.

### Options open

The board shrinks instead of being covered, so all 15 frets stay visible. It keeps its proportions because `TVDisplayView` sizes from its own frame.

```
+------------------------------------------+-----------------------+
| G Major                 Standard tuning  | SCALE                 |
| (G)(A)(B)(C)(D)(E)(F#)                   | [Root            G  >]|  <- focus starts here
|                                          |  Scale       Major  > |
|    1  2  3  4  5  6  7  8  9 ... 15      | DISPLAY               |
| E (E)   (F#)(G)  (A)  (B)(C)  ...        |  Notes|Degrees|Dots   |
| B (B)(C)    (D)  (E)  (F#)(G) ...        |  Frets    15 frets  > |
| G (G)   (A)  (B)(C)   (D)  ...           |  Tuning   Standard  > |
| D (D)   (E)  (F#)(G)  (A)  ...           |  Thin string on top On|
| A (A)   (B)(C)   (D)  (E)  ...           |  Left-handed       Off|
| E (E)   (F#)(G)  (A)  (B)(C) ...         |  Dark board         On|
|                                          |  Chord tables         |
+------------------------------------------+-----------------------+
```

### Choice list

Replaces the panel's contents. Scales keep their family headings. The list opens at the top rather than on the current choice (see risks).

```
+------------------------------------------+-----------------------+
| A Dorian                Standard tuning  |        Scale          |
| (A)(B)(C)(D)(E)(F#)(G)                   | Major & Minor         |
|                                          |[Major                ]|
|    the live board, updating as           |  Natural Minor        |
|    each choice is clicked                |  Harmonic Minor       |
|                                          | Pentatonic & Blues    |
|                                          |  Major Pentatonic     |
|                                          |  ...                  |
+------------------------------------------+-----------------------+
```

### Chord tables

Full screen. The switch takes focus first; down moves into the rows, and the focused row is tinted as it scrolls.

```
+------------------------------------------------------------------+
|              [ Major keys ]      Minor keys                      |
|    Key      I     ii    iii    IV     V     vi    vii°           |
|    C       [C]    Dm    Em     F      G     Am    B°             |
|  > G       [G]    Am    Bm     C      D     Em    F#°      <focus|
|    D       [D]    Em    F#m    G      A     Bm    C#°            |
|    ...                                                           |
+------------------------------------------------------------------+
```

### What keeps it small

SwiftUI's `focusable`, `@FocusState`, `onMoveCommand`, `onPlayPauseCommand`, `onExitCommand`, `onTapGesture` (a click on a focused view), `NavigationStack`, `List`, `Toggle`, segmented `Picker` and `fullScreenCover` all worked. The app needs no UIKit beyond the idle timer. `fullScreenCover`'s Menu handling comes free.

## Files

Line counts come from the running prototype, except where marked.

### New

| File | Lines | Purpose |
|---|---:|---|
| `ScalesTV/App/ScalesTVApp.swift` | 19 | App entry; keeps the screen saver away |
| `ScalesTV/BoardScreen.swift` | 98 | Board, remote commands, panel slot, hint |
| `ScalesTV/OptionsPanel.swift` | 111 | Options list, `ChoiceGroup`, `ChoiceRow` |
| `Core/Session/PracticeSettings+Stepping.swift` | 29 | `stepRoot`, `stepScale`, `cycleLabelStyle` |
| `ScalesTV/Assets.xcassets` | none | Icon, top shelf, accent colour |
| **New app code** | **257** | |

Stepping sits in `Core` because Android TV needs the same behaviour, and that lets the answers file pin it.

### Changed

| File | Change | Lines |
|---|---|---:|
| `SharedViews/ChordTablesView.swift` | `television` metrics set (13), `FocusableKeyRow` (15) and its call, metrics choice under `#if os(tvOS)` (3), title hidden on tvOS (3) | +34 |
| `Core/Session/PracticeSession.swift` | Library methods move to `PracticeSession+Library.swift`, iOS only | moved |
| `project.yml` | As above | about +60 |
| `Makefile` | TV targets, below | about +35 |
| `.gitignore` | `ScalesTV/Info.plist` | +1 |
| `data/text.json` | Panel labels and hint text | about +10 |
| `data/features.json`, `tools/check_parity.py` | Apple TV column; remote focus as a TV-only feature | small |
| `tools/make_icons.py` | tvOS layers and top shelf, inside its 40-line budget or about +15 | |
| `CLAUDE.md` | How the TV app is built and tested, the Picker bug, the focus rule | about +8 |
| `scripts/deploy_to_store.sh` | Owner's change, below | about +20 |

Untouched: `TVDisplayView`, `ScaleNotesRow`, `FretboardView`, all of `Core/Music`, and everything under Transform.

### Tests (outside the app budget)

| File | Lines |
|---|---:|
| `CoreTests/SteppingTests.swift` | about 35 |
| `ScalesTVUITests/RemoteTests.swift` | about 110 |
| `ScalesTVUITests/StoreScreenshots.swift` | about 40 |

### Where I refused to cut

- **`ChoiceRow` and `ChoiceGroup` stay as named types,** about 55 lines. Inlining them would repeat a list four times.
- **Each option remains a multi-line call,** one argument per line as swift-format's `lineBreakBeforeEachArgument` requires. Panel rows make up most of the 111.
- **The hint stays,** 16 lines. Nothing else on the board tells a new player what the remote does.
- **`board` keeps its explicit two-branch form** with a comment explaining why, because that branch is the fix for the focus bug.
- **Each view keeps a "why" docstring,** as your code style asks. Reaching 200 would mean removing these.

### Line budget

| | Lines |
|---|---:|
| Strategy's shared set | 1,764 |
| Less library, not compiled on TV | −90 |
| Less practice options, not usable on TV | −50 |
| More for measured settings and session | +29 |
| **Shared code on Apple TV** | **1,653** |
| New files | 257 |
| Chord table additions | 34 |
| **Apple TV total** | **1,944** |
| Strategy's ceiling | 1,964 |

The 29 stepping lines and 34 chord lines also compile into iOS, raising its total by 63, and the true library size raises it by a further 109. The iPhone total comes to about 4,100 against the strategy's 3,942, assuming its other iOS figures hold. The refactor should recount this once Transform lands.

## Settings

**Local storage.** `UserDefaults` is the right place, and `PracticeSession` needs no change. Apple's archived tvOS guide gives apps 500 KB of persistent storage through `UserDefaults` and says everything else may be purged when space is low. The settings are well under 1 KB. If they were ever lost, the app would reopen on G major, and the player would lose a few clicks. The archived guide is old, so its limits are unverified for tvOS 27.

**iCloud key-value store.** Costs about 45 lines (write on change, read at launch, respond to outside changes, last write wins), an iCloud entitlement on both targets and a new iPhone release. Its quota is 1 MB. Because the two apps share a bundle identifier, they would share a store. Nothing was built, so these are estimates. As a backup against purging it is not worth having.

**Syncing between iPhone and Apple TV: I recommend no for the first release.** It would take the total to about 1,989, over the ceiling by 25. A synced saved scale would also reach a TV with no picker entry for it, needing a few more lines and a `customScale = nil` in `stepScale`. The saved-settings format and `init(defaults:)` already leave room to add it later, if players ask.

## Testing

**Shared logic.** `swift test` runs `CoreTests` on the Mac in about a second, as the strategy planned, including the answers-file check, because tvOS compiles the same Swift. `ScalesTVTests` runs the same tests on tvOS itself. That needs only the yml above and adds no test code, and 42 tests took 0.02 s there. A test that reads `expected/answers.json` by source path should work on a simulator, but that is unverified.

**New logic tests.** Root wrapping both ways, scale order matching the picker, label cycling, and each new option's round trip through `UserDefaults`.

**UI tests.** `XCUIRemote.shared.press(.up / .down / .left / .right / .select / .menu / .playPause)` drives all of it, and `tap()` does not exist on tvOS. Find the focused element with the predicate `hasFocus == true`. A focused row reports as `"Root, G"`, which makes assertions easy. Each test should start from a fresh install or a launch argument, because settings persist between runs; my runs inherited each other's scale.

Tests to write, each covering a problem found or a parity feature:

1. Launch shows the board with focus.
2. Each direction and play/pause changes the board.
3. **Regression:** with options open, down moves focus and leaves the scale alone.
4. **Regression:** choosing from every list works and the list is not empty.
5. **Regression:** Menu walks list, panel, board, Home one step at a time.
6. Every option row is reachable with down presses.
7. Chord tables scroll to the last key in both families.
8. Light board shows readable options, by screenshot attachment.
9. Settings persist across `app.terminate()` and relaunch.

**Parity.** Tag these tests the way the iOS UI tests are tagged, so `check_parity.py` passes for every Apple TV "Yes". "Screen stays awake" has no possible automated test, so that tag points to the owner's real-device check and the reviewer knows why. Screenshots must be compared by eye.

**Makefile.** `make build-tv` was run as written, and `make test-tv`'s `xcodebuild` command was run directly with the same destination. `run-tv` stopped at `open -a Simulator`, because I could not find `Simulator.app` inside this Mac's Xcode. The leading `-` below lets it continue, and `simctl install` and `launch` both worked by hand. The existing `make run` will stop at the same line. If no Simulator window can be opened here, run the `ScalesTV` scheme from Xcode to see it.

```make
TV_SIM     ?= Apple TV 4K (3rd generation)
TV_SIM_APP := $(DERIVED)/Build/Products/Debug-appletvsimulator/$(NAME).app

build-tv: gen
	xcodebuild -project $(NAME).xcodeproj -scheme ScalesTV \
	  -destination "platform=tvOS Simulator,name=$(TV_SIM)" \
	  -derivedDataPath $(DERIVED) -quiet build

test-tv: gen
	xcodebuild -project $(NAME).xcodeproj -scheme ScalesTV \
	  -destination "platform=tvOS Simulator,name=$(TV_SIM)" \
	  -derivedDataPath $(DERIVED) -quiet test

run-tv: build-tv
	xcrun simctl boot "$(TV_SIM)" 2>/dev/null || true
	-open -a Simulator
	xcrun simctl install "$(TV_SIM)" $(TV_SIM_APP)
	xcrun simctl launch "$(TV_SIM)" $(BUNDLE_ID)
```

Add `build-tv` and `test-tv` to `.PHONY`, add `ScalesTV` and `ScalesTVUITests` to `make lint` and `make format`, and add `build-tv` to `make verify`. Avoid `xcrun simctl shutdown all` in any target or script: it stops every simulator on the Mac, including another session's test run.

## Build steps

Each step ends with `make test`, `make test-tv` and a commit. The running total starts from the 1,653 shared lines. Hours are agent hours.

| # | Step | Agent time | New lines | Running total | Owner looks at |
|---|---|---:|---:|---:|---|
| 1 | Target, app entry, board on screen, Makefile targets, accent colour, launch test. Replaces the strategy's empty target | 0.5 h | 44 | 1,697 | Run the `ScalesTV` scheme from Xcode or `make run-tv`. The board fills the screen |
| 2 | Remote commands: stepping in `Core` with its tests, then `onMoveCommand` and play/pause | 0.5 h | 69 | 1,766 | Arrow keys in the simulator change the board. Is left/right the right mapping? |
| 3 | Options panel, `ChoiceRow`, focus handling, the three regression tests | 1 h | 128 | 1,894 | Open options, try every row, close with Menu |
| 4 | Chord tables: TV metrics, focusable rows, background, title | 0.5 h | 34 | 1,928 | Readability from across the room; re-run iPhone and iPad tests, since this file is shared |
| 5 | Hint, accessibility labels, remaining UI tests, parity tags, `CLAUDE.md` | 0.5 h | 16 | 1,944 | The hint's wording; `make verify` |
| 6 | Icon layers, top shelf, screenshot run with `make screenshots-tv` | 0.75 h | 0 | 1,944 | The icon and top-shelf images, which need a designer's eye |
| 7 | Deploy script change, for the owner to read and approve | 0.25 h | 0 | 1,944 | The diff. The owner runs the upload, then adds tvOS in App Store Connect |
| | **Total** | **about 4 h** | **291** | | About 2 hours of review, plus real-device time if a device is available |

Steps 1 to 5 use the running prototype as their starting point, so they carry little design risk. The prototype sits in `/private/tmp/claude-501/-Users-alnourkhalifa-personal-projects-scales/73bf8688-637a-4773-a72e-57ae3794d0f4/scratchpad/tvos-layout`, a temporary folder, so copy `ScalesTV/` from it before it is cleaned up.

## Store

**Screenshots.** One size is enough: 3840×2160 or 1920×1080, 1 to 10 images, PNG or JPEG. The 4K simulator's screenshots are 3840×2160 RGB without alpha, so they fit as captured. Reuse the iPhone set's states, saved in a new `Store/Screenshots/tv-4k/` folder: G major, A minor pentatonic, E blues with degrees, the options panel, chord tables. Wait six seconds first so the hint has gone. No home-screen status bar exists to override. `make screenshots-tv` follows the `screenshots` target, with the Python step moved into `tools/` as the strategy asks.

**Icon and top shelf.** tvOS uses a brand-assets set named "App Icon & Top Shelf Image", in place of the single 1024 px iPhone icon. The compiled catalog produced the right `CFBundlePrimaryIcon` and `TVTopShelfImage` keys.

| Asset | @1x | @2x | Form |
|---|---:|---:|---|
| App icon | 400×240 | 800×480 | Layered image stack |
| App Store icon | 1280×768 | none | Layered image stack |
| Top shelf | 1920×720 | 3840×1440 | Flat image |
| Top shelf wide | 2320×720 | 4640×1440 | Flat image |

The sizes come from secondary sources, because Apple's design page could not be fetched; the asset compiler accepted them, and it enforced that the back layer must exactly fill the stack. Two layers compiled. I recall the usual range being two to five, which is unverified. The back layer must be opaque (secondary source). Today's flat icon splits simply into a background and a foreground, so `make_icons.py` makes both sizes from `Store/Icon/`. Xcode's own validation on upload is the final check. A static image is enough for the top shelf; no extension is needed.

**Deploy script.** I did not run or change it. Its build number step works unchanged with the yml above. It archives iOS only, so it needs a second archive and upload. Steps 3 to 5 become a function called once per platform:

```bash
ship_platform() {
  local scheme="$1" platform="$2"
  local archive="$DERIVED/$scheme.xcarchive"
  local export_dir="$DERIVED/Export-$scheme"
  # archive, export and check the upload as today, with those three values
}

ship_platform "$APP_NAME" "iOS"
ship_platform "ScalesTV" "tvOS"
```

Add `--tv-only` and `--ios-only` flags, because each platform's review runs separately and a failed second upload should not force a fresh bump. The existing `Store/ExportOptions.plist` should work for tvOS, which is unverified until the first real upload. Add the platform in App Store Connect before that upload. The build number is shared; Apple allows platforms to differ, but sharing keeps one line in the yml.

## Risks

| Risk | Mitigation | Check on a real device |
|---|---|---|
| The empty Picker page may be specific to tvOS 27.0 or the simulator | `ChoiceRow` avoids it either way. Worth a Feedback report to Apple. Retest on each tvOS release | Open a plain Picker once for the record |
| Swipes on the clickpad may fire `onMoveCommand`, so resting a thumb changes the key mid-song | If so, add a short pause between accepted steps, about 10 lines | Rest and brush the remote while playing; does the board jump? |
| Holding a direction may repeat too fast or not at all | Simulated presses only tested single steps | Hold left; try reaching a far root |
| Screen saver or sleep during long practice. `isIdleTimerDisabled` cannot be tested on a simulator | Set at launch, and re-set when the app becomes active if needed | Leave the board 30 minutes, past the set's sleep timer |
| Overscan: `TVDisplayView`'s margins (77 by 54 points at 1080p) are slightly outside Apple's commonly cited safe area of about 90 by 60, which is unverified, and the view ignores the system's safe area | If the edges crop, drop `.ignoresSafeArea()` on the board. That is one line and leaves a slightly smaller board | Check edge frets and the title on a real TV, including an older set |
| Distance reading: the 1080p simulator shows nothing about three metres away | The sizes come from the existing TV layout, proven through casting. Chord metrics may need tuning | Read chords from where you sit with the guitar. Does the panel's shrunken board stay readable? |
| 22 or 24 frets beside the open panel may be cramped | Tested at 15 frets only. Widen by narrowing the panel from 720 | Open the panel at 24 frets; also try the simulator |
| Lists open at the top, not on the current choice | Up/down on the board covers most use. Fix with `defaultFocus`, about 3 lines, if the owner minds | Feel of a 17-item list with the remote |
| tvOS 26 behaviour is untested | Decision 3 | Run on tvOS 26 if a device has it |
| Bundle shares an identifier across two targets | Worked for build, test, archive here. Signing a real device and uploading are not yet tested | First install, first upload |
| Accessibility: labels exist and the tree reads correctly, but VoiceOver on tvOS has not been heard | The board speaks its scale title; rows speak "E major" | Turn on VoiceOver and walk each screen |
| Changes to shared `ChordTablesView` could disturb iPhone and iPad | TV additions sit behind `#if os(tvOS)`, with the strategy's render checks | Covered by step 4's review |
| Transform decision reverses later | `ScaleLibrary` would need iCloud on TV, because Application Support can be purged there. Cost is unverified | None yet |

**Only a real Apple TV can confirm:** how the clickpad feels, the screen saver, overscan on real sets, text size across a room, and signing and installation. Any Apple TV 4K will do. A friend's device through TestFlight is the fallback, because the simulator alone cannot sign this off.

## Decisions for the owner

1. **One listing, permanently?** I recommend yes, as the strategy does: free for existing buyers, one page. Once approved, tvOS can no longer be removed from the record, so this is the one step in the plan that cannot be undone.
2. **Do you own, or will you buy, an Apple TV before release?** I recommend getting one, because the top real-device risks (clickpad swipes, screen saver) can only be tested on hardware. Steps 1 to 5 can proceed without it; submission should wait.
3. **Which tvOS floor?** I recommend 26.0, matching iPhone, and running one pass on a tvOS 26 simulator before release. If that fails, 27.0 is a one-line change.
4. **Sync settings through iCloud?** I recommend no for the first release, as argued above. It adds about 45 lines, takes the total over the ceiling, and needs a new iPhone release.
5. **Remote mapping?** I recommend left/right for root, up/down for scale, play/pause for labels. This is a design call the simulator cannot settle. Alternatives are easy, because they all live in `handleMove`.
6. **Accept 291 new lines against the 200 budgeted?** I recommend yes, since the total stays under the ceiling and hitting 200 would mean stripping comments or dropping the hint.
7. **Approve the deploy script change?** It is written in step 7 and needs your review before use. You run the upload, as always.

## Unverified

- Anything on real hardware, and any upload or review step.
- tvOS 26 behaviour.
- Whether the Picker bug exists outside the simulator.
- `CoreTests` on tvOS as a whole folder, including `answers.json` access.
- `.pickerStyle(.segmented)` and toggles with a real remote; they passed in simulation.
- Icon layer range, opacity rule and Apple's safe-area numbers.
- iCloud line and entitlement costs.
- tvOS storage limits in current Apple documentation.
- The iPhone line total, until the refactor recounts it.

## Sources

- [App Store Connect: Add platforms](https://developer.apple.com/help/app-store-connect/create-an-app-record/add-platforms/)
- [App Store Connect: Screenshot specifications](https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications/)
- [App Programming Guide for tvOS (archived)](https://developer.apple.com/library/archive/documentation/General/Conceptual/AppleTV_PG/index.html)
- [Storing Preferences in iCloud](https://developer.apple.com/library/ios/documentation/Cocoa/Conceptual/UserDefaults/StoringPreferenceDatainiCloud/StoringPreferenceDatainiCloud.html)
- [tvOS icon and top shelf guide (third party)](https://blog.akring.com/posts/tvos_app_icon_guide_en/)
- [Xamarin tvOS icons notes](https://github.com/MicrosoftDocs/xamarin-docs/blob/live/docs/ios/tvos/app-fundamentals/icons-images.md)
- [Sample brand-assets catalog](https://github.com/bitrise-io/sample-tvos-app/blob/master/sample-tvos-app/Assets.xcassets/App%20Icon%20&%20Top%20Shelf%20Image.brandassets/Contents.json)
