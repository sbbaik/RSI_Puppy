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

                    rsiMonitorUseCase.getAllSymbols().collect { symbols ->
                        // [핵심 1] 진행 중인 구형 작업 취소
                        coroutineContext[Job]?.cancelChildren()

                        if (symbols.isEmpty()) {
                            val defaultStocks = listOf("KOSPI200", "KOSDAQ", "KT", "삼성전자", "LG전자")
                            defaultStocks.forEach { rsiMonitorUseCase.addSymbol(it) }
                            return@collect
                        }

                        // [핵심 2] Smart Sync: clear() 하지 않고 필요한 부분만 수정
                        val currentSymbolNames = symbols.map { StockMasterLoader.getName(it) }

                        // 1. 삭제된 종목 제거
                        rows.removeAll { it.name !in currentSymbolNames }

                        // 2. 추가된 종목만 삽입 (기존 종목은 RSI 유지)
                        symbols.forEachIndexed { index, rawSymbol ->
                            val displayName = StockMasterLoader.getName(rawSymbol)
                            val existingIndex = rows.indexOfFirst { it.name == displayName }

                            if (existingIndex == -1) {
                                // 리스트에 없는 새 종목이면 해당 위치에 즉시 0으로 추가 (반응성 향상)
                                if (index <= rows.size) rows.add(index, RsiRowUi(displayName, 0))
                                else rows.add(RsiRowUi(displayName, 0))
                            }

                            // 3. 개별 종목 RSI 업데이트 (병렬 실행)
                            launch {
                                try {
                                    val result = rsiMonitorUseCase.check(rawSymbol)
                                    val updatedName = StockMasterLoader.getName(result.symbol)

                                    // 이름 기반으로 정확한 위치를 찾아 업데이트 (인덱스 꼬임 방지)
                                    val targetIndex = rows.indexOfFirst { it.name == updatedName }
                                    if (targetIndex != -1) {
                                        rows[targetIndex] = RsiRowUi(updatedName, result.rsi.toInt())
                                        updateLauncherBadge(rows)
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Error: ${e.message}")
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

    // --- 알림 및 배지 함수 (기존과 동일) ---

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
            val channel = NotificationChannel(
                "RSI_ALERTS", "RSI 알림", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "RSI 배지 업데이트용 채널"
                setShowBadge(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateLauncherBadge(rows: List<RsiRowUi>) {
        val alertCount = rows.count { it.rsi >= 70 || (it.rsi <= 30 && it.rsi > 0) }
        ShortcutBadger.applyCount(applicationContext, alertCount)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (alertCount > 0) {
            val notification = NotificationCompat.Builder(this, "RSI_ALERTS")
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle("RSI 지표 알림")
                .setContentText("주의 종목 $alertCount 건이 감지되었습니다.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setNumber(alertCount)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(1, notification)
        } else {
            notificationManager.cancel(1)
        }
    }

    private fun setupPeriodicWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val rsiWorkRequest = PeriodicWorkRequestBuilder<RsiWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "RsiUpdateWork",
            ExistingPeriodicWorkPolicy.KEEP,
            rsiWorkRequest
        )
    }
}