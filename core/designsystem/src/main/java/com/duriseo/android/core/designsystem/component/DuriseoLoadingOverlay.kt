package com.duriseo.android.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duriseo.android.core.designsystem.theme.Brand500
import com.duriseo.android.core.designsystem.theme.DuriseoShadows
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.duriseoShadow

// 로딩 덮개 값 (design-system.md "로딩 표시 (요청 중, D-115)", 보드 `AddEventLoading.dc.html`).

/** 판 크기 84 = 원형 표시 44 + 좌우 `space-5`(20) */
private val PlateSize = DuriseoSize.Touch + DuriseoSpacing.Space5 * 2

/** 원형 표시 선 두께 (문서 값 4) */
private val LoadingStroke = 4.dp

/**
 * 쓰기·계정 요청 중에 화면 전체를 덮는 로딩 덮개 (D-115, Q-18). 요청이 시작되면 바로 보이고(지연 없음),
 * 요청이 끝나면(성공·실패 모두) 걷는다. [DuriseoLoadingLayer] 안에서 쓴다.
 *
 * 덮개 `scrim`, 가운데 판 84(모서리 `radius-lg`, 면 `surface-0`, `shadow-dialog`), 안에 `brand-500` 원형 44 / 선 4. 문구는 없다.
 * 덮는 동안 아래 화면은 눌리지 않고(누름·끌기를 여기서 받음) 뒤로 가기도 막는다.
 */
@Composable
fun DuriseoLoadingOverlay(modifier: Modifier = Modifier) {
    // 요청이 도는 동안 뒤로 가기로 화면이 닫히지 않게 한다
    BackHandler(enabled = true, onBack = {})
    val shape = MaterialTheme.shapes.large
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim)
            // 모든 누름을 받아 아래 화면으로 보내지 않는다
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(PlateSize)
                .duriseoShadow(shape, DuriseoShadows.Dialog)
                .background(MaterialTheme.colorScheme.surface, shape),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(DuriseoSize.Touch)
                    .clearAndSetSemantics { },
                color = Brand500,
                strokeWidth = LoadingStroke,
                trackColor = Line100,
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

/**
 * 화면 내용과 로딩 덮개를 함께 그리는 틀. [loading]이 true인 동안 뒤 화면([content])을 접근성 트리에서 빼서
 * TalkBack 초점이 뒤 화면으로 가지 않게 한다 ([DuriseoDialogLayer]와 같은 방식).
 *
 * ```
 * DuriseoLoadingLayer(loading = state.loading) { 화면 내용 }
 * ```
 */
@Composable
fun DuriseoLoadingLayer(
    loading: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (loading) Modifier.clearAndSetSemantics { } else Modifier),
        ) {
            content()
        }
        if (loading) DuriseoLoadingOverlay()
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoadingOverlayPreview() {
    DuriseoTheme {
        // 뒤 화면 견본과 함께 그린다 (견본 글자라 앱 문구가 아니다)
        DuriseoLoadingLayer(loading = true) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                Text("Behind", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
