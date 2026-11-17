/// Which instrument the board is drawn for.
///
/// The guitar gets the fretboard; the keyboard gets one piano octave. The choice only
/// changes how the scale is pictured, so the scale, root and labels are shared.
enum Instrument: String, CaseIterable, Codable, Identifiable, Sendable {
    case guitar
    case keyboard

    var id: String { rawValue }

    var title: String {
        switch self {
        case .guitar: "Guitar"
        case .keyboard: "Keyboard"
        }
    }
}
