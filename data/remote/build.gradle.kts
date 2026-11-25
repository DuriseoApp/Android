plugins {
    id("duriseo.android.library")
    id("duriseo.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.duriseo.android.data.remote"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.domain)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    // 프로필 사진: 인증 OkHttp로 받는 Coil ImageLoader (docs/MODULES.md 3장, 단위 8)
    implementation(libs.coil.core)
    implementation(libs.coil.network.okhttp)

    // 공지·업데이트 팝업의 Remote Config (docs/MODULES.md 3장, 단위 9). BoM이 버전을 정한다
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)

    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
