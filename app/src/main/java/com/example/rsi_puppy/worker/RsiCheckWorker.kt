package com.example.rsi_puppy.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.rsi_puppy.data.StockDataSource

class RsiCheckWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val symbol = inputData.getString("symbol") ?: return Result.failure()

        return try {
            val dataSource = StockDataSource()
            val quote = dataSource.fetchQuote(symbol)

            Log.d(
                "RSI_PUPPY",
                "QUOTE symbol=${quote.symbol}, price=${quote.price}, prevClose=${quote.previousClose}, " +
                        "open=${quote.open}, high=${quote.dayHigh}, low=${quote.dayLow}, vol=${quote.volume}"
            )

            // 여기서 quote.price 등을 이용해 알림/저장/UI 갱신 로직으로 확장 가능
            Result.success()
        } catch (e: Exception) {
            Log.e("RSI_PUPPY", "Quote fetch failed: ${e.message}", e)
            Result.retry()
        }
    }
}
