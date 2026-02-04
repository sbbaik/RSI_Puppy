package com.example.rsi_puppy.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yahoofinance.YahooFinance
import yahoofinance.histquotes.Interval
import java.math.BigDecimal
import java.util.Calendar

class StockDataSource {

    data class QuoteSnapshot(
        val symbol: String,
        val price: BigDecimal?,
        val previousClose: BigDecimal?,
        val open: BigDecimal?,
        val dayHigh: BigDecimal?,
        val dayLow: BigDecimal?,
        val volume: Long?
    )

    suspend fun fetchQuote(symbol: String): QuoteSnapshot = withContext(Dispatchers.IO) {
        val stock = YahooFinance.get(symbol)
        val q = stock.quote

        QuoteSnapshot(
            symbol = symbol,
            price = q?.price,
            previousClose = q?.previousClose,
            open = q?.open,
            dayHigh = q?.dayHigh,
            dayLow = q?.dayLow,
            volume = q?.volume
        )
    }

    suspend fun fetchDailyCloseHistory(symbol: String, days: Int = 60): List<BigDecimal> =
        withContext(Dispatchers.IO) {
            val stock = YahooFinance.get(symbol)

            val from = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -days) }
            val to = Calendar.getInstance()

            val history = stock.getHistory(from, to, Interval.DAILY)
            history.mapNotNull { it.close }.filter { it > BigDecimal.ZERO }
        }
}
