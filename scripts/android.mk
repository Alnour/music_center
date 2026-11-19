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

.PHONY: android-setup build-android test-android test-android-ui run-android run-android-tv \
	run-fire lint-android format-android bundle-android screenshots-android android-bump \
	android-release-notes

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
# Each emulator runs only its own interface tests: a D-pad walk means nothing on a phone and
# a touch flow nothing on a TV, and Gradle would otherwise run every test on every device.
TEST_ARGS := -Pandroid.testInstrumentationRunnerArguments.package
test-android-ui:
	scripts/android_boot.sh $(PHONE_AVD) $(PHONE_PORT)
	scripts/android_boot.sh $(TV_AVD) $(TV_PORT)
	cd android && ANDROID_SERIAL=emulator-$(PHONE_PORT) ./gradlew --console=plain connectedDebugAndroidTest $(TEST_ARGS)=$(ANDROID_ID).app.phone
	cd android && ANDROID_SERIAL=emulator-$(TV_PORT) ./gradlew --console=plain connectedDebugAndroidTest $(TEST_ARGS)=$(ANDROID_ID).app.tv

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

# The store build: an AAB for Play and an APK for Amazon, both shrunk by R8, then checked the
# way TV reviewers check them. `parity --release` runs first because a feature still marked
# `later` must not reach a store. Without the upload key (only on the owner's Mac, in
# ~/.gradle/gradle.properties) the APK is app-release-unsigned.apk; the checks run either way.
# Agents never upload: the owner uploads both files by hand (Store/android/listing.md).
RELEASE_OUTPUTS := android/app/build/outputs
bundle-android:
	python3 -I scripts/check_parity.py --release
	$(GRADLE) bundleRelease assembleRelease
	scripts/check_apk.sh
	@ls -l $(RELEASE_OUTPUTS)/bundle/release/*.aab $(RELEASE_OUTPUTS)/apk/release/*.apk

# versionCode + 1 in android/version.properties before each upload: both stores refuse a
# versionCode they have already seen.
android-bump:
	scripts/bump_android_version.sh

# The commit subjects that touched the Android app since a tag or commit, as a start for the
# Play "What's new" text (500 characters at most) and Amazon's release notes:
#   make android-release-notes SINCE=<tag or commit of the last upload>
SINCE ?= main
android-release-notes:
	@git log --no-merges --format='- %s' $(SINCE)..HEAD -- android features.json

# Store screenshots. The instrumented tests StoreScreenshots (phone and tablet) and
# TvStoreScreenshots (TV) walk the app through its listing states and save PNGs in the app's
# external files folder; this target runs them on each emulator and pulls the pictures into
# Store/android/screenshots/<device>/. `ANDROID_SHOTS=tablet` runs one device only.
#
# The APKs are installed and the class run with `am instrument` rather than through Gradle's
# connected task, because that task uninstalls the app afterwards and the pictures with it.
# The status bar is put in demo mode first (9:41, full battery and Wi-Fi, no notifications)
# so the pictures carry no stray icons, the light theme is chosen, as for the iOS store
# pictures, and the system's one-time "Viewing full screen" hint is marked as seen so it
# does not cover the full-screen board.
ANDROID_SHOTS    ?= phone tablet tv
STORE_SHOTS_DIR  := Store/android/screenshots
TEST_APK         := android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
TEST_RUNNER      := $(ANDROID_ID).test/androidx.test.runner.AndroidJUnitRunner
DEVICE_SHOTS_DIR := /sdcard/Android/data/$(ANDROID_ID)/files/store-screenshots
PHONE_SHOTS      := $(ANDROID_ID).app.phone.StoreScreenshots
TV_SHOTS         := $(ANDROID_ID).app.tv.TvStoreScreenshots
DEMO             := shell am broadcast -a com.android.systemui.demo -e command

# $(call store_shots,<console port>,<test class>,<folder under Store/android/screenshots>)
# `am instrument` exits 0 even when a test fails, so its output is searched for JUnit's
# "OK (" line; demo mode is left whether the test passed or not.
define store_shots
	$(ADB) -s emulator-$(1) install -r $(DEBUG_APK)
	$(ADB) -s emulator-$(1) install -r $(TEST_APK)
	$(ADB) -s emulator-$(1) shell 'rm -f $(DEVICE_SHOTS_DIR)/*.png'
	$(ADB) -s emulator-$(1) shell cmd uimode night no
	$(ADB) -s emulator-$(1) shell settings put secure immersive_mode_confirmations confirmed
	$(ADB) -s emulator-$(1) shell settings put global sysui_demo_allowed 1
	$(ADB) -s emulator-$(1) $(DEMO) enter > /dev/null
	$(ADB) -s emulator-$(1) $(DEMO) clock -e hhmm 0941 > /dev/null
	$(ADB) -s emulator-$(1) $(DEMO) battery -e level 100 -e plugged false > /dev/null
	$(ADB) -s emulator-$(1) $(DEMO) network -e wifi show -e level 4 -e fully true \
	  -e mobile hide > /dev/null
	$(ADB) -s emulator-$(1) $(DEMO) notifications -e visible false > /dev/null
	output=$$($(ADB) -s emulator-$(1) shell am instrument -w -e class $(2) $(TEST_RUNNER)); \
	  status=$$?; \
	  $(ADB) -s emulator-$(1) $(DEMO) exit > /dev/null; \
	  echo "$$output"; \
	  test $$status -eq 0 && echo "$$output" | grep -q '^OK ('
	mkdir -p $(STORE_SHOTS_DIR)/$(3)
	rm -f $(STORE_SHOTS_DIR)/$(3)/*.png
	$(ADB) -s emulator-$(1) pull $(DEVICE_SHOTS_DIR)/. $(STORE_SHOTS_DIR)/$(3)
	@echo "$(3) screenshots in $(STORE_SHOTS_DIR)/$(3)"
endef

screenshots-android:
	$(GRADLE) assembleDebug assembleDebugAndroidTest
ifneq ($(filter phone,$(ANDROID_SHOTS)),)
	scripts/android_boot.sh $(PHONE_AVD) $(PHONE_PORT)
	$(call store_shots,$(PHONE_PORT),$(PHONE_SHOTS),phone)
endif
ifneq ($(filter tablet,$(ANDROID_SHOTS)),)
	scripts/android_boot.sh $(TABLET_AVD) $(TABLET_PORT)
	$(call store_shots,$(TABLET_PORT),$(PHONE_SHOTS),tablet)
endif
ifneq ($(filter tv,$(ANDROID_SHOTS)),)
	scripts/android_boot.sh $(TV_AVD) $(TV_PORT)
	$(call store_shots,$(TV_PORT),$(TV_SHOTS),tv)
endif
