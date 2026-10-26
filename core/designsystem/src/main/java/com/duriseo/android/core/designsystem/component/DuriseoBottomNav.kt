package com.duriseo.android.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Line100

/**
 * 하단 탭 한 칸의 상태. `feature:main`은 이 상태만 넘기고 그림은 [DuriseoBottomNav]가 그린다.
 *
 * @property iconRes 탭 아이콘(VectorDrawable). 라벨 글자가 있으므로 `contentDescription`은 없다.
 *   TODO(Docs PR): 달력·메모 아이콘 원본이 Docs에 정해지면 `feature:main`이 넘긴다 (확인 필요 3).
 *   null이면 아이콘 자리만 비워 둔다.
 */
@Immutable
data class DuriseoBottomNavItem(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    @DrawableRes val iconRes: Int? = null,
)

/**
 * 하단 탭 (D-30, SCREENS.md 공통 "하단 탭"). 높이 80(`size-bottomnav`), 위 구분선 `line-100`, 배경 `surface-0`.
 * 선택 탭은 `brand-100` 알약 + `brand-700` 아이콘, 라벨 `ink-900`. 선택하지 않은 탭은 `ink-500`.
 *
 * 80은 내용 높이이고, 제스처·내비게이션 바 inset은 그 아래에 더한다 (unit-1.md 결정 5).
 * 탭 칸 전체가 터치 영역이고(>= `size-touch`), `Role.Tab` + selected로 읽힌다.
 *
 * TODO(Docs PR): 토큰에 없는 보드 값을 Docs 보충 뒤에 맞춘다 (확인 필요 2).
 *  - 알약 56x30 모서리 15 (지금은 아이콘 24 둘레 `space-5`/`space-1` 여백의 `radius-full` 알약 64x32)
 *  - 아이콘 22 (지금은 `size-icon` 24)
 *  - 선택 라벨 12/700 (지금은 `caption` 12/500 그대로)
 *  - 위 테두리 1 (지금은 Material 기본 구분선 두께)
 */
@Composable
fun DuriseoBottomNav(
    items: List<DuriseoBottomNavItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DuriseoSize.BottomNav),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .selectableGroup(),
            ) {
                items.forEach { item ->
                    BottomNavTab(
                        item = item,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.align(Alignment.TopCenter),
                color = Line100,
            )
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun BottomNavTab(
    item: DuriseoBottomNavItem,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val pillColor = if (item.selected) scheme.primaryContainer else Color.Transparent
    val iconColor = if (item.selected) scheme.onPrimaryContainer else Ink500
    val labelColor = if (item.selected) scheme.onSurface else Ink500

    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = item.selected,
                onClick = item.onClick,
                role = Role.Tab,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space1, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .clip(DuriseoShapes.Full)
                .background(pillColor)
                .padding(horizontal = DuriseoSpacing.Space5, vertical = DuriseoSpacing.Space1),
            contentAlignment = Alignment.Center,
        ) {
            if (item.iconRes != null) {
                Icon(
                    painter = painterResource(item.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(DuriseoSize.Icon),
                    tint = iconColor,
                )
            } else {
                Spacer(Modifier.size(DuriseoSize.Icon))
            }
        }
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
        )
    }
}

@Composable
private fun previewItems(selectedIndex: Int): List<DuriseoBottomNavItem> = listOf(
    DuriseoBottomNavItem(
        label = stringResource(R.string.tab_calendar),
        selected = selectedIndex == 0,
        onClick = {},
    ),
    DuriseoBottomNavItem(
        label = stringResource(R.string.tab_memo),
        selected = selectedIndex == 1,
        onClick = {},
    ),
)

@Preview(showBackground = true)
@Composable
private fun DuriseoBottomNavCalendarSelectedPreview() {
    DuriseoTheme {
        DuriseoBottomNav(items = previewItems(selectedIndex = 0))
    }
}

@Preview(showBackground = true)
@Composable
private fun DuriseoBottomNavMemoSelectedPreview() {
    DuriseoTheme {
        DuriseoBottomNav(items = previewItems(selectedIndex = 1))
    }
}
