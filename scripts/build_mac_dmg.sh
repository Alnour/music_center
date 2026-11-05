#!/usr/bin/env bash
# Build the Mac version of Scales as one disk image people download and open.
#
#   scripts/build_mac_dmg.sh               Developer ID signed, ready for notarizing
#   scripts/build_mac_dmg.sh --local-test  signed for this Mac only, to check the packaging
#
# Why two modes: a download opens on other Macs without a warning only when it is signed
# with a "Developer ID Application" certificate and then notarized by Apple. That
# certificate is created once in Xcode › Settings › Accounts › Manage Certificates › + ›
# Developer ID Application (only the team's Account Holder can). Until it exists,
# --local-test builds the same disk image signed for this Mac alone, so the packaging can
# be checked; other Macs will refuse to open that copy.
#
# The Developer ID mode signs automatically through the Apple ID in Xcode, so its first
# run registers the Mac app's id (maccatalyst.com.alnourkhalifa.Scales) with the team.
#
# Notarizing uploads the disk image to Apple and is a separate step:
#   scripts/notarize_mac_dmg.sh
set -euo pipefail

APP_NAME="Scales"
TEAM_ID="858CYQVKF8"
APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_YML="$APP_DIR/project.yml"
DERIVED="$APP_DIR/DerivedData"
OUT_DIR="$DERIVED/MacRelease"
ARCHIVE="$OUT_DIR/$APP_NAME-Mac.xcarchive"
EXPORT_DIR="$OUT_DIR/Export"
STAGING="$OUT_DIR/dmg-contents"
EXPORT_OPTIONS="$APP_DIR/Store/ExportOptions-Mac.plist"

log() { printf '\033[1;34m▸ %s\033[0m\n' "$*" >&2; }
die() { printf '\033[1;31m✗ %s\033[0m\n' "$*" >&2; exit 1; }

LOCAL_TEST=false
for argument in "$@"; do
  case "$argument" in
    --local-test) LOCAL_TEST=true ;;
    -h|--help) sed -n '2,18p' "$0"; exit 0 ;;
    *) die "unknown option: $argument (try --help)" ;;
  esac
done

command -v xcodegen >/dev/null 2>&1 || die "missing xcodegen (brew install xcodegen)"
[[ -f "$EXPORT_OPTIONS" ]] || die "missing $EXPORT_OPTIONS"

version="$(grep -E 'CFBundleShortVersionString: "[0-9.]+"' "$PROJECT_YML" | sed -E 's/.*"([0-9.]+)".*/\1/')"
[[ -n "$version" ]] || die "project.yml has no CFBundleShortVersionString"

# --- 1. Signing identity ------------------------------------------------------------
# Checked before anything is built, so a missing certificate fails in a second rather
# than after a full archive.
identity=""
if [[ "$LOCAL_TEST" == true ]]; then
  log "local test: signing for this Mac only"
else
  identity="$(security find-identity -v -p codesigning \
    | grep -o "\"Developer ID Application: [^\"]*($TEAM_ID)\"" | head -1 | tr -d '"' || true)"
  [[ -n "$identity" ]] || die "no Developer ID Application certificate for team $TEAM_ID in the keychain.
  Create it in Xcode › Settings › Accounts › Manage Certificates › + › Developer ID Application,
  or run with --local-test to check the packaging without it."
  log "signing with $identity"
fi

# --- 2. Regenerate the Xcode project ------------------------------------------------
log "xcodegen generate"
(cd "$APP_DIR" && xcodegen generate --quiet)

# --- 3. Release archive for the Mac -------------------------------------------------
rm -rf "${OUT_DIR:?}"
mkdir -p "$OUT_DIR"
if [[ "$LOCAL_TEST" == true ]]; then
  signing_args=(CODE_SIGN_STYLE=Manual "CODE_SIGN_IDENTITY=-" DEVELOPMENT_TEAM= PROVISIONING_PROFILE_SPECIFIER=)
else
  signing_args=(-allowProvisioningUpdates)
fi
log "archiving $APP_NAME $version for the Mac (Release)"
xcodebuild -project "$APP_DIR/$APP_NAME.xcodeproj" -scheme "$APP_NAME" -configuration Release \
  -destination "generic/platform=macOS,variant=Mac Catalyst" -archivePath "$ARCHIVE" \
  -derivedDataPath "$DERIVED" "${signing_args[@]}" -quiet archive

# --- 4. The app to ship -------------------------------------------------------------
if [[ "$LOCAL_TEST" == true ]]; then
  mkdir -p "$EXPORT_DIR"
  cp -R "$ARCHIVE/Products/Applications/$APP_NAME.app" "$EXPORT_DIR/"
else
  log "exporting with Developer ID signing"
  xcodebuild -exportArchive -archivePath "$ARCHIVE" -exportOptionsPlist "$EXPORT_OPTIONS" \
    -exportPath "$EXPORT_DIR" -allowProvisioningUpdates
fi
app="$EXPORT_DIR/$APP_NAME.app"
[[ -d "$app" ]] || die "no $APP_NAME.app in $EXPORT_DIR"

# --- 5. Check the signature before packaging ------------------------------------------
# Apple's notary service rejects apps without the hardened runtime, so its absence is
# caught here instead of after an upload.
codesign --verify --deep --strict "$app" || die "the app's signature does not verify"
signature="$(codesign --display --verbose=2 "$app" 2>&1)"
grep -q "flags=.*runtime" <<<"$signature" || die "the app was built without the hardened runtime"
if [[ "$LOCAL_TEST" == false ]]; then
  grep -q "Authority=Developer ID Application" <<<"$signature" \
    || die "the exported app is not signed with Developer ID"
fi

# --- 6. Disk image: the app beside a shortcut to Applications, to drag it across ------
mkdir -p "$STAGING"
cp -R "$app" "$STAGING/"
ln -s /Applications "$STAGING/Applications"
suffix=""
if [[ "$LOCAL_TEST" == true ]]; then
  suffix="-local-test"
fi
dmg="$OUT_DIR/$APP_NAME-$version$suffix.dmg"
log "making $(basename "$dmg")"
# `diskutil image` replaces the deprecated `hdiutil create`; older macOS only has the latter.
if diskutil image create from --help >/dev/null 2>&1; then
  diskutil image create from --format UDZO --volumeName "$APP_NAME" "$STAGING" "$dmg" >/dev/null
else
  hdiutil create -volname "$APP_NAME" -srcfolder "$STAGING" -ov -format UDZO "$dmg" >/dev/null
fi
rm -rf "${STAGING:?}"
if [[ "$LOCAL_TEST" == false ]]; then
  codesign --sign "$identity" --timestamp "$dmg"
fi

log "done: $dmg"
if [[ "$LOCAL_TEST" == true ]]; then
  log "this copy opens only on this Mac; build without --local-test to make the one to share"
else
  log "next: scripts/notarize_mac_dmg.sh   (uploads it to Apple's notary service)"
fi
