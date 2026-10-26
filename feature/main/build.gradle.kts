plugins {
    id("duriseo.android.feature")
}

android {
    namespace = "com.duriseo.android.feature.main"
}

dependencies {
    implementation(projects.feature.calendar)
    implementation(projects.feature.settings)
    implementation(projects.core.designsystem)
    implementation(projects.core.common)
}
