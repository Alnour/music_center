import SwiftUI

/// The scale spelled out as a row of chips, each with its degree underneath,
/// so the player can read the formula (1 2 ♭3 …) as well as the note names.
/// A lesson rings the notes it is about: the one that was changed, and what it became;
/// the Scales screen rings the notes of the chord the player picked.
struct ScaleNotesRow: View {
    let scale: Scale
    let palette: FretboardPalette
    let noteSize: CGFloat
    var highlightedNotes: Set<NoteName> = []

    var body: some View {
        HStack(spacing: noteSize * 0.45) {
            ForEach(scale.tones, id: \.self) { tone in
                VStack(spacing: noteSize * 0.2) {
                    Text(tone.name.displayName)
                        .font(.system(size: noteSize, weight: .bold, design: .rounded))
                        .foregroundStyle(tone.isRoot ? palette.rootText : palette.toneText)
                        .frame(width: noteSize * 2, height: noteSize * 2)
                        .background(
                            Circle().fill(tone.isRoot ? palette.rootFill : palette.toneFill)
                        )
                        .overlay {
                            if highlightedNotes.contains(tone.name) {
                                Circle().strokeBorder(ChordRing.color, lineWidth: noteSize * 0.18)
                            }
                        }
                    Text(tone.degree.label)
                        .font(.system(size: noteSize * 0.6, weight: .semibold, design: .rounded))
                        .foregroundStyle(palette.secondaryText)
                }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityDescription)
    }

    private var accessibilityDescription: String {
        var names: [String] = []
        for tone in scale.tones {
            names.append(tone.name.displayName)
        }
        return "Notes: " + names.joined(separator: ", ")
    }
}
