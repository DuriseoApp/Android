package com.duriseo.android.feature.settings.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.component.DuriseoAvatar
import com.duriseo.android.core.designsystem.component.DuriseoAvatarSize
import com.duriseo.android.core.designsystem.component.DuriseoButtonStyle
import com.duriseo.android.core.designsystem.component.DuriseoDashedLink
import com.duriseo.android.core.designsystem.component.DuriseoDialog
import com.duriseo.android.core.designsystem.component.DuriseoDialogLayer
import com.duriseo.android.core.designsystem.component.DuriseoEmptyPartnerSlot
import com.duriseo.android.core.designsystem.component.DuriseoTogetherMark
import com.duriseo.android.core.designsystem.component.DuriseoTopAppBar
import com.duriseo.android.core.designsystem.theme.Brand100
import com.duriseo.android.core.designsystem.theme.Brand500
import com.duriseo.android.core.designsystem.theme.Brand700
import com.duriseo.android.core.designsystem.theme.Danger700
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.Line200
import com.duriseo.android.core.designsystem.theme.OnBrand
import com.duriseo.android.core.designsystem.theme.PersonMe
import com.duriseo.android.core.designsystem.theme.PersonPartner
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.core.designsystem.theme.Surface100
import com.duriseo.android.feature.settings.R
import com.duriseo.android.feature.settings.menu.SettingsPerson
import com.duriseo.android.feature.settings.menu.SettingsSpace
import com.duriseo.android.feature.settings.menu.formatDate
import java.time.LocalDate
import com.duriseo.android.core.designsystem.R as DesignR

// SCR-SET-01 하위 화면 "연결 정보" (FR-SET-01, FR-COUPLE-05). 이미지 `SCR-SET-01--couple.png`·`--couple-solo.png`·
// `--unlink-dialog.png`, 보드 `ConnectionInfo.dc.html`. 수치는 보드 값을 이름 붙인 상수로 두고, 토큰이 있는 값은 토큰을 쓴다.
//
// 디자인 반영 후 맞춤(이미지 대기): 처음 만난 날 없는 커플 (`SCR-SET-01--couple-no-start-date.png` 예정, D-99).
// SCREENS.md는 "행을 숨기는지 라벨만 남기는지는 요청서 5번의 그림대로 정해요"라서, 그림이 오기 전까지는
// 처음 만난 날·함께한 지 행을 모두 숨긴다.

/** 본문 여백: 위 8, 좌우 20, 아래 28. 덩어리 사이 20 (보드 `padding: 8px 20px 28px`, `gap: 20px`) */
private val ContentTop = DuriseoSpacing.Space2
private val ContentBottom = 28.dp
private val BlockGap = DuriseoSpacing.Space5

/** 연결 상태 카드: 안쪽 위 24·좌우 16·아래 20, 덩어리 사이 16 */
private val StatusPaddingTop = DuriseoSpacing.Space6
private val StatusPaddingHorizontal = DuriseoSpacing.Space4
private val StatusPaddingBottom = DuriseoSpacing.Space5
private val StatusGap = DuriseoSpacing.Space4

/** 카드 안 세 칸: 가운데 칸 폭 72, 높이 56. 이름과 아바타 사이 8 */
private val StatusMiddleWidth = 72.dp
private val StatusMiddleHeight = 56.dp
private val PersonGap = DuriseoSpacing.Space2

/** "함께" 마크 40 x 28, 혼자 점선 56 */
private val MarkWidth = 40.dp
private val MarkHeight = 28.dp
private val DashedLinkWidth = 56.dp

/** 상태 칩: 안쪽 위아래 4·좌우 12, 체크 14, 체크와 글자 사이 5 */
private val ChipPaddingVertical = DuriseoSpacing.Space1
private val ChipPaddingHorizontal = DuriseoSpacing.Space3
private val ChipIconSize = 14.dp
private val ChipGap = 5.dp

/** 정보 행 높이 52 */
private val InfoRowHeight = 52.dp

/** [연결 해제] 높이 52, 테두리 1. 안내와 사이 12 */
private val UnlinkButtonHeight = 52.dp
private val OutlineWidth = 1.dp
private val UnlinkGap = DuriseoSpacing.Space3

