// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import OSLog
import SwiftUI
import UIKit

/// Owns the window on the TV while AirPlay screen mirroring is active.
///
/// Connect and disconnect events are logged under the "TV" category, so a mirroring
/// problem on a real Roku can be diagnosed from Console.app on the Mac.
final class TVSceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?
    private let logger = Logger(subsystem: "com.alnourkhalifa.Scales", category: "TV")

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = scene as? UIWindowScene else { return }
        let practiceSession = PracticeSession.shared
        let tvView = TVDisplayView().environment(practiceSession)
        let window = UIWindow(windowScene: windowScene)
        window.rootViewController = UIHostingController(rootView: tvView)
        window.isHidden = false
        self.window = window
        practiceSession.isTVConnected = true
        logger.notice("TV scene connected")
        fitWindow(to: windowScene)
    }

    /// The TV's size can change while connected (a resolution renegotiation or a change in
    /// the TV's display settings), so the window follows the scene whenever it does.
    func windowScene(
        _ windowScene: UIWindowScene,
        didUpdateEffectiveGeometry previousEffectiveGeometry: UIWindowScene.Geometry
    ) {
        fitWindow(to: windowScene)
    }

    func sceneDidDisconnect(_ scene: UIScene) {
        window = nil
        PracticeSession.shared.isTVConnected = false
        logger.notice("TV scene disconnected")
    }

    private func fitWindow(to windowScene: UIWindowScene) {
        let bounds = windowScene.effectiveGeometry.coordinateSpace.bounds
        window?.frame = bounds
        logger.notice(
            "TV window sized to \(bounds.width, privacy: .public)×\(bounds.height, privacy: .public) points"
        )
    }
}
