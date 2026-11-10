package com.duriseo.android.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink400
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Surface100
import java.time.LocalTime

// 시간 휠 (design-system.md "시간 휠 (TimeWheel)", 보드 `AddEvent.dc.html` 144~163행·275~290행, SCR-CAL-03).
// Material 3에는 휠 시간 선택이 없어 직접 만든다. 새 토큰은 없다 (줄 높이 44 = `size-touch`, 띠 모서리 14 = `radius-md`).

/**
 * 휠이 값을 읽는 순수 계산. 화면 없이 단위 테스트한다 (`TimeWheelValuesTest`).
 * 오전/오후 휠은 [오전, 오후], 시 휠은 1~12, 분 휠은 0~59(1분 단위)이고 각 휠의 값 목록 위치가 곧 선택 위치다.
 */
object TimeWheelValues {
    /** 한 휠에 보이는 줄 수. 가운데가 선택, 위아래 두 줄씩 (보드: 5줄 = 220) */
    const val VISIBLE_ROWS = 5

    /** 가운데 위·아래로 보이는 줄 수 */
    const val ROWS_AROUND = 2

    val HOURS: List<Int> = (1..12).toList()
    val MINUTES: List<Int> = (0..59).toList()

    /** 24시간 값 → 12시간 값 (0시·12시는 12) */
    fun hour12(hour24: Int): Int = if (hour24 % 12 == 0) 12 else hour24 % 12

    fun isPm(hour24: Int): Boolean = hour24 >= 12

    /** 12시간 값과 오전/오후 → 24시간 값. 오전 12시는 0시, 오후 12시는 12시 */
    fun hour24(hour12: Int, pm: Boolean): Int = hour12 % 12 + if (pm) 12 else 0

    fun time(pm: Boolean, hour12: Int, minute: Int): LocalTime = LocalTime.of(hour24(hour12, pm), minute)

    /**
     * 스크롤 위치 → 가운데 줄에 걸린 값의 위치. 가운데 줄이 맨 위 줄 자리에 오도록 위아래에 두 줄 여백을 둔 목록에서,
     * 첫 줄 위치와 그 줄이 밀려난 픽셀로 전체 스크롤 픽셀을 구하고 가장 가까운 줄로 반올림한다.
     */
    fun selectedIndex(firstVisibleIndex: Int, firstVisibleOffsetPx: Int, rowHeightPx: Int, itemCount: Int): Int {
        if (rowHeightPx <= 0 || itemCount <= 0) return 0
        val scrollPx = firstVisibleIndex * rowHeightPx + firstVisibleOffsetPx
        return ((scrollPx + rowHeightPx / 2) / rowHeightPx).coerceIn(0, itemCount - 1)
    }

    /** 가운데에서 몇 줄 떨어졌는지(0, 1, 2 이상) → 글자 모양 구분 */
    fun distanceFromSelected(index: Int, selectedIndex: Int): Int = kotlin.math.abs(index - selectedIndex)
}


/** 줄 높이 44, 휠 사이 8, 가운데 띠는 휠 위에서 두 줄(88) 아래 */
private val RowHeight = DuriseoSize.Touch
private val WheelGap = DuriseoSpacing.Space2

/**
 * 시간 휠 3줄(오전/오후 · 시 · 분). 같은 폭으로 나란히 두고 가운데 줄에 `surface-100` 띠가 세 휠에 걸쳐 이어진다.
 * 가운데 줄 22/30/700 `ink-900`, 위·아래 첫 줄 16/24/500 `ink-500`, 둘째 줄 `ink-400`(D-128: 글자에 쓰는 예외).
 * 스크롤이 멈추면 가장 가까운 줄로 붙는다. 값이 바뀔 때마다 [onTimeChange]로 알린다.
 *
 * @param time 처음 가운데에 놓을 시각 (이후 값은 휠이 들고 있다)
 * @param description 휠 묶음의 접근성 문구. 보드: "시작 시간 오후 7:30"
 */
