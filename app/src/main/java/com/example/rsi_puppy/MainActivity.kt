package com.example.rsi_puppy

import android.os.Bundle
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

        // 1. CSV 마스터 데이터 로드 (메모리 캐싱)
        StockMasterLoader.load(applicationContext)

        stockRepository = StockRepository(applicationContext)
        stockDataSource = StockDataSource()
        rsiMonitorUseCase = RsiMonitorUseCase(stockDataSource, stockRepository)

        setContent {
            RSI_PuppyTheme {
                // UI 상태를 유지하는 리스트 (화면에 표시될 이름과 RSI 값을 가짐)
                val rows = remember { mutableStateListOf<RsiRowUi>() }

                // DataStore 리스트 관찰 및 UI 갱신
                LaunchedEffect(Unit) {
                    rsiMonitorUseCase.getAllSymbols().collect { symbols ->
                        // 1. 저장된 종목이 없을 때 디폴트 종목 추가
                        if (symbols.isEmpty()) {
                            val defaultStocks = listOf("KOSPI200", "KOSDAQ", "KT", "삼성전자", "LG전자")
                            defaultStocks.forEach { rsiMonitorUseCase.addSymbol(it) }
                            return@collect
                        }

                        // 2. UI 리스트 초기화 및 이름 변환 로직 적용
                        rows.clear()
                        symbols.forEach { rawSymbol ->
                            // StockMasterLoader를 통해 코드를 이름으로 변환 (예: 005930.KS -> 삼성전자)
                            // 만약 이미 이름이면 그대로 반환됨
                            val displayName = if (rawSymbol.contains(".")) {
                                StockMasterLoader.getName(rawSymbol)
                            } else {
                                rawSymbol
                            }
                            rows.add(RsiRowUi(displayName, 0))
                        }

                        // 3. 실제 RSI 데이터 페치 및 개별 업데이트
                        symbols.forEachIndexed { index, rawSymbol ->
                            try {
                                val result = rsiMonitorUseCase.check(rawSymbol)
                                if (index < rows.size) {
                                    // 서버에서 받은 심볼(005930.KS 등)을 다시 이름으로 변환하여 UI 업데이트
                                    val friendlyName = StockMasterLoader.getName(result.symbol)
                                    rows[index] = RsiRowUi(friendlyName, result.rsi.toInt())
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
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
                        // UI에 표시된 이름 대신, 실제 로직 처리를 위해
                        // UseCase가 내부 저장소의 데이터를 관리하도록 설계됨
                        lifecycleScope.launch {
                            // 이름으로 등록된 경우와 코드로 등록된 경우 모두 대응하기 위해
                            // 삭제 시에도 MasterLoader의 도움을 받을 수 있음
                            val targetSymbol = StockMasterLoader.getSymbol(friendlyName) ?: friendlyName
                            rsiMonitorUseCase.deleteSymbol(targetSymbol)
                        }
                    },
                    onOrderChanged = { updatedRows ->
                        lifecycleScope.launch {
                            // UI의 이름들을 다시 서버용 심볼/코드로 변환하여 저장 순서 유지
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