// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import Foundation
import Testing

@testable import Scales

/// Shorthand for degrees in the tests: `degree("♭7")`.
private func degree(_ symbol: String) -> ScaleDegree {
    ScaleDegree(symbol: symbol)
}

private func noteNames(_ scale: Scale) -> [String] {
    var names: [String] = []
    for tone in scale.tones {
        names.append(tone.name.displayName)
    }
    return names
}

private func chordNames(_ chords: [ScaleChord]) -> [String] {
    var names: [String] = []
    for chord in chords {
        names.append(chord.triad.name)
    }
    return names
}

/// Definitions: the shared description of built-in and saved scales.
struct ScaleDefinitionTests {
    // feature: roots-scales
    @Test func everyBuiltInFormulaIsAlreadyInLetterOrder() {
        for type in ScaleType.allCases {
            #expect(type.definition.degrees == type.degrees, "\(type) was reordered")
            #expect(type.definition.formula == type.formula)
            #expect(type.definition.name == type.displayName)
        }
    }

    // feature: roots-scales
    @Test func degreesAreSortedIntoLetterOrder() {
        let definition = ScaleDefinition(
            name: "Shuffled",
            degrees: [degree("5"), degree("1"), degree("♭3"), degree("2")]
        )
        #expect(definition.formula == "1 2 ♭3 5")
    }

    // feature: roots-scales, transform-lessons
    @Test func definitionsRoundTripThroughJSON() throws {
        let definition = ScaleType.phrygianDominant.definition
        let data = try JSONEncoder().encode(definition)
        let decoded = try JSONDecoder().decode(ScaleDefinition.self, from: data)
        #expect(decoded == definition)
    }

    // feature: roots-scales
    @Test func samePitchesIgnoreSpellingAndName() {
        let respelledWholeTone = ScaleDefinition(
            name: "Respelled",
            degrees: [
                degree("1"), degree("2"), degree("3"), degree("♭5"), degree("♭6"), degree("♭7"),
            ]
        )
        #expect(respelledWholeTone.hasSamePitches(as: ScaleType.wholeTone.definition))
        #expect(ScaleType.major.definition.hasSamePitches(as: ScaleType.ionian.definition))
        #expect(!ScaleType.major.definition.hasSamePitches(as: ScaleType.mixolydian.definition))
    }

    // feature: roots-scales
    @Test func scaleTitleUsesTheDefinitionName() {
        let definition = ScaleDefinition(name: "Bright Major", degrees: ScaleType.lydian.degrees)
        #expect(Scale(root: NoteName(.g), definition: definition).title == "G Bright Major")
        #expect(Scale(root: NoteName(.b, -1), type: .dorian).title == "B♭ Dorian")
    }
}

/// Edits: what one change does to a formula, and which changes are refused.
struct ScaleEditTests {
    private let major = ScaleType.major.definition

    // feature: note-edit-revert, transform-lessons
    @Test func loweringTheSeventhOfMajorGivesMixolydian() throws {
        let result = try major.applying(.lower(degree("7")))
        #expect(result.degrees == ScaleType.mixolydian.degrees)
        #expect(result.name == "Major ♭7")
    }

    // feature: note-edit-revert, transform-lessons
    @Test func raisingTheFourthOfMajorGivesLydian() throws {
        let result = try major.applying(.raise(degree("4")))
        #expect(result.degrees == ScaleType.lydian.degrees)
        #expect(result.name == "Major ♯4")
    }

    // feature: note-edit-revert, transform-lessons
    @Test func takingAnAccidentalAwayIsWrittenAsANatural() throws {
        let edit = ScaleEdit.lower(degree("♯4"))
        let result = try ScaleType.lydian.definition.applying(edit)
        #expect(result.degrees == ScaleType.major.degrees)
        #expect(edit.shortLabel == "♮4")
        #expect(result.name == "Lydian ♮4")
    }

    // feature: note-edit-revert, transform-lessons
    @Test func removingTheFourthOfMajorLeavesSixNotes() throws {
        let result = try major.applying(.remove(degree("4")))
        #expect(result.formula == "1 2 3 5 6 7")
        #expect(result.name == "Major no 4")
    }

    // feature: note-edit-revert, transform-lessons
    @Test func addingTheFourthToMajorPentatonicSortsItIntoPlace() throws {
        let result = try ScaleType.majorPentatonic.definition.applying(.add(degree("4")))
        #expect(result.formula == "1 2 3 4 5 6")
        #expect(result.name == "Major Pentatonic add 4")
    }

