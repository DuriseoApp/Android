package com.duriseo.android.feature.auth.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.common.Logger
import com.duriseo.android.core.common.legal.LegalLinks
import com.duriseo.android.core.common.legal.openExternalUrl
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.DuriseoButton
import com.duriseo.android.core.designsystem.component.DuriseoButtonSize
import com.duriseo.android.core.designsystem.component.DuriseoButtonStyle
import com.duriseo.android.core.common.social.SOCIAL_LOG_TAG
import com.duriseo.android.core.common.social.SocialTokenResult
import com.duriseo.android.core.common.social.findActivity
import com.duriseo.android.core.common.social.socialTokenProvider
import com.duriseo.android.core.designsystem.component.DuriseoDialog
import com.duriseo.android.core.designsystem.component.DuriseoDialogLayer
import com.duriseo.android.core.designsystem.component.DuriseoSnackbar
import com.duriseo.android.core.designsystem.component.DuriseoSnackbarHost
import com.duriseo.android.core.designsystem.theme.Brand500
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.OnBrand
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.feature.auth.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// SCR-START-01 시작 (FR-AUTH-01, FR-AUTH-02, FR-AUTH-08). 수치는 design-system.md "시작 화면"과 보드 `Main.dc.html`.
// 토큰에 없는 수치라 이름을 붙인 상수로 둔다. 여백은 시스템 바 안쪽(Scaffold가 준 padding) 기준이다.

/** 화면 위 여백 96 (`Main.dc.html` 18행) */
private val ScreenTop = 96.dp

/** 위쪽 묶음(로고, 이름·소개, 칩) 사이 28 (19행) */
private val UpperGroupGap = 28.dp

/** 로고 112x72 (20행) */
private val LogoWidth = 112.dp
private val LogoHeight = 72.dp

/** 소개 17 / 400, 행간 1.55 = 26.35 (28행). `title-md`(17/24/700)와 다르다 */
private val IntroFontSize = 17.sp
private val IntroLineHeight = 26.35.sp

/** 기능 칩 안쪽 위아래 6·좌우 12 (30~33행) */
private val ChipVerticalPadding = 6.dp
private val ChipHorizontalPadding = DuriseoSpacing.Space3

/** "간편하게 시작하기" 아래로 4 더, "또는" 구분선 위로 4 더 (37·42행) */
private val ExtraGap = DuriseoSpacing.Space1

/** 게스트 설명 아래 동의 문구를 6 더 띄운다 (48행) */
private val ConsentTopGap = 6.dp

/** 작은 글 12 / 400, 행간 1.6 = 19.2 (47·48행). `caption`은 12/500/16이라 따로 쓴다 */
private val SmallLineHeight = 19.2.sp

/** "최근 사용" 배지: 버튼 오른쪽 위 모서리에 걸친다. 위 -9, 오른쪽 14, 안쪽 세로 2·가로 8, 글자 11/16 600 (`Main.dc.html` 38·39행) */
// "최근 사용" 배지 값(위 −9, 오른쪽 14, 안쪽 2·8, 11/16 600)은 보드 값이고 토큰이 없다 (Main.dc.html, SCREENS SCR-START-01)
private val BadgeTopOffset = (-9).dp
private val BadgeEndInset = 14.dp
private val BadgeVerticalPadding = 2.dp
private val BadgeHorizontalPadding = DuriseoSpacing.Space2
private val BadgeTextStyle = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)

/** 스낵바는 위에서 40 (51행, SCREENS.md SCR-START-01 "오류·안내 위치") */
private val SnackbarTop = 40.dp

