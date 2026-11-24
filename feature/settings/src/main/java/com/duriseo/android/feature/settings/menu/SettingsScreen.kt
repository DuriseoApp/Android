package com.duriseo.android.feature.settings.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import com.duriseo.android.core.common.legal.LegalLinks
import com.duriseo.android.core.common.legal.openExternalUrl
import android.os.Build
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.DuriseoButton
import com.duriseo.android.core.designsystem.component.DuriseoBottomSheetLayer
import com.duriseo.android.core.designsystem.component.DuriseoButtonStyle
import com.duriseo.android.core.designsystem.component.DuriseoCloseTopAppBar
import com.duriseo.android.core.designsystem.component.DuriseoDialog
import com.duriseo.android.core.designsystem.component.DuriseoDialogLayer
import com.duriseo.android.core.designsystem.component.DuriseoGuestBanner
import com.duriseo.android.core.designsystem.component.DuriseoInfoRow
import com.duriseo.android.core.designsystem.component.DuriseoMenuRow
import com.duriseo.android.core.designsystem.component.DuriseoSectionTitle
import com.duriseo.android.core.designsystem.component.LogoutConfirmDialog
import com.duriseo.android.core.designsystem.component.StartDatePickerSheet
import com.duriseo.android.core.designsystem.theme.Brand100
import com.duriseo.android.core.designsystem.theme.Brand700
import com.duriseo.android.core.designsystem.theme.Danger700
import com.duriseo.android.core.designsystem.theme.DuriseoPickPalette
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.Surface100
import com.duriseo.android.core.designsystem.theme.Surface50
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.feature.settings.R
import com.duriseo.android.feature.settings.appupdate.AppUpdateStatus
import com.duriseo.android.feature.settings.appupdate.AppUpdateUiState
import com.duriseo.android.feature.settings.appupdate.AppUpdateViewModel
import com.duriseo.android.feature.settings.appupdate.findActivity
import java.time.LocalDate
import java.time.YearMonth
import com.duriseo.android.domain.anniversary.DayCount

// SCR-SET-01 전체 메뉴 (FR-SET-01, FR-SET-02, FR-AUTH-04, FR-AUTH-05, FR-COUPLE-04). 앱바, 게스트 배너, 우리 프로필 카드,
// 꾸미기 › 우리 색상, "설정" 섹션(알림 설정·연결 정보·계정), "정보" 섹션(문의하기·이용약관·개인정보처리방침·오픈소스 라이선스),
// 로그아웃, 맨 아래 앱 정보 카드 (단위 8: FR-SET-02 AC1~AC4, D-150·D-151).
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

/** 앱 정보 카드 (보드 144행): 바깥 위 8·좌우 20·아래 28, 안쪽 위아래 20·좌우 16, 모서리 20, 줄 사이 6 */
private val AppCardTop = DuriseoSpacing.Space2
private val AppCardBottom = 28.dp
private val AppCardPaddingVertical = DuriseoSpacing.Space5
private val AppCardPaddingHorizontal = DuriseoSpacing.Space4
private val AppCardGap = 6.dp

/** 카드 로고 36 x 25 (145행) */
private val AppLogoWidth = 36.dp
private val AppLogoHeight = 25.dp

/** 최신 줄: 체크 14, 체크와 글자 사이 4 (148행) */
private val LatestIconSize = 14.dp
private val LatestGap = DuriseoSpacing.Space1

/** [업데이트] 알약: 위 4 더, 높이 36, 좌우 14, 아이콘 16과 글자 사이 6 (157행) */
private val UpdateTop = DuriseoSpacing.Space1
private val UpdateHeight = 36.dp
private val UpdatePaddingHorizontal = 14.dp
private val UpdateIconSize = 16.dp
private val UpdateGap = 6.dp

