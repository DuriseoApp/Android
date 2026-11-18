package com.duriseo.android.feature.settings.licenses

import android.content.Context
import androidx.annotation.RawRes
import com.duriseo.android.core.common.config.LicensesJsonRes
import com.duriseo.android.core.common.coroutines.IoDispatcher
import com.duriseo.android.feature.settings.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [LicenseSource]의 Android 구현. 라이브러리 목록은 app 모듈에 AboutLibraries 플러그인이 만든 `res/raw/aboutlibraries.json`이다.
 * 그 리소스 ID는 app의 R에만 있어서(feature는 볼 수 없다) app의 `AppModule`이 컴파일 타임 참조(`R.raw.aboutlibraries`)로
 * [LicensesJsonRes] 한정자로 넘긴다. 문자열 이름 찾기는 쓰지 않는다. 읽기·해석이 실패하면 예외를 던지고 ViewModel이 글꼴 행만 보인다.
 * 글꼴 라이선스는 이 모듈의 `res/raw/ofl_1_1.txt`다.
 */
class AndroidLicenseSource @Inject constructor(
    @ApplicationContext private val context: Context,
    @LicensesJsonRes @RawRes private val librariesJsonRes: Int,
    @IoDispatcher private val io: CoroutineDispatcher,
) : LicenseSource {

    override suspend fun libraries(): List<LicenseEntry> = withContext(io) {
        val json = context.resources.openRawResource(librariesJsonRes).bufferedReader().use { it.readText() }
        parseAboutLibraries(json)
    }

    override suspend fun fontLicenseText(): String = withContext(io) {
        context.resources.openRawResource(R.raw.ofl_1_1).bufferedReader().use { it.readText() }.trim()
    }
}
