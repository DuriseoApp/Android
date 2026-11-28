package com.duriseo.android.core.ads

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink700
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.Line200
import com.duriseo.android.core.designsystem.theme.Line300
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.core.designsystem.theme.Surface50
import com.duriseo.android.core.designsystem.theme.Brand100
import com.duriseo.android.core.designsystem.theme.Warning50
import com.duriseo.android.core.designsystem.theme.Warning800

// Preview 전용: 실제 광고(SDK) 없이 화면 Preview에서 광고 자리를 같은 모양으로 그린다.
// 앱 실행 코드에서는 쓰지 않는다. 글은 "[ ]"로 가짜임을 드러낸 자리표시자다.

/** Preview 전용 가짜 네이티브 광고 카드. 같은 토큰으로 `NativeAdCard`와 같은 모양을 Compose로 그린다. */
@Composable
fun NativeAdCardPreviewSample(style: NativeAdCardStyle, modifier: Modifier = Modifier) {
    when (style) {
        NativeAdCardStyle.Compact -> CompactSample(modifier)
        NativeAdCardStyle.Large -> LargeSample(modifier)
    }
}

/** Preview 전용 가짜 적응형 배너 (SCR-SET-01). 실제 배너는 SDK가 그린다. */
@Composable
fun AnchoredBannerAdPreviewSample(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AdDimens.BannerPreviewHeight)
            .background(Surface0)
            .border(BorderStroke(AdDimens.Border, Line100)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.padding(start = AdDimens.CompactGap)) { Badge() }
        Text(
            text = stringResource(R.string.ads_preview_headline),
            style = MaterialTheme.typography.titleSmall,
            color = Ink900,
            modifier = Modifier.padding(start = AdDimens.BadgeGap),
        )
    }
}

@Composable
private fun Badge() {
    Text(
        text = stringResource(R.string.ads_badge),
        style = MaterialTheme.typography.labelSmall,
        color = Warning800,
        modifier = Modifier
            .clip(RoundedCornerShape(AdDimens.BadgeRadius))
            .background(Warning50)
            .padding(horizontal = AdDimens.BadgePaddingH, vertical = AdDimens.BadgePaddingV),
    )
}

@Composable
private fun IconPlaceholder(size: Dp, radius: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(Line300),
    )
}

@Composable
private fun SampleButton(textRes: Int, height: Dp, radius: Dp, modifier: Modifier = Modifier, bold: Boolean = false) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(Surface0)
            .border(BorderStroke(AdDimens.Border, Line200), RoundedCornerShape(radius)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(textRes),
            style = if (bold) {
                MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.labelLarge
            },
            color = Ink900,
            modifier = Modifier.padding(horizontal = AdDimens.CompactButtonPadding),
        )
    }
}

@Composable
private fun CompactSample(modifier: Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AdDimens.CompactCardRadius))
            .background(Surface50)
            .padding(AdDimens.CompactPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AdDimens.CompactGap),
    ) {
        IconPlaceholder(AdDimens.CompactIcon, AdDimens.CompactIconRadius)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AdDimens.TextGap)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AdDimens.BadgeGap)) {
                Badge()
                Text(
                    text = stringResource(R.string.ads_preview_advertiser),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                    color = Ink600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(R.string.ads_preview_headline),
                style = MaterialTheme.typography.titleSmall,
                color = Ink900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.ads_preview_body_one_line),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = Ink600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SampleButton(
            textRes = R.string.ads_preview_cta_install,
            height = AdDimens.CompactButtonHeight,
            radius = AdDimens.CompactButtonRadius,
            modifier = Modifier.padding(top = AdDimens.CompactButtonTop),
        )
    }
}

@Composable
private fun LargeSample(modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AdDimens.LargeCardRadius))
            .background(Surface50)
            .padding(AdDimens.LargePadding),
        verticalArrangement = Arrangement.spacedBy(AdDimens.LargeGap),
    ) {
        Row(
            modifier = Modifier.padding(end = AdDimens.LargeHeaderEndPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AdDimens.LargeGap),
        ) {
            IconPlaceholder(AdDimens.LargeIcon, AdDimens.LargeIconRadius)
            Badge()
            Text(
                text = stringResource(R.string.ads_preview_advertiser),
                style = MaterialTheme.typography.labelLarge,
                color = Ink700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AdDimens.LargeMedia)
                .clip(RoundedCornerShape(AdDimens.LargeMediaRadius))
                .background(Brand100),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.ads_preview_media),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = Ink600,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(AdDimens.TextGap)) {
            Text(
                text = stringResource(R.string.ads_preview_headline),
                style = MaterialTheme.typography.titleSmall,
                color = Ink900,
            )
            Text(
                text = stringResource(R.string.ads_preview_body_two_lines),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = Ink600,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SampleButton(
            textRes = R.string.ads_preview_cta_detail,
            height = AdDimens.LargeButtonHeight,
            radius = AdDimens.LargeButtonRadius,
            modifier = Modifier.fillMaxWidth(),
            bold = true,
        )
    }
}

@Preview(showBackground = true, widthDp = 350)
@Composable
private fun NativeAdCardCompactPreview() {
    DuriseoTheme { NativeAdCardPreviewSample(NativeAdCardStyle.Compact, Modifier.padding(AdDimens.CompactPadding)) }
}

@Preview(showBackground = true, widthDp = 350)
@Composable
private fun NativeAdCardLargePreview() {
    DuriseoTheme { NativeAdCardPreviewSample(NativeAdCardStyle.Large, Modifier.padding(AdDimens.CompactPadding)) }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun AnchoredBannerAdPreview() {
    DuriseoTheme { AnchoredBannerAdPreviewSample() }
}
