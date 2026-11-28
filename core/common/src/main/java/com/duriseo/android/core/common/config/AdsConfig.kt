package com.duriseo.android.core.common.config

/**
 * 광고 설정 (D-186). app의 `BuildConfig`를 [create]로 걸러서 Hilt로 넘긴다.
 * `debug`는 Google 테스트 ID, `noAdsDebug`는 [enabled] = false, `release`는 `local.properties`의 값이다.
 *
 * [enabled]가 false면 SDK 초기화도 광고 요청도 하지 않고, 광고 자리도 그리지 않는다 (FR-AD-04 AC4).
 */
data class AdsConfig(
    val enabled: Boolean,
    val memoListNativeId: String,
    val anniversaryListNativeId: String,
    val menuBottomBannerId: String,
) {
    /** 광고 단위 ID는 비밀값은 아니지만 로그에 남길 이유가 없어 찍지 않는다. */
    override fun toString(): String = "AdsConfig(enabled=$enabled)"

    companion object {
        /** 광고를 쓰지 않는 설정 */
        val Disabled = AdsConfig(
            enabled = false,
            memoListNativeId = "",
            anniversaryListNativeId = "",
            menuBottomBannerId = "",
        )

        /**
         * 빌드가 켜 달라고 해도 단위 ID가 하나라도 비면 끈다 (release인데 `local.properties` 값이 빠진 경우, 결정 4).
         */
        fun create(
            enabled: Boolean,
            memoListNativeId: String,
            anniversaryListNativeId: String,
            menuBottomBannerId: String,
        ): AdsConfig {
            val allSet = memoListNativeId.isNotBlank() &&
                anniversaryListNativeId.isNotBlank() &&
                menuBottomBannerId.isNotBlank()
            return AdsConfig(
                enabled = enabled && allSet,
                memoListNativeId = memoListNativeId,
                anniversaryListNativeId = anniversaryListNativeId,
                menuBottomBannerId = menuBottomBannerId,
            )
        }
    }
}
