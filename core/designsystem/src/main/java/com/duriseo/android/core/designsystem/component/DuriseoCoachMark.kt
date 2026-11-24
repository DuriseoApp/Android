package com.duriseo.android.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.core.designsystem.theme.DuriseoMaterialShapes
import com.duriseo.android.core.designsystem.theme.DuriseoShadows
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.duriseoShadow
import kotlin.math.roundToInt

// 코치 마크(튜토리얼) 값 (SCREENS.md SCR-CAL-01 "튜토리얼", FR-CAL-06, D-168).
// 덮개(`scrim`)로 화면 전체를 어둡게 하고 설명할 곳만 구멍을 뚫는다. 덮개는 다른 시트·다이얼로그와 같은 별도 창
// (DuriseoOverlayWindow)에 그려 하단 탭까지 덮는다. 화면 안에 scrim Box를 직접 그리지 않는다 (OverlayRulesTest).

/**
 * 코치 마크 대상의 창 기준 위치 모음. 화면(캘린더)과 하단 탭이 각자 [coachMarkTarget]으로 자기 위치를 넣고,
 * 튜토리얼이 꺼내 쓴다. 키는 호출하는 쪽이 정한다 (캘린더 튜토리얼은 단계 enum).
 */
@Stable
class CoachMarkTargets {
    private val rects = mutableStateMapOf<Any, Rect>()

    /** 지금 위치를 알고 있는 대상들 (Compose 상태라 읽으면 바뀔 때 다시 그려진다) */
    val keys: Set<Any> get() = rects.keys

    fun rectOf(key: Any): Rect? = rects[key]

    internal fun update(key: Any, rect: Rect) {
        if (rects[key] != rect) rects[key] = rect
    }

    internal fun remove(key: Any) {
        rects.remove(key)
    }
}

/** 코치 마크 대상 모음. 없으면(Preview, 튜토리얼이 없는 화면) [coachMarkTarget]은 아무것도 하지 않는다 */
val LocalCoachMarkTargets = compositionLocalOf<CoachMarkTargets?> { null }

/**
 * 이 요소를 코치 마크 대상 [key]로 등록한다. 배치가 바뀔 때마다 창 기준 위치(`boundsInWindow`)를 알리고,
 * 화면에서 사라지면 지운다. 덮개 창도 화면 전체라서 같은 좌표계다.
 */
fun Modifier.coachMarkTarget(key: Any): Modifier = composed {
    val targets = LocalCoachMarkTargets.current
    if (targets == null) {
        Modifier
    } else {
        DisposableEffect(targets, key) { onDispose { targets.remove(key) } }
        Modifier.onGloballyPositioned { targets.update(key, it.boundsInWindow()) }
    }
}

/** 구멍은 대상 둘레에서 8씩 넓다 (SCREENS.md "모양") */
private val HoleOutset = DuriseoSpacing.Space2

/** 말풍선은 구멍에서 12 떨어지고, 좌우는 화면 끝에서 20 안쪽이다 (SCREENS.md "말풍선") */
private val BubbleGap = DuriseoSpacing.Space3
private val BubbleSide = DuriseoSpacing.Space5

/**
 * 코치 마크 한 단계. 화면 전체를 `scrim`으로 덮고 [target]만 구멍을 뚫어 보여 주며, 구멍 아래(대상이 화면 아래쪽 반에 있으면 위)에
 * 말풍선을 띄운다. 단계가 바뀌면 구멍과 말풍선이 플랫폼 기본 애니메이션(시간·곡선 토큰 없음)으로 옮겨 간다.
 * 단계마다 새로 열지 않고 같은 레이어에 값만 바꿔 넘긴다.
 *
 * - 화면 아무 곳이나(구멍 안 포함) 누르면 [onNext]. 구멍 안의 기능은 실행되지 않는다 (별도 창이 누름을 받는다).
 * - [건너뛰기]와 뒤로 가기는 [onSkip].
 * - 접근성: 덮개 누름 영역과 말풍선은 형제 노드라서 [건너뛰기]는 독립 버튼으로 읽힌다. 진행 표시와 설명 문구는 각각 읽히고
 *   (단계가 바뀌면 다시 읽는다) 두 번 누르면 [onNext]다. 새 접근성 문구는 없다.
 * - [target]이 없거나 비어 있으면(회전 직후, 탭 전환 등 아직 못 쟀을 때) 아무것도 그리지 않고 기다린다. 다시 재면 같은 단계로 이어서
 *   그리며, 처음 나타날 때는 날아오지 않고 대상 자리에서 시작한다.
 *
 * @param target 창 기준 대상 위치
 * @param holeShape 구멍 모서리. 대상 모양을 따른다 (달력 `radius-lg`, FAB `radius-fab`, 아이콘 버튼·탭 항목·링크 `radius-full`)
 */
@Composable
fun DuriseoCoachMarkLayer(
    target: Rect?,
    holeShape: CornerBasedShape,
    message: String,
    stepNumber: Int,
    stepCount: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (target == null || target.isEmpty) return
    DuriseoOverlayWindow(onBack = onSkip, showSnackbar = false) {
        CoachMarkContent(target, holeShape, message, stepNumber, stepCount, onNext, onSkip, modifier)
    }
}

