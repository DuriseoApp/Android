plugins {
    id("duriseo.android.feature")
}

android {
    namespace = "com.duriseo.android.feature.settings"
}

dependencies {
    implementation(projects.core.ads)
    implementation(projects.core.designsystem)
    implementation(projects.core.common)
    implementation(projects.domain)

    // 큰 사진을 줄일 때 EXIF 회전을 읽는다 (docs/MODULES.md 3장, 단위 8)
    implementation(libs.androidx.exifinterface)

    // 오픈소스 라이선스 JSON(aboutlibraries.json)을 읽는다 (SCR-SET-05)
    implementation(libs.kotlinx.serialization.json)

    // AccountViewModel이 경로 인자를 SavedStateHandle.toRoute로 읽어 Bundle이 필요하다 (data:repository와 같은 버전)
    testImplementation(libs.robolectric)
}
