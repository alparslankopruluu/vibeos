import Foundation

final class EngagementTracker {
    private let defaults = UserDefaults.standard
    private let lastOpenKey = "vibeos.engagement.lastOpen"
    private let streakKey = "vibeos.engagement.streak"

    var streak: Int {
        max(defaults.integer(forKey: streakKey), 1)
    }

    @discardableResult
    func recordOpen(now: Date = Date()) -> Int {
        let calendar = Calendar.current
        let previous = defaults.object(forKey: lastOpenKey) as? Date
        let currentStreak = defaults.integer(forKey: streakKey)

        let next: Int
        if let previous {
            if calendar.isDate(previous, inSameDayAs: now) {
                next = max(currentStreak, 1)
            } else {
                let previousDay = calendar.startOfDay(for: previous)
                let today = calendar.startOfDay(for: now)
                let delta = calendar.dateComponents([.day], from: previousDay, to: today).day
                next = delta == 1 ? max(currentStreak + 1, 1) : 1
            }
        } else {
            next = 1
        }

        defaults.set(now, forKey: lastOpenKey)
        defaults.set(next, forKey: streakKey)
        return next
    }
}
