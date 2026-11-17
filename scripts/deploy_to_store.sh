#!/usr/bin/env bash
# Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
# Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
# SPDX-License-Identifier: GPL-3.0-or-later
#
# This file is part of Scales, free software: you may redistribute and change it under the
# GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

# Ship the current source to App Store Connect (TestFlight / App Store review).
#
#   scripts/deploy_to_store.sh            bump the build number, archive, upload
#   scripts/deploy_to_store.sh --no-bump  keep the build number in project.yml as it is
#
# Why the build number is bumped by default: App Store Connect refuses a second upload
# that carries a version/build pair it has already seen, so every deploy needs a fresh
# CFBundleVersion. The marketing version (CFBundleShortVersionString) is left alone;
# edit it by hand in project.yml when a new store release is wanted.
#
# The upload itself goes through xcodebuild -exportArchive with Store/ExportOptions.plist
# (destination "upload"), which signs with the Apple ID logged into Xcode
# (Xcode › Settings › Accounts). No App Store Connect API key is needed.
set -euo pipefail

APP_NAME="Scales"
APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_YML="$APP_DIR/project.yml"
DERIVED="$APP_DIR/DerivedData"
ARCHIVE="$DERIVED/$APP_NAME.xcarchive"
EXPORT_DIR="$DERIVED/Export"
EXPORT_OPTIONS="$APP_DIR/Store/ExportOptions.plist"

log()  { printf '\033[1;34m▸ %s\033[0m\n' "$*" >&2; }
die()  { printf '\033[1;31m✗ %s\033[0m\n' "$*" >&2; exit 1; }

BUMP_BUILD=true
for argument in "$@"; do
  case "$argument" in
    --no-bump) BUMP_BUILD=false ;;
    -h|--help) sed -n '2,14p' "$0"; exit 0 ;;
    *) die "unknown option: $argument (try --help)" ;;
  esac
done

command -v xcodegen >/dev/null 2>&1 || die "missing xcodegen (brew install xcodegen)"
[[ -f "$EXPORT_OPTIONS" ]] || die "missing $EXPORT_OPTIONS"

# --- 1. Build number -------------------------------------------------------------
current_build="$(grep -E 'CFBundleVersion: "[0-9]+"' "$PROJECT_YML" | sed -E 's/.*"([0-9]+)".*/\1/')"
[[ -n "$current_build" ]] || die "project.yml has no CFBundleVersion"
marketing_version="$(grep -E 'CFBundleShortVersionString: "[0-9.]+"' "$PROJECT_YML" | sed -E 's/.*"([0-9.]+)".*/\1/')"

if [[ "$BUMP_BUILD" == true ]]; then
  next_build=$((current_build + 1))
  sed -i '' -E "s/CFBundleVersion: \"$current_build\"/CFBundleVersion: \"$next_build\"/" "$PROJECT_YML"
  log "build number $current_build → $next_build (version $marketing_version)"
else
  next_build="$current_build"
  log "keeping build number $current_build (version $marketing_version)"
fi

# --- 2. Regenerate the Xcode project so the new Info.plist values are picked up ---
log "xcodegen generate"
(cd "$APP_DIR" && xcodegen generate --quiet)

# --- 3. Release archive for a real iPhone -------------------------------------------
log "archiving $APP_NAME (Release)"
rm -rf "$ARCHIVE" "$EXPORT_DIR"
xcodebuild -project "$APP_DIR/$APP_NAME.xcodeproj" -scheme "$APP_NAME" -configuration Release \
  -destination "generic/platform=iOS" -archivePath "$ARCHIVE" \
  -derivedDataPath "$DERIVED" -allowProvisioningUpdates -quiet archive

# --- 4. Upload to App Store Connect ---------------------------------------------------
log "uploading build $next_build to App Store Connect"
xcodebuild -exportArchive -archivePath "$ARCHIVE" \
  -exportOptionsPlist "$EXPORT_OPTIONS" \
  -exportPath "$EXPORT_DIR" -allowProvisioningUpdates

# --- 5. Confirm from the archive's own record that Apple accepted it ------------------
# Xcode writes every distribution attempt into the archive's Info.plist; the last one
# carries an uploadEvent whose state is "success" when the upload went through.
upload_state="$(plutil -extract 'Distributions.0.uploadEvent.state' raw -o - "$ARCHIVE/Info.plist" 2>/dev/null || true)"
uploaded_build="$(plutil -extract 'Distributions.0.uploadedBuildNumber' raw -o - "$ARCHIVE/Info.plist" 2>/dev/null || true)"

# Apple's numeric app id (adamId) is also recorded there; it is what the App Store Connect
# page URLs are keyed on. 6821490303 is Scales' id, kept as a fallback for older archives.
app_store_id="$(plutil -extract 'Distributions.0.adamId' raw -o - "$ARCHIVE/Info.plist" 2>/dev/null || true)"
app_store_id="${app_store_id:-6821490303}"

if [[ "$upload_state" == "success" ]]; then
  log "done: version $marketing_version build ${uploaded_build:-$next_build} is on App Store Connect"
  log "it shows up under TestFlight after Apple finishes processing (usually 10–30 min)"
  log "attach it to the release here: https://appstoreconnect.apple.com/apps/$app_store_id/distribution"
  log "TestFlight builds:             https://appstoreconnect.apple.com/apps/$app_store_id/testflight/ios"
else
  die "the archive does not record a successful upload (state: '${upload_state:-none}'); check the xcodebuild output above"
fi
