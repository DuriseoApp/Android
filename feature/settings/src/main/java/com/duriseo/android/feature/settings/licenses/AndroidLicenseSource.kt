package com.duriseo.android.feature.settings.licenses

import android.content.Context
import com.duriseo.android.core.common.coroutines.IoDispatcher
import com.duriseo.android.feature.settings.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [LicenseSource]의 Android 구현. 라이브러리 목록은 app 모듈에 AboutLibraries 플러그인이 만든 `res/raw/aboutlibraries.json`이다.
 * 그 리소스는 app의 R에만 있어서(feature는 볼 수 없다) 이름으로 찾는다. 없으면(플러그인이 못 만든 빌드) 빈 목록이다.
 * 글꼴 라이선스는 이 모듈의 `res/raw/ofl_1_1.txt`다.
 */
class AndroidLicenseSource @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : LicenseSource {

    override suspend fun libraries(): List<LicenseEntry> = withContext(io) {
        val resId = context.resources.getIdentifier(LIBRARIES_RESOURCE, "raw", context.packageName)
        if (resId == 0) {
            emptyList()
        } else {
            val json = context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
            parseAboutLibraries(json)
        }
    }

    override suspend fun fontLicenseText(): String = withContext(io) {
        context.resources.openRawResource(R.raw.ofl_1_1).bufferedReader().use { it.readText() }.trim()
    }

    private companion object {
        const val LIBRARIES_RESOURCE = "aboutlibraries"
    }
}
