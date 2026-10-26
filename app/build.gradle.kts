import java.util.Properties

plugins {
    id("duriseo.android.application")
    id("duriseo.android.compose")
    id("duriseo.android.hilt")
}

// local.properties (커밋 안 됨)의 설정값. 키 이름은 docs/SETUP.md 2장.
// 파일이나 키가 없으면 빈 문자열로 빌드한다 (CI). 그 기능만 동작하지 않는다.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun localProperty(key: String): String = localProperties.getProperty(key, "").trim()

/** BuildConfig 문자열 상수 값 (따옴표·역슬래시 이스케이프) */
fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.duriseo.android"

    defaultConfig {
        applicationId = "com.duriseo.android"
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()

        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", localProperty("duriseo.google.webClientId").asBuildConfigString())
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", localProperty("duriseo.kakao.nativeAppKey").asBuildConfigString())
    }

    buildTypes {
        // debug = 개발 서버, release = 운영 서버 (ARCHITECTURE.md 6장). applicationId는 둘 다 같다.
        getByName("debug") {
            buildConfigField("String", "API_BASE_URL", localProperty("duriseo.api.devUrl").asBuildConfigString())
        }
        getByName("release") {
            buildConfigField("String", "API_BASE_URL", localProperty("duriseo.api.prodUrl").asBuildConfigString())
        }
    }
}

dependencies {
    implementation(projects.feature.main)
    implementation(projects.core.designsystem)
    implementation(projects.core.common)
    // Hilt 바인딩(리포지토리, 원격, 로컬)을 앱 그래프에 모으기 위해
    implementation(projects.data.repository)

    implementation(libs.timber)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
}
