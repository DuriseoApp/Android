package com.duriseo.android.core.ads

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink700
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line200
import com.duriseo.android.core.designsystem.theme.Line300
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.core.designsystem.theme.Surface50
import com.duriseo.android.core.designsystem.theme.Warning50
import com.duriseo.android.core.designsystem.theme.Warning800
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/** 네이티브 광고 카드 모양 (FR-AD-01 AC2, FR-AD-02 AC1) */
enum class NativeAdCardStyle {
    /** 한 줄형: 아이콘, "광고" 배지 + 광고주, 제목, 설명 한 줄, 행동 버튼 (SCR-MEMO-01) */
    Compact,

    /** 큰 카드형: 헤더, 미디어(이미지·영상), 제목, 설명 두 줄, 행동 버튼 (SCR-ANNIV-01) */
    Large,
}

/**
 * 받은 네이티브 광고 하나를 그린다. SDK가 광고 영역(`NativeAdView`)의 자식 뷰에 등록된 것만 클릭·노출로 세므로
 * 카드는 Compose가 아니라 Android 뷰로 만들고 값은 디자인 토큰에서 가져온다 (AdChoices는 SDK가 오른쪽 위에 그린다).
 * 글꼴 파일(IBM Plex)은 뷰에 적용하지 않고 시스템 글꼴의 굵기만 맞춘다.
 *
 * 받지 못한 광고는 호출하지 않는다: 자리 자체를 만들지 않는다 (FR-AD-04 AC1).
 */
@Composable
fun NativeAdCard(
    ad: NativeAd,
    style: NativeAdCardStyle,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val typography = MaterialTheme.typography
    val look = remember(style, density, typography) { AdCardLook.of(style, density, typography) }
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context -> AdCardViews.create(context, style, look) },
        update = { view -> AdCardViews.bind(view, ad) },
    )
}

