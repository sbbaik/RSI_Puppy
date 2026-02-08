package com.example.rsi_puppy.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.rsi_puppy.data.StockDataSource
import com.example.rsi_puppy.ui.NotificationHelper

class RsiCheckWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val symbol = inputData.getString("symbol") ?: return Result.failure()

        return try {
            val ds = StockDataSource(baseUrl = "http://144.24.90.255:8000")
            val r = ds.fetchRsi(symbol, period = 14)

            val state = when {
                r.rsi <= 30.0 -> "LOW"
                r.rsi >= 70.0 -> "HIGH"
                else -> "NORMAL"
            }

            if (state != "NORMAL") {
                NotificationHelper(applicationContext)
                    .notifyRsi(r.symbol, r.rsi, state)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
