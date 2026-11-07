package com.kabuapp.app

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/** Downloads the morning data in the background so opening the app shows today's at once. */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        if (Repository(applicationContext).refresh()) Result.success() else Result.retry()

    companion object {
        // The pipeline publishes around 6:17; aim a little after 7:00
        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var next = now.with(LocalTime.of(7, 0))
            if (!next.isAfter(now)) next = next.plusDays(1)
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(Duration.ofDays(1))
                .setInitialDelay(Duration.between(now, next))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("morning_refresh", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
