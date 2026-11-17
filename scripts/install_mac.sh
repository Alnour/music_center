#!/usr/bin/env bash
# Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
# Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
# SPDX-License-Identifier: GPL-3.0-or-later
#
# This file is part of Scales, free software: you may redistribute and change it under the
# GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

# Put the current source on this Mac as /Applications/Scales.app.
#
#   scripts/install_mac.sh            build a Release copy, replace the installed one, open it
#   scripts/install_mac.sh --no-open  the same without opening it afterwards
#
# Why this exists: the download for other people is a notarized disk image
# (scripts/build_mac_dmg.sh), which needs the Developer ID certificate and a trip to Apple's
# notary service. For the Mac in hand none of that is needed: the app is built in Release,
# signed for this Mac alone ("-", the same as `make mac`), and copied straight into
# Applications. Run it again after every release to update the installed copy; the old one
# is quit and replaced, and its settings survive because they live in the app's container,
# not in the app.
set -euo pipefail

APP_NAME="Scales"
APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_YML="$APP_DIR/project.yml"
DERIVED="$APP_DIR/DerivedData"
BUILT_APP="$DERIVED/Build/Products/Release-maccatalyst/$APP_NAME.app"
INSTALLED_APP="/Applications/$APP_NAME.app"

log() { printf '\033[1;34m▸ %s\033[0m\n' "$*" >&2; }
die() { printf '\033[1;31m✗ %s\033[0m\n' "$*" >&2; exit 1; }

OPEN_AFTER=true
for argument in "$@"; do
  case "$argument" in
    --no-open) OPEN_AFTER=false ;;
    -h|--help) sed -n '2,13p' "$0"; exit 0 ;;
    *) die "unknown option: $argument (try --help)" ;;
  esac
done

command -v xcodegen >/dev/null 2>&1 || die "missing xcodegen (brew install xcodegen)"

version="$(grep -E 'CFBundleShortVersionString: "[0-9.]+"' "$PROJECT_YML" | sed -E 's/.*"([0-9.]+)".*/\1/')"
build="$(grep -E 'CFBundleVersion: "[0-9]+"' "$PROJECT_YML" | sed -E 's/.*"([0-9]+)".*/\1/')"

# --- 1. Build ----------------------------------------------------------------------------
log "xcodegen generate"
(cd "$APP_DIR" && xcodegen generate --quiet)

log "building $APP_NAME $version ($build) for the Mac (Release, signed for this Mac only)"
xcodebuild -project "$APP_DIR/$APP_NAME.xcodeproj" -scheme "$APP_NAME" -configuration Release \
  -destination "platform=macOS,variant=Mac Catalyst" -derivedDataPath "$DERIVED" \
  CODE_SIGN_STYLE=Manual "CODE_SIGN_IDENTITY=-" DEVELOPMENT_TEAM= PROVISIONING_PROFILE_SPECIFIER= \
  -quiet build
[[ -d "$BUILT_APP" ]] || die "no app at $BUILT_APP"
codesign --verify --deep --strict "$BUILT_APP" || die "the built app's signature does not verify"

# --- 2. Replace the installed copy ------------------------------------------------------
# A running copy keeps its old code in memory, so it is quit first; macOS would otherwise
# show the old version until the next launch.
if pgrep -fq "$INSTALLED_APP/Contents/MacOS/$APP_NAME"; then
  log "quitting the running $APP_NAME"
  osascript -e "tell application \"$INSTALLED_APP\" to quit" >/dev/null 2>&1 || true
  sleep 2
fi
if [[ -d "$INSTALLED_APP" ]]; then
  log "replacing $INSTALLED_APP"
  rm -rf "$INSTALLED_APP"
else
  log "installing $INSTALLED_APP"
fi
# ditto keeps the signature and resource forks intact, which cp -R does not promise.
ditto "$BUILT_APP" "$INSTALLED_APP"

installed_version="$(plutil -extract CFBundleShortVersionString raw -o - "$INSTALLED_APP/Contents/Info.plist")"
installed_build="$(plutil -extract CFBundleVersion raw -o - "$INSTALLED_APP/Contents/Info.plist")"
log "done: $APP_NAME $installed_version ($installed_build) is in /Applications"

if [[ "$OPEN_AFTER" == true ]]; then
  open "$INSTALLED_APP"
fi
