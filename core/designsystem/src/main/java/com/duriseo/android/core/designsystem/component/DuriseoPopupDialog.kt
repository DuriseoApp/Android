package com.duriseo.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.duriseo.android.core.designsystem.theme.DuriseoShadows
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.duriseoShadow

// 공지·업데이트 팝업(SCR-APP-01·02) 틀 (design-system.md "공지·업데이트 팝업", 보드 `AppNotice.dc.html`·`AppUpdate.dc.html`).
// 다이얼로그 표를 따르되 뒤 막을 눌러도 닫히지 않고, 내용 영역만 스크롤되고, 항상 맨 위 창이다.

/** 패널 폭은 100%에 최대 320 (다이얼로그와 같다) */
private val PopupMaxWidth = 320.dp

/** 넘칠 때 내용 영역 아래 선 두께 1 */
private val PopupDividerWidth = 1.dp

/** 내용 영역이 넘칠 때 안쪽 아래 12 (`space-3`) */
private val PopupContentBottomPadding = DuriseoSpacing.Space3

/**
 * 앱 전체 공지·업데이트 팝업 틀. 별도 창에 그리고 **항상 맨 위**다: 뜬 뒤에 시트·다이얼로그·요청 중 덮개가 열려도 그 위에 다시 올라온다.
 *
 * - 뒤 막: 토큰 `scrim`, 누르면 아무 일도 없다(닫히지 않는다). 접근성에서도 뺀다.
 * - 뒤로 가기: [onBack]이 있으면 그것을 부르고, null이면 아무것도 하지 않고 막는다 (닫을 수 없는 팝업, D-179. 닫을 수 있는 팝업의 뒤로 가기는 Q-21).
 * - 패널: 폭 최대 320, 화면 바깥 여백 `space-6`(높이도 그만큼 뺀다), 면 `surface-0`, 모서리 `radius-2xl`, `shadow-dialog`,
 *   안쪽 위 `space-6`·좌우 `space-5`·아래 `space-5`.
 * - [content]는 한 영역에 세로로 두고 사이 `space-2`. 넘치면 이 영역만 스크롤되고, 영역 아래에 `line-100` 선 1과 안쪽 아래 `space-3`이 생긴다.
 * - [actions]는 내용 아래 `space-4`, 버튼끼리 `space-2`. 늘 보인다.
 * - 접근성: 패널은 [paneTitle]로 읽히고(제목·본문, 이미지만 넣은 팝업은 `imageAlt`) 열리면 초점이 패널로 온다.
 */
@Composable
fun DuriseoPopupDialog(
    paneTitle: String,
    onBack: (() -> Unit)?,
    actions: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.extraLarge
    val focusRequester = remember { FocusRequester() }
    // 창이 다시 만들어져도(다른 덮개가 열릴 때) 스크롤 위치가 남도록 창 밖에 둔다
    val scrollState = rememberScrollState()
    val overflowing = scrollState.canScrollForward || scrollState.canScrollBackward
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    DuriseoOverlayWindow(onBack = onBack, showSnackbar = false, alwaysOnTop = true) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // 뒤 막: 눌러도 닫히지 않고, 뒤 화면으로 누름이 새지도 않는다
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .clearAndSetSemantics { },
            )
            Column(
                modifier = Modifier
                    .padding(DuriseoSpacing.Space6)
                    .widthIn(max = PopupMaxWidth)
                    .fillMaxWidth()
                    .duriseoShadow(shape, DuriseoShadows.Dialog)
                    .background(MaterialTheme.colorScheme.surface, shape)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .semantics {
                        this.paneTitle = paneTitle
                        isTraversalGroup = true
                    }
                    .focusRequester(focusRequester)
                    .focusable()
                    .padding(
                        top = DuriseoSpacing.Space6,
                        start = DuriseoSpacing.Space5,
                        end = DuriseoSpacing.Space5,
                        bottom = DuriseoSpacing.Space5,
                    ),
            ) {
                // 내용 영역: 남는 높이만큼만 차지하고(버튼은 늘 보인다) 넘치면 스크롤
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
                    ) {
                        content()
                        if (overflowing) Spacer(Modifier.height(PopupContentBottomPadding - DuriseoSpacing.Space2))
                    }
                    if (overflowing) HorizontalDivider(thickness = PopupDividerWidth, color = Line100)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = DuriseoSpacing.Space4),
                    verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
                ) {
                    actions()
                }
            }
        }
    }
}