@Composable
fun SettingsRoute(
    padding: PaddingValues,
    onClose: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenColors: () -> Unit,
    onOpenConnect: () -> Unit,
    onOpenConnectionInfo: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenProfileEdit: () -> Unit,
    onOpenLicenses: () -> Unit,
    onLoggedOut: () -> Unit,
    onShowSnackBar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    appUpdateViewModel: AppUpdateViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val appUpdate by appUpdateViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsSideEffect.ShowMessage -> onShowSnackBar(effect.messageRes)
                SettingsSideEffect.NavigateToProfileEdit -> onOpenProfileEdit()
                SettingsSideEffect.NavigateToLicenses -> onOpenLicenses()
                // 튜토리얼 다시 보기: 전체 메뉴를 닫고 캘린더로 (FR-SET-02 AC5)
                SettingsSideEffect.CloseForTutorial -> onClose()
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
        appUpdate = appUpdate,
        // 메일 앱이 없으면(열 수 있는 앱이 없으면) 주소 다이얼로그 (D-150)
        onContact = {
            if (!context.openSupportMail(appUpdate.versionName)) viewModel.onAction(SettingsAction.MailAppMissing)
        },
        // "이용약관·개인정보처리방침" 한 행은 개인정보처리방침 주소를 연다 (D-154)
        onOpenTerms = { context.openExternalUrl(LegalLinks.SETTINGS_ROW_URL) },
        onCopyMailAddress = { address -> context.copyToClipboard(address) },
        onUpdate = { context.findActivity()?.let(appUpdateViewModel::onUpdateClicked) },
    )
}

/**
 * 문의하기: 메일 앱을 연다. `ACTION_SENDTO` `mailto:`에 받는 사람(D-155)과 제목·본문을 채운다.
 * 열 수 있는 앱이 없으면 false.
 */
private fun Context.openSupportMail(appVersion: String): Boolean {
    val mail = supportMail(
        subject = getString(R.string.settings_mail_subject),
        appVersionLine = getString(R.string.settings_mail_body_app_version, appVersion),
        androidVersionLine = getString(R.string.settings_mail_body_android_version, Build.VERSION.RELEASE),
        deviceModelLine = getString(R.string.settings_mail_body_device_model, deviceModelLabel(Build.MANUFACTURER, Build.MODEL)),
    )
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(supportMailUri(getString(R.string.settings_mail_address)))).apply {
        putExtra(Intent.EXTRA_SUBJECT, mail.subject)
        putExtra(Intent.EXTRA_TEXT, mail.body)
    }
    return try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

