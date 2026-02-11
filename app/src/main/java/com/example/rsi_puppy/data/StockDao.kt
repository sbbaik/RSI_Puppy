package com.example.rsi_puppy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    // 모든 종목 정보를 가져옵니다.
    // Flow를 사용하면 DB 값이 바뀔 때마다 UI가 자동으로 업데이트됩니다.
    @Query("SELECT * FROM stocks")
    fun getAllStocks(): Flow<List<StockEntity>>

    // 특정 종목의 RSI 값을 업데이트하거나, 없으면 새로 넣습니다.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stock: StockEntity)

    // 종목 전체 개수 (배지에 표시할 count 계산용)
    @Query("SELECT COUNT(*) FROM stocks WHERE rsi >= 70 OR (rsi <= 30 AND rsi > 0)")
    fun getAlertCountFlow(): Flow<Int>

    // 특정 종목 하나만 가져오기
    @Query("SELECT * FROM stocks WHERE symbol = :symbol")
    suspend fun getStockBySymbol(symbol: String): StockEntity?

    // 모든 데이터 삭제 (필요 시)
    @Query("DELETE FROM stocks")
    suspend fun deleteAll()
}