    // feature: note-edit-revert, transform-lessons
    @Test func rootCannotBeAlteredOrRemoved() {
        let root = ScaleDegree.root
        #expect(throws: ScaleEditError.rootCannotChange) { try major.applying(.lower(root)) }
        #expect(throws: ScaleEditError.rootCannotChange) { try major.applying(.raise(root)) }
        #expect(throws: ScaleEditError.rootCannotChange) { try major.applying(.remove(root)) }
        #expect(throws: ScaleEditError.rootCannotChange) { try major.applying(.add(degree("♯1"))) }
    }

    // feature: note-edit-revert, transform-lessons
    @Test func editingAMissingDegreeIsRefused() {
        #expect(throws: ScaleEditError.degreeNotInScale) {
            try major.applying(.lower(degree("♭3")))
        }
        #expect(throws: ScaleEditError.degreeNotInScale) {
            try major.applying(.remove(degree("♯4")))
        }
    }

    // feature: note-edit-revert, transform-lessons
    @Test func addingADegreeAlreadyThereIsRefused() {
        let lydian = ScaleType.lydian.definition
        #expect(throws: ScaleEditError.degreeAlreadyInScale) {
            try lydian.applying(.add(degree("♯4")))
        }
    }

    // feature: note-edit-revert, transform-lessons
    @Test func addingADegreeBeyondTheSeventhIsRefused() {
        let eighth = ScaleDegree(number: 8, alteration: 0)
        #expect(throws: ScaleEditError.noSuchDegree) { try major.applying(.add(eighth)) }
    }

    // feature: note-edit-revert, transform-lessons
    @Test func loweringPastOneFlatIsRefused() {
        let dorian = ScaleType.dorian.definition
        #expect(throws: ScaleEditError.alterationTooFar) {
            try dorian.applying(.lower(degree("♭3")))
        }
    }

    // feature: note-edit-revert, transform-lessons
    @Test func aNoteMayNotSoundTheSameAsAnother() {
        let lydian = ScaleType.lydian.definition
        let sameAsSharpFour = ScaleEditError.samePitchAsAnotherNote(degree("♯4"))
        #expect(throws: sameAsSharpFour) { try lydian.applying(.add(degree("♭5"))) }
        let sameAsRoot = ScaleEditError.samePitchAsAnotherNote(.root)
        #expect(throws: sameAsRoot) { try major.applying(.raise(degree("7"))) }
    }

    // feature: note-edit-revert, transform-lessons
    @Test func scalesKeepAtLeastThreeNotes() {
        let triad = ScaleDefinition(
            name: "Triad",
            degrees: [degree("1"), degree("3"), degree("5")]
        )
        #expect(throws: ScaleEditError.tooFewNotes) { try triad.applying(.remove(degree("3"))) }
    }

    // feature: note-edit-revert, transform-lessons
    @Test func majorOffersTheFiveMissingPitchesInBothSpellings() {
        var addedLabels: [String] = []
        for edit in major.legalEdits() {
            if case .add(let added) = edit {
                addedLabels.append(added.label)
            }
        }
        #expect(addedLabels == ["♭2", "♯2", "♭3", "♯4", "♭5", "♯5", "♭6", "♯6", "♭7"])
    }

    // feature: note-edit-revert, transform-lessons
    @Test func everyLegalEditAppliesAndKeepsPitchesDistinct() throws {
        for type in ScaleType.allCases {
            let definition = type.definition
            for edit in definition.legalEdits() {
                let result = try definition.applying(edit)
                var pitches = Set<Int>()
                for resultDegree in result.degrees {
                    pitches.insert(resultDegree.pitchClassAboveRoot)
                }
                #expect(pitches.count == result.degrees.count, "\(type) \(edit) repeats a pitch")
                #expect(result.degrees.first == .root)
            }
        }
    }

    // feature: note-edit-revert, transform-lessons
    @Test func editSentencesSpellNotesInTheKey() {
        #expect(
            ScaleEdit.lower(degree("7")).sentence(above: NoteName(.g))
                == "Lowered F♯ (7) to F (♭7)."
        )
        #expect(
            ScaleEdit.raise(degree("♭3")).sentence(above: NoteName(.a))
                == "Raised C (♭3) to C♯ (3)."
        )
        #expect(ScaleEdit.remove(degree("4")).sentence(above: NoteName(.c)) == "Removed F (4).")
        #expect(ScaleEdit.add(degree("♭6")).sentence(above: NoteName(.e)) == "Added C (♭6).")
    }
}

