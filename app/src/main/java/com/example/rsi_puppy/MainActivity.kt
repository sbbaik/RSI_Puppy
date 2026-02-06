package com.example.rsi_puppy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.rsi_puppy.data.StockDataSource
import com.example.rsi_puppy.domain.RsiCalculator
import com.example.rsi_puppy.ui.theme.RSI_PuppyTheme
import com.example.rsi_puppy.worker.WorkScheduler
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 알림용: 15분 주기 + 실행 즉시 1회(이미 구현돼 있다면 유지)
        WorkScheduler.scheduleRsiCheck(this, "030200")
        WorkScheduler.runOnceNow(this, "030200")

        enableEdgeToEdge()
        setContent {
            RSI_PuppyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RsiScreen(
                        symbol = "030200",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun RsiScreen(symbol: String, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var statusText by remember { mutableStateOf("RSI 계산 중...") }

    LaunchedEffect(symbol) {
        scope.launch {
            try {
                val dataSource = StockDataSource()
                val closes = dataSource.fetchDailyCloseHistory(symbol, days = 90)

                val rsi = RsiCalculator(14).calculateLatestRsi(closes)
                statusText = if (rsi == null) {
                    "$symbol RSI 계산 실패(데이터 부족)"
                } else {
                    "$symbol RSI = ${"%.2f".format(rsi)}"
                }
            } catch (e: Exception) {
                statusText = "$symbol RSI 조회 실패: ${e.message ?: "unknown error"}"
            }
        }
    }

    Text(text = statusText, modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun PreviewRsiScreen() {
    RSI_PuppyTheme {
        RsiScreen(symbol = "030200")
    }
}
