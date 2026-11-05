/// A step of a scale described relative to the major scale, such as 3, ♭3 or ♯4.
///
/// Describing scales this way gives two things at once: the semitone distance from the
/// root (for finding notes on the neck) and the letter offset (for spelling them
/// correctly). A ♭3 is always a third-letter note, so in A it is C, never B♯.
///
/// Degrees sort in letter order (by number, then flats before sharps), the order a formula
/// is written in. Saved scales store their degrees, so the type is Codable.
struct ScaleDegree: Hashable, Codable, Comparable, Sendable {
    /// 1 through 7.
    let number: Int
    /// −1 lowers the major-scale step by a semitone, +1 raises it.
    let alteration: Int

    private static let majorScaleSemitones = [0, 2, 4, 5, 7, 9, 11]

    /// Degree numbers a scale can use: one per letter of the scale.
    static let numbers = 1...7

    init(number: Int, alteration: Int) {
        self.number = number
        self.alteration = alteration
    }

    /// Reads one symbol of a scale formula, such as "5", "♭3" or "♯4". Formulas are fixed
    /// in the source and covered by tests, so a malformed one is a programming error.
    init(symbol: String) {
        var alteration = 0
        var digits = Substring(symbol)
        if digits.first == "♭" {
            alteration = -1
            digits = digits.dropFirst()
        } else if digits.first == "♯" {
            alteration = 1
            digits = digits.dropFirst()
        }
        guard let number = Int(digits), Self.numbers.contains(number) else {
            preconditionFailure("Malformed scale degree symbol: \(symbol)")
        }
        self.init(number: number, alteration: alteration)
    }

    /// The root, which every scale starts on and no edit may move.
    static let root = ScaleDegree(number: 1, alteration: 0)

    var semitonesAboveRoot: Int {
        Self.majorScaleSemitones[number - 1] + alteration
    }

    /// Semitones above the root folded into one octave, so a ♯7 lands on the root itself.
    /// Two degrees with the same value are the same pitch, whatever their spelling.
    var pitchClassAboveRoot: Int {
        PitchMath.wrap(semitonesAboveRoot)
    }

    var isRoot: Bool {
        number == 1 && alteration == 0
    }

    var label: String {
        let prefix: String
        switch alteration {
        case ..<0: prefix = "♭"
        case 1...: prefix = "♯"
        default: prefix = ""
        }
        return prefix + String(number)
    }

    /// The same step a semitone lower, keeping its letter: 7 becomes ♭7, ♯4 becomes 4.
    func lowered() -> ScaleDegree {
        ScaleDegree(number: number, alteration: alteration - 1)
    }

    /// The same step a semitone higher, keeping its letter: 4 becomes ♯4, ♭3 becomes 3.
    func raised() -> ScaleDegree {
        ScaleDegree(number: number, alteration: alteration + 1)
    }

    /// Letter order: 2 before ♭3 before 3 before ♯4. Within one letter, flat comes first.
    static func < (lhs: ScaleDegree, rhs: ScaleDegree) -> Bool {
        if lhs.number != rhs.number {
            return lhs.number < rhs.number
        }
        return lhs.alteration < rhs.alteration
    }
}
