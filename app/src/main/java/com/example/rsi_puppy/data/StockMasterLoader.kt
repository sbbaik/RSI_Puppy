package com.example.rsi_puppy.data

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

data class StockMaster(
    val name: String,
    val symbol: String,
    val market: String,
)

/**
 * [수정 사항]
 * 1. searchStocks(query) 추가: 입력된 텍스트가 포함된 종목 리스트를 반환하여 자동완성 기능을 지원합니다.
 * 2. 기존 getName, getSymbol 등 핵심 매핑 로직 유지.
 * 3. 종목명 검색 시 대소문자 및 공백에 유연하게 대응합니다.
 */
object StockMasterLoader {

    private const val FILE_NAME = "stocks_kr.csv"
    private const val TAG = "StockMasterLoader"

    // 메모리 캐시: 검색 효율을 위해 Map과 원본 List를 병행 운용합니다.
    private var nameMap: Map<String, StockMaster> = emptyMap()
    private var symbolMap: Map<String, StockMaster> = emptyMap()
    private var allStocks: List<StockMaster> = emptyList()
    private var isLoaded = false

    /**
     * CSV 데이터를 메모리에 로드합니다.
     */
    fun load(context: Context) {
        if (isLoaded) return

        try {
            context.assets.open(FILE_NAME).use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { br ->
                    allStocks = br.lineSequence()
                        .drop(1) // header 제외
                        .mapNotNull { line -> parseLine(line) }
                        .toList()

                    // 빠른 조회를 위한 맵 생성
                    nameMap = allStocks.associateBy { it.name.uppercase() }
                    symbolMap = allStocks.associateBy { it.symbol.uppercase() }

                    isLoaded = true
                    Log.d(TAG, "Successfully loaded ${allStocks.size} stocks from CSV.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading CSV: ${e.message}")
        }
    }

    /**
     * [추가된 기능] 실시간 검색 제안용 함수
     * 사용자가 "커버드"라고 치면 "커버드콜" 관련 종목들을 리스트로 반환합니다.
     */
    fun searchStocks(query: String): List<StockMaster> {
        val q = query.trim().uppercase()
        if (q.isEmpty()) return emptyList()

        // 1. 이름에 검색어가 포함되거나 2. 심볼에 검색어가 포함된 항목 필터링
        // 상위 15개 정도만 반환하여 UI 쾌적함 유지
        return allStocks.filter {
            it.name.uppercase().contains(q) || it.symbol.uppercase().contains(q)
        }.take(15)
    }

    /**
     * [서버 요청용] 종목명이나 코드를 yfinance 심볼로 변환
     */
    fun getSymbol(query: String): String? {
        val q = query.trim().uppercase()
        if (q.isEmpty()) return null

        nameMap[q]?.let { return it.symbol }
        symbolMap[q]?.let { return it.symbol }

        val indexSymbol = when (q) {
            "KOSPI200" -> "^KS200"
            "KOSDAQ" -> "^KQ11"
            else -> null
        }
        if (indexSymbol != null) return indexSymbol

        if (q.length == 6 && q.all { it.isDigit() }) {
            val ksQuery = "$q.KS"
            return symbolMap[ksQuery]?.symbol ?: ksQuery
        }

        return null
    }

    /**
     * [UI 표시용] 심볼을 한글 종목명으로 변환
     */
    fun getName(symbol: String): String {
        val s = symbol.trim().uppercase()

        symbolMap[s]?.let { return it.name }

        return when (s) {
            "^KS200" -> "KOSPI200"
            "^KQ11" -> "KOSDAQ"
            else -> symbol
        }
    }

    private fun parseLine(line: String): StockMaster? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        val parts = trimmed.split(',')
        if (parts.size < 2) return null

        val name = parts[0].trim()
        val symbol = parts[1].trim()
        val market = if (parts.size >= 3) parts[2].trim() else "KOSPI"

        if (name.isEmpty() || symbol.isEmpty()) return null

        return StockMaster(name, symbol, market)
    }
}