/** 혼자 사용 중: 초대코드 만들기 카드 최소 높이 88, 안쪽 위아래 18·좌우 20, 아이콘 칸 44, 사이 14 */
private val MakeCodeMinHeight = 88.dp
private val MakeCodePaddingVertical = 18.dp
private val MakeCodePaddingHorizontal = DuriseoSpacing.Space5
private val MakeCodeTileSize = 44.dp
private val CardIconGap = 14.dp
private val CardIconSize = 22.dp

/** 카드 제목과 부제 사이 2 */
private val CardTextGap = 2.dp
private const val OnBrandSubtitleAlpha = 0.88f
private const val OnBrandTileAlpha = 0.18f

/** 받은 초대코드 입력하기 행: 최소 높이 64, 좌우 18, 오른쪽 화살표 20 */
private val EnterCodeMinHeight = 64.dp
private val EnterCodePaddingHorizontal = 18.dp
private val EnterChevronSize = 20.dp

@Composable
fun ConnectionInfoRoute(
    padding: PaddingValues,
    onBack: () -> Unit,
    onInviteCreate: () -> Unit,
    onInviteEnter: () -> Unit,
    onShowSnackBar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConnectionInfoViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ConnectionInfoSideEffect.ShowMessage -> onShowSnackBar(effect.messageRes)
            }
        }
    }

    ConnectionInfoScreen(
        state = state,
        padding = padding,
        onBack = onBack,
        onInviteCreate = onInviteCreate,
        onInviteEnter = onInviteEnter,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
fun ConnectionInfoScreen(
    state: ConnectionInfoUiState,
    padding: PaddingValues,
    onBack: () -> Unit,
    onInviteCreate: () -> Unit,
    onInviteEnter: () -> Unit,
    onAction: (ConnectionInfoAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val couple = state.space as? SettingsSpace.Couple
    DuriseoDialogLayer(
        dialogVisible = state.unlinkDialogVisible,
        dialog = {
            if (couple != null) {
                DuriseoDialog(
                    title = stringResource(R.string.connection_unlink_dialog_title),
                    body = stringResource(R.string.connection_unlink_dialog_body, couple.partner.name),
                    cancelText = stringResource(DesignR.string.action_cancel),
                    confirmText = stringResource(R.string.connection_unlink),
                    onCancel = { onAction(ConnectionInfoAction.UnlinkDismissed) },
                    onConfirm = { onAction(ConnectionInfoAction.UnlinkConfirmed) },
                    confirmStyle = DuriseoButtonStyle.Destructive,
                    buttonsEnabled = !state.requesting,
                )
            }
        },
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
        loading = state.loading,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            DuriseoTopAppBar(title = stringResource(R.string.connection_title), onBack = onBack)
            when (val space = state.space) {
                null -> Unit
                is SettingsSpace.Couple -> CoupleContent(
                    space = space,
                    onUnlink = { onAction(ConnectionInfoAction.UnlinkClicked) },
                    enabled = !state.requesting,
                    modifier = Modifier.weight(1f),
                )
                is SettingsSpace.Alone -> AloneContent(
                    space = space,
                    onInviteCreate = onInviteCreate,
                    onInviteEnter = onInviteEnter,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CoupleContent(
    space: SettingsSpace.Couple,
    onUnlink: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = DuriseoSpacing.Space5,
                end = DuriseoSpacing.Space5,
                top = ContentTop,
                bottom = ContentBottom,
            ),
        verticalArrangement = Arrangement.spacedBy(BlockGap),
    ) {
        StatusCard(
            left = { PersonItem(space.me, stringResource(R.string.profile_letter_me), PersonMe) },
            middle = {
                DuriseoTogetherMark(
                    width = MarkWidth,
                    height = MarkHeight,
                    meColorKey = space.me.colorKey,
                    partnerColorKey = space.partner.colorKey,
                    togetherColorKey = space.togetherColorKey,
                )
            },
            right = { PersonItem(space.partner, stringResource(R.string.profile_letter_partner), PersonPartner) },
            chip = { ConnectedChip() },
        )
        InfoRows(space)
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(UnlinkGap)) {
            Text(
                text = stringResource(R.string.connection_unlink_note),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = Ink600,
            )
            UnlinkButton(onClick = onUnlink, enabled = enabled)
        }
    }
}

/** 정보 행 하나. [highlight]면 값이 `brand-700` (함께한 지) */
private data class InfoRow(val label: String, val value: String, val highlight: Boolean = false)

/** 연결한 날 · 처음 만난 날 · 함께한 지 · 함께 쓰는 것. 값이 없는 행은 숨긴다 (D-99, 서버가 `linkedAt`을 안 준 경우) */
@Composable
private fun InfoRows(space: SettingsSpace.Couple) {
    val rows = listOfNotNull(
        space.linkedDate?.let { InfoRow(stringResource(R.string.connection_row_linked), formatDate(it)) },
        space.startDate?.let { InfoRow(stringResource(R.string.connection_row_start_date), formatDate(it)) },
        space.dayNumber?.let {
            InfoRow(
                stringResource(R.string.connection_row_days),
                stringResource(R.string.profile_days_together, it),
                highlight = true,
            )
        },
        InfoRow(stringResource(R.string.connection_row_shared), stringResource(R.string.connection_shared_value)),
    )
    Column {
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(InfoRowHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink600,
                )
                Text(
                    text = row.value,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (row.highlight) Brand700 else Ink900,
                )
            }
            if (index != rows.lastIndex) HorizontalDivider(color = Line100)
        }
    }
}

@Composable
private fun UnlinkButton(onClick: () -> Unit, enabled: Boolean) {
    val shape = MaterialTheme.shapes.large
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(UnlinkButtonHeight)
            .clip(shape)
            .background(Surface0)
            .border(OutlineWidth, Line200, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.connection_unlink),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Danger700,
        )
    }
}