/// Chords built by letter, for scales of any size.
struct LetterTriadTests {
    /// The textbook way for a seven-note scale: stack every other note by position.
    private func everyOtherNoteChordNames(_ scale: Scale) -> [String] {
        var names: [String] = []
        let count = scale.tones.count
        for step in 0..<count {
            let root = scale.tones[step].name
            let third = scale.tones[(step + 2) % count].name
            let fifth = scale.tones[(step + 4) % count].name
            let quality = TriadQuality(
                thirdSemitones: PitchMath.wrap(third.pitchClass - root.pitchClass),
                fifthSemitones: PitchMath.wrap(fifth.pitchClass - root.pitchClass)
            )
            if let quality {
                names.append(Triad(root: root, quality: quality).name)
            }
        }
        return names
    }

    // feature: roots-scales, chord-chips-rings
    @Test func byLetterMatchesEveryOtherNoteForSevenNoteScales() {
        for type in ScaleType.allCases where type.degrees.count == 7 {
            for root in NoteName.rootChoices {
                let scale = Scale(root: root, type: type)
                let expected = everyOtherNoteChordNames(scale)
                #expect(chordNames(scale.letterTriads()) == expected, "\(root) \(type)")
            }
        }
    }

    // feature: roots-scales, chord-chips-rings
    @Test func pentatonicScalesMakeTwoTriadsAndTwoSuspendedChords() {
        let cMajorPentatonic = Scale(root: NoteName(.c), type: .majorPentatonic)
        #expect(chordNames(cMajorPentatonic.letterTriads()) == ["C", "Dsus4", "Gsus4", "Am"])
        let aMinorPentatonic = Scale(root: NoteName(.a), type: .minorPentatonic)
        #expect(chordNames(aMinorPentatonic.letterTriads()) == ["Am", "C", "Dsus4", "Gsus4"])
    }

    // feature: roots-scales, chord-chips-rings
    @Test func suspendedChordsCarryTheStandInNote() throws {
        let chords = Scale(root: NoteName(.c), type: .majorPentatonic).letterTriads()
        let onD = try #require(chords.first { $0.degree.number == 2 })
        #expect(onD.notes == [NoteName(.d), NoteName(.g), NoteName(.a)])
        #expect(onD.triad.spokenName == "D suspended fourth")
        #expect(onD.formulaNumeral == "IIsus4")
    }

    // feature: roots-scales, chord-chips-rings
    @Test func theSecondStandsInWhenThereIsNoFourthEither() {
        // C D G: no E, no F, so the chord on C is Csus2.
        let definition = ScaleDefinition(
            name: "Test",
            degrees: [degree("1"), degree("2"), degree("5")]
        )
        let chords = Scale(root: NoteName(.c), definition: definition).letterTriads()
        #expect(chordNames(chords) == ["Csus2", "Gsus4"])
    }

