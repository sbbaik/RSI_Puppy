package com.example.rsi_puppy

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.example.rsi_puppy.data.StockDataSource
import com.example.rsi_puppy.data.StockMasterLoader
import com.example.rsi_puppy.data.StockRepository
import com.example.rsi_puppy.domain.RsiMonitorUseCase
import com.example.rsi_puppy.ui.MainRsiScreen
import com.example.rsi_puppy.ui.RsiRowUi
import com.example.rsi_puppy.ui.theme.RSI_PuppyTheme
import com.example.rsi_puppy.worker.RsiWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import me.leolin.shortcutbadger.ShortcutBadger
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var stockRepository: StockRepository
    private lateinit var stockDataSource: StockDataSource
    private lateinit var rsiMonitorUseCase: RsiMonitorUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkNotificationPermission()
        createNotificationChannel()

        stockRepository = StockRepository(applicationContext)
        stockDataSource = StockDataSource()
        rsiMonitorUseCase = RsiMonitorUseCase(stockDataSource, stockRepository)

        setupPeriodicWork()

        setContent {
            RSI_PuppyTheme {
                val rows = remember { mutableStateListOf<RsiRowUi>() }

                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        StockMasterLoader.load(applicationContext)
                    }

                    stockRepository.monitoredStocks.combine(stockRepository.allStockData) { symbols, dbData ->
                        if (symbols.isEmpty()) {
                            val defaultStocks = listOf("KOSPI200", "KOSDAQ", "KT", "삼성전자", "LG전자")
                            defaultStocks.forEach { symbol ->
                                lifecycleScope.launch { rsiMonitorUseCase.addSymbol(symbol) }
                            }
                            emptyList<RsiRowUi>()
                        } else {
                            val dataMap = dbData.associateBy { it.symbol }
                            symbols.map { symbol ->
                                val displayName = StockMasterLoader.getName(symbol)
                                val rsiValue = dataMap[symbol]?.rsi?.toInt() ?: 0
                                RsiRowUi(displayName, rsiValue)
                            }
                        }
                    }.collect { combinedRows ->
                        rows.clear()
                        rows.addAll(combinedRows)
                        // 리스트 변경 시 알림/배지 업데이트
                        updateRsiNotification(rows)
                    }
                }

                LaunchedEffect(Unit) {
                    stockRepository.monitoredStocks.collect { symbols ->
                        if (symbols.isEmpty()) return@collect
                        symbols.forEach { symbol ->
                            launch {
                                try {
                                    val result = rsiMonitorUseCase.check(symbol)
                                    stockRepository.updateRsiValue(symbol, result.rsi)
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Check failed for $symbol: ${e.message}")
                                }
                            }
                        }
                    }
                }

                MainRsiScreen(
                    rows = rows,
                    onAddSymbol = { symbol ->
                        lifecycleScope.launch { rsiMonitorUseCase.addSymbol(symbol) }
                    },
                    onDeleteSymbol = { friendlyName ->
                        lifecycleScope.launch {
                            val targetSymbol = StockMasterLoader.getSymbol(friendlyName) ?: friendlyName
                            rsiMonitorUseCase.deleteSymbol(targetSymbol)
                        }
                    },
                    onOrderChanged = { updatedRows ->
                        lifecycleScope.launch {
                            val newOrder = updatedRows.map {
                                StockMasterLoader.getSymbol(it.name) ?: it.name
                            }
                            rsiMonitorUseCase.updateOrder(newOrder)
                        }
                    },
                    checkValidSymbol = { symbol -> rsiMonitorUseCase.isValidSymbol(symbol) }
                )
            }
        }
    }

    // --- 알림 메시지 복구 및 배지 업데이트 함수 ---
    private fun updateRsiNotification(rows: List<RsiRowUi>) {
        val alertedStocks = rows.filter { it.rsi >= 70 || (it.rsi <= 30 && it.rsi > 0) }
        val alertCount = alertedStocks.size
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "RSI_ALERTS"

        if (alertCount > 0) {
            // 외부 라이브러리 배지 시도
            ShortcutBadger.applyCount(applicationContext, alertCount)

            // 메시지 내용 구성
            val stockNames = alertedStocks.joinToString(", ") { it.name }
            val message = "주의 종목($alertCount): $stockNames"

            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("RSI Puppy 알림")
                .setContentText(message)
                .setNumber(alertCount) // 시스템 배지 연동
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true) // 데이터 갱신 시마다 소리나는 것 방지
                .build()

            notificationManager.notify(100, notification)
        } else {
            ShortcutBadger.removeCount(applicationContext)
            notificationManager.cancel(100)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 일반 알림 채널 복구 (IMPORTANCE_HIGH)
            val channel = NotificationChannel(
                "RSI_ALERTS",
                "RSI 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "RSI 수치 알림 및 배지 관리"
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun setupPeriodicWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val rsiWorkRequest = PeriodicWorkRequestBuilder<RsiWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "RsiUpdateWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            rsiWorkRequest
        )
    }
}