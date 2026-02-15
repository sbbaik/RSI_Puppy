package com.example.rsi_puppy.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
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
            withContext(Dispatchers.IO) {
                StockMasterLoader.load(applicationContext)
            }

            val symbols = useCase.getAllSymbols().first()
            if (symbols.isEmpty()) return@coroutineScope Result.success()

            // 1. 모든 종목 체크 수행
            val results = symbols.map { symbol ->
                async {
                    try {
                        val result = useCase.check(symbol)
                        repository.updateRsiValue(symbol, result.rsi)

                        if (result.rsi >= 70 || (result.rsi <= 30 && result.rsi > 0)) {
                            StockMasterLoader.getName(symbol)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        // 개별 종목 에러 발생 시 로그를 남기고 예외를 밖으로 던집니다.
                        Log.e("RsiWorker", "Critical error checking $symbol: ${e.message}")
                        throw e
                    }
                }
            }.awaitAll().filterNotNull()

            // 2. 모든 요청이 성공했을 때만 알림 업데이트 (기존 카운트 변경 지점)
            sendRsiNotification(results)

            Result.success()
        } catch (e: Exception) {
            // 하나라도 실패하여 throw가 발생하면 일로 들어옵니다.
            // sendRsiNotification을 호출하지 않으므로 기존 배지/알림이 그대로 유지됩니다.
            Log.e("RsiWorker", "Worker failed (skipping update, maintaining old count): ${e.message}")

            // 즉시 재시도하지 않고 이번 회차는 종료합니다. (1시간 후 다음 주기에 다시 실행)
            Result.failure()
        }
    }

    private fun sendRsiNotification(alertedStockNames: List<String>) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "RSI_ALERTS"
        val count = alertedStockNames.size

        if (count > 0) {
            // 1. 배지 카운트 적용
            ShortcutBadger.applyCount(applicationContext, count)
/*
            // 2. 알림 채널 생성
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId, "RSI 알림",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // 3. 알림 메시지 빌드
            val message = "주의 종목($count): ${alertedStockNames.joinToString(", ")}"
            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("RSI Puppy 알림")
                .setContentText(message)
                .setNumber(count)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(100, notification)
            */
        } else {
            ShortcutBadger.removeCount(applicationContext)
            /*
            notificationManager.cancel(100)
                         */
        }
    }
}