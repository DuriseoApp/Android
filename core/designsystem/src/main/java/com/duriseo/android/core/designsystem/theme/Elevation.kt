package com.duriseo.android.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

// 값의 기준은 tokens.json `shadow.*` (CSS `0 <y>px <blur>px rgba(28, 26, 34, <alpha>)`).
// 그림자는 FAB와 다이얼로그에만 쓴다 (design-system.md 원칙 3).
//
// Compose 1.9의 `Modifier.dropShadow`로 그대로 표현한다. composeBom을 2024.09.00(Compose 1.7)으로
// 되돌리면 `dropShadow`가 없으므로 이 파일의 [duriseoShadow]만 `Modifier.shadow(elevation)` 근사로 바꾼다.

/** 그림자 토큰 하나. x 오프셋은 모두 0이다. */
@Immutable
data class DuriseoShadow(
    val offsetY: Dp,
    val blur: Dp,
    val color: Color,
)

object DuriseoShadows {
    /** shadow-sm: `0 1px 3px rgba(28, 26, 34, 0.12)` */
    val Sm = DuriseoShadow(offsetY = 1.dp, blur = 3.dp, color = Ink900.copy(alpha = 0.12f))

    /** shadow-fab: `0 6px 16px rgba(28, 26, 34, 0.18)` */
    val Fab = DuriseoShadow(offsetY = 6.dp, blur = 16.dp, color = Ink900.copy(alpha = 0.18f))

    /** shadow-dialog: `0 16px 40px rgba(28, 26, 34, 0.24)` */
    val Dialog = DuriseoShadow(offsetY = 16.dp, blur = 40.dp, color = Ink900.copy(alpha = 0.24f))
}

/** [shape] 모양의 면 뒤에 그림자 토큰을 그린다. 면(배경)은 이 Modifier 뒤에 그린다. */
fun Modifier.duriseoShadow(shape: Shape, shadow: DuriseoShadow): Modifier = dropShadow(
    shape = shape,
    shadow = Shadow(
        radius = shadow.blur,
        color = shadow.color,
        offset = DpOffset(x = 0.dp, y = shadow.offsetY),
    ),
)
