package com.example.rsi_puppy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey val symbol: String, // 종목 코드 (예: "AAPL")
    val rsi: Double,               // 계산된 RSI 값
    val lastUpdated: Long          // 마지막 업데이트 시간
)