package com.example.rsialert.domain

import org.ta4j.core.BaseBarSeriesBuilder
import org.ta4j.core.indicators.RSIIndicator
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.num.DecimalNum
import java.math.BigDecimal
import java.time.Duration
import java.time.ZonedDateTime

class RsiCalculator(private val period: Int = 14) {

    fun calculateLatestRsi(closePrices: List<BigDecimal>): Double? {
        if (closePrices.size < period + 1) return null

        val series = BaseBarSeriesBuilder().withName("price").build()
        val now = ZonedDateTime.now()

        closePrices.forEachIndexed { idx, price ->
            val barTime = now.minusMinutes((closePrices.size - idx).toLong())
            series.addBar(Duration.ofMinutes(1), barTime, DecimalNum.valueOf(price))
        }

        val close = ClosePriceIndicator(series)
        val rsi = RSIIndicator(close, period)
        return rsi.getValue(series.endIndex).doubleValue()
    }
}
