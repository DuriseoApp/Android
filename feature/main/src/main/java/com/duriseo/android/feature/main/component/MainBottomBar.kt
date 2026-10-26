package com.duriseo.android.feature.main.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.duriseo.android.core.designsystem.component.DuriseoBottomNav
import com.duriseo.android.core.designsystem.component.DuriseoBottomNavItem

/**
 * 하단 탭. `feature:main`은 선택 상태와 클릭만 넘기고 그림은 `core:designsystem`의 [DuriseoBottomNav]가 그린다.
 *
 * TODO(Docs PR): 탭 아이콘(달력·메모)은 Docs에 원본이 정해지면 `iconRes`로 넘긴다 (확인 필요 3).
 */
@Composable
fun MainBottomBar(
    currentTab: MainTab?,
    onTabSelected: (MainTab) -> Unit,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) +
            expandVertically(expandFrom = Alignment.Top) +
            fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) +
            shrinkVertically(shrinkTowards = Alignment.Top) +
            fadeOut(),
    ) {
        DuriseoBottomNav(
            modifier = modifier,
            items = MainTab.entries.map { tab ->
                DuriseoBottomNavItem(
                    label = stringResource(tab.labelRes),
                    selected = tab == currentTab,
                    onClick = { onTabSelected(tab) },
                )
            },
        )
    }
}
