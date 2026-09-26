package com.vibeos.app.services

import android.content.Context
import java.time.LocalDate

class EngagementTracker(context: Context) {
    private val prefs = context.getSharedPreferences("vibeos_engagement", Context.MODE_PRIVATE)

    val streak: Int
        get() = prefs.getInt(KEY_STREAK, 1).coerceAtLeast(1)

    fun recordOpen(): Int {
        val today = LocalDate.now().toEpochDay()
        val lastOpen = prefs.getLong(KEY_LAST_OPEN_DAY, Long.MIN_VALUE)
        val previousStreak = prefs.getInt(KEY_STREAK, 0)

        val nextStreak = when {
            lastOpen == today -> previousStreak.coerceAtLeast(1)
            lastOpen == today - 1 -> (previousStreak + 1).coerceAtLeast(1)
            else -> 1
        }

        prefs.edit()
            .putLong(KEY_LAST_OPEN_DAY, today)
            .putInt(KEY_STREAK, nextStreak)
            .apply()

        return nextStreak
    }

    private companion object {
        const val KEY_LAST_OPEN_DAY = "last_open_day"
        const val KEY_STREAK = "open_streak"
    }
}
