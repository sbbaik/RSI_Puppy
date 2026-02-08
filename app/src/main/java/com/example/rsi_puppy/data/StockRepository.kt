package com.example.rsi_puppy.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Context 확장 프로퍼티로 DataStore 선언
private val Context.dataStore by preferencesDataStore(name = "rsi_settings")

class StockRepository(private val context: Context) {

    // 순서를 유지하기 위해 StringSet 대신 String으로 저장 (쉼표로 구분)
    private val MONITORED_STOCKS_ORDER = stringPreferencesKey("monitored_stocks_order")

    // 1. 저장된 종목 리스트를 순서대로 가져오기
    val monitoredStocks: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            val rawString = preferences[MONITORED_STOCKS_ORDER] ?: ""
            if (rawString.isEmpty()) {
                // 저장된 데이터가 없으면 빈 리스트 반환 (MainActivity에서 기본값 처리)
                emptyList()
            } else {
                // 쉼표로 분리하여 리스트로 변환
                rawString.split(",")
            }
        }

    // 2. 종목 추가 (중복 방지 로직 포함 및 순서 유지)
    suspend fun addStock(symbol: String) {
        context.dataStore.edit { preferences ->
            val rawString = preferences[MONITORED_STOCKS_ORDER] ?: ""
            val currentList = if (rawString.isEmpty()) emptyList() else rawString.split(",")

            if (!currentList.contains(symbol)) {
                val newList = currentList + symbol
                preferences[MONITORED_STOCKS_ORDER] = newList.joinToString(",")
            }
        }
    }

    // 3. 종목 삭제
    suspend fun removeStock(symbol: String) {
        context.dataStore.edit { preferences ->
            val rawString = preferences[MONITORED_STOCKS_ORDER] ?: ""
            val currentList = if (rawString.isEmpty()) emptyList() else rawString.split(",")

            val newList = currentList - symbol
            preferences[MONITORED_STOCKS_ORDER] = newList.joinToString(",")
        }
    }

    // 4. 순서 변경 후 리스트 전체 갱신 (드래그 앤 드롭 결과 저장용)
    suspend fun updateStockOrder(newList: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[MONITORED_STOCKS_ORDER] = newList.joinToString(",")
        }
    }
}