package com.duriseo.android.feature.couple.connect

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.DuriseoButton
import com.duriseo.android.core.designsystem.component.DuriseoButtonSize
import com.duriseo.android.core.designsystem.component.DuriseoButtonStyle
import com.duriseo.android.core.designsystem.component.DuriseoDialogLayer
import com.duriseo.android.core.designsystem.component.DuriseoSnackbar
import com.duriseo.android.core.designsystem.component.DuriseoSnackbarHost
import com.duriseo.android.core.designsystem.component.LogoutConfirmDialog
import com.duriseo.android.core.designsystem.theme.Brand500
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink300
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line200
import com.duriseo.android.core.designsystem.theme.OnBrand
import com.duriseo.android.core.designsystem.theme.PersonMe
import com.duriseo.android.core.designsystem.theme.PersonPartner
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.core.designsystem.theme.Surface100
import com.duriseo.android.core.designsystem.theme.Warning50
import com.duriseo.android.core.designsystem.theme.Warning800
import com.duriseo.android.feature.couple.R
import com.duriseo.android.feature.couple.component.CoupleHeader
import com.duriseo.android.feature.couple.component.CoupleTitle
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

// SCR-COUPLE-01 커플 연결 (연결 방법 선택) (FR-COUPLE-01, FR-AUTH-04 AC2, FR-AUTH-05 AC3).
// 수치는 보드 `Connect.dc.html`. 토큰에 없는 값이라 이름을 붙인 상수로 둔다 (unit-3.md 확인 필요 14).

/** 본문 위 여백 8, 사이 간격 28, 위쪽 묶음 간격 20 (23·24·26행) */
private val ContentTop = 8.dp
private val SectionGap = 28.dp
private val IntroGap = 20.dp

/** 떨어진 두 원 그림 164x56, 원 지름 52, 점선 (27~31행) */
private val GraphicWidth = 164.dp
private val GraphicHeight = 56.dp
private val GraphicCircleRadius = 26.dp
private val GraphicLeftCenterX = 28.dp
private val GraphicRightCenterX = 136.dp
private val GraphicDashStart = 62.dp
private val GraphicDashEnd = 102.dp
private val GraphicDashWidth = 2.5.dp
private val GraphicDashOn = 2.dp
private val GraphicDashOff = 8.dp

/** 큰 카드: 최소 높이 136, 안쪽 위아래 20·좌우 22, 모서리 24 (38행) */
private val CardMinHeight = 136.dp
private val CardPaddingVertical = 20.dp
private val CardPaddingHorizontal = 22.dp

/** 카드 아이콘 칸 48, 모서리 16 (40행) */
private val CardIconBoxSize = 48.dp

/** 카드 제목 20/700 (46행). `title-lg`(22)가 아니다 */
private val CardTitleSize = 20.sp
private val CardTitleLineHeight = 28.sp

/** 카드 부제 위 간격 4 (45행) */
private val CardTitleGap = 4.dp

/** 카드 오른쪽 화살표 22 (42행) */
private val CardChevronSize = 22.dp

/** 카드 안 흰 글자 부제는 0.88 불투명 (47행) */
private const val OnBrandSubtitleAlpha = 0.88f

/** 카드 아이콘 칸의 반투명 흰 면 0.18 (40행) */
private const val OnBrandTileAlpha = 0.18f

/** [혼자 시작하기] 행: 최소 높이 72, 좌우 18, 모서리 20, 원 40, 사이 14 (58행) */
private val SoloRowMinHeight = 72.dp
private val SoloRowPaddingHorizontal = 18.dp
private val SoloIconCircleSize = 40.dp
private val SoloRowGap = 14.dp
private val SoloChevronSize = 20.dp
private val SoloIconSize = 22.dp

/** 선 굵기 1 (카드 테두리) */
private val BorderWidth = 1.dp

/** 제목과 부제 사이 2, 안내 아이콘 위 1 (59·74행) */
private val SoloTextGap = 2.dp
private val GuestIconTop = 1.dp

/** 부제 13/400 행간 18. `label`이 13/18 600이라 굵기만 바꿔 쓴다 */
private val SoloSubtitleLineHeight = 18.sp

/** 게스트 안내 카드: 안쪽 18, 모서리 20, 아이콘 22, 사이 12 (73~75행) */
private val GuestCardPadding = 18.dp
private val GuestIconSize = 22.dp
private val GuestBodyLineHeight = 20.8.sp

/** 아래 "다른 계정으로 로그인": 안쪽 위 12·좌우 24·아래 28, 눌리는 영역 안쪽 12 (86~87행) */
private val OtherAccountBottom = 28.dp
private val OtherAccountPadding = 12.dp

/** 스낵바는 위에서 40 (SCREENS.md에 이 화면의 위치가 없어 시작 화면과 같게 둔다) */
private val SnackbarTop = 40.dp

