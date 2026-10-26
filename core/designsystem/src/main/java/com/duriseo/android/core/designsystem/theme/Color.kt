package com.duriseo.android.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// 값의 기준은 ../Docs/design/tokens.json (color.*). 값을 지어내거나 바꾸지 않는다.
// 주석은 tokens.json `usage`를 줄인 것이다.

/** brand-50: 가장 옅은 브랜드 면. 고정된 핀 카드 배경. 위 글자는 ink-900/ink-700. */
val Brand50 = Color(0xFFF3F0FD)

/** brand-100: 선택된 하단 탭 알약, 토널 강조 배경. 위 아이콘·글자는 brand-700. */
val Brand100 = Color(0xFFECE6FC)

/** brand-500: Primary. FAB, 주요 버튼, 선택된 날짜, 켜진 스위치, 포커스 테두리. 위 글자는 on-brand. */
val Brand500 = Color(0xFF6D4AE0)

/** brand-700: 흰 배경 위 브랜드색 글자·링크, 선택 탭 아이콘. */
val Brand700 = Color(0xFF4A2FC0)

/** on-brand: brand-500, danger-700, new-600 등 진한 채움 위의 글자·아이콘. */
val OnBrand = Color(0xFFFFFFFF)

/** person-me: '나'의 기본 색. 사용자가 바꿀 수 있어서 화면은 [PersonColors]로 읽는다. */
val PersonMe = Color(0xFFE0612F)

/** person-partner: '상대'의 기본 색. 사용자가 바꿀 수 있다. */
val PersonPartner = Color(0xFF2F6FDB)

/** person-together: '함께' 일정의 기본 색. 브랜드 UI에는 쓰지 말고 [Brand500]을 쓴다. */
val PersonTogether = Color(0xFF6D4AE0)

/** ink-900: 기본 글자, 아이콘, 어두운 버튼·선택된 필터 칩 채움. */
val Ink900 = Color(0xFF1C1A22)

/** ink-700: 카드 안 본문. */
val Ink700 = Color(0xFF4A4553)

/** ink-600: 보조 글자(입력 라벨, 부제, 메타 정보). */
val Ink600 = Color(0xFF5B5666)

/** ink-500: 3차 글자(캡션, 날짜 메타, placeholder, 비활성 탭 라벨). surface-0 위에서만 쓴다. */
val Ink500 = Color(0xFF77727F)

/** ink-400: 꺼진 상태 아이콘, 목록 화살표. 그래픽 전용, 글자에는 쓰지 않는다. */
val Ink400 = Color(0xFF8E8996)

/** ink-300: 점선 테두리, 빈 체크박스, 지난달 날짜 숫자(비활성). */
val Ink300 = Color(0xFFB3AEBB)

/** line-300: 텍스트 필드 기본 테두리, 꺼진 스위치 트랙. */
val Line300 = Color(0xFFD9D4DF)

/** line-200: 아웃라인 버튼·칩·카드 테두리. 장식용 경계. */
val Line200 = Color(0xFFE6E2EA)

/** line-100: 구분선, 하단 탭 상단선, 비활성 버튼·입력 채움. */
val Line100 = Color(0xFFEEEBF1)

/** surface-0: 화면 배경, 다이얼로그, 입력창, 아웃라인 버튼 면. */
val Surface0 = Color(0xFFFFFFFF)

/** surface-50: 일정 카드, 메모 카드 배경. */
val Surface50 = Color(0xFFF7F6F9)

/** surface-100: 세그먼트 탭 트랙, 초대코드 패널, 정보 카드. 위 보조 글자는 ink-600. */
val Surface100 = Color(0xFFF5F3F7)

/**
 * scrim: 다이얼로그·바텀시트 뒤를 덮는 막. `rgba(28, 26, 34, 0.48)`.
 * 알파 0.48 × 255 = 122.4 → 122 = 0x7A 이므로 ARGB 0x7A1C1A22.
 */
val Scrim = Color(0x7A1C1A22)

/** danger-600: 일요일 날짜 숫자, 입력 오류 테두리·메시지. */
val Danger600 = Color(0xFFC93545)

/** danger-700: 파괴적 행동(로그아웃, 연결 해제) 버튼 글자·채움. */
val Danger700 = Color(0xFFB42335)

/** warning-50: 주의 배너 배경. */
val Warning50 = Color(0xFFFDF1E8)

/** warning-700: 인라인 주의 문구. */
val Warning700 = Color(0xFFA8411A)

/** warning-800: warning-50 위 배너 글자. */
val Warning800 = Color(0xFF8A3A12)

/** new-600: NEW 배지 채움. MVP에서는 쓰지 않음 (PRD D-41). */
val New600 = Color(0xFFC9481A)

/** info-700: 읽음 표시. MVP에서는 쓰지 않음 (PRD D-41). */
val Info700 = Color(0xFF1F55B5)

/** favorite-600: 즐겨찾기 켜진 별 채움·테두리. */
val Favorite600 = Color(0xFFC27803)
