plugins {
    id("duriseo.android.library")
    id("duriseo.android.hilt")
}

android {
    namespace = "com.duriseo.android.data.repository"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.domain)
    implementation(projects.data.remote)
    implementation(projects.data.local)

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.retrofit)
    testImplementation(libs.kotlinx.serialization.json)
}
