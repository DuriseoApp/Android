package com.duriseo.android.feature.settings.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.DuriseoButton
import com.duriseo.android.core.designsystem.component.DuriseoBottomSheetLayer
import com.duriseo.android.core.designsystem.component.DuriseoButtonStyle
import com.duriseo.android.core.designsystem.component.DuriseoCloseTopAppBar
import com.duriseo.android.core.designsystem.component.DuriseoDialogLayer
import com.duriseo.android.core.designsystem.component.DuriseoGuestBanner
import com.duriseo.android.core.designsystem.component.DuriseoMenuRow
import com.duriseo.android.core.designsystem.component.DuriseoSectionTitle
import com.duriseo.android.core.designsystem.component.LogoutConfirmDialog
import com.duriseo.android.core.designsystem.component.StartDatePickerSheet
import com.duriseo.android.core.designsystem.theme.Danger700
import com.duriseo.android.core.designsystem.theme.DuriseoPickPalette
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.feature.settings.R
import java.time.LocalDate
import java.time.YearMonth
import com.duriseo.android.domain.anniversary.DayCount

// SCR-SET-01 전체 메뉴 (FR-SET-01, FR-AUTH-04, FR-AUTH-05, FR-COUPLE-04). 앱바, 게스트 배너, 우리 프로필 카드(단위 3),
// 꾸미기 › 우리 색상(단위 3), "설정" 섹션의 연결 정보(단위 3)·계정 행, 로그아웃. 알림 설정은 단위 7,
// 정보 섹션과 앱 정보 카드는 어느 단위에도 계획이 없어 unit-2.md 확인 필요 4에 올렸다.
// 수치는 design-system.md "메뉴 행"과 보드 `Menu.dc.html`. 토큰에 없는 수치는 이름 붙인 상수로 둔다.

/**
 * 앱바 아래 위 여백 4. 출처: 캔버스 원본 `../Docs/design/source/Menu.dc.html` 24행(`padding: 4px 20px 0`).
 * design-system 명세에는 아직 없다 (Docs 후속: "전체 메뉴 본문 위 여백 4" 추가 필요, unit-2.md).
 */
private val ContentTop = DuriseoSpacing.Space1

/** 덩어리 사이 간격 24 (24행, `space-6`) */
private val BlockGap = DuriseoSpacing.Space6

/** 현재 색 점 지름 14, 사이 4 (105·206행) */
private val DotSize = 14.dp
private val DotGap = DuriseoSpacing.Space1

/** 로그아웃 행 높이 52 (139행) */
private val LogoutHeight = 52.dp

