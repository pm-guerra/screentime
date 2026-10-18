package com.atharok.screentime.domain.usecases

import com.atharok.screentime.common.utils.DateTimeUtils
import com.atharok.screentime.common.utils.NUMBER_OF_DAYS
import com.atharok.screentime.domain.entities.usage.DailyDeviceUsage
import com.atharok.screentime.domain.entities.usage.DeviceUsage
import com.atharok.screentime.domain.entities.usage.WeeklyDeviceUsage
import com.atharok.screentime.domain.repositories.DeviceUsageRepository
import com.atharok.screentime.domain.resources.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DeviceUsageUseCase(private val repository: DeviceUsageRepository) {

    // ---- Permission ----

    fun hasAppUsagePermission(): Boolean = repository.hasAppUsagePermission()

    // ---- Compute Device Usage ----

    // The calculations share caches and the calendar of DateTimeUtils, so they are run one at
    // a time (e.g. when switching quickly between days).
    private val mutex = Mutex()

    private var calculationTimestamp = System.currentTimeMillis()
    private var isInitialised = false
    private var earliestDayTimestamp: Long = 0L

    // Number of days / weeks that can be displayed before the current ones.
    var maxDayOffset: Int = 0
        private set
    var maxWeekOffset: Int = 0
        private set

    private suspend fun calculateDeviceUsage(
        ignoreSystemApps: Boolean,
        ignoredPackages: List<String>
    ) {
        calculationTimestamp = System.currentTimeMillis()
        repository.calculateDeviceUsage(
            timeInterval = DateTimeUtils.computeInterval(calculationTimestamp, NUMBER_OF_DAYS - 1),
            ignoreSystemApps = ignoreSystemApps,
            ignoredPackages = ignoredPackages
        )

        earliestDayTimestamp = repository.getEarliestDayTimestamp()
        maxDayOffset = DateTimeUtils.calculateNumberOfDaysBetween(
            startTimestamp = earliestDayTimestamp,
            endTimestamp = calculationTimestamp
        ).coerceAtLeast(0)
        // A week can be displayed as long as its last day is available.
        maxWeekOffset = (DateTimeUtils.calculateNumberOfDaysBetween(
            startTimestamp = earliestDayTimestamp,
            endTimestamp = DateTimeUtils.addDaysToTimestamp(calculationTimestamp, -1)
        ) / 7).coerceAtLeast(0)

        isInitialised = true
    }

    // ---- Compute Device Usage By Week and Day ----

    private val _deviceUsageFlow: MutableStateFlow<Resource<DeviceUsage<*>>> = MutableStateFlow(Resource.loading())
    val deviceUsageFlow: StateFlow<Resource<DeviceUsage<*>>> = _deviceUsageFlow

    // ---- Week ----

    private val weeklyDeviceUsageCache: MutableMap<Int, WeeklyDeviceUsage> = mutableMapOf()

    suspend fun calculateWeekDeviceUsage(
        ignoreSystemApps: Boolean,
        ignoredPackages: List<String>,
        weekOffset: Int,
        useCache: Boolean = true
    ) = mutex.withLock {
        if(!isInitialised || !useCache) // -> Condition to prevent re-displaying the loading screen when switching weeks
            _deviceUsageFlow.value = Resource.loading()

        try {
            if(!isInitialised || !useCache) {
                reset(ignoreSystemApps, ignoredPackages)
            }

            val offset = weekOffset.coerceIn(0, maxWeekOffset)

            val weeklyDeviceUsage = weeklyDeviceUsageCache.getOrPut(offset) {
                val weekInterval = DateTimeUtils.computeWeekIntervalFrom(
                    now = calculationTimestamp,
                    weekOffset = offset
                )
                repository.getWeekDeviceUsage(
                    // The oldest week may begin before the first available day.
                    timeInterval = maxOf(weekInterval.first, earliestDayTimestamp) to weekInterval.second
                )
            }

            _deviceUsageFlow.value = Resource.success(weeklyDeviceUsage)

        } catch (e: Exception) {
            _deviceUsageFlow.value = Resource.failure(e)
        }
    }

    // ---- Day ----

    private val dailyDeviceUsageCache: MutableMap<Int, DailyDeviceUsage> = mutableMapOf()

    suspend fun calculateDayDeviceUsage(
        ignoreSystemApps: Boolean,
        ignoredPackages: List<String>,
        dayOffset: Int,
        useCache: Boolean = true
    ) = mutex.withLock {
        if(!isInitialised || !useCache) // -> Condition to prevent re-displaying the loading screen when switching days
            _deviceUsageFlow.value = Resource.loading()

        try {
            if(!isInitialised || !useCache) {
                reset(ignoreSystemApps, ignoredPackages)
            }

            val offset = dayOffset.coerceIn(0, maxDayOffset)

            val dailyDeviceUsage = dailyDeviceUsageCache.getOrPut(offset) {
                val dayInterval = DateTimeUtils.computeDayIntervalFrom(
                    now = calculationTimestamp,
                    dayOffset = offset
                )
                repository.getDayDeviceUsage(
                    timeInterval = dayInterval,
                    dayTimestamp = dayInterval.first
                )
            }

            _deviceUsageFlow.value = Resource.success(dailyDeviceUsage)

        } catch (e: Exception) {
            _deviceUsageFlow.value = Resource.failure(e)
        }
    }

    // ---- Reset ----

    private suspend fun reset(
        ignoreSystemApps: Boolean,
        ignoredPackages: List<String>
    ) {
        weeklyDeviceUsageCache.clear()
        dailyDeviceUsageCache.clear()
        calculateDeviceUsage(ignoreSystemApps, ignoredPackages)
    }
}
