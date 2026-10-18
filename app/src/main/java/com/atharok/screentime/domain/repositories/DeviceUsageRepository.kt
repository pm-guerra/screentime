package com.atharok.screentime.domain.repositories

import com.atharok.screentime.domain.entities.usage.DailyDeviceUsage
import com.atharok.screentime.domain.entities.usage.WeeklyDeviceUsage

interface DeviceUsageRepository {

    // Permission

    fun hasAppUsagePermission(): Boolean

    // Calculate

    suspend fun calculateDeviceUsage(
        timeInterval: Pair<Long, Long>,
        ignoreSystemApps: Boolean,
        ignoredPackages: List<String>
    )

    /**
     * @return The first day for which usage data is available (saved history or system events).
     */
    fun getEarliestDayTimestamp(): Long

    suspend fun getWeekDeviceUsage(
        timeInterval: Pair<Long, Long>
    ): WeeklyDeviceUsage

    suspend fun getDayDeviceUsage(
        timeInterval: Pair<Long, Long>,
        dayTimestamp: Long
    ): DailyDeviceUsage
}