/** 문의 메일 주소 복사. 복사했다는 안내는 시스템이 보여 주고 스낵바는 따로 띄우지 않는다 (SCREENS.md "문의하기") */
private fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.settings_mail_copy_label), text))
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
    appUpdate: AppUpdateUiState = AppUpdateUiState(versionName = "1.0.0", versionCode = 100, status = AppUpdateStatus.UP_TO_DATE),
    onContact: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onCopyMailAddress: (String) -> Unit = {},
    onUpdate: () -> Unit = {},
) {
    val sheet = state.startDateSheet
    DuriseoDialogLayer(
        dialogVisible = state.logoutDialogVisible || state.mailDialogVisible || sheet != null,
        dialog = {
            if (sheet != null) {
                // 처음 만난 날 선택 시트 (D-74). 저장 요청 중에는 뒤 막과 뒤로 가기로 닫히지 않는다
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
            } else if (state.mailDialogVisible) {
                MailMissingDialog(
                    onClose = { onAction(SettingsAction.MailDialogDismissed) },
                    onCopy = { address ->
                        onCopyMailAddress(address)
                        onAction(SettingsAction.MailDialogDismissed)
                    },
                )
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
                // 앱 정보 카드는 메뉴 목록 맨 끝에서 함께 스크롤된다. 메뉴가 화면보다 짧으면 화면 맨 아래에 붙는다 (D-170)
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val viewportHeight = maxHeight
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .heightIn(min = viewportHeight),
                    ) {
                        Column(
                            modifier = Modifier
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
                                    onEditProfile = { onAction(SettingsAction.ProfileEditClicked) },
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

                            // 정보 (FR-SET-02 AC1~AC3, 보드 130~134행): 앞 아이콘 없는 높이 52 행
                            Column {
                                DuriseoSectionTitle(text = stringResource(R.string.settings_section_info))
                                // 첫 행: 다른 정보 행과 같은 모양 (FR-SET-02 AC5, D-169)
                                DuriseoInfoRow(
                                    label = stringResource(R.string.settings_tutorial_replay),
                                    onClick = { onAction(SettingsAction.TutorialReplayClicked) },
                                )
                                DuriseoInfoRow(label = stringResource(R.string.settings_contact), onClick = onContact)
                                DuriseoInfoRow(label = stringResource(R.string.settings_terms), onClick = onOpenTerms)
                                DuriseoInfoRow(
                                    label = stringResource(R.string.settings_licenses),
                                    onClick = { onAction(SettingsAction.LicensesClicked) },
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
                        Spacer(modifier = Modifier.weight(1f))
                        AppInfoCard(state = appUpdate, onUpdate = onUpdate)
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

/**
 * 메일 앱 없음 다이얼로그 (D-150, design-system.md "메일 앱 없음 다이얼로그"). 주소 상자: 본문 아래 4, 면 `surface-100`,
 * 모서리 12, 안쪽 12/16, 글자 15/24/600 `ink-900`. [닫기] [복사](`brand-500`).
 */
@Composable
private fun MailMissingDialog(onClose: () -> Unit, onCopy: (String) -> Unit) {
    val address = stringResource(R.string.settings_mail_address)
    DuriseoDialog(
        title = stringResource(R.string.settings_mail_dialog_title),
        body = stringResource(R.string.settings_mail_dialog_body),
        cancelText = stringResource(DesignR.string.nav_close),
        confirmText = stringResource(R.string.settings_mail_dialog_copy),
        onCancel = onClose,
        onConfirm = { onCopy(address) },
        extra = {
            Text(
                text = address,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface100, MaterialTheme.shapes.small)
                    .padding(horizontal = DuriseoSpacing.Space4, vertical = DuriseoSpacing.Space3),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
    )
}

/**
 * 앱 정보 카드 (FR-SET-02 AC4, D-151, 보드 `Menu.dc.html` 144~165행). 면 `surface-50`, 모서리 20, 가운데 정렬.
 * 로고 · "둘이서" 14/700 · "버전 N (코드)" 13 `ink-600` · 최신 여부 줄 또는 [업데이트].
 * 확인 중·받는 중은 최신 여부 줄을 안 보이게만 해 높이를 유지하고(`SCR-SET-01--update-checking.png`),
 * 확인할 수 없음은 줄을 빼서 카드가 짧아진다(`--update-unknown.png`).
 */
@Composable
private fun AppInfoCard(state: AppUpdateUiState, onUpdate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = DuriseoSpacing.Space5, end = DuriseoSpacing.Space5, top = AppCardTop, bottom = AppCardBottom)
            .background(Surface50, DuriseoShapes.Xl)
            .padding(horizontal = AppCardPaddingHorizontal, vertical = AppCardPaddingVertical),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppCardGap),
    ) {
        Image(
            painter = painterResource(DesignR.drawable.logo_mark_card),
            contentDescription = null,
            modifier = Modifier.size(width = AppLogoWidth, height = AppLogoHeight),
        )
        Text(
            text = stringResource(R.string.settings_app_info_name),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.settings_app_info_version, state.versionName, state.versionCode),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
            color = Ink600,
        )
        if (state.latestLineVisible || state.latestLineReserved) {
            LatestLine(
                modifier = if (state.latestLineVisible) Modifier else Modifier.alpha(0f).clearAndSetSemantics { },
            )
        }
        if (state.updateButtonVisible) UpdateButton(onClick = onUpdate)
    }
}

/** "최신 버전을 사용하고 있어요": 체크 14 + 12/500 `ink-600` */
@Composable
private fun LatestLine(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LatestGap),
    ) {
        Icon(
            painter = painterResource(DesignR.drawable.ic_check),
            contentDescription = null,
            modifier = Modifier.size(LatestIconSize),
            tint = Ink600,
        )
        Text(
            text = stringResource(R.string.settings_app_info_latest),
            style = MaterialTheme.typography.labelMedium,
            color = Ink600,
        )
    }
}

/** [업데이트]: 알약(높이 36, 좌우 14), 면 `brand-100`, 글자·아이콘 `brand-700` 13/700 */
@Composable
private fun UpdateButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(top = UpdateTop)
            .height(UpdateHeight)
            .background(Brand100, DuriseoShapes.Full)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = UpdatePaddingHorizontal),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UpdateGap),
    ) {
        Icon(
            painter = painterResource(DesignR.drawable.ic_download),
            contentDescription = null,
            modifier = Modifier.size(UpdateIconSize),
            tint = Brand700,
        )
        Text(
            text = stringResource(R.string.settings_app_info_update),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Brand700,
        )
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
private fun SettingsPreview_Couple() {
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
private fun SettingsPreview_Solo() {
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
private fun SettingsPreview_Guest() {
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

private fun previewCoupleState(mailDialogVisible: Boolean = false) = SettingsUiState(
    account = SettingsAccount.Member(SocialProvider.GOOGLE),
    space = previewCouple(LocalDate.of(2026, 10, 12)),
    colorKeys = listOf("pick-orange", "pick-blue", "pick-purple"),
    today = PreviewToday,
    mailDialogVisible = mailDialogVisible,
)

@Composable
private fun PreviewSettings(state: SettingsUiState, appUpdate: AppUpdateUiState) {
    DuriseoTheme {
        SettingsScreen(
            state = state,
            padding = PaddingValues(),
            onClose = {},
            onOpenAccount = {},
            onOpenColors = {},
            onOpenConnect = {},
            onOpenConnectionInfo = {},
            onAction = {},
            appUpdate = appUpdate,
        )
    }
}

/** `SCR-SET-01--mail-dialog.png`: 문의하기를 눌렀는데 메일 앱이 없을 때 (D-150) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsPreview_MailDialog() = PreviewSettings(
    state = previewCoupleState(mailDialogVisible = true),
    appUpdate = AppUpdateUiState("1.0.0", 100, AppUpdateStatus.UP_TO_DATE),
)

/** `SCR-SET-01--update-checking.png`: 확인 중. 최신 여부 줄만 안 보이고 카드 높이는 그대로 */
@Preview(showBackground = true, widthDp = 390, heightDp = 1080)
@Composable
private fun SettingsPreview_UpdateChecking() = PreviewSettings(
    state = previewCoupleState(),
    appUpdate = AppUpdateUiState("1.0.0", 100, AppUpdateStatus.CHECKING),
)

/** `SCR-SET-01--update-unknown.png`: 확인할 수 없음. 최신 여부 줄을 빼서 카드가 짧다 */
@Preview(showBackground = true, widthDp = 390, heightDp = 1080)
@Composable
private fun SettingsPreview_UpdateUnknown() = PreviewSettings(
    state = previewCoupleState(),
    appUpdate = AppUpdateUiState("1.0.0", 100, AppUpdateStatus.UNKNOWN),
)

/** 새 버전이 있을 때: [업데이트] (`Menu.dc.html` 속성 `updateAvailable`) */
@Preview(showBackground = true, widthDp = 390, heightDp = 1080)
@Composable
private fun SettingsPreview_UpdateAvailable() = PreviewSettings(
    state = previewCoupleState(),
    appUpdate = AppUpdateUiState("1.0.0", 100, AppUpdateStatus.UPDATE_AVAILABLE),
)
