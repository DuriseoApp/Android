package com.duriseo.android.feature.settings.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.DuriseoButtonStyle
import com.duriseo.android.core.designsystem.component.DuriseoDialog
import com.duriseo.android.core.designsystem.component.LogoutConfirmDialog
import com.duriseo.android.feature.settings.R

/**
 * 계정 화면의 다이얼로그 4종 (SCREENS.md SCR-SET-01 "다이얼로그"). 한 번에 하나만 뜬다.
 * 요청이 도는 동안은 두 버튼이 모두 비활성이다 (D-88).
 */
@Composable
internal fun AccountDialogs(
    state: AccountUiState,
    onAction: (AccountAction) -> Unit,
) {
    val dismiss = { onAction(AccountAction.DialogDismissed) }
    val enabled = !state.requesting
    when (state.dialog) {
        null -> Unit

        // 이미 가입된 계정 ①: [그 계정으로 로그인]은 파괴적이지 않아 brand-500 채움 (FR-AUTH-03 AC2)
        AccountDialog.AlreadyUsed -> DuriseoDialog(
            title = stringResource(R.string.already_used_dialog_title),
            body = stringResource(R.string.already_used_dialog_body),
            cancelText = stringResource(DesignR.string.action_cancel),
            confirmText = stringResource(R.string.already_used_dialog_confirm),
            onCancel = dismiss,
            onConfirm = { onAction(AccountAction.ExistingLoginClicked) },
            confirmStyle = DuriseoButtonStyle.Brand,
            buttonsEnabled = enabled,
        )

        // 이미 가입된 계정 ②: 게스트 기록이 삭제되므로 danger-700 채움 (FR-AUTH-03 AC3)
        AccountDialog.SwitchConfirm -> DuriseoDialog(
            title = stringResource(R.string.switch_dialog_title),
            body = stringResource(R.string.switch_dialog_body),
            cancelText = stringResource(DesignR.string.action_cancel),
            confirmText = stringResource(R.string.switch_dialog_confirm),
            onCancel = dismiss,
            onConfirm = { onAction(AccountAction.SwitchConfirmed) },
            confirmStyle = DuriseoButtonStyle.Destructive,
            buttonsEnabled = enabled,
        )

        AccountDialog.Logout -> LogoutConfirmDialog(
            onCancel = dismiss,
            onConfirm = { onAction(AccountAction.LogoutConfirmed) },
            buttonsEnabled = enabled,
        )

        // 탈퇴 확인: 커플 공간이면 상대 이름이 들어가고, 개인 공간이면 바로 삭제된다는 문구 (FR-AUTH-06 AC1·AC2)
        AccountDialog.Withdraw -> DuriseoDialog(
            title = stringResource(R.string.withdraw_dialog_title),
            body = when (val message = state.withdrawMessage) {
                WithdrawMessage.Personal -> stringResource(R.string.withdraw_dialog_body_personal)
                is WithdrawMessage.Couple ->
                    stringResource(R.string.withdraw_dialog_body_couple, message.partnerNickname)
            },
            cancelText = stringResource(DesignR.string.action_cancel),
            confirmText = stringResource(R.string.account_withdraw),
            onCancel = dismiss,
            onConfirm = { onAction(AccountAction.WithdrawConfirmed) },
            confirmStyle = DuriseoButtonStyle.Destructive,
            buttonsEnabled = enabled,
        )
    }
}
