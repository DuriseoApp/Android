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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Line100

// 하단 탭 보드 값 (design-system.md "하단 탭", 보드 `Memo.dc.html` 104~112행).
// 토큰에 없는 수치라 이름을 붙인 상수로 둔다. 토큰(높이 80, 색, 간격 4)은 DuriseoSize·색 역할을 쓴다.

/** 위 테두리 두께 1 (104행) */
private val TopBorderWidth = 1.dp

/** 알약 56x30, 모서리 15 = 높이의 절반 (106·110행) */
private val PillWidth = 56.dp
private val PillHeight = 30.dp
private val PillCornerRadius = 15.dp

/** 아이콘 22 (24 그리드 아이콘을 줄여 그림, 106·110행). `size-icon` 24가 아니다 */
private val TabIconSize = 22.dp

/** 선택 라벨 굵기 700 (109행). 크기·행간은 `caption`(12/16) 그대로 */
private val SelectedLabelWeight = FontWeight.Bold

/**
 * 하단 탭 한 칸의 상태. `feature:main`은 이 상태만 넘기고 그림은 [DuriseoBottomNav]가 그린다.
 *
 * @property iconRes 탭 아이콘(VectorDrawable, 선 아이콘). 라벨 글자가 있으므로 `contentDescription`은 없다.
 *   선택돼도 같은 아이콘이고 색만 바뀐다. null이면 아이콘 자리만 비워 둔다.
 */
@Immutable
data class DuriseoBottomNavItem(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    @DrawableRes val iconRes: Int? = null,
)

/**
 * 하단 탭 (D-30, design-system.md "하단 탭"). 높이 80(`size-bottomnav`), 배경 `surface-0`,
 * 위 테두리 `line-100` 1.
 * 선택 칸은 `brand-100` 알약(56x30) + `brand-700` 아이콘 22 + 라벨 `ink-900` 12/700,
 * 비선택 칸은 같은 크기의 빈 알약 자리 + `ink-500` 아이콘·라벨(`caption` 12/500). 알약과 라벨 사이 `space-1`.
 *
 * 80은 내용 높이이고, 제스처·내비게이션 바 inset은 그 아래에 더한다 (unit-1.md 결정 5).
 * 탭 칸 전체가 터치 영역이고(>= `size-touch`), 묶음 라벨은 "주요 메뉴", 칸은 `Role.Tab` + selected로 읽힌다.
 */
@Composable
fun DuriseoBottomNav(
    items: List<DuriseoBottomNavItem>,
    modifier: Modifier = Modifier,
) {
    val navLabel = stringResource(R.string.bottom_nav_label)
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
                    .semantics { contentDescription = navLabel }
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
                thickness = TopBorderWidth,
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
    // 선택 알약·아이콘은 secondaryContainer / onSecondaryContainer = brand-100 / brand-700 (매핑 표)
    val pillColor = if (item.selected) scheme.secondaryContainer else Color.Transparent
    val iconColor = if (item.selected) scheme.onSecondaryContainer else Ink500
    val labelColor = if (item.selected) scheme.onSurface else Ink500
    val labelStyle = MaterialTheme.typography.labelMedium.let {
        if (item.selected) it.copy(fontWeight = SelectedLabelWeight) else it
    }

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
                .size(width = PillWidth, height = PillHeight)
                .clip(RoundedCornerShape(PillCornerRadius))
                .background(pillColor),
            contentAlignment = Alignment.Center,
        ) {
            if (item.iconRes != null) {
                Icon(
                    painter = painterResource(item.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(TabIconSize),
                    tint = iconColor,
                )
            }
        }
        Text(
            text = item.label,
            style = labelStyle,
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
        iconRes = R.drawable.ic_tab_calendar,
    ),
    DuriseoBottomNavItem(
        label = stringResource(R.string.tab_memo),
        selected = selectedIndex == 1,
        onClick = {},
        iconRes = R.drawable.ic_tab_memo,
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
