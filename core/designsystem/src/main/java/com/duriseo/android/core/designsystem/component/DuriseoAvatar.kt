package com.duriseo.android.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.core.designsystem.theme.DuriseoPickPalette
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.PersonPartner
import com.duriseo.android.core.designsystem.theme.onPersonColor

// 사람 색 원 아바타 (design-system.md "사람 호칭", 보드 `Menu.dc.html` meAvatarStyle). 나 "나", 상대 "너", 함께 "둘".
// 프로필 사진(단위 8, FR-AUTH-07 AC3·AC4): 사진이 있으면 원형으로 가운데를 채워(`ContentScale.Crop`) 글자 원 위에 그리고,
// 받는 중·받지 못함·사진 없음(null)이면 글자 원이 그대로 보인다.

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
 * @param photo 사진 모델(Coil이 읽을 수 있는 값, 앱에서는 `ProfileImage`). null이면 요청 없이 글자 원만 그린다.
 *   받는 중이거나 받지 못하면(404·네트워크 오류) 글자 원이 그대로 보인다 (FR-AUTH-07 AC4)
 */
@Composable
fun DuriseoAvatar(
    label: String,
    colorKey: String,
    modifier: Modifier = Modifier,
    size: DuriseoAvatarSize = DuriseoAvatarSize.Large,
    fallback: Color = PersonPartner,
    photo: Any? = null,
) {
    DuriseoAvatarFrame(label, colorKey, modifier, size, fallback) {
        DuriseoAvatarPhoto(photo, Modifier.matchParentSize())
    }
}

/**
 * 글자 원 위에 얹는 프로필 사진 층: 원형으로 자르고 가운데를 채운다(`ContentScale.Crop`, FR-AUTH-07 AC3·AC4).
 * [photo]가 null이면 아무것도 그리지 않고 요청도 하지 않는다. 받는 중·받지 못함도 아무것도 그리지 않아 아래 글자 원이 보인다.
 * 지름이 24인 메모 작성자 아바타처럼 [DuriseoAvatar]의 크기에 없는 아바타가 글자 원을 직접 그릴 때 쓴다
 * (부모 `Box`의 [Modifier.matchParentSize]를 넘긴다).
 */
@Composable
fun DuriseoAvatarPhoto(photo: Any?, modifier: Modifier = Modifier) {
    if (photo == null) return
    AsyncImage(
        model = photo,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(DuriseoShapes.Full),
    )
}

/** 글자 원 + 그 위에 얹는 [photoLayer]. 미리보기가 Coil 없이 사진 모양을 그리려고 나눠 둔 부분이다 */
@Composable
internal fun DuriseoAvatarFrame(
    label: String,
    colorKey: String,
    modifier: Modifier,
    size: DuriseoAvatarSize,
    fallback: Color,
    photoLayer: @Composable BoxScope.() -> Unit,
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
        photoLayer()
    }
}

/** 사진이 있는 모양: 가운데를 채운 원형. 실제 사진은 Coil이 받아 오므로 미리보기는 같은 자리·같은 자르기로 그림 하나를 얹는다 */
@Preview(showBackground = true)
@Composable
private fun DuriseoAvatarPreview_Photo() {
    DuriseoTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DuriseoAvatarSize.entries.forEach { avatarSize ->
                DuriseoAvatarFrame("나", "pick-orange", Modifier, avatarSize, PersonPartner) {
                    Image(
                        painter = painterResource(R.drawable.logo_mark_start),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize().clip(DuriseoShapes.Full),
                    )
                }
            }
        }
    }
}

/** 사진이 없거나(null) 받지 못한 모양: 사람 색 + 글자 (FR-AUTH-07 AC4) */
@Preview(showBackground = true)
@Composable
private fun DuriseoAvatarPreview_Fallback() {
    DuriseoTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DuriseoAvatar(label = "나", colorKey = "pick-orange", photo = null)
            DuriseoAvatar(label = "너", colorKey = "pick-blue", size = DuriseoAvatarSize.Medium, photo = null)
            DuriseoAvatar(label = "나", colorKey = "pick-amber", size = DuriseoAvatarSize.Small, photo = null)
        }
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
