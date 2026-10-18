package com.atharok.screentime.common.utils

import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.util.Calendar
import java.util.TimeZone

class DateTimeUtilsTest {

    companion object {
        // Daylight saving time ends on Sunday 25 October 2026 in Lisbon.
        private val timeZone: TimeZone = TimeZone.getTimeZone("Europe/Lisbon")

        @BeforeClass
        @JvmStatic
        fun setUp() {
            startKoin {
                modules(module { single { Calendar.getInstance(timeZone) } })
            }
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            stopKoin()
        }

        private fun timestamp(day: Int, hour: Int = 0, minute: Int = 0): Long =
            Calendar.getInstance(timeZone).apply {
                clear()
                set(2026, Calendar.OCTOBER, day, hour, minute)
            }.timeInMillis
    }

    private val now = timestamp(day = 28, hour = 10, minute = 30)

    @Test
    fun computeWeekIntervalFrom_returnsTheLast7CompleteDays() {
        assertEquals(
            timestamp(21) to timestamp(28) - 1,
            DateTimeUtils.computeWeekIntervalFrom(now, weekOffset = 0)
        )
    }

    @Test
    fun computeWeekIntervalFrom_movesBack7DaysPerOffset() {
        assertEquals(
            timestamp(14) to timestamp(21) - 1,
            DateTimeUtils.computeWeekIntervalFrom(now, weekOffset = 1)
        )
        assertEquals(
            timestamp(7) to timestamp(14) - 1,
            DateTimeUtils.computeWeekIntervalFrom(now, weekOffset = 2)
        )
    }

    @Test
    fun computeDayIntervalFrom_returnsTheWholeDay() {
        assertEquals(
            timestamp(28) to timestamp(29) - 1,
            DateTimeUtils.computeDayIntervalFrom(now, dayOffset = 0)
        )
        // 25 hours long because of the time change.
        assertEquals(
            timestamp(25) to timestamp(26) - 1,
            DateTimeUtils.computeDayIntervalFrom(now, dayOffset = 3)
        )
    }

    @Test
    fun calculateNumberOfDaysBetween_countsCalendarDays() {
        assertEquals(0, DateTimeUtils.calculateNumberOfDaysBetween(timestamp(28), now))
        assertEquals(1, DateTimeUtils.calculateNumberOfDaysBetween(timestamp(27, hour = 23), now))
        // Across the time change.
        assertEquals(10, DateTimeUtils.calculateNumberOfDaysBetween(timestamp(18, hour = 12), now))
    }
}
