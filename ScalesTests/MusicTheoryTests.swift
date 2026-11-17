import AVFoundation
import CoreGraphics
import Foundation
import Testing

@testable import Scales

/// Spelling checks: every key must read the way it is written in notation.
struct ScaleSpellingTests {
    private func spelledNames(_ root: NoteName, _ type: ScaleType) -> [String] {
        var names: [String] = []
        for tone in Scale(root: root, type: type).tones {
            names.append(tone.name.displayName)
        }
        return names
    }

    @Test func gMajorUsesFSharp() {
        #expect(spelledNames(NoteName(.g), .major) == ["G", "A", "B", "C", "D", "E", "F♯"])
    }

    @Test func fMajorUsesBFlat() {
        #expect(spelledNames(NoteName(.f), .major) == ["F", "G", "A", "B♭", "C", "D", "E"])
    }

    @Test func eFlatMajorIsAllFlats() {
        let expected = ["E♭", "F", "G", "A♭", "B♭", "C", "D"]
        #expect(spelledNames(NoteName(.e, -1), .major) == expected)
    }

    @Test func cSharpMinorKeepsSharps() {
        let expected = ["C♯", "D♯", "E", "F♯", "G♯", "A", "B"]
        #expect(spelledNames(NoteName(.c, 1), .naturalMinor) == expected)
    }

    @Test func aBluesHasFlatFive() {
        #expect(spelledNames(NoteName(.a), .blues) == ["A", "C", "D", "E♭", "E", "G"])
    }

    @Test func eMinorPentatonic() {
        #expect(spelledNames(NoteName(.e), .minorPentatonic) == ["E", "G", "A", "B", "D"])
    }

    @Test func dLydianRaisesTheFourth() {
        #expect(spelledNames(NoteName(.d), .lydian).contains("G♯"))
    }

    @Test func gSharpHarmonicMinorNeedsDoubleSharp() {
        #expect(spelledNames(NoteName(.g, 1), .harmonicMinor).last == "F𝄪")
    }

    @Test func degreeLabelsFollowTheFormula() {
        var labels: [String] = []
        for tone in Scale(root: NoteName(.c), type: .dorian).tones {
            labels.append(tone.degree.label)
        }
        #expect(labels == ["1", "2", "♭3", "4", "5", "6", "♭7"])
    }

    @Test func everyFormulaReadsBackAsWritten() {
        for type in ScaleType.allCases {
            var labels: [String] = []
            for degree in type.degrees {
                labels.append(degree.label)
            }
            #expect(labels.joined(separator: " ") == type.formula)
        }
    }

    @Test func everyScaleHasDistinctPitches() {
        for type in ScaleType.allCases {
            let tones = Scale(root: NoteName(.c), type: type).tones
            var pitchClasses = Set<Int>()
            for tone in tones {
                pitchClasses.insert(tone.name.pitchClass)
            }
            #expect(pitchClasses.count == tones.count, "\(type) repeats a pitch")
        }
    }

    @Test func everyScaleTypeAppearsInExactlyOneFamily() {
        var listed: [ScaleType] = []
        for family in ScaleFamily.allCases {
            listed.append(contentsOf: family.scales)
        }
        #expect(listed.count == ScaleType.allCases.count)
        #expect(Set(listed) == Set(ScaleType.allCases))
    }
}

/// Position checks against the reference "G Major full fretboard" chart.
struct FretboardLayoutTests {
    private let gMajor = FretboardLayout(
        scale: Scale(root: NoteName(.g), type: .major),
        tuning: .standard,
        fretCount: 15
    )

