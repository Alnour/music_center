/// A scale the app already knows by name.
enum ScaleMatch: Hashable, Sendable {
    case builtIn(ScaleType)
    case custom(CustomScale)

    var name: String {
        switch self {
        case .builtIn(let type): type.displayName
        case .custom(let scale): scale.definition.name
        }
    }

    /// Where a lesson that ends on this scale should be filed.
    var reference: ScaleReference {
        switch self {
        case .builtIn(let type): .builtIn(type)
        case .custom(let scale): .custom(scale.id)
        }
    }
}

/// Names a scale from its sound, so an edit that lands on a known scale is called by name.
enum ScaleIdentifier {
    /// The known scale holding the same pitches above the root, or nil for a new scale.
    ///
    /// Built-in scales are checked first in picker order, so the major scale is called
    /// Major rather than Ionian and the natural minor Natural Minor rather than Aeolian.
    /// The player's saved scales come after. Pitches are compared rather than spellings,
    /// so a scale written with ♯4 is still recognised as one written with ♭5.
    static func match(_ definition: ScaleDefinition, among customScales: [CustomScale])
        -> ScaleMatch?
    {
        for family in ScaleFamily.allCases {
            for type in family.scales where type.definition.hasSamePitches(as: definition) {
                return .builtIn(type)
            }
        }
        for customScale in customScales
        where customScale.definition.hasSamePitches(as: definition) {
            return .custom(customScale)
        }
        return nil
    }
}
