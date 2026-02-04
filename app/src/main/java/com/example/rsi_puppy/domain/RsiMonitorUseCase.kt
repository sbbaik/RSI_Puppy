package com.example.rsi_puppy.domain

import com.example.rsi_puppy.data.StockDataSource

class RsiMonitorUseCase(
    private val dataSource: StockDataSource,
    private val calculator: RsiCalculator
) {
    data class Result(val symbol: String, val rsi: Double, val state: State)
    enum class State { OVERSOLD, OVERBOUGHT, NORMAL }

    suspend fun check(symbol: String): Result? {
        val closes = dataSource.fetchDailyCloseHistory(symbol, days = 60)
        if (closes.isEmpty()) return null

        val rsi = calculator.calculateLatestRsi(closes) ?: return null
        val state = when {
            rsi <= 30.0 -> State.OVERSOLD
            rsi >= 70.0 -> State.OVERBOUGHT
            else -> State.NORMAL
        }
        return Result(symbol, rsi, state)
    }
}
