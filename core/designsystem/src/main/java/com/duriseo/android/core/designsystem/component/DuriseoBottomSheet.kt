package com.duriseo.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line300
import com.duriseo.android.core.designsystem.theme.Surface0

// 하단 시트의 면과 뒤 막 (design-system.md "바텀시트", 보드 `Calendar.dc.html` 128~131행, `StartDateSheet.dc.html` 19·20행).
// 수치는 토큰에 없어 보드 값을 이름 붙인 상수로 둔다.

/** 시트 위 모서리 24 (`radius-2xl`) */
private val SheetTopRadius = DuriseoSpacing.Space6

/** 시트 안쪽: 위 10·좌우 24·아래 28 */
private val SheetPaddingTop = 10.dp
private val SheetPaddingBottom = 28.dp

/** 손잡이 36x4, 모서리 2, `line-300` */
private val HandleWidth = 36.dp
private val HandleHeight = 4.dp
private val HandleRadius = 2.dp

/**
 * 하단 시트의 면: 위 모서리 24, 흰 면, 위 가운데 손잡이, 안쪽 위 10·좌우 24·아래 28.
 * 뒤 막과 위치는 [DuriseoBottomSheetLayer]가 맡는다.
 *
 * @param gap 덩어리 사이 간격 (처음 만난 날 시트 16, 연결 완료 시트 22)
 */
@Composable
fun DuriseoSheetPanel(
    modifier: Modifier = Modifier,
    gap: Dp = DuriseoSpacing.Space4,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface0, RoundedCornerShape(topStart = SheetTopRadius, topEnd = SheetTopRadius))
            .padding(
                start = DuriseoSpacing.Space6,
                end = DuriseoSpacing.Space6,
                top = SheetPaddingTop,
                bottom = SheetPaddingBottom,
            ),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = HandleWidth, height = HandleHeight)
                .background(Line300, RoundedCornerShape(HandleRadius))
                .clearAndSetSemantics { },
        )
        content()
    }
}

/**
 * 화면 전체를 덮는 뒤 막(토큰 `scrim`)과 그 위 아래쪽의 시트 [content]. [DuriseoDialog]처럼 시스템 창을 열지 않아
 * Preview에 그려진다. 시트가 열린 동안 뒤 화면의 접근성 처리는 부르는 쪽이 한다 ([DuriseoDialogLayer]).
 *
 * @param onDismissRequest 뒤 막을 눌렀을 때. null이면 뒤 막을 눌러도 닫히지 않는다 (연결 완료 시트에서 기록을 고르기 전).
 *   기기 뒤로 가기는 부르는 쪽이 `BackHandler`로 처리한다.
 */
@Composable
fun DuriseoBottomSheetLayer(
    onDismissRequest: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scrimLabel = stringResource(R.string.nav_close)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim)
                .then(
                    if (onDismissRequest != null) {
                        Modifier
                            .semantics {
                                contentDescription = scrimLabel
                                role = Role.Button
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismissRequest,
                            )
                    } else {
                        // 눌러도 뒤 화면으로 새지 않게 누름은 받되 아무것도 하지 않는다
                        Modifier
                            .clearAndSetSemantics { }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                            )
                    },
                ),
        )
        content()
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 600)
@Composable
private fun DuriseoSheetPanelPreview() {
    DuriseoTheme {
        DuriseoBottomSheetLayer(onDismissRequest = {}) {
            DuriseoSheetPanel {
                Text(text = "시트 제목", style = MaterialTheme.typography.titleLarge, color = Ink900)
            }
        }
    }
}
