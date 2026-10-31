package com.duriseo.android.feature.couple.invitecreate

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.duriseo.android.core.designsystem.component.DuriseoButton
import com.duriseo.android.core.designsystem.component.DuriseoButtonSize
import com.duriseo.android.core.designsystem.component.DuriseoButtonStyle
import com.duriseo.android.core.designsystem.component.DuriseoLargeButton
import com.duriseo.android.core.designsystem.component.DuriseoSnackbar
import com.duriseo.android.core.designsystem.component.DuriseoSnackbarHost
import com.duriseo.android.core.designsystem.theme.Brand100
import com.duriseo.android.core.designsystem.theme.Brand500
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink300
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink700
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.OnBrand
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.core.designsystem.theme.Surface100
import com.duriseo.android.feature.couple.R
import com.duriseo.android.feature.couple.component.CoupleHeader
import com.duriseo.android.feature.couple.component.CoupleTitle
import com.duriseo.android.feature.couple.component.InviteCodeDisplay
import com.duriseo.android.core.designsystem.R as DesignR
import kotlinx.coroutines.launch

// SCR-COUPLE-02 초대코드 만들기 (FR-COUPLE-01, FR-COUPLE-02 AC5). 수치는 보드 `InviteCreate.dc.html`.
// 토큰에 없는 값이라 이름을 붙인 상수로 둔다 (unit-3.md 확인 필요 14).

/** 본문 위 여백 12, 사이 간격 24 (23행) */
private val ContentTop = 12.dp
private val SectionGap = 24.dp

/** 코드 패널: 안쪽 위 24·좌우 20·아래 20, 사이 14 (28행) */
private val PanelPaddingTop = 24.dp
private val PanelPaddingHorizontal = 20.dp
private val PanelPaddingBottom = 20.dp
private val PanelGap = 14.dp

/** 남은 시간 줄: 아이콘 16, 글자와 사이 6 (43행) */
private val TimerIconSize = 16.dp
private val TimerGap = 6.dp

/** 만료 안내: 안쪽 위아래 10·좌우 14, 모서리 12, 글자 13/600 행간 1.5 (48행) */
private val NoticePaddingVertical = 10.dp
private val NoticePaddingHorizontal = 14.dp
private val NoticeLineHeight = 19.5.sp

/** 복사·공유 버튼 줄 위로 4 더, 아이콘 18 (50·55행) */
private val ButtonsTop = 4.dp
private val ButtonIconSize = 18.dp

/** 연결 상태 카드: 안쪽 위아래 16·좌우 18, 모서리 18, 테두리 1 `line-100`, 사이 14 (65행) */
private val StatusPaddingVertical = 16.dp
private val StatusPaddingHorizontal = 18.dp
private val StatusGap = 14.dp
private val StatusBorderWidth = 1.dp

/** 상태 점: 바깥 20 `brand-100`, 안쪽 8 `brand-500` (66행) */
private val StatusDotOuter = 20.dp
private val StatusDotInner = 8.dp

/** 카드 안 글자 사이 2 (67행) */
private val StatusTextGap = 2.dp

/** [코드 새로 만들기]: 높이 44, 좌우 12, 글자 14/500, 아래 안내 12 (77~84행) */
private val ReissueHeight = 44.dp
private val ReissuePaddingHorizontal = 12.dp
private val ReissueGap = 2.dp
private val WaitTextSize = 12.sp
private val WaitTextLineHeight = 16.sp

/** 맨 아래 버튼 영역: 위 16·좌우 24·아래 32 (89행) */
private val BottomAreaTop = 16.dp
private val BottomAreaBottom = 32.dp

/** 스낵바는 바닥에서 112 (버튼 위, 94행. SCREENS.md "아직 연결 전") */
private val SnackbarBottom = 112.dp

