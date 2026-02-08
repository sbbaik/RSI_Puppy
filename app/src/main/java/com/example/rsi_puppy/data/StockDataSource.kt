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

    /**
     * 입력을 서버가 이해할 수 있는 심볼(티커)로 변환합니다.
     */
    private fun normalizeSymbol(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return s

        // 1. 이미 점(.)이 포함된 경우 (예: AAPL, 005930.KS 등)는 그대로 반환
        if (s.contains(".")) return s

        // 2. StockMasterLoader(CSV)에서 검색 시도 (삼성전자 -> 005930.KS)
        val mastered = StockMasterLoader.getSymbol(s)
        if (mastered != null) return mastered

        // 3. 마스터에 없지만 숫자 6자리면 한국 주식(.KS)으로 간주
        return if (s.length == 6 && s.all { it.isDigit() }) "$s.KS" else s
    }

    suspend fun fetchRsi(symbol: String, period: Int = 14): RsiResponse =
        withContext(Dispatchers.IO) {
            // 입력값(이름 혹은 코드)을 서버용 심볼로 변환
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

                // [중요 수정] 서버가 소문자로 응답(030200.ks)하더라도
                // UI 매칭(StockMasterLoader)을 위해 대문자로 변환(030200.KS)하여 반환합니다.
                val responseSymbol = json.getString("symbol").uppercase()

                RsiResponse(
                    symbol = responseSymbol,
                    rsi = json.getDouble("rsi"),
                    period = json.getInt("period")
                )
            }
        }
}