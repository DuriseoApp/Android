plugins {
    id("juco.android.feature")
}

android {
    namespace = "com.juco.duriseo_android.feature.home"
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.common)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