    // feature: roots-scales, chord-chips-rings
    @Test func everyChordQualityHasAChartName() {
        var names: [String] = []
        for quality in TriadQuality.allCases {
            names.append(Triad(root: NoteName(.c), quality: quality).name)
        }
        #expect(
            names == [
                "C", "Cm", "C°", "C+", "C(♭5)", "Cm(♯5)", "Csus4", "Csus2",
                "Csus4(♭5)", "Csus2(♭5)", "Csus4(♯5)", "Csus2(♯5)",
            ]
        )
        #expect(TriadQuality.majorFlatFive.numeral("I") == "I(♭5)")
        #expect(TriadQuality.minorSharpFive.numeral("III") == "iii(♯5)")
        #expect(TriadQuality.suspendedSecond.numeral("IV") == "IVsus2")
        #expect(TriadQuality(thirdSemitones: 5, fifthSemitones: 9) == nil)
    }

    // feature: roots-scales, chord-chips-rings
    @Test func bluesPrefersThePairingWithFewestAccidentals() throws {
        let chords = Scale(root: NoteName(.c), type: .blues).letterTriads()
        let tonic = try #require(chords.first)
        #expect(tonic.triad.name == "Cm")
        #expect(tonic.notes == [NoteName(.c), NoteName(.e, -1), NoteName(.g)])
    }

    // feature: roots-scales, chord-chips-rings
    @Test func chordsCarryTheirSpelledNotes() {
        let dominant = Scale(root: NoteName(.g), type: .major).letterTriads()[4]
        #expect(dominant.notes == [NoteName(.d), NoteName(.f, 1), NoteName(.a)])
        #expect(dominant.spokenTitle == "V, D major")
    }

    // feature: roots-scales, chord-tables
    @Test func formulaNumeralsCarryAccidentalsAndKeyNumeralsStayPlain() {
        let harmonicMinor = Scale(root: NoteName(.c), type: .harmonicMinor).letterTriads()
        var formulaNumerals: [String] = []
        var numerals: [String] = []
        for chord in harmonicMinor {
            formulaNumerals.append(chord.formulaNumeral)
            numerals.append(chord.numeral)
        }
        #expect(formulaNumerals == ["i", "ii°", "♭III+", "iv", "V", "♭VI", "vii°"])
        #expect(numerals == ["i", "ii°", "III+", "iv", "V", "VI", "vii°"])
        let phrygianSecond = Scale(root: NoteName(.c), type: .phrygian).letterTriads()[1]
        #expect(phrygianSecond.formulaNumeral == "♭II")
    }

    // feature: chord-tables
    @Test func keyTablesStillOnlyCoverSevenNoteScales() {
        #expect(Scale(root: NoteName(.a), type: .minorPentatonic).diatonicTriads().isEmpty)
        #expect(Scale(root: NoteName(.a), type: .minorPentatonic).letterTriads().count == 4)
    }
}

/// Where the chords under the board come from: a scale's own notes, or the key a
/// pentatonic or blues scale is picked from.
struct HarmonicHomeTests {
    @Test func sevenNoteAndSavedScalesAreHarmonisedOnTheirOwnNotes() {
        let gMajor = Scale(root: NoteName(.g), type: .major)
        #expect(chordNames(gMajor.boardChords()) == chordNames(gMajor.letterTriads()))
        #expect(gMajor.harmonyNote == nil)
        // A saved copy of the minor pentatonic keeps its own stacked chords, since the
        // edit lessons talk about those.
        let saved = ScaleDefinition(
            name: "My Pentatonic",
            degrees: ScaleType.minorPentatonic.degrees
        )
        let savedScale = Scale(root: NoteName(.a), definition: saved)
        #expect(savedScale.builtInType == nil)
        #expect(chordNames(savedScale.boardChords()) == ["Am", "C", "Dsus4", "Gsus4"])
    }

    @Test func pentatonicScalesBorrowTheirKeysChords() {
        let aMinorPentatonic = Scale(root: NoteName(.a), type: .minorPentatonic)
        #expect(
            chordNames(aMinorPentatonic.boardChords()) == ["Am", "B°", "C", "Dm", "Em", "F", "G"]
        )
        #expect(
            aMinorPentatonic.harmonyNote
                == "Minor Pentatonic is A minor without its 2 and ♭6, so it is played over A minor's chords."
        )
        let gMajorPentatonic = Scale(root: NoteName(.g), type: .majorPentatonic)
        #expect(
            chordNames(gMajorPentatonic.boardChords()) == ["G", "Am", "Bm", "C", "D", "Em", "F♯°"]
        )
    }

    @Test func bluesScalesTakeTheTwelveBarSeventhChords() throws {
        let aBlues = Scale(root: NoteName(.a), type: .blues)
        let chords = aBlues.boardChords()
        #expect(chordNames(chords) == ["A", "D", "E"])
        var names: [String] = []
        var numerals: [String] = []
        for chord in chords {
            names.append(chord.name)
            numerals.append(chord.formulaNumeral)
        }
        #expect(names == ["A7", "D7", "E7"])
        #expect(numerals == ["I7", "IV7", "V7"])
        let tonic = try #require(chords.first)
        #expect(tonic.notes == [NoteName(.a), NoteName(.c, 1), NoteName(.e), NoteName(.g)])
        #expect(tonic.spokenTitle == "I7, A major seventh")
        let eMajorBlues = Scale(root: NoteName(.e), type: .majorBlues)
        #expect(chordNames(eMajorBlues.boardChords()) == ["E", "A", "B"])
        #expect(eMajorBlues.harmonyNote?.hasPrefix("Major Blues is played over") == true)
    }
}

