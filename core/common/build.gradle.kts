plugins {
    id("juco.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.juco.core.common"
}

dependencies {
    implementation(libs.timber)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
