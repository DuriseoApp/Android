plugins {
    id("duriseo.android.library")
    id("duriseo.android.hilt")
}

android {
    namespace = "com.duriseo.android.data.local"
}

// Room 스키마 내보내기 위치. @Database 클래스는 첫 엔티티가 생기는 단위에서 만든다
// (엔티티 없는 @Database는 컴파일 오류, unit-1.md 결정 3).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
