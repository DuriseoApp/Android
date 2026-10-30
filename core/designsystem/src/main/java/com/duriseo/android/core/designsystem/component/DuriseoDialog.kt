package com.duriseo.android.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.core.designsystem.theme.DuriseoShadows
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.duriseoShadow

// 다이얼로그 값 (design-system.md "다이얼로그", 보드 `Account.dc.html` 75~122행, SCREENS.md 공통 "다이얼로그").

/** 패널 폭은 100%에 최대 320 */
private val DialogMaxWidth = 320.dp

/**
 * 확인 다이얼로그. 화면 위에 덮는 오버레이라서 **화면의 맨 위 Box 안, 다른 내용 뒤에** 둔다
 * (`Box(Modifier.fillMaxSize()) { 화면 내용; if (보임) DuriseoDialog(...) }`). 시스템 창을 따로 열지 않아서
 * 뒤 막이 토큰 `scrim` 그대로이고 Preview에서도 보인다.
 *
 * 뒤 막: 화면 전체, 누르거나 뒤로 가기를 누르면 [onDismissRequest] (접근성 라벨 "닫기").
 * 패널: 폭 최대 320, 면 `surface-0`, 모서리 `radius-2xl`(24), 그림자 `shadow-dialog`, 안쪽 위 24·좌우 20·아래 20.
 * 제목 `title-md`, 본문 `body-md` `ink-600`, 둘 사이 `space-2`, 버튼 위 `space-4`. 버튼 2개는 같은 폭에 사이 `space-2`.
 *
 * @param confirmStyle 파괴적 확인은 [DuriseoButtonStyle.Destructive], 그 밖은 [DuriseoButtonStyle.Brand]
 * @param buttonsEnabled false면 두 버튼과 뒤 막이 모두 눌리지 않는다. 확인 뒤 요청이 도는 동안 쓴다 (D-88)
 */
@Composable
fun DuriseoDialog(
    title: String,
    body: String,
    cancelText: String,
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmStyle: DuriseoButtonStyle = DuriseoButtonStyle.Brand,
    buttonsEnabled: Boolean = true,
    onDismissRequest: () -> Unit = onCancel,
) {
    BackHandler(enabled = buttonsEnabled, onBack = onDismissRequest)
    val shape = MaterialTheme.shapes.extraLarge
    val scrimLabel = stringResource(R.string.nav_close)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // 뒤 막
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim)
                .semantics {
                    contentDescription = scrimLabel
                    role = Role.Button
                }
                .clickable(
                    enabled = buttonsEnabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest,
                ),
        )
        // 패널
        Column(
            modifier = Modifier
                .padding(DuriseoSpacing.Space6)
                .widthIn(max = DialogMaxWidth)
                .fillMaxWidth()
                .duriseoShadow(shape, DuriseoShadows.Dialog)
                .background(MaterialTheme.colorScheme.surface, shape)
                // 패널 안을 눌러도 뒤 막이 닫히지 않게 누름을 여기서 받는다
                .pointerInput(Unit) { detectTapGestures { } }
                .semantics { paneTitle = title }
                .padding(
                    top = DuriseoSpacing.Space6,
                    start = DuriseoSpacing.Space5,
                    end = DuriseoSpacing.Space5,
                    bottom = DuriseoSpacing.Space5,
                ),
            verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = DuriseoSpacing.Space2),
                horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
            ) {
                // 위 Column의 간격 space-2에 padding space-2를 더해 버튼 위 간격이 space-4(16)가 된다
                DuriseoButton(
                    text = cancelText,
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    style = DuriseoButtonStyle.Outline,
                    enabled = buttonsEnabled,
                )
                DuriseoButton(
                    text = confirmText,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    style = confirmStyle,
                    enabled = buttonsEnabled,
                )
            }
        }
    }
}

/**
 * 로그아웃 확인 (SCREENS.md SCR-SET-01 "로그아웃 확인", FR-AUTH-05). 전체 메뉴의 [로그아웃], 계정 화면의 [로그아웃],
 * SCR-COUPLE-01의 "다른 계정으로 로그인"이 같은 다이얼로그를 쓴다 (D-80). 로그아웃은 파괴적 행동이라
 * 확인 버튼이 `danger-700` 채움이다.
 */
@Composable
fun LogoutConfirmDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    buttonsEnabled: Boolean = true,
) {
    DuriseoDialog(
        title = stringResource(R.string.logout_dialog_title),
        body = stringResource(R.string.logout_dialog_body),
        cancelText = stringResource(R.string.action_cancel),
        confirmText = stringResource(R.string.action_logout),
        onCancel = onCancel,
        onConfirm = onConfirm,
        modifier = modifier,
        confirmStyle = DuriseoButtonStyle.Destructive,
        buttonsEnabled = buttonsEnabled,
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LogoutConfirmDialogPreview() {
    DuriseoTheme {
        LogoutConfirmDialog(onCancel = {}, onConfirm = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LogoutConfirmDialogRequestingPreview() {
    DuriseoTheme {
        LogoutConfirmDialog(onCancel = {}, onConfirm = {}, buttonsEnabled = false)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DuriseoDialogBrandConfirmPreview() {
    // 견본 글자라 앱 문구가 아니다
    DuriseoTheme {
        DuriseoDialog(
            title = "Brand confirm",
            body = "파괴적이지 않은 확인은 brand-500 채움이에요.",
            cancelText = stringResource(R.string.action_cancel),
            confirmText = "확인",
            onCancel = {},
            onConfirm = {},
        )
    }
}
