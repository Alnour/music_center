import UIKit

/// Routes the TV screen to its own scene and controls which orientations the phone allows.
@MainActor
final class AppDelegate: NSObject, UIApplicationDelegate {
    /// Narrowed to landscape while the full-screen board is shown on the phone.
    static var allowedOrientations: UIInterfaceOrientationMask = .allButUpsideDown

    /// When AirPlay screen mirroring starts, iOS asks for a scene for the TV. Answering
    /// with our own delegate makes the TV show a dedicated full-screen fretboard while
    /// the phone keeps the controls, instead of a shrunken copy of the phone screen.
    /// Phone scenes get no delegate class so SwiftUI keeps managing them.
    ///
    /// The TV scene is also declared in Info.plist (see project.yml): iOS 26 only offers
    /// the external screen to apps that list it there, and this method alone is not enough.
    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        let role = connectingSceneSession.role
        let configuration = UISceneConfiguration(name: nil, sessionRole: role)
        if role == .windowExternalDisplayNonInteractive {
            configuration.delegateClass = TVSceneDelegate.self
        }
        return configuration
    }

    func application(
        _ application: UIApplication,
        supportedInterfaceOrientationsFor window: UIWindow?
    ) -> UIInterfaceOrientationMask {
        Self.allowedOrientations
    }
}

/// Rotates the phone into landscape for the full-screen board and back again afterwards.
@MainActor
enum OrientationLock {
    /// Whether the app can turn the screen itself. iPadOS 26 ignores orientation requests
    /// because every iPad window can be resized, so on iPad the player turns the device
    /// and the full-screen board asks them to (see `FullScreenBoardView`). A Mac window
    /// never rotates either.
    static var canRotateScreen: Bool {
        DeviceKind.current == .phone
    }

    static func lock(_ orientations: UIInterfaceOrientationMask) {
        guard canRotateScreen else { return }
        AppDelegate.allowedOrientations = orientations
        for scene in UIApplication.shared.connectedScenes {
            guard let windowScene = scene as? UIWindowScene else { continue }
            guard windowScene.session.role == .windowApplication else { continue }
            let preferences = UIWindowScene.GeometryPreferences.iOS(
                interfaceOrientations: orientations
            )
            windowScene.requestGeometryUpdate(preferences)
            let rootController = windowScene.keyWindow?.rootViewController
            rootController?.setNeedsUpdateOfSupportedInterfaceOrientations()
        }
    }

    static func unlock() {
        lock(.allButUpsideDown)
    }
}
