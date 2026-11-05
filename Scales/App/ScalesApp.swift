import SwiftUI

@main
struct ScalesApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @State private var session = PracticeSession.shared
    @State private var library = ScaleLibrary.shared

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(session)
                .environment(library)
        }
    }
}