    /// Strings are listed thickest first: 0 is low E, 5 is high E.
    @Test func gMajorMatchesReferenceChart() {
        #expect(gMajor.frets(withToneOnString: 5) == [0, 2, 3, 5, 7, 8, 10, 12, 14, 15])
        #expect(gMajor.frets(withToneOnString: 4) == [0, 1, 3, 5, 7, 8, 10, 12, 13, 15])
        #expect(gMajor.frets(withToneOnString: 3) == [0, 2, 4, 5, 7, 9, 11, 12, 14])
        #expect(gMajor.frets(withToneOnString: 2) == [0, 2, 4, 5, 7, 9, 10, 12, 14])
        #expect(gMajor.frets(withToneOnString: 1) == [0, 2, 3, 5, 7, 9, 10, 12, 14, 15])
        #expect(gMajor.frets(withToneOnString: 0) == [0, 2, 3, 5, 7, 8, 10, 12, 14, 15])
    }

    @Test func gMajorRootsMatchReferenceChart() {
        var rootFretsByString: [[Int]] = []
        for guitarString in gMajor.strings {
            var rootFrets: [Int] = []
            for (fret, tone) in guitarString.tonesByFret.enumerated() where tone?.isRoot == true {
                rootFrets.append(fret)
            }
            rootFretsByString.append(rootFrets)
        }
        #expect(rootFretsByString == [[3, 15], [10], [5], [0, 12], [8], [3, 15]])
    }

    @Test func dropDLowStringIsTheFifthOfG() {
        let layout = FretboardLayout(
            scale: Scale(root: NoteName(.g), type: .major),
            tuning: .dropD,
            fretCount: 12
        )
        let openTone = layout.strings[0].tonesByFret[0]
        #expect(openTone?.degree.label == "5")
    }

    @Test func layoutCoversOpenStringPlusEveryFret() {
        for guitarString in gMajor.strings {
            #expect(guitarString.tonesByFret.count == 16)
        }
    }
}

struct PitchMathTests {
    @Test func wrapHandlesNegatives() {
        #expect(PitchMath.wrap(-1) == 11)
        #expect(PitchMath.wrap(25) == 1)
    }

    @Test func signedDistanceChoosesShortestWay() {
        #expect(PitchMath.signedDistance(from: 11, to: 0) == 1)
        #expect(PitchMath.signedDistance(from: 0, to: 11) == -1)
    }
}

@MainActor
struct PracticeSessionTests {
    @Test func settingsSurviveARelaunch() throws {
        let suiteName = "ScalesTests-\(UUID().uuidString)"
        let defaults = try #require(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }

        let firstLaunch = PracticeSession(defaults: defaults)
        firstLaunch.settings.root = NoteName(.b, -1)
        firstLaunch.settings.scaleType = .dorian
        firstLaunch.settings.fretCount = 22

        let secondLaunch = PracticeSession(defaults: defaults)
        #expect(secondLaunch.settings == firstLaunch.settings)
        #expect(secondLaunch.scale.title == "B♭ Dorian")
    }

    @Test func tvStatusFollowsMirroringAndTVScreen() throws {
        let suiteName = "ScalesTests-\(UUID().uuidString)"
        let defaults = try #require(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }

        let session = PracticeSession(defaults: defaults)
        #expect(session.tvStatus == .disconnected)
        session.isPhoneMirrored = true
        #expect(session.tvStatus == .mirroringPhone)
        session.isTVConnected = true
        #expect(session.tvStatus == .showingBoard)
        session.isPhoneMirrored = false
        #expect(session.tvStatus == .showingBoard, "owning the TV screen outranks mirroring")
    }

    @Test func boardClipIsWrittenAsPlayableVideo() async throws {
        let width = 192
        let height = 108
        let context = try #require(
            CGContext(
                data: nil,
                width: width,
                height: height,
                bitsPerComponent: 8,
                bytesPerRow: 0,
                space: CGColorSpaceCreateDeviceRGB(),
                bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
            )
        )
        context.setFillColor(CGColor(red: 0.2, green: 0.3, blue: 0.5, alpha: 1))
        context.fill(CGRect(x: 0, y: 0, width: width, height: height))
        let image = try #require(context.makeImage())
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("clip-test.mp4")
        defer { try? FileManager.default.removeItem(at: url) }

        try BoardVideoWriter.write(image, to: url)

