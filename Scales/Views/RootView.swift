import Observation
import SwiftUI

/// The app's three screens.
enum AppTab: Hashable {
    case scales
    case chords
    case transform
}

/// Which screen is showing, shared so a lesson can send the player to the fretboard.
@MainActor
@Observable
final class AppNavigation {
    var selectedTab: AppTab = .scales
}

/// The app's three screens: the fretboard with its TV controls, the key chord tables, and
/// the Transform screen where the player changes a scale and reads what it does to the chords.
struct RootView: View {
    @Environment(PracticeSession.self) private var session
    @Environment(ScaleLibrary.self) private var library
    @State private var navigation = AppNavigation()

    var body: some View {
        @Bindable var navigation = navigation
        TabView(selection: $navigation.selectedTab) {
            Tab("Scales", systemImage: "guitars", value: AppTab.scales) {
                ContentView()
            }
            Tab("Chords", systemImage: "tablecells", value: AppTab.chords) {
                ChordTablesView()
            }
            Tab("Transform", systemImage: "wand.and.stars", value: AppTab.transform) {
                TransformView()
            }
        }
        .environment(navigation)
        // A saved scale on the board follows the library: renamed, it takes the new name;
        // deleted, the board goes back to the built-in scale chosen before it.
        .onChange(of: library.scales, initial: true) {
            session.refreshCustomScale(from: library)
        }
    }
}
