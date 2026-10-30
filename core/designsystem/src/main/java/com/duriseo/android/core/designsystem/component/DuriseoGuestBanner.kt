package com.duriseo.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Warning50
import com.duriseo.android.core.designsystem.theme.Warning800

// 게스트 안내 배너 값 (design-system.md "게스트 안내 배너", 보드 `Menu.dc.html` 26~35행, `Account.dc.html` 29~35행).
// 토큰에 없는 수치라 이름을 붙인 상수로 둔다.

/** 안쪽 여백 18 (보드 값) */
private val BannerPadding = 18.dp

/** 정보 아이콘 22 (`size-icon` 24가 아님) */
private val BannerIconSize = 22.dp

/** 아이콘을 글자 첫 줄에 맞추려고 아래로 1 내린다 */
private val BannerIconTopOffset = 1.dp

/** 본문 13 / 400, 행간 1.6 = 20.8 (보드 값. `label`은 13/600이라 굵기를 400으로 바꿔 쓴다) */
private val BannerBodyLineHeight = 20.8.sp

/** 본문 아래에 버튼이 오면 그 사이 14 (보드 값) */
private val BannerActionGap = 14.dp

/**
 * 게스트 안내 배너 (FR-AUTH-04). 면 `warning-50`, 글자·아이콘 `warning-800`, 모서리 `radius-xl`(20).
 * 문구는 화면마다 다르다 (전체 메뉴와 계정 화면, SCREENS.md SCR-SET-01).
 *
 * @param action 본문 아래에 놓을 버튼 (전체 메뉴의 [로그인해서 기록 지키기]). 계정 화면의 배너에는 없다.
 */
@Composable
fun DuriseoGuestBanner(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Warning50, DuriseoShapes.Xl)
            .padding(BannerPadding),
        verticalArrangement = Arrangement.spacedBy(BannerActionGap),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3)) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = BannerIconTopOffset)
                    .size(BannerIconSize),
                tint = Warning800,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space1),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Warning800,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Normal,
                        lineHeight = BannerBodyLineHeight,
                    ),
                    color = Warning800,
                )
            }
        }
        action?.invoke()
    }
}

// Preview 글자는 모양 견본이라 앱 문구가 아니다 (실제 문구는 각 화면 Preview).

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DuriseoGuestBannerPreview() {
    DuriseoTheme {
        Column(Modifier.padding(DuriseoSpacing.Space5)) {
            DuriseoGuestBanner(title = "Banner title 15/700", body = "Banner body 13/400 line-height 1.6, 두 줄이 되도록 조금 길게 써요.")
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DuriseoGuestBannerWithActionPreview() {
    DuriseoTheme {
        Column(Modifier.padding(DuriseoSpacing.Space5)) {
            DuriseoGuestBanner(
                title = "Banner title 15/700",
                body = "Banner body 13/400",
                action = {
                    DuriseoButton(text = "Action 48", onClick = {}, modifier = Modifier.fillMaxWidth())
                },
            )
        }
    }
}
