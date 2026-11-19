// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import Foundation
import Testing

@testable import Scales

/// Grooves: the patterns and how they are counted.
struct DrumGrooveTests {
    // feature: drum-trainer
    @Test func everyBuiltInGrooveFitsItsBar() {
        for groove in DrumGroove.builtIn {
            for hit in groove.hits {
                #expect(hit.step >= 0 && hit.step < groove.stepsPerBar, "\(groove.name) overflows")
            }
            #expect(Set(groove.hits).count == groove.hits.count, "\(groove.name) repeats a hit")
        }
    }

    // feature: drum-trainer
    @Test func builtInNamesAreUnique() {
        var names: Set<String> = []
        for groove in DrumGroove.builtIn {
            names.insert(groove.name)
        }
        #expect(names.count == DrumGroove.builtIn.count)
    }

    // feature: drum-trainer
    @Test func basicRockPutsTheBackbeatOnTwoAndFour() {
        let rock = DrumGroove.builtIn[0]
        #expect(rock.stepsPerBar == 8)
        #expect(rock.voices(atStep: 0) == [.hiHat, .kick])
        #expect(rock.voices(atStep: 2) == [.hiHat, .snare])
        #expect(rock.voices(atStep: 1) == [.hiHat])
        #expect(rock.voices(atStep: 6) == [.hiHat, .snare])
    }

    // feature: drum-trainer
    @Test func eighthsAreCountedWithAnds() {
        let groove = DrumGroove(name: "Eighths", beatsPerBar: 4, subdivision: 2, hits: [])
        var labels: [String] = []
        for step in 0..<groove.stepsPerBar {
            labels.append(groove.countLabel(forStep: step))
        }
        #expect(labels == ["1", "+", "2", "+", "3", "+", "4", "+"])
    }

    // feature: drum-trainer
    @Test func sixteenthsAreCountedOneEAndA() {
        let groove = DrumGroove(name: "Sixteenths", beatsPerBar: 2, subdivision: 4, hits: [])
        var labels: [String] = []
        for step in 0..<groove.stepsPerBar {
            labels.append(groove.countLabel(forStep: step))
        }
        #expect(labels == ["1", "e", "+", "a", "2", "e", "+", "a"])
    }
}

/// The clock: turning time into a place in the bar and a ball position.
struct DrumClockTests {
    // feature: drum-trainer
    @Test func aBarAtOneTwentyLastsTwoSeconds() {
        #expect(DrumClock.barDuration(tempo: 120, beatsPerBar: 4) == 2)
    }

    // feature: drum-trainer
    @Test func progressWrapsAtTheBarLine() {
        #expect(DrumClock.barProgress(elapsed: 0, tempo: 120, beatsPerBar: 4) == 0)
        #expect(DrumClock.barProgress(elapsed: 1, tempo: 120, beatsPerBar: 4) == 0.5)
        #expect(DrumClock.barProgress(elapsed: 2, tempo: 120, beatsPerBar: 4) == 0)
        #expect(DrumClock.barProgress(elapsed: 2.5, tempo: 120, beatsPerBar: 4) == 0.25)
    }

    // feature: drum-trainer
    @Test func negativeTimeRestsOnBeatOne() {
        #expect(DrumClock.barProgress(elapsed: -1, tempo: 120, beatsPerBar: 4) == 0)
    }

    // feature: drum-trainer
    @Test func theBallTouchesDownOnEveryCount() {
        let onCount = DrumClock.ballPosition(progress: 0.25, stepsPerBar: 8)
        #expect(onCount.step == 2)
        #expect(onCount.travel == 0)
        #expect(abs(onCount.bounce) < 1e-9)
    }

    // feature: drum-trainer
    @Test func theBallPeaksHalfwayBetweenCounts() {
        let midway = DrumClock.ballPosition(progress: 3.5 / 8, stepsPerBar: 8)
        #expect(midway.step == 3)
        #expect(midway.travel == 0.5)
        #expect(abs(midway.bounce - 1) < 1e-9)
    }

    // feature: drum-trainer
    @Test func theBallNeverLeavesTheBar() {
        let end = DrumClock.ballPosition(progress: 0.999_999_999, stepsPerBar: 8)
        #expect(end.step == 7)
        let overflow = DrumClock.ballPosition(progress: 1, stepsPerBar: 8)
        #expect(overflow.step == 7)
    }
}

/// The sound: one bar of synthesized kit.
struct DrumBarRendererTests {
    // feature: drum-trainer
    @Test func theBarIsExactlyOneBarLong() {
        let renderer = DrumBarRenderer(sampleRate: 1000)
        let samples = renderer.render(DrumGroove.builtIn[0], tempo: 120)
        #expect(samples.count == 2000)
    }

    // feature: drum-trainer
    @Test func everyHitMakesSoundAndNothingClips() {
        let renderer = DrumBarRenderer(sampleRate: 8000)
        let rock = DrumGroove.builtIn[0]
        let samples = renderer.render(rock, tempo: 120)
        let stepFrames = samples.count / rock.stepsPerBar
        for step in 0..<rock.stepsPerBar {
            let window = samples[(step * stepFrames)..<(step * stepFrames + 100)]
            var loudest: Float = 0
            for sample in window {
                loudest = max(loudest, abs(sample))
            }
            #expect(loudest > 0.01, "step \(step) is silent")
            #expect(loudest <= 1)
        }
    }

    // feature: drum-trainer
    @Test func aSilentGrooveRendersSilence() {
        let renderer = DrumBarRenderer(sampleRate: 1000)
        let silent = DrumGroove(name: "Rest", beatsPerBar: 1, subdivision: 2, hits: [])
        let samples = renderer.render(silent, tempo: 60)
        #expect(samples.allSatisfy { $0 == 0 })
    }

    // feature: drum-trainer
    @Test func renderingIsDeterministic() {
        let renderer = DrumBarRenderer(sampleRate: 4000)
        let first = renderer.render(DrumGroove.builtIn[0], tempo: 100)
        let second = renderer.render(DrumGroove.builtIn[0], tempo: 100)
        #expect(first == second)
    }
}