/// The chord-by-chord story of an edit.
struct ChordChangeReportTests {
    private func report(_ root: NoteName, _ type: ScaleType, _ edit: ScaleEdit) throws
        -> ChordChangeReport
    {
        let transformation = try ScaleTransformation(
            root: root,
            before: type.definition,
            edit: edit
        )
        return transformation.chordChanges
    }

    private func kinds(_ report: ChordChangeReport) -> [ChordChange.Kind] {
        var kinds: [ChordChange.Kind] = []
        for change in report.changes {
            kinds.append(change.kind)
        }
        return kinds
    }

    // feature: transform-lessons, note-edit-revert
    @Test func loweringTheSeventhOfCMajor() throws {
        let changes = try report(NoteName(.c), .major, .lower(degree("7"))).changes
        #expect(changes.count == 7)
        #expect(
            kinds(try report(NoteName(.c), .major, .lower(degree("7"))))
                == [.unchanged, .unchanged, .changed, .unchanged, .changed, .unchanged, .changed]
        )
        #expect(changes[0].summary == "I, C major, stays the same: C, E, G.")
        #expect(changes[2].after?.triad.name == "E°")
        #expect(
            changes[4].summary
                == "V, G major, becomes v, G minor: it had G, B, D and now has G, B♭, D."
        )
        let seventh = changes[6]
        #expect(seventh.before?.triad.name == "B°")
        #expect(seventh.after?.triad.name == "B♭")
        #expect(seventh.notesGone == [NoteName(.b)])
        #expect(seventh.notesNew == [NoteName(.b, -1)])
    }

    // feature: transform-lessons, note-edit-revert
    @Test func removingTheFourthDropsOrSuspendsTheChordsThatNeededIt() throws {
        let result = try report(NoteName(.c), .major, .remove(degree("4")))
        #expect(
            kinds(result)
                == [.unchanged, .changed, .unchanged, .removed, .unchanged, .unchanged, .removed]
        )
        #expect(
            result.changes[1].summary
                == "ii, D minor, becomes IIsus4, D suspended fourth: it had D, F, A "
                + "and now has D, G, A."
        )
        #expect(result.changes[3].notesGone == [NoteName(.f)])
        #expect(
            result.changes[6].summary
                == "vii°, B diminished, is gone: it needed F, which is no longer in the scale."
        )
    }

    // feature: transform-lessons, note-edit-revert
    @Test func addingTheFourthToPentatonicMakesNewChordsAndResolvesASuspension() throws {
        let result = try report(NoteName(.c), .majorPentatonic, .add(degree("4")))
        #expect(kinds(result) == [.unchanged, .changed, .added, .unchanged, .unchanged])
        #expect(
            result.changes[1].summary
                == "IIsus4, D suspended fourth, becomes ii, D minor: it had D, G, A "
                + "and now has D, F, A."
        )
        #expect(result.changes[2].after?.triad.name == "F")
        // There is still no B, so the chord on G keeps its suspended fourth.
        #expect(result.changes[3].after?.triad.name == "Gsus4")
    }

    // feature: transform-lessons, note-edit-revert
    @Test func raisingTheFlatThreeFollowsTheChordOntoItsNewRoot() throws {
        let result = try report(NoteName(.a), .naturalMinor, .raise(degree("♭3")))
        let third = result.changes[2]
        #expect(third.kind == .changed)
        #expect(
            third.summary
                == "♭III, C major, becomes iii°, C♯ diminished: it had C, E, G and now has C♯, E, G."
        )
        #expect(third.notesGone == [NoteName(.c)])
        #expect(third.notesNew == [NoteName(.c, 1)])
        #expect(result.changes[0].after?.triad.name == "A")
        #expect(result.changes[5].after?.triad.name == "F+")
    }

    // feature: transform-lessons, note-edit-revert
    @Test func loweringTheFifthFlattensTheHomeChord() throws {
        let result = try report(NoteName(.c), .major, .lower(degree("5")))
        #expect(result.changes[0].kind == .changed)
        #expect(
            result.changes[0].summary
                == "I, C major, becomes I(♭5), C major flat five: it had C, E, G "
                + "and now has C, E, G♭."
        )
        #expect(result.changes[0].after?.triad.name == "C(♭5)")
    }

    // feature: transform-lessons, note-edit-revert
    @Test func raisingTheFifthOfAMinorChordSharpensIt() throws {
        let result = try report(NoteName(.a), .dorian, .raise(degree("5")))
        #expect(result.changes[0].after?.triad.name == "Am(♯5)")
        #expect(result.changes[0].after?.formulaNumeral == "i(♯5)")
    }

    // feature: transform-lessons, note-edit-revert
    @Test func identicalScalesReportNoChange() {
        let scale = Scale(root: NoteName(.e), type: .dorian)
        let report = ChordChangeReport(before: scale, after: scale)
        #expect(!report.changesAnyChord)
        #expect(report.changes.count == 7)
    }
}

