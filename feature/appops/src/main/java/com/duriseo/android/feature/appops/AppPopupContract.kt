package com.duriseo.android.feature.appops

import android.app.Activity
import com.duriseo.android.domain.appops.AppPopup

/** 앱 전체 공지·업데이트 팝업 상태 (SCR-APP-01·02). [popup]이 null이면 아무것도 뜨지 않는다. 한 번에 하나만 보인다 (D-178) */
data class AppPopupUiState(val popup: AppPopup? = null)

sealed interface AppPopupAction {
    /** [확인]·[닫기]: 이번에만 닫는다. 다음 실행·돌아올 때 다시 뜬다 (D-179) */
    data object Dismiss : AppPopupAction

    /** [오늘 하루 안 보기]: 그 `id`를 오늘 자정까지(기기 시간대) 다시 띄우지 않는다 */
    data object HideToday : AppPopupAction

    /** 뒤로 가기. 닫을 수 있는 팝업은 [Dismiss]와 같고(기억하지 않는다, Q-21) 닫을 수 없는 팝업은 무시한다 (D-179) */
    data object Back : AppPopupAction

    /** [앱 종료]. 다음 실행 때 다시 확인한다 */
    data object Exit : AppPopupAction

    /** 링크 버튼. 브라우저로 열고 팝업은 그대로 둔다 */
    data object OpenLink : AppPopupAction

    /** [업데이트]. 즉시 업데이트를 시작하고, 쓸 수 없으면 스토어를 연다 (FR-APP-02 AC2) */
    data class Update(val activity: Activity) : AppPopupAction
}

sealed interface AppPopupSideEffect {
    /** `Activity.finishAndRemoveTask()` (unit-9 결정 4) */
    data object FinishApp : AppPopupSideEffect

    /** 브라우저로 열 `https://` 주소 */
    data class OpenUrl(val url: String) : AppPopupSideEffect

    /** Play 스토어의 둘이서 페이지: [marketUri]로 열고, 열 앱이 없으면 [webUrl] (unit-9 결정 6) */
    data class OpenStore(val marketUri: String, val webUrl: String) : AppPopupSideEffect
}

/** Play 스토어 주소 (unit-9 결정 6) */
internal object PlayStoreLinks {
    const val MARKET_URI = "market://details?id=com.duriseo.android"
    const val WEB_URL = "https://play.google.com/store/apps/details?id=com.duriseo.android"
}
