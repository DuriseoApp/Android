# core:designsystem

- 값의 기준은 `../Docs/design/tokens.json`, 대응 규칙은 `../Docs/design/design-system.md`의 "Android (Jetpack Compose) 매핑"이다. 반영은 `/sync-tokens` 절차로 한다.
- **값을 지어내지 않는다.** 토큰에 없는 색·크기는 추가하지 않고 멈추고 보고한다 (토큰 추가는 Docs의 `/design-sync`).
- 테마 이름은 `DuriseoTheme`. 색 상수는 `Color.kt`, Material 3 역할 매핑은 `Theme.kt`(표에 있는 역할만). 화면은 `MaterialTheme.colorScheme`·`typography`·`shapes`로 읽는다. 사람 색은 `ColorScheme`에 넣지 않고 `PersonColors` + `LocalPersonColors`로 제공한다.
- 공용 컴포넌트(버튼, 다이얼로그, 아래쪽 시트, 날짜 시트, 앱바, 배너, 메뉴 행, 아바타, 커플 표시, 스낵바, 하단 탭)는 design-system.md "컴포넌트"를 기준으로 여기 둔다. 기능 로직을 넣지 않는다. 한 모듈에서만 쓰는 부품(캘린더의 FAB 등)은 그 feature 모듈에 둔다 (`docs/MODULES.md`).
- 다크 모드는 design-system.md에 정의되기 전까지 만들지 않는다.
- 컴포넌트마다 `@Preview`를 둔다.
- **하단 시트·다이얼로그·요청 중 덮개는 반드시 `DuriseoBottomSheetLayer`·`DuriseoDialogLayer`(`DuriseoDialog`)·`DuriseoLoadingLayer`로만 그린다.** 이 레이어는 별도 창(`DuriseoOverlayWindow`)에 그려 하단 탭·시스템 바 위에 뜬다. 화면 안에 scrim `Box`나 M3 `ModalBottomSheet`·`AlertDialog`를 직접 쓰지 않는다 (`OverlayRulesTest`가 막는다). 새 덮개 종류가 필요하면 여기에 레이어로 추가한다.
- 덮개 창은 뒤로 가기를 직접 받는다(`onBackPress`/`onDismissRequest`). 호출부의 `BackHandler`는 창이 떠 있는 동안 닿지 않는다. 창 위에도 앱 스낵바가 보이게 `LocalOverlaySnackbar`를 쓴다 (D-126).
