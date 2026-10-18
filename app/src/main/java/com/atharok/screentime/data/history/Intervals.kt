package com.atharok.screentime.data.history

/**
 * @return The parts of [intervals] that are inside [from, until).
 */
fun clipIntervals(
    intervals: List<Pair<Long, Long>>,
    from: Long,
    until: Long
): List<Pair<Long, Long>> = intervals.mapNotNull { (start: Long, end: Long) ->
    val clippedStart = maxOf(start, from)
    val clippedEnd = minOf(end, until)
    if (clippedEnd > clippedStart) clippedStart to clippedEnd else null
}

/**
 * Combines the saved history (used before [liveCutoff]) with the intervals computed from the
 * system events (used from [liveCutoff]), for the period [start, endExclusive).
 */
fun combineHistoryAndLiveIntervals(
    historyIntervals: Map<String, List<Pair<Long, Long>>>,
    liveIntervals: Map<String, List<Pair<Long, Long>>>,
    liveCutoff: Long,
    start: Long,
    endExclusive: Long
): Map<String, List<Pair<Long, Long>>> {
    val intervalsByPackage = mutableMapOf<String, MutableList<Pair<Long, Long>>>()

    historyIntervals.forEach { (packageName: String, intervals: List<Pair<Long, Long>>) ->
        val clippedIntervals = clipIntervals(intervals, start, minOf(endExclusive, liveCutoff))
        if (clippedIntervals.isNotEmpty()) {
            intervalsByPackage.getOrPut(packageName) { mutableListOf() }.addAll(clippedIntervals)
        }
    }

    liveIntervals.forEach { (packageName: String, intervals: List<Pair<Long, Long>>) ->
        val clippedIntervals = clipIntervals(intervals, maxOf(start, liveCutoff), endExclusive)
        if (clippedIntervals.isNotEmpty()) {
            intervalsByPackage.getOrPut(packageName) { mutableListOf() }.addAll(clippedIntervals)
        }
    }

    return intervalsByPackage
}
