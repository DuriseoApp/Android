import java.util.Properties

plugins {
    id("duriseo.android.application")
    id("duriseo.android.compose")
    id("duriseo.android.hilt")
    // 오픈소스 라이선스 목록 JSON을 빌드 때 만든다 (SCR-SET-05, D-152). 설정은 기본값 그대로
    alias(libs.plugins.aboutlibraries)
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

/** Google 공개 테스트 광고 ID (공개 값이라 코드에 둔다, D-186). debug·noAdsDebug가 쓴다 */
object GoogleTestAdIds {
    const val APP = "ca-app-pub-3940256099942544~3347511713"
    const val NATIVE = "ca-app-pub-3940256099942544/2247696110"
    const val ADAPTIVE_BANNER = "ca-app-pub-3940256099942544/9214589741"
}

/** 광고 BuildConfig 4개와 매니페스트 앱 ID 자리표시자(`admobAppId`)를 한 번에 넣는다 */
fun com.android.build.api.dsl.ApplicationBuildType.adsBuildConfig(
    enabled: Boolean,
    appId: String,
    memoListNativeId: String,
    anniversaryListNativeId: String,
    menuBottomBannerId: String,
) {
    buildConfigField("boolean", "ADS_ENABLED", enabled.toString())
    buildConfigField("String", "ADMOB_MEMO_LIST_NATIVE_ID", memoListNativeId.asBuildConfigString())
    buildConfigField("String", "ADMOB_ANNIVERSARY_LIST_NATIVE_ID", anniversaryListNativeId.asBuildConfigString())
    buildConfigField("String", "ADMOB_MENU_BOTTOM_BANNER_ID", menuBottomBannerId.asBuildConfigString())
    manifestPlaceholders["admobAppId"] = appId
}

// Firebase(FCM) 설정 파일. 비밀값이라 커밋하지 않는다 (docs/SETUP.md). 파일이 있을 때만 google-services 플러그인을 적용한다.
// 없으면(CI 등) FCM이 꺼진 채 빌드되고, 앱은 토큰 등록·푸시 수신 없이 동작한다 (core:push `FcmAvailability`).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.duriseo.android"

    defaultConfig {
        applicationId = "com.duriseo.android"
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()

        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", localProperty("duriseo.google.webClientId").asBuildConfigString())
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", localProperty("duriseo.kakao.nativeAppKey").asBuildConfigString())

        // 카카오 로그인 리다이렉트 scheme `kakao<네이티브 앱 키>`를 매니페스트에 넣는다. 키가 비면(CI) scheme이 `kakao`인
        // 채로 빌드되고 카카오 로그인만 동작하지 않는다. 키 값은 레포에 쓰지 않는다 (unit-2.md G절).
        manifestPlaceholders["kakaoNativeAppKey"] = localProperty("duriseo.kakao.nativeAppKey")
    }

    buildTypes {
        // debug = 개발 서버 + Google 테스트 광고, noAdsDebug = debug와 같고 광고 없음, release = 운영 서버 + 실제 광고
        // (ARCHITECTURE.md 6장, D-186). applicationId는 셋 다 같다.
        getByName("debug") {
            buildConfigField("String", "API_BASE_URL", localProperty("duriseo.api.devUrl").asBuildConfigString())
            adsBuildConfig(
                enabled = true,
                appId = GoogleTestAdIds.APP,
                memoListNativeId = GoogleTestAdIds.NATIVE,
                anniversaryListNativeId = GoogleTestAdIds.NATIVE,
                menuBottomBannerId = GoogleTestAdIds.ADAPTIVE_BANNER,
            )
        }
        // debug 설정(서버 주소, 서명, 디버그 가능)을 복사한 뒤 광고만 끈다. 라이브러리 모듈에는 noAdsDebug가 없으니 debug를 쓴다
        create("noAdsDebug") {
            initWith(getByName("debug"))
            matchingFallbacks += listOf("debug")
            signingConfig = signingConfigs.getByName("debug")
            adsBuildConfig(
                enabled = false,
                appId = GoogleTestAdIds.APP,
                memoListNativeId = GoogleTestAdIds.NATIVE,
                anniversaryListNativeId = GoogleTestAdIds.NATIVE,
                menuBottomBannerId = GoogleTestAdIds.ADAPTIVE_BANNER,
            )
        }
        getByName("release") {
            buildConfigField("String", "API_BASE_URL", localProperty("duriseo.api.prodUrl").asBuildConfigString())
            // 값이 하나라도 비면 광고를 끈다 (AdsConfig.create, 결정 4). 앱 ID가 비면 매니페스트에는 테스트 앱 ID를 넣어 SDK가 죽지 않게 한다
            val releaseAppId = localProperty("duriseo.admob.appId")
            adsBuildConfig(
                enabled = releaseAppId.isNotBlank(),
                appId = releaseAppId.ifBlank { GoogleTestAdIds.APP },
                memoListNativeId = localProperty("duriseo.admob.memoListNativeId"),
                anniversaryListNativeId = localProperty("duriseo.admob.anniversaryListNativeId"),
                menuBottomBannerId = localProperty("duriseo.admob.menuBottomBannerId"),
            )
        }
    }
}

dependencies {
    implementation(projects.feature.main)
    implementation(projects.core.designsystem)
    implementation(projects.core.common)
    implementation(projects.core.push)
    implementation(projects.core.ads)
    implementation(projects.domain)
    // Hilt 바인딩(리포지토리, 원격, 로컬)을 앱 그래프에 모으기 위해
    implementation(projects.data.repository)

    // 프로필 사진 이미지 로더를 앱 전체 로더로 등록 (SingletonImageLoader.Factory)
    implementation(libs.coil.singleton)

    implementation(libs.timber)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
}
