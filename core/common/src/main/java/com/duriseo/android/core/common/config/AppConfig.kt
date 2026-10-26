package com.duriseo.android.core.common.config

/**
 * 빌드 때 `local.properties`에서 읽어 `BuildConfig`로 넣은 설정값 (docs/SETUP.md 2장, ARCHITECTURE.md 6장).
 * `data:remote`는 app의 `BuildConfig`를 볼 수 없어서 app의 Hilt 모듈이 이 객체로 넘겨 준다.
 *
 * 값이 없으면 빈 문자열이다 (CI 빌드). 이때 그 기능은 동작하지 않는다.
 *
 * @property apiBaseUrl 서버 주소. **호스트까지만** 적는다 (`https://…`). `/api/v1/`은 앱이 붙인다 (API.md 1장)
 */
data class AppConfig(
    val apiBaseUrl: String,
    val googleWebClientId: String,
    val kakaoNativeAppKey: String,
) {
    /** 서버 주소가 설정돼 있는지 */
    val isApiConfigured: Boolean get() = apiBaseUrl.isNotBlank()

    /**
     * Retrofit 기본 주소 (호스트 + `/api/v1/`). 서버 주소가 비어 있으면 null.
     * 모든 경로는 `/api/v1` 아래에 있다 (API.md 1장).
     */
    fun apiV1BaseUrl(): String? {
        val host = apiBaseUrl.trim().trimEnd('/')
        if (host.isEmpty()) return null
        return "$host$API_V1_PATH"
    }

    /** 로그·크래시 보고에 값이 새지 않도록 값을 찍지 않는다 (NFR-SEC-01). */
    override fun toString(): String = "AppConfig(apiConfigured=$isApiConfigured)"

    private companion object {
        const val API_V1_PATH = "/api/v1/"
    }
}
