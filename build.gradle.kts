// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.aboutlibraries) apply false
}
// Robolectric은 테스트 때 android-all jar를 Gradle을 거치지 않고 직접 받는다(MavenArtifactFetcher).
// CI에서 Maven Central(repo1)로 받다가 IOException으로 자주 실패해, CI에서는 Google의 Maven Central 미러로 받는다.
// 맥에서는 그대로 Maven Central (docs/SETUP.md CI 절)
if (System.getenv("CI") == "true") {
    subprojects {
        tasks.withType<Test>().configureEach {
            systemProperty("robolectric.dependency.repo.id", "google-maven-central")
            systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2")
        }
    }
}
