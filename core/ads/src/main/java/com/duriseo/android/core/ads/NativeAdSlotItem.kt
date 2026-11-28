package com.duriseo.android.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.nativead.NativeAd

/**
 * 화면(Composable)이 들고 있는 네이티브 광고 보관함. `adUnitId`가 null이면(광고가 꺼진 빌드) null을 돌려주고 아무것도 요청하지 않는다.
 * 화면이 컴포지션에서 빠지면(떠나면) 받은 광고를 모두 destroy한다 (FR-AD-04, 결정 6).
 * ViewModel이 SDK 객체(`NativeAd`)를 들지 않도록 Compose 쪽에 둔다 (결정 10).
 */
@Composable
fun rememberNativeAdSlots(adUnitId: String?): NativeAdSlots<NativeAd>? {
    val context = LocalContext.current
    val slots = remember(adUnitId) {
        if (adUnitId.isNullOrBlank()) null else googleNativeAdSlots(context, adUnitId)
    }
    DisposableEffect(slots) {
        onDispose { slots?.clear() }
    }
    return slots
}

/**
 * 자리 번호 [slot]의 광고를 받아(한 번만) 받은 뒤에만 그린다. 받는 중·실패면 아무것도 그리지 않는다 (FR-AD-04 AC1).
 * [modifier]는 카드에만 붙으므로 여백을 넣어도 광고가 없으면 자리가 생기지 않는다.
 */
@Composable
fun NativeAdSlotItem(
    slots: NativeAdSlots<NativeAd>?,
    slot: Int,
    style: NativeAdCardStyle,
    modifier: Modifier = Modifier,
) {
    if (slots == null) return
    LaunchedEffect(slots, slot) { slots.request(slot) }
    val ads by slots.ads.collectAsState()
    ads[slot]?.let { NativeAdCard(ad = it, style = style, modifier = modifier) }
}