@Composable
fun DuriseoTimeWheel(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    var pm by remember { mutableStateOf(TimeWheelValues.isPm(time.hour)) }
    var hour12 by remember { mutableStateOf(TimeWheelValues.hour12(time.hour)) }
    var minute by remember { mutableStateOf(time.minute) }
    val meridiems = listOf(stringResource(R.string.time_wheel_am), stringResource(R.string.time_wheel_pm))
    val bandShape = MaterialTheme.shapes.medium
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight * TimeWheelValues.VISIBLE_ROWS)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = RowHeight * TimeWheelValues.ROWS_AROUND)
                .height(RowHeight)
                .clip(bandShape)
                .background(Surface100)
                .clearAndSetSemantics { },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WheelGap),
        ) {
            val wheelModifier = Modifier.weight(1f)
            TimeWheelColumn(
                items = meridiems,
                initialIndex = if (pm) 1 else 0,
                label = stringResource(R.string.time_wheel_meridiem),
                onSelected = {
                    pm = it == 1
                    onTimeChange(TimeWheelValues.time(pm, hour12, minute))
                },
                modifier = wheelModifier,
            )
            TimeWheelColumn(
                items = TimeWheelValues.HOURS.map { it.toString() },
                initialIndex = TimeWheelValues.HOURS.indexOf(hour12),
                label = stringResource(R.string.time_wheel_hour),
                onSelected = {
                    hour12 = TimeWheelValues.HOURS[it]
                    onTimeChange(TimeWheelValues.time(pm, hour12, minute))
                },
                modifier = wheelModifier,
            )
            TimeWheelColumn(
                items = TimeWheelValues.MINUTES.map { it.toString() },
                initialIndex = TimeWheelValues.MINUTES.indexOf(minute),
                label = stringResource(R.string.time_wheel_minute),
                onSelected = {
                    minute = TimeWheelValues.MINUTES[it]
                    onTimeChange(TimeWheelValues.time(pm, hour12, minute))
                },
                modifier = wheelModifier,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimeWheelColumn(
    items: List<String>,
    initialIndex: Int,
    label: String,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex.coerceAtLeast(0))
    val rowPx = with(LocalDensity.current) { RowHeight.roundToPx() }
    val selected by remember(items.size, rowPx) {
        derivedStateOf {
            TimeWheelValues.selectedIndex(
                firstVisibleIndex = listState.firstVisibleItemIndex,
                firstVisibleOffsetPx = listState.firstVisibleItemScrollOffset,
                rowHeightPx = rowPx,
                itemCount = items.size,
            )
        }
    }
    LaunchedEffect(selected) { onSelected(selected) }
    LazyColumn(
        modifier = modifier
            .height(RowHeight * TimeWheelValues.VISIBLE_ROWS)
            .semantics { contentDescription = label },
        state = listState,
        contentPadding = PaddingValues(vertical = RowHeight * TimeWheelValues.ROWS_AROUND),
        flingBehavior = rememberSnapFlingBehavior(listState),
    ) {
        items(items.size) { index ->
            val distance = TimeWheelValues.distanceFromSelected(index, selected)
            Box(modifier = Modifier.height(RowHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = items[index],
                    style = when (distance) {
                        0 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    },
                    // 가운데 줄 `ink-900`, 첫 줄 `ink-500`, 둘째 줄부터 `ink-400` (D-128). 값은 가운데 줄이 전달한다
                    color = rowColor(distance),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun rowColor(distance: Int): Color = when (distance) {
    0 -> Ink900
    1 -> Ink500
    else -> Ink400
}

/** `SCR-CAL-03--time-picker.png`: 오후 7:30 (시 5~9, 분 28~32가 보인다) */
@Preview(showBackground = true, widthDp = 342)
@Composable
private fun DuriseoTimeWheelPreview() {
    DuriseoTheme { DuriseoTimeWheel(time = LocalTime.of(19, 30), onTimeChange = {}) }
}

/** 오전 12:00(자정): 시 휠 12, 위쪽 줄은 비어 있다 */
@Preview(showBackground = true, widthDp = 342)
@Composable
private fun DuriseoTimeWheelMidnightPreview() {
    DuriseoTheme { DuriseoTimeWheel(time = LocalTime.MIDNIGHT, onTimeChange = {}) }
}