@Composable
fun ConnectRoute(
    canGoBack: Boolean,
    padding: PaddingValues,
    onBack: () -> Unit,
    onInviteCreate: () -> Unit,
    onInviteEnter: () -> Unit,
    onHome: () -> Unit,
    onAccount: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConnectViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ConnectSideEffect.NavigateToInviteCreate -> onInviteCreate()
                ConnectSideEffect.NavigateToInviteEnter -> onInviteEnter()
                ConnectSideEffect.NavigateToHome -> onHome()
                ConnectSideEffect.NavigateToAccount -> onAccount()
                ConnectSideEffect.NavigateToStart -> onLoggedOut()
                is ConnectSideEffect.ShowMessage -> scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = context.resources.getString(effect.messageRes),
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    ConnectScreen(
        state = state,
        canGoBack = canGoBack,
        padding = padding,
        onAction = viewModel::onAction,
        onBack = onBack,
        snackbarHost = { DuriseoSnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    )
}

@Composable
fun ConnectScreen(
    state: ConnectUiState,
    canGoBack: Boolean,
    padding: PaddingValues,
    onAction: (ConnectAction) -> Unit,
    onBack: () -> Unit,
    snackbarHost: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    DuriseoDialogLayer(
        dialogVisible = state.logoutDialogVisible,
        dialog = {
            LogoutConfirmDialog(
                onCancel = { onAction(ConnectAction.LogoutDismissed) },
                onConfirm = { onAction(ConnectAction.LogoutConfirmed) },
                buttonsEnabled = !state.requesting,
            )
        },
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                CoupleHeader(showBack = canGoBack, onBack = onBack)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            top = ContentTop,
                            start = DuriseoSpacing.Space6,
                            end = DuriseoSpacing.Space6,
                        ),
                    verticalArrangement = Arrangement.spacedBy(SectionGap),
                ) {
                    Intro()
                    if (state.isGuest) {
                        GuestSection(onAction)
                    } else {
                        MemberSection(onAction)
                    }
                }
                if (!state.isGuest) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = DuriseoSpacing.Space3,
                                start = DuriseoSpacing.Space6,
                                end = DuriseoSpacing.Space6,
                                bottom = OtherAccountBottom,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.connect_other_account),
                            modifier = Modifier
                                .clickable(role = Role.Button) { onAction(ConnectAction.OtherAccountClicked) }
                                .padding(OtherAccountPadding),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Ink600,
                            textDecoration = TextDecoration.Underline,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(padding)
                    .padding(top = SnackbarTop),
            ) {
                snackbarHost()
            }
        }
    }
}

@Composable
private fun Intro() {
    Column(verticalArrangement = Arrangement.spacedBy(IntroGap)) {
        TwoCirclesGraphic()
        Column(verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2)) {
            CoupleTitle(stringResource(R.string.connect_title))
            Text(
                text = stringResource(R.string.connect_description),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink600,
            )
        }
    }
}

/** 떨어진 두 원과 점선 (`Connect.dc.html` 27~31행). 꾸밈이라 스크린리더는 읽지 않는다 */
@Composable
private fun TwoCirclesGraphic() {
    Canvas(
        modifier = Modifier
            .size(width = GraphicWidth, height = GraphicHeight)
            .clearAndSetSemantics { },
    ) {
        val radius = GraphicCircleRadius.toPx()
        val centerY = size.height / 2
        drawCircle(PersonMe, radius = radius, center = Offset(GraphicLeftCenterX.toPx(), centerY))
        drawCircle(PersonPartner, radius = radius, center = Offset(GraphicRightCenterX.toPx(), centerY))
        drawLine(
            color = Ink300,
            start = Offset(GraphicDashStart.toPx(), centerY),
            end = Offset(GraphicDashEnd.toPx(), centerY),
            strokeWidth = GraphicDashWidth.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(GraphicDashOn.toPx(), GraphicDashOff.toPx())),
        )
    }
}

@Composable
private fun MemberSection(onAction: (ConnectAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3)) {
        ConnectCard(
            title = stringResource(R.string.connect_make_code),
            subtitle = stringResource(R.string.connect_make_code_subtitle),
            iconRes = R.drawable.ic_ticket_plus,
            onClick = { onAction(ConnectAction.MakeCodeClicked) },
            container = Brand500,
            titleColor = OnBrand,
            subtitleColor = OnBrand.copy(alpha = OnBrandSubtitleAlpha),
            iconTint = OnBrand,
            iconTileColor = OnBrand.copy(alpha = OnBrandTileAlpha),
            chevronTint = OnBrand,
        )
        ConnectCard(
            title = stringResource(R.string.connect_enter_code),
            subtitle = stringResource(R.string.connect_enter_code_subtitle),
            iconRes = R.drawable.ic_keyboard,
            onClick = { onAction(ConnectAction.EnterCodeClicked) },
            container = Surface100,
            titleColor = Ink900,
            subtitleColor = Ink600,
            iconTint = Ink900,
            iconTileColor = Surface0,
            chevronTint = Ink600,
        )
        SoloRow(onClick = { onAction(ConnectAction.SoloClicked) })
    }
}