/// Naming an edited scale from its sound.
struct ScaleIdentifierTests {
    // feature: transform-lessons
    @Test func majorIsCalledMajorNotIonian() {
        #expect(ScaleIdentifier.match(ScaleType.ionian.definition, among: []) == .builtIn(.major))
        let aeolian = ScaleType.aeolian.definition
        #expect(ScaleIdentifier.match(aeolian, among: []) == .builtIn(.naturalMinor))
    }

    // feature: transform-lessons
    @Test func majorWithAFlatSeventhIsMixolydian() throws {
        let edited = try ScaleType.major.definition.applying(.lower(degree("7")))
        let match = ScaleIdentifier.match(edited, among: [])
        #expect(match == .builtIn(.mixolydian))
        #expect(match?.name == "Mixolydian")
        #expect(match?.reference == .builtIn(.mixolydian))
    }

    // feature: transform-lessons
    @Test func aRespelledScaleIsStillRecognised() {
        let respelled = ScaleDefinition(
            name: "Respelled",
            degrees: [
                degree("1"), degree("2"), degree("3"), degree("♭5"), degree("♭6"), degree("♭7"),
            ]
        )
        #expect(ScaleIdentifier.match(respelled, among: []) == .builtIn(.wholeTone))
    }

    // feature: transform-lessons
    @Test func savedScalesAreRecognisedAndNewOnesAreNot() throws {
        let edited = try ScaleType.major.definition.applying(.raise(degree("5")))
        #expect(ScaleIdentifier.match(edited, among: []) == nil)
        let saved = CustomScale(
            definition: ScaleDefinition(name: "Augmented Major", degrees: edited.degrees)
        )
        let match = ScaleIdentifier.match(edited, among: [saved])
        #expect(match == .custom(saved))
        #expect(match?.name == "Augmented Major")
        #expect(match?.reference == .custom(saved.id))
    }
}

/// A whole lesson: one edit in one key, named and kept.
struct ScaleTransformationTests {
    // feature: transform-lessons
    @Test func gMajorWithALoweredSeventh() throws {
        let transformation = try ScaleTransformation(
            root: NoteName(.g),
            before: ScaleType.major.definition,
            edit: .lower(degree("7"))
        )
        #expect(transformation.after.name == "Major ♭7")
        #expect(transformation.editSentence == "Lowered F♯ (7) to F (♭7).")
        #expect(noteNames(transformation.afterScale) == ["G", "A", "B", "C", "D", "E", "F"])
        let named = transformation.naming("Mixolydian")
        #expect(named.title == "G Major → G Mixolydian")
        #expect(named.after.degrees == transformation.after.degrees)
    }

    // feature: transform-lessons
    @Test func anImpossibleEditThrows() {
        #expect(throws: ScaleEditError.rootCannotChange) {
            try ScaleTransformation(
                root: NoteName(.g),
                before: ScaleType.major.definition,
                edit: .remove(.root)
            )
        }
    }

    // feature: transform-lessons
    @Test func lessonsRoundTripThroughJSON() throws {
        let transformation = try ScaleTransformation(
            root: NoteName(.e, -1),
            before: ScaleType.naturalMinor.definition,
            edit: .raise(degree("♭7"))
        )
        let lesson = TransformationLesson(
            transformation: transformation.naming("Harmonic Minor"),
            result: .builtIn(.harmonicMinor)
        )
        let data = try JSONEncoder().encode(lesson)
        let decoded = try JSONDecoder().decode(TransformationLesson.self, from: data)
        #expect(decoded == lesson)
        #expect(decoded.title == "E♭ Natural Minor → E♭ Harmonic Minor")
    }
}
