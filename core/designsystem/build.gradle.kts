plugins {
    id("duriseo.android.library")
    id("duriseo.android.compose")
}

android {
    namespace = "com.duriseo.android.core.designsystem"
}

dependencies {
    // 오류(DataError) -> 안내 문구 도우미
    implementation(projects.domain)

    // Google Downloadable Fonts (IBM Plex Sans KR / IBM Plex Mono)
    implementation(libs.androidx.compose.ui.text.google.fonts)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
}
