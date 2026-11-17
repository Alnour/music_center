import Foundation

/// Where the bouncing ball is at one instant of a bar.
///
/// The ball lands on every count of the groove and flies in an arc to the next one, so it
/// touches down exactly when the player should strike. `step` is the count it left last,
/// `travel` how far it is towards the next one, and `bounce` its height, 0 on a count and
/// 1 at the top of the arc.
struct BallPosition: Equatable, Sendable {
    let step: Int
    let travel: Double
    let bounce: Double
}

/// Turns playing time into a place in the bar. Pure maths, so the view and the tests
/// share it.
enum DrumClock {
    /// Seconds one bar takes at this tempo.
    static func barDuration(tempo: Double, beatsPerBar: Int) -> TimeInterval {
        Double(beatsPerBar) * 60 / tempo
    }

    /// How far through the bar the player is, 0 at beat one and wrapping at the bar line.
    static func barProgress(elapsed: TimeInterval, tempo: Double, beatsPerBar: Int) -> Double {
        let duration = barDuration(tempo: tempo, beatsPerBar: beatsPerBar)
        let bars = max(elapsed, 0) / duration
        return bars - floor(bars)
    }

    /// The ball for a bar progress, on a bar of `stepsPerBar` counts.
    static func ballPosition(progress: Double, stepsPerBar: Int) -> BallPosition {
        let position = progress * Double(stepsPerBar)
        let step = min(Int(floor(position)), stepsPerBar - 1)
        let travel = position - Double(step)
        let bounce = sin(travel * .pi)
        return BallPosition(step: step, travel: travel, bounce: bounce)
    }
}
