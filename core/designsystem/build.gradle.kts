plugins {
    id("duriseo.android.library")
    id("duriseo.android.compose")
}

android {
    namespace = "com.duriseo.android.core.designsystem"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
}
