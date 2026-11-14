package com.duriseo.android.feature.settings.notification

import androidx.annotation.StringRes
import com.duriseo.android.domain.notification.NotificationSettingKind
import com.duriseo.android.domain.notification.NotificationSettings

/**
 * SCR-SET-02 알림 설정 상태 (FR-NOTI-02, D-145).
 *
 * @property settings 기기에 저장된 마지막 서버 값. 한 번도 못 받았으면 null이고 스위치 4개를 비활성으로 둔다 (Q-14와 같은 상황)
 * @property saving 저장(`PATCH`) 요청 중. 로딩 덮개를 그린다 (D-115)
 */
data class NotificationSettingsUiState(
    val settings: NotificationSettings? = null,
    val saving: Boolean = false,
) {
    /** 스위치를 누를 수 있다: 서버 값을 받았고 저장 중이 아니다 */
    val switchesEnabled: Boolean get() = settings != null && !saving

    /** 저장 요청이 도는 동안 true. 로딩 덮개를 그린다 (D-115) */
    val loading: Boolean get() = saving

    /** 스위치의 켜짐. 값을 모르면 꺼진 모양이다 (보드 `loadFailed`: 손잡이는 왼쪽) */
    fun isOn(kind: NotificationSettingKind): Boolean = settings?.get(kind) ?: false
}

sealed interface NotificationSettingsAction {
    data object BackClicked : NotificationSettingsAction

    /** 스위치를 눌렀다. 바뀐 종류 하나만 저장한다 (D-145) */
    data class Toggled(val kind: NotificationSettingKind) : NotificationSettingsAction
}

sealed interface NotificationSettingsSideEffect {
    data object Back : NotificationSettingsSideEffect

    /** 스낵바 (위에서 40, D-126): 저장 실패는 공통 오류, 오프라인은 "인터넷 연결이 필요해요", 한 번도 못 받았으면 공통 오류 */
    data class ShowMessage(@StringRes val messageRes: Int) : NotificationSettingsSideEffect
}
