#!/usr/bin/env bash
# Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
# Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
# SPDX-License-Identifier: GPL-3.0-or-later
#
# This file is part of Scales, free software: you may redistribute and change it under the
# GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

# Raise the Android versionCode by one before a store upload.
#
#   scripts/bump_android_version.sh     versionCode N → N+1 in android/version.properties
#
# Why: Play and Amazon both refuse an upload whose versionCode they have already seen, the
# same rule App Store Connect applies to CFBundleVersion (scripts/deploy_to_store.sh bumps
# that one). versionName, the number players see, is left alone; edit it by hand when a new
# store release is wanted.
#
# This script only edits the file. Agents never upload: the owner builds the signed bundle
# with `make bundle-android` and uploads it to Play Console and the Amazon Developer Console
# by hand.
set -euo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION_FILE="$APP_DIR/android/version.properties"

log()  { printf '\033[1;34m▸ %s\033[0m\n' "$*" >&2; }
die()  { printf '\033[1;31m✗ %s\033[0m\n' "$*" >&2; exit 1; }

for argument in "$@"; do
  case "$argument" in
    -h|--help) sed -n '9,20p' "$0"; exit 0 ;;
    *) die "unknown option: $argument (try --help)" ;;
  esac
done

[[ -f "$VERSION_FILE" ]] || die "missing $VERSION_FILE"

current_code="$(grep -E '^versionCode=[0-9]+$' "$VERSION_FILE" | cut -d= -f2)"
[[ -n "$current_code" ]] || die "$VERSION_FILE has no versionCode=<number> line"
version_name="$(grep -E '^versionName=' "$VERSION_FILE" | cut -d= -f2)"

next_code=$((current_code + 1))
sed -i '' -E "s/^versionCode=$current_code\$/versionCode=$next_code/" "$VERSION_FILE"

# Read the file back, so a sed that matched nothing cannot report a bump that did not happen.
written_code="$(grep -E '^versionCode=[0-9]+$' "$VERSION_FILE" | cut -d= -f2)"
[[ "$written_code" == "$next_code" ]] || die "versionCode is still $written_code"

log "versionCode $current_code → $next_code (versionName $version_name)"
log "next: make bundle-android, then upload by hand (see Store/android/listing.md)"
