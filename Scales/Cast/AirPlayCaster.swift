// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import AVFoundation
import CoreGraphics
import Foundation
import OSLog
import Observation
import UIKit

/// Sends the board to a TV as AirPlay video, the way video apps do.
///
/// The player loops a clip of the current board, fetched from the phone's own clip
/// server. When the player picks a TV in the AirPlay menu, iOS plays that clip on the TV
/// ("external playback") and the phone stays free for the controls. Every scale change
/// writes a new clip and swaps it in. The app ships with a ready-made G major clip so
/// there is something to cast from the first second, before any clip has been written.
@MainActor
@Observable
final class AirPlayCaster: BoardCaster {
    /// The one caster the app uses: the session reads its status, and the TV menu and the
    /// Scales screen drive it. One instance because it owns the player and the clip server.
    static let shared = AirPlayCaster()

    /// True while the clip is playing on a TV rather than on the phone.
    private(set) var isPlayingOnTV = false
    /// The TV's name as AirPlay reports it, for the status line.
    private(set) var receiverName: String?
    /// Set when the player gave up on the clip, so the status line can say so.
    private(set) var playbackError: String?

    let player: AVPlayer
    private let server = ClipServer()
    @ObservationIgnored private var observers: [NSObjectProtocol] = []
    @ObservationIgnored private var playbackObservation: NSKeyValueObservation?
    @ObservationIgnored private var itemObservation: NSKeyValueObservation?
    @ObservationIgnored private var renderTask: Task<Void, Never>?
    @ObservationIgnored private var pendingClip: Data?
    private let logger = Logger(subsystem: "com.alnourkhalifa.Scales", category: "AirPlay")

    init() {
        player = AVPlayer()
        player.allowsExternalPlayback = true
        player.usesExternalPlaybackWhileExternalScreenIsActive = true
        player.preventsDisplaySleepDuringVideoPlayback = true
        player.actionAtItemEnd = .none
        configureAudioSession()
        observePlayer()
        server.start()
    }

    /// Plays the board for these settings. A cached clip plays at once; otherwise a short
    /// clip is taken from the app bundle (default G major) or drawn and encoded, then
    /// extended to five minutes, stored, and played. The newest request wins, so rapid
    /// changes don't queue up stale boards.
    func cast(_ settings: PracticeSettings, drawBoard: () -> CGImage?) {
        renderTask?.cancel()
        let name = "\(ClipCache.key(for: settings)).mp4"
        if let clip = ClipCache.clip(for: settings) {
            play(clip: clip, named: name)
            return
        }

        let bundledShortClip = ClipCache.bundledShortClip(for: settings)
        let boardImage = bundledShortClip == nil ? drawBoard() : nil
        guard bundledShortClip != nil || boardImage != nil else { return }
        let scratch = FileManager.default.temporaryDirectory
        let shortURL = scratch.appendingPathComponent("short-\(name)")
        let longURL = scratch.appendingPathComponent("long-\(name)")

        renderTask = Task { [weak self] in
            let clip: Data? = await Task.detached(priority: .userInitiated) {
                do {
                    var source = shortURL
                    if let bundledShortClip {
                        source = bundledShortClip
                    } else if let boardImage {
                        try BoardVideoWriter.write(boardImage, to: shortURL)
                    }
                    try await ClipExtender.extend(source, to: longURL)
                    let data = try Data(contentsOf: longURL)
                    try? FileManager.default.removeItem(at: shortURL)
                    try? FileManager.default.removeItem(at: longURL)
                    return data
                } catch {
                    return nil
                }
            }.value
            guard let clip else {
                self?.logger.error("Making \(name, privacy: .public) failed")
                return
            }
            ClipCache.store(clip, for: settings)
            guard !Task.isCancelled, let self else { return }
            self.play(clip: clip, named: name)
        }
    }

    /// Starts the clip again on the TV after the TV stopped it or another app took it.
    /// Picking the TV again in the AirPlay menu does the same automatically.
    func resume() {
        reclaimPlayback(reason: "Resume tapped")
    }

