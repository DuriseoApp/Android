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
 * tokens에 `letterSpacing`이 없는 스타일은 0으로 둔다 (design-system.md "Android 매핑": 보드도 지정하지 않음 = CSS 기본값).
 * 행간은 위아래 가운데 정렬(CSS line-height와 같은 모양)로 맞춘다.
 */
private fun plexStyle(
    size: TextUnit,
    lineHeight: TextUnit,
    weight: FontWeight,
    letterSpacing: TextUnit = 0.em,
    fontFamily: FontFamily = IbmPlexSansKr,
) = TextStyle(
    fontFamily = fontFamily,
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
 * design-system.md "Android 매핑"의 Typography 대응 11개. 남는 자리(displayMedium·displaySmall·
 * headlineLarge·headlineSmall)는 쓰지 않는다. `code-lg`·`code-md`는 [DuriseoTextStyles].
 */
val DuriseoTypography = Typography(
    // display 40/46 700, -0.02em (로그인 앱 이름)
    displayLarge = plexStyle(40.sp, 46.sp, FontWeight.Bold, (-0.02).em),
    // headline 26/35 700, -0.01em
    headlineMedium = plexStyle(26.sp, 35.sp, FontWeight.Bold, (-0.01).em),
    // title-lg 22/30 700, -0.01em
    titleLarge = plexStyle(22.sp, 30.sp, FontWeight.Bold, (-0.01).em),
    // title-md 17/24 700
    titleMedium = plexStyle(17.sp, 24.sp, FontWeight.Bold),
    // title-sm 15/22 700
    titleSmall = plexStyle(15.sp, 22.sp, FontWeight.Bold),
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

/**
 * M3 `Typography`에 고정폭 자리가 없어서 따로 둔 초대코드용 스타일 (`font.mono` IBM Plex Mono, 500).
 * `DuriseoTheme`가 제공하지 않고 `DuriseoTextStyles.CodeLg`처럼 바로 쓴다.
 */
object DuriseoTextStyles {
    /** code-lg 26/32 500 mono, 0.04em (초대코드 칸, InviteCreate.dc.html 32행) */
    val CodeLg: TextStyle = plexStyle(26.sp, 32.sp, FontWeight.Medium, 0.04.em, IbmPlexMono)

    /** code-md 24/30 500 mono (InviteEnter.dc.html 65행). letterSpacing은 토큰에 없어 0 */
    val CodeMd: TextStyle = plexStyle(24.sp, 30.sp, FontWeight.Medium, fontFamily = IbmPlexMono)
}
