package com.duriseo.android.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme

// 앱바 값 (design-system.md "정보 행 (계정 화면)" 끝, 보드 `Account.dc.html` 18~24행, `Menu.dc.html` 17~22행).
// 높이 64(`size-appbar`), 아이콘 버튼 48x48, 아이콘 24(선 1.8).

@Composable
private fun AppBarIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(DuriseoSize.ButtonMd),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(DuriseoSize.Icon),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * 하위 화면 앱바: 왼쪽 뒤로 가기 버튼(접근성 "뒤로 가기"), 가운데 제목 `title-md`(17/700). 계정 화면이 쓴다.
 * 오른쪽에 뒤로 가기와 같은 폭의 빈 자리를 둬서 제목이 화면 가운데에 온다.
 */
@Composable
fun DuriseoTopAppBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DuriseoSize.Appbar)
            .padding(horizontal = DuriseoSpacing.Space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppBarIconButton(
            iconRes = R.drawable.ic_back,
            contentDescription = stringResource(R.string.nav_back),
            onClick = onBack,
        )
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(Modifier.width(DuriseoSize.ButtonMd).clearAndSetSemantics { })
    }
}

/**
 * 탭 밖 화면 앱바: 왼쪽 제목 `title-lg`(22/700), 오른쪽 닫기 버튼(접근성 "닫기"). 전체 메뉴가 쓴다.
 * 제목 왼쪽 여백은 `space-5`(20), 닫기 버튼 오른쪽은 `space-2`(8).
 */
@Composable
fun DuriseoCloseTopAppBar(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DuriseoSize.Appbar)
            .padding(start = DuriseoSpacing.Space5, end = DuriseoSpacing.Space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        AppBarIconButton(
            iconRes = R.drawable.ic_close,
            contentDescription = stringResource(R.string.nav_close),
            onClick = onClose,
        )
    }
}

// Preview 제목은 모양을 보여 주는 견본이라 앱 문구가 아니다.

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DuriseoTopAppBarPreview() {
    DuriseoTheme {
        DuriseoTopAppBar(title = "Title 17/700", onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DuriseoCloseTopAppBarPreview() {
    DuriseoTheme {
        DuriseoCloseTopAppBar(title = "Title 22/700", onClose = {})
    }
}
