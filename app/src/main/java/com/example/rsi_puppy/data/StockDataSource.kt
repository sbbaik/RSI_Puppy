package com.example.rsi_puppy.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.math.BigDecimal

class StockDataSource(
    private val apiKey: String = com.example.rsi_puppy.BuildConfig.TWELVE_DATA_API_KEY,
    private val client: OkHttpClient = OkHttpClient()
) {

    suspend fun fetchDailyCloseHistory(
        queryOrSymbol: String,
        days: Int = 120
    ): List<BigDecimal> = withContext(Dispatchers.IO) {

        // 1) 입력이 "030200" 같은 심볼인지, "KT" 같은 검색어인지 모를 수 있으니
        //    search로 먼저 확정 (실패하면 queryOrSymbol 그대로 시도)
        val resolved = resolveSymbolFromSearch(queryOrSymbol) ?: queryOrSymbol

        // 2) time_series 호출 (exchange 지정은 일단 생략하고, symbol 문자열에 맡김)
        //    (exchange를 강제하면 오히려 invalid 되는 케이스가 있음)
        val outputSize = days.coerceIn(60, 500)
        val url =
            "https://api.twelvedata.com/time_series" +
                    "?symbol=$resolved&interval=1day&outputsize=$outputSize&apikey=$apiKey"

        Log.d("RSI_PUPPY", "URL=$url")

        val json = httpGetJson(url)
        if (json.optString("status") == "error") {
            throw RuntimeException(json.optString("message", "Twelve Data error"))
        }

        val values = json.optJSONArray("values") ?: return@withContext emptyList<BigDecimal>()
        val closes = ArrayList<BigDecimal>(values.length())
        for (i in 0 until values.length()) {
            val item = values.getJSONObject(i)
            val close = item.optString("close").toBigDecimalOrNull()
            if (close != null && close > BigDecimal.ZERO) closes.add(close)
        }
        closes.reversed()
    }

    // Twelve Data search API로 심볼 확정
    private fun resolveSymbolFromSearch(q: String): String? {
        val query = q.trim()
        if (query.isEmpty()) return null

        val url =
            "https://api.twelvedata.com/symbol_search" +
                    "?symbol=$query&outputsize=30&apikey=$apiKey"

        Log.d("RSI_PUPPY", "SEARCH=$url")

        val json = httpGetJson(url)
        if (json.optString("status") == "error") return null

        val data = json.optJSONArray("data") ?: return null

        // 우선순위: exchange가 KRX 계열이면 그것부터
        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)
            val exchange = item.optString("exchange", "")
            val symbol = item.optString("symbol", "")
            if (symbol.isNotBlank() && exchange.contains("KR", ignoreCase = true)) {
                return symbol
            }
        }

        // 없으면 첫 번째 symbol 사용
        val first = data.optJSONObject(0)
        return first?.optString("symbol")?.takeIf { it.isNotBlank() }
    }

    private fun httpGetJson(url: String): JSONObject {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code} for $url")
            val body = resp.body?.string() ?: throw RuntimeException("Empty body for $url")
            return JSONObject(body)
        }
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? =
        try {
            if (isBlank()) null else BigDecimal(this)
        } catch (_: Exception) {
            null
        }
}
