// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import Foundation

// The drum trainer's sections: the grooves and the clock that moves the ball, then the
// synthesized bar itself, sampled finely enough that a Kotlin renderer that drifts by one
// sample or one formula shows up.

// MARK: - drums

struct DrumHitAnswer: Encodable {
    /// The `DrumVoice` case name: "hiHat", "snare" or "kick".
    let voice: String
    let step: Int
}

struct GrooveAnswer: Encodable {
    let name: String
    let beatsPerBar: Int
    let subdivision: Int
    let stepsPerBar: Int
    /// In the order the groove lists them.
    let hits: [DrumHitAnswer]
    let countLabels: [String]
    /// The voices struck on each step, in `DrumVoice.allCases` order.
    let voicesPerStep: [[String]]
}

struct VoiceAnswer: Encodable {
    let id: String
    let name: String
}

struct BarDurationAnswer: Encodable {
    let tempo: Double
    let beatsPerBar: Int
    let seconds: Double
}

struct BarProgressAnswer: Encodable {
    let elapsed: Double
    let tempo: Double
    let beatsPerBar: Int
    let progress: Double
}

struct BallAnswer: Encodable {
    /// k / 64 for k in 0...64.
    let progress: Double
    let step: Int
    let travel: Double
    let bounce: Double
}

struct BallTrackAnswer: Encodable {
    let stepsPerBar: Int
    let positions: [BallAnswer]
}

struct DrumsSection: Encodable {
    let voices: [VoiceAnswer]
    let grooves: [GrooveAnswer]
    let barDurations: [BarDurationAnswer]
    let barProgress: [BarProgressAnswer]
    let ballTracks: [BallTrackAnswer]
}

func voiceID(_ voice: DrumVoice) -> String {
    String(describing: voice)
}

func grooveAnswer(_ groove: DrumGroove) -> GrooveAnswer {
    var hits: [DrumHitAnswer] = []
    for hit in groove.hits {
        hits.append(DrumHitAnswer(voice: voiceID(hit.voice), step: hit.step))
    }
    var countLabels: [String] = []
    var voicesPerStep: [[String]] = []
    for step in 0..<groove.stepsPerBar {
        countLabels.append(groove.countLabel(forStep: step))
        let struck = groove.voices(atStep: step)
        var voices: [String] = []
        for voice in DrumVoice.allCases where struck.contains(voice) {
            voices.append(voiceID(voice))
        }
        voicesPerStep.append(voices)
    }
    return GrooveAnswer(
        name: groove.name,
        beatsPerBar: groove.beatsPerBar,
        subdivision: groove.subdivision,
        stepsPerBar: groove.stepsPerBar,
        hits: hits,
        countLabels: countLabels,
        voicesPerStep: voicesPerStep
    )
}

func drumsSection() -> DrumsSection {
    var voices: [VoiceAnswer] = []
    for voice in DrumVoice.allCases {
        voices.append(VoiceAnswer(id: voiceID(voice), name: voice.name))
    }
    var grooves: [GrooveAnswer] = []
    for groove in DrumGroove.builtIn {
        grooves.append(grooveAnswer(groove))
    }
    var durations: [BarDurationAnswer] = []
    for tempo in [40.0, 90.0, 120.0, 220.0] {
        let seconds = DrumClock.barDuration(tempo: tempo, beatsPerBar: 4)
        durations.append(BarDurationAnswer(tempo: tempo, beatsPerBar: 4, seconds: rounded(seconds)))
    }
    var progress: [BarProgressAnswer] = []
    for elapsed in [-1.0, 0.0, 0.5, 1.999, 2.0, 2.5, 7.3] {
        let value = DrumClock.barProgress(elapsed: elapsed, tempo: 120, beatsPerBar: 4)
        let answer = BarProgressAnswer(
            elapsed: elapsed,
            tempo: 120,
            beatsPerBar: 4,
            progress: rounded(value)
        )
        progress.append(answer)
    }
    var tracks: [BallTrackAnswer] = []
    for stepsPerBar in [8, 16] {
        var positions: [BallAnswer] = []
        for k in 0...64 {
            let barProgress = Double(k) / 64
            let ball = DrumClock.ballPosition(progress: barProgress, stepsPerBar: stepsPerBar)
            let answer = BallAnswer(
                progress: rounded(barProgress),
                step: ball.step,
                travel: rounded(ball.travel),
                bounce: rounded(ball.bounce)
            )
            positions.append(answer)
        }
        tracks.append(BallTrackAnswer(stepsPerBar: stepsPerBar, positions: positions))
    }
    return DrumsSection(
        voices: voices,
        grooves: grooves,
        barDurations: durations,
        barProgress: progress,
        ballTracks: tracks
    )
}

