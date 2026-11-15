plugins {
    id("duriseo.android.feature")
}

android {
    namespace = "com.duriseo.android.feature.main"
}

dependencies {
    implementation(projects.feature.anniversary)
    implementation(projects.feature.auth)
    implementation(projects.feature.calendar)
    implementation(projects.feature.couple)
    implementation(projects.feature.memo)
    implementation(projects.feature.notification)
    implementation(projects.feature.settings)
    implementation(projects.core.designsystem)
    implementation(projects.core.common)
    implementation(projects.domain)
}
