plugins {
    id("duriseo.android.feature")
}

android {
    namespace = "com.duriseo.android.feature.anniversary"
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.common)
    implementation(projects.domain)

    // AnniversaryEditViewModel이 경로 인자를 SavedStateHandle.toRoute로 읽어 Bundle이 필요하다 (feature:calendar와 같다)
    testImplementation(libs.robolectric)
}
