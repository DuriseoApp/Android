package com.duriseo.android.feature.settings.appupdate

/** 앱 정보 카드의 업데이트 상태 (FR-SET-02 AC4, D-151) */
enum class AppUpdateStatus {
    /** 확인 중: 최신 여부 줄만 안 보이게 해서 카드 높이를 유지한다 */
    CHECKING,

    /** 최신: "최신 버전을 사용하고 있어요" */
    UP_TO_DATE,

    /** 새 버전이 있다: [업데이트] 버튼 */
    UPDATE_AVAILABLE,

    /** 받는 중이거나 다 받았다 (확인 필요 5): 확인 중과 같은 모양이다. 다 받은 것은 앱이 백그라운드로 갈 때 설치한다 */
    DOWNLOADING,

    /** 확인할 수 없다(오프라인, Play 미설치·직접 설치, API 실패): 버전만 보이고 최신 여부 줄은 뺀다 */
    UNKNOWN,
}

/**
 * 앱 정보 카드의 상태. 버전은 "버전 [name] ([code])"로 보인다 (보드 `Menu.dc.html` 144행).
 */
data class AppUpdateUiState(
    val versionName: String = "",
    val versionCode: Long = 0,
    val status: AppUpdateStatus = AppUpdateStatus.CHECKING,
) {
    /** "최신 버전을 사용하고 있어요" 줄을 보인다 */
    val latestLineVisible: Boolean get() = status == AppUpdateStatus.UP_TO_DATE

    /** 줄은 안 보이지만 자리는 차지한다 (카드 높이 유지) */
    val latestLineReserved: Boolean get() = status == AppUpdateStatus.CHECKING || status == AppUpdateStatus.DOWNLOADING

    val updateButtonVisible: Boolean get() = status == AppUpdateStatus.UPDATE_AVAILABLE
}
