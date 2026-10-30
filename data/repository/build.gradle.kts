plugins {
    id("duriseo.android.library")
    id("duriseo.android.hilt")
}

android {
    namespace = "com.duriseo.android.data.repository"

    testOptions {
        // Robolectric(메모리 Room 테스트)가 리소스를 쓴다
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.domain)
    implementation(projects.data.remote)
    implementation(projects.data.local)

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.retrofit)
    testImplementation(libs.retrofit.converter.kotlinx.serialization)
    testImplementation(libs.kotlinx.serialization.json)
    // 메모리 Room (DuriseoDatabase는 data:local의 implementation 의존이라 테스트가 직접 필요)
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.room.ktx)
    testImplementation(libs.androidx.datastore.preferences)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
