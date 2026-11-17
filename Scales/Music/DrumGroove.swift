import Foundation

/// The three voices of a basic kit, each with its place on the drum staff.
enum DrumVoice: CaseIterable, Hashable, Sendable {
    case hiHat
    case snare
    case kick

    var name: String {
        switch self {
        case .hiHat: return "Hi-hat"
        case .snare: return "Snare"
        case .kick: return "Kick"
        }
    }
}

/// One stroke: which voice, and where in the bar it lands.
struct DrumHit: Hashable, Sendable {
    let voice: DrumVoice
    /// Counted in the groove's subdivisions from the start of the bar (0 is beat one).
    let step: Int
}

/// A one-bar pattern the player practises along to.
///
/// Every groove is written in subdivisions of the beat: 2 for eighth notes (counted
/// "1 + 2 +"), 4 for sixteenths ("1 e + a"). The counting labels and the staff columns both
/// come from that grid, so a groove of any length in beats draws itself.
struct DrumGroove: Identifiable, Hashable, Sendable {
    let name: String
    let beatsPerBar: Int
    /// Notes per beat: 2 for eighths, 4 for sixteenths.
    let subdivision: Int
    let hits: [DrumHit]

    var id: String { name }

    /// How many columns the bar has: one per subdivision of every beat.
    var stepsPerBar: Int { beatsPerBar * subdivision }

    /// The voices struck together on one step.
    func voices(atStep step: Int) -> Set<DrumVoice> {
        var voices: Set<DrumVoice> = []
        for hit in hits where hit.step == step {
            voices.insert(hit.voice)
        }
        return voices
    }

    /// What the player says on this step: the beat number on the beat, "+" on the and,
    /// "e" and "a" for the sixteenths between.
    func countLabel(forStep step: Int) -> String {
        let beat = step / subdivision + 1
        let withinBeat = step % subdivision
        if withinBeat == 0 {
            return String(beat)
        }
        if subdivision == 4 {
            return withinBeat == 2 ? "+" : (withinBeat == 1 ? "e" : "a")
        }
        return "+"
    }

    /// Grooves to practise, simplest first.
    static let builtIn: [DrumGroove] = [
        DrumGroove(
            name: "Basic Rock",
            beatsPerBar: 4,
            subdivision: 2,
            hits: hiHatEighths + [
                DrumHit(voice: .kick, step: 0),
                DrumHit(voice: .snare, step: 2),
                DrumHit(voice: .kick, step: 4),
                DrumHit(voice: .snare, step: 6),
            ]
        ),
        DrumGroove(
            name: "Four on the Floor",
            beatsPerBar: 4,
            subdivision: 2,
            hits: hiHatEighths + [
                DrumHit(voice: .kick, step: 0),
                DrumHit(voice: .kick, step: 2),
                DrumHit(voice: .snare, step: 2),
                DrumHit(voice: .kick, step: 4),
                DrumHit(voice: .kick, step: 6),
                DrumHit(voice: .snare, step: 6),
            ]
        ),
        DrumGroove(
            name: "Kick on the And",
            beatsPerBar: 4,
            subdivision: 2,
            hits: hiHatEighths + [
                DrumHit(voice: .kick, step: 0),
                DrumHit(voice: .snare, step: 2),
                DrumHit(voice: .kick, step: 3),
                DrumHit(voice: .kick, step: 4),
                DrumHit(voice: .snare, step: 6),
            ]
        ),
        DrumGroove(
            name: "Half Time",
            beatsPerBar: 4,
            subdivision: 2,
            hits: hiHatEighths + [
                DrumHit(voice: .kick, step: 0),
                DrumHit(voice: .kick, step: 3),
                DrumHit(voice: .snare, step: 4),
            ]
        ),
        DrumGroove(
            name: "Sixteenth Hats",
            beatsPerBar: 4,
            subdivision: 4,
            hits: hiHatSixteenths + [
                DrumHit(voice: .kick, step: 0),
                DrumHit(voice: .snare, step: 4),
                DrumHit(voice: .kick, step: 8),
                DrumHit(voice: .kick, step: 10),
                DrumHit(voice: .snare, step: 12),
            ]
        ),
    ]

    private static let hiHatEighths = hiHat(steps: 8)
    private static let hiHatSixteenths = hiHat(steps: 16)

    private static func hiHat(steps: Int) -> [DrumHit] {
        var hits: [DrumHit] = []
        for step in 0..<steps {
            hits.append(DrumHit(voice: .hiHat, step: step))
        }
        return hits
    }
}
