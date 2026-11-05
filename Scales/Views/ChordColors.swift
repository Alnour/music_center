import SwiftUI

extension TriadQuality {
    /// One colour per chord quality, shared by the key tables and the lessons so major,
    /// minor and diminished read the same at a glance everywhere in the app.
    var tint: Color {
        switch self {
        case .major: .primary
        case .minor: .blue
        case .diminished, .augmented: .orange
        }
    }
}
