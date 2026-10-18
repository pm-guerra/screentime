package com.atharok.screentime.data.history

import org.junit.Assert.assertEquals
import org.junit.Test

class IntervalsTest {

    @Test
    fun clipIntervals_keepsOnlyThePartsInsideTheRange() {
        val intervals = listOf(0L to 10L, 15L to 25L, 30L to 40L, 50L to 60L)

        assertEquals(
            listOf(15L to 25L, 30L to 40L, 50L to 55L),
            clipIntervals(intervals, from = 12L, until = 55L)
        )
        assertEquals(listOf(5L to 10L), clipIntervals(listOf(0L to 10L), from = 5L, until = 100L))
    }

    @Test
    fun clipIntervals_dropsTheIntervalsOutsideTheRange() {
        assertEquals(
            emptyList<Pair<Long, Long>>(),
            clipIntervals(listOf(0L to 10L, 20L to 30L), from = 10L, until = 20L)
        )
    }

    @Test
    fun combine_usesTheHistoryBeforeTheCutoffAndTheLiveIntervalsAfter() {
        val history = mapOf(
            "a" to listOf(0L to 10L, 90L to 110L),
            "b" to listOf(20L to 30L)
        )
        val live = mapOf(
            "a" to listOf(95L to 120L),
            "c" to listOf(50L to 60L, 130L to 140L)
        )

        val combined = combineHistoryAndLiveIntervals(
            historyIntervals = history,
            liveIntervals = live,
            liveCutoff = 100L,
            start = 5L,
            endExclusive = 135L
        )

        assertEquals(
            mapOf(
                // History until the cutoff, live data after: the overlap is not counted twice.
                "a" to listOf(5L to 10L, 90L to 100L, 100L to 120L),
                "b" to listOf(20L to 30L),
                // Live data before the cutoff is ignored.
                "c" to listOf(130L to 135L)
            ),
            combined
        )
    }

    @Test
    fun combine_withoutHistoryUsesOnlyTheLiveIntervals() {
        val combined = combineHistoryAndLiveIntervals(
            historyIntervals = emptyMap(),
            liveIntervals = mapOf("a" to listOf(10L to 20L)),
            liveCutoff = 0L,
            start = 0L,
            endExclusive = 100L
        )

        assertEquals(mapOf("a" to listOf(10L to 20L)), combined)
    }
}
