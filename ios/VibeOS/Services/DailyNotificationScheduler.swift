import UIKit
import UserNotifications

enum DailyNotificationScheduler {
    static func requestAndSchedule() {
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .badge, .sound]) { granted, _ in
            AppServices.shared.analytics.log(
                "notification_permission_result",
                params: ["granted": granted]
            )
            guard granted else { return }

            DispatchQueue.main.async {
                UIApplication.shared.registerForRemoteNotifications()
            }

            let content = UNMutableNotificationContent()
            content.title = "Your Daily Drop is ready ✦"
            content.body = "Open VibeOS to claim today's fresh theme."
            content.sound = .default
            content.userInfo = ["route": "discover"]

            var components = DateComponents()
            components.hour = 19

            let trigger = UNCalendarNotificationTrigger(
                dateMatching: components,
                repeats: true
            )
            let request = UNNotificationRequest(
                identifier: "vibeos.daily_drop",
                content: content,
                trigger: trigger
            )

            center.removePendingNotificationRequests(withIdentifiers: ["vibeos.daily_drop"])
            center.add(request)
            AppServices.shared.analytics.log("daily_drop_notification_scheduled")
        }
    }
}
