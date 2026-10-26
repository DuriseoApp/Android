plugins {
    id("duriseo.kotlin.jvm")
}

dependencies {
    // Flow (NetworkMonitor 등). Android·data 모듈에는 의존하지 않는다.
    implementation(libs.kotlinx.coroutines.core)
}
