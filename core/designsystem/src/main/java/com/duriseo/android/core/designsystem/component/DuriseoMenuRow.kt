package com.duriseo.android.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.core.designsystem.theme.Brand700
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink400
import com.duriseo.android.core.designsystem.theme.Ink500

// 메뉴 행 값 (design-system.md "메뉴 행", "섹션 제목", 보드 `Menu.dc.html` 99~134행).
// 토큰에 없는 수치라 이름을 붙인 상수로 둔다.

/** 주요 행 높이 56 (우리 색상, 알림 설정, 연결 정보, 계정) */
private val MenuRowHeight = 56.dp

/** 앞 아이콘 22 (`size-icon` 24가 아님) */
private val MenuIconSize = 22.dp

/** 앞 아이콘과 글자 사이 14 */
private val MenuIconGap = 14.dp

/** 오른쪽 화살표 18 */
private val ChevronSize = 18.dp

/** 섹션 제목 아래로 더 띄우는 2 */
private val SectionTitleBottom = 2.dp

/**
 * 전체 메뉴의 주요 행. 높이 56, 앞 아이콘 22 + 글자 `body-lg` 16/500 `ink-900`(사이 14),
 * 오른쪽에 값(13, 사이 8)과 화살표 18(`ink-400`). 행 전체가 눌려요.
 *
 * @param value 오른쪽 값. 평소 `ink-500` 400, [highlightValue]면 `brand-700` 600(게스트의 "게스트 · 로그인하기")
 * @param trailing 글자 값 대신 오른쪽에 둘 것(우리 색상의 색 점 3개). 있으면 [value]는 쓰지 않는다
 */
@Composable
fun DuriseoMenuRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    value: String? = null,
    highlightValue: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MenuRowHeight)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MenuIconGap),
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(MenuIconSize),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
        ) {
            if (trailing != null) {
                trailing()
            } else if (value != null) {
                Text(
                    text = value,
                    style = if (highlightValue) {
                        MaterialTheme.typography.labelLarge
                    } else {
                        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal)
                    },
                    color = if (highlightValue) Brand700 else Ink500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(ChevronSize),
                tint = Ink400,
            )
        }
    }
}

/**
 * 섹션 제목: 13 / 600(`label`), 아래로 2 더 띄운다. 글자색은 화면마다 보드가 달라 [color]로 받는다
 * (전체 메뉴 `ink-500`, 계정 화면 `ink-600`).
 */
@Composable
fun DuriseoSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Ink500,
) {
    Text(
        text = text,
        modifier = modifier
            .padding(bottom = SectionTitleBottom)
            .semantics { heading() },
        style = MaterialTheme.typography.labelLarge,
        color = color,
    )
}

// Preview 글자는 모양 견본이라 앱 문구가 아니다.

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DuriseoMenuRowPreview() {
    DuriseoTheme {
        Column(Modifier.padding(horizontal = DuriseoSpacing.Space5)) {
            DuriseoSectionTitle(text = "Section 13/600")
            DuriseoMenuRow(label = "Row 56", onClick = {}, iconRes = R.drawable.ic_user, value = "Value 13/400")
            DuriseoMenuRow(label = "Row 56", onClick = {}, iconRes = R.drawable.ic_user, value = "Highlight 13/600", highlightValue = true)
            DuriseoMenuRow(label = "Row 56 no value", onClick = {}, iconRes = R.drawable.ic_user)
        }
    }
}
