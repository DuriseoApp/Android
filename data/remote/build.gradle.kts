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

    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
