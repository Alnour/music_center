#!/bin/sh
# Usage: scripts/check_apk.sh [release.apk] [release.aab]
#
# Checks the release APK and bundle against what Play and Amazon reviewers test for TV apps,
# so a wrong artefact fails here instead of in review:
#   - native code: the app has none of its own; Compose brings only
#     libandroidx.graphics.path.so, and both 32-bit (armeabi-v7a, Fire TV sticks) and 64-bit
#     (arm64-v8a) copies must ship;
#   - 16 KB page alignment of native libraries (Play requirement, zipalign -P 16);
#   - manifest: a phone launcher and a TV launcher activity, leanback and touchscreen not
#     required, no required feature at all, no permissions beyond the one AndroidX adds for
#     itself, and a TV banner.
# Every failure is listed before the script exits, so one run shows everything to fix.
set -eu

sdk=${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}
build_tools="$sdk/build-tools/36.0.0"
outputs=android/app/build/outputs

default_apk=$(ls "$outputs"/apk/release/*.apk 2> /dev/null | head -n 1 || true)
apk=${1:-$default_apk}
aab=${2:-$outputs/bundle/release/app-release.aab}

package=com.alnourkhalifa.scales
allowed_native=libandroidx.graphics.path.so
# AndroidX core declares this permission for its own dynamic receivers; it grants nothing.
allowed_permission="$package.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"

failures=0
fail() {
    echo "FAIL: $1" >&2
    failures=$((failures + 1))
}

# Every native file must be the allowed library, and both ABIs must be present.
# $1 = archive, $2 = path prefix of lib/ inside it ("" for an APK, "base/" for a bundle).
check_native_files() {
    archive=$1
    prefix=$2
    native_files=$(unzip -Z1 "$archive" | grep "^${prefix}lib/" || true)
    for entry in $native_files; do
        library=$(basename "$entry")
        if [ "$library" != "$allowed_native" ]; then
            fail "$archive: unexpected native file $entry"
        fi
    done
    for abi in armeabi-v7a arm64-v8a; do
        if ! echo "$native_files" | grep -q "^${prefix}lib/$abi/"; then
            fail "$archive: no $abi native library"
        fi
    done
}

if [ -z "$apk" ] || [ ! -f "$apk" ]; then
    echo "No release APK found; run: make bundle-android" >&2
    exit 1
fi
echo "Checking $apk"

check_native_files "$apk" ""

if ! "$build_tools/zipalign" -c -P 16 -v 4 "$apk" > /dev/null; then
    fail "$apk: not aligned for 16 KB pages (zipalign -c -P 16 -v 4)"
fi

badging=$("$build_tools/aapt2" dump badging "$apk")
expect_badging() {
    if ! echo "$badging" | grep -qF "$1"; then
        fail "$apk: badging lacks: $1"
    fi
}
expect_badging "launchable-activity: name='$package.app.phone.PhoneActivity'"
expect_badging "leanback-launchable-activity: name='$package.app.tv.TvActivity'"
expect_badging "uses-feature-not-required: name='android.software.leanback'"
expect_badging "uses-feature-not-required: name='android.hardware.touchscreen'"

# A required feature (declared or implied, for example by a fixed orientation) hides the app
# from every device that lacks it.
required_features=$(echo "$badging" | grep "^ *uses-feature:" || true)
if [ -n "$required_features" ]; then
    fail "$apk: required features: $required_features"
fi

permissions=$(echo "$badging" | grep "^uses-permission:" || true)
unexpected_permissions=$(echo "$permissions" | grep -vF "name='$allowed_permission'" || true)
if [ -n "$unexpected_permissions" ]; then
    fail "$apk: unexpected permissions: $unexpected_permissions"
fi

# R8 renames resource files in release builds, so the banner is checked through the
# manifest's attribute rather than by file name.
manifest_tree=$("$build_tools/aapt2" dump xmltree --file AndroidManifest.xml "$apk")
if ! echo "$manifest_tree" | grep -q "android:banner"; then
    fail "$apk: application has no android:banner"
fi

if [ -f "$aab" ]; then
    echo "Checking $aab"
    check_native_files "$aab" "base/"
else
    fail "no bundle at $aab"
fi

if [ "$failures" -gt 0 ]; then
    echo "$failures check(s) failed" >&2
    exit 1
fi
echo "APK and bundle checks passed"
