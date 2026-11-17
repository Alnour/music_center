// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import AVFoundation
import Foundation
import Observation

/// Runs the drum trainer: the tempo, the groove, and the clock the ball follows.
///
/// Sound and picture are started together. The bar is rendered once at the chosen tempo
/// and looped by the audio engine, while the view reads the start date and works out
/// where the ball is from the wall clock, so both agree on where beat one falls. Any
/// change while playing (tempo, groove, sound on or off) restarts both from beat one, which
/// keeps them in step without any cross-clock bookkeeping.
@MainActor
@Observable
final class DrumPlayer {
    static let tempoRange: ClosedRange<Double> = 40...220

    var groove: DrumGroove = DrumGroove.builtIn[0] {
        didSet { restartIfPlaying() }
    }

    var tempo: Double = 90 {
        didSet { restartIfPlaying() }
    }

    var isSoundOn = true {
        didSet { restartIfPlaying() }
    }

    /// When the current run began; nil while stopped.
    private(set) var startDate: Date?

    var isPlaying: Bool { startDate != nil }

    @ObservationIgnored private let engine = AVAudioEngine()
    @ObservationIgnored private let node = AVAudioPlayerNode()
    @ObservationIgnored private var isEngineWired = false

    func togglePlayback() {
        if isPlaying {
            stop()
        } else {
            start()
        }
    }

    /// The ball at this instant, or resting on beat one while stopped.
    func ballPosition(at date: Date) -> BallPosition {
        guard let startDate else {
            return BallPosition(step: 0, travel: 0, bounce: 0)
        }
        let progress = DrumClock.barProgress(
            elapsed: date.timeIntervalSince(startDate),
            tempo: tempo,
            beatsPerBar: groove.beatsPerBar
        )
        return DrumClock.ballPosition(progress: progress, stepsPerBar: groove.stepsPerBar)
    }

    func start() {
        stop()
        if isSoundOn {
            startSound()
        }
        startDate = Date()
    }

    func stop() {
        startDate = nil
        node.stop()
        engine.stop()
    }

    private func restartIfPlaying() {
        if isPlaying {
            start()
        }
    }

    private func startSound() {
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playback, mode: .default)
        try? session.setActive(true)

        let format = node.outputFormat(forBus: 0)
        let sampleRate = format.sampleRate > 0 ? format.sampleRate : 44100
        guard
            let monoFormat = AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 1),
            let buffer = loopBuffer(format: monoFormat)
        else { return }

        if !isEngineWired {
            engine.attach(node)
            isEngineWired = true
        }
        engine.connect(node, to: engine.mainMixerNode, format: monoFormat)
        do {
            try engine.start()
        } catch {
            return
        }
        node.scheduleBuffer(buffer, at: nil, options: .loops)
        node.play()
    }

    private func loopBuffer(format: AVAudioFormat) -> AVAudioPCMBuffer? {
        let renderer = DrumBarRenderer(sampleRate: format.sampleRate)
        let samples = renderer.render(groove, tempo: tempo)
        let frameCount = AVAudioFrameCount(samples.count)
        guard
            let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: frameCount),
            let channel = buffer.floatChannelData?[0]
        else { return nil }
        for (index, sample) in samples.enumerated() {
            channel[index] = sample
        }
        buffer.frameLength = frameCount
        return buffer
    }
}
