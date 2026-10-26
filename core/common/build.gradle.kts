plugins {
    id("duriseo.android.library")
    id("duriseo.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.duriseo.android.core.common"
}

dependencies {
    implementation(libs.timber)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
}
