# Scales

SwiftUI iOS app (iOS 26+, Swift 6) that draws any scale across the guitar neck, or on one
piano keyboard (two octaves by default) when the player picks Keyboard, and shows it full-screen on a TV through AirPlay
screen mirroring (Roku, Apple TV, AirPlay smart TVs).
The Xcode project is **generated** by xcodegen from `project.yml`; edit the yml, never the
`.xcodeproj` (git-ignored).

- `make build` · `make run` (simulator) · `make test` · `make lint` / `make format`
  · `make run-device DEVICE="<iPhone name>"` · `scripts/deploy_to_store.sh` (bumps the build
  number, archives, uploads to App Store Connect). The Makefile is self-contained because the shared
  `../ios_apps/tools` scripts only find apps inside the ios_apps folder. Xcode 27 replaced the
  Simulator app with DeviceHub (`Xcode.app/Contents/Applications`); `make run` opens either.
- Mac version: the same app built with Mac Catalyst and handed out as a download, outside the
  App Store. `make mac` · `make mac-test` (unit tests only) · `make run-mac` sign locally ("-"),
  so nothing is registered with the developer account. The Mac copy's bundle id is
  `maccatalyst.com.alnourkhalifa.Scales` (`PRODUCT_BUNDLE_IDENTIFIER[sdk=macosx*]`; Xcode 27
  rejects `DERIVE_MACCATALYST_PRODUCT_BUNDLE_IDENTIFIER`), so the App Store record is untouched.
  `make install-mac` (`scripts/install_mac.sh`) builds a Release copy signed for this Mac
  only and replaces `/Applications/Scales.app`, quitting a running copy first; run it after
  each release to update the Mac in hand.
  `scripts/build_mac_dmg.sh` makes `DerivedData/MacRelease/Scales-<version>.dmg`; it needs a
  Developer ID Application certificate (`--local-test` signs for this Mac only), and Release
  builds use the hardened runtime that notarization requires. `scripts/notarize_mac_dmg.sh`
  uploads it to Apple's notary service and staples it; like the store script, the owner runs it.
  `DeviceKind` names the device (phone, iPad, Mac); the Mac skips the mirroring-detected
  fallback to the full-screen board.
- Instrument (a segmented pill in the Scales tab's toolbar row): `Instrument` is guitar or keyboard.
  `KeyboardLayout` (C to a closing C, 13 keys per octave plus one; one or two octaves) and
  `KeyboardView` mirror `FretboardLayout` and `FretboardView`; `TVDisplayView` and
  `ContentView` switch on `PracticeSettings.selectedInstrument`. The stored fields
  `instrument` and `keyboardOctaves` are optional and nil for the defaults (guitar, two
  octaves), so old settings still load, a guitar player's saved JSON and TV clip key are
  unchanged, and default settings still equal `PracticeSettings()` for the prebuilt clip.
  Scale keys carry a circle (labels as on the fretboard) and the note name on the key
  unless the circles already show names. Frets, tuning and string options are hidden while
  Keyboard is chosen; the Octaves picker is hidden for the guitar.
- Layout: `Scales/Music` is pure theory (spelling, formulas, tunings, fret positions) with no UI;
  `Scales/Session` holds the saved settings shared by both screens; `Scales/Views` draws them.
- Chords on the Scales screen (`ScaleChordsRow`): the scale's `letterTriads()` as chips
  under the board, all on one screen with the notes row, the board and a single row holding
  the Root and Scale menus plus `CompactScaleEditMenu` (nested menus; the Transform tab
  keeps the row-per-note `ScaleEditMenus`) to change one note in place. Tapping a chip
  rings the chord's notes on the board, the notes row, the TV scene and the full-screen
  board (`PracticeSession.ringedChord`, this run only; `ringedNotes` is empty once the
  scale lacks one of the notes; `ChordRing` draws the ring; chord notes take the palette's
  green `chordFill`, the root keeps its own colour, the ring is `chordRing`). The cast AirPlay clip is keyed
  by settings and does not carry the rings. `PracticeSession.transform` saves the change as a lesson (a new scale is kept
  under its working name, "Major ♭7"), puts the result on the board and keeps a
  `BoardChange` for this run only; `currentBoardChange` is nil once the player picks another
  scale or key. The chips then mark each chord that used the note ("was D", new note in
  green, lost chords crossed out) and a Back button restores the starting scale. The
  The Scales screen is a `VStack`, not a Form: one toolbar row (`BoardToolbar`: Root and
  Scale menus, Guitar/Keyboard and Notes/Degrees/Dots pills, the note-change menu), the
  notes row, the board filling the remaining height, and the chord cards in a grid capped
  at a third of the screen. One-time setup (frets, tuning, string order, octaves) sits in
  a popover behind the gear button (`BoardSettingsMenu`, identifier `boardSettings`); the
  AirPlay picker, status, dark TV theme and full-screen board sit in a popover behind the
  TV button (`TVMenu`, identifier `tvMenu`), whose symbol and tint follow `TVStatus`.
- Transform tab (`TransformView`): the player changes one note of a scale (lower, raise,
  remove or add) and a lesson explains what each chord becomes. Scales are `ScaleDefinition`s
  (name + degrees); chords are stacked by letter (`Scale.letterTriads`) so any scale size
  works, and `diatonicTriads()` (the Chords tab) wraps it for seven-note scales. Chords carry
  two numerals: `numeral` is plain for the key tables (III in minor), `formulaNumeral` carries
  the degree's flat or sharp for lessons (♭III). Results are recognised by sound
  (`ScaleIdentifier`, built-ins before saved scales so Major wins over Ionian). Saved scales
  and lessons live in `ScaleLibrary`, one JSON file at
  `Application Support/Scales/ScaleLibrary.json`; an unreadable file is moved aside as
  `ScaleLibrary-unreadable.json`. The board keeps a full copy of a chosen saved scale in
  `PracticeSettings.customScale` (optional, so old settings still load and the TV clip key
  changes with the scale); `RootView` keeps that copy in step when a scale is renamed or deleted.
- TV screen: iOS 26 only hands the app the external screen because a scene for the role
  `UIWindowSceneSessionRoleExternalDisplayNonInteractive` is declared in Info.plist
  (`project.yml`). Answering in `AppDelegate` alone is not enough. `TVSceneDelegate` owns the
  TV window and logs under subsystem `com.alnourkhalifa.Scales`, category `TV`.
- Simulator quirk: I/O › External Displays always reports a 720×480 screen whatever size is
  picked, so the TV layout renders in the top-left corner there. Check real sizing on a TV.
- iPad: the app runs on iPhone and iPad (`TARGETED_DEVICE_FAMILY` "1,2"). iPadOS 26 ignores
  orientation requests because every iPad window can be resized, so `OrientationLock` only
  acts on the iPhone (`canRotateScreen`); on an upright iPad the full-screen board keeps the
  TV's 16:9 shape and asks the player to turn the iPad. Wide windows (regular horizontal
  size class) get a taller board on the Scales screen and roomier chord tables; an iPad
  app sharing the screen with another one falls back to the phone sizes. The iPad shows its
  tabs floating at the top, which UI tests can't find as a tab bar, so tests open tabs
  through `XCUIApplication.openTab`. Test on `make test SIM="iPad Pro 13-inch (M5)"` too;
  `make screenshots-ipad` makes the 13-inch iPad store screenshots.
