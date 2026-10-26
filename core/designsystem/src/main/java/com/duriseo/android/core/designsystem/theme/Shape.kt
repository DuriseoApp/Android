package com.duriseo.android.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// 값의 기준은 tokens.json `radius.*`, 대응은 design-system.md "Android 매핑"의 Shapes.

/**
 * M3 `Shapes`에 대응하는 4개: small=radius-sm(12), medium=radius-md(14), large=radius-lg(16),
 * extraLarge=radius-2xl(24). `extraSmall`은 매핑이 없어 지정하지 않는다.
 */
val DuriseoMaterialShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** `Shapes`에 대응이 없는 나머지 모서리 토큰. */
object DuriseoShapes {
    /** radius-xs 6: 배지 */
    val Xs = RoundedCornerShape(6.dp)

    /** radius-fab 18: FAB */
    val Fab = RoundedCornerShape(18.dp)

    /** radius-xl 20: 카드 */
    val Xl = RoundedCornerShape(20.dp)

    /** radius-full 999: 알약과 원 */
    val Full = CircleShape
}
