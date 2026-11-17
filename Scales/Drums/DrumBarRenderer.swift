import Foundation

/// Synthesizes one bar of a groove as mono samples, so the kit needs no sound files.
///
/// Each voice is a short burst shaped like the real drum: the kick is a sine whose pitch
/// falls from 150 Hz to 50 Hz, the snare a tone under a burst of noise, the hi-hat a
/// click of bright noise. The bar is rendered whole so the player can loop it
/// sample-accurately; the noise comes from a fixed seed so a bar renders the same twice.
struct DrumBarRenderer {
    let sampleRate: Double

    /// The samples for one bar of the groove at the tempo. Hits that overlap are summed
    /// and the result is kept inside ±1.
    func render(_ groove: DrumGroove, tempo: Double) -> [Float] {
        let duration = DrumClock.barDuration(tempo: tempo, beatsPerBar: groove.beatsPerBar)
        let frameCount = Int(duration * sampleRate)
        let stepFrames = Double(frameCount) / Double(groove.stepsPerBar)
        var samples = [Float](repeating: 0, count: frameCount)
        for hit in groove.hits {
            let start = Int(Double(hit.step) * stepFrames)
            mix(sound(for: hit.voice), into: &samples, at: start)
        }
        for index in samples.indices {
            samples[index] = max(-1, min(1, samples[index]))
        }
        return samples
    }

    private func mix(_ sound: [Float], into samples: inout [Float], at start: Int) {
        for (offset, value) in sound.enumerated() {
            let index = start + offset
            if index < samples.count {
                samples[index] += value
            }
        }
    }

    private func sound(for voice: DrumVoice) -> [Float] {
        switch voice {
        case .kick: return kick()
        case .snare: return snare()
        case .hiHat: return hiHat()
        }
    }

    private func kick() -> [Float] {
        let length = Int(0.18 * sampleRate)
        var samples: [Float] = []
        var phase = 0.0
        for frame in 0..<length {
            let time = Double(frame) / sampleRate
            // The pitch drops fast, which is what makes a kick sound like a thump.
            let frequency = 50 + 100 * exp(-time * 30)
            phase += 2 * .pi * frequency / sampleRate
            let envelope = exp(-time * 14)
            samples.append(Float(sin(phase) * envelope * 0.9))
        }
        return samples
    }

    private func snare() -> [Float] {
        let length = Int(0.16 * sampleRate)
        var noise = NoiseSource(seed: 7)
        var samples: [Float] = []
        for frame in 0..<length {
            let time = Double(frame) / sampleRate
            let tone = sin(2 * .pi * 185 * time) * exp(-time * 25) * 0.35
            let rattle = noise.next() * exp(-time * 18) * 0.45
            samples.append(Float(tone + rattle))
        }
        return samples
    }

    private func hiHat() -> [Float] {
        let length = Int(0.05 * sampleRate)
        var noise = NoiseSource(seed: 11)
        var samples: [Float] = []
        var previous = 0.0
        for frame in 0..<length {
            let time = Double(frame) / sampleRate
            let value = noise.next()
            // Differencing the noise keeps only its highs, the "ts" of a closed hat.
            let bright = value - previous
            previous = value
            samples.append(Float(bright * exp(-time * 80) * 0.25))
        }
        return samples
    }
}

/// White noise in -1...1 from a fixed seed (a linear congruential generator), so the
/// drum sounds are the same every time they are rendered.
private struct NoiseSource {
    private var state: UInt32

    init(seed: UInt32) {
        state = seed
    }

    mutating func next() -> Double {
        state = state &* 1_664_525 &+ 1_013_904_223
        return Double(state) / Double(UInt32.max) * 2 - 1
    }
}
