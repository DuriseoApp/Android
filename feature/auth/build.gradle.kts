plugins {
    id("duriseo.android.feature")
}

android {
    namespace = "com.duriseo.android.feature.auth"
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.common)
    implementation(projects.domain)

    // 테스트 도구(JUnit, 코루틴 테스트, Turbine)는 duriseo.android.feature 플러그인이 넣어 준다
}