// MARK: - drumRender

struct RenderAnswer: Encodable {
    let groove: String
    let tempo: Double
    let sampleRate: Double
    let frames: Int
    /// The first index holding the largest |sample|.
    let peakIndex: Int
    /// Sums taken in Double over the Float samples, in index order.
    let sum: Double
    let sumAbs: Double
    /// The largest |sample|.
    let peak: Double
    /// The first 32 samples.
    let head: [Double]
    /// 64 samples at index i * frames / 64 (integer division), i in 0..<64.
    let probe: [Double]
}

struct NoiseAnswer: Encodable {
    let seed: UInt32
    /// The generator's state after each of the first 16 `next()` calls, exact.
    let states: [UInt32]
    /// The values those calls returned.
    let values: [Double]
}

struct DrumRenderSection: Encodable {
    let fixtures: [RenderAnswer]
    let noise: [NoiseAnswer]
}

func renderAnswer(_ groove: DrumGroove, tempo: Double, sampleRate: Double) -> RenderAnswer {
    let samples = DrumBarRenderer(sampleRate: sampleRate).render(groove, tempo: tempo)
    var sum = 0.0
    var sumAbs = 0.0
    var peak = 0.0
    var peakIndex = 0
    for (index, sample) in samples.enumerated() {
        let value = Double(sample)
        sum += value
        sumAbs += abs(value)
        if abs(value) > peak {
            peak = abs(value)
            peakIndex = index
        }
    }
    var head: [Double] = []
    for index in 0..<min(32, samples.count) {
        head.append(rounded(Double(samples[index])))
    }
    var probe: [Double] = []
    for slot in 0..<64 {
        let index = slot * samples.count / 64
        probe.append(rounded(Double(samples[index])))
    }
    return RenderAnswer(
        groove: groove.name,
        tempo: tempo,
        sampleRate: sampleRate,
        frames: samples.count,
        peakIndex: peakIndex,
        sum: rounded(sum),
        sumAbs: rounded(sumAbs),
        peak: rounded(peak),
        head: head,
        probe: probe
    )
}

func noiseAnswer(seed: UInt32) -> NoiseAnswer {
    var noise = NoiseSource(seed: seed)
    var states: [UInt32] = []
    var values: [Double] = []
    for _ in 0..<16 {
        let value = noise.next()
        states.append(noise.state)
        values.append(rounded(value))
    }
    return NoiseAnswer(seed: seed, states: states, values: values)
}

/// Every groove at 90 BPM and a low 8 kHz rate (small, still covers every voice), then
/// Basic Rock at the slowest and fastest tempos at real output rates.
func drumRenderSection() throws -> DrumRenderSection {
    var fixtures: [RenderAnswer] = []
    for groove in DrumGroove.builtIn {
        fixtures.append(renderAnswer(groove, tempo: 90, sampleRate: 8_000))
    }
    guard let basicRock = DrumGroove.builtIn.first(where: { $0.name == "Basic Rock" }) else {
        throw AnswersError.fixtureFailed("Basic Rock is missing")
    }
    fixtures.append(renderAnswer(basicRock, tempo: 40, sampleRate: 48_000))
    fixtures.append(renderAnswer(basicRock, tempo: 220, sampleRate: 44_100))
    let noise = [noiseAnswer(seed: 7), noiseAnswer(seed: 11)]
    return DrumRenderSection(fixtures: fixtures, noise: noise)
}
