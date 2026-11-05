#!/usr/bin/env bash
# Send the Mac disk image to Apple's notary service and staple the verdict to it, so the
# download opens on any Mac without a warning, even offline.
#
#   scripts/notarize_mac_dmg.sh                     the newest DerivedData/MacRelease/Scales-*.dmg
#   scripts/notarize_mac_dmg.sh path/to/Scales.dmg  a specific disk image
#
# One-time setup, which keeps an app-specific password in the keychain under a profile name
# (make the password at account.apple.com › Sign-In and Security › App-Specific Passwords):
#   xcrun notarytool store-credentials scales-notary --apple-id <your Apple ID> --team-id 858CYQVKF8
# Another profile name can be given with NOTARY_PROFILE=<name>.
set -euo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROFILE="${NOTARY_PROFILE:-scales-notary}"

log() { printf '\033[1;34m▸ %s\033[0m\n' "$*" >&2; }
die() { printf '\033[1;31m✗ %s\033[0m\n' "$*" >&2; exit 1; }

case "${1:-}" in
  -h|--help) sed -n '2,12p' "$0"; exit 0 ;;
esac

dmg="${1:-}"
if [[ -z "$dmg" ]]; then
  for candidate in $(ls -t "$APP_DIR"/DerivedData/MacRelease/Scales-*.dmg 2>/dev/null); do
    if [[ "$candidate" != *local-test* ]]; then
      dmg="$candidate"
      break
    fi
  done
fi
[[ -n "$dmg" && -f "$dmg" ]] || die "no disk image found; build one with scripts/build_mac_dmg.sh"
[[ "$dmg" != *local-test* ]] || die "a --local-test disk image can't be notarized; build without --local-test"

log "sending $(basename "$dmg") to Apple's notary service (usually a few minutes)"
result="$(xcrun notarytool submit "$dmg" --keychain-profile "$PROFILE" --wait --output-format json)"
status="$(python3 -I -c 'import json, sys; print(json.loads(sys.argv[1]).get("status", ""))' "$result")"
submission="$(python3 -I -c 'import json, sys; print(json.loads(sys.argv[1]).get("id", ""))' "$result")"
if [[ "$status" != "Accepted" ]]; then
  die "Apple answered '$status'. See why with: xcrun notarytool log $submission --keychain-profile $PROFILE"
fi

log "accepted; stapling the ticket to the disk image"
xcrun stapler staple "$dmg"
spctl --assess --type open --context context:primary-signature --verbose "$dmg"
log "done: $dmg is ready to put online"
