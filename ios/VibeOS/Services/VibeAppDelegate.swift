import UIKit
import UserNotifications
import FirebaseCore
import FirebaseMessaging

extension Notification.Name {
    static let vibeRoute = Notification.Name("vibeos.route")
}

final class VibeAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        if FirebaseApp.app() != nil {
            Messaging.messaging().delegate = self
        }
        return true
    }

    func requestPushPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, _ in
            AppServices.shared.analytics.log("notification_permission_result", params: ["granted": granted])
            if granted {
                DispatchQueue.main.async {
                    UIApplication.shared.registerForRemoteNotifications()
                }
            }
        }
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        if FirebaseApp.app() != nil {
            Messaging.messaging().apnsToken = deviceToken
        }
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        AppServices.shared.analytics.log("push_token_refreshed")
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        AppServices.shared.analytics.log("push_received_foreground")
        return [.banner, .sound, .badge]
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        let userInfo = response.notification.request.content.userInfo
        let route = (userInfo["route"] as? String) ?? "discover"
        AppServices.shared.analytics.log("push_open", params: ["route": route])
        NotificationCenter.default.post(
            name: .vibeRoute,
            object: nil,
            userInfo: ["route": route]
        )
    }
}
