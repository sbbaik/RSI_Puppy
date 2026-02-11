package com.example.rsi_puppy.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 1. 사용할 Entity들과 버전을 명시합니다.
@Database(entities = [StockEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // 2. Dao를 가져올 수 있는 추상 함수를 선언합니다.
    abstract fun stockDao(): StockDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 3. 싱글톤 패턴으로 DB 인스턴스를 하나만 생성하도록 관리합니다.
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rsi_puppy_database" // DB 파일 이름
                )
                    .fallbackToDestructiveMigration() // 스키마 변경 시 기존 데이터 삭제 후 재생성 (개발 단계에서 편리)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}