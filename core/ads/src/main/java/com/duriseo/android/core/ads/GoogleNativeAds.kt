package com.duriseo.android.core.ads

import android.content.Context
import com.duriseo.android.core.common.Logger
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

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
                    // 광고 오류는 사용자에게 보이지 않는다. 개발 확인용 로그만 남긴다 (코드만, 개인 정보 없음)
                    Logger.d("native ad failed: slot=$slot code=${error.code}")
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
