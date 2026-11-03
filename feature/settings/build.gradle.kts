plugins {
    id("duriseo.android.feature")
}

android {
    namespace = "com.duriseo.android.feature.settings"
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.common)
    implementation(projects.domain)

    // AccountViewModel이 경로 인자를 SavedStateHandle.toRoute로 읽어 Bundle이 필요하다 (data:repository와 같은 버전)
    testImplementation(libs.robolectric)
}
