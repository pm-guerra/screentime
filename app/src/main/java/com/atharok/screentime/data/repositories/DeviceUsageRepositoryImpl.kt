package com.atharok.screentime.data.repositories

import android.content.Context
import com.atharok.screentime.common.extensions.getApplicationIconSafety
import com.atharok.screentime.common.extensions.getApplicationName
import com.atharok.screentime.common.utils.DateTimeUtils
import com.atharok.screentime.data.history.UsageHistoryRecorder
import com.atharok.screentime.data.history.UsageHistoryStore
import com.atharok.screentime.data.history.combineHistoryAndLiveIntervals
import com.atharok.screentime.data.statsTracker.UsageStatsTracker
import com.atharok.screentime.domain.entities.usage.DailyAppUsage
import com.atharok.screentime.domain.entities.usage.DailyDeviceUsage
import com.atharok.screentime.domain.entities.usage.WeeklyAppUsage
import com.atharok.screentime.domain.entities.usage.WeeklyDeviceUsage
import com.atharok.screentime.domain.repositories.DeviceUsageRepository

class DeviceUsageRepositoryImpl(
    private val context: Context,
    private val tracker: UsageStatsTracker,
    private val historyStore: UsageHistoryStore,
    private val historyRecorder: UsageHistoryRecorder
): DeviceUsageRepository {

    // Permission

    override fun hasAppUsagePermission(): Boolean = tracker.hasAppUsagePermission()

    // Calculate

    // Intervals computed from the system events, without ignoring any app.
    private var liveIntervalsByPackage: Map<String, List<Pair<Long, Long>>>? = null
    // Before this timestamp, the saved history is used instead of the system events.
    private var liveCutoff: Long = 0L
    private var earliestDayTimestamp: Long = 0L

    private var ignoreSystemApps: Boolean = false
    private var ignoredPackages: List<String> = emptyList()
    private var storedPackages: Map<String, UsageHistoryStore.StoredPackage> = emptyMap()
    private var dateOfLastRefresh: String = ""

    override suspend fun calculateDeviceUsage(
        timeInterval: Pair<Long, Long>,
        ignoreSystemApps: Boolean,
        ignoredPackages: List<String>
    ) {
        val liveIntervals: Map<String, List<Pair<Long, Long>>> = tracker
            .calculateUsageIntervalsByPackage(
                startTimestamp = timeInterval.first,
                endTimestamp = timeInterval.second,
                ignoreSystemApps = false,
                ignoredPackages = emptyList()
            )

        // The history must never prevent the usage of the last days from being displayed.
        try {
            historyRecorder.record(
                intervalsByPackage = liveIntervals,
                windowStart = timeInterval.first,
                now = timeInterval.second
            )
        } catch (_: Exception) {}

        val (coveredFrom: Long?, coveredUntil: Long?) = try {
            historyStore.getCoveredFrom() to historyStore.getCoveredUntil()
        } catch (_: Exception) {
            null to null
        }

        // The first day of the system events may be incomplete (a session started the day
        // before is missed), so the saved history is preferred for that day.
        val secondLiveDay: Long = DateTimeUtils.truncateToDay(
            DateTimeUtils.addDaysToTimestamp(timeInterval.first, 1)
        )
        liveCutoff = (coveredUntil ?: timeInterval.first).coerceIn(timeInterval.first, secondLiveDay)
        earliestDayTimestamp = DateTimeUtils.truncateToDay(
            minOf(coveredFrom ?: secondLiveDay, secondLiveDay)
        )

        storedPackages = try {
            historyStore.getPackages()
        } catch (_: Exception) {
            emptyMap()
        }

        liveIntervalsByPackage = liveIntervals
        this.ignoreSystemApps = ignoreSystemApps
        this.ignoredPackages = ignoredPackages
        dateOfLastRefresh = DateTimeUtils.formatToFullDateTime(timeInterval.second)
    }

    override fun getEarliestDayTimestamp(): Long = earliestDayTimestamp

    override suspend fun getWeekDeviceUsage(
        timeInterval: Pair<Long, Long>
    ): WeeklyDeviceUsage {

        val (durationsByPackage, totalDurations) = calculateDurations(timeInterval)

        return WeeklyDeviceUsage(
            usages = durationsByPackage.mapValues { (key: String, value: Map<Long, LongArray>) ->
                WeeklyAppUsage(
                    packageName = key,
                    appName = getApplicationName(key),
                    appIcon = context.packageManager.getApplicationIconSafety(key),
                    durations = value
                )
            },
            timeInterval = timeInterval,
            dateOfLastRefresh = dateOfLastRefresh,
            totalDeviceUsageByDayAndHour = totalDurations
        )
    }

    override suspend fun getDayDeviceUsage(
        timeInterval: Pair<Long, Long>,
        dayTimestamp: Long
    ): DailyDeviceUsage {

        val (durationsByPackage, totalDurations) = calculateDurations(timeInterval)

        return DailyDeviceUsage(
            usages = durationsByPackage.mapValues { (key: String, value: Map<Long, LongArray>) ->
                DailyAppUsage(
                    packageName = key,
                    appName = getApplicationName(key),
                    appIcon = context.packageManager.getApplicationIconSafety(key),
                    durations = value[dayTimestamp] ?: LongArray(24)
                )
            },
            timeInterval = timeInterval,
            dateOfLastRefresh = dateOfLastRefresh,
            totalDeviceUsageByHour = totalDurations[dayTimestamp] ?: LongArray(24)
        )
    }

    /**
     * @return The durations by day and hour for each app, and for the device.
     */
    private suspend fun calculateDurations(
        timeInterval: Pair<Long, Long>
    ): Pair<Map<String, Map<Long, LongArray>>, Map<Long, LongArray>> {

        val liveIntervals = liveIntervalsByPackage
            ?: throw Exception("DeviceUsageRepositoryImpl.calculateDeviceUsage() not called.")

        val start: Long = timeInterval.first
        val endExclusive: Long = timeInterval.second + 1

        val historyIntervals: Map<String, List<Pair<Long, Long>>> = if (start < liveCutoff) {
            historyStore.getIntervals(start, minOf(endExclusive, liveCutoff))
        } else emptyMap()

        val usageIntervalsByPackage = combineHistoryAndLiveIntervals(
            historyIntervals = historyIntervals,
            liveIntervals = liveIntervals,
            liveCutoff = liveCutoff,
            start = start,
            endExclusive = endExclusive
        ).filterKeys { isDisplayed(it) }

        val durationsByPackage = tracker.getDurationPerPackageByDayAndHour(
            startTimestamp = start,
            endTimestamp = timeInterval.second,
            usageIntervalsByPackage = usageIntervalsByPackage
        )

        val totalDurations = tracker.getTotalDurationByDayAndHour(
            startTimestamp = start,
            endTimestamp = timeInterval.second,
            usageIntervalsByPackage = usageIntervalsByPackage
        )

        return durationsByPackage to totalDurations
    }

    /**
     * Same filters as [UsageStatsTracker.calculateUsageIntervalsByPackage], also applied to the
     * apps that are no longer installed.
     */
    private fun isDisplayed(packageName: String): Boolean {
        if (ignoredPackages.contains(packageName)) return false
        if (ignoreSystemApps) {
            return tracker.isLaunchable(packageName) || storedPackages[packageName]?.launchable == true
        }
        return true
    }

    private fun getApplicationName(packageName: String): String =
        context.packageManager.getApplicationName(packageName)
            ?: storedPackages[packageName]?.label
            ?: ""
}
