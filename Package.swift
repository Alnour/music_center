// swift-tools-version: 6.0
// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

// Builds the answers generator only: `swift run --quiet ExpectedAnswers expected/answers.json`
// (or `make answers`). The app itself is built by Xcode from project.yml; this package
// compiles the app's UI-free Swift sources through the symlinks in scripts/ExpectedAnswers so
// the Android tests can check their Kotlin twins against what the Swift code really does.
// Swift 6 language mode implies complete strict concurrency, matching project.yml.

import PackageDescription

let package = Package(
    name: "ScalesAnswers",
    platforms: [.macOS(.v14)],
    targets: [
        .executableTarget(
            name: "ExpectedAnswers",
            path: "scripts/ExpectedAnswers",
            swiftSettings: [.swiftLanguageMode(.v6)]
        )
    ]
)