@Composable
fun StartRoute(
    sessionReplaced: Boolean,
    padding: PaddingValues,
    onLoggedIn: (AfterLogin) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StartViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val socialTokenProvider = remember(context) { context.socialTokenProvider() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarJob by remember { mutableStateOf<Job?>(null) }

    // 다른 기기 로그인으로 온 경우 안내 스낵바를 한 번 보여 준다 (D-51). 화면이 다시 그려져도 ViewModel이 한 번만 내보낸다
    LaunchedEffect(sessionReplaced) {
        if (sessionReplaced) viewModel.onAction(StartAction.SessionReplacedArrived)
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                // 소셜 SDK는 Route가 현재 Activity로 연다. ViewModel에는 결과(토큰·취소·실패)만 넘긴다
                is StartSideEffect.RequestSocialToken -> scope.launch {
                    val result = try {
                        val activity = context.findActivity()
                        if (activity == null) {
                            Logger.w(SOCIAL_LOG_TAG, "${effect.provider}: 화면에서 Activity를 찾지 못해 로그인을 열 수 없어요")
                            SocialTokenResult.Failed()
                        } else {
                            socialTokenProvider.requestToken(activity, effect.provider)
                        }
                    } catch (e: CancellationException) {
                        // 화면이 사라지며 취소됐다: 요청 중 상태가 남지 않게 취소로 알린다
                        viewModel.onAction(StartAction.SocialTokenReceived(effect.provider, SocialTokenResult.Cancelled))
                        throw e
                    }
                    viewModel.onAction(StartAction.SocialTokenReceived(effect.provider, result))
                }
                is StartSideEffect.ShowMessage -> {
                    snackbarJob?.cancel()
                    snackbarJob = scope.launch {
                        // 보이는 시간은 다른 화면과 같은 기본 길이(Short)다
                        snackbarHostState.showSnackbar(
                            message = context.resources.getString(effect.messageRes),
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
                is StartSideEffect.LoggedIn -> onLoggedIn(effect.destination)
            }
        }
    }

    StartScreen(
        state = state,
        padding = padding,
        onAction = viewModel::onAction,
        snackbarHost = {
            // 위에서 40에 뜬다 (StartScreen이 자리를 잡는다)
            DuriseoSnackbarHost(hostState = snackbarHostState)
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StartScreen(
    state: StartUiState,
    padding: PaddingValues,
    onAction: (StartAction) -> Unit,
    snackbarHost: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 서버 요청(게스트 시작·로그인)이 도는 동안 로딩 덮개 (D-115). 다른 수단 확인 다이얼로그도 같은 층에 그린다 (FR-AUTH-10 AC3)
    DuriseoDialogLayer(
        dialogVisible = state.otherMethodDialog != null && state.lastProvider != null,
        dialog = { OtherMethodDialog(state = state, onAction = onAction) },
        modifier = modifier,
        loading = state.loading,
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // 작은 화면·큰 글꼴에서는 세로로 스크롤한다. 높이가 남으면 위쪽 묶음과 로그인 묶음 사이가 벌어져 로그인 묶음이 아래에 붙는다
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val screenHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = screenHeight)
                    .padding(
                        top = ScreenTop,
                        start = DuriseoSpacing.Space6,
                        end = DuriseoSpacing.Space6,
                        bottom = DuriseoSpacing.Space8,
                    ),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
            // 위쪽 묶음 (아래로 최소 28을 띄워 스크롤할 때 로그인 묶음과 붙지 않게 한다)
            Column(
                modifier = Modifier.padding(bottom = UpperGroupGap),
                verticalArrangement = Arrangement.spacedBy(UpperGroupGap),
            ) {
                Image(
                    painter = painterResource(DesignR.drawable.logo_mark_start),
                    contentDescription = stringResource(R.string.start_logo_description),
                    modifier = Modifier.size(width = LogoWidth, height = LogoHeight),
                )
                Column(verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3)) {
                    Text(
                        text = stringResource(R.string.start_app_name),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.start_intro),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = IntroFontSize,
                            lineHeight = IntroLineHeight,
                        ),
                        color = Ink600,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
                    verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
                ) {
                    FeatureChip(stringResource(R.string.start_chip_calendar))
                    FeatureChip(stringResource(R.string.start_chip_notice))
                    FeatureChip(stringResource(R.string.start_chip_memo))
                }
            }

            // 로그인 묶음: 사이 간격 12
            Column(verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3)) {
                Text(
                    text = stringResource(R.string.start_easy_start),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = ExtraGap),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                    color = Ink500,
                    textAlign = TextAlign.Center,
                )
                SocialButton(
                    text = stringResource(R.string.start_continue_google),
                    lastUsed = state.lastProvider == SocialProvider.GOOGLE,
                    onPressed = { onAction(StartAction.SocialClicked(SocialProvider.GOOGLE)) },
                    style = DuriseoButtonStyle.Filled,
                    enabled = !state.requesting,
                )
                SocialButton(
                    text = stringResource(R.string.start_continue_kakao),
                    lastUsed = state.lastProvider == SocialProvider.KAKAO,
                    onPressed = { onAction(StartAction.SocialClicked(SocialProvider.KAKAO)) },
                    style = DuriseoButtonStyle.Outline,
                    enabled = !state.requesting,
                )
                OrDivider()
                DuriseoButton(
                    text = stringResource(R.string.start_guest),
                    onClick = { onAction(StartAction.GuestClicked) },
                    modifier = Modifier.fillMaxWidth(),
                    style = DuriseoButtonStyle.Tonal,
                    size = DuriseoButtonSize.Medium,
                    enabled = !state.requesting,
                    leadingIconRes = DesignR.drawable.ic_user,
                )
                Text(
                    text = stringResource(R.string.start_guest_description),
                    modifier = Modifier.fillMaxWidth(),
                    style = SmallTextStyle(),
                    color = Ink600,
                    textAlign = TextAlign.Center,
                )
                ConsentText(modifier = Modifier.padding(top = ConsentTopGap))
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

/**
 * 소셜 로그인 버튼. [lastUsed]면 오른쪽 위 모서리에 걸친 "최근 사용" 배지를 붙이고 낭독에 ", 최근 사용"을 더한다 (FR-AUTH-10 AC2).
 * 버튼이 모서리를 잘라 그리므로(clip) 배지는 버튼 밖 형제로 겹쳐 그려서 잘리지 않고, 버튼 높이·글자 자리를 바꾸지 않는다.
 */
@Composable
private fun SocialButton(
    text: String,
    lastUsed: Boolean,
    onPressed: () -> Unit,
    style: DuriseoButtonStyle,
    enabled: Boolean,
) {
    val badge = stringResource(R.string.start_last_used_badge)
    val description = if (lastUsed) stringResource(R.string.start_button_last_used_description, text) else text
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                if (enabled) onClick(label = null) { onPressed(); true } else disabled()
            },
    ) {
        DuriseoButton(
            text = text,
            onClick = onPressed,
            modifier = Modifier.fillMaxWidth(),
            style = style,
            size = DuriseoButtonSize.Social,
            enabled = enabled,
        )
        if (lastUsed) {
            Text(
                text = badge,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = -BadgeEndInset, y = BadgeTopOffset)
                    .background(Brand500, DuriseoShapes.Full)
                    .padding(horizontal = BadgeHorizontalPadding, vertical = BadgeVerticalPadding),
                style = BadgeTextStyle,
                color = OnBrand,
                maxLines = 1,
            )
        }
    }
}

