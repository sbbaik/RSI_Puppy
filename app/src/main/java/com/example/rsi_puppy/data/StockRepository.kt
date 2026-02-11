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

    // --- [DataStore 관련: 종목 순서 관리] ---
    private val MONITORED_STOCKS_ORDER = stringPreferencesKey("monitored_stocks_order")

    // 1. 저장된 종목 리스트(심볼)를 순서대로 가져오기
    val monitoredStocks: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            val rawString = preferences[MONITORED_STOCKS_ORDER] ?: ""
            if (rawString.isEmpty()) emptyList() else rawString.split(",")
        }

    // --- [Room DB 관련: RSI 상세 데이터 관리] ---
    private val stockDao = AppDatabase.getDatabase(context).stockDao()

    // 2. 모든 종목의 RSI 데이터를 실시간으로 관찰 (UI 바인딩용)
    val allStockData: Flow<List<StockEntity>> = stockDao.getAllStocks()

    // 3. 알림 대상 종목 개수 관찰 (배지 카운트용)
    val alertCount: Flow<Int> = stockDao.getAlertCountFlow()

    // 4. RSI 값 업데이트 (Worker에서 호출)
    suspend fun updateRsiValue(symbol: String, rsi: Double) {
        val entity = StockEntity(
            symbol = symbol,
            rsi = rsi,
            lastUpdated = System.currentTimeMillis()
        )
        stockDao.insertOrUpdate(entity)
    }


    // --- [기존 기능 유지: 종목 추가/삭제/순서변경] ---

    // 종목 추가
    suspend fun addStock(symbol: String) {
        context.dataStore.edit { preferences ->
            val rawString = preferences[MONITORED_STOCKS_ORDER] ?: ""
            val currentList = if (rawString.isEmpty()) emptyList() else rawString.split(",")

            if (!currentList.contains(symbol)) {
                val newList = currentList + symbol
                preferences[MONITORED_STOCKS_ORDER] = newList.joinToString(",")

                // [추가] Room DB에도 초기값(0)으로 등록하여 UI 준비
                updateRsiValue(symbol, 0.0)
            }
        }
    }

    // 종목 삭제
    suspend fun removeStock(symbol: String) {
        context.dataStore.edit { preferences ->
            val rawString = preferences[MONITORED_STOCKS_ORDER] ?: ""
            val currentList = if (rawString.isEmpty()) emptyList() else rawString.split(",")

            val newList = currentList - symbol
            preferences[MONITORED_STOCKS_ORDER] = newList.joinToString(",")

            // [추가] Room DB에서도 해당 종목 삭제 로직을 넣고 싶다면 Dao에 Delete 추가 후 여기서 호출
        }
    }

    // 순서 변경 후 리스트 전체 갱신 (드래그 앤 드롭 결과 저장용)
    suspend fun updateStockOrder(newList: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[MONITORED_STOCKS_ORDER] = newList.joinToString(",")
        }
    }
}