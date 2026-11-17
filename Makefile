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

.PHONY: gen build test run run-device lint format clean screenshots screenshots-ipad archive upload \
	mac mac-test run-mac mac-dmg install-mac

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
	xcrun swift-format lint --strict --recursive Scales ScalesTests

format:
	xcrun swift-format format --in-place --recursive Scales ScalesTests

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