- How casting works (checked against Apple DTS replies on the developer forums, Oct 2026):
  an app cannot start Screen Mirroring itself; the player starts it from Control Center.
  Once mirroring runs, iOS offers the foreground app a scene with the external-display role;
  attaching a window to it replaces the mirrored copy with the app's own TV content, and
  removing the window restores plain mirroring. Capture tools (QuickTime, iPhone Mirroring
  to a Mac) never offer that scene. The phone's own mirroring state is read through the
  `sceneCaptureState` trait (`CaptureStateReader`); when it is active but no TV scene
  arrives within two seconds, `ContentView` opens the full-screen board as a fallback.
- In-app casting (the AirPlay button): the TV layout is rendered at 1080p and encoded once as
  a 10-second H.264 piece with a single keyframe (`BoardVideoWriter`, fixed quality when the
  encoder supports it). `ClipExtender` repeats its compressed frames into one continuous
  5-minute stream with fresh silent AAC, without re-encoding (~0.4 s, ~4 MB). Stitching
  copies with an edit list made the Roku stutter every 10 s, and short clips that loop
  make the Roku drop the session, so keep both properties. Clips are cached per settings
  (`ClipCache`, bump `clipFormatVersion` when the format changes); default G major's piece
  ships prebuilt as `Resources/GMajorBoard.mp4` (ffmpeg, `-g 300 -bf 0`). Played by an
  `AVPlayer` with external playback allowed; picking the TV again resumes a paused player.
  Another app streaming (a video or music app) interrupts the audio session and pauses
  the player, which hands the TV to that app; the session is reactivated and the clip
  restarted when the app returns to the foreground, when the interruption ends, from
  the Resume button, and when the AirPlay menu closes with a receiver in the route
  (`AirPlayPickerButton` delegate; iOS reports no route change when the other app had
  already picked that same TV), so Scales takes the TV back. iOS will not AirPlay the picture of a video
  file stored on the phone (it falls back to audio only while still reporting external
  playback), so `ClipServer` serves clips over HTTP on the phone's Wi-Fi address and the
  player gets that URL; the TV fetches it directly. This needs Local Network permission.
- Roku: AirPlay mirroring needs the one-time four-digit code (Settings › Apple AirPlay and
  HomeKit › Require Code). Roku's control protocol (port 8060) is handy for diagnosis
  (`curl http://<roku>:8060/query/active-app`) but Roku forbids third-party mobile apps from
  sending it commands, so it is not a casting path for this app.
