import SwiftUI

/// The choices inside a scale picker: the built-in scales grouped as a practice book groups
/// them, then the player's own scales. Every choice is tagged with a `ScaleReference`, so
/// the picker's selection must be one too.
struct ScalePickerOptions: View {
    let customScales: [CustomScale]

    var body: some View {
        ForEach(ScaleFamily.allCases) { family in
            Section(family.title) {
                ForEach(family.scales) { scaleType in
                    Text(scaleType.displayName).tag(ScaleReference.builtIn(scaleType))
                }
            }
        }
        if !customScales.isEmpty {
            Section("My Scales") {
                ForEach(customScales) { customScale in
                    Text(customScale.definition.name).tag(ScaleReference.custom(customScale.id))
                }
            }
        }
    }
}