/**
 * 혼자 사용 중: 소개문, [초대코드 만들기] → SCR-COUPLE-02, [받은 초대코드 입력하기] → SCR-COUPLE-03 (D-97).
 * 화면 안에 코드 패널과 "연결을 해제했어요" 안내는 없다 (SCREENS.md).
 */
@Composable
private fun AloneContent(
    space: SettingsSpace.Alone,
    onInviteCreate: () -> Unit,
    onInviteEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = DuriseoSpacing.Space5,
                end = DuriseoSpacing.Space5,
                top = ContentTop,
                bottom = ContentBottom,
            ),
        verticalArrangement = Arrangement.spacedBy(BlockGap),
    ) {
        StatusCard(
            left = { PersonItem(space.me, stringResource(R.string.profile_letter_me), PersonMe) },
            middle = { DuriseoDashedLink(width = DashedLinkWidth) },
            right = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(PersonGap),
                ) {
                    DuriseoEmptyPartnerSlot(DuriseoAvatarSize.Medium)
                    Text(
                        text = stringResource(R.string.profile_partner_empty),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Ink600,
                    )
                }
            },
            chip = { AloneChip() },
        )
        Text(
            text = stringResource(R.string.connection_solo_intro),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = Ink600,
            textAlign = TextAlign.Center,
        )
        MakeCodeCard(onClick = onInviteCreate)
        EnterCodeRow(onClick = onInviteEnter)
    }
}

@Composable
private fun StatusCard(
    left: @Composable () -> Unit,
    middle: @Composable () -> Unit,
    right: @Composable () -> Unit,
    chip: @Composable () -> Unit,
) {
    val description = stringResource(R.string.connection_status_description)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface100, MaterialTheme.shapes.extraLarge)
            .padding(
                start = StatusPaddingHorizontal,
                end = StatusPaddingHorizontal,
                top = StatusPaddingTop,
                bottom = StatusPaddingBottom,
            )
            .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StatusGap),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) { left() }
            Box(
                modifier = Modifier
                    .width(StatusMiddleWidth)
                    .height(StatusMiddleHeight),
                contentAlignment = Alignment.Center,
            ) { middle() }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) { right() }
        }
        chip()
    }
}

