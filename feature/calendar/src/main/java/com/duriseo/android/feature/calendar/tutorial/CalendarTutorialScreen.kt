package com.duriseo.android.feature.calendar.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.component.DuriseoCoachMarkLayer
import com.duriseo.android.core.designsystem.component.LocalCoachMarkTargets
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.feature.calendar.CalendarScreen
import com.duriseo.android.feature.calendar.CalendarUiState

/**
 * 캘린더 튜토리얼을 앱 화면 위에 그린다 (FR-CAL-06, D-168). 하단 탭까지 덮어야 해서 캘린더 화면 안이 아니라 하단 탭과 캘린더가 함께 있는
 * 곳(`feature:main`의 `MainScreen`)에 둔다. 구멍 대상의 위치는 캘린더 화면과 하단 탭이 `coachMarkTarget`으로 [LocalCoachMarkTargets]에 알린다.
 *
 * @param onCalendar 캘린더 탭이 보이는 중이다 (로그인 뒤 SCR-CAL-01)
 * @param blocked 연결 완료 시트(SCR-COUPLE-04) 같은 다른 시트·다이얼로그가 떠 있다. 닫힌 뒤에 뜬다 (FR-CAL-06 AC1)
 */
@Composable
fun CalendarTutorialHost(
    onCalendar: Boolean,
    blocked: Boolean,
    modifier: Modifier = Modifier,
    viewModel: CalendarTutorialViewModel = hiltViewModel(),
) {
    val targets = LocalCoachMarkTargets.current ?: return
    val state by viewModel.uiState.collectAsState()
    // 5단계 대상의 위치를 모두 쟀을 때만 띄운다 (앱바 링크는 내 정보가 와야 생긴다)
    val ready = CalendarTutorialStep.entries.all { it in targets.keys }

    LaunchedEffect(viewModel, onCalendar, ready, blocked) {
        viewModel.onAction(CalendarTutorialAction.EnvironmentChanged(onCalendar, ready, blocked))
    }

    val step = state.step
    if (step != null && state.visible) {
        CalendarTutorialOverlay(
            step = step,
            solo = state.solo,
            target = targets.rectOf(step),
            onAction = viewModel::onAction,
            modifier = modifier,
        )
    }
}

/** 튜토리얼 한 단계. 상태만 받는다. 단계가 바뀌어도 같은 레이어에 값만 바꿔 넘겨 구멍과 말풍선이 옮겨 간다 */
@Composable
internal fun CalendarTutorialOverlay(
    step: CalendarTutorialStep,
    solo: Boolean,
    target: Rect?,
    onAction: (CalendarTutorialAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    DuriseoCoachMarkLayer(
        target = target,
        holeShape = step.holeShape(),
        message = stringResource(step.messageRes(solo)),
        stepNumber = step.number,
        stepCount = CalendarTutorialStep.count,
        onNext = { onAction(CalendarTutorialAction.NextClicked) },
        onSkip = { onAction(CalendarTutorialAction.SkipClicked) },
        modifier = modifier,
    )
}

/** 구멍 모서리는 대상 모양을 따른다: 달력 `radius-lg`, FAB `radius-fab`, 아이콘 버튼·하단 탭 항목·링크 `radius-full` (SCREENS.md "모양") */
@Composable
internal fun CalendarTutorialStep.holeShape(): CornerBasedShape = when (this) {
    CalendarTutorialStep.Calendar -> MaterialTheme.shapes.large
    CalendarTutorialStep.Fab -> DuriseoShapes.Fab
    CalendarTutorialStep.Anniversary,
    CalendarTutorialStep.MemoTab,
    CalendarTutorialStep.MenuButton,
    -> DuriseoShapes.Full
}

// --- Preview. 390x844 화면의 견본 위치(dp)로 그린다. 실제 위치는 실행 중에 재는 값이다 ---

private fun dpRect(density: Density, left: Int, top: Int, right: Int, bottom: Int): Rect =
    with(density) { Rect(left.dp.toPx(), top.dp.toPx(), right.dp.toPx(), bottom.dp.toPx()) }

@Composable
private fun TutorialPreview(step: CalendarTutorialStep, solo: Boolean = false, rect: (Density) -> Rect) {
    DuriseoTheme {
        val density = LocalDensity.current
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            CalendarScreen(state = CalendarUiState(), padding = PaddingValues(), onOpenMenu = {}, onAction = {})
            CalendarTutorialOverlay(step = step, solo = solo, target = rect(density), onAction = {})
        }
    }
}

/** 1단계: 요일 행 + 월간 달력 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarTutorialPreview_Step1Calendar() =
    TutorialPreview(CalendarTutorialStep.Calendar) { dpRect(it, 0, 128, 390, 396) }

/** 2단계: FAB */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarTutorialPreview_Step2Fab() =
    TutorialPreview(CalendarTutorialStep.Fab) { dpRect(it, 314, 668, 370, 724) }

/** 3단계: 커플 "함께한 지 D+N ›" */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarTutorialPreview_Step3Couple() =
    TutorialPreview(CalendarTutorialStep.Anniversary) { dpRect(it, 88, 34, 200, 52) }

/** 3단계: 개인 공간 "기념일 ›" (문구가 다르다) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarTutorialPreview_Step3Solo() =
    TutorialPreview(CalendarTutorialStep.Anniversary, solo = true) { dpRect(it, 56, 34, 112, 52) }

/** 4단계: 하단 탭의 메모 (말풍선이 구멍 위에 뜬다) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarTutorialPreview_Step4MemoTab() =
    TutorialPreview(CalendarTutorialStep.MemoTab) { dpRect(it, 195, 764, 390, 844) }

/** 5단계: 전체 메뉴 버튼 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarTutorialPreview_Step5Menu() =
    TutorialPreview(CalendarTutorialStep.MenuButton) { dpRect(it, 334, 8, 382, 56) }
