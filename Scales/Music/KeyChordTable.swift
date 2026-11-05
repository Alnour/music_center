/// One row of a key table: the key and the chords that belong to it.
struct KeyChordRow: Identifiable, Sendable {
    let key: Scale
    /// The seven chords built from the key's own scale.
    let chords: [DiatonicChord]
    /// Chords from a related scale that can stand in for the chord at a given step,
    /// keyed by step index (minor keys only, see `KeyChordTable.minorRows`).
    let alternatives: [Int: DiatonicChord]
    /// "major" or "minor", the word musicians put after the key's root.
    let familyName: String

    var id: String { key.title }

    /// The way musicians say the key: "C major", "A minor".
    var keyName: String {
        "\(key.root.displayName) \(familyName)"
    }
}

/// The major and minor key tables, laid out around the circle of fifths.
///
/// Rows follow the circle (each key one sharp more, or one flat fewer, than the last),
/// the order musicians learn keys in, and each minor key sits in the same row position as
/// its relative major (A minor beside C major, E minor beside G major, and so on).
enum KeyChordTable {
    static let majorKeyRoots: [NoteName] = [
        NoteName(.c), NoteName(.g), NoteName(.d), NoteName(.a), NoteName(.e), NoteName(.b),
        NoteName(.f, 1), NoteName(.d, -1), NoteName(.a, -1), NoteName(.e, -1), NoteName(.b, -1),
        NoteName(.f),
    ]

    static let minorKeyRoots: [NoteName] = [
        NoteName(.a), NoteName(.e), NoteName(.b), NoteName(.f, 1), NoteName(.c, 1),
        NoteName(.g, 1), NoteName(.d, 1), NoteName(.b, -1), NoteName(.f), NoteName(.c),
        NoteName(.g), NoteName(.d),
    ]

    static func majorRows() -> [KeyChordRow] {
        var rows: [KeyChordRow] = []
        for root in majorKeyRoots {
            let key = Scale(root: root, type: .major)
            let row = KeyChordRow(
                key: key,
                chords: key.diatonicTriads(),
                alternatives: [:],
                familyName: "major"
            )
            rows.append(row)
        }
        return rows
    }

    /// Steps whose chord changes when the seventh note is raised (harmonic minor): the
    /// fifth step (v becomes V) and the seventh (VII becomes vii°).
    static let harmonicMinorSteps = [4, 6]

    /// Minor keys use the natural minor scale for their seven chords. Harmonic minor raises
    /// the seventh note, which turns the minor v into a major V, the chord most minor-key
    /// songs use to pull back home, and VII into vii°. Those two are offered as
    /// alternatives at the steps they replace.
    static func minorRows() -> [KeyChordRow] {
        var rows: [KeyChordRow] = []
        for root in minorKeyRoots {
            let key = Scale(root: root, type: .naturalMinor)
            let harmonic = Scale(root: root, type: .harmonicMinor).diatonicTriads()
            var alternatives: [Int: DiatonicChord] = [:]
            for step in harmonicMinorSteps {
                alternatives[step] = harmonic[step]
            }
            let row = KeyChordRow(
                key: key,
                chords: key.diatonicTriads(),
                alternatives: alternatives,
                familyName: "minor"
            )
            rows.append(row)
        }
        return rows
    }
}
