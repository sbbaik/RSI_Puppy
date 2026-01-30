package com.example.rsialert.domain

import com.example.rsialert.data.StockDataSource
import yahoofinance.histquotes.Interval

class RsiMonitorUseCase(
    private val dataSource: StockDataSource,
    private val calculator: RsiCalculator
) {
    data class Result(val symbol: String, val rsi: Double, val state: State)
    enum class State { OVERSOLD, OVERBOUGHT, NORMAL }

    suspend fun check(symbol: String): Result? {
        val closes = dataSource.fetchClosePrices(
            symbol = symbol,
            days = 60,
            interval = Interval.DAILY
        )
        val rsi = calculator.calculateLatestRsi(closes) ?: return null
        val state = when {
            rsi <= 30.0 -> State.OVERSOLD
            rsi >= 70.0 -> State.OVERBOUGHT
            else -> State.NORMAL
        }
        return Result(symbol, rsi, state)
    }
}
