package com.duriseo.android.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.core.designsystem.theme.Danger700
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.OnBrand

// 버튼 보드 값 (design-system.md "버튼", 보드 `Main.dc.html` 38~46행, `Account.dc.html` 39·40·82행).
// 토큰에 없는 수치라 이름을 붙인 상수로 둔다.

/** 소셜 로그인 버튼 높이 54. `size-button-lg`(56)와 다른 보드 값이다. 시작 화면과 계정 화면(게스트)만 쓴다 */
private val SocialButtonHeight = 54.dp

/** 아웃라인 테두리 두께 1 */
private val OutlineWidth = 1.dp

/** 버튼 앞 아이콘 20 (`size-icon` 24가 아니다, `Main.dc.html` 44행) */
private val LeadingIconSize = 20.dp

/** 버튼 모양 종류 */
enum class DuriseoButtonStyle {
    /** `ink-900` 채움 + `on-brand` 글자: 소셜 로그인 첫째 버튼, 전체 메뉴 [로그인해서 기록 지키기] */
    Filled,

    /** `surface-0` 면 + `line-200` 1 테두리 + `ink-900` 글자: 소셜 로그인 둘째 버튼, 다이얼로그 취소 */
    Outline,

    /** `surface-100` 면 + `ink-900` 글자: 게스트로 시작하기 */
    Tonal,

    /** `danger-700` 채움 + `on-brand` 글자: 파괴적 확인(로그아웃, 탈퇴하기, 삭제하고 로그인) */
    Destructive,

    /** `brand-500` 채움 + `on-brand` 글자: 파괴적이지 않은 다이얼로그 확인(그 계정으로 로그인) */
    Brand,
}

/** 버튼 크기. 높이는 design-system.md "버튼"의 두 값이다. */
enum class DuriseoButtonSize(val height: Dp) {
    /** 높이 54, 글자 16 / 600 */
    Social(SocialButtonHeight),

    /** 높이 48(`size-button-md`), 글자 15 / 600 */
    Medium(DuriseoSize.ButtonMd),
}

@Immutable
private data class ButtonColors(
    val container: Color,
    val content: Color,
    val border: Color?,
)

@Composable
private fun DuriseoButtonStyle.colors(enabled: Boolean): ButtonColors {
    // 비활성: 면은 `line-100`("비활성 버튼·입력 채움", tokens.json). 글자색은 문서에 없어 `ink-600`을 쓴다
    // (ink-500은 surface-0 밖에서 대비가 모자라다, tokens.json `ink-500` usage).
    if (!enabled) return ButtonColors(container = Line100, content = Ink600, border = null)
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        DuriseoButtonStyle.Filled -> ButtonColors(scheme.secondary, scheme.onSecondary, null)
        DuriseoButtonStyle.Outline -> ButtonColors(scheme.surface, scheme.onSurface, scheme.outlineVariant)
        DuriseoButtonStyle.Tonal -> ButtonColors(scheme.surfaceVariant, scheme.onSurface, null)
        DuriseoButtonStyle.Destructive -> ButtonColors(Danger700, OnBrand, null)
        DuriseoButtonStyle.Brand -> ButtonColors(scheme.primary, scheme.onPrimary, null)
    }
}

@Composable
private fun DuriseoButtonSize.textStyle(): TextStyle {
    val base = when (this) {
        DuriseoButtonSize.Social -> MaterialTheme.typography.bodyLarge
        DuriseoButtonSize.Medium -> MaterialTheme.typography.bodyMedium
    }
    return base.copy(fontWeight = FontWeight.SemiBold)
}

/**
 * 글자 버튼 (design-system.md "버튼"). 모서리 `radius-md`(14), 가운데 정렬 한 줄 글자.
 * 폭은 [modifier]로 정한다 (가로 전체는 `Modifier.fillMaxWidth()`, 다이얼로그는 `weight(1f)`).
 *
 * @param enabled false면 `line-100` 면으로 보이고 눌리지 않는다. 요청 중 시작 버튼을 비활성으로 둘 때 쓴다 (D-88)
 * @param leadingIconRes 글자 앞 아이콘(20). 아이콘과 글자 사이 `space-2`
 */
@Composable
fun DuriseoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DuriseoButtonStyle = DuriseoButtonStyle.Filled,
    size: DuriseoButtonSize = DuriseoButtonSize.Medium,
    enabled: Boolean = true,
    @DrawableRes leadingIconRes: Int? = null,
) {
    val shape = MaterialTheme.shapes.medium
    val colors = style.colors(enabled)
    val border = colors.border
    Row(
        modifier = modifier
            .height(size.height)
            .clip(shape)
            .background(colors.container)
            .then(if (border != null) Modifier.border(OutlineWidth, border, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = DuriseoSpacing.Space4),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIconRes != null) {
            Icon(
                painter = painterResource(leadingIconRes),
                contentDescription = null,
                modifier = Modifier.size(LeadingIconSize),
                tint = colors.content,
            )
            Spacer(Modifier.width(DuriseoSpacing.Space2))
        }
        Text(
            text = text,
            style = size.textStyle(),
            color = colors.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Preview 글자는 버튼 모양을 보여 주는 개발용 견본이라 앱 문구(strings.xml)가 아니다.

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DuriseoButtonStylesPreview() {
    DuriseoTheme {
        Column(
            modifier = Modifier.padding(DuriseoSpacing.Space4),
            verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3),
        ) {
            DuriseoButton("Filled Social 54", {}, Modifier.fillMaxWidth(), DuriseoButtonStyle.Filled, DuriseoButtonSize.Social)
            DuriseoButton("Outline Social 54", {}, Modifier.fillMaxWidth(), DuriseoButtonStyle.Outline, DuriseoButtonSize.Social)
            DuriseoButton("Tonal Medium 48", {}, Modifier.fillMaxWidth(), DuriseoButtonStyle.Tonal, leadingIconRes = R.drawable.ic_user)
            DuriseoButton("Destructive Medium 48", {}, Modifier.fillMaxWidth(), DuriseoButtonStyle.Destructive)
            DuriseoButton("Brand Medium 48", {}, Modifier.fillMaxWidth(), DuriseoButtonStyle.Brand)
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DuriseoButtonDisabledPreview() {
    DuriseoTheme {
        Column(
            modifier = Modifier.padding(DuriseoSpacing.Space4),
            verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3),
        ) {
            DuriseoButton("Filled disabled", {}, Modifier.fillMaxWidth(), DuriseoButtonStyle.Filled, DuriseoButtonSize.Social, enabled = false)
            DuriseoButton("Outline disabled", {}, Modifier.fillMaxWidth(), DuriseoButtonStyle.Outline, DuriseoButtonSize.Social, enabled = false)
            DuriseoButton("Tonal disabled", {}, Modifier.fillMaxWidth(), DuriseoButtonStyle.Tonal, enabled = false, leadingIconRes = R.drawable.ic_user)
        }
    }
}
