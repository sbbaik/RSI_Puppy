package com.example.rsi_puppy.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class StockDataSource(
    private val baseUrl: String = "http://144.24.90.255:8000"
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    data class RsiResponse(
        val symbol: String,
        val rsi: Double,
        val period: Int
    )

    private fun normalizeSymbol(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return s
        if (s.contains(".")) return s

        // 숫자 6자리면 한국 주식으로 보고 기본 .KS를 붙임
        return if (s.length == 6 && s.all { it.isDigit() }) "$s.KS" else s
    }

    suspend fun fetchRsi(symbol: String, period: Int = 14): RsiResponse =
        withContext(Dispatchers.IO) {
            val normalized = normalizeSymbol(symbol)
            val encodedSymbol = URLEncoder.encode(normalized, StandardCharsets.UTF_8.toString())
            val url = "$baseUrl/rsi?symbol=$encodedSymbol&period=$period"

            val req = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException("HTTP ${resp.code}: $body")
                }
                val json = JSONObject(body)
                RsiResponse(
                    symbol = json.getString("symbol"),
                    rsi = json.getDouble("rsi"),
                    period = json.getInt("period")
                )
            }
        }
}
