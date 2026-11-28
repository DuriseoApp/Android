package com.duriseo.android.core.ads

import android.content.Context
import com.duriseo.android.core.common.config.AdsConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 화면의 ViewModel이 쓰는 [NativeAdSlots] 만들기 (묶음 B).
 * 광고가 꺼져 있으면(`enabled = false`) 요청을 하지 않는 보관함을 돌려준다.
 */
@Singleton
class NativeAdSlotsFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: AdsConfig,
) {
    /** 광고가 켜져 있는지. 꺼져 있으면 화면은 자리를 계산하지도 않는다 */
    val enabled: Boolean get() = config.enabled

    /** 메모 목록 네이티브 광고 단위 ID */
    val memoListNativeId: String get() = config.memoListNativeId

    /** 기념일 목록 네이티브 광고 단위 ID */
    val anniversaryListNativeId: String get() = config.anniversaryListNativeId

    /** 적응형 배너 광고 단위 ID (SCR-SET-01) */
    val menuBottomBannerId: String get() = config.menuBottomBannerId

    /** [adUnitId]의 광고를 자리마다 하나씩 받는 보관함. ViewModel 범위에서 만들고 `onCleared`에서 `clear()`한다 */
    fun create(adUnitId: String): NativeAdSlots<NativeAd> =
        if (config.enabled) googleNativeAdSlots(context, adUnitId) else googleNativeAdSlots(context, "")
}

/** [adUnitId]가 비면 요청하지 않는 보관함 (광고가 꺼진 경우) */
internal fun googleNativeAdSlots(context: Context, adUnitId: String): NativeAdSlots<NativeAd> = NativeAdSlots(
    requester = if (adUnitId.isNotBlank()) {
        GoogleNativeAdRequester(context.applicationContext, adUnitId)
    } else {
        AdRequester { _, onResult -> onResult(null) }
    },
    destroyer = { it.destroy() },
)

/**
 * `AdLoader.Builder(context, unitId).forNativeAd(...).withAdListener(...).build().loadAd(AdRequest)`
 * (Google 네이티브 광고 고급 안내의 표준 형태). 실패는 조용히 null로 넘긴다 (FR-AD-04 AC1, 오류 안내 없음).
 */
private class GoogleNativeAdRequester(
    private val context: Context,
    private val adUnitId: String,
) : AdRequester<NativeAd> {
    override fun request(slot: Int, onResult: (NativeAd?) -> Unit) {
        val loader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad -> onResult(ad) }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    // 광고 오류는 사용자에게 보이지 않는다. 개발 확인용 로그만 남긴다 (코드·문구만, 개인 정보 없음)
                    Timber.d("native ad failed: slot=%d code=%d", slot, error.code)
                    onResult(null)
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build(),
            )
            .build()
        loader.loadAd(AdRequest.Builder().build())
    }
}