/** 기억한 수단과 다른 수단을 눌렀을 때 묻는 다이얼로그. 확인은 파괴적이지 않아 기본 `brand-500` (FR-AUTH-10 AC3) */
@Composable
private fun OtherMethodDialog(state: StartUiState, onAction: (StartAction) -> Unit) {
    val tapped = state.otherMethodDialog ?: return
    val last = state.lastProvider ?: return
    val lastName = methodName(last)
    val tappedName = methodName(tapped)
    DuriseoDialog(
        title = stringResource(R.string.start_other_method_title, lastName),
        body = stringResource(R.string.start_other_method_body, tappedName),
        cancelText = stringResource(DesignR.string.action_cancel),
        confirmText = stringResource(R.string.start_other_method_confirm, tappedName),
        onCancel = { onAction(StartAction.OtherMethodDismissed) },
        onConfirm = { onAction(StartAction.OtherMethodConfirmed) },
        confirmStyle = DuriseoButtonStyle.Brand,
    )
}

@Composable
private fun methodName(provider: SocialProvider): String = stringResource(
    when (provider) {
        SocialProvider.GOOGLE -> R.string.start_method_google
        SocialProvider.KAKAO -> R.string.start_method_kakao
    },
)

@Composable
private fun SmallTextStyle() = MaterialTheme.typography.labelMedium.copy(
    fontWeight = FontWeight.Normal,
    lineHeight = SmallLineHeight,
)

