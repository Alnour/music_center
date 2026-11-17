#!/usr/bin/env bash
# Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
# Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
# SPDX-License-Identifier: GPL-3.0-or-later
#
# This file is part of Scales, free software: you may redistribute and change it under the
# GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

# Build Scales for Alnour's iPhone 18 Pro Max, install it, and launch it.
#
#   scripts/install_iphone_18.sh
#
# Why this exists instead of `make run-device`: two phones are paired with this Mac and
# both are named plain "iPhone", so a name-based destination is ambiguous. On top of
# that, xcodebuild and devicectl identify the same phone differently — xcodebuild wants
# the hardware UDID, devicectl wants its own CoreDevice identifier — so the two
# identifiers are pinned here side by side.
#
# Requires an Xcode that supports the phone's iOS version (iOS 27 needs Xcode 27). With
# an older Xcode the build fails with "developer disk image could not be mounted".
set -euo pipefail

APP_NAME="Scales"
BUNDLE_ID="com.alnourkhalifa.Scales"
APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DERIVED="$APP_DIR/DerivedData"
APP_BUNDLE="$DERIVED/Build/Products/Debug-iphoneos/$APP_NAME.app"

# iPhone 18 Pro Max — the same phone, as each tool names it
# (xcrun xctrace list devices / xcrun devicectl list devices).
XCODEBUILD_UDID="00008160-000261E234A0000A"
DEVICECTL_ID="39C46D06-A136-587E-AE8E-6AD94BE5E68D"

log()  { printf '\033[1;34m▸ %s\033[0m\n' "$*" >&2; }
fail() { printf '\033[1;31m✗ %s\033[0m\n' "$*" >&2; exit 1; }

cd "$APP_DIR"

log "Checking the phone is connected"
# Newer devicectl releases list the phone by its hardware UDID rather than the
# CoreDevice identifier, so accept either.
xcrun devicectl list devices 2>/dev/null \
  | grep -E "($DEVICECTL_ID|$XCODEBUILD_UDID).*available" -q \
  || fail "iPhone 18 Pro Max is not connected (unlock it and plug it in, then retry)"

log "Generating the Xcode project"
xcodegen generate --quiet

log "Building for the iPhone"
xcodebuild -project "$APP_NAME.xcodeproj" -scheme "$APP_NAME" \
  -destination "platform=iOS,id=$XCODEBUILD_UDID" \
  -derivedDataPath "$DERIVED" -allowProvisioningUpdates -quiet build

log "Installing"
xcrun devicectl device install app --device "$DEVICECTL_ID" "$APP_BUNDLE"

log "Launching"
xcrun devicectl device process launch --device "$DEVICECTL_ID" \
  --terminate-existing "$BUNDLE_ID"

log "Done — $APP_NAME is running on the iPhone"
