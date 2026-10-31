package com.duriseo.android.feature.settings.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.common.social.SocialTokenResult
import com.duriseo.android.core.common.social.findActivity
import com.duriseo.android.core.common.social.socialTokenProvider
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.DuriseoButton
import com.duriseo.android.core.designsystem.component.DuriseoButtonSize
import com.duriseo.android.core.designsystem.component.DuriseoButtonStyle
import com.duriseo.android.core.designsystem.component.DuriseoDialogLayer
import com.duriseo.android.core.designsystem.component.DuriseoGuestBanner
import com.duriseo.android.core.designsystem.component.DuriseoSectionTitle
import com.duriseo.android.core.designsystem.component.DuriseoTopAppBar
import com.duriseo.android.core.designsystem.theme.Brand700
import com.duriseo.android.core.designsystem.theme.Danger700
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.feature.settings.R
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// SCR-SET-01 하위 화면 "계정" (게스트 · 회원). 수치는 design-system.md "정보 행 (계정 화면)", "로그인 카드", "게스트 안내 배너"와
// 보드 `Account.dc.html`. 토큰에 없는 수치는 이름 붙인 상수로 둔다.

/** 앱바 아래 위 여백 8, 아래 28 (26행) */
private val ContentTop = 8.dp
private val ContentBottom = 28.dp

/** 덩어리 사이 간격 24 (26행, `space-6`) */
private val BlockGap = DuriseoSpacing.Space6

/** 로그인 연결 묶음 안 간격 12 (37행) */
private val LoginGroupGap = DuriseoSpacing.Space3

/**
 * 안내 문구 위로 4 더. 출처: 캔버스 원본 `../Docs/design/source/Account.dc.html` 41행(`margin: 4px 0 0`).
 * design-system 명세에는 아직 없다 (Docs 후속, unit-2.md).
 */
private val NoteTopGap = 4.dp

/**
 * 작은 글 12 / 400, 행간 1.6 = 19.2. 출처: `../Docs/design/source/Account.dc.html` 41행(`font-size: 12px; line-height: 1.6`).
 * `caption`은 12/500/16이라 따로 쓴다. design-system 명세에는 아직 없다 (Docs 후속, unit-2.md).
 */
private val SmallLineHeight = 19.2.sp

/** 게스트 ID 영역: 위 선 1 아래 안쪽 위 12, 라벨과 값 사이 4 (44~46행) */
private val GuestIdTopPadding = DuriseoSpacing.Space3

/** [탈퇴하기] 높이 44 (`size-button-sm`), 13 / 500 `ink-500` (47·70행) */
private val WithdrawHeight = DuriseoSize.ButtonSm

/** [로그아웃] 높이 52, 15 / 600 `danger-700` (69행) */
private val LogoutHeight = 52.dp

/** 정보 행 높이 52 (65·66행) */
private val InfoRowHeight = 52.dp

/** 로그인 카드: 안쪽 20, 안쪽 요소 사이 14, 체크 원 지름 44(`size-avatar-lg`), 체크 아이콘 22, 제목 아래 2 (55~61행) */
private val CardPadding = 20.dp
private val CardGap = 14.dp
private val CheckIconSize = 22.dp
private val CardTitleGap = 2.dp

/** 연동 직후 상태 문구: 안쪽 위아래 12·좌우 14, 13 행간 1.6 = 20.8 (53행) */
private val StatusVerticalPadding = 12.dp
private val StatusHorizontalPadding = 14.dp
private val StatusLineHeight = 20.8.sp

@Composable
fun AccountRoute(
    padding: PaddingValues,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onSwitched: () -> Unit,
    onShowSnackBar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val socialTokenProvider = remember(context) { context.socialTokenProvider() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                // 소셜 SDK는 Route가 현재 Activity로 연다. ViewModel은 결과만 Action으로 받는다
                is AccountSideEffect.RequestSocialToken -> scope.launch {
                    val result = try {
                        val activity = context.findActivity()
                        if (activity == null) {
                            SocialTokenResult.Failed()
                        } else {
                            socialTokenProvider.requestToken(activity, effect.provider)
                        }
                    } catch (e: CancellationException) {
                        // 화면이 사라져도 요청 중 상태가 남지 않게 취소로 돌려보낸다
                        viewModel.onAction(AccountAction.SocialTokenReceived(effect.provider, SocialTokenResult.Cancelled))
                        throw e
                    }
                    viewModel.onAction(AccountAction.SocialTokenReceived(effect.provider, result))
                }
                is AccountSideEffect.ShowMessage -> onShowSnackBar(effect.messageRes)
                AccountSideEffect.NavigateToStart -> onLoggedOut()
                AccountSideEffect.NavigateToHome -> onSwitched()
            }
        }
    }

    AccountScreen(
        state = state,
        padding = padding,
        onBack = onBack,
        onAction = viewModel::onAction,
        onGuestIdLongClick = { guestId -> context.copyToClipboard(guestId) },
        modifier = modifier,
    )
}

