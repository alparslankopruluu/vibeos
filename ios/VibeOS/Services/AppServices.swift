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
        analytics.log("app_open")
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
        id: "launch_annual_40",
        title: "40% Launch Offer",
        subtitle: "Premium yearly plan — limited time",
        expiry: Date().addingTimeInterval(6 * 60 * 60)
    )

    func configure(firebaseReady: Bool) {
        guard firebaseReady else { return }
        let remote = RemoteConfig.remoteConfig()
        remote.setDefaults([
            "daily_drop_theme_id": "sakura_night" as NSObject,
            "offer_title": "40% Launch Offer" as NSObject,
            "offer_subtitle": "Premium yearly plan — limited time" as NSObject,
            "offer_expiry_epoch": Date().addingTimeInterval(6 * 60 * 60).timeIntervalSince1970 as NSObject
        ])

        remote.fetchAndActivate { [weak self] _, error in
            guard error == nil, let self else { return }
            let id = remote["daily_drop_theme_id"].stringValue ?? "sakura_night"
            let title = remote["offer_title"].stringValue ?? "40% Launch Offer"
            let subtitle = remote["offer_subtitle"].stringValue ?? "Premium yearly plan — limited time"
            let rawExpiry = remote["offer_expiry_epoch"].numberValue.doubleValue
            let expiry = rawExpiry > 0 ? Date(timeIntervalSince1970: rawExpiry) : Date().addingTimeInterval(6 * 60 * 60)

            DispatchQueue.main.async {
                self.dailyDropThemeID = id
                self.limitedOffer = LimitedOffer(
                    id: "remote_offer",
                    title: title,
                    subtitle: subtitle,
                    expiry: expiry
                )
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
