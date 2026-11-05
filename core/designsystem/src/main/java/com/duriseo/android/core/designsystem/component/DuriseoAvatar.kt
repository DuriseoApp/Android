package com.duriseo.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duriseo.android.core.designsystem.theme.DuriseoPickPalette
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.PersonPartner
import com.duriseo.android.core.designsystem.theme.onPersonColor

// 사람 색 원 아바타 (design-system.md "사람 호칭", 보드 `Menu.dc.html` meAvatarStyle). 나 "나", 상대 "너", 함께 "둘".
// 프로필 사진은 단위 8 (프로필 수정). 지금은 글자 원만 그린다.

/** 글자 크기는 보드 값 그대로: 72는 24, 56은 20, 굵기 700 (`Menu.dc.html` 199행, `ConnectionInfo.dc.html` 연결 정보) */
private val AvatarLargeSize = 72.dp
private val AvatarMediumSize = 56.dp
private val AvatarSmallSize = 32.dp

/** 아바타 크기 (보드 값: 전체 메뉴 프로필 카드 72, 연결 정보 56) */
enum class DuriseoAvatarSize(val diameter: Dp, val fontSize: Float) {
    Large(AvatarLargeSize, 24f),
    Medium(AvatarMediumSize, 20f),

    /** 캘린더 앱바: 지름 32, 글자 13/700 (`Calendar.dc.html` 70행 `avatar`) */
    Small(AvatarSmallSize, 13f),
}

/**
 * 사람 색 원 안에 [label] 한 글자. 글자색은 배경 상대휘도로 정한다 (`onPersonColor`).
 * 접근성으로는 읽히지 않는다(이름이 옆에 같이 읽힌다). 모르는 팔레트 키는 [fallback] 색으로 그린다 (API.md 3.2 앱 동작).
 *
 * @param colorKey 팔레트 키 (`pick-*`)
 */
@Composable
fun DuriseoAvatar(
    label: String,
    colorKey: String,
    modifier: Modifier = Modifier,
    size: DuriseoAvatarSize = DuriseoAvatarSize.Large,
    fallback: Color = PersonPartner,
) {
    val color = DuriseoPickPalette.colorOf(colorKey) ?: fallback
    Box(
        modifier = modifier
            .size(size.diameter)
            .background(color, DuriseoShapes.Full)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(fontSize = size.fontSize.sp, fontWeight = FontWeight.Bold),
            color = onPersonColor(color),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DuriseoAvatarPreview() {
    DuriseoTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DuriseoAvatar(label = "나", colorKey = "pick-orange")
            DuriseoAvatar(label = "너", colorKey = "pick-blue")
            DuriseoAvatar(label = "나", colorKey = "pick-amber", size = DuriseoAvatarSize.Medium)
            DuriseoAvatar(label = "너", colorKey = "unknown-key", size = DuriseoAvatarSize.Medium)
        }
    }
}
