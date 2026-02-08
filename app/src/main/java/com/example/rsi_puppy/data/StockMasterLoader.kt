package com.example.rsi_puppy.data

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

data class StockMaster(
    val name: String,
    val symbol: String,
    val market: String,
)

object StockMasterLoader {

    private const val FILE_NAME = "stocks_kr.csv"

    fun load(context: Context): List<StockMaster> {
        context.assets.open(FILE_NAME).use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { br ->
                return br.lineSequence()
                    .drop(1) // header: name,symbol,market
                    .mapNotNull { line -> parseLine(line) }
                    .toList()
            }
        }
    }

    private fun parseLine(line: String): StockMaster? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        // CSV가 단순(콤마 2개)하다고 가정. (현재 샘플이 그렇게 되어 있음)
        val parts = trimmed.split(',')
        if (parts.size < 3) return null

        val name = parts[0].trim()
        val symbol = parts[1].trim()
        val market = parts[2].trim()

        if (name.isEmpty() || symbol.isEmpty() || market.isEmpty()) return null

        return StockMaster(
            name = name,
            symbol = symbol,
            market = market
        )
    }
}
