package com.example.rsi_puppy.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.math.BigDecimal

class TwelveDataClient(
    private val apiKey: String,
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun fetchDailyCloses(symbol: String, exchange: String = "XKRX", outputSize: Int = 120): List<BigDecimal> =
        withContext(Dispatchers.IO) {
            val url =
                "https://api.twelvedata.com/time_series" +
                        "?symbol=$symbol&exchange=$exchange&interval=1day&outputsize=$outputSize&apikey=$apiKey"

            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
                val body = resp.body?.string() ?: throw RuntimeException("empty body")

                val json = JSONObject(body)
                if (json.has("status") && json.getString("status") == "error") {
                    throw RuntimeException(json.optString("message", "api error"))
                }

                val values = json.getJSONArray("values")
                // values는 최신→과거 순서인 경우가 많으니 RSI 계산용으로 과거→최신 정렬
                val closes = mutableListOf<BigDecimal>()
                for (i in 0 until values.length()) {
                    val item = values.getJSONObject(i)
                    closes.add(BigDecimal(item.getString("close")))
                }
                closes.reversed()
            }
        }
}
