package com.duriseo.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.duriseo.android.core.designsystem.theme.Brand500
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.OnBrand

/**
 * 화면 아래 큰 버튼: 높이 `size-button-lg` 56, 모서리 `radius-lg` 16, 글자 16/700 (보드 `InviteCreate.dc.html` 118행,
 * `InviteEnter.dc.html` 55·58행, `StartDateSheet.dc.html` 40행). 비활성은 `line-100` 면과 `ink-500` 글자 (58행).
 * 보통 버튼([DuriseoButton], 높이 48·54, 모서리 14)과 다른 규격이라 따로 둔다.
 *
 * @param container 활성일 때 면. 연결 확인하기는 `ink-900`, 연결하고 시작하기·저장은 `brand-500`
 */
@Composable
fun DuriseoLargeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = Brand500,
    contentColor: Color = OnBrand,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(DuriseoSize.ButtonLg)
            .clip(MaterialTheme.shapes.large)
            .background(if (enabled) container else Line100)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = if (enabled) contentColor else Ink500,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Preview 글자는 버튼 모양을 보여 주는 개발용 견본이라 앱 문구(strings.xml)가 아니다.

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DuriseoLargeButtonPreview() {
    DuriseoTheme {
        Column(
            modifier = Modifier.padding(DuriseoSpacing.Space6),
            verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3),
        ) {
            DuriseoLargeButton("Brand", {}, container = Brand500)
            DuriseoLargeButton("Ink", {}, container = Ink900)
            DuriseoLargeButton("Disabled", {}, enabled = false)
        }
    }
}
