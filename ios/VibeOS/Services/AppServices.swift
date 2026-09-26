import Foundation
import FirebaseCore
import FirebaseAnalytics
import FirebaseCrashlytics
import FirebaseRemoteConfig
import RevenueCat

final class AppServices {
    static let shared = AppServices()

    let analytics = AnalyticsService()
    let offers = OfferService()
    let purchases = PurchaseService()
    let engagement = EngagementTracker()

    private(set) var firebaseReady = false

    private init() {}

    func configure() {
        if FirebaseApp.app() == nil,
           let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
           let options = FirebaseOptions(contentsOfFile: path) {
            FirebaseApp.configure(options: options)
            firebaseReady = true
        } else {
            firebaseReady = FirebaseApp.app() != nil
        }

        analytics.enabled = firebaseReady
        offers.configure(firebaseReady: firebaseReady)
        purchases.configure()
        let streak = engagement.recordOpen()
        analytics.log("app_open", params: ["streak": streak])
    }
}

final class AnalyticsService {
    var enabled = false

    func log(_ name: String, params: [String: Any] = [:]) {
        guard enabled else { return }
        Analytics.logEvent(String(name.prefix(40)), parameters: params)
    }

    func record(_ error: Error, context: String) {
        guard enabled else { return }
        Crashlytics.crashlytics().setCustomValue(context, forKey: "vibe_context")
        Crashlytics.crashlytics().record(error: error)
    }
}

final class OfferService: ObservableObject {
    @Published private(set) var dailyDropThemeID = "sakura_night"
    @Published private(set) var limitedOffer = LimitedOffer(
        id: "standard",
        title: "VibeOS Premium",
        subtitle: "Unlock the complete VibeOS experience.",
        expiry: .distantPast
    )

    func configure(firebaseReady: Bool) {
        guard firebaseReady else { return }
        let remote = RemoteConfig.remoteConfig()
        remote.setDefaults([
            "daily_drop_theme_id": "sakura_night" as NSObject,
            "offer_title": "Limited-time Premium offer" as NSObject,
            "offer_subtitle": "Special annual plan" as NSObject,
            "offer_expiry_epoch": 0 as NSObject
        ])

        remote.fetchAndActivate { [weak self] _, error in
            guard error == nil, let self else { return }
            let id = remote["daily_drop_theme_id"].stringValue ?? "sakura_night"
            let title = remote["offer_title"].stringValue ?? "Limited-time Premium offer"
            let subtitle = remote["offer_subtitle"].stringValue ?? "Special annual plan"
            let rawExpiry = remote["offer_expiry_epoch"].numberValue.doubleValue
            let expiry = Date(timeIntervalSince1970: rawExpiry)

            DispatchQueue.main.async {
                self.dailyDropThemeID = id

                if rawExpiry > 0 && expiry > Date() {
                    self.limitedOffer = LimitedOffer(
                        id: "remote_offer",
                        title: title,
                        subtitle: subtitle,
                        expiry: expiry
                    )
                } else {
                    self.limitedOffer = LimitedOffer(
                        id: "standard",
                        title: "VibeOS Premium",
                        subtitle: "Unlock the complete VibeOS experience.",
                        expiry: .distantPast
                    )
                }
            }
        }
    }
}

final class PurchaseService {
    private(set) var configured = false

    func configure() {
        guard let key = Bundle.main.object(forInfoDictionaryKey: "VIBE_REVENUECAT_API_KEY") as? String,
              !key.isEmpty else { return }
        Purchases.configure(withAPIKey: key)
        configured = true
    }

    func purchaseAnnual(completion: @escaping (Bool, String?) -> Void) {
        guard configured else {
            completion(false, "RevenueCat key is not configured")
            return
        }

        Purchases.shared.getOfferings { offerings, error in
            guard let package = offerings?.current?.annual else {
                completion(false, error?.localizedDescription ?? "Annual package is missing")
                return
            }

            Purchases.shared.purchase(package: package) { _, customerInfo, error, cancelled in
                if cancelled {
                    completion(false, "cancelled")
                    return
                }
                completion(
                    customerInfo?.entitlements["premium"]?.isActive == true,
                    error?.localizedDescription
                )
            }
        }
    }

    func restore(completion: @escaping (Bool) -> Void) {
        guard configured else {
            completion(false)
            return
        }

        Purchases.shared.restorePurchases { info, _ in
            completion(info?.entitlements["premium"]?.isActive == true)
        }
    }
}
