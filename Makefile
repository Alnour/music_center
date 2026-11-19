# Self-contained build commands for Scales. The shared ios_apps/tools scripts locate apps
# inside the ios_apps folder by name, so this app (living in personal_projects) drives
# xcodebuild, simctl and devicectl directly with the same conventions.
NAME      := Scales
BUNDLE_ID := com.alnourkhalifa.Scales
SIM       ?= iPhone 17 Pro
DEVICE    ?=
DERIVED   := DerivedData
SIM_APP   := $(DERIVED)/Build/Products/Debug-iphonesimulator/$(NAME).app
DEVICE_APP := $(DERIVED)/Build/Products/Debug-iphoneos/$(NAME).app
PY        := python3 -I
# Swift that swift-format checks; the answers generator joins once it exists.
SWIFT_SOURCES := Scales ScalesTests ScalesUITests $(wildcard scripts/ExpectedAnswers Package.swift)

.PHONY: gen build test test-unit test-ui run run-device lint format clean screenshots \
	screenshots-ipad archive upload mac mac-test run-mac mac-dmg install-mac \
	answers answers-check parity mirror features verify verify-ui verify-all

# The included Android targets come first in the file, so name the default goal explicitly.
.DEFAULT_GOAL := gen

# The Android targets (android-setup, build-android, test-android, test-android-ui,
# run-android, run-android-tv, run-fire, lint-android, format-android, bundle-android,
# screenshots-android) live in their own file next to the Android build scripts.
-include scripts/android.mk

# Until that file exists, the Android steps of `make verify` report themselves skipped.
ifeq ($(wildcard scripts/android.mk),)
build-android test-android test-android-ui lint-android:
	@echo "$@: skipped, scripts/android.mk is not here yet"
endif

gen:
	xcodegen generate --quiet

build: gen
	xcodebuild -project $(NAME).xcodeproj -scheme $(NAME) \
	  -destination "platform=iOS Simulator,name=$(SIM)" \
	  -derivedDataPath $(DERIVED) -quiet build

test: gen
	xcodebuild -project $(NAME).xcodeproj -scheme $(NAME) \
	  -destination "platform=iOS Simulator,name=$(SIM)" \
	  -derivedDataPath $(DERIVED) -quiet test

# The two halves of `make test`: logic tests take seconds, interface tests minutes.
test-unit: gen
	xcodebuild -project $(NAME).xcodeproj -scheme $(NAME) \
	  -destination "platform=iOS Simulator,name=$(SIM)" \
	  -derivedDataPath $(DERIVED) -only-testing:ScalesTests -quiet test

test-ui: gen
	xcodebuild -project $(NAME).xcodeproj -scheme $(NAME) \
	  -destination "platform=iOS Simulator,name=$(SIM)" \
	  -derivedDataPath $(DERIVED) -only-testing:ScalesUITests -quiet test

run: build
	xcrun simctl boot "$(SIM)" 2>/dev/null || true
	# Xcode 27 replaced the Simulator app with DeviceHub; open whichever this Xcode has.
	open -a Simulator 2>/dev/null || open "$$(xcode-select -p)/../Applications/DeviceHub.app"
	xcrun simctl install "$(SIM)" $(SIM_APP)
	xcrun simctl launch "$(SIM)" $(BUNDLE_ID)

# make run-device DEVICE="Alnour's iPhone"   (name or UDID from: xcrun devicectl list devices)
run-device: gen
	@test -n "$(DEVICE)" || (echo 'set DEVICE="<iPhone name or UDID>"'; exit 1)
	xcodebuild -project $(NAME).xcodeproj -scheme $(NAME) \
	  -destination "platform=iOS,name=$(DEVICE)" \
	  -derivedDataPath $(DERIVED) -allowProvisioningUpdates \
	  -allowProvisioningDeviceRegistration -quiet build
	xcrun devicectl device install app --device "$(DEVICE)" $(DEVICE_APP)
	xcrun devicectl device process launch --device "$(DEVICE)" --terminate-existing $(BUNDLE_ID)

# The Mac version: this app built for macOS (Mac Catalyst). These local builds are signed
# for this Mac only ("-"), so nothing is registered with the developer account; the
# downloadable, Developer ID–signed copy comes from `make mac-dmg`.
MAC_DEST    := platform=macOS,variant=Mac Catalyst
MAC_SIGNING := CODE_SIGN_STYLE=Manual CODE_SIGN_IDENTITY=- DEVELOPMENT_TEAM= PROVISIONING_PROFILE_SPECIFIER=
MAC_APP     := $(DERIVED)/Build/Products/Debug-maccatalyst/$(NAME).app

mac: gen
	xcodebuild -project $(NAME).xcodeproj -scheme $(NAME) -destination "$(MAC_DEST)" \
	  -derivedDataPath $(DERIVED) $(MAC_SIGNING) -quiet build

# Unit tests only: the interface tests drive iPhone and iPad gestures.
mac-test: gen
	xcodebuild -project $(NAME).xcodeproj -scheme $(NAME) -destination "$(MAC_DEST)" \
	  -derivedDataPath $(DERIVED) $(MAC_SIGNING) -only-testing:ScalesTests -quiet test

run-mac: mac
	open $(MAC_APP)

