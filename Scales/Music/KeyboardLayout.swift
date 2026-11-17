/// Which scale tone (if any) sits on each piano key, ready to be drawn.
///
/// The keys run from C to a closing C, the way a keyboard is read, whatever the scale's
/// root: the root is picked out by its colour wherever it falls. Like `FretboardLayout`
/// this is pure data, so the phone and the TV draw the same keys and tests can check
/// membership without rendering.
struct KeyboardLayout: Sendable {
    struct Key: Sendable {
        /// Semitones above C: 0 is C, 11 is B. The closing key is C again, so it is 0 too.
        let pitchClass: Int
        let isBlack: Bool
        /// The scale tone on this key, or nil when the key is outside the scale.
        let tone: ScaleTone?
    }

    /// The pitch classes no natural letter lands on: C♯/D♭, D♯/E♭, F♯/G♭, G♯/A♭, A♯/B♭.
    static let blackPitchClasses: Set<Int> = [1, 3, 6, 8, 10]

    /// Pitch classes of one octave's twelve keys, white and black, in keyboard order.
    private static let octavePitchClasses = Array(0...11)

    /// C D E F G A B for each octave and then the closing C, with the black keys in
    /// between, left to right: 13 keys for one octave, 25 for two.
    let keys: [Key]

    init(scale: Scale, octaveCount: Int = 1) {
        var pitchClasses: [Int] = []
        for _ in 0..<max(1, octaveCount) {
            pitchClasses.append(contentsOf: Self.octavePitchClasses)
        }
        pitchClasses.append(0)
        var keys: [Key] = []
        for pitchClass in pitchClasses {
            let key = Key(
                pitchClass: pitchClass,
                isBlack: Self.blackPitchClasses.contains(pitchClass),
                tone: scale.tone(forPitchClass: pitchClass)
            )
            keys.append(key)
        }
        self.keys = keys
    }

    /// The white keys, left to right. Drawn first because the black keys sit on them.
    var whiteKeys: [Key] {
        keys.filter { !$0.isBlack }
    }

    /// The black keys, left to right.
    var blackKeys: [Key] {
        keys.filter(\.isBlack)
    }
}
