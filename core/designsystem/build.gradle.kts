plugins {
    id("duriseo.android.library")
    id("duriseo.android.compose")
}

android {
    namespace = "com.duriseo.android.core.designsystem"

    testOptions {
        // Compose UI 테스트(Robolectric)가 리소스와 문구를 읽는다
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    // 오류(DataError) -> 안내 문구 도우미
    implementation(projects.domain)

    // Google Downloadable Fonts (IBM Plex Sans KR / IBM Plex Mono)
    implementation(libs.androidx.compose.ui.text.google.fonts)

    // 프로필 사진을 원형으로 그리는 아바타 (docs/MODULES.md 3장, 단위 8)
    implementation(libs.coil.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
