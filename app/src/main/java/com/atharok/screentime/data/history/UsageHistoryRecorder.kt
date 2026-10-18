package com.atharok.screentime.data.history

import android.content.Context
import com.atharok.screentime.common.extensions.getApplicationName
import com.atharok.screentime.common.extensions.setTime
import com.atharok.screentime.common.utils.NUMBER_OF_DAYS
import com.atharok.screentime.data.statsTracker.UsageStatsTracker
import java.util.Calendar

/**
 * Saves the complete days of usage into [UsageHistoryStore] before Android deletes them.
 */
class UsageHistoryRecorder(
    private val context: Context,
    private val tracker: UsageStatsTracker,
    private val store: UsageHistoryStore
) {

    // Own calendar: the recorder also runs in background, concurrently with the UI.
    private val calendar: Calendar = Calendar.getInstance()

    /**
     * @param intervalsByPackage Intervals computed from the system events between [windowStart]
     * and [now], without ignoring any app (the filters are applied when the history is read).
     */
    fun record(
        intervalsByPackage: Map<String, List<Pair<Long, Long>>>,
        windowStart: Long,
        now: Long
    ) {
        store.save(
            // The intervals still open at [now] are excluded: they come from the last RESUMED
            // event without PAUSED, which can be an old one (e.g. before a reboot) and would be
            // saved as a session of several days.
            intervalsByPackage = intervalsByPackage.mapValues { (_, intervals) ->
                intervals.filter { (_, end: Long) -> end < now }
            },
            windowStart = windowStart,
            saveUntil = startOfDay(now, 0)
        ) { packageName: String ->
            UsageHistoryStore.StoredPackage(
                label = context.packageManager.getApplicationName(packageName),
                launchable = tracker.isLaunchable(packageName)
            )
        }
    }

    /**
     * Reads the system events of the last days and saves the complete days.
     */
    suspend fun recordFromSystemEvents() {
        if (!tracker.hasAppUsagePermission()) return

        val now = System.currentTimeMillis()
        val windowStart = startOfDay(now, -(NUMBER_OF_DAYS - 1))

        record(
            intervalsByPackage = tracker.calculateUsageIntervalsByPackage(
                startTimestamp = windowStart,
                endTimestamp = now,
                ignoreSystemApps = false,
                ignoredPackages = emptyList()
            ),
            windowStart = windowStart,
            now = now
        )
    }

    private fun startOfDay(timestamp: Long, dayOffset: Int): Long = synchronized(calendar) {
        calendar.timeInMillis = timestamp
        calendar.add(Calendar.DATE, dayOffset)
        calendar.setTime(0, 0, 0, 0)
        calendar.timeInMillis
    }
}