@Composable
fun InviteCreateRoute(
    lifecycleOwner: LifecycleOwner,
    padding: PaddingValues,
    onBack: () -> Unit,
    onConnected: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InviteCreateViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 화면이 보이는 동안(STARTED~STOP)만 5초 확인과 코드 만료 확인을 한다 (D-42, FR-COUPLE-02 AC5)
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onAction(InviteCreateAction.Shown)
                Lifecycle.Event.ON_STOP -> viewModel.onAction(InviteCreateAction.Hidden)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onAction(InviteCreateAction.Hidden)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is InviteCreateSideEffect.CopyCode -> context.copyToClipboard(
                    label = context.getString(R.string.invite_create_panel_description),
                    text = effect.code,
                )
                is InviteCreateSideEffect.ShareCode -> context.shareText(effect.code)
                InviteCreateSideEffect.NavigateToHome -> onConnected()
                is InviteCreateSideEffect.ShowMessage -> scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = context.resources.getString(effect.messageRes),
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    InviteCreateScreen(
        state = state,
        padding = padding,
        onAction = viewModel::onAction,
        onBack = onBack,
        snackbarHost = { DuriseoSnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    )
}

private fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

/** 공유 시트에 코드만 넘긴다 (안내 문구는 문서에 없어 넣지 않는다, unit-3.md 확인 필요 13) */
private fun Context.shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        startActivity(Intent.createChooser(send, null))
    } catch (e: ActivityNotFoundException) {
        // 공유할 앱이 없으면 아무 일도 없다
    }
}

@Composable
fun InviteCreateScreen(
    state: InviteCreateUiState,
    padding: PaddingValues,
    onAction: (InviteCreateAction) -> Unit,
    onBack: () -> Unit,
    snackbarHost: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            CoupleHeader(showBack = true, onBack = onBack)
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
                Column(verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2)) {
                    CoupleTitle(stringResource(R.string.invite_create_title))
                    Text(
                        text = stringResource(R.string.invite_create_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink600,
                    )
                }
                CodePanel(state = state, onAction = onAction)
                StatusCard()
                ReissueArea(state = state, onAction = onAction)
            }
            Box(
                modifier = Modifier.padding(
                    start = DuriseoSpacing.Space6,
                    end = DuriseoSpacing.Space6,
                    top = BottomAreaTop,
                    bottom = BottomAreaBottom,
                ),
            ) {
                DuriseoLargeButton(
                    text = stringResource(R.string.invite_create_check),
                    onClick = { onAction(InviteCreateAction.CheckClicked) },
                    container = Ink900,
                    contentColor = OnBrand,
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(padding)
                .padding(bottom = SnackbarBottom),
        ) {
            snackbarHost()
        }
    }
}

@Composable
private fun CodePanel(state: InviteCreateUiState, onAction: (InviteCreateAction) -> Unit) {
    val codeEnabled = state.code != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface100, MaterialTheme.shapes.extraLarge)
            .padding(
                top = PanelPaddingTop,
                start = PanelPaddingHorizontal,
                end = PanelPaddingHorizontal,
                bottom = PanelPaddingBottom,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PanelGap),
    ) {
        InviteCodeDisplay(
            code = state.code,
            description = stringResource(R.string.invite_create_code_description, state.code.orEmpty()),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(TimerGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_clock),
                contentDescription = null,
                modifier = Modifier.size(TimerIconSize),
                tint = Ink600,
            )
            Text(
                text = stringResource(
                    R.string.invite_create_remaining,
                    state.remainingSeconds / SECONDS_PER_MINUTE,
                    state.remainingSeconds % SECONDS_PER_MINUTE,
                ),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = Ink600,
            )
        }
        if (state.renewedNotice) {
            Text(
                text = stringResource(R.string.invite_create_renewed),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface0, MaterialTheme.shapes.small)
                    .padding(horizontal = NoticePaddingHorizontal, vertical = NoticePaddingVertical)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.labelLarge.copy(lineHeight = NoticeLineHeight),
                color = Ink700,
                textAlign = TextAlign.Center,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ButtonsTop),
            horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
        ) {
            DuriseoButton(
                text = stringResource(if (state.copied) R.string.invite_create_copied else R.string.invite_create_copy),
                onClick = { onAction(InviteCreateAction.CopyClicked) },
                modifier = Modifier.weight(1f),
                style = DuriseoButtonStyle.Outline,
                size = DuriseoButtonSize.Medium,
                enabled = codeEnabled,
                leadingIconRes = R.drawable.ic_copy,
            )
            DuriseoButton(
                text = stringResource(R.string.invite_create_share),
                onClick = { onAction(InviteCreateAction.ShareClicked) },
                modifier = Modifier.weight(1f),
                style = DuriseoButtonStyle.Outline,
                size = DuriseoButtonSize.Medium,
                enabled = codeEnabled,
                leadingIconRes = R.drawable.ic_share,
            )
        }
    }
}

