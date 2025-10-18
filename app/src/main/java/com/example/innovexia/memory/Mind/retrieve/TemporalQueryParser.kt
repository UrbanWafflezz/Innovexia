package com.example.innovexia.memory.Mind.retrieve

import java.util.Calendar
import java.util.Locale

/**
 * Parses temporal expressions from user queries and converts them to timestamp ranges.
 * Handles queries like "yesterday", "last week", "Monday", "December 15", etc.
 */
object TemporalQueryParser {

    /**
     * Result of parsing a temporal query
     */
    data class TemporalQuery(
        val startTimeMs: Long,
        val endTimeMs: Long,
        val description: String  // Human-readable description like "yesterday" or "last Monday"
    )

    /**
     * Detects if a query contains temporal expressions and parses the time range.
     * Returns null if no temporal expression is detected.
     */
    fun parse(query: String): TemporalQuery? {
        val lowerQuery = query.lowercase(Locale.getDefault())
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        // Pattern 1: "today"
        if (lowerQuery.contains("today") || lowerQuery.contains("this morning") ||
            lowerQuery.contains("this afternoon") || lowerQuery.contains("this evening")) {
            val (start, end) = getTodayRange(calendar)
            return TemporalQuery(start, end, "today")
        }

        // Pattern 2: "yesterday"
        if (lowerQuery.contains("yesterday")) {
            val (start, end) = getYesterdayRange(calendar)
            return TemporalQuery(start, end, "yesterday")
        }

        // Pattern 3: "last week" or "this week"
        if (lowerQuery.contains("last week")) {
            val (start, end) = getLastWeekRange(calendar)
            return TemporalQuery(start, end, "last week")
        }
        if (lowerQuery.contains("this week")) {
            val (start, end) = getThisWeekRange(calendar)
            return TemporalQuery(start, end, "this week")
        }

        // Pattern 4: "last month" or "this month"
        if (lowerQuery.contains("last month")) {
            val (start, end) = getLastMonthRange(calendar)
            return TemporalQuery(start, end, "last month")
        }
        if (lowerQuery.contains("this month")) {
            val (start, end) = getThisMonthRange(calendar)
            return TemporalQuery(start, end, "this month")
        }

        // Pattern 5: Day of week (Monday, Tuesday, etc.)
        val dayOfWeek = parseDayOfWeek(lowerQuery)
        if (dayOfWeek != null) {
            val (start, end) = getLastDayOfWeekRange(calendar, dayOfWeek)
            val dayName = getDayName(dayOfWeek)
            return TemporalQuery(start, end, dayName)
        }

        // Pattern 6: "last N days" or "past N days"
        val daysAgo = parseLastNDays(lowerQuery)
        if (daysAgo != null) {
            val (start, end) = getLastNDaysRange(calendar, daysAgo)
            return TemporalQuery(start, end, "last $daysAgo days")
        }

        // Pattern 7: Month names (December, Jan, etc.)
        val monthQuery = parseMonth(lowerQuery, calendar)
        if (monthQuery != null) {
            return monthQuery
        }

        // Pattern 8: Specific date patterns (e.g., "15th", "December 15", "Dec 15")
        val dateQuery = parseSpecificDate(lowerQuery, calendar)
        if (dateQuery != null) {
            return dateQuery
        }

        // Pattern 9: Time-based queries ("this morning", "this afternoon", etc.)
        val timeOfDayQuery = parseTimeOfDay(lowerQuery, calendar)
        if (timeOfDayQuery != null) {
            return timeOfDayQuery
        }

        return null
    }

    /**
     * Get start and end timestamps for today (00:00:00 to 23:59:59)
     */
    private fun getTodayRange(calendar: Calendar): Pair<Long, Long> {
        val start = calendar.clone() as Calendar
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)

        val end = calendar.clone() as Calendar
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        end.set(Calendar.MILLISECOND, 999)

