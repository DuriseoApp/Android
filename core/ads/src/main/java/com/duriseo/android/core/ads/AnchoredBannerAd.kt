package com.duriseo.android.core.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * 적응형 고정 배너 (FR-AD-03). 지금 폭에 맞춰 크기를 정하고, 받기 전·실패면 높이 0이라 자리가 없다 (FR-AD-04 AC1).
 * 시스템 내비게이션 위에 놓는 일은 호출하는 화면이 한다 (묶음 B).
 *
 * `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)`는 Google 적응형 배너 안내의 표준 호출이다.
 * 광고가 꺼져 있으면(`AdsConfig.enabled = false`) 이 함수를 호출하지 않는다.
 *
 * @param adUnitId 적응형 배너 광고 단위 ID (`NativeAdSlotsFactory.menuBottomBannerId`)
 */
@Composable
fun AnchoredBannerAd(
    adUnitId: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt()
        var loaded by remember(adUnitId, widthDp) { mutableStateOf(false) }
        val adSize = remember(adUnitId, widthDp) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        }
        val unitId = adUnitId
        val adView = remember(adUnitId, widthDp) {
            AdView(context).apply {
                setAdSize(adSize)
                // apply 안에서 `adUnitId`는 AdView의 속성을 가리키므로 바깥 값을 따로 잡아 둔다
                this.adUnitId = unitId
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        loaded = true
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loaded = false
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
        DisposableEffect(adView) {
            onDispose { adView.destroy() }
        }
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (loaded) adSize.height.dp else 0.dp),
            factory = { adView },
        )
    }
}
