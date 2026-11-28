package com.duriseo.android.core.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** 광고 SDK 시작 호출. 단위 테스트에서 가짜로 바꿀 수 있게 인터페이스로 둔다. */
interface MobileAdsSdk {
    /** 실제 SDK를 초기화한다. 백그라운드 스레드에서 부른다 (SDK 권장). */
    fun initialize()
}

/** `MobileAds.initialize(context)` (Google Mobile Ads SDK 시작 안내의 표준 호출) */
class GoogleMobileAdsSdk @Inject constructor(
    @ApplicationContext private val context: Context,
) : MobileAdsSdk {
    override fun initialize() {
        MobileAds.initialize(context)
    }
}
