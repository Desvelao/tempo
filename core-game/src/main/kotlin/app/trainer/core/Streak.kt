package app.trainer.core

import java.time.LocalDate

object Streak {
    /**
     * Consecutive days with at least one completed session, ending today. A streak whose last
     * day is yesterday is still alive (today's session is pending); older than that it is 0.
     */
    fun compute(days: Collection<LocalDate>, today: LocalDate): Int {
        val set = days.toHashSet()
        var day = when {
            today in set -> today
            today.minusDays(1) in set -> today.minusDays(1)
            else -> return 0
        }
        var count = 0
        while (day in set) { count++; day = day.minusDays(1) }
        return count
    }
}