    /// Called when the AirPlay menu closes. If another app was already streaming to the
    /// TV the player picked, iOS reports no route change (the route was that TV all
    /// along), so the menu closing is the only signal that the player wants the board
    /// there now. The TV is taken over whenever an AirPlay receiver is in the route.
    func takeOverTVIfChosen() {
        refreshReceiverName()
        guard receiverName != nil else { return }
        reclaimPlayback(reason: "TV chosen in the AirPlay menu")
    }

    /// Publishes the clip on the server and points the player at it. If the server has
    /// no address yet (it comes up a moment after launch) the clip waits for it.
    private func play(clip: Data, named name: String) {
        guard let url = server.publish(clip, named: name) else {
            pendingClip = clip
            logger.notice("Server not ready; holding \(name, privacy: .public)")
            retryPendingClipSoon(named: name)
            return
        }
        pendingClip = nil
        playbackError = nil
        let item = AVPlayerItem(url: url)
        observeFailures(of: item)
        player.replaceCurrentItem(with: item)
        player.play()
        logger.notice("Playing \(url.absoluteString, privacy: .public)")
    }

    private func retryPendingClipSoon(named name: String) {
        Task { [weak self] in
            try? await Task.sleep(for: .milliseconds(500))
            guard let self, let clip = self.pendingClip else { return }
            self.play(clip: clip, named: name)
        }
    }

