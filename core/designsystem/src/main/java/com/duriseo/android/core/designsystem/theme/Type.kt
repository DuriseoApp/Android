package com.duriseo.android.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.duriseo.android.core.designsystem.R

// 값의 기준은 ../Docs/design/tokens.json (font, typography.*)과
// design-system.md "Android (Jetpack Compose) 매핑"의 Typography 대응이다.
//
// 글꼴은 Google Downloadable Fonts로 받는다 (리드 결정 2026-10-26). 받기 전이나 오프라인이면
// 시스템 글꼴로 보인다. 오픈소스 고지(SIL OFL)는 오픈소스 라이선스 화면이 생기는 단위에서 한다.

private val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val IbmPlexSansKrName = GoogleFont("IBM Plex Sans KR")
private val IbmPlexMonoName = GoogleFont("IBM Plex Mono")

private fun sansKr(weight: FontWeight) = Font(
    googleFont = IbmPlexSansKrName,
    fontProvider = GoogleFontProvider,
    weight = weight,
)

/** tokens `font.sans`: 모든 UI 글자. 400·500·600·700 */
val IbmPlexSansKr = FontFamily(
    sansKr(FontWeight.Normal),
    sansKr(FontWeight.Medium),
    sansKr(FontWeight.SemiBold),
    sansKr(FontWeight.Bold),
)

/** tokens `font.mono`: 초대코드 전용. 500 */
val IbmPlexMono = FontFamily(
    Font(
        googleFont = IbmPlexMonoName,
        fontProvider = GoogleFontProvider,
        weight = FontWeight.Medium,
    ),
)

/**
 * tokens에 `letterSpacing`이 없는 스타일은 0으로 둔다 (CSS 기본값과 같음, unit-1.md 결정 6).
 * 행간은 위아래 가운데 정렬(CSS line-height와 같은 모양)로 맞춘다.
 */
private fun plexStyle(
    size: TextUnit,
    lineHeight: TextUnit,
    weight: FontWeight,
    letterSpacing: TextUnit = 0.em,
) = TextStyle(
    fontFamily = IbmPlexSansKr,
    fontSize = size,
    lineHeight = lineHeight,
    fontWeight = weight,
    letterSpacing = letterSpacing,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

/**
 * design-system.md 매핑 표에 있는 9개만 지정한다. 나머지(display·title-sm·code-lg·code-md)와
 * 표에 없는 M3 스타일은 Docs 보충(PR) 뒤에 더한다.
 * TODO(Docs PR): display, title-sm, code-lg, code-md 대응이 정해지면 추가.
 */
val DuriseoTypography = Typography(
    // headline 26/35 700, -0.01em
    headlineMedium = plexStyle(26.sp, 35.sp, FontWeight.Bold, (-0.01).em),
    // title-lg 22/30 700, -0.01em
    titleLarge = plexStyle(22.sp, 30.sp, FontWeight.Bold, (-0.01).em),
    // title-md 17/24 700
    titleMedium = plexStyle(17.sp, 24.sp, FontWeight.Bold),
    // body-lg 16/24 400
    bodyLarge = plexStyle(16.sp, 24.sp, FontWeight.Normal),
    // body-md 15/24 400
    bodyMedium = plexStyle(15.sp, 24.sp, FontWeight.Normal),
    // body-sm 14/22 400
    bodySmall = plexStyle(14.sp, 22.sp, FontWeight.Normal),
    // label 13/18 600
    labelLarge = plexStyle(13.sp, 18.sp, FontWeight.SemiBold),
    // caption 12/16 500
    labelMedium = plexStyle(12.sp, 16.sp, FontWeight.Medium),
    // micro 11/14 600
    labelSmall = plexStyle(11.sp, 14.sp, FontWeight.SemiBold),
)
