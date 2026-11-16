package com.duriseo.android.feature.settings.licenses

/** 라이선스 목록의 출처. 실제 구현은 [AndroidLicenseSource], 테스트는 Fake를 쓴다 */
interface LicenseSource {
    /** 빌드 때 만들어진 의존성 라이선스. 못 읽으면 예외를 던진다 (ViewModel이 글꼴 행만 보인다) */
    suspend fun libraries(): List<LicenseEntry>

    /** 글꼴(IBM Plex)의 SIL OFL 1.1 전문 (앱 리소스) */
    suspend fun fontLicenseText(): String
}