        let asset = AVURLAsset(url: url)
        let duration = try await asset.load(.duration)
        let tracks = try await asset.loadTracks(withMediaType: .video)
        #expect(Int(duration.seconds.rounded()) == Int(BoardVideoWriter.clipSeconds))
        let audioTracks = try await asset.loadTracks(withMediaType: .audio)
        #expect(audioTracks.count == 1)
        #expect(tracks.count == 1)
        let size = try await tracks[0].load(.naturalSize)
        #expect(Int(size.width) == width)
        #expect(Int(size.height) == height)
    }

    @Test func freshInstallOpensOnGMajor() throws {
        let suiteName = "ScalesTests-\(UUID().uuidString)"
        let defaults = try #require(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }

        let session = PracticeSession(defaults: defaults)
        #expect(session.scale.title == "G Major")
        #expect(session.settings.fretCount == 15)
    }
}

struct HTTPRequestTests {
    @Test func parsesMethodPathAndRange() {
        let request = HTTPRequest("GET /board-3.mp4 HTTP/1.1\r\nHost: x\r\nRange: bytes=100-199")
        #expect(request.method == "GET")
        #expect(request.path == "/board-3.mp4")
        #expect(request.byteRange(totalLength: 1000) == 100..<200)
    }

    @Test func openEndedAndSuffixRanges() {
        let openEnded = HTTPRequest("GET /a HTTP/1.1\r\nRange: bytes=900-")
        #expect(openEnded.byteRange(totalLength: 1000) == 900..<1000)
        let suffix = HTTPRequest("GET /a HTTP/1.1\r\nRange: bytes=-50")
        #expect(suffix.byteRange(totalLength: 1000) == 950..<1000)
        let past = HTTPRequest("GET /a HTTP/1.1\r\nRange: bytes=1000-")
        #expect(past.byteRange(totalLength: 1000) == nil)
        #expect(HTTPRequest("HEAD /a HTTP/1.1").byteRange(totalLength: 10) == nil)
    }
}

struct ClipCacheTests {
    @Test func sameSettingsGiveSameKeyAndChangesGiveNewOne() {
        let defaults = PracticeSettings()
        var dorian = PracticeSettings()
        dorian.scaleType = .dorian
        #expect(ClipCache.key(for: defaults) == ClipCache.key(for: PracticeSettings()))
        #expect(ClipCache.key(for: defaults) != ClipCache.key(for: dorian))
        #expect(ClipCache.key(for: defaults).count == 16)
    }

    @Test func onlyDefaultSettingsHaveABundledShortClip() {
        #expect(ClipCache.bundledShortClip(for: PracticeSettings()) != nil)
        var dorian = PracticeSettings()
        dorian.scaleType = .dorian
        #expect(ClipCache.bundledShortClip(for: dorian) == nil)
    }

    @Test func extendedClipRepeatsTheShortOne() async throws {
        let short = try #require(ClipCache.bundledShortClip(for: PracticeSettings()))
        let long = FileManager.default.temporaryDirectory.appendingPathComponent("long-test.mp4")
        defer { try? FileManager.default.removeItem(at: long) }

        try await ClipExtender.extend(short, to: long)

        let shortSeconds = try await AVURLAsset(url: short).load(.duration).seconds
        let longSeconds = try await AVURLAsset(url: long).load(.duration).seconds
        let expected = shortSeconds * Double(ClipExtender.repetitions)
        #expect(abs(longSeconds - expected) < 1)
        let audioTracks = try await AVURLAsset(url: long).loadTracks(withMediaType: .audio)
        #expect(audioTracks.count == 1)
    }

    @Test func storedClipsComeBack() {
        var settings = PracticeSettings()
        settings.root = NoteName(.d, -1)
        settings.scaleType = .wholeTone
        let clip = Data("pretend-mp4".utf8)
        ClipCache.store(clip, for: settings)
        #expect(ClipCache.clip(for: settings) == clip)
    }
}

