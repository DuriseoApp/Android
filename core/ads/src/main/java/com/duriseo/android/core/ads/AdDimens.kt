package com.duriseo.android.core.ads

import androidx.compose.ui.unit.dp
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing

/**
 * 광고 카드 크기. 토큰이 있는 값은 토큰을 쓰고, 광고 보드(`AdDemoMemo.dc.html`·`AdDemoAnniversaries.dc.html`)에만 있는
 * 값(토큰에 없음)은 여기 한 곳에 모은다. 이 파일 밖에서 dp를 새로 쓰지 않는다.
 */
internal object AdDimens {
    // 한 줄형 Compact (AdDemoMemo 97~99행)
    val CompactPadding = DuriseoSpacing.Space3
    val CompactGap = DuriseoSpacing.Space3
    val CompactIcon = 48.dp
    val CompactIconRadius = 12.dp
    val CompactButtonHeight = 34.dp
    val CompactButtonPadding = DuriseoSpacing.Space3
    val CompactButtonRadius = 10.dp
    val CompactButtonTop = DuriseoSpacing.Space3
    val CompactCardRadius = 16.dp

    // 큰 카드형 Large (AdDemoAnniversaries 77~81행)
    val LargePadding = 14.dp
    val LargeGap = 10.dp
    val LargeIcon = 32.dp
    val LargeIconRadius = 8.dp
    val LargeMedia = 180.dp
    val LargeMediaRadius = 12.dp
    val LargeButtonHeight = DuriseoSize.ButtonSm
    val LargeButtonRadius = 12.dp
    val LargeCardRadius = 20.dp
    /** AdChoices 자리 (오른쪽 위 16dp + 여백) */
    val LargeHeaderEndPadding = 22.dp

    // 공통
    val Border = 1.dp
    val BadgeRadius = 4.dp
    val BadgePaddingH = 6.dp
    val BadgePaddingV = 1.dp
    val BadgeGap = 6.dp
    val TextGap = 3.dp

    // 배너 미리보기 (AdDemoMenu 177행)
    val BannerPreviewHeight = 64.dp
}