/** 게스트 ID 복사. 복사했다는 안내는 시스템이 보여 주고 스낵바는 따로 띄우지 않는다 (SCREENS.md "게스트 ID (Q-12)") */
private fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("", text))
}

@Composable
fun AccountScreen(
    state: AccountUiState,
    padding: PaddingValues,
    onBack: () -> Unit,
    onAction: (AccountAction) -> Unit,
    onGuestIdLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    DuriseoDialogLayer(
        dialogVisible = state.dialog != null,
        dialog = { AccountDialogs(state = state, onAction = onAction) },
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            DuriseoTopAppBar(
                title = stringResource(R.string.account_title),
                onBack = onBack,
            )
            when (val content = state.content) {
                null -> Unit
                is AccountContent.Guest -> GuestContent(
                    content = content,
                    requesting = state.requesting,
                    onAction = onAction,
                    onGuestIdLongClick = onGuestIdLongClick,
                    modifier = Modifier.weight(1f),
                )
                is AccountContent.Member -> MemberContent(
                    content = content,
                    justLinked = state.justLinked,
                    requesting = state.requesting,
                    onAction = onAction,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * 본문 틀: 위 덩어리 아래에 바닥 덩어리를 붙인다 (원본의 `margin-top: auto`).
 * 글자를 크게 키우거나 화면이 낮아도 잘리지 않게 세로로 스크롤한다. 스크롤 안에서는 weight를 쓸 수 없어
 * 화면 높이를 최소 높이로 잡고 SpaceBetween으로 벌린다. 두 덩어리 사이에는 최소 [BlockGap]을 둔다.
 */
@Composable
private fun AccountBody(
    modifier: Modifier = Modifier,
    top: @Composable ColumnScope.() -> Unit,
    bottom: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val minHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minHeight)
                .padding(
                    start = DuriseoSpacing.Space5,
                    end = DuriseoSpacing.Space5,
                    top = ContentTop,
                    bottom = ContentBottom,
                ),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.padding(bottom = BlockGap),
                verticalArrangement = Arrangement.spacedBy(BlockGap),
                content = top,
            )
            Column(content = bottom)
        }
    }
}

@Composable
private fun GuestContent(
    content: AccountContent.Guest,
    requesting: Boolean,
    onAction: (AccountAction) -> Unit,
    onGuestIdLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountBody(
        modifier = modifier,
        top = {
            DuriseoGuestBanner(
                title = stringResource(R.string.account_guest_banner_title),
                body = stringResource(R.string.account_guest_banner_body),
            )

            Column(verticalArrangement = Arrangement.spacedBy(LoginGroupGap)) {
                DuriseoSectionTitle(
                    text = stringResource(R.string.account_section_login),
                    color = Ink600,
                )
                DuriseoButton(
                    text = stringResource(R.string.account_link_google),
                    onClick = { onAction(AccountAction.LinkClicked(SocialProvider.GOOGLE)) },
                    modifier = Modifier.fillMaxWidth(),
                    style = DuriseoButtonStyle.Filled,
                    size = DuriseoButtonSize.Social,
                    enabled = !requesting,
                )
                DuriseoButton(
                    text = stringResource(R.string.account_link_kakao),
                    onClick = { onAction(AccountAction.LinkClicked(SocialProvider.KAKAO)) },
                    modifier = Modifier.fillMaxWidth(),
                    style = DuriseoButtonStyle.Outline,
                    size = DuriseoButtonSize.Social,
                    enabled = !requesting,
                )
                Text(
                    text = stringResource(R.string.account_existing_note),
                    modifier = Modifier.padding(top = NoteTopGap),
                    style = SmallTextStyle(),
                    color = Ink600,
                )
            }
        },
        bottom = {
            Column(verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space1)) {
                HorizontalDivider(color = Line100)
                GuestId(
                    guestId = content.guestId,
                    onCopy = onGuestIdLongClick,
                    modifier = Modifier.padding(top = GuestIdTopPadding - DuriseoSpacing.Space1),
                )
                WithdrawButton(
                    enabled = !requesting,
                    onClick = { onAction(AccountAction.WithdrawClicked) },
                )
            }
        },
    )
}

