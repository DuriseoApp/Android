package com.duriseo.android.feature.couple.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.Ink900

// SCR-COUPLE-01~03 공용 조각. 수치는 보드 Connect·InviteCreate·InviteEnter.dc.html에서 가져온 값이고
// 토큰에 없는 것은 이름을 붙인 상수로 둔다 (unit-3.md 확인 필요 14).

/** 뒤로 가기 버튼 한 변 48 (보드 `Connect.dc.html` 20행). `size-touch` 44 이상 */
private val BackButtonSize = 48.dp

/**
 * 맨 위 64 높이 줄. 왼쪽에 뒤로 가기 버튼. [showBack]이 false면 자리만 두고 버튼은 그리지 않는다
 * (앞 화면이 없는 진입, unit-3.md 확인 필요 4).
 */
@Composable
fun CoupleHeader(
    showBack: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(DuriseoSize.Appbar)
            .padding(horizontal = DuriseoSpacing.Space2),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (showBack) {
            Box(
                modifier = Modifier
                    .size(BackButtonSize)
                    .clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(DesignR.drawable.ic_back),
                    contentDescription = stringResource(DesignR.string.nav_back),
                    modifier = Modifier.size(DuriseoSize.Icon),
                    tint = Ink900,
                )
            }
        }
    }
}

/** 화면 제목 `headline` 26/35 700. 접근성 제목으로 읽힌다 */
@Composable
fun CoupleTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineMedium,
        color = Ink900,
    )
}
