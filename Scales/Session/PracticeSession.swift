import Foundation
import Observation

/// The single source of truth shared by the phone scene and the TV scene.
///
/// Both scenes live in the same process, so when the player changes the scale on the
/// phone the TV redraws immediately; no networking is involved.
@MainActor
@Observable
final class PracticeSession {
    static let shared = PracticeSession(defaults: .standard)

    var settings: PracticeSettings {
        didSet { save() }
    }

    /// True while iOS has given the app its own screen on the TV through AirPlay.
    var isTVConnected = false

    /// True while the phone's screen is being mirrored or recorded. With AirPlay this
    /// turns on a moment before iOS offers the app the TV screen; if the TV screen never
    /// arrives, the TV is showing a plain copy of the phone.
    var isPhoneMirrored = false

    /// Sends the board to a TV as AirPlay video when the player picks one in the app.
    let caster = AirPlayCaster()

    var tvStatus: TVStatus {
        if isTVConnected { return .showingBoard }
        if let error = caster.playbackError { return .castFailed(reason: error) }
        if caster.isPlayingOnTV { return .castingVideo(receiver: caster.receiverName) }
        if let receiver = caster.receiverName { return .castInterrupted(receiver: receiver) }
        if isPhoneMirrored { return .mirroringPhone }
        return .disconnected
    }

    @ObservationIgnored private let defaults: UserDefaults
    private static let settingsKey = "practiceSettings"

    init(defaults: UserDefaults) {
        self.defaults = defaults
        self.settings = PracticeSession.loadSettings(from: defaults)
    }

    var scale: Scale {
        Scale(root: settings.root, definition: settings.scaleDefinition)
    }

    /// Puts a scale on the board, in a new key when one is given. A built-in scale clears
    /// any saved scale; a saved scale is copied in from the library, and an id the library
    /// doesn't know changes nothing. Settings are written once, so the TV redraws once.
    func choose(_ reference: ScaleReference, root: NoteName? = nil, from library: ScaleLibrary) {
        var updated = settings
        if let root {
            updated.root = root
        }
        switch reference {
        case .builtIn(let type):
            updated.scaleType = type
            updated.customScale = nil
        case .custom(let id):
            guard let customScale = library.scale(id: id) else { return }
            updated.customScale = customScale
        }
        settings = updated
    }

    /// Keeps the board's copy of a saved scale in step with the library: a renamed scale
    /// takes its new name, and a deleted one gives way to the built-in scale chosen before.
    func refreshCustomScale(from library: ScaleLibrary) {
        guard let current = settings.customScale else { return }
        guard let stored = library.scale(id: current.id) else {
            settings.customScale = nil
            return
        }
        if stored != current {
            settings.customScale = stored
        }
    }

    var layout: FretboardLayout {
        FretboardLayout(scale: scale, tuning: settings.tuning, fretCount: settings.fretCount)
    }

    private func save() {
        guard let data = try? JSONEncoder().encode(settings) else { return }
        defaults.set(data, forKey: Self.settingsKey)
    }

    /// Falls back to defaults when nothing is stored yet or the stored shape is outdated.
    private static func loadSettings(from defaults: UserDefaults) -> PracticeSettings {
        guard let data = defaults.data(forKey: settingsKey) else { return PracticeSettings() }
        let decoded = try? JSONDecoder().decode(PracticeSettings.self, from: data)
        return decoded ?? PracticeSettings()
    }
}

/// What the TV is showing, as far as the app can tell.
enum TVStatus: Equatable, Sendable {
    /// No mirroring session is running.
    case disconnected
    /// The phone is being mirrored but iOS kept copying the phone screen to the TV.
    case mirroringPhone
    /// The app owns the TV screen and is drawing the large board there.
    case showingBoard
    /// The board clip is playing on a TV picked from the in-app AirPlay menu.
    case castingVideo(receiver: String?)
    /// The TV accepted the connection but could not play the clip.
    case castFailed(reason: String)
    /// A TV is still chosen in the AirPlay menu but it stopped playing the board.
    case castInterrupted(receiver: String)
}
