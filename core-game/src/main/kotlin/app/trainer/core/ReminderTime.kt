package app.trainer.core

import java.time.Duration
import java.time.ZonedDateTime

object ReminderTime {
    /** Milliseconds from [now] until the next occurrence of [hour]:[minute] (strictly in the future). */
    fun delayUntilNextMillis(now: ZonedDateTime, hour: Int, minute: Int): Long {
        require(hour in 0..23 && minute in 0..59)
        fun at(base: ZonedDateTime) = base.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        var next = at(now)
        if (!next.isAfter(now)) next = at(now.plusDays(1))
        return Duration.between(now, next).toMillis()
    }
}
