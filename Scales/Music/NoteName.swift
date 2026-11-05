/// The seven letter names of standard notation.
enum Letter: Int, CaseIterable, Codable, Sendable {
    case c, d, e, f, g, a, b

    /// The pitch class of the letter with no sharp or flat.
    var naturalPitchClass: Int {
        switch self {
        case .c: 0
        case .d: 2
        case .e: 4
        case .f: 5
        case .g: 7
        case .a: 9
        case .b: 11
        }
    }

    var symbol: String {
        switch self {
        case .c: "C"
        case .d: "D"
        case .e: "E"
        case .f: "F"
        case .g: "G"
        case .a: "A"
        case .b: "B"
        }
    }

    /// The letter a number of steps higher, wrapping from B back to C.
    func advanced(by steps: Int) -> Letter {
        let letterCount = Letter.allCases.count
        let wrappedIndex = ((rawValue + steps) % letterCount + letterCount) % letterCount
        return Letter.allCases[wrappedIndex]
    }
}

/// A spelled note such as F♯ or B♭.
///
/// Spelling is kept, rather than only a pitch number, because musicians read G major as
/// containing F♯ and F major as containing B♭, never "G♭" or "A♯". The scale model picks
/// the letter for each degree and this type records the sharps or flats that letter needs.
struct NoteName: Hashable, Codable, Sendable {
    let letter: Letter
    /// Negative values are flats, positive values are sharps (−2 is a double flat).
    let accidental: Int

    init(_ letter: Letter, _ accidental: Int = 0) {
        self.letter = letter
        self.accidental = accidental
    }

    var pitchClass: Int {
        PitchMath.wrap(letter.naturalPitchClass + accidental)
    }

    var displayName: String {
        letter.symbol + accidentalSymbol
    }

    private var accidentalSymbol: String {
        switch accidental {
        case -2: "𝄫"
        case -1: "♭"
        case 0: ""
        case 1: "♯"
        case 2: "𝄪"
        case let count where count < 0: String(repeating: "♭", count: -count)
        default: String(repeating: "♯", count: accidental)
        }
    }

    /// Roots offered in the picker. Both spellings are listed for the black keys so that
    /// keys like D♭ major and C♯ minor each come out spelled the way they are written.
    static let rootChoices: [NoteName] = [
        NoteName(.c),
        NoteName(.c, 1),
        NoteName(.d, -1),
        NoteName(.d),
        NoteName(.d, 1),
        NoteName(.e, -1),
        NoteName(.e),
        NoteName(.f),
        NoteName(.f, 1),
        NoteName(.g, -1),
        NoteName(.g),
        NoteName(.g, 1),
        NoteName(.a, -1),
        NoteName(.a),
        NoteName(.a, 1),
        NoteName(.b, -1),
        NoteName(.b),
    ]
}
