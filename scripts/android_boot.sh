#!/bin/sh
# Usage: scripts/android_boot.sh <avd name> <console port>
#
# Starts an Android emulator unless one already answers on that port, then waits until
# Android reports it has finished booting, so the install and test steps that follow never
# race a half-started system. Animations are switched off because Compose UI tests and
# screenshots should not catch screens mid-transition.
#
# macOS has no `timeout` command, so the wait is a counted loop.
set -eu

if [ $# -ne 2 ]; then
    echo "usage: $0 <avd> <port>" >&2
    exit 2
fi
avd=$1
port=$2

sdk=${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}
adb="$sdk/platform-tools/adb"
emulator="$sdk/emulator/emulator"
serial="emulator-$port"
boot_log="${TMPDIR:-/tmp}/android_boot_$avd.log"

max_wait_seconds=240
poll_seconds=2

if "$adb" -s "$serial" get-state > /dev/null 2>&1; then
    echo "$serial is already running"
else
    echo "Starting $avd on $serial (log: $boot_log)"
    # -no-snapshot-save: every boot starts from the same clean state, so tests never see
    # leftovers from an earlier run's quick-boot snapshot.
    nohup "$emulator" -avd "$avd" -port "$port" -no-boot-anim -no-snapshot-save \
        > "$boot_log" 2>&1 &
fi

waited=0
booted=""
while [ "$waited" -lt "$max_wait_seconds" ]; do
    booted=$("$adb" -s "$serial" shell getprop sys.boot_completed 2> /dev/null | tr -d '\r' || true)
    if [ "$booted" = "1" ]; then
        break
    fi
    sleep "$poll_seconds"
    waited=$((waited + poll_seconds))
done

if [ "$booted" != "1" ]; then
    echo "$avd did not finish booting within $max_wait_seconds s; see $boot_log" >&2
    exit 1
fi

# Wake and unlock the screen (82 is KEYCODE_MENU, which dismisses the swipe lock).
"$adb" -s "$serial" shell input keyevent 82
"$adb" -s "$serial" shell settings put global window_animation_scale 0
"$adb" -s "$serial" shell settings put global transition_animation_scale 0
"$adb" -s "$serial" shell settings put global animator_duration_scale 0
echo "$avd is ready on $serial"
