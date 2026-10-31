package com.duriseo.android.feature.couple.inviteenter

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.component.DuriseoLargeButton
import com.duriseo.android.core.designsystem.component.DuriseoSnackbar
import com.duriseo.android.core.designsystem.component.DuriseoSnackbarHost
import com.duriseo.android.core.designsystem.theme.Brand500
import com.duriseo.android.core.designsystem.theme.Danger600
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line200
import com.duriseo.android.core.designsystem.theme.OnBrand
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.domain.couple.InviteCodeInput
import com.duriseo.android.feature.couple.R
import com.duriseo.android.feature.couple.component.CoupleHeader
import com.duriseo.android.feature.couple.component.CoupleTitle
import com.duriseo.android.feature.couple.component.InviteCodeInputBoxes
import kotlinx.coroutines.launch

// SCR-COUPLE-03 초대코드 입력하기 (FR-COUPLE-02, FR-COUPLE-01 AC4). 수치는 보드 `InviteEnter.dc.html`.
// 토큰에 없는 값이라 이름을 붙인 상수로 둔다 (unit-3.md 확인 필요 14).

/** 본문 위 여백 12, 사이 간격 28 (23행) */
private val ContentTop = 12.dp
private val SectionGap = 28.dp

/** 칸 묶음: 라벨·칸·붙여넣기 사이 12 (29행), 입력 칸 높이 58 (InviteCodeInputBoxes) */
private val FieldGap = 12.dp
private val InputHeight = 58.dp

/** [붙여넣기]: 높이 44, 안쪽 왼쪽 10·오른쪽 14, 모서리 12, 테두리 1, 아이콘 18, 사이 6 (36행) */
private val PasteHeight = 44.dp
private val PastePaddingStart = 10.dp
private val PastePaddingEnd = 14.dp
private val PasteBorderWidth = 1.dp
private val PasteIconSize = 18.dp
private val PasteGap = 6.dp

/** 맨 아래 버튼 영역: 위 16·좌우 24·아래 32 (47행) */
private val BottomAreaTop = 16.dp
private val BottomAreaBottom = 32.dp

/** 스낵바는 위에서 40 (SCREENS.md에 이 화면의 위치가 없어 시작 화면과 같게 둔다) */
private val SnackbarTop = 40.dp

@Composable
fun InviteEnterRoute(
    padding: PaddingValues,
    onBack: () -> Unit,
    onConnected: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InviteEnterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                InviteEnterSideEffect.NavigateToHome -> onConnected()
                is InviteEnterSideEffect.ShowMessage -> scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = context.resources.getString(effect.messageRes),
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    InviteEnterScreen(
        state = state,
        padding = padding,
        onAction = viewModel::onAction,
        onPasteClicked = { viewModel.onAction(InviteEnterAction.Pasted(context.clipboardText())) },
        onBack = onBack,
        snackbarHost = { DuriseoSnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    )
}

/** 클립보드 첫 항목의 글자. 비었으면 null */
private fun Context.clipboardText(): String? {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(this)?.toString()
}

@Composable
fun InviteEnterScreen(
    state: InviteEnterUiState,
    padding: PaddingValues,
    onAction: (InviteEnterAction) -> Unit,
    onPasteClicked: () -> Unit,
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
                    CoupleTitle(stringResource(R.string.invite_enter_title))
                    Text(
                        text = stringResource(R.string.invite_enter_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink600,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(FieldGap)) {
                    val label = stringResource(R.string.invite_enter_label)
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = Ink600,
                    )
                    CodeField(
                        code = state.code,
                        label = label,
                        enabled = !state.requesting,
                        onCodeChange = { onAction(InviteEnterAction.CodeChanged(it)) },
                    )
                    state.errorRes?.let { errorRes ->
                        Text(
                            text = stringResource(errorRes),
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            style = MaterialTheme.typography.labelMedium,
                            color = Danger600,
                        )
                    }
                    PasteButton(onClick = onPasteClicked, enabled = !state.requesting)
                }
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
                    text = stringResource(R.string.invite_enter_connect),
                    onClick = { onAction(InviteEnterAction.ConnectClicked) },
                    container = Brand500,
                    contentColor = OnBrand,
                    enabled = state.canConnect,
                )
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

/**
 * 코드 칸 6개 위에 보이지 않는 입력 칸을 겹쳐 둔다 (보드 `InviteEnter.dc.html` 31행과 같은 구조).
 * 칸을 누르면 키보드가 열리고, 소문자·기호 처리는 ViewModel이 한다.
 */
@Composable
private fun CodeField(
    code: String,
    label: String,
    enabled: Boolean,
    onCodeChange: (String) -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        InviteCodeInputBoxes(code = code)
        BasicTextField(
            value = code,
            onValueChange = onCodeChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(InputHeight)
                .alpha(0f)
                .semantics { contentDescription = label },
            enabled = enabled,
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
        )
    }
}

@Composable
private fun PasteButton(onClick: () -> Unit, enabled: Boolean) {
    val shape = MaterialTheme.shapes.small
    Row(
        modifier = Modifier
            .height(PasteHeight)
            .clip(shape)
            .background(Surface0)
            .border(PasteBorderWidth, Line200, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(start = PastePaddingStart, end = PastePaddingEnd),
        horizontalArrangement = Arrangement.spacedBy(PasteGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_paste),
            contentDescription = null,
            modifier = Modifier.size(PasteIconSize),
            tint = Ink900,
        )
        Text(
            text = stringResource(R.string.invite_enter_paste),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = Ink900,
        )
    }
}

@Composable
private fun PreviewInviteEnter(
    state: InviteEnterUiState,
    snackbarMessageRes: Int? = null,
) {
    DuriseoTheme {
        InviteEnterScreen(
            state = state,
            padding = PaddingValues(),
            onAction = {},
            onPasteClicked = {},
            onBack = {},
            snackbarHost = {
                if (snackbarMessageRes != null) DuriseoSnackbar(message = stringResource(snackbarMessageRes))
            },
        )
    }
}

/** `SCR-COUPLE-03.png`: 입력 전. 첫 칸이 `brand-500` 2, [연결하고 시작하기] 비활성 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InviteEnterScreenPreview() = PreviewInviteEnter(InviteEnterUiState())

/** 입력 중: 3자리 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InviteEnterScreenTypingPreview() = PreviewInviteEnter(InviteEnterUiState(code = "K7Q"))

/** `SCR-COUPLE-03--filled.png`: 6자리를 넣어 버튼이 켜졌다 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InviteEnterScreenFilledPreview() = PreviewInviteEnter(InviteEnterUiState(code = "K7Q29X"))

/** 만료된 코드: 문구가 문서에 있는 유일한 오류 (위치·모양은 v0.9 반영 대기) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InviteEnterScreenExpiredPreview() =
    PreviewInviteEnter(InviteEnterUiState(code = "K7Q29X", errorRes = R.string.invite_enter_error_expired))

/** 요청 중: 연결 버튼 비활성 (SCREENS 공통 "요청 중") */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InviteEnterScreenRequestingPreview() =
    PreviewInviteEnter(InviteEnterUiState(code = InviteCodeInput.clean("k7q29x"), requesting = true))
