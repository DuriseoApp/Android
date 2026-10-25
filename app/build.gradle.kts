plugins {
    id("duriseo.android.application")
    id("duriseo.android.compose")
    id("duriseo.android.hilt")
}

android {
    namespace = "com.duriseo.android"

    defaultConfig {
        applicationId = "com.duriseo.android"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(projects.feature.main)
    implementation(projects.core.designsystem)
    implementation(projects.core.common)

    implementation(libs.timber)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