@Composable
fun SettingsRoute(
    padding: PaddingValues,
    onClose: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenColors: () -> Unit,
    onOpenConnect: () -> Unit,
    onOpenConnectionInfo: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onLoggedOut: () -> Unit,
    onShowSnackBar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsSideEffect.ShowMessage -> onShowSnackBar(effect.messageRes)
                SettingsSideEffect.NavigateToStart -> onLoggedOut()
            }
        }
    }

    SettingsScreen(
        state = state,
        padding = padding,
        onClose = onClose,
        onOpenAccount = onOpenAccount,
        onOpenColors = onOpenColors,
        onOpenConnect = onOpenConnect,
        onOpenConnectionInfo = onOpenConnectionInfo,
        onOpenNotificationSettings = onOpenNotificationSettings,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    padding: PaddingValues,
    onClose: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenColors: () -> Unit,
    onOpenConnect: () -> Unit,
    onOpenConnectionInfo: () -> Unit,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
    onOpenNotificationSettings: () -> Unit = {},
) {
    val sheet = state.startDateSheet
    DuriseoDialogLayer(
        dialogVisible = state.logoutDialogVisible || sheet != null,
        dialog = {
            if (sheet != null) {
                // 처음 만난 날 선택 시트 (D-74). 저장 요청 중에는 뒤 막과 뒤로 가기로 닫히지 않는다
                BackHandler(enabled = !sheet.saving) { onAction(SettingsAction.StartDateSheetDismissed) }
                DuriseoBottomSheetLayer(
                    onDismissRequest = if (sheet.saving) null else ({ onAction(SettingsAction.StartDateSheetDismissed) }),
                ) {
                    StartDatePickerSheet(
                        visibleMonth = sheet.visibleMonth,
                        selectedDate = sheet.selected,
                        today = state.today,
                        onPreviousMonth = { onAction(SettingsAction.StartDatePreviousMonth) },
                        onNextMonth = { onAction(SettingsAction.StartDateNextMonth) },
                        onDateSelected = { onAction(SettingsAction.StartDateSelected(it)) },
                        onSave = { onAction(SettingsAction.StartDateSaved) },
                        onMonthSelected = { onAction(SettingsAction.StartDateMonthSelected(it)) },
                        saveEnabled = sheet.selected != null && !sheet.saving,
                    )
                }
            } else {
                LogoutConfirmDialog(
                    onCancel = { onAction(SettingsAction.DialogDismissed) },
                    onConfirm = { onAction(SettingsAction.LogoutConfirmed) },
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
            DuriseoCloseTopAppBar(
                title = stringResource(R.string.settings_title),
                onClose = onClose,
            )
            val account = state.account
            if (account != null) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = DuriseoSpacing.Space5,
                            end = DuriseoSpacing.Space5,
                            top = ContentTop,
                        ),
                    verticalArrangement = Arrangement.spacedBy(BlockGap),
                ) {
                    if (account is SettingsAccount.Guest) {
                        DuriseoGuestBanner(
                            title = stringResource(R.string.settings_guest_banner_title),
                            body = stringResource(R.string.settings_guest_banner_body),
                            action = {
                                DuriseoButton(
                                    text = stringResource(R.string.settings_guest_banner_action),
                                    onClick = onOpenAccount,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = DuriseoButtonStyle.Filled,
                                    enabled = !state.requesting,
                                )
                            },
                        )
                    }

                    // 우리 프로필 카드 (FR-SET-01). 게스트도 "혼자" 모양이다. [상대와 연결하기]는 SCR-COUPLE-01로 가고,
                    // 게스트면 거기서 로그인 안내(guest 모양)가 나온다
                    state.space?.let { space ->
                        SettingsProfileCard(
                            space = space,
                            onEditStartDate = { onAction(SettingsAction.StartDateEditClicked) },
                            onConnect = onOpenConnect,
                            enabled = !state.requesting,
                        )
                    }

                    // 꾸미기 › 우리 색상 (단위 3)
                    if (state.colorKeys.isNotEmpty()) {
                        Column {
                            DuriseoSectionTitle(text = stringResource(R.string.settings_section_deco))
                            DuriseoMenuRow(
                                label = stringResource(R.string.settings_colors),
                                onClick = onOpenColors,
                                iconRes = DesignR.drawable.ic_palette,
                                trailing = { ColorDots(state.colorKeys) },
                            )
                        }
                    }

                    Column {
                        DuriseoSectionTitle(text = stringResource(R.string.settings_section_settings))
                        // 알림 설정 -> SCR-SET-02 (보드 `Menu.dc.html` 126행: 종 아이콘, 값 없음. 게스트도 바꿀 수 있다, D-62)
                        DuriseoMenuRow(
                            label = stringResource(R.string.settings_notification_settings),
                            onClick = onOpenNotificationSettings,
                            iconRes = DesignR.drawable.ic_bell,
                        )
                        // 연결 정보: 값은 "연결됨"/"혼자 사용 중". 게스트는 [상대와 연결하기]와 같이 SCR-COUPLE-01(guest)로 간다
                        // (SCREENS.md "게스트의 연결 정보 행", FR-AUTH-04 AC2)
                        state.space?.let { space ->
                            DuriseoMenuRow(
                                label = stringResource(R.string.settings_connection),
                                onClick = if (account is SettingsAccount.Guest) onOpenConnect else onOpenConnectionInfo,
                                iconRes = DesignR.drawable.ic_link,
                                value = stringResource(
                                    if (space is SettingsSpace.Couple) {
                                        R.string.settings_connection_connected
                                    } else {
                                        R.string.settings_connection_alone
                                    },
                                ),
                            )
                        }
                        DuriseoMenuRow(
                            label = stringResource(R.string.settings_account),
                            onClick = onOpenAccount,
                            iconRes = DesignR.drawable.ic_user,
                            value = accountValue(account),
                            highlightValue = account is SettingsAccount.Guest,
                        )
                    }

                    if (state.canLogout) {
                        Column {
                            HorizontalDivider(color = Line100)
                            LogoutRow(
                                enabled = !state.requesting,
                                onClick = { onAction(SettingsAction.LogoutClicked) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 현재 색 점: 지름 14, 사이 4 (보드 `Menu.dc.html` 105·206행). 커플은 나·상대·함께 3개, 개인 공간은 나 1개.
 * 팔레트에 없는 키는 그리지 않는다. 읽는 이름은 행 전체에 붙는다 ("현재 색상: 나, 상대, 함께", 커플일 때).
 */
@Composable
private fun ColorDots(keys: List<String>) {
    val description = if (keys.size > 1) stringResource(R.string.settings_colors_description) else ""
    Row(
        modifier = Modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(DotGap),
    ) {
        keys.forEach { key ->
            val color = DuriseoPickPalette.colorOf(key) ?: return@forEach
            Box(
                modifier = Modifier
                    .size(DotSize)
                    .background(color, DuriseoShapes.Full),
            )
        }
    }
}

@Composable
private fun accountValue(account: SettingsAccount): String = when (account) {
    SettingsAccount.Guest -> stringResource(R.string.settings_account_guest)
    is SettingsAccount.Member -> when (account.provider) {
        SocialProvider.GOOGLE -> stringResource(R.string.settings_account_google)
        SocialProvider.KAKAO -> stringResource(R.string.settings_account_kakao)
        null -> ""
    }
}

/** 로그아웃: 높이 52, 15 / 600 `danger-700`, 왼쪽 정렬, 가로 전체 (139행) */
@Composable
private fun LogoutRow(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(LogoutHeight)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = stringResource(DesignR.string.action_logout),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Danger700,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenGuestPreview() {
    DuriseoTheme {
        SettingsScreen(
            state = SettingsUiState(account = SettingsAccount.Guest),
            padding = PaddingValues(),
            onClose = {},
            onOpenAccount = {},
            onOpenColors = {},
            onOpenConnect = {},
            onOpenConnectionInfo = {},
            onAction = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenMemberPreview() {
    DuriseoTheme {
        SettingsScreen(
            state = SettingsUiState(account = SettingsAccount.Member(SocialProvider.GOOGLE)),
            padding = PaddingValues(),
            onClose = {},
            onOpenAccount = {},
            onOpenColors = {},
            onOpenConnect = {},
            onOpenConnectionInfo = {},
            onAction = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenKakaoMemberPreview() {
    DuriseoTheme {
        SettingsScreen(
            state = SettingsUiState(account = SettingsAccount.Member(SocialProvider.KAKAO)),
            padding = PaddingValues(),
            onClose = {},
            onOpenAccount = {},
            onOpenColors = {},
            onOpenConnect = {},
            onOpenConnectionInfo = {},
            onAction = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenLogoutDialogPreview() {
    DuriseoTheme {
        SettingsScreen(
            state = SettingsUiState(
                account = SettingsAccount.Member(SocialProvider.GOOGLE),
                logoutDialogVisible = true,
            ),
            padding = PaddingValues(),
            onClose = {},
            onOpenAccount = {},
            onOpenColors = {},
            onOpenConnect = {},
            onOpenConnectionInfo = {},
            onAction = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenLogoutRequestingPreview() {
    DuriseoTheme {
        SettingsScreen(
            state = SettingsUiState(
                account = SettingsAccount.Member(SocialProvider.GOOGLE),
                logoutDialogVisible = true,
                requesting = true,
            ),
            padding = PaddingValues(),
            onClose = {},
            onOpenAccount = {},
            onOpenColors = {},
            onOpenConnect = {},
            onOpenConnectionInfo = {},
            onAction = {},
        )
    }
}

/** 꾸미기 › 우리 색상 행: 커플은 색 점 3개 (`SCR-SET-01.png`의 해당 줄) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenCoupleColorsPreview() {
    DuriseoTheme {
        SettingsScreen(
            state = SettingsUiState(
                account = SettingsAccount.Member(SocialProvider.GOOGLE),
                colorKeys = listOf("pick-orange", "pick-blue", "pick-purple"),
            ),
            padding = PaddingValues(),
            onClose = {},
            onOpenAccount = {},
            onOpenColors = {},
            onOpenConnect = {},
            onOpenConnectionInfo = {},
            onAction = {},
        )
    }
}

/** 개인 공간은 나 색 점 하나 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenSoloColorsPreview() {
    DuriseoTheme {
        SettingsScreen(
            state = SettingsUiState(
                account = SettingsAccount.Member(SocialProvider.GOOGLE),
                colorKeys = listOf("pick-orange"),
            ),
            padding = PaddingValues(),
            onClose = {},
            onOpenAccount = {},
            onOpenColors = {},
            onOpenConnect = {},
            onOpenConnectionInfo = {},
            onAction = {},
        )
    }
}

private val PreviewMe = SettingsPerson("민지", "pick-orange")
private val PreviewPartner = SettingsPerson("지우", "pick-blue")
private val PreviewToday: LocalDate = LocalDate.of(2026, 10, 31)

private fun previewCouple(startDate: LocalDate?) = SettingsSpace.Couple(
    me = PreviewMe,
    partner = PreviewPartner,
    togetherColorKey = "pick-purple",
    startDate = startDate,
    dayNumber = startDate?.let { DayCount.of(it, PreviewToday) },
    linkedDate = LocalDate.of(2026, 10, 12),
)

/** 커플: 나·너 아바타, D+N, 처음 만난 날 (`SCR-SET-01.png`) */
@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun SettingsScreenCouplePreview() {
    DuriseoTheme {
        SettingsScreen(
            state = SettingsUiState(
                account = SettingsAccount.Member(SocialProvider.GOOGLE),
                space = previewCouple(LocalDate.of(2026, 10, 12)),
                colorKeys = listOf("pick-orange", "pick-blue", "pick-purple"),
                today = PreviewToday,
            ),
            padding = PaddingValues(),
            onClose = {},
            onOpenAccount = {},
            onOpenColors = {},
            onOpenConnect = {},
            onOpenConnectionInfo = {},
            onAction = {},
        )
    }
}

/** 혼자(회원): 나 + "아직 없음" + [상대와 연결하기] (`SCR-SET-01--solo.png`) */
@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun SettingsScreenAlonePreview() {
    DuriseoTheme {
        SettingsScreen(
            state = SettingsUiState(
                account = SettingsAccount.Member(SocialProvider.KAKAO),
                space = SettingsSpace.Alone(PreviewMe),
                colorKeys = listOf("pick-orange"),
                today = PreviewToday,
            ),
            padding = PaddingValues(),
            onClose = {},
            onOpenAccount = {},
            onOpenColors = {},
            onOpenConnect = {},
            onOpenConnectionInfo = {},
            onAction = {},
        )
    }
}

/** 게스트: 맨 위 배너 + 혼자 모양 프로필 카드 (`SCR-SET-01--guest.png`) */
@Preview(showBackground = true, widthDp = 390, heightDp = 1100)
@Composable
private fun SettingsScreenGuestProfilePreview() {
    DuriseoTheme {
        SettingsScreen(
            state = SettingsUiState(
                account = SettingsAccount.Guest,
                space = SettingsSpace.Alone(SettingsPerson("게스트", "pick-orange")),
                colorKeys = listOf("pick-orange"),
                today = PreviewToday,
            ),
            padding = PaddingValues(),
            onClose = {},
            onOpenAccount = {},
            onOpenColors = {},
            onOpenConnect = {},
            onOpenConnectionInfo = {},
            onAction = {},
        )
    }
}

/** `SCR-SET-01--no-start-date.png`: 처음 만난 날 없는 커플 (D-99). D+N·"부터 함께하고 있어요" 줄 없음, 마크는 아바타 높이 가운데 */
@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun SettingsPreview_NoStartDate() {
    DuriseoTheme {
        SettingsScreen(
            state = SettingsUiState(
                account = SettingsAccount.Member(SocialProvider.GOOGLE),
                space = previewCouple(startDate = null),
                colorKeys = listOf("pick-orange", "pick-blue", "pick-purple"),
                today = PreviewToday,
            ),
            padding = PaddingValues(),
            onClose = {},
            onOpenAccount = {},
            onOpenColors = {},
            onOpenConnect = {},
            onOpenConnectionInfo = {},
            onAction = {},
        )
    }
}

/** [처음 만난 날 수정]으로 연 선택 시트 (`SCR-SET-01--start-date-sheet.png`) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsPreview_StartDateSheet() {
    DuriseoTheme {
        SettingsScreen(
            state = SettingsUiState(
                account = SettingsAccount.Member(SocialProvider.GOOGLE),
                space = previewCouple(LocalDate.of(2026, 10, 12)),
                colorKeys = listOf("pick-orange", "pick-blue", "pick-purple"),
                today = PreviewToday,
                startDateSheet = StartDateSheetState(
                    visibleMonth = YearMonth.of(2026, 10),
                    selected = LocalDate.of(2026, 10, 12),
                ),
            ),
            padding = PaddingValues(),
            onClose = {},
            onOpenAccount = {},
            onOpenColors = {},
            onOpenConnect = {},
            onOpenConnectionInfo = {},
            onAction = {},
        )
    }
}

/**
 * `SCR-SET-01--start-date-sheet-year-month.png`: 전체 메뉴 위의 처음 만난 날 시트에서 월 제목(⌃)을 눌러 연도·월 선택이 열린 모습.
 * 2026년 4월 선택, 오늘은 2026-04-30이라 5~12월은 비활성 (이전·다음 달 버튼은 숨겨진다)
 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StartDateSheetPreview_YearMonth() {
    val today = LocalDate.of(2026, 4, 30)
    DuriseoTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            SettingsScreen(
                state = SettingsUiState(
                    account = SettingsAccount.Member(SocialProvider.GOOGLE),
                    space = previewCouple(LocalDate.of(2026, 4, 7)),
                    colorKeys = listOf("pick-orange", "pick-blue", "pick-purple"),
                    today = today,
                ),
                padding = PaddingValues(),
                onClose = {},
                onOpenAccount = {},
                onOpenColors = {},
                onOpenConnect = {},
                onOpenConnectionInfo = {},
                onAction = {},
            )
            DuriseoBottomSheetLayer(onDismissRequest = null) {
                StartDatePickerSheet(
                    visibleMonth = YearMonth.of(2026, 4),
                    selectedDate = LocalDate.of(2026, 4, 7),
                    today = today,
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onDateSelected = {},
                    onSave = {},
                    onMonthSelected = {},
                    pickerInitiallyOpen = true,
                )
            }
        }
    }
}
