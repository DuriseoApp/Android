package com.duriseo.android.feature.settings.connection

import androidx.annotation.StringRes
import com.duriseo.android.feature.settings.menu.SettingsSpace

/**
 * SCR-SET-01 하위 화면 "연결 정보"의 상태 (FR-SET-01, FR-COUPLE-05). 화면은 Room의 내 정보만 구독한다.
 *
 * @property space 커플이면 [SettingsSpace.Couple] (연결됨), 혼자면 [SettingsSpace.Alone] (혼자 사용 중).
 *   내 정보를 아직 못 받았으면 null (앱바만 그린다)
 * @property unlinkDialogVisible [연결 해제]를 눌러 확인 다이얼로그가 떠 있다
 * @property requesting `DELETE /couple` 요청 중. 다이얼로그 버튼은 눌리지 않는다
 */
data class ConnectionInfoUiState(
    val space: SettingsSpace? = null,
    val unlinkDialogVisible: Boolean = false,
    val requesting: Boolean = false,
) {
    /** 해제 요청이 도는 동안 true. 로딩 덮개를 그린다 (D-115) */
    val loading: Boolean get() = requesting
}

sealed interface ConnectionInfoAction {
    /** [연결 해제] → 확인 다이얼로그 */
    data object UnlinkClicked : ConnectionInfoAction

    /** 다이얼로그의 [취소], 뒤 막, 뒤로 가기 */
    data object UnlinkDismissed : ConnectionInfoAction

    /** 다이얼로그의 [연결 해제]: `DELETE /couple` */
    data object UnlinkConfirmed : ConnectionInfoAction
}

sealed interface ConnectionInfoSideEffect {
    /** 공통 스낵바 (오프라인 "인터넷 연결이 필요해요", 서버 오류 공통 문구) */
    data class ShowMessage(@StringRes val messageRes: Int) : ConnectionInfoSideEffect
}
