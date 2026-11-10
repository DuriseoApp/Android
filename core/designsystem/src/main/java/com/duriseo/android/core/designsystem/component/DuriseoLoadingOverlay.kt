package com.duriseo.android.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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
 * 로딩 덮개를 화면 전체(하단 탭까지) 위층에서 그리는 호스트의 상태 (D-115 "화면 전체").
 * 하단 탭은 `MainScaffold` 소속이라 탭 루트 화면 안에서 그린 덮개는 탭을 덮지 못한다. 그래서 앱의 맨 위층이
 * [DuriseoLoadingHost]로 호스트를 두면, 그 아래에서 [DuriseoLoadingLayer]·[DuriseoDialogLayer]가 요청 중임을
 * 호스트에 알리고(덮개를 직접 그리지 않고), 호스트가 가장 위에 하나만 그린다. 여러 화면이 동시에 알려도 덮개는 하나다.
 */
@Stable
class DuriseoLoadingHostState {
    private var count by mutableIntStateOf(0)

    /** 요청 중인 화면이 하나라도 있으면 true */
    val active: Boolean get() = count > 0

    fun acquire() {
        count++
    }

    fun release() {
        if (count > 0) count--
    }
}

/** 위층 호스트. 없으면(null) 화면이 덮개를 직접 그린다 (Preview, 호스트 밖 화면) */
val LocalLoadingHost = staticCompositionLocalOf<DuriseoLoadingHostState?> { null }

/**
 * 앱 맨 위층에 두는 호스트: [content] 위에 [state]가 켜져 있는 동안 [DuriseoLoadingOverlay]를 그린다.
 * `MainScaffold`(하단 탭 포함)와 그 위 시트까지 모두 [content] 안에 둔다.
 */
@Composable
fun DuriseoLoadingHost(
    state: DuriseoLoadingHostState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalLoadingHost provides state) {
        Box(modifier = modifier.fillMaxSize()) {
            content()
            if (state.active) DuriseoLoadingOverlay()
        }
    }
}

/**
 * 화면 쪽 덮개 자리. 위층 호스트가 있으면 [loading] 동안 호스트에 알리기만 하고, 없으면 직접 [DuriseoLoadingOverlay]를 그린다.
 */
@Composable
internal fun LoadingOverlaySlot(loading: Boolean) {
    val host = LocalLoadingHost.current
    if (host == null) {
        if (loading) DuriseoLoadingOverlay()
        return
    }
    DisposableEffect(host, loading) {
        if (loading) host.acquire()
        onDispose { if (loading) host.release() }
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
        LoadingOverlaySlot(loading)
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

/** 탭 루트 화면: 호스트가 하단 탭까지 덮는다 (D-115 "화면 전체"). 아래 막대는 `MainScaffold`의 하단 탭 견본 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoadingOverlayPreview_HostCoversBottomBar() {
    DuriseoTheme {
        val host = remember { DuriseoLoadingHostState() }
        DuriseoLoadingHost(state = host) {
            Column(Modifier.fillMaxSize()) {
                DuriseoLoadingLayer(loading = true, modifier = Modifier.weight(1f)) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                        Text("Content", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Box(Modifier.fillMaxWidth().height(DuriseoSize.BottomNav).background(Line100), contentAlignment = Alignment.Center) {
                    Text("Bottom bar", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