@Composable
private fun CoachMarkContent(
    target: Rect,
    holeShape: CornerBasedShape,
    message: String,
    stepNumber: Int,
    stepCount: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    val scrim = MaterialTheme.colorScheme.scrim
    val hole = target.inflate(with(density) { HoleOutset.toPx() })
    // 처음에는 대상 자리에서 시작하고(이 구성은 대상이 있을 때만 만들어진다), 단계가 바뀌면 새 대상으로 옮겨 간다
    val animatedHole = animateRectAsState(hole, label = "coachMarkHole")
    val animatedRadius = animateFloatAsState(
        targetValue = holeShape.topStart.toPx(hole.size, density),
        label = "coachMarkHoleRadius",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
                addRoundRect(RoundRect(animatedHole.value, CornerRadius(animatedRadius.value)))
            }
            drawPath(path, scrim)
        }
        // 덮개 누름 영역: 화면 아무 곳이나 누르면 다음 단계. 말풍선과 형제라서 [건너뛰기]를 감싸지 않는다.
        // 접근성에서는 뺀다 (이름 없는 버튼을 만들지 않고, 낭독기 두 번 누르기는 아래 말풍선 글자의 클릭이 받는다)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clearAndSetSemantics { }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onNext,
                ),
        )
        Layout(
            content = {
                CoachMarkBubble(
                    message = message,
                    stepNumber = stepNumber,
                    stepCount = stepCount,
                    onNext = onNext,
                    onSkip = onSkip,
                )
            },
            modifier = Modifier.fillMaxSize(),
        ) { measurables, constraints ->
            val side = BubbleSide.roundToPx()
            val gap = BubbleGap.roundToPx()
            val width = (constraints.maxWidth - side * 2).coerceAtLeast(0)
            val bubble = measurables.single().measure(
                Constraints(minWidth = width, maxWidth = width, maxHeight = constraints.maxHeight),
            )
            val current = animatedHole.value
            val maxY = (constraints.maxHeight - bubble.height).coerceAtLeast(0)
            val below = current.bottom + gap
            val above = current.top - gap - bubble.height
            // 대상이 화면 아래쪽 반에 있으면 구멍 위, 아니면 구멍 아래가 먼저다. 모자라면 반대쪽, 양쪽 다 모자라면(큰 대상)
            // 구멍 안쪽 아래 끝에서 위로 붙여 화면 안에 둔다
            val fitsBelow = below + bubble.height <= constraints.maxHeight
            val fitsAbove = above >= 0f
            val y = if (hole.center.y > constraints.maxHeight / 2f) {
                when {
                    fitsAbove -> above
                    fitsBelow -> below
                    else -> current.bottom - bubble.height
                }
            } else {
                when {
                    fitsBelow -> below
                    fitsAbove -> above
                    else -> current.bottom - bubble.height
                }
            }
            layout(constraints.maxWidth, constraints.maxHeight) {
                bubble.place(side, y.roundToInt().coerceIn(0, maxY))
            }
        }
    }
}

/**
 * 말풍선: `surface-0` 면, 모서리 `radius-lg`, `shadow-dialog`, 안쪽 여백 16. 꼬리는 없다.
 * 맨 위 줄 왼쪽에 진행 표시("1/5", `caption` `ink-500`), 오른쪽에 텍스트 버튼 [건너뛰기](`label` `ink-500`), 아래에 설명 문구(`body-md` `ink-900`).
 * 진행 표시와 설명 문구는 누르면 다음 단계이고(낭독기 두 번 누르기 포함) 바뀌면 다시 읽는다. [건너뛰기]는 독립 버튼이다.
 */
@Composable
private fun CoachMarkBubble(
    message: String,
    stepNumber: Int,
    stepCount: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
    // 글자를 눌러도 덮개를 누른 것과 같다: 다음 단계. 눌림 효과는 없다
    val nextOnClick = Modifier
        .semantics { liveRegion = LiveRegionMode.Polite }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onNext,
        )
    Column(
        modifier = modifier
            .duriseoShadow(shape, DuriseoShadows.Dialog)
            .background(MaterialTheme.colorScheme.surface, shape)
            .padding(DuriseoSpacing.Space4),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.coach_mark_progress, stepNumber, stepCount),
                modifier = Modifier.weight(1f).then(nextOnClick),
                style = MaterialTheme.typography.labelMedium,
                color = Ink500,
            )
            // 터치 영역은 44 이상 (size-touch). 독립 버튼으로 읽힌다
            Box(
                modifier = Modifier
                    .heightIn(min = DuriseoSize.Touch)
                    .clickable(role = Role.Button, onClick = onSkip)
                    .padding(horizontal = DuriseoSpacing.Space2),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.coach_mark_skip),
                    style = MaterialTheme.typography.labelLarge,
                    color = Ink500,
                )
            }
        }
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth().then(nextOnClick),
            style = MaterialTheme.typography.bodyMedium,
            color = Ink900,
        )
    }
}

// 견본 글자(앱 문구가 아니다)와 가짜 대상으로 그린다. 위치는 390x844 화면 기준 dp를 px로 바꿔 쓴다.

@Composable
private fun CoachMarkPreview(
    target: DpRect,
    shape: CornerBasedShape,
    step: Int,
) {
    DuriseoTheme {
        val density = LocalDensity.current
        val rect = with(density) {
            Rect(target.left.toPx(), target.top.toPx(), target.right.toPx(), target.bottom.toPx())
        }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            DuriseoCoachMarkLayer(
                target = rect,
                holeShape = shape,
                message = "Coach mark sample $step",
                stepNumber = step,
                stepCount = 5,
                onNext = {},
                onSkip = {},
            )
        }
    }
}

private fun dpRect(left: Int, top: Int, right: Int, bottom: Int) = DpRect(left.dp, top.dp, right.dp, bottom.dp)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CoachMarkLargeHolePreview() =
    CoachMarkPreview(dpRect(8, 190, 382, 360), DuriseoMaterialShapes.large, 1)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CoachMarkFabHolePreview() =
    CoachMarkPreview(dpRect(314, 664, 370, 720), DuriseoShapes.Fab, 2)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CoachMarkFullHolePreview() =
    CoachMarkPreview(dpRect(100, 14, 240, 50), DuriseoShapes.Full, 3)