# One downloadable Scales-<version>.dmg; see the script for signing and notarizing.
mac-dmg:
	scripts/build_mac_dmg.sh

# A Release copy in this Mac's /Applications, replacing the one there; run after each release.
install-mac:
	scripts/install_mac.sh

lint:
	xcrun swift-format lint --strict --recursive $(SWIFT_SOURCES)

format:
	xcrun swift-format format --in-place --recursive $(SWIFT_SOURCES)

# Cross-platform checks. expected/answers.json is written by the Swift code and read by the
# Kotlin tests, so both apps answer every music question the same way; answers-check fails
# when the committed file no longer matches what the Swift code writes.
answers:
	swift run --quiet ExpectedAnswers expected/answers.json

answers-check:
	@mkdir -p $(DERIVED)
	swift run --quiet ExpectedAnswers $(DERIVED)/answers.json
	@cmp -s expected/answers.json $(DERIVED)/answers.json || \
	  (echo "expected/answers.json is stale: run make answers and commit it"; exit 1)

# Every feature in features.json has a tagged test on each platform that owes it.
parity:
	$(PY) scripts/check_parity.py

# Every Swift logic file has a same-named Kotlin twin in android/core, and the reverse.
mirror:
	$(PY) scripts/check_mirror.py

# The feature registry as a table: which platform owes what, and by which wave.
features:
	$(PY) scripts/check_parity.py --features

# Cheapest first (~4 min warm): run before every commit and by .githooks/pre-push.
verify: parity mirror lint lint-android answers-check test-unit mac-test test-android build-android

# The interface tests on iPhone, iPad and the Android emulators (~13 min).
verify-ui:
	$(MAKE) test-ui
	$(MAKE) test-ui SIM="iPad Pro 13-inch (M5)"
	$(MAKE) test-android-ui

# Before each owner gate and each store upload.
verify-all: verify verify-ui

clean:
	rm -rf build DerivedData

# App Store screenshots: walks the app through its best states on the iPhone 17 Pro
# simulator (the 6.3-inch size the App Store Connect iPhone slot accepts, 1206x2622) and
# leaves the PNGs in Store/Screenshots/6.3-inch. `make screenshots-ipad` does the same on
# the iPad Pro 13-inch (the 13-inch iPad slot, 2064x2752) into Store/Screenshots/13-inch;
# the store asks for both because the app runs on iPhone and iPad.
SHOTS_SIM := iPhone 17 Pro
SHOTS_DIR := Store/Screenshots/6.3-inch
SHOTS_RESULT := $(DERIVED)/Screenshots.xcresult
screenshots: gen
	UDID=$$(xcrun simctl list devices available | grep "$(SHOTS_SIM) " | head -1 | grep -oE '[0-9A-F]{8}(-[0-9A-F]{4}){3}-[0-9A-F]{12}'); \
	xcrun simctl boot $$UDID 2>/dev/null || true; \
	xcrun simctl uninstall $$UDID $(BUNDLE_ID) 2>/dev/null || true; \
	xcrun simctl status_bar $$UDID override --time 9:41 --batteryLevel 100 \
	  --batteryState charged --wifiBars 3 --cellularBars 4 --operatorName ""; \
	xcrun simctl ui $$UDID appearance light
	rm -rf $(SHOTS_RESULT) $(SHOTS_DIR) && mkdir -p $(SHOTS_DIR)
	xcodebuild -project $(NAME).xcodeproj -scheme $(NAME) \
	  -destination "platform=iOS Simulator,name=$(SHOTS_SIM)" \
	  -derivedDataPath $(DERIVED) -resultBundlePath $(SHOTS_RESULT) \
	  -only-testing:ScalesUITests/StoreScreenshots -quiet test
	xcrun xcresulttool export attachments --path $(SHOTS_RESULT) --output-path $(SHOTS_DIR)
	cd $(SHOTS_DIR) && python3 -I -c 'import json, os; [os.rename(a["exportedFileName"], a["suggestedHumanReadableName"].split("_")[0] + ".png") for t in json.load(open("manifest.json")) for a in t.get("attachments", [])]' && rm manifest.json
	sips -r 270 $(SHOTS_DIR)/04-tv-board-landscape.png >/dev/null && python3 -I Store/Screenshots/strip_png_orientation.py $(SHOTS_DIR)/04-tv-board-landscape.png

screenshots-ipad:
	$(MAKE) screenshots SHOTS_SIM="iPad Pro 13-inch (M5)" SHOTS_DIR=Store/Screenshots/13-inch

# App Store release. `make archive` builds a signed release archive for a real iPhone;
# `make upload` sends it to App Store Connect with the Apple ID signed into Xcode
# (Xcode › Settings › Accounts), creating the distribution certificate on first use.
ARCHIVE := $(DERIVED)/$(NAME).xcarchive
archive: gen
	xcodebuild -project $(NAME).xcodeproj -scheme $(NAME) -configuration Release \
	  -destination "generic/platform=iOS" -archivePath $(ARCHIVE) \
	  -allowProvisioningUpdates -quiet archive

upload: archive
	xcodebuild -exportArchive -archivePath $(ARCHIVE) \
	  -exportOptionsPlist Store/ExportOptions.plist \
	  -exportPath $(DERIVED)/Export -allowProvisioningUpdates