/// Chord tables: every chord must be the right quality and spelled from its key.
struct DiatonicChordTests {
    private func chordNames(_ root: NoteName, _ type: ScaleType) -> [String] {
        var names: [String] = []
        for chord in Scale(root: root, type: type).diatonicTriads() {
            names.append(chord.triad.name)
        }
        return names
    }

    @Test func cMajorChords() {
        #expect(chordNames(NoteName(.c), .major) == ["C", "Dm", "Em", "F", "G", "Am", "B°"])
    }

    @Test func majorNumerals() {
        var numerals: [String] = []
        for chord in Scale(root: NoteName(.g), type: .major).diatonicTriads() {
            numerals.append(chord.numeral)
        }
        #expect(numerals == ["I", "ii", "iii", "IV", "V", "vi", "vii°"])
    }

    @Test func sharpAndFlatKeysAreSpelledFromTheirScale() {
        let fSharp = ["F♯", "G♯m", "A♯m", "B", "C♯", "D♯m", "E♯°"]
        #expect(chordNames(NoteName(.f, 1), .major) == fSharp)
        let dFlat = ["D♭", "E♭m", "Fm", "G♭", "A♭", "B♭m", "C°"]
        #expect(chordNames(NoteName(.d, -1), .major) == dFlat)
    }

    @Test func aMinorChordsAndNumerals() {
        #expect(chordNames(NoteName(.a), .naturalMinor) == ["Am", "B°", "C", "Dm", "Em", "F", "G"])
        var numerals: [String] = []
        for chord in Scale(root: NoteName(.a), type: .naturalMinor).diatonicTriads() {
            numerals.append(chord.numeral)
        }
        #expect(numerals == ["i", "ii°", "III", "iv", "v", "VI", "VII"])
    }

    @Test func minorRowsOfferHarmonicMinorStandIns() {
        let aMinor = KeyChordTable.minorRows()[0]
        #expect(aMinor.keyName == "A minor")
        #expect(aMinor.alternatives.count == 2)
        #expect(aMinor.alternatives[4]?.numeral == "V")
        #expect(aMinor.alternatives[4]?.triad.name == "E")
        #expect(aMinor.alternatives[6]?.numeral == "vii°")
        #expect(aMinor.alternatives[6]?.triad.name == "G♯°")
        #expect(KeyChordTable.majorRows()[0].alternatives.isEmpty)
    }

    @Test func tablesCoverTwelveKeysWithMatchingRelatives() {
        let majors = KeyChordTable.majorRows()
        let minors = KeyChordTable.minorRows()
        #expect(majors.count == 12)
        #expect(minors.count == 12)
        var majorSignatures: Set<String> = []
        for major in majors {
            majorSignatures.insert(major.key.keySignature)
            #expect(major.chords.count == 7)
        }
        var minorSignatures: Set<String> = []
        for minor in minors {
            minorSignatures.insert(minor.key.keySignature)
            #expect(minor.chords.count == 7)
        }
        #expect(majorSignatures.count == 12)
        #expect(majorSignatures == minorSignatures)
    }

    @Test func tableRowsAreSortedByKeyName() {
        var majorNames: [String] = []
        for row in KeyChordTable.majorRows() {
            majorNames.append(row.key.root.displayName)
        }
        #expect(majorNames == ["A♭", "A", "B♭", "B", "C", "D♭", "D", "E♭", "E", "F", "F♯", "G"])
        var minorNames: [String] = []
        for row in KeyChordTable.minorRows() {
            minorNames.append(row.key.root.displayName)
        }
        #expect(minorNames == ["A", "B♭", "B", "C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯"])
    }

    @Test func keySignatures() {
        #expect(Scale(root: NoteName(.c), type: .major).keySignature == "—")
        #expect(Scale(root: NoteName(.e), type: .major).keySignature == "4♯")
        #expect(Scale(root: NoteName(.b, -1), type: .major).keySignature == "2♭")
    }

    @Test func nonHeptatonicScalesHaveNoTriadTable() {
        #expect(Scale(root: NoteName(.a), type: .minorPentatonic).diatonicTriads().isEmpty)
    }
}