        return Pair(start.timeInMillis, end.timeInMillis)
    }

    /**
     * Get start and end timestamps for yesterday
     */
    private fun getYesterdayRange(calendar: Calendar): Pair<Long, Long> {
        val start = calendar.clone() as Calendar
        start.add(Calendar.DAY_OF_YEAR, -1)
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)

        val end = calendar.clone() as Calendar
        end.add(Calendar.DAY_OF_YEAR, -1)
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        end.set(Calendar.MILLISECOND, 999)

        return Pair(start.timeInMillis, end.timeInMillis)
    }

    /**
     * Get start and end timestamps for last week (7 days ago)
     */
    private fun getLastWeekRange(calendar: Calendar): Pair<Long, Long> {
        val start = calendar.clone() as Calendar
        start.add(Calendar.WEEK_OF_YEAR, -1)
        start.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)

        val end = start.clone() as Calendar
        end.add(Calendar.DAY_OF_YEAR, 6)
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        end.set(Calendar.MILLISECOND, 999)

        return Pair(start.timeInMillis, end.timeInMillis)
    }

    /**
     * Get start and end timestamps for this week
     */
    private fun getThisWeekRange(calendar: Calendar): Pair<Long, Long> {
        val start = calendar.clone() as Calendar
        start.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)

        val end = calendar.clone() as Calendar
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        end.set(Calendar.MILLISECOND, 999)

        return Pair(start.timeInMillis, end.timeInMillis)
    }

    /**
     * Get start and end timestamps for last month
     */
    private fun getLastMonthRange(calendar: Calendar): Pair<Long, Long> {
        val start = calendar.clone() as Calendar
        start.add(Calendar.MONTH, -1)
        start.set(Calendar.DAY_OF_MONTH, 1)
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)

        val end = start.clone() as Calendar
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        end.set(Calendar.MILLISECOND, 999)

        return Pair(start.timeInMillis, end.timeInMillis)
    }

    /**
     * Get start and end timestamps for this month
     */
    private fun getThisMonthRange(calendar: Calendar): Pair<Long, Long> {
        val start = calendar.clone() as Calendar
        start.set(Calendar.DAY_OF_MONTH, 1)
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)

        val end = calendar.clone() as Calendar
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        end.set(Calendar.MILLISECOND, 999)

        return Pair(start.timeInMillis, end.timeInMillis)
    }

    /**
     * Get start and end timestamps for the last occurrence of a specific day of week
     */
    private fun getLastDayOfWeekRange(calendar: Calendar, targetDayOfWeek: Int): Pair<Long, Long> {
        val start = calendar.clone() as Calendar
        val currentDay = start.get(Calendar.DAY_OF_WEEK)

        // Calculate days to subtract to get to target day
        var daysToSubtract = currentDay - targetDayOfWeek
        if (daysToSubtract <= 0) {
            daysToSubtract += 7
        }

        start.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)

        val end = start.clone() as Calendar
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        end.set(Calendar.MILLISECOND, 999)

        return Pair(start.timeInMillis, end.timeInMillis)
    }

    /**
     * Get start and end timestamps for last N days
     */
    private fun getLastNDaysRange(calendar: Calendar, days: Int): Pair<Long, Long> {
        val end = calendar.clone() as Calendar
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        end.set(Calendar.MILLISECOND, 999)

        val start = calendar.clone() as Calendar
        start.add(Calendar.DAY_OF_YEAR, -days)
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)

        return Pair(start.timeInMillis, end.timeInMillis)
    }

    /**
     * Parse day of week from query (returns Calendar constant or null)
     */
    private fun parseDayOfWeek(query: String): Int? {
        return when {
            query.contains("monday") || query.contains("mon") -> Calendar.MONDAY
            query.contains("tuesday") || query.contains("tue") -> Calendar.TUESDAY
            query.contains("wednesday") || query.contains("wed") -> Calendar.WEDNESDAY
            query.contains("thursday") || query.contains("thu") -> Calendar.THURSDAY
            query.contains("friday") || query.contains("fri") -> Calendar.FRIDAY
            query.contains("saturday") || query.contains("sat") -> Calendar.SATURDAY
            query.contains("sunday") || query.contains("sun") -> Calendar.SUNDAY
            else -> null
        }
    }

    /**
     * Get day name from Calendar constant
     */
    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Unknown"
        }
    }

    /**
     * Parse "last N days" or "past N days" pattern
     */
    private fun parseLastNDays(query: String): Int? {
        val regex = Regex("""(?:last|past)\s+(\d+)\s+days?""")
        val match = regex.find(query)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    /**
     * Parse month name from query
     */
    private fun parseMonth(query: String, calendar: Calendar): TemporalQuery? {
        val months = mapOf(
            "january" to 0, "jan" to 0,
            "february" to 1, "feb" to 1,
            "march" to 2, "mar" to 2,
            "april" to 3, "apr" to 3,
            "may" to 4,
            "june" to 5, "jun" to 5,
            "july" to 6, "jul" to 6,
            "august" to 7, "aug" to 7,
            "september" to 8, "sep" to 8, "sept" to 8,
            "october" to 9, "oct" to 9,
            "november" to 10, "nov" to 10,
            "december" to 11, "dec" to 11
        )

        for ((name, monthIndex) in months) {
            if (query.contains(name)) {
                val start = calendar.clone() as Calendar
                start.set(Calendar.MONTH, monthIndex)
                start.set(Calendar.DAY_OF_MONTH, 1)
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)

                // If month is in the future, assume last year
                if (start.timeInMillis > calendar.timeInMillis) {
                    start.add(Calendar.YEAR, -1)
                }

                val end = start.clone() as Calendar
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
                end.set(Calendar.HOUR_OF_DAY, 23)
                end.set(Calendar.MINUTE, 59)
                end.set(Calendar.SECOND, 59)
                end.set(Calendar.MILLISECOND, 999)

                return TemporalQuery(start.timeInMillis, end.timeInMillis, name.capitalize())
            }
        }

        return null
    }

    /**
     * Parse specific date patterns like "15th", "December 15", "Dec 15"
     */
    private fun parseSpecificDate(query: String, calendar: Calendar): TemporalQuery? {
        // Pattern: "December 15" or "Dec 15"
        val monthDayPattern = Regex("""(january|jan|february|feb|march|mar|april|apr|may|june|jun|july|jul|august|aug|september|sep|sept|october|oct|november|nov|december|dec)\s+(\d{1,2})""")
        val monthDayMatch = monthDayPattern.find(query)

        if (monthDayMatch != null) {
            val monthName = monthDayMatch.groupValues[1]
            val day = monthDayMatch.groupValues[2].toIntOrNull() ?: return null

            val months = mapOf(
                "january" to 0, "jan" to 0,
                "february" to 1, "feb" to 1,
                "march" to 2, "mar" to 2,
                "april" to 3, "apr" to 3,
                "may" to 4,
                "june" to 5, "jun" to 5,
                "july" to 6, "jul" to 6,
                "august" to 7, "aug" to 7,
                "september" to 8, "sep" to 8, "sept" to 8,
                "october" to 9, "oct" to 9,
                "november" to 10, "nov" to 10,
                "december" to 11, "dec" to 11
            )

            val monthIndex = months[monthName] ?: return null

            val start = calendar.clone() as Calendar
            start.set(Calendar.MONTH, monthIndex)
            start.set(Calendar.DAY_OF_MONTH, day)
            start.set(Calendar.HOUR_OF_DAY, 0)
            start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0)
            start.set(Calendar.MILLISECOND, 0)

            // If date is in the future, assume last year
            if (start.timeInMillis > calendar.timeInMillis) {
                start.add(Calendar.YEAR, -1)
            }

            val end = start.clone() as Calendar
            end.set(Calendar.HOUR_OF_DAY, 23)
            end.set(Calendar.MINUTE, 59)
            end.set(Calendar.SECOND, 59)
            end.set(Calendar.MILLISECOND, 999)

            return TemporalQuery(start.timeInMillis, end.timeInMillis, "$monthName $day")
        }

        // Pattern: "15th", "1st", "22nd", etc. (assumes current month)
        val dayOnlyPattern = Regex("""(\d{1,2})(?:st|nd|rd|th)""")
        val dayOnlyMatch = dayOnlyPattern.find(query)

        if (dayOnlyMatch != null) {
            val day = dayOnlyMatch.groupValues[1].toIntOrNull() ?: return null

            val start = calendar.clone() as Calendar
            start.set(Calendar.DAY_OF_MONTH, day)
            start.set(Calendar.HOUR_OF_DAY, 0)
            start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0)
            start.set(Calendar.MILLISECOND, 0)

            // If date is in the future, assume last month
            if (start.timeInMillis > calendar.timeInMillis) {
                start.add(Calendar.MONTH, -1)
            }

            val end = start.clone() as Calendar
            end.set(Calendar.HOUR_OF_DAY, 23)
            end.set(Calendar.MINUTE, 59)
            end.set(Calendar.SECOND, 59)
            end.set(Calendar.MILLISECOND, 999)

            return TemporalQuery(start.timeInMillis, end.timeInMillis, "the ${day}th")
        }

        return null
    }

    /**
     * Parse time of day queries like "this morning", "this afternoon"
     */
    private fun parseTimeOfDay(query: String, calendar: Calendar): TemporalQuery? {
        val start = calendar.clone() as Calendar
        val end = calendar.clone() as Calendar

        when {
            query.contains("this morning") || query.contains("morning") -> {
                start.set(Calendar.HOUR_OF_DAY, 5)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)

                end.set(Calendar.HOUR_OF_DAY, 11)
                end.set(Calendar.MINUTE, 59)
                end.set(Calendar.SECOND, 59)
                end.set(Calendar.MILLISECOND, 999)

                return TemporalQuery(start.timeInMillis, end.timeInMillis, "this morning")
            }
            query.contains("this afternoon") || query.contains("afternoon") -> {
                start.set(Calendar.HOUR_OF_DAY, 12)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)

                end.set(Calendar.HOUR_OF_DAY, 17)
                end.set(Calendar.MINUTE, 59)
                end.set(Calendar.SECOND, 59)
                end.set(Calendar.MILLISECOND, 999)

                return TemporalQuery(start.timeInMillis, end.timeInMillis, "this afternoon")
            }
            query.contains("this evening") || query.contains("evening") || query.contains("tonight") -> {
                start.set(Calendar.HOUR_OF_DAY, 18)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)

                end.set(Calendar.HOUR_OF_DAY, 23)
                end.set(Calendar.MINUTE, 59)
                end.set(Calendar.SECOND, 59)
                end.set(Calendar.MILLISECOND, 999)

                return TemporalQuery(start.timeInMillis, end.timeInMillis, "this evening")
            }
        }

        return null
    }
}
