package com.duriseo.android.feature.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme

// SCR-CAL-01 (월 캘린더)의 자리표시. 화면 구성은 단위 4에서 SCREENS.md대로 만든다.
// 문서에 없는 문구를 만들지 않기 위해 지금은 문구 없는 빈 화면이고, 앱바 오른쪽에 [전체 메뉴] 아이콘 버튼만 있다
// (단위 2의 전체 메뉴·계정 화면에 들어가는 임시 진입, unit-2.md 확인 필요 3. 보드 `Calendar.dc.html` 45행, 접근성 "전체 메뉴").

@Composable
fun CalendarRoute(
    padding: PaddingValues,
    onOpenMenu: () -> Unit,
    @Suppress("UNUSED_PARAMETER") viewModel: CalendarViewModel = hiltViewModel(),
) {
    CalendarScreen(padding = padding, onOpenMenu = onOpenMenu)
}

@Composable
fun CalendarScreen(
    padding: PaddingValues,
    onOpenMenu: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(DuriseoSize.Appbar)
                .padding(horizontal = DuriseoSpacing.Space2),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier.size(DuriseoSize.ButtonMd),
            ) {
                Icon(
                    painter = painterResource(DesignR.drawable.ic_menu),
                    contentDescription = stringResource(R.string.calendar_menu_description),
                    modifier = Modifier.size(DuriseoSize.Icon),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarScreenPlaceholderPreview() {
    DuriseoTheme {
        CalendarScreen(padding = PaddingValues(), onOpenMenu = {})
    }
}
