package com.duriseo.android.core.common.social

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Compose 화면(Route)이 [SocialTokenProvider]를 얻는 통로. ViewModel은 Activity도 소셜 SDK도 들고 있지 않고,
 * Route가 현재 Activity로 SDK를 열어 결과(토큰·취소·실패)만 ViewModel에 Action으로 넘긴다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SocialTokenEntryPoint {
    fun socialTokenProvider(): SocialTokenProvider
}

/** 앱에 바인딩된 [SocialTokenProvider]. 화면(Route)에서 `LocalContext.current.socialTokenProvider()`로 쓴다. */
fun Context.socialTokenProvider(): SocialTokenProvider =
    EntryPointAccessors.fromApplication(applicationContext, SocialTokenEntryPoint::class.java).socialTokenProvider()

/** 이 Context를 감싼 Activity. 소셜 SDK를 열려면 Activity가 필요하다. 없으면 null */
fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
