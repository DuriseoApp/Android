plugins {
    id("duriseo.android.library")
    id("duriseo.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.duriseo.android.data.local"

    testOptions {
        // Robolectric(메모리 Room 테스트)가 리소스를 쓴다
        unitTests.isIncludeAndroidResources = true
    }
}

// Room 스키마 내보내기 위치. 스키마 JSON(`schemas/`)은 빌드가 만들고 커밋한다 (unit-2.md H절).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // TokenStorage 인터페이스(domain), Logger·@IoDispatcher(core:common)
    implementation(projects.domain)
    implementation(projects.core.common)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // 토큰 암호화 (AES256_GCM, Android Keystore 마스터 키)
    implementation(libs.tink.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
