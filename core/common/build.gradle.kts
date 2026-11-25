plugins {
    id("duriseo.android.library")
    id("duriseo.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.duriseo.android.core.common"
}

dependencies {
    // 소셜 로그인 래퍼가 SocialProvider를 쓴다 (domain은 순수 Kotlin이라 core:common이 의존해도 순환이 없다)
    implementation(projects.domain)

    implementation(libs.timber)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.core.ktx)

    // 소셜 로그인 (docs/MODULES.md 3장): Google = Credential Manager + Google ID, 카카오 = Kakao SDK (user)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.id)
    implementation(libs.kakao.v2.user)

    // 인앱 업데이트 (D-151, docs/MODULES.md 3장). `app-update-ktx`가 `app-update`를 함께 가져온다
    implementation(libs.play.app.update.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
