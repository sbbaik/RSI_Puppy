package com.example.rsi_puppy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateListOf
import com.example.rsi_puppy.ui.MainRsiScreen
import com.example.rsi_puppy.ui.RsiRowUi
import com.example.rsi_puppy.ui.theme.RSI_PuppyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RSI_PuppyTheme {
                // 드래그 정렬/삭제가 동작하려면 "변경 가능한 리스트"가 필요합니다.
                val rows = mutableStateListOf(
                    RsiRowUi("KOSPI200", 63),
                    RsiRowUi("KOSDAQ", 73),
                    RsiRowUi("KT", 57),
                    RsiRowUi("Samsung", 70),
                    RsiRowUi("LG", 78),
                )

                MainRsiScreen(
                    rows = rows,
                    onAddClick = {
                        // 2단계(종목 검색 다이얼로그)에서 연결
                        // 지금은 임시로 하나 추가해도 됩니다:
                        // rows.add(RsiRowUi("NEW", 50))
                    }
                )
            }
        }
    }
}
