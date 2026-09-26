import SwiftUI
import FirebaseCore
import FirebaseAppCheck
import FirebaseAnalytics
import FirebaseCrashlytics
import FirebaseRemoteConfig
import FirebaseAuth
import FirebaseFunctions
import FirebaseMessaging
import RevenueCat
import UserNotifications
import WidgetKit

struct Theme: Codable, Identifiable, Hashable {
    let id: String
    let title: String
    let category: String
    let subtitle: String
    let colors: [String]
    let world: String
    let premium: Bool
    var palette: [Color] { colors.map { Color(hex: $0) } }
}
struct ThemeCatalog: Codable { let version: Int; let themes: [Theme] }

extension Color {
    init(hex: String) {
        let n = UInt64(hex.replacingOccurrences(of: "#", with: ""), radix: 16) ?? 0
        self.init(red: Double((n >> 16) & 255) / 255, green: Double((n >> 8) & 255) / 255, blue: Double(n & 255) / 255)
    }
}

@MainActor final class VibeStore: ObservableObject {
    static let shared = VibeStore()
    @Published var themes: [Theme] = []
    @Published var route: Theme?
    @Published var premium = false
    @Published var packages: [RevenueCat.Package] = []
    @Published var offerEnd: Date?
    @Published var message: String?
    @Published var generatedURL: URL?
    @Published var generating = false
    @Published var notificationsEnabled = UserDefaults.standard.bool(forKey: "dailyNotifications")
    var selectedTheme: Theme { themes.first(where: { $0.id == UserDefaults(suiteName: "group.com.vibeos.app")?.string(forKey: "selectedTheme") }) ?? themes[0] }
    var daily: Theme { themes.first(where: { $0.id == (firebaseReady ? RemoteConfig.remoteConfig().configValue(forKey: "daily_drop_theme_id").stringValue : nil) }) ?? themes[Calendar.current.ordinality(of: .day, in: .year, for: .now)! % themes.count] }
    private var firebaseReady = false
    private var purchasesReady = false
    private init() {
        if let url = Bundle.main.url(forResource: "catalog", withExtension: "json"), let data = try? Data(contentsOf: url), let catalog = try? JSONDecoder().decode(ThemeCatalog.self, from: data) { themes = catalog.themes }
    }
    func configure() {
        if Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
            #if DEBUG
            AppCheck.setAppCheckProviderFactory(AppCheckDebugProviderFactory())
            #else
            AppCheck.setAppCheckProviderFactory(VibeAppCheckFactory())
            #endif
            FirebaseApp.configure(); firebaseReady = true
            RemoteConfig.remoteConfig().fetchAndActivate { _, _ in Task { await self.refreshCatalog() } }
            Messaging.messaging().delegate = PushBridge.shared
        }
        let key = (Bundle.main.object(forInfoDictionaryKey: "RevenueCatKey") as? String) ?? ""
        if !key.isEmpty && !key.contains("$(") {
            Purchases.configure(withAPIKey: key); purchasesReady = true
            Purchases.shared.getCustomerInfo { info, _ in Task { @MainActor in self.premium = info?.entitlements["premium"]?.isActive == true } }
        }
        Task { await refreshCatalog() }
    }
    func event(_ name: String, themeId: String? = nil) { if firebaseReady { Analytics.logEvent(name, parameters: themeId.map { ["theme_id": $0] }) } }
    func error(_ error: Error) { if firebaseReady { Crashlytics.crashlytics().record(error: error) } }
    func refreshCatalog() async {
        let remote = firebaseReady ? RemoteConfig.remoteConfig().configValue(forKey: "catalog_url").stringValue ?? "" : ""
        let urlText = remote.isEmpty ? (Bundle.main.object(forInfoDictionaryKey: "CatalogURL") as? String ?? "") : remote
        guard let url = URL(string: urlText), url.scheme == "https" else { return }
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let catalog = try JSONDecoder().decode(ThemeCatalog.self, from: data)
            if !catalog.themes.isEmpty { themes = catalog.themes }
        } catch { self.error(error) }
    }
    func select(_ theme: Theme) {
        UserDefaults(suiteName: "group.com.vibeos.app")?.set(theme.id, forKey: "selectedTheme")
        WidgetCenter.shared.reloadAllTimelines()
        event("theme_applied", themeId: theme.id)
    }
    func fetchPackages() {
        guard purchasesReady else { return }
        Purchases.shared.getOfferings { offerings, error in
            Task { @MainActor in
                let id = self.firebaseReady ? RemoteConfig.remoteConfig().configValue(forKey: "offer_id").stringValue ?? "" : ""
                let endText = self.firebaseReady ? RemoteConfig.remoteConfig().configValue(forKey: "offer_ends_at").stringValue ?? "" : ""
                let end = ISO8601DateFormatter().date(from: endText)
                let special = end != nil && end! > .now && !id.isEmpty ? offerings?.offering(identifier: id) : nil
                self.offerEnd = special == nil ? nil : end
                self.packages = (special ?? offerings?.current)?.availablePackages ?? []
                if special != nil { self.event("offer_viewed") }
                if let error { self.error(error) }
            }
        }
    }
    func purchase(_ package: RevenueCat.Package) {
        event("purchase_started")
        Purchases.shared.purchase(package: package) { _, info, error, cancelled in
            Task { @MainActor in
                if info?.entitlements["premium"]?.isActive == true { self.premium = true; self.event("purchase_completed") }
                else if let error, !cancelled { self.message = error.localizedDescription; self.error(error); self.event("purchase_failed") }
            }
        }
    }
    func restore() {
        guard purchasesReady else { return }
        Purchases.shared.restorePurchases { info, error in
            Task { @MainActor in
                self.premium = info?.entitlements["premium"]?.isActive == true
                if let error { self.message = error.localizedDescription }
            }
        }
    }
    func enableNotifications() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { allowed, _ in
            Task { @MainActor in
                self.notificationsEnabled = allowed
                UserDefaults.standard.set(allowed, forKey: "dailyNotifications")
                self.event("notification_permission_result", themeId: allowed ? "granted" : "denied")
                if allowed { UIApplication.shared.registerForRemoteNotifications(); self.scheduleDaily() }
            }
        }
    }
    func scheduleDaily() {
        let content = UNMutableNotificationContent(); content.title = "Today's drop is here"; content.body = "Open VibeOS to explore a new look"; content.sound = .default
        var at = DateComponents(); at.hour = 11; at.minute = 0
        UNUserNotificationCenter.current().add(UNNotificationRequest(identifier: "daily_drop", content: content, trigger: UNCalendarNotificationTrigger(dateMatching: at, repeats: true)))
    }
    func create(prompt: String, imageBase64: String? = nil) async {
        let description = prompt.isEmpty && imageBase64 != nil ? "Create a coordinated theme from my photo" : prompt
        guard firebaseReady, description.count >= 8 else { message = "Connect Firebase and describe your look."; return }
        generating = true; event("create_started")
        defer { generating = false }
        do {
            if Auth.auth().currentUser == nil { _ = try await Auth.auth().signInAnonymously() }
            var payload: [String: Any] = ["prompt": description]
            if let imageBase64 { payload["imageBase64"] = imageBase64 }
            let result = try await Functions.functions().httpsCallable("generateTheme").call(payload)
            guard let data = result.data as? [String: Any], let text = data["wallpaperUrl"] as? String,
                  let link = URL(string: text), link.scheme == "https" else { throw URLError(.badServerResponse) }
            generatedURL = link; event("create_completed")
        } catch { self.error(error); message = error.localizedDescription; event("create_failed") }
    }
}
final class VibeAppCheckFactory: NSObject, AppCheckProviderFactory {
    func createProvider(with app: FirebaseApp) -> AppCheckProvider? { AppAttestProvider(app: app) }
}

final class PushBridge: NSObject, MessagingDelegate {
    static let shared = PushBridge()
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) { /* Associate with user only after opt-in. */ }
}

final class NotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationDelegate()
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
        let id = response.notification.request.content.userInfo["theme_id"] as? String
        await MainActor.run { if let id { VibeStore.shared.route = VibeStore.shared.themes.first { $0.id == id } }; VibeStore.shared.event("notification_opened", themeId: id) }
    }
}

@main struct VibeOSApp: App {
    @StateObject private var store = VibeStore.shared
    init() { VibeStore.shared.configure(); UNUserNotificationCenter.current().delegate = NotificationDelegate.shared }
    var body: some Scene {
        WindowGroup { RootView().environmentObject(store).preferredColorScheme(.dark).onOpenURL { url in
            if url.scheme == "vibeos", url.host == "theme" { store.route = store.themes.first { $0.id == url.lastPathComponent } }
        } }
    }
}
