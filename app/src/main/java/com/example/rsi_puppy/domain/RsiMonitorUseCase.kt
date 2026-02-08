package com.example.rsi_puppy.domain

import com.example.rsi_puppy.data.StockDataSource
import com.example.rsi_puppy.data.StockMasterLoader
import com.example.rsi_puppy.data.StockRepository
import kotlinx.coroutines.flow.Flow

class RsiMonitorUseCase(
    private val dataSource: StockDataSource,
    private val repository: StockRepository,
    private val period: Int = 14,
    private val low: Double = 30.0,
    private val high: Double = 70.0
) {
    enum class State { LOW, HIGH, NORMAL }

    data class Result(
        val symbol: String,
        val rsi: Double,
        val state: State
    )

    // --- 유효성 검사 기능 (신규 추가) ---

    /**
     * 입력된 심볼이 유효한지 검사합니다.
     * 1. StockMasterLoader(CSV)에 등록된 종목명 또는 코드인가?
     * 2. yfinance 직접 입력용 티커(점 포함)인가?
     * 3. 일반적인 한국 주식 코드(숫자 6자리)인가?
     */
    fun isValidSymbol(symbol: String): Boolean {
        val trimmed = symbol.trim()
        if (trimmed.isEmpty()) return false

        // 1. CSV 마스터 데이터에서 검색 (가장 정확)
        if (StockMasterLoader.getSymbol(trimmed) != null) return true

        // 2. 이미 점이 포함된 티커 형태 (예: AAPL, 005930.KS 등)
        if (trimmed.contains(".")) return true

        // 3. 마스터에 없더라도 숫자 6자리면 한국 종목으로 간주하여 시도 허용
        if (trimmed.length == 6 && trimmed.all { it.isDigit() }) return true

        return false
    }

    // --- 기존 기능 유지 ---
    suspend fun check(symbol: String): Result {
        val r = dataSource.fetchRsi(symbol, period)
        val state = when {
            r.rsi <= low -> State.LOW
            r.rsi >= high -> State.HIGH
            else -> State.NORMAL
        }
        return Result(symbol = r.symbol, rsi = r.rsi, state = state)
    }

    // --- 종목 관리 기능 ---

    /**
     * 저장된 모든 종목 리스트를 Flow로 반환 (순서 보장됨)
     */
    fun getAllSymbols(): Flow<List<String>> = repository.monitoredStocks

    /**
     * 새로운 종목 추가
     * (MainActivity에서 이미 isValidSymbol을 체크하므로 여기서는 저장만 수행합니다)
     */
    suspend fun addSymbol(symbol: String) {
        if (symbol.isNotBlank()) {
            repository.addStock(symbol.trim().uppercase())
        }
    }

    /**
     * 기존 종목 삭제
     */
    suspend fun deleteSymbol(symbol: String) {
        repository.removeStock(symbol)
    }

    /**
     * 종목 순서 변경 사항 저장
     */
    suspend fun updateOrder(newOrder: List<String>) {
        repository.updateStockOrder(newOrder)
    }
}