/** 아바타 56 + 이름 15/700 */
@Composable
private fun PersonItem(person: SettingsPerson, letter: String, fallback: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PersonGap),
    ) {
        DuriseoAvatar(
            label = letter,
            colorKey = person.colorKey,
            size = DuriseoAvatarSize.Medium,
            fallback = fallback,
        )
        Text(
            text = person.name,
            style = MaterialTheme.typography.titleSmall,
            color = Ink900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ConnectedChip() {
    Row(
        modifier = Modifier
            .background(Brand100, DuriseoShapes.Full)
            .padding(horizontal = ChipPaddingHorizontal, vertical = ChipPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChipGap),
    ) {
        Icon(
            painter = painterResource(DesignR.drawable.ic_check),
            contentDescription = null,
            modifier = Modifier.size(ChipIconSize),
            tint = Brand700,
        )
        Text(
            text = stringResource(R.string.settings_connection_connected),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Brand700,
        )
    }
}

@Composable
private fun AloneChip() {
    Text(
        text = stringResource(R.string.settings_connection_alone),
        modifier = Modifier
            .background(Surface0, DuriseoShapes.Full)
            .padding(horizontal = ChipPaddingHorizontal, vertical = ChipPaddingVertical),
        style = MaterialTheme.typography.labelLarge,
        color = Ink600,
    )
}

@Composable
private fun MakeCodeCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MakeCodeMinHeight)
            .clip(DuriseoShapes.Xl)
            .background(Brand500)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = MakeCodePaddingHorizontal, vertical = MakeCodePaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CardIconGap),
    ) {
        Box(
            modifier = Modifier
                .size(MakeCodeTileSize)
                .background(OnBrand.copy(alpha = OnBrandTileAlpha), MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_ticket_plus),
                contentDescription = null,
                modifier = Modifier.size(CardIconSize),
                tint = OnBrand,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(CardTextGap)) {
            Text(
                text = stringResource(R.string.connection_make_code),
                style = MaterialTheme.typography.titleMedium,
                color = OnBrand,
            )
            Text(
                text = stringResource(R.string.connection_make_code_subtitle),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = OnBrand.copy(alpha = OnBrandSubtitleAlpha),
            )
        }
    }
}

@Composable
private fun EnterCodeRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = EnterCodeMinHeight)
            .clip(DuriseoShapes.Xl)
            .border(OutlineWidth, Line200, DuriseoShapes.Xl)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = EnterCodePaddingHorizontal),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CardIconGap),
    ) {
        Icon(
            painter = painterResource(DesignR.drawable.ic_keyboard),
            contentDescription = null,
            modifier = Modifier.size(CardIconSize),
            tint = Ink900,
        )
        Text(
            text = stringResource(R.string.connection_enter_code),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Ink900,
        )
        Icon(
            painter = painterResource(DesignR.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(EnterChevronSize),
            tint = Ink600,
        )
    }
}

private val PreviewMe = SettingsPerson("민지", "pick-orange")
private val PreviewPartner = SettingsPerson("지우", "pick-blue")

private fun previewCouple(startDate: LocalDate?) = SettingsSpace.Couple(
    me = PreviewMe,
    partner = PreviewPartner,
    togetherColorKey = "pick-purple",
    startDate = startDate,
    dayNumber = startDate?.let { 20L },
    linkedDate = LocalDate.of(2026, 10, 12),
)

@Composable
private fun PreviewConnectionInfo(state: ConnectionInfoUiState) {
    DuriseoTheme {
        ConnectionInfoScreen(
            state = state,
            padding = PaddingValues(),
            onBack = {},
            onInviteCreate = {},
            onInviteEnter = {},
            onAction = {},
        )
    }
}

/** 연결됨 (`SCR-SET-01--couple.png`) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ConnectionInfoScreenCouplePreview() =
    PreviewConnectionInfo(ConnectionInfoUiState(space = previewCouple(LocalDate.of(2026, 10, 12))))

/** 혼자 사용 중 (`SCR-SET-01--couple-solo.png`) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ConnectionInfoScreenSoloPreview() =
    PreviewConnectionInfo(ConnectionInfoUiState(space = SettingsSpace.Alone(PreviewMe)))

/** 연결 해제 확인 (`SCR-SET-01--unlink-dialog.png`) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ConnectionInfoScreenUnlinkDialogPreview() = PreviewConnectionInfo(
    ConnectionInfoUiState(space = previewCouple(LocalDate.of(2026, 10, 12)), unlinkDialogVisible = true),
)

/** 해제 요청 중: 다이얼로그 버튼이 눌리지 않는다 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ConnectionInfoScreenUnlinkRequestingPreview() = PreviewConnectionInfo(
    ConnectionInfoUiState(
        space = previewCouple(LocalDate.of(2026, 10, 12)),
        unlinkDialogVisible = true,
        requesting = true,
    ),
)

/** 디자인 반영 후 맞춤(이미지 대기): 처음 만난 날 없는 커플 (`SCR-SET-01--couple-no-start-date.png` 예정, D-99). 두 행을 숨긴다 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ConnectionInfoScreenCoupleNoStartDatePreview() =
    PreviewConnectionInfo(ConnectionInfoUiState(space = previewCouple(startDate = null)))
