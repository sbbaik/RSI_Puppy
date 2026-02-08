package com.example.rsi_puppy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

data class RsiRowUi(
    val name: String,
    val rsi: Int
)

private fun isOverbought(rsi: Int) = rsi >= 70
private fun isOversold(rsi: Int) = rsi <= 30

private val MENU_SLOT = 40.dp
private val RSI_COL_WIDTH: Dp = 56.dp
private val OversoldBlue = Color(0xFF1565C0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainRsiScreen(
    rows: SnapshotStateList<RsiRowUi>,
    onAddClick: () -> Unit = {}
) {
    val lazyListState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        rows.add(to.index, rows.removeAt(from.index))
        haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RSI 알리미",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onAddClick) {
                        Icon(Icons.Filled.Add, contentDescription = "Add")
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ---- 컬럼 헤더: 배경을 "세련된 톤"으로 추가 (surfaceVariant + 약한 알파)
            val headerBg =
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(start = 40.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "  종목",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "RSI",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .width(RSI_COL_WIDTH)
                        .padding(end = 8.dp),
                    textAlign = TextAlign.End
                )

                Spacer(Modifier.width(MENU_SLOT))
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = rows,
                    key = { _, item -> item.name }
                ) { index, row ->

                    ReorderableItem(
                        state = reorderState,
                        key = row.name
                    ) { isDragging ->
                        StockRow(
                            scope = this,
                            row = row,
                            isDragging = isDragging,
                            canMoveUp = index > 0,
                            canMoveDown = index < rows.lastIndex,
                            onMoveUp = { if (index > 0) rows.swap(index, index - 1) },
                            onMoveDown = { if (index < rows.lastIndex) rows.swap(index, index + 1) },
                            onDelete = { rows.removeAt(index) }
                        )
                    }

                    if (index < rows.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockRow(
    scope: ReorderableCollectionItemScope,
    row: RsiRowUi,
    isDragging: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val rowBg = when {
        isOverbought(row.rsi) -> MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
        isOversold(row.rsi) -> OversoldBlue.copy(alpha = 0.10f)
        else -> Color.Transparent
    }

    val rsiColor = when {
        isOverbought(row.rsi) -> MaterialTheme.colorScheme.error
        isOversold(row.rsi) -> OversoldBlue
        else -> MaterialTheme.colorScheme.onSurface
    }

    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isDragging) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else rowBg
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = row.name,
            fontSize = 19.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = row.rsi.toString(),
            fontSize = 19.sp,
            color = rsiColor,
            modifier = Modifier.width(RSI_COL_WIDTH),
            textAlign = TextAlign.End
        )

        Box(
            modifier = Modifier.size(MENU_SLOT),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { menuOpen = true },
                modifier = with(scope) { Modifier.draggableHandle() }
            ) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }

            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text("위로 이동") },
                    onClick = { menuOpen = false; onMoveUp() },
                    enabled = canMoveUp
                )
                DropdownMenuItem(
                    text = { Text("아래로 이동") },
                    onClick = { menuOpen = false; onMoveDown() },
                    enabled = canMoveDown
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("삭제") },
                    onClick = { menuOpen = false; onDelete() }
                )
            }
        }
    }
}

private fun <T> SnapshotStateList<T>.swap(i: Int, j: Int) {
    val tmp = this[i]
    this[i] = this[j]
    this[j] = tmp
}
