package com.example.rsi_puppy.domain

import com.example.rsi_puppy.data.StockDataSource

class RsiMonitorUseCase(
    private val dataSource: StockDataSource,
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

    suspend fun check(symbol: String): Result {
        val r = dataSource.fetchRsi(symbol, period)
        val state = when {
            r.rsi <= low -> State.LOW
            r.rsi >= high -> State.HIGH
            else -> State.NORMAL
        }
        return Result(symbol = r.symbol, rsi = r.rsi, state = state)
    }
}
