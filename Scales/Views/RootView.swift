// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import Observation
import SwiftUI

/// The app's four screens.
enum AppTab: Hashable {
    case scales
    case chords
    case transform
    case drums
}

/// Which screen is showing, shared so a lesson can send the player to the fretboard.
@MainActor
@Observable
final class AppNavigation {
    var selectedTab: AppTab = .scales
}

/// The app's four screens: the fretboard with its TV controls, the key chord tables, the
/// Transform screen where the player changes a scale and reads what it does to the chords,
/// and the drum trainer with its bouncing ball.
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
            Tab("Drums", systemImage: "metronome", value: AppTab.drums) {
                DrumTrainerView()
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
