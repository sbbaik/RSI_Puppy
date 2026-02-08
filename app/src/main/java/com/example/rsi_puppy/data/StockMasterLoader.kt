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
 * 1. getName(symbol) 함수 추가: 내부 심볼(005930.KS)을 UI용 이름(삼성전자)으로 변환합니다.
 * 2. 지수(Index) 매핑 확장: KOSPI200, KOSDAQ 등 주요 지수에 대한 양방향 변환을 지원합니다.
 * 3. 공백 및 대소문자 예외 처리 강화.
 */
object StockMasterLoader {

    private const val FILE_NAME = "stocks_kr.csv"
    private const val TAG = "StockMasterLoader"

    // 메모리 캐시: 검색 효율을 위해 두 종류의 Map을 운용합니다.
    private var nameMap: Map<String, StockMaster> = emptyMap()
    private var symbolMap: Map<String, StockMaster> = emptyMap()
    private var isLoaded = false

    /**
     * CSV 데이터를 메모리에 로드합니다.
     */
    fun load(context: Context) {
        if (isLoaded) return

        try {
            context.assets.open(FILE_NAME).use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { br ->
                    val allStocks = br.lineSequence()
                        .drop(1) // header 제외
                        .mapNotNull { line -> parseLine(line) }
                        .toList()

                    // 대소문자 구분 없는 검색을 위해 Key를 대문자로 통일
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
     * [서버 요청용] 종목명이나 코드를 서버가 이해하는 yfinance 심볼로 변환합니다.
     * 예: "삼성전자" -> "005930.KS", "KOSPI200" -> "^KS200"
     */
    fun getSymbol(query: String): String? {
        val q = query.trim().uppercase()
        if (q.isEmpty()) return null

        // 1. 종목명으로 검색
        nameMap[q]?.let { return it.symbol }

        // 2. 심볼(코드)로 검색
        symbolMap[q]?.let { return it.symbol }

        // 3. 지수 예외 처리
        val indexSymbol = when (q) {
            "KOSPI200" -> "^KS200"
            "KOSDAQ" -> "^KQ11"
            else -> null
        }
        if (indexSymbol != null) return indexSymbol

        // 4. 숫자 6자리만 온 경우 한국 시장(.KS) 기본 적용
        if (q.length == 6 && q.all { it.isDigit() }) {
            val ksQuery = "$q.KS"
            return symbolMap[ksQuery]?.symbol ?: ksQuery
        }

        return null
    }

    /**
     * [UI 표시용] 서버 심볼을 사용자가 보기 편한 종목명으로 변환합니다.
     * 예: "005930.KS" -> "삼성전자", "^KS200" -> "KOSPI200"
     */
    fun getName(symbol: String): String {
        val s = symbol.trim().uppercase()

        // 1. 마스터 데이터에서 이름 찾기
        symbolMap[s]?.let { return it.name }

        // 2. 지수 심볼 역변환
        return when (s) {
            "^KS200" -> "KOSPI200"
            "^KQ11" -> "KOSDAQ"
            else -> symbol // 매칭되는 이름이 없으면 심볼 자체를 반환 (예: 미등록 해외주식 등)
        }
    }

    private fun parseLine(line: String): StockMaster? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        val parts = trimmed.split(',')
        if (parts.size < 2) return null // 최소 name, symbol은 있어야 함

        val name = parts[0].trim()
        val symbol = parts[1].trim()
        val market = if (parts.size >= 3) parts[2].trim() else "KOSPI"

        if (name.isEmpty() || symbol.isEmpty()) return null

        return StockMaster(name, symbol, market)
    }
}