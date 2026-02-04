package com.example.rsi_puppy.domain

import org.ta4j.core.BaseBarSeriesBuilder
import org.ta4j.core.indicators.RSIIndicator
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import java.math.BigDecimal
import java.time.Duration
import java.time.ZonedDateTime

class RsiCalculator(private val period: Int = 14) {

    fun calculateLatestRsi(closePrices: List<BigDecimal>): Double? {
        if (closePrices.size < period + 1) return null

        val series = BaseBarSeriesBuilder().withName("price").build()
        val now = ZonedDateTime.now()
        val barDuration = Duration.ofDays(1)

        closePrices.forEachIndexed { idx, price ->
            val t = now.minusDays((closePrices.size - 1 - idx).toLong())
            val p = price.toDouble()

            // open, high, low, close, volume
            series.addBar(barDuration, t, p, p, p, p, 0.0)
        }

        val close = ClosePriceIndicator(series)
        val rsi = RSIIndicator(close, period)
        return rsi.getValue(series.endIndex).doubleValue()
    }
}
