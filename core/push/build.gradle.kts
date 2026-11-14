plugins {
    id("duriseo.android.library")
    id("duriseo.android.hilt")
}

android {
    namespace = "com.duriseo.android.core.push"

    testOptions {
        // Robolectric(알림 채널 테스트)가 리소스를 쓴다
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.designsystem)
    implementation(projects.domain)

    // FCM (docs/MODULES.md 3장). BoM이 버전을 정한다
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