/**
 * 게스트 ID: 값은 `Me.id` 그대로, 한 줄에 안 들어가면 줄바꿈하고, 길게 누르면 복사한다 (Q-12).
 * 짧게 눌러서는 아무 일도 없으므로 클릭 동작(`combinedClickable`)은 달지 않고, 길게 누르기만 접근성 동작으로 알린다.
 * 동작 이름 "복사"는 SCREENS.md에 없어 strings.xml에 두었다 (Docs 후속, unit-2.md).
 */
@Composable
private fun GuestId(
    guestId: String,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val copyLabel = stringResource(R.string.account_guest_id_copy_label)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(guestId) {
                detectTapGestures(onLongPress = { onCopy(guestId) })
            }
            .semantics {
                onLongClick(label = copyLabel) {
                    onCopy(guestId)
                    true
                }
            },
        verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space1),
    ) {
        Text(
            text = stringResource(R.string.account_guest_id_label),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
            color = Ink500,
        )
        Text(
            text = guestId,
            style = MaterialTheme.typography.labelLarge,
            color = Ink600,
        )
    }
}

@Composable
private fun MemberContent(
    content: AccountContent.Member,
    justLinked: Boolean,
    requesting: Boolean,
    onAction: (AccountAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountBody(
        modifier = modifier,
        top = {
            if (justLinked) {
                Text(
                    text = stringResource(R.string.account_just_linked),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                        .padding(horizontal = StatusHorizontalPadding, vertical = StatusVerticalPadding)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Normal,
                        lineHeight = StatusLineHeight,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            content.provider?.let { LoginCard(provider = it, nickname = content.nickname) }

            Column {
                content.provider?.let { provider ->
                    InfoRow(
                        label = stringResource(R.string.account_method_label),
                        value = providerName(provider),
                        showDivider = true,
                    )
                }
                InfoRow(
                    label = stringResource(R.string.account_joined_label),
                    value = joinedDateText(content.joinedDate),
                    showDivider = false,
                )
            }
        },
        bottom = {
            HorizontalDivider(color = Line100)
            LogoutButton(
                enabled = !requesting,
                onClick = { onAction(AccountAction.LogoutClicked) },
            )
            WithdrawButton(
                enabled = !requesting,
                onClick = { onAction(AccountAction.WithdrawClicked) },
            )
        },
    )
}

/** 로그인 카드: 체크 원 + "Google로 로그인 중" / "카카오로 로그인 중" + 닉네임 */
@Composable
private fun LoginCard(provider: SocialProvider, nickname: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, DuriseoShapes.Xl)
            .padding(CardPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        Box(
            modifier = Modifier
                .size(DuriseoSize.AvatarLg)
                .background(Surface0, DuriseoShapes.Full),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(CheckIconSize),
                tint = Brand700,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(CardTitleGap),
        ) {
            Text(
                text = stringResource(
                    when (provider) {
                        SocialProvider.GOOGLE -> R.string.account_signed_in_google
                        SocialProvider.KAKAO -> R.string.account_signed_in_kakao
                    },
                ),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = nickname,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = Ink600,
            )
        }
    }
}

/** 정보 행: 높이 52, 라벨 14 `ink-600` 왼쪽, 값 15 / 600 `ink-900` 오른쪽. 첫 행 아래에 선 1 (`line-100`) */
@Composable
private fun InfoRow(label: String, value: String, showDivider: Boolean) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(InfoRowHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Ink600,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (showDivider) HorizontalDivider(color = Line100)
    }
}

@Composable
private fun LogoutButton(enabled: Boolean, onClick: () -> Unit) {
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

@Composable
private fun WithdrawButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(WithdrawHeight)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = stringResource(R.string.account_withdraw),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = Ink500,
        )
    }
}

@Composable
private fun SmallTextStyle() = MaterialTheme.typography.labelMedium.copy(
    fontWeight = FontWeight.Normal,
    lineHeight = SmallLineHeight,
)

