plugins {
    id("juco.android.library")
    id("juco.android.compose")
}

android {
    namespace = "com.juco.duriseo.core.designsystem"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
}
