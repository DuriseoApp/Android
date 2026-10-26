package com.duriseo.android.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 값의 기준은 tokens.json `space.*`, `size.*` (px = dp).

/** 4dp 그리드. `space-7`은 토큰에 없다 (만들지 않는다). */
object DuriseoSpacing {
    val Space1: Dp = 4.dp
    val Space2: Dp = 8.dp
    val Space3: Dp = 12.dp
    val Space4: Dp = 16.dp

    /** 탭 화면 좌우 여백 */
    val Space5: Dp = 20.dp

    /** 온보딩 좌우 여백 */
    val Space6: Dp = 24.dp
    val Space8: Dp = 32.dp
}

object DuriseoSize {
    /** 터치 영역 최소 크기 */
    val Touch: Dp = 44.dp
    val Icon: Dp = 24.dp
    val ButtonSm: Dp = 44.dp
    val ButtonMd: Dp = 48.dp
    val ButtonLg: Dp = 56.dp
    val Input: Dp = 52.dp
    val Appbar: Dp = 64.dp
    val BottomNav: Dp = 80.dp
    val Fab: Dp = 56.dp
    val AvatarSm: Dp = 24.dp
    val AvatarMd: Dp = 32.dp
    val AvatarLg: Dp = 44.dp
}