    /// AirPlay routing follows the audio session, so it has to be a playback session.
    private func configureAudioSession() {
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playback, mode: .moviePlayback)
        try? session.setActive(true)
    }

    private func observePlayer() {
        playbackObservation = player.observe(\.isExternalPlaybackActive, options: [.initial]) {
            [weak self] player, _ in
            let isActive = player.isExternalPlaybackActive
            Task { @MainActor [weak self] in
                self?.isPlayingOnTV = isActive
                self?.logger.notice("External playback active: \(isActive, privacy: .public)")
            }
        }
        let center = NotificationCenter.default
        observers.append(
            center.addObserver(
                forName: AVAudioSession.routeChangeNotification,
                object: nil,
                queue: .main
            ) { [weak self] _ in
                MainActor.assumeIsolated { self?.routeChanged() }
            }
        )
        observers.append(
            center.addObserver(
                forName: AVPlayerItem.didPlayToEndTimeNotification,
                object: nil,
                queue: .main
            ) { [weak self] notification in
                // Only plain values cross to the main actor; the notification itself can't.
                let finishedItem = (notification.object as AnyObject?).map(ObjectIdentifier.init)
                MainActor.assumeIsolated { self?.loop(afterItem: finishedItem) }
            }
        )
        observers.append(
            center.addObserver(
                forName: AVPlayerItem.failedToPlayToEndTimeNotification,
                object: nil,
                queue: .main
            ) { [weak self] notification in
                let key = AVPlayerItemFailedToPlayToEndTimeErrorKey
                let error = notification.userInfo?[key] as? Error
                let message = error?.localizedDescription ?? "unknown error"
                MainActor.assumeIsolated { self?.recordFailure(message) }
            }
        )
        observers.append(
            center.addObserver(
                forName: UIApplication.didEnterBackgroundNotification,
                object: nil,
                queue: .main
            ) { [weak self] _ in
                MainActor.assumeIsolated { self?.keepPlayingInBackground() }
            }
        )
        observers.append(
            center.addObserver(
                forName: UIApplication.willEnterForegroundNotification,
                object: nil,
                queue: .main
            ) { [weak self] _ in
                MainActor.assumeIsolated { self?.reclaimTVOnReturn() }
            }
        )
        observers.append(
            center.addObserver(
                forName: AVAudioSession.interruptionNotification,
                object: nil,
                queue: .main
            ) { [weak self] notification in
                // Only plain values cross to the main actor; the notification itself can't.
                let info = notification.userInfo
                let rawType = info?[AVAudioSessionInterruptionTypeKey] as? UInt
                let type = rawType.flatMap(AVAudioSession.InterruptionType.init)
                let rawOptions = info?[AVAudioSessionInterruptionOptionKey] as? UInt ?? 0
                let options = AVAudioSession.InterruptionOptions(rawValue: rawOptions)
                let canResume = options.contains(.shouldResume)
                MainActor.assumeIsolated {
                    self?.audioSessionInterrupted(type, canResume: canResume)
                }
            }
        )
        refreshReceiverName()
    }

    /// Takes the TV back when the player returns to the app after another app streamed
    /// to it (a video, a music app). Coming back to Scales is read as wanting the board
    /// on the TV, so the other app's stream is interrupted in turn.
    private func reclaimTVOnReturn() {
        guard player.currentItem != nil, player.timeControlStatus != .playing else { return }
        reclaimPlayback(reason: "Returned to the app while paused")
    }

    /// Another app's non-mixable audio (any video or music app) interrupts this app's
    /// session, which pauses the player and hands the TV to that app. When that app ends
    /// its session and says others may resume, the board is put back on the TV. An app
    /// that keeps streaming never ends the interruption; the return to the foreground
    /// and the Resume button cover that case.
    private func audioSessionInterrupted(
        _ type: AVAudioSession.InterruptionType?,
        canResume: Bool
    ) {
        switch type {
        case .began:
            logger.notice("Audio session interrupted; another app took the stream")
        case .ended:
            guard canResume else { return }
            reclaimPlayback(reason: "Interruption ended")
        default:
            break
        }
    }

    /// iOS gives the AirPlay picture to whichever app's player is playing, but a player
    /// paused by an interruption sends nothing until its audio session is active again.
    /// So the session is reactivated first, which interrupts the other app, and only then
    /// is playback restarted.
    private func reclaimPlayback(reason: String) {
        do {
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            let message = error.localizedDescription
            logger.error("Could not reactivate the audio session: \(message, privacy: .public)")
        }
        refreshReceiverName()
        let holdsNoPicture =
            player.timeControlStatus == .playing && !player.isExternalPlaybackActive
        if receiverName != nil, holdsNoPicture {
            // A player that kept running while another app held the AirPlay picture does
            // not reclaim it by itself; restarting playback makes iOS hand it over.
            player.pause()
        }
        player.play()
        logger.notice("\(reason, privacy: .public); playing the board again")
    }

    /// If the board is on the TV when the player leaves the app, it stays there: iOS may
    /// pause video as the app moves to the background, so playback is nudged back on.
    private func keepPlayingInBackground() {
        guard receiverName != nil else { return }
        logger.notice("App in background; keeping the board playing on the TV")
        player.play()
    }

    /// Restarts the clip when it ends. A plain player is used instead of a looping queue
    /// because queued items are not carried over to AirPlay video reliably.
    private func loop(afterItem finishedItem: ObjectIdentifier?) {
        let currentItem = player.currentItem.map(ObjectIdentifier.init)
        guard let finishedItem, finishedItem == currentItem else { return }
        player.seek(to: .zero)
        player.play()
    }

    private func observeFailures(of item: AVPlayerItem) {
        itemObservation = item.observe(\.status) { [weak self] item, _ in
            guard item.status == .failed else { return }
            let message = item.error?.localizedDescription ?? "unknown error"
            Task { @MainActor [weak self] in
                self?.playbackError = message
                self?.logger.error("Item failed: \(message, privacy: .public)")
            }
        }
    }

    private func recordFailure(_ message: String) {
        playbackError = message
        logger.error("Failed to play to end: \(message, privacy: .public)")
    }

    /// When the TV drops the session, iOS moves playback back to the phone and pauses it.
    /// Picking the TV again reconnects, but a paused player would send nothing, and a
    /// player interrupted by another app's stream holds no picture, so the clip is
    /// restarted whenever an AirPlay destination is in the route and the board is not
    /// already showing there.
    private func routeChanged() {
        refreshReceiverName()
        guard receiverName != nil, !player.isExternalPlaybackActive else { return }
        reclaimPlayback(reason: "AirPlay destination chosen")
    }

    private func refreshReceiverName() {
        let outputs = AVAudioSession.sharedInstance().currentRoute.outputs
        var airPlayName: String?
        for output in outputs where output.portType == .airPlay {
            airPlayName = output.portName
        }
        receiverName = airPlayName
    }
}
