package com.example.rsi_puppy.worker

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.rsi_puppy.data.StockDataSource
import com.example.rsi_puppy.data.StockMasterLoader
import com.example.rsi_puppy.data.StockRepository
import com.example.rsi_puppy.domain.RsiMonitorUseCase
import me.leolin.shortcutbadger.ShortcutBadger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class RsiWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        val repository = StockRepository(applicationContext)
        val dataSource = StockDataSource()
        val useCase = RsiMonitorUseCase(dataSource, repository)

        try {
            // 1. [필수] 마스터 데이터 로드 확인 (백그라운드 필수 절차)
            withContext(Dispatchers.IO) {
                StockMasterLoader.load(applicationContext)
            }

            // 2. 저장된 모든 종목 가져오기
            val symbols = useCase.getAllSymbols().first()
            if (symbols.isEmpty()) return@coroutineScope Result.success()

            // 3. [개선] 병렬 처리를 통해 모든 종목 동시 체크
            val deferredResults = symbols.map { symbol ->
                async {
                    try {
                        val result = useCase.check(symbol)
                        if (result.rsi >= 70 || (result.rsi <= 30 && result.rsi > 0)) 1 else 0
                    } catch (e: Exception) {
                        Log.e("RsiWorker", "Error checking $symbol: ${e.message}")
                        0
                    }
                }
            }

            // 모든 결과가 올 때까지 대기 후 합산
            val alertCount = deferredResults.awaitAll().sum()

            // 4. 배지 및 알림 업데이트
            updateNotificationAndBadge(alertCount)

            Result.success()
        } catch (e: Exception) {
            Log.e("RsiWorker", "Worker failed: ${e.message}")
            Result.retry()
        }
    }

    private fun updateNotificationAndBadge(count: Int) {
        // 배지 업데이트
        ShortcutBadger.applyCount(applicationContext, count)

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (count > 0) {
            val notification = NotificationCompat.Builder(applicationContext, "RSI_ALERTS")
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle("RSI 실시간 알림")
                .setContentText("현재 주의 종목이 ${count}건 감지되었습니다.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setNumber(count)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1, notification)
        } else {
            notificationManager.cancel(1)
            ShortcutBadger.removeCount(applicationContext)
        }
    }
}