/** 뷰 쪽에서 쓸 색(ARGB)·글자·픽셀 값. 토큰을 한 번 풀어 둔다. */
internal class AdCardLook private constructor(
    val density: Density,
    val badge: TextStyle,
    val advertiser: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val button: TextStyle,
) {
    val card = Surface50.toArgb()
    val badgeBackground = Warning50.toArgb()
    val badgeText = Warning800.toArgb()
    val headlineText = Ink900.toArgb()
    val advertiserText = Ink600.toArgb()
    val advertiserTextLarge = Ink700.toArgb()
    val bodyText = Ink600.toArgb()
    val buttonText = Ink900.toArgb()
    val buttonBackground = Surface0.toArgb()
    val buttonBorder = Line200.toArgb()
    val iconPlaceholder = Line300.toArgb()

    fun px(dp: Dp): Int = with(density) { dp.roundToPx() }
    fun pxF(dp: Dp): Float = with(density) { dp.toPx() }

    companion object {
        fun of(style: NativeAdCardStyle, density: Density, t: androidx.compose.material3.Typography): AdCardLook = when (style) {
            NativeAdCardStyle.Compact -> AdCardLook(
                density = density,
                badge = t.labelSmall,
                advertiser = t.labelMedium.copy(fontWeight = FontWeight.Normal),
                headline = t.titleSmall,
                body = t.labelLarge.copy(fontWeight = FontWeight.Normal),
                button = t.labelLarge,
            )
            NativeAdCardStyle.Large -> AdCardLook(
                density = density,
                badge = t.labelSmall,
                advertiser = t.labelLarge,
                headline = t.titleSmall,
                body = t.labelLarge.copy(fontWeight = FontWeight.Normal),
                button = t.bodySmall.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

/** `NativeAdView` 안의 자산 뷰들. 뷰의 tag에 넣어 `update`에서 다시 찾는다. */
internal class AdCardHolder(
    val root: NativeAdView,
    val headline: TextView,
    val advertiser: TextView,
    val body: TextView,
    val callToAction: TextView,
    val icon: ImageView,
    val media: MediaView?,
    val iconPlaceholder: Int,
) {
    var boundAd: NativeAd? = null
}

internal object AdCardViews {

    fun create(context: Context, style: NativeAdCardStyle, look: AdCardLook): NativeAdView {
        val root = NativeAdView(context)
        val holder = when (style) {
            NativeAdCardStyle.Compact -> buildCompact(context, root, look)
            NativeAdCardStyle.Large -> buildLarge(context, root, look)
        }
        root.headlineView = holder.headline
        root.advertiserView = holder.advertiser
        root.bodyView = holder.body
        root.callToActionView = holder.callToAction
        root.iconView = holder.icon
        holder.media?.let { root.mediaView = it }
        root.tag = holder
        return root
    }

    /** 광고 값을 뷰에 넣고 SDK에 등록한다. 같은 광고면 다시 하지 않는다 (화면에 있는 동안 바꾸지 않음) */
    fun bind(view: NativeAdView, ad: NativeAd) {
        val holder = view.tag as AdCardHolder
        if (holder.boundAd === ad) return
        holder.boundAd = ad

        holder.headline.text = ad.headline
        holder.advertiser.showOrHide(ad.advertiser)
        holder.body.showOrHide(ad.body)
        holder.callToAction.showOrHide(ad.callToAction)
        val drawable = ad.icon?.drawable
        if (drawable != null) {
            holder.icon.setImageDrawable(drawable)
            holder.icon.setBackgroundColor(0)
        } else {
            holder.icon.setImageDrawable(null)
            holder.icon.setBackgroundColor(holder.iconPlaceholder)
        }
        holder.media?.let { media ->
            val content = ad.mediaContent
            if (content != null) media.mediaContent = content
        }
        // 등록은 값을 다 넣은 뒤에 마지막으로 한다
        view.setNativeAd(ad)
    }

    private fun TextView.showOrHide(value: String?) {
        if (value.isNullOrBlank()) {
            visibility = View.GONE
        } else {
            text = value
            visibility = View.VISIBLE
        }
    }

    // ---- 한 줄형 (AdDemoMemo 97~99행) ----

    private fun buildCompact(context: Context, root: NativeAdView, look: AdCardLook): AdCardHolder {
        root.background = rounded(look.card, look.pxF(AdDimens.CompactCardRadius))

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val p = look.px(AdDimens.CompactPadding)
            setPadding(p, p, p, p)
        }

        val icon = iconView(context, look, AdDimens.CompactIcon, AdDimens.CompactIconRadius)
        row.addView(icon, LinearLayout.LayoutParams(look.px(AdDimens.CompactIcon), look.px(AdDimens.CompactIcon)))

        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val badgeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        badgeRow.addView(badge(context, look))
        val advertiser = text(context, look.advertiser, look.advertiserText).apply {
            setPadding(look.px(AdDimens.BadgeGap), 0, 0, 0)
        }
        badgeRow.addView(advertiser, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        column.addView(badgeRow)

        val headline = text(context, look.headline, look.headlineText).topGap(look, AdDimens.TextGap)
        column.addView(headline)
        val body = text(context, look.body, look.bodyText).topGap(look, AdDimens.TextGap)
        column.addView(body)

        row.addView(
            column,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = look.px(AdDimens.CompactGap)
            },
        )

        val cta = button(context, look, AdDimens.CompactButtonRadius).apply {
            val h = look.px(AdDimens.CompactButtonPadding)
            setPadding(h, 0, h, 0)
        }
        row.addView(
            cta,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, look.px(AdDimens.CompactButtonHeight)).apply {
                marginStart = look.px(AdDimens.CompactGap)
                topMargin = look.px(AdDimens.CompactButtonTop)
            },
        )

        root.addView(row, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return AdCardHolder(root, headline, advertiser, body, cta, icon, null, look.iconPlaceholder)
    }

    // ---- 큰 카드형 (AdDemoAnniversaries 77~81행) ----

    private fun buildLarge(context: Context, root: NativeAdView, look: AdCardLook): AdCardHolder {
        root.background = rounded(look.card, look.pxF(AdDimens.LargeCardRadius))

        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val p = look.px(AdDimens.LargePadding)
            setPadding(p, p, p, p)
        }
        val gap = look.px(AdDimens.LargeGap)

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            // AdChoices(오른쪽 위)와 겹치지 않게 비운다
            setPaddingRelative(0, 0, look.px(AdDimens.LargeHeaderEndPadding), 0)
        }
        val icon = iconView(context, look, AdDimens.LargeIcon, AdDimens.LargeIconRadius)
        header.addView(icon, LinearLayout.LayoutParams(look.px(AdDimens.LargeIcon), look.px(AdDimens.LargeIcon)))
        header.addView(
            badge(context, look),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = gap
            },
        )
        val advertiser = text(context, look.advertiser, look.advertiserTextLarge).apply {
            setPadding(look.px(AdDimens.BadgeGap), 0, 0, 0)
        }
        header.addView(advertiser, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        column.addView(header)

        val media = MediaView(context).apply {
            setImageScaleType(ImageView.ScaleType.CENTER_CROP)
            clipToOutline = true
            outlineProvider = roundedOutline(look.pxF(AdDimens.LargeMediaRadius))
        }
        column.addView(
            media,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, look.px(AdDimens.LargeMedia)).apply {
                topMargin = gap
            },
        )

        val headline = text(context, look.headline, look.headlineText).topGap(look, AdDimens.LargeGap)
        column.addView(headline)
        val body = text(context, look.body, look.bodyText, maxLines = 2).topGap(look, AdDimens.TextGap)
        column.addView(body)

        val cta = button(context, look, AdDimens.LargeButtonRadius)
        column.addView(
            cta,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, look.px(AdDimens.LargeButtonHeight)).apply {
                topMargin = gap
            },
        )

        root.addView(column, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return AdCardHolder(root, headline, advertiser, body, cta, icon, media, look.iconPlaceholder)
    }

    // ---- 부품 ----

    private fun TextView.topGap(look: AdCardLook, dp: Dp): TextView = apply {
        setPadding(paddingLeft, look.px(dp), paddingRight, paddingBottom)
    }

    private fun text(context: Context, style: TextStyle, color: Int, maxLines: Int = 1): TextView =
        TextView(context).apply {
            applyStyle(style)
            setTextColor(color)
            this.maxLines = maxLines
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

    /** "광고" 배지: `warning-50` 면 + `warning-800` 글자 (FR-AD-04 AC2). 낭독 순서상 광고주보다 먼저 온다 */
    private fun badge(context: Context, look: AdCardLook): TextView =
        TextView(context).apply {
            applyStyle(look.badge)
            text = context.getString(R.string.ads_badge)
            setTextColor(look.badgeText)
            background = rounded(look.badgeBackground, look.pxF(AdDimens.BadgeRadius))
            setPadding(
                look.px(AdDimens.BadgePaddingH),
                look.px(AdDimens.BadgePaddingV),
                look.px(AdDimens.BadgePaddingH),
                look.px(AdDimens.BadgePaddingV),
            )
        }

    private fun button(context: Context, look: AdCardLook, radius: Dp): TextView =
        TextView(context).apply {
            applyStyle(look.button)
            setTextColor(look.buttonText)
            gravity = Gravity.CENTER
            maxLines = 1
            background = GradientDrawable().apply {
                setColor(look.buttonBackground)
                cornerRadius = look.pxF(radius)
                setStroke(look.px(AdDimens.Border), look.buttonBorder)
            }
        }

    private fun iconView(context: Context, look: AdCardLook, size: Dp, radius: Dp): ImageView =
        ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = roundedOutline(look.pxF(radius))
            setBackgroundColor(look.iconPlaceholder)
            // 장식이 아니라 광고 자산이지만 낭독 대상은 제목이므로 아이콘은 읽지 않는다
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            minimumWidth = look.px(size)
            minimumHeight = look.px(size)
        }

    private fun rounded(color: Int, radiusPx: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radiusPx
    }

    private fun roundedOutline(radiusPx: Float) = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: android.graphics.Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
        }
    }

    /** Compose `TextStyle`의 크기(sp)·굵기를 뷰에 옮긴다. 줄 높이는 뷰 기본값을 쓴다 */
    private fun TextView.applyStyle(style: TextStyle) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, style.fontSize.value)
        val bold = (style.fontWeight?.weight ?: FontWeight.Normal.weight) >= FontWeight.SemiBold.weight
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
}
