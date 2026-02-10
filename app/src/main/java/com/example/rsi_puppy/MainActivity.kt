package com.example.rsi_puppy

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.example.rsi_puppy.data.StockDataSource
import com.example.rsi_puppy.data.StockMasterLoader
import com.example.rsi_puppy.data.StockRepository
import com.example.rsi_puppy.domain.RsiMonitorUseCase
import com.example.rsi_puppy.ui.MainRsiScreen
import com.example.rsi_puppy.ui.RsiRowUi
import com.example.rsi_puppy.ui.theme.RSI_PuppyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var stockRepository: StockRepository
    private lateinit var stockDataSource: StockDataSource
    private lateinit var rsiMonitorUseCase: RsiMonitorUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. CSV 마스터 데이터 로드 (최우선 실행)
        StockMasterLoader.load(applicationContext)

        stockRepository = StockRepository(applicationContext)
        stockDataSource = StockDataSource()
        rsiMonitorUseCase = RsiMonitorUseCase(stockDataSource, stockRepository)

        setContent {
            RSI_PuppyTheme {
                // UI 상태 리스트
                val rows = remember { mutableStateListOf<RsiRowUi>() }

                // 데이터 관찰 및 업데이트 로직
                LaunchedEffect(Unit) {
                    rsiMonitorUseCase.getAllSymbols().collect { symbols ->
                        // 종목이 없을 때 기본값 추가
                        if (symbols.isEmpty()) {
                            val defaultStocks = listOf("KOSPI200", "KOSDAQ", "KT", "삼성전자", "LG전자")
                            defaultStocks.forEach { rsiMonitorUseCase.addSymbol(it) }
                            return@collect
                        }

                        // [개선] 1. UI 리스트 즉시 초기화 (RSI가 오기 전에도 이름은 먼저 보여줌)
                        rows.clear()
                        symbols.forEach { rawSymbol ->
                            // getName은 매칭 실패 시 원본을 반환하므로 조건문 없이 호출 가능
                            val displayName = StockMasterLoader.getName(rawSymbol)
                            rows.add(RsiRowUi(displayName, 0))
                        }

                        // [개선] 2. 병렬 처리를 통한 RSI 업데이트
                        symbols.forEachIndexed { index, rawSymbol ->
                            // lifecycleScope.launch를 통해 각 종목 요청을 독립적으로 실행
                            lifecycleScope.launch {
                                try {
                                    val result = rsiMonitorUseCase.check(rawSymbol)

                                    // 데이터가 도착한 순서대로 해당 줄만 업데이트
                                    if (index < rows.size) {
                                        val friendlyName = StockMasterLoader.getName(result.symbol)
                                        rows[index] = RsiRowUi(friendlyName, result.rsi.toInt())
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Error fetching $rawSymbol: ${e.message}")
                                }
                            }
                        }
                    }
                }

                MainRsiScreen(
                    rows = rows,
                    onAddSymbol = { symbol ->
                        lifecycleScope.launch {
                            rsiMonitorUseCase.addSymbol(symbol)
                        }
                    },
                    onDeleteSymbol = { friendlyName ->
                        lifecycleScope.launch {
                            // UI 이름 -> 저장용 심볼 변환 후 삭제
                            val targetSymbol = StockMasterLoader.getSymbol(friendlyName) ?: friendlyName
                            rsiMonitorUseCase.deleteSymbol(targetSymbol)
                        }
                    },
                    onOrderChanged = { updatedRows ->
                        lifecycleScope.launch {
                            // 순서 변경 저장
                            val newOrder = updatedRows.map {
                                StockMasterLoader.getSymbol(it.name) ?: it.name
                            }
                            rsiMonitorUseCase.updateOrder(newOrder)
                        }
                    },
                    checkValidSymbol = { symbol ->
                        rsiMonitorUseCase.isValidSymbol(symbol)
                    }
                )
            }
        }
    }
}