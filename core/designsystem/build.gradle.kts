plugins {
    id("juco.android.library")
    id("juco.android.compose")
}

android {
    namespace = "com.juco.core.designsystem"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
}
