plugins {
    id("duriseo.android.library")
    id("duriseo.android.compose")
    id("duriseo.android.hilt")
}

android {
    namespace = "com.duriseo.android.core.ads"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.designsystem)

    // Google Mobile Ads SDK (docs/MODULES.md 3장). feature가 NativeAd 타입을 쓰므로 api로 연다 (feature는 core:ads만 안다)
    api(libs.play.services.ads)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
