# Scales: what's left to do

Last updated 5 November 2026.

The Transform tab is finished on iPhone, iPad and Mac: the player changes one note of a
scale, reads what happens to every chord, and keeps the result as a lesson. The Mac version
builds and packages as a disk image. What remains is below, in the agreed order. Each step
is built, tested, and checked with Alnour before the next one starts.

## Mac download: steps only Alnour can take

- [ ] Create a Developer ID Application certificate in Xcode › Settings › Accounts ›
      Manage Certificates › + › Developer ID Application. Only the team's Account Holder can.
- [ ] Make an app-specific password at account.apple.com › Sign-In and Security ›
      App-Specific Passwords, and store it once:
      `xcrun notarytool store-credentials scales-notary --apple-id <Apple ID> --team-id 858CYQVKF8`
- [ ] Build the disk image to share with `scripts/build_mac_dmg.sh`. Its first run registers
      the Mac app's id, `maccatalyst.com.alnourkhalifa.Scales`, with the team.
- [ ] Notarize and staple it with `scripts/notarize_mac_dmg.sh`.
- [ ] Open the notarized disk image on another Mac and confirm it installs without a warning.
- [ ] Try casting to the TV from the Mac with the AirPlay button, and look over the Mac
      window layout. Neither has been checked yet.

## Web version

TypeScript, React and Vite, in a `web/` folder in this project. It has four parts: the
fretboard with its scale picker, the chord tables, Transform with lessons, and a
full-screen board page for a TV. Scales and lessons are saved in the browser.

- [ ] Expected answers: a small command-line tool compiled from `Scales/Music` writes every
      scale's notes, chords and lesson sentences to a JSON file, with a check that the file
      is current. It should be the same file the Android plan relies on
      (`docs/plans/shared-strategy.md`), so it is only built once.
- [ ] Web music theory: port `Scales/Music` to TypeScript, tested against every expected answer.
- [ ] Web saving: settings, scales and lessons in browser storage, in the same shape as the
      app's `ScaleLibrary.json`.
- [ ] Web screens: Scales, Chords, Transform and the TV page in React, with browser tests that
      walk through a whole lesson.

## Windows installer

- [ ] Wrap the web screens with Tauri into one Windows setup file, built on this Mac. Rust is
      installed; Tauri calls building Windows installers on a Mac experimental.
- [ ] Open the installer on a Windows PC, since this Mac can't run it.
- [ ] Decide on Windows code signing. Without a paid certificate, Windows warns "Windows
      protected your PC" and people have to click More info, then Run anyway.

## Decisions waiting

- [ ] Where the downloads go online: GitHub Releases, the personal website, or somewhere else.
      Nothing gets published without asking first.
- [ ] Whether to push this repository to a private GitHub repository, as the shared strategy
      recommends.
- [ ] Whether My Scales should come first in the scale pickers instead of after the built-in
      scales. On the phone it currently means scrolling the menu.
- [ ] The Apple TV and Android plans in `docs/plans/` stay on hold until Alnour says to start.

## Known quirks

- On this Mac, xcodebuild waits several minutes after the last test before it exits. The
  Xcode 27 beta can't read the iOS 26.5 simulator's build number. The tests themselves
  finish in seconds.