@Composable
private fun FeatureChip(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, DuriseoShapes.Full)
            .padding(horizontal = ChipHorizontalPadding, vertical = ChipVerticalPadding),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
        color = Ink600,
    )
}

/** "또는" 구분선: 양쪽 선 1(`line-100`) 사이에 12, `ink-500`. 스크린리더는 읽지 않는다 (42행 `aria-hidden`) */
@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ExtraGap)
            .clearAndSetSemantics { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Line100)
        Text(
            text = stringResource(R.string.start_or),
            style = SmallTextStyle(),
            color = Ink500,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Line100)
    }
}

/**
 * 동의 문구 (FR-AUTH-08 AC1). "이용약관"·"개인정보처리방침"은 링크 모양이다: 글자 `ink-600`, 누르는 중 `ink-900`,
 * 밑줄 없음 (design-system.md "시작 화면"). 누르면 각 주소를 외부 브라우저로 연다 (D-154).
 */
@Composable
private fun ConsentText(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val linkStyles = TextLinkStyles(
        style = SpanStyle(color = Ink600),
        pressedStyle = SpanStyle(color = Ink900),
    )
    val terms = stringResource(R.string.start_consent_terms)
    val privacy = stringResource(R.string.start_consent_privacy)
    val template = stringResource(R.string.start_consent, "\u0000", "\u0001")
    val text = buildAnnotatedString {
        // 문구 틀(strings.xml)의 자리표시 두 곳에 링크 글자를 넣는다
        var rest = template
        while (rest.isNotEmpty()) {
            val first = rest.indexOfFirst { it == '\u0000' || it == '\u0001' }
            if (first < 0) {
                append(rest)
                break
            }
            append(rest.substring(0, first))
            val isTerms = rest[first] == '\u0000'
            withLink(LinkAnnotation.Clickable(tag = if (isTerms) "terms" else "privacy", styles = linkStyles) {
                context.openExternalUrl(if (isTerms) LegalLinks.TERMS_URL else LegalLinks.PRIVACY_URL)
            }) {
                append(if (isTerms) terms else privacy)
            }
            rest = rest.substring(first + 1)
        }
    }
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = SmallTextStyle(),
        color = Ink500,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PreviewStartScreen(state: StartUiState = StartUiState(), messageRes: Int? = null) {
    DuriseoTheme {
        StartScreen(
            state = state,
            padding = PaddingValues(),
            onAction = {},
            snackbarHost = {
                // 스낵바가 떠 있는 모습을 정적으로 그린다
                if (messageRes != null) DuriseoSnackbar(message = stringResource(messageRes))
            },
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StartScreenPreview() = PreviewStartScreen()

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StartScreenSessionReplacedPreview() = PreviewStartScreen(messageRes = R.string.start_session_replaced)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StartScreenLoadingPreview() = PreviewStartScreen(state = StartUiState(requesting = true, loading = true))

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StartScreenCommonErrorPreview() = PreviewStartScreen(messageRes = DesignR.string.error_common)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StartScreenOfflinePreview() = PreviewStartScreen(messageRes = DesignR.string.offline_required)

/** 작은 화면(높이 480)에서 스크롤되는 모습 */
@Preview(showBackground = true, widthDp = 390, heightDp = 480)
@Composable
private fun StartScreenSmallPreview() = PreviewStartScreen()

/** 카카오로 마지막 로그인한 기기: Kakao 버튼에 배지 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StartPreview_LastUsedKakao() = PreviewStartScreen(state = StartUiState(lastProvider = SocialProvider.KAKAO))

/** 카카오를 기억한 기기에서 [Google로 계속하기]를 누른 모습 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StartPreview_OtherMethodDialog() = PreviewStartScreen(
    state = StartUiState(lastProvider = SocialProvider.KAKAO, otherMethodDialog = SocialProvider.GOOGLE),
)
