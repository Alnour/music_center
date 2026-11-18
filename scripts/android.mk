# Android build commands for Scales. The root Makefile pulls this file in with
# `-include scripts/android.mk`; it also runs on its own: `make -f scripts/android.mk <target>`.
# Every target runs from the repository root. Variables carry an ANDROID_ (or similar) prefix
# so they never collide with the iOS variables in the root Makefile.

# Homebrew's command-line tools and the keg-only JDK 17 (AGP 9.5's minimum). `?=` lets CI
# and other Macs point elsewhere; `export` hands both to Gradle, sdkmanager and avdmanager.
ANDROID_HOME ?= /opt/homebrew/share/android-commandlinetools
JAVA_HOME    ?= /opt/homebrew/opt/openjdk@17
export ANDROID_HOME JAVA_HOME

GRADLE   := cd android && ./gradlew --console=plain
SDK_BIN  := $(ANDROID_HOME)/cmdline-tools/latest/bin
ADB      := $(ANDROID_HOME)/platform-tools/adb
EMULATOR := $(ANDROID_HOME)/emulator/emulator

ANDROID_ID     := com.alnourkhalifa.scales
PHONE_ACTIVITY := .app.phone.PhoneActivity
TV_ACTIVITY    := .app.tv.TvActivity
DEBUG_APK      := android/app/build/outputs/apk/debug/app-debug.apk

# Emulators. Each runs on its own console port, so `adb -s emulator-<port>` names it.
PHONE_AVD   := scales_phone
PHONE_PORT  := 5554
TABLET_AVD  := scales_tablet
TABLET_PORT := 5558
TV_AVD      := scales_tv
TV_PORT     := 5556
FLOOR_AVD   := scales_floor
FLOOR_PORT  := 5560

# All arm64, the only images that run at speed on Apple silicon. The Android 9 image stands
# in for Fire OS 7 (Android 9), the oldest system the app supports (minSdk 28).
IMAGE_PHONE := system-images;android-36;google_apis;arm64-v8a
IMAGE_TV    := system-images;android-36;google-tv;arm64-v8a
IMAGE_FLOOR := system-images;android-28;google_apis;arm64-v8a

# make run-fire FIRE=192.168.1.40   (the stick needs ADB debugging turned on)
FIRE ?=
ANDROID_SHOTS_DIR ?= DerivedData/AndroidScreenshots

.PHONY: android-setup build-android test-android test-android-ui run-android run-android-tv \
	run-fire lint-android format-android bundle-android screenshots-android

# One-time SDK setup (~4.2 GB). compileSdk 37 is not on a fresh Mac, so this runs before the
# first build. An AVD is created only when absent, so a rerun never wipes an emulator's data.
android-setup:
	yes | $(SDK_BIN)/sdkmanager --licenses > /dev/null
	$(SDK_BIN)/sdkmanager --install "platforms;android-37.0" "build-tools;36.0.0" \
	  "platform-tools" "emulator" "$(IMAGE_PHONE)" "$(IMAGE_TV)" "$(IMAGE_FLOOR)"
	@$(SDK_BIN)/avdmanager list avd -c | grep -qx $(PHONE_AVD) || \
	  echo no | $(SDK_BIN)/avdmanager create avd -n $(PHONE_AVD) -k "$(IMAGE_PHONE)" -d pixel_8
	@$(SDK_BIN)/avdmanager list avd -c | grep -qx $(TABLET_AVD) || \
	  echo no | $(SDK_BIN)/avdmanager create avd -n $(TABLET_AVD) -k "$(IMAGE_PHONE)" -d pixel_tablet
	@$(SDK_BIN)/avdmanager list avd -c | grep -qx $(TV_AVD) || \
	  echo no | $(SDK_BIN)/avdmanager create avd -n $(TV_AVD) -k "$(IMAGE_TV)" -d tv_1080p
	@$(SDK_BIN)/avdmanager list avd -c | grep -qx $(FLOOR_AVD) || \
	  echo no | $(SDK_BIN)/avdmanager create avd -n $(FLOOR_AVD) -k "$(IMAGE_FLOOR)" -d tv_1080p
	$(SDK_BIN)/avdmanager list avd -c

