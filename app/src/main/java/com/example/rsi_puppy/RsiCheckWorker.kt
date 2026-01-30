package com.example.rsialert.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.rsialert.data.StockDataSource
import com.example.rsialert.domain.RsiCalculator
import com.example.rsialert.domain.RsiMonitorUseCase
import com.example.rsialert.ui.NotificationHelper

class RsiCheckWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val symbol = inputData.getString("symbol") ?: return Result.failure()

        val useCase = RsiMonitorUseCase(StockDataSource(), RsiCalculator(14))
        val result = useCase.check(symbol) ?: return Result.retry()

        if (result.state != RsiMonitorUseCase.State.NORMAL) {
            NotificationHelper(applicationContext).notifyRsi(result.symbol, result.rsi, result.state.name)
        }
        return Result.success()
    }
}