@Composable
private fun ConnectCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    onClick: () -> Unit,
    container: Color,
    titleColor: Color,
    subtitleColor: Color,
    iconTint: Color,
    iconTileColor: Color,
    chevronTint: Color,
) {
    val shape = MaterialTheme.shapes.extraLarge
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = CardMinHeight)
            .clip(shape)
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = CardPaddingHorizontal, vertical = CardPaddingVertical),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(CardIconBoxSize)
                    .background(iconTileColor, MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(DuriseoSize.Icon),
                    tint = iconTint,
                )
            }
            Icon(
                painter = painterResource(DesignR.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(CardChevronSize),
                tint = chevronTint,
            )
        }
        Column(
            modifier = Modifier.padding(top = DuriseoSpacing.Space4),
            verticalArrangement = Arrangement.spacedBy(CardTitleGap),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = CardTitleSize,
                    lineHeight = CardTitleLineHeight,
                ),
                color = titleColor,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor,
            )
        }
    }
}

@Composable
private fun SoloRow(onClick: () -> Unit) {
    val shape = DuriseoShapes.Xl
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SoloRowMinHeight)
            .clip(shape)
            .border(BorderWidth, Line200, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = SoloRowPaddingHorizontal),
        horizontalArrangement = Arrangement.spacedBy(SoloRowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(SoloIconCircleSize)
                .background(Surface100, DuriseoShapes.Full),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_user),
                contentDescription = null,
                modifier = Modifier.size(SoloIconSize),
                tint = Ink900,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = DuriseoSpacing.Space2),
            verticalArrangement = Arrangement.spacedBy(SoloTextGap),
        ) {
            Text(
                text = stringResource(R.string.connect_solo_start),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = Ink900,
            )
            Text(
                text = stringResource(R.string.connect_solo_start_hint),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Normal,
                    lineHeight = SoloSubtitleLineHeight,
                ),
                color = Ink600,
            )
        }
        Icon(
            painter = painterResource(DesignR.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(SoloChevronSize),
            tint = Ink600,
        )
    }
}

@Composable
private fun GuestSection(onAction: (ConnectAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Warning50, DuriseoShapes.Xl)
                .padding(GuestCardPadding),
            horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_info),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = GuestIconTop)
                    .size(GuestIconSize),
                tint = Warning800,
            )
            Column(verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space1)) {
                Text(
                    text = stringResource(R.string.connect_guest_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleSmall,
                    color = Warning800,
                )
                Text(
                    text = stringResource(R.string.connect_guest_body),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Normal,
                        lineHeight = GuestBodyLineHeight,
                    ),
                    color = Warning800,
                )
            }
        }
        DuriseoButton(
            text = stringResource(R.string.connect_guest_login),
            onClick = { onAction(ConnectAction.GuestLoginClicked) },
            modifier = Modifier.fillMaxWidth(),
            style = DuriseoButtonStyle.Filled,
            size = DuriseoButtonSize.Social,
        )
        DuriseoButton(
            text = stringResource(R.string.connect_guest_solo),
            onClick = { onAction(ConnectAction.SoloClicked) },
            modifier = Modifier.fillMaxWidth(),
            style = DuriseoButtonStyle.Outline,
            size = DuriseoButtonSize.Medium,
        )
    }
}

@Composable
private fun PreviewConnectScreen(
    state: ConnectUiState = ConnectUiState(),
    canGoBack: Boolean = true,
) {
    DuriseoTheme {
        ConnectScreen(
            state = state,
            canGoBack = canGoBack,
            padding = PaddingValues(),
            onAction = {},
            onBack = {},
            snackbarHost = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ConnectScreenMemberPreview() = PreviewConnectScreen()

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ConnectScreenGuestPreview() = PreviewConnectScreen(ConnectUiState(isGuest = true))

/** 앞 화면이 없는 진입(로그인 직후, 연결 해제 뒤): 뒤로 가기 버튼이 없다 (unit-3.md 확인 필요 4) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ConnectScreenNoBackPreview() = PreviewConnectScreen(canGoBack = false)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ConnectScreenLogoutDialogPreview() =
    PreviewConnectScreen(ConnectUiState(logoutDialogVisible = true))

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ConnectScreenLoggingOutPreview() =
    PreviewConnectScreen(ConnectUiState(logoutDialogVisible = true, requesting = true))

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ConnectScreenOfflinePreview() {
    DuriseoTheme {
        ConnectScreen(
            state = ConnectUiState(),
            canGoBack = true,
            padding = PaddingValues(),
            onAction = {},
            onBack = {},
            snackbarHost = { DuriseoSnackbar(message = stringResource(DesignR.string.offline_required)) },
        )
    }
}
