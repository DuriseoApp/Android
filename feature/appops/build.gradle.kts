plugins {
    id("duriseo.android.feature")
}

android {
    namespace = "com.duriseo.android.feature.appops"
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.common)
    implementation(projects.domain)

    // https:// 팝업 이미지를 그린다. 로더는 core:common의 @PopupImageLoader (토큰·공통 헤더 없는 OkHttp, 미리 받은 캐시를 같이 씀)
    implementation(libs.coil.core)
    implementation(libs.coil.compose)

    // 테스트 도구(JUnit, 코루틴 테스트, Turbine)는 duriseo.android.feature 플러그인이 넣어 준다
    // 앱 종료·업데이트 효과가 Activity를 받아서 Robolectric으로 돈다
    testImplementation(libs.robolectric)
}
