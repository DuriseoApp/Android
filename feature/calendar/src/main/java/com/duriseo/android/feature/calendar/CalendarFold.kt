package com.duriseo.android.feature.calendar

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity

// SCR-CAL-01 쓸어서 펼치기·접기 (D-163). 판정은 순수 함수, 끌기 연결은 아래 두 곳이 같은 함수를 쓴다.

/** 한 번의 끌기에서 누적 이동량(px, 아래가 +)으로 정한 결과 */
internal enum class FoldChange { None, Expand, Collapse }

/** 누적 이동량이 기준(px) 이상이면 바꾼다. 아래로 끌면 펼치고 위로 끌면 접는다. 이미 그 상태면 그대로 */
internal fun foldChangeFor(totalDy: Float, expanded: Boolean, thresholdPx: Float): FoldChange = when {
    totalDy >= thresholdPx && !expanded -> FoldChange.Expand
    totalDy <= -thresholdPx && expanded -> FoldChange.Collapse
    else -> FoldChange.None
}

/** 달력·손잡이용: 끌기가 끝났을 때 누적 이동량으로 한 번 판정한다 (소스 `dragEnd`) */
internal fun Modifier.foldDrag(expanded: Boolean, thresholdPx: Float, onToggle: () -> Unit): Modifier = composed {
    val currentExpanded by rememberUpdatedState(expanded)
    val currentToggle by rememberUpdatedState(onToggle)
    pointerInput(thresholdPx) {
        var dragged = 0f
        detectVerticalDragGestures(
            onDragStart = { dragged = 0f },
            onDragCancel = { dragged = 0f },
            onDragEnd = {
                if (foldChangeFor(dragged, currentExpanded, thresholdPx) != FoldChange.None) currentToggle()
                dragged = 0f
            },
        ) { _, delta -> dragged += delta }
    }
}

/**
 * 날짜 목록용 (D-163): 펼친 상태에서 위로 끌면 목록이 스크롤되기 전에(onPreScroll) 이동량을 소비하며 접는다.
 * 접힌 상태에서 목록이 맨 위라 남은 아래 이동량(onPostScroll)이 기준 이상이면 펼친다.
 * 끌기 한 번(손가락을 뗄 때까지)에 한 번만 바꾼다.
 */
internal class FoldNestedScrollConnection(
    private val isExpanded: () -> Boolean,
    private val thresholdPx: Float,
    private val onToggle: () -> Unit,
) : NestedScrollConnection {
    private var accumulated = 0f
    private var toggled = false

    private fun reset() {
        accumulated = 0f
        toggled = false
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || toggled || !isExpanded() || available.y >= 0f) return Offset.Zero
        accumulated += available.y
        if (foldChangeFor(accumulated, expanded = true, thresholdPx = thresholdPx) == FoldChange.Collapse) {
            toggled = true
            onToggle()
        }
        // 접는 동안(기준에 닿을 때까지 포함) 목록이 같이 움직이지 않게 이동량을 소비한다
        return Offset(0f, available.y)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || toggled || isExpanded() || available.y <= 0f) return Offset.Zero
        accumulated += available.y
        if (foldChangeFor(accumulated, expanded = false, thresholdPx = thresholdPx) == FoldChange.Expand) {
            toggled = true
            onToggle()
        }
        // 남은 아래 이동량은 펼치기에 쓴 것으로 보고 소비한다 (당겨서 새로고침과 겹치지 않게)
        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        reset()
        return Velocity.Zero
    }
}
