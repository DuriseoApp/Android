package com.duriseo.android.feature.settings.licenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.component.DuriseoTopAppBar
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink400
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.Surface50
import com.duriseo.android.feature.settings.R
import com.duriseo.android.core.designsystem.R as DesignR

// SCR-SET-05 오픈소스 라이선스 (FR-SET-02 AC3, D-152). 이미지 `SCR-SET-05.png`·`SCR-SET-05--expanded.png`,
// 보드 `OpenSourceLicenses.dc.html`, design-system.md "오픈소스 라이선스 목록". 하단 탭이 없다.

/** 목록 여백: 위 4, 좌우 20, 아래 28 (보드 `padding: 4px 20px 28px`) */
private val ListPadding = PaddingValues(
    start = DuriseoSpacing.Space5,
    end = DuriseoSpacing.Space5,
    top = DuriseoSpacing.Space1,
    bottom = 28.dp,
)

/** 행 높이 52, 이름과 라이선스 이름 사이 12, 라이선스 이름과 화살표 사이 8, 화살표 18 */
private val RowHeight = 52.dp
private val RowGap = DuriseoSpacing.Space3
private val ChevronGap = DuriseoSpacing.Space2
private val ChevronSize = 18.dp

/** 펼친 칸: 안쪽 16, 아래 간격 16, 글자 13 / 줄 높이 1.6배 */
private val BodyPadding = DuriseoSpacing.Space4
private val BodyBottom = DuriseoSpacing.Space4
private val BodyLineHeight = 20.8.sp

@Composable
fun LicensesRoute(
    padding: PaddingValues,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LicensesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LicensesScreen(
        state = state,
        padding = padding,
        onBack = onBack,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
fun LicensesScreen(
    state: LicensesUiState,
    padding: PaddingValues,
    onBack: () -> Unit,
    onAction: (LicensesAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(padding),
    ) {
        DuriseoTopAppBar(title = stringResource(R.string.licenses_title), onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = ListPadding,
        ) {
            val rows = state.displayRows
            items(rows, key = { it.id }) { row ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 첫 행 말고는 위에 구분선 1 (`line-100`)
                    if (row.id != rows.first().id) HorizontalDivider(color = Line100)
                    LicenseRowHeader(row = row, onClick = { onAction(LicensesAction.RowClicked(row.id)) })
                    if (row.expanded) {
                        Text(
                            text = row.body,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = BodyBottom)
                                .background(Surface50, MaterialTheme.shapes.small)
                                .padding(BodyPadding),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Normal,
                                lineHeight = BodyLineHeight,
                            ),
                            color = Ink600,
                        )
                    }
                }
            }
        }
    }
}

/** 이름 16/500 `ink-900`(한 줄 말줄임), 오른쪽에 라이선스 이름 13 `ink-500` + 화살표 18 `ink-400`(펼치면 위) */
@Composable
private fun LicenseRowHeader(row: LicenseRow, onClick: () -> Unit) {
    val description = stringResource(R.string.licenses_row_description, row.name, row.licenseName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = description
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RowGap),
    ) {
        Text(
            text = row.name,
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics { },
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ChevronGap),
        ) {
            Text(
                text = row.licenseName,
                modifier = Modifier.clearAndSetSemantics { },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = Ink500,
                maxLines = 1,
            )
            Icon(
                painter = painterResource(if (row.expanded) DesignR.drawable.ic_chevron_up else DesignR.drawable.ic_chevron_down),
                contentDescription = null,
                modifier = Modifier.size(ChevronSize),
                tint = Ink400,
            )
        }
    }
}

// Preview 목록은 보드의 자리표시 대신 모양을 보여 주는 견본이다 (라이브러리 이름·라이선스 전문은 구현 때 정해진다).
private const val SampleBody = "Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
    "you may not use this file except in compliance with the License.\n\n" +
    "Unless required by applicable law or agreed to in writing, software distributed under the License " +
    "is distributed on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND."

private val PreviewRows = listOf(
    LicenseRow("a", "AndroidX Core", "Apache License 2.0", SampleBody),
    LicenseRow("b", "Dagger", "Apache License 2.0", SampleBody),
    LicenseRow("c", "Kotlin Standard Library", "Apache License 2.0", SampleBody),
    LicenseRow("d", "Retrofit", "Apache License 2.0", SampleBody),
    LicenseRow("font:IBM Plex Sans KR", "IBM Plex Sans KR", "SIL OFL 1.1", SampleBody),
    LicenseRow("font:IBM Plex Mono", "IBM Plex Mono", "SIL OFL 1.1", SampleBody),
)

/** `SCR-SET-05.png`: 모두 접힌 목록, 글꼴 두 행이 끝 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LicensesPreview_Default() {
    DuriseoTheme {
        LicensesScreen(
            state = LicensesUiState(rows = PreviewRows),
            padding = PaddingValues(),
            onBack = {},
            onAction = {},
        )
    }
}

/** `SCR-SET-05--expanded.png`: 글꼴 행(5번째)을 펼친 모습 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LicensesPreview_Expanded() {
    DuriseoTheme {
        LicensesScreen(
            state = LicensesUiState(rows = PreviewRows, expandedId = "font:IBM Plex Sans KR"),
            padding = PaddingValues(),
            onBack = {},
            onAction = {},
        )
    }
}
