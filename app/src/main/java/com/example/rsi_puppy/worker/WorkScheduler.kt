package com.example.rsi_puppy.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {

    fun scheduleRsiCheck(context: Context, symbol: String) {
        val workName = "rsi_check_${symbol.replace(".", "_")}"

        val request = PeriodicWorkRequestBuilder<RsiCheckWorker>(15, TimeUnit.MINUTES)
            .setInputData(Data.Builder().putString("symbol", symbol).build())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun runOnceNow(context: Context, symbol: String) {
    val workName = "rsi_once_${symbol.replace(".", "_")}"

    val request = androidx.work.OneTimeWorkRequestBuilder<RsiCheckWorker>()
        .setInputData(androidx.work.Data.Builder().putString("symbol", symbol).build())
        .build()

    androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
        workName,
        androidx.work.ExistingWorkPolicy.REPLACE,
        request
    )
}

    fun cancelRsiCheck(context: Context, symbol: String) {
        val workName = "rsi_check_${symbol.replace(".", "_")}"
        WorkManager.getInstance(context).cancelUniqueWork(workName)
    }
}