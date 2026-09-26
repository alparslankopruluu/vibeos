import SwiftUI

@main
struct VibeOSApp: App {
    @UIApplicationDelegateAdaptor(VibeAppDelegate.self) private var appDelegate

    init() {
        AppServices.shared.configure()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .preferredColorScheme(.dark)
        }
    }
}
