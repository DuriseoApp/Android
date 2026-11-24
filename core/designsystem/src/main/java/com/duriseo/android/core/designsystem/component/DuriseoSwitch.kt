package com.duriseo.android.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duriseo.android.core.designsystem.theme.Brand500
import com.duriseo.android.core.designsystem.theme.DuriseoShadows
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.Line200
import com.duriseo.android.core.designsystem.theme.Line300
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.core.designsystem.theme.duriseoShadow

// 스위치 (보드 `AddEvent.dc.html` 96행·`NotificationSettings.dc.html` 63~72행, design-system.md "스위치 비활성").
// M3 `Switch`는 꺼짐 손잡이(outline)가 꺼짐 트랙(line-300)에 묻혀서 보드대로 직접 그린다.
// 수치는 토큰에 없어 보드 값을 이름 붙인 상수로 둔다.

/** 트랙 52x32, 모서리 16, 안쪽 3, 손잡이 26 */
private val TrackWidth = 52.dp
private val TrackHeight = 32.dp
private val TrackRadius = 16.dp
private val TrackInset = 3.dp
private val ThumbSize = 26.dp
private val ThumbTravel = TrackWidth - TrackInset * 2 - ThumbSize

/**
 * 보드대로 그린 스위치. 꺼짐은 트랙 `line-300`·손잡이 왼쪽, 켜짐은 트랙 `brand-500`·손잡이 오른쪽, 손잡이는 `surface-0` + 그림자.
 * 비활성은 트랙 `line-100`, 손잡이 테두리 1 `line-200`, 그림자 없음 (D-145).
 *
 * @param onCheckedChange null이면 스위치 자체는 누르지 않는다 (행 전체가 눌릴 때). 이때 접근성 동작도 붙이지 않는다.
 */
@Composable
fun DuriseoSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val thumbX by animateDpAsState(if (checked) ThumbTravel else 0.dp, label = "switchThumb")
    val trackColor = when {
        !enabled -> Line100
        checked -> Brand500
        else -> Line300
    }
    val trackShape = RoundedCornerShape(TrackRadius)
    val interaction = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        )
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .then(interaction)
            .size(width = TrackWidth, height = TrackHeight)
            .background(trackColor, trackShape)
            .padding(TrackInset),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbX)
                .then(if (enabled) Modifier.duriseoShadow(CircleShape, DuriseoShadows.Sm) else Modifier)
                .size(ThumbSize)
                .background(Surface0, CircleShape)
                .then(if (enabled) Modifier else Modifier.border(1.dp, Line200, CircleShape)),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DuriseoSwitchPreview_On() {
    DuriseoTheme { Box(Modifier.padding(DuriseoSpacing.Space4)) { DuriseoSwitch(checked = true, onCheckedChange = {}) } }
}

@Preview(showBackground = true)
@Composable
private fun DuriseoSwitchPreview_Off() {
    DuriseoTheme { Box(Modifier.padding(DuriseoSpacing.Space4)) { DuriseoSwitch(checked = false, onCheckedChange = {}) } }
}

@Preview(showBackground = true)
@Composable
private fun DuriseoSwitchPreview_Disabled() {
    DuriseoTheme {
        Row(Modifier.padding(DuriseoSpacing.Space4)) {
            DuriseoSwitch(checked = false, onCheckedChange = null, enabled = false)
        }
    }
}
