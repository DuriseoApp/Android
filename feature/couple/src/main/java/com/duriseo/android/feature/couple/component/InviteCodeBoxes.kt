package com.duriseo.android.feature.couple.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duriseo.android.core.designsystem.theme.Brand500
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTextStyles
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line300
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.core.designsystem.theme.Surface100

// 초대코드 칸. 수치는 보드 `InviteCreate.dc.html`(보여 주기 42x56)와 `InviteEnter.dc.html`(입력 높이 58, 폭 나눔)에서 가져왔다.

/** 코드 글자 수 (6자리) */
const val INVITE_CODE_LENGTH = 6

/** SCR-COUPLE-02 코드 칸: 42x56, 모서리 `radius-sm` 12, 흰 면, 칸 사이 8 (InviteCreate.dc.html 38행) */
private val DisplayBoxWidth = 42.dp
private val DisplayBoxHeight = 56.dp

/** SCR-COUPLE-03 코드 칸: 높이 58, 같은 폭으로 나눔, 모서리 12, 사이 8 (InviteEnter.dc.html 21·73행) */
private val InputBoxHeight = 58.dp

/** 칸 테두리 1, 지금 칸은 2 (InviteEnter.dc.html 72행) */
private val BoxBorderWidth = 1.dp
private val ActiveBorderWidth = 2.dp

/**
 * SCR-COUPLE-02: 받은 코드를 한 칸에 한 글자씩 보여 준다 (읽기 전용). [code]가 null이면 빈 칸 6개.
 * 스크린리더는 칸이 아니라 [description] 하나로 읽는다 (`InviteCreate.dc.html` 38행 `aria-label`).
 */
@Composable
fun InviteCodeDisplay(code: String?, description: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
    ) {
        repeat(INVITE_CODE_LENGTH) { index ->
            Box(
                modifier = Modifier
                    .size(width = DisplayBoxWidth, height = DisplayBoxHeight)
                    .background(Surface0, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = code?.getOrNull(index)?.toString().orEmpty(),
                    style = DuriseoTextStyles.CodeLg,
                    color = Ink900,
                )
            }
        }
    }
}

/**
 * SCR-COUPLE-03: 입력한 글자를 칸에 보여 준다. 글자가 있는 칸은 흰 면 + `line-300` 1, 빈 칸은 `surface-100` 면,
 * 다음에 쓸 칸(6자리가 차기 전)은 `brand-500` 2 (InviteEnter.dc.html 70~73행).
 */
@Composable
fun InviteCodeInputBoxes(code: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
    ) {
        repeat(INVITE_CODE_LENGTH) { index ->
            val char = code.getOrNull(index)
            val active = code.length < INVITE_CODE_LENGTH && index == code.length
            val shape = MaterialTheme.shapes.small
            val boxModifier = Modifier
                .weight(1f)
                .height(InputBoxHeight)
                .background(if (char != null) Surface0 else Surface100, shape)
                .let { base ->
                    when {
                        active -> base.border(ActiveBorderWidth, Brand500, shape)
                        char != null -> base.border(BoxBorderWidth, Line300, shape)
                        else -> base.border(BoxBorderWidth, Surface100, shape)
                    }
                }
            Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
                Text(
                    text = char?.toString().orEmpty(),
                    style = DuriseoTextStyles.CodeMd,
                    color = Ink900,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun InviteCodeDisplayPreview() {
    DuriseoTheme {
        Box(Modifier.background(Surface100)) { InviteCodeDisplay(code = "K7Q29X", description = "초대코드 K7Q29X") }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun InviteCodeInputBoxesPreview() {
    DuriseoTheme {
        InviteCodeInputBoxes(code = "K7Q")
    }
}
