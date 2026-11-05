/// What goes inside each note circle.
enum NoteLabelStyle: String, CaseIterable, Codable, Identifiable, Sendable {
    case noteNames
    case degrees
    case none

    var id: String { rawValue }

    var title: String {
        switch self {
        case .noteNames: "Notes"
        case .degrees: "Degrees"
        case .none: "Dots"
        }
    }
}

/// Everything the player chooses, saved between launches so the app reopens on the
/// scale they were last practising.
struct PracticeSettings: Codable, Equatable, Sendable {
    var root = NoteName(.g)
    /// The built-in scale, and the one the board falls back to if a saved scale is deleted.
    var scaleType: ScaleType = .major
    var tuning: Tuning = .standard
    var fretCount = 15
    var labelStyle: NoteLabelStyle = .noteNames
    var isLeftHanded = false
    /// Matches the reference chart and tab notation: the thin high E string at the top.
    var thinStringOnTop = true
    /// A dark board is easier on the eyes on a big TV in a dim room.
    var tvUsesDarkTheme = true
    /// One of the player's own scales, shown instead of `scaleType` while it is set.
    ///
    /// A full copy is kept rather than an id, for three reasons: settings saved before
    /// custom scales existed still load (the field is optional, so a missing key is fine);
    /// the TV clip cache, keyed by these settings, makes a new clip whenever the scale's
    /// notes or name change; and the TV screen can draw the board without the library.
    var customScale: CustomScale?

    static let fretCountChoices = [12, 15, 17, 22, 24]

    /// The scale on the board, for pickers to select.
    var scaleReference: ScaleReference {
        if let customScale {
            return .custom(customScale.id)
        }
        return .builtIn(scaleType)
    }

    /// The name and formula of the scale on the board.
    var scaleDefinition: ScaleDefinition {
        customScale?.definition ?? scaleType.definition
    }
}