@Composable
private fun StatusCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(StatusBorderWidth, Line100, DuriseoShapes.Fab)
            .padding(horizontal = StatusPaddingHorizontal, vertical = StatusPaddingVertical),
        horizontalArrangement = Arrangement.spacedBy(StatusGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(StatusDotOuter)
                .background(Brand100, DuriseoShapes.Full),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(StatusDotInner)
                    .background(Brand500, DuriseoShapes.Full),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(StatusTextGap)) {
            Text(
                text = stringResource(R.string.invite_create_waiting_title),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Ink900,
            )
            Text(
                text = stringResource(R.string.invite_create_waiting_body),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = Ink600,
            )
        }
    }
}

@Composable
private fun ReissueArea(state: InviteCreateUiState, onAction: (InviteCreateAction) -> Unit) {
    val label = stringResource(R.string.invite_create_reissue)
    val shape = MaterialTheme.shapes.small
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ReissueGap),
    ) {
        Box(
            modifier = Modifier
                .height(ReissueHeight)
                .clip(shape)
                .clickable(enabled = state.canReissue, role = Role.Button) {
                    onAction(InviteCreateAction.ReissueClicked)
                }
                .padding(horizontal = ReissuePaddingHorizontal),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = if (state.canReissue) Ink600 else Ink300,
                textDecoration = if (state.canReissue) TextDecoration.Underline else TextDecoration.None,
            )
        }
        if (state.reissueWaitSeconds > 0) {
            Text(
                text = stringResource(R.string.invite_create_reissue_wait, state.reissueWaitSeconds),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = WaitTextSize,
                    lineHeight = WaitTextLineHeight,
                    fontWeight = FontWeight.Normal,
                ),
                color = Ink600,
            )
        }
    }
}

private const val SECONDS_PER_MINUTE = 60

@Composable
private fun PreviewInviteCreate(
    state: InviteCreateUiState,
    snackbarMessageRes: Int? = null,
) {
    DuriseoTheme {
        InviteCreateScreen(
            state = state,
            padding = PaddingValues(),
            onAction = {},
            onBack = {},
            snackbarHost = {
                if (snackbarMessageRes != null) DuriseoSnackbar(message = stringResource(snackbarMessageRes))
            },
        )
    }
}

/** `SCR-COUPLE-02.png`: 코드 K7Q29X, 8분 12초 남음 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InviteCreateScreenPreview() = PreviewInviteCreate(
    InviteCreateUiState(code = "K7Q29X", remainingSeconds = 8 * 60 + 12),
)

/** `SCR-COUPLE-02--cooldown.png`: 9분 18초 남음, 18초 뒤에 다시 만들 수 있어요 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InviteCreateScreenCooldownPreview() = PreviewInviteCreate(
    InviteCreateUiState(code = "K7Q29X", remainingSeconds = 9 * 60 + 18, reissueWaitSeconds = 18),
)

/** `SCR-COUPLE-02--renewed.png`: 새 코드 P4WM7R, 안내 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InviteCreateScreenRenewedPreview() = PreviewInviteCreate(
    InviteCreateUiState(code = "P4WM7R", remainingSeconds = 9 * 60 + 56, reissueWaitSeconds = 56, renewedNotice = true),
)

/** `SCR-COUPLE-02--not-yet.png`: 스낵바 "아직 연결되지 않았어요" (바닥에서 112) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InviteCreateScreenNotYetPreview() = PreviewInviteCreate(
    state = InviteCreateUiState(code = "K7Q29X", remainingSeconds = 8 * 60 + 12),
    snackbarMessageRes = R.string.invite_create_not_yet,
)

/** 복사한 뒤: 버튼 글자가 "복사됨" */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InviteCreateScreenCopiedPreview() = PreviewInviteCreate(
    InviteCreateUiState(code = "K7Q29X", remainingSeconds = 8 * 60 + 12, copied = true),
)

/** 첫 코드를 받는 중: 문서에 없는 상태라 빈 칸으로 둔다 (v0.9 반영 대기, unit-3.md 확인 필요 3) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InviteCreateScreenLoadingPreview() = PreviewInviteCreate(InviteCreateUiState())

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InviteCreateScreenOfflinePreview() = PreviewInviteCreate(
    state = InviteCreateUiState(code = "K7Q29X", remainingSeconds = 8 * 60 + 12),
    snackbarMessageRes = DesignR.string.offline_required,
)
