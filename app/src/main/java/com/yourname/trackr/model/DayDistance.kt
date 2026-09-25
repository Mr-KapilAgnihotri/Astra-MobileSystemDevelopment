package com.yourname.trackr.model

import com.yourname.trackr.data.local.SessionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** One bar's worth of data for the weekly chart: a day label ("Mon") and the km covered that day. */
data class DayDistance(val dayLabel: String, val km: Float)

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

/**
 * Groups sessions by calendar day for the last 7 days (oldest first, today last) and sums
 * distanceMeters per day, in km. Shared by WeeklyStatsViewModel and HomeViewModel so both
 * the standalone weekly screen and the Home card compute this identically.
 */
fun buildLastSevenDayDistances(sessions: List<SessionEntity>): List<DayDistance> {
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val calendar = Calendar.getInstance()
    // Start of "today" (midnight) so day boundaries are calendar days, not rolling 24h windows.
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val todayStart = calendar.timeInMillis

    return (6 downTo 0).map { daysAgo ->
        val dayStart = todayStart - daysAgo * DAY_MILLIS
        val dayEnd = dayStart + DAY_MILLIS
        val kmForDay = sessions
            .filter { it.startTime in dayStart until dayEnd }
            .sumOf { it.distanceMeters.toDouble() }
            .toFloat() / 1000f
        DayDistance(dayFormat.format(Date(dayStart)), kmForDay)
    }
}
