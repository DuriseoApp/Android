plugins {
    id("duriseo.android.feature")
}

android {
    namespace = "com.duriseo.android.feature.memo"
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.common)
    implementation(projects.domain)

    // 테스트 도구(JUnit, 코루틴 테스트, Turbine)는 duriseo.android.feature 플러그인이 넣어 준다.
    // Robolectric: feature:anniversary·feature:calendar와 같이 ViewModel 테스트가 Android 클래스(리소스 ID, Bundle)를 쓴다
    testImplementation(libs.robolectric)
}
