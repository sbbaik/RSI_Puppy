package com.example.rsialert.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yahoofinance.YahooFinance
import yahoofinance.histquotes.Interval
import java.math.BigDecimal
import java.util.Calendar

class StockDataSource {

    suspend fun fetchClosePrices(
        symbol: String,
        days: Int = 30,
        interval: Interval = Interval.DAILY
    ): List<BigDecimal> = withContext(Dispatchers.IO) {
        val stock = YahooFinance.get(symbol)
        val from = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -days) }
        val to = Calendar.getInstance()

        val history = stock.getHistory(from, to, interval)
        history.mapNotNull { it.close }.filter { it > BigDecimal.ZERO }
    }
}
