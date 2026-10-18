package com.atharok.screentime.presentation.history

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.atharok.screentime.common.utils.USAGE_HISTORY_PERIODIC_WORK_NAME
import com.atharok.screentime.data.history.UsageHistoryRecorder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * Saves the usage history regularly, even if the app is not opened, so that no day is lost
 * when Android deletes its usage events (after about 10 days).
 */
class UsageHistoryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    companion object {
        fun schedule(context: Context) {
            val work = PeriodicWorkRequestBuilder<UsageHistoryWorker>(
                repeatInterval = 12,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                uniqueWorkName = USAGE_HISTORY_PERIODIC_WORK_NAME,
                existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
                request = work
            )
        }
    }

    override suspend fun doWork(): Result {
        val recorder by inject<UsageHistoryRecorder>()

        return try {
            recorder.recordFromSystemEvents()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
