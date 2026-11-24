package com.duriseo.android.feature.calendar

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// SCR-CAL-01 쓸어서 펼치기·접기 (D-163). 진행도 progress(0 = 접힘, 1 = 펼침)가 손가락을 따라가고,
// 손을 떼면 가까운 쪽(또는 튕긴 방향)으로 spring 으로 맞춘다. 판정은 순수 함수, 끌기 연결은 아래 두 곳이 같은 상태를 쓴다.

/** 이 속도(dp/초) 이상으로 튕기면 그 방향으로 맞춘다. 디자인 토큰이 아니라 동작 기준이다 */
internal const val FOLD_FLING_VELOCITY_DP = 600f

private const val DAYS_PER_WEEK = 7

/** 보이는 달이 차지하는 주 수 (일요일 시작) */
internal fun weekCount(month: YearMonth): Int {
    val leading = month.atDay(1).dayOfWeek.value % DAYS_PER_WEEK
    return (leading + month.lengthOfMonth() + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK
}

/** 끌기 이동량(px, 아래가 +)만큼 progress를 바꾼다. 범위는 (주 수 × (펼침 − 접힘 칸 높이)) px, 결과는 0..1 */
internal fun progressAfterDrag(progress: Float, dy: Float, rangePx: Float): Float =
    if (rangePx <= 0f) progress else (progress + dy / rangePx).coerceIn(0f, 1f)

/** 손을 뗐을 때 맞출 쪽(true = 펼침). 튕긴 속도가 기준 이상이면 그 방향, 아니면 0.5 기준 가까운 쪽 */
internal fun settleTarget(progress: Float, velocityPx: Float, flingThresholdPx: Float): Boolean = when {
    velocityPx >= flingThresholdPx -> true
    velocityPx <= -flingThresholdPx -> false
    else -> progress >= 0.5f
}

/** 점의 투명도: 앞쪽 절반(p 0..0.5)에 1 → 0, 그 뒤는 0 */
internal fun foldDotsAlpha(progress: Float): Float = (1f - 2f * progress).coerceIn(0f, 1f)

/** 막대의 투명도: 뒤쪽 절반(p 0.5..1)에 0 → 1, 그 앞은 0. 점과 막대가 동시에 보이지 않는다 */
internal fun foldBarsAlpha(progress: Float): Float = (2f * progress - 1f).coerceIn(0f, 1f)

/**
 * 달력 펼침 진행도. 끄는 동안은 [dragBy]로 바로 따라가고(재구성 없이 읽는 쪽이 layout·draw 단계에서 읽는다),
 * 손을 떼면 [settle], ViewModel 상태가 바뀌면 [animateTo]로 [Animatable]이 progress를 움직인다.
 * 맞춘 쪽이 ViewModel 상태와 다르면 애니메이션이 끝난 뒤 한 번만 [onToggle]을 보낸다.
 */
@Stable
internal class CalendarFoldState(
    initialProgress: Float,
    private val scope: CoroutineScope,
    private val flingThresholdPx: Float,
    private val isExpanded: () -> Boolean,
    private val onToggle: () -> Unit,
) {
    var progress: Float by mutableFloatStateOf(initialProgress)
        private set

    private var job: Job? = null

    /** 진행 중인 애니메이션을 멈추고 이동량만큼 따라간다. 실제로 쓴 이동량(px)을 돌려준다 */
    fun dragBy(dy: Float, rangePx: Float): Float {
        job?.cancel()
        job = null
        val before = progress
        progress = progressAfterDrag(before, dy, rangePx)
        return (progress - before) * rangePx
    }

    /** 손을 뗐다: 튕김·위치로 정한 쪽으로 맞춘다. 튕긴 속도를 spring 처음 속도로 넘긴다 */
    fun settle(velocityPx: Float, rangePx: Float) {
        val target = settleTarget(progress, velocityPx, flingThresholdPx)
        animateTo(target, if (rangePx > 0f) velocityPx / rangePx else 0f)
    }

    fun animateTo(expanded: Boolean, initialVelocity: Float = 0f) {
        job?.cancel()
        job = scope.launch {
            // 플랫폼 기본 spring (시간·곡선 토큰 없음)
            Animatable(progress).animateTo(if (expanded) 1f else 0f, initialVelocity = initialVelocity) {
                progress = value.coerceIn(0f, 1f)
            }
            if (expanded != isExpanded()) onToggle()
        }
    }
}

@Composable
internal fun rememberCalendarFoldState(expanded: Boolean, onToggle: () -> Unit): CalendarFoldState {
    val scope = rememberCoroutineScope()
    val flingPx = with(LocalDensity.current) { FOLD_FLING_VELOCITY_DP.dp.toPx() }
    val currentExpanded by rememberUpdatedState(expanded)
    val currentToggle by rememberUpdatedState(onToggle)
    val state = remember(scope, flingPx) {
        CalendarFoldState(
            initialProgress = if (expanded) 1f else 0f,
            scope = scope,
            flingThresholdPx = flingPx,
            isExpanded = { currentExpanded },
            onToggle = { currentToggle() },
        )
    }
    // 접근성 동작 등 다른 경로로 상태가 바뀌어도 progress가 그 값으로 간다 (이미 그 값이면 바로 끝난다)
    LaunchedEffect(expanded, state) { state.animateTo(expanded) }
    return state
}

/** 달력·손잡이용: 끄는 동안 이동량을 progress로 넘기고, 손을 떼면 속도와 함께 맞춘다 */
internal fun Modifier.foldDrag(fold: CalendarFoldState, rangePx: Float): Modifier = composed {
    val currentRange by rememberUpdatedState(rangePx)
    val dragState = rememberDraggableState { delta -> fold.dragBy(delta, currentRange) }
    draggable(
        state = dragState,
        orientation = Orientation.Vertical,
        onDragStopped = { velocity -> fold.settle(velocity, currentRange) },
    )
}

/**
 * 날짜 목록용 (D-163): progress가 0보다 크면 위로 끄는 이동량을 목록이 스크롤되기 전에(onPreScroll) progress가 먼저 쓰고,
 * 목록이 맨 위라 남은 아래 이동량(onPostScroll)은 progress를 늘린다. 한 번 쓰기 시작한 끌기에서는 남은 아래 이동량을
 * 계속 소비한다(당겨서 새로고침과 겹치지 않게). 손을 떼면(onPreFling) 속도와 함께 맞춘다.
 */
internal class FoldNestedScrollConnection(
    private val progress: () -> Float,
    private val dragBy: (Float) -> Float,
    private val onRelease: (velocityY: Float) -> Unit,
) : NestedScrollConnection {
    /** 이번 끌기에서 progress를 움직였는지 */
    private var active = false

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || available.y >= 0f || progress() <= 0f) return Offset.Zero
        val consumed = dragBy(available.y)
        if (consumed != 0f) active = true
        return Offset(0f, consumed)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
        // 이미 펼친 상태에서 시작한 끌기의 아래 이동량은 당겨서 새로고침에 맡긴다
        if (!active && progress() >= 1f) return Offset.Zero
        val used = dragBy(available.y)
        if (used != 0f) active = true
        return Offset(0f, if (active) available.y else used)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (!active) return Velocity.Zero
        active = false
        onRelease(available.y)
        // 맞추는 중에 목록이 같이 튀지 않게 속도를 쓴다
        return Velocity(0f, available.y)
    }
}
