plugins {
    id("duriseo.android.feature")
}

android {
    namespace = "com.duriseo.android.feature.settings"
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.common)
    implementation(projects.domain)
}
