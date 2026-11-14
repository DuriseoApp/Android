package com.duriseo.android.feature.settings.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.DuriseoLoadingLayer
import com.duriseo.android.core.designsystem.component.DuriseoTopAppBar
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink400
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.Line200
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.domain.notification.NotificationSettingKind
import com.duriseo.android.domain.notification.NotificationSettings
import com.duriseo.android.feature.settings.R

// SCR-SET-02 알림 설정 (FR-NOTI-02). 수치는 보드 `NotificationSettings.dc.html`. 토큰에 없는 보드 수치는 이름을 붙인 상수로 둔다.

/** 본문 위 4·좌우 20·아래 28, 카드와 안내 사이 16 */
private val ContentTop = DuriseoSpacing.Space1
private val ContentBottom = 28.dp
private val BlockGap = DuriseoSpacing.Space4

/** 스위치 행: 최소 높이 76, 안쪽 위아래 14·좌우 16, 글과 스위치 사이 12, 글 줄 사이 2 (보드 `rowStyle`) */
private val RowMinHeight = 76.dp
private val RowPaddingVertical = 14.dp
private val RowGap = DuriseoSpacing.Space3
private val TextGap = 2.dp

/** 안내 줄: 좌우 4, 줄 사이 6, 아이콘 16과 글 사이 8 */
private val NoteInset = DuriseoSpacing.Space1
private val NoteGap = 6.dp
private val NoteIcon = 16.dp

@Composable
fun NotificationSettingsRoute(
    padding: PaddingValues,
    onBack: () -> Unit,
    onShowSnackBar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                NotificationSettingsSideEffect.Back -> onBack()
                is NotificationSettingsSideEffect.ShowMessage -> onShowSnackBar(effect.messageRes)
            }
        }
    }
    NotificationSettingsScreen(state = state, onAction = viewModel::onAction, padding = padding, modifier = modifier)
}

@Composable
fun NotificationSettingsScreen(
    state: NotificationSettingsUiState,
    onAction: (NotificationSettingsAction) -> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
) {
    // 저장 중에는 화면 전체를 덮는다 (D-115)
    DuriseoLoadingLayer(loading = state.loading, modifier = modifier.background(Surface0)) {
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DuriseoTopAppBar(
                title = stringResource(R.string.notification_settings_title),
                onBack = { onAction(NotificationSettingsAction.BackClicked) },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = DuriseoSpacing.Space5, end = DuriseoSpacing.Space5, top = ContentTop, bottom = ContentBottom),
                verticalArrangement = Arrangement.spacedBy(BlockGap),
            ) {
                Column(modifier = Modifier.fillMaxWidth().border(1.dp, Line100, MaterialTheme.shapes.large)) {
                    NotificationSettingKind.entries.forEachIndexed { index, kind ->
                        if (index > 0) HorizontalDivider(color = Line100)
                        SettingRow(
                            label = stringResource(kind.labelRes()),
                            description = stringResource(kind.descriptionRes()),
                            checked = state.isOn(kind),
                            enabled = state.switchesEnabled,
                            dimmed = state.settings == null,
                            onToggle = { onAction(NotificationSettingsAction.Toggled(kind)) },
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(horizontal = NoteInset),
                    verticalArrangement = Arrangement.spacedBy(NoteGap),
                ) {
                    Note(iconRes = DesignR.drawable.ic_link, text = stringResource(R.string.notification_settings_note_couple))
                    Note(iconRes = DesignR.drawable.ic_bell, text = stringResource(R.string.notification_settings_note_list))
                }
            }
        }
    }
}

/**
 * 스위치 한 줄. 값을 한 번도 못 받았으면([dimmed]) 글자가 `ink-500`이고 스위치는 비활성이다 (보드 `loadFailed`:
 * 트랙 `line-100`, 손잡이 `surface-0` + `line-200` 테두리). 읽는 이름은 스위치에 붙인다 (`role=switch`, 라벨).
 */
@Composable
private fun SettingRow(
    label: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    dimmed: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            // 줄 전체가 스위치다: 글이 스위치의 이름으로 읽힌다 (보드 `role="switch"`, `aria-label`)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = { onToggle() })
            .padding(horizontal = DuriseoSpacing.Space4, vertical = RowPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RowGap),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TextGap)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (dimmed) Ink500 else Ink900,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = if (dimmed) Ink500 else Ink600,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                disabledUncheckedTrackColor = Line100,
                disabledUncheckedThumbColor = Surface0,
                disabledUncheckedBorderColor = Line200,
            ),
        )
    }
}

@Composable
private fun Note(iconRes: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2)) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(NoteIcon).padding(top = 1.dp),
            tint = Ink400,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal, lineHeight = MaterialTheme.typography.bodySmall.lineHeight),
            color = Ink600,
        )
    }
}

private fun NotificationSettingKind.labelRes(): Int = when (this) {
    NotificationSettingKind.EVENT_REMINDER -> R.string.notification_settings_event_reminder
    NotificationSettingKind.PARTNER_CREATED -> R.string.notification_settings_partner_created
    NotificationSettingKind.MEMO_PINNED -> R.string.notification_settings_memo_pinned
    NotificationSettingKind.ANNIVERSARY -> R.string.notification_settings_anniversary
}

private fun NotificationSettingKind.descriptionRes(): Int = when (this) {
    NotificationSettingKind.EVENT_REMINDER -> R.string.notification_settings_event_reminder_description
    NotificationSettingKind.PARTNER_CREATED -> R.string.notification_settings_partner_created_description
    NotificationSettingKind.MEMO_PINNED -> R.string.notification_settings_memo_pinned_description
    NotificationSettingKind.ANNIVERSARY -> R.string.notification_settings_anniversary_description
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotificationSettingsPreview_Default() {
    DuriseoTheme {
        NotificationSettingsScreen(
            state = NotificationSettingsUiState(settings = NotificationSettings(true, true, true, true)),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotificationSettingsPreview_Mixed() {
    DuriseoTheme {
        NotificationSettingsScreen(
            state = NotificationSettingsUiState(settings = NotificationSettings(true, false, false, true)),
            onAction = {},
        )
    }
}

/** 서버 값을 한 번도 받지 못함: 스위치 4개 비활성 (공통 오류 스낵바는 앱 쪽이 위에서 40에 그린다) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotificationSettingsPreview_LoadFailed() {
    DuriseoTheme {
        NotificationSettingsScreen(state = NotificationSettingsUiState(settings = null), onAction = {})
    }
}

/** 저장 중: 화면 전체를 덮는다 (D-115) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotificationSettingsPreview_Saving() {
    DuriseoTheme {
        NotificationSettingsScreen(
            state = NotificationSettingsUiState(settings = NotificationSettings(true, true, true, true), saving = true),
            onAction = {},
        )
    }
}