build-android:
	$(GRADLE) assembleDebug

# JVM tests only (no emulator): the pure-Kotlin core and the app's unit tests.
test-android:
	$(GRADLE) :core:test :app:testDebugUnitTest

# Compose interface tests on the phone and TV emulators, booted first if needed.
test-android-ui:
	scripts/android_boot.sh $(PHONE_AVD) $(PHONE_PORT)
	scripts/android_boot.sh $(TV_AVD) $(TV_PORT)
	$(GRADLE) connectedDebugAndroidTest

run-android: build-android
	scripts/android_boot.sh $(PHONE_AVD) $(PHONE_PORT)
	$(ADB) -s emulator-$(PHONE_PORT) install -r $(DEBUG_APK)
	$(ADB) -s emulator-$(PHONE_PORT) shell am start -n $(ANDROID_ID)/$(PHONE_ACTIVITY)

run-android-tv: build-android
	scripts/android_boot.sh $(TV_AVD) $(TV_PORT)
	$(ADB) -s emulator-$(TV_PORT) install -r $(DEBUG_APK)
	$(ADB) -s emulator-$(TV_PORT) shell am start -n $(ANDROID_ID)/$(TV_ACTIVITY)

run-fire: build-android
	@test -n "$(FIRE)" || (echo 'set FIRE=<Fire TV IP address>'; exit 1)
	$(ADB) connect $(FIRE):5555
	$(ADB) -s $(FIRE):5555 install -r $(DEBUG_APK)
	$(ADB) -s $(FIRE):5555 shell am start -n $(ANDROID_ID)/$(TV_ACTIVITY)

# ktlint and detekt run as Gradle plugins (Homebrew's detekt cannot parse Kotlin 2.4). The
# greps keep phone Material and TV Material apart: mixing them in one call path breaks focus.
lint-android:
	$(GRADLE) ktlintCheck detekt :app:lintDebug
	@! grep -rln "androidx\.tv" android/app/src/*/kotlin/com/alnourkhalifa/scales/app/phone \
	  || (echo "app/phone must not use androidx.tv"; exit 1)
	@! grep -rln "androidx\.compose\.material3" android/app/src/*/kotlin/com/alnourkhalifa/scales/app/tv \
	  || (echo "app/tv must not use androidx.compose.material3"; exit 1)

format-android:
	$(GRADLE) ktlintFormat

bundle-android:
	$(GRADLE) bundleRelease assembleRelease
	scripts/check_apk.sh

# Launch screens of the phone and TV emulators, for side-by-side review with the iOS shots.
screenshots-android: build-android
	mkdir -p $(ANDROID_SHOTS_DIR)
	scripts/android_boot.sh $(PHONE_AVD) $(PHONE_PORT)
	$(ADB) -s emulator-$(PHONE_PORT) install -r $(DEBUG_APK)
	$(ADB) -s emulator-$(PHONE_PORT) shell am start -W -n $(ANDROID_ID)/$(PHONE_ACTIVITY)
	$(ADB) -s emulator-$(PHONE_PORT) exec-out screencap -p > $(ANDROID_SHOTS_DIR)/phone.png
	scripts/android_boot.sh $(TV_AVD) $(TV_PORT)
	$(ADB) -s emulator-$(TV_PORT) install -r $(DEBUG_APK)
	$(ADB) -s emulator-$(TV_PORT) shell am start -W -n $(ANDROID_ID)/$(TV_ACTIVITY)
	$(ADB) -s emulator-$(TV_PORT) exec-out screencap -p > $(ANDROID_SHOTS_DIR)/tv.png
	@echo "Screenshots in $(ANDROID_SHOTS_DIR)"