@Composable
private fun providerName(provider: SocialProvider): String = stringResource(
    when (provider) {
        SocialProvider.GOOGLE -> R.string.account_method_google
        SocialProvider.KAKAO -> R.string.account_method_kakao
    },
)

/** "2026년 10월 12일": 연·월·일, 요일 없음, 앞자리 0 없음 (SCREENS.md "가입일 형식") */
@Composable
private fun joinedDateText(date: LocalDate): String =
    stringResource(R.string.account_joined_date, date.year, date.monthValue, date.dayOfMonth)

// ---- Preview: 상태마다 하나씩 ----

private const val SampleGuestId = "5b0c7e2a-1f3d-4c8a-9e61-7a2d4b9f3c10"
private val SampleJoined = LocalDate.of(2026, 10, 12)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AccountScreenGuestPreview() {
    DuriseoTheme {
        AccountScreen(
            state = AccountUiState(content = AccountContent.Guest(SampleGuestId)),
            padding = PaddingValues(),
            onBack = {},
            onAction = {},
            onGuestIdLongClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AccountScreenGuestRequestingPreview() {
    DuriseoTheme {
        AccountScreen(
            state = AccountUiState(content = AccountContent.Guest(SampleGuestId), requesting = true),
            padding = PaddingValues(),
            onBack = {},
            onAction = {},
            onGuestIdLongClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AccountScreenAlreadyUsedDialogPreview() {
    DuriseoTheme {
        AccountScreen(
            state = AccountUiState(
                content = AccountContent.Guest(SampleGuestId),
                dialog = AccountDialog.AlreadyUsed,
            ),
            padding = PaddingValues(),
            onBack = {},
            onAction = {},
            onGuestIdLongClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AccountScreenSwitchConfirmDialogPreview() {
    DuriseoTheme {
        AccountScreen(
            state = AccountUiState(
                content = AccountContent.Guest(SampleGuestId),
                dialog = AccountDialog.SwitchConfirm,
            ),
            padding = PaddingValues(),
            onBack = {},
            onAction = {},
            onGuestIdLongClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AccountScreenMemberGooglePreview() {
    DuriseoTheme {
        AccountScreen(
            state = AccountUiState(content = AccountContent.Member(SocialProvider.GOOGLE, "닉네임", SampleJoined)),
            padding = PaddingValues(),
            onBack = {},
            onAction = {},
            onGuestIdLongClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AccountScreenMemberKakaoPreview() {
    DuriseoTheme {
        AccountScreen(
            state = AccountUiState(content = AccountContent.Member(SocialProvider.KAKAO, "닉네임", SampleJoined)),
            padding = PaddingValues(),
            onBack = {},
            onAction = {},
            onGuestIdLongClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AccountScreenJustLinkedPreview() {
    DuriseoTheme {
        AccountScreen(
            state = AccountUiState(
                content = AccountContent.Member(SocialProvider.GOOGLE, "닉네임", SampleJoined),
                justLinked = true,
            ),
            padding = PaddingValues(),
            onBack = {},
            onAction = {},
            onGuestIdLongClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AccountScreenLogoutDialogPreview() {
    DuriseoTheme {
        AccountScreen(
            state = AccountUiState(
                content = AccountContent.Member(SocialProvider.GOOGLE, "닉네임", SampleJoined),
                dialog = AccountDialog.Logout,
            ),
            padding = PaddingValues(),
            onBack = {},
            onAction = {},
            onGuestIdLongClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AccountScreenWithdrawDialogCouplePreview() {
    DuriseoTheme {
        AccountScreen(
            state = AccountUiState(
                content = AccountContent.Member(SocialProvider.GOOGLE, "닉네임", SampleJoined),
                withdrawMessage = WithdrawMessage.Couple(partnerNickname = "상대"),
                dialog = AccountDialog.Withdraw,
            ),
            padding = PaddingValues(),
            onBack = {},
            onAction = {},
            onGuestIdLongClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AccountScreenWithdrawDialogPersonalPreview() {
    DuriseoTheme {
        AccountScreen(
            state = AccountUiState(
                content = AccountContent.Guest(SampleGuestId),
                withdrawMessage = WithdrawMessage.Personal,
                dialog = AccountDialog.Withdraw,
            ),
            padding = PaddingValues(),
            onBack = {},
            onAction = {},
            onGuestIdLongClick = {},
        )
    }
}
