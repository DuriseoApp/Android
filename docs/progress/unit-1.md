# 단위 1: 기반

> 브랜치: `feat/1-foundation` · PR: https://github.com/DuriseoApp/Android/pull/8 · 상태: 리뷰 중
> 기준: PRD v0.7 (Docs main, Docs PR #8 머지됨), SCREENS.md(캔버스 버전 38) 공통 표, design-system.md(하단 탭·스낵바 명세, Android 매핑 포함), tokens.json(`$meta.updated` 2026-10-18), API.md 1장·1.1·5장 (main)
> PRD v0.7(D-81 refresh 토큰 형식, D-82 글자 수 기준)은 인증·입력 제한 변경이라 단위 1 범위에는 영향이 없다.

단위 1은 기반만 만든다. 인증 화면(단위 2)과 기능 화면(SCR-CAL·MEMO 등)은 만들지 않는다. 캘린더·메모 탭 내용은 문구 없는 빈 자리표시 화면이다 (문서에 없는 문구를 만들지 않기 위해).

## 범위

### A. 템플릿 정리 (`docs/UNIT1-TEMPLATE-CLEANUP.md`)

| ID | 내용 | 테스트 |
| --- | --- | --- |
| 정리-1 | `feature:home` → `feature:calendar`: 폴더, `settings.gradle.kts`, namespace `com.duriseo.android.feature.calendar`, 패키지, `HomeRoute/HomeScreen/HomeViewModel` → `CalendarRoute/CalendarScreen/CalendarViewModel`, `homeNavGraph` → `calendarNavGraph`, `MainRouteModel.Home` → `MainRouteModel.Calendar`. 화면 안 템플릿 문구 `"Home"` 삭제 (빈 자리표시) | 빌드로 확인. Preview `CalendarScreenPlaceholderPreview` |
| 정리-2 | `feature:menu` → `feature:settings` (같은 방법, `MainRouteModel.Menu` → `MainRouteModel.Settings`). 하단 탭에서 빼고 NavHost에만 등록 (진입은 캘린더 앱바, 단위 4·SCR-SET-01). 단위 1에서는 들어갈 길이 없다 | 빌드로 확인. Preview `SettingsScreenPlaceholderPreview` |
| 정리-3 | `feature:main`: `MainMenu`(HOME·MENU) → `MainTab`(CALENDAR·MEMO). 메모 탭은 `feature:memo`(단위 6) 전까지 `feature:main` 안의 빈 자리표시 `MemoTabPlaceholder`. `MainNavigator.navigate()` 안에 잘못 들어간 지역 함수(`popBackStack`, `popAllBackStack`) 정리, `MainScreen`의 쓰지 않는 `context` 인자 정리, 스낵바 메시지를 `String` 대신 `@StringRes`로 | C절 테스트 |
| 정리-4 | 템플릿 테스트 삭제: 7개 모듈의 `ExampleUnitTest`·`ExampleInstrumentedTest` (app, core:common, core:designsystem, data:remote, feature:main·home·menu). androidTest 폴더가 비면 `androidTestImplementation` 줄도 뺀다 | — |
| 정리-5 | 템플릿 리소스 삭제: `core:designsystem` `res/font/pretendard_*.otf` 3개, `res/drawable/ic_home_*`·`ic_setting_*` 4개, `app/res/values/colors.xml`의 purple·teal 값, 다크 색(`*Dark`, `DuriseoDarkColors`, `isSystemInDarkTheme`) — 다크 모드는 design-system.md에 정의 전까지 만들지 않음 | — |
| 정리-6 | `app/src/main/AndroidManifest.xml`: `INTERNET`, `ACCESS_NETWORK_STATE` 권한 추가 (지금 없음. `data:remote` manifest에 두고 병합해도 됨) | 빌드로 확인 |
| 정리-7 | build-logic: 플러그인에 박힌 숫자(`compileSdk = 36`, `minSdk = 26`, `targetSdk = 36`, `JavaVersion.VERSION_11`)를 `libs.versions.toml` `[versions]`에서 읽게 (build-logic/CLAUDE.md 위반 상태). JVM은 카탈로그 `jvmTarget = "17"`로 통일 (지금은 11과 17이 섞여 있음) | 빌드로 확인 |
| 정리-8 | 카탈로그 정리 (아래 "카탈로그 정리" 절) | 빌드로 확인 |
| 정리-9 | 마무리: `docs/UNIT1-TEMPLATE-CLEANUP.md` 삭제, 루트 `CLAUDE.md`와 `.claude/agents/android-reviewer.md` 40~42행 "단위 1 추가 확인" 절 삭제, `docs/MODULES.md` 1장 갱신 | — |

### B. 디자인 토큰 → Compose 테마 (`/sync-tokens`)

위치: `core/designsystem/src/main/java/com/duriseo/android/core/designsystem/theme/`. `null`인 토큰은 없다. 다만 아래 "확인 필요" 4~6번(글꼴 파일, 매핑 없는 M3 역할, 매핑 없는 글자 스타일)은 정해져야 끝낼 수 있다.

| 토큰 묶음 | 개수 | 반영 | 비고 |
| --- | --- | --- | --- |
| `color.*` (단색) | 29 | `Color.kt`: `Brand50`·`Brand100`·`Brand500`·`Brand700`·`OnBrand`·`PersonMe`·`PersonPartner`·`PersonTogether`·`Ink900/700/600/500/400/300`·`Line300/200/100`·`Surface0/50/100`·`Scrim`·`Danger600/700`·`Warning50/700/800`·`New600`·`Info700`·`Favorite600`. `usage`는 KDoc | `scrim`은 `rgba(28, 26, 34, 0.48)` → ARGB `0x7A1C1A22`(0.48×255=122.4 → 0x7A). `new-600`·`info-700`은 MVP 미사용이지만 토큰이 있으니 상수는 둔다 |
| Material 3 역할 | 14 | `Theme.kt` `lightColorScheme(primary=Brand500, onPrimary=OnBrand, primaryContainer=Brand100, onPrimaryContainer=Brand700, surface=Surface0, onSurface=Ink900, onSurfaceVariant=Ink600, surfaceContainerLow=Surface50, surfaceContainer=Surface100, outline=Line300, outlineVariant=Line200, error=Danger600, onError=OnBrand, scrim=Scrim)` | 표에 없는 역할(secondary·tertiary·background·surfaceContainerHigh/Highest·inverseSurface 등)은 M3 기본(보라 계열)으로 남는다 → 확인 필요 5 |
| `person-*` | 3 | `PersonColors.kt`: `data class PersonColors(me, partner, together)` + `LocalPersonColors`(기본값 = 토큰). `ColorScheme`에 넣지 않음 | 실제 값은 단위 3에서 서버·DataStore로 바꿔 넣는다 |
| `pick` | 12 | `PersonColors.kt`: `DuriseoPickPalette` = 키(`pick-orange` … `pick-ink`) → 색, 순서 유지, `label` 포함 | 서버 팔레트 키 그대로 (ERD `space_members.color`) |
| 아바타 글자색 규칙 | — | `PersonColors.kt` `fun onPersonColor(bg): Color` — WCAG 상대휘도 > 0.2면 `Ink900`, 아니면 흰색 (design-system "사람 색 규칙", SCREENS 공통 "아바타 글자색") | 흰색은 `OnBrand`(#ffffff) 사용 |
| `font` | 2 | `Type.kt` `FontFamily`: IBM Plex Sans KR(400·500·600·700), IBM Plex Mono(500) | 글꼴 파일이 레포에 없음 (Pretendard만 있음) → 확인 필요 4 |
| `typography.*` | 13 | `Type.kt` M3 `Typography`: `headlineMedium`=headline, `titleLarge`=title-lg, `titleMedium`=title-md, `bodyLarge`=body-lg, `bodyMedium`=body-md, `bodySmall`=body-sm, `labelLarge`=label, `labelMedium`=caption, `labelSmall`=micro (9개). 나머지 `display`·`title-sm`·`code-lg`·`code-md`는 `DuriseoTypography`에 | size·lineHeight는 sp, weight는 `FontWeight`, letterSpacing은 em. letterSpacing이 없는 스타일은 확인 필요 6 |
| `radius.*` | 8 | `Shape.kt` M3 `Shapes`: `small`=radius-sm(12), `medium`=radius-md(14), `large`=radius-lg(16), `extraLarge`=radius-2xl(24). 나머지 `radius-xs`(6)·`radius-fab`(18)·`radius-xl`(20)·`radius-full`(999 → `CircleShape`/50%)은 `DuriseoShapes` | `extraSmall`은 매핑 없음 → 지정하지 않음 |
| `space.*` | 7 | `Dimens.kt` `object DuriseoSpacing` (space-1 4 … space-8 32, dp) | space-7은 토큰에 없음 (만들지 않음) |
| `size.*` | 12 | `Dimens.kt` `object DuriseoSize` (touch 44, icon 24, button sm/md/lg 44/48/56, input 52, appbar 64, bottomnav 80, fab 56, avatar sm/md/lg 24/32/44) | |
| `shadow.*` | 3 | `Elevation.kt`: `shadow-sm`(0 1 3, α0.12), `shadow-fab`(0 6 16, α0.18), `shadow-dialog`(0 16 40, α0.24). Compose 1.9의 `Modifier.dropShadow(Shadow(radius, offset, color))`로 그대로 표현 가능 | 지금 BOM(2024.09.00, Compose 1.7)에는 `dropShadow`가 없어 elevation 근사만 가능 → BOM 올리기 권장 (카탈로그 정리 1) |
| `icon` | 4 | 아이콘 VectorDrawable 규칙: 24 격자, 선 1.8, round cap·join, `currentColor` → `tint` | C절 탭 아이콘에 적용 |
| 테마 | — | `DuriseoTheme(content)`: `MaterialTheme(colorScheme, typography, shapes)` + `LocalPersonColors` + `LocalDuriseoTypography`. 라이트만 | 기존 `DuriseoColors`(템플릿 역할 이름) 삭제, `DuriseoTheme.colors` 사용처 교체 |

| 테스트 / Preview | 내용 |
| --- | --- |
| `` `FR-CAL-04 아바타 글자색은 배경 상대휘도가 0.2보다 크면 ink-900이다`() `` | `pick-amber`(#f59f00) → Ink900 |
| `` `FR-CAL-04 아바타 글자색은 배경 상대휘도가 0.2 이하면 흰색이다`() `` | `pick-blue`(#2f6fdb), `pick-ink` → 흰색 |
| `` `FR-CAL-04 우리 색상 팔레트는 tokens 순서대로 12색이다`() `` | 키 순서 `pick-orange` … `pick-ink` |
| `` `FR-CAL-04 사람 색 기본값은 person-me, person-partner, person-together다`() `` | |
| `` `tokens scrim은 rgba 0.48을 ARGB 0x7A1C1A22로 옮긴다`() `` | |
| Preview | `ColorTokensPreview`, `PickPalettePreview`, `TypographyTokensPreview`, `ShapeTokensPreview`, `ShadowTokensPreview` |

### C. 하단 탭 (D-30, SCREENS 공통 "하단 탭")

| ID | 내용 | 테스트 / Preview |
| --- | --- | --- |
| D-30 | 탭은 캘린더·메모 두 개, 첫 화면은 캘린더. `MainTab.CALENDAR`·`MainTab.MEMO`, `MainNavigator.startDestination = MainRouteModel.Calendar` | `` `D-30 하단 탭은 캘린더와 메모 두 개다`() `` · `` `D-30 앱의 첫 화면은 캘린더 탭이다`() `` |
| D-30 | 공용 컴포넌트 `DuriseoBottomNav`를 `core:designsystem`에 (core/designsystem/CLAUDE.md "하단 탭"). `feature:main`은 상태만 넘긴다. 문구 "캘린더"·"메모"는 SCREENS 공통 표 그대로 `strings.xml`에 | Preview `DuriseoBottomNavCalendarSelectedPreview`, `DuriseoBottomNavMemoSelectedPreview`, `MainScreenCalendarTabPreview`, `MainScreenMemoTabPreview` |
| SCREENS 공통 | 높이 80(`size-bottomnav`), 선택 탭 `brand-100` 알약 + `brand-700` 아이콘. 보드 원본(`design/source/Memo.dc.html` 104~112행) 값: 위 테두리 1 `line-100`, 배경 `surface-0`, 아이콘·라벨 간격 4(`space-1`), 라벨 12/500 `ink-500`(= `caption`), 선택 라벨 `ink-900` 700, 알약 56×30 모서리 15, 아이콘 22 | 토큰에 없는 값(알약 56×30, 아이콘 22, 선택 라벨 700, 테두리 1)은 확인 필요 2 |
| SCREENS 공통 | 탭 아이콘 달력·메모: 보드 원본의 인라인 SVG(`Memo.dc.html` 106·110행)를 VectorDrawable로 | 확인 필요 3 |
| 접근성 | 라벨 글자가 있으므로 아이콘 `contentDescription = null`, 탭은 `Role.Tab` + `selected` semantics. 터치 영역은 탭 칸 전체(≥ `size-touch`) | `` `D-30 탭 라벨은 strings의 캘린더와 메모다`() `` (`MainTab.labelRes` 확인) |
| 탭 전환 | 기존 `popUpTo(start){saveState}` + `restoreState` 유지 | 내비게이션은 androidTest라 CI 밖 → 맥에서 실기기로 확인 |

### D. 새 모듈 (`domain`, `data:repository`, `data:local`)

| 모듈 | 플러그인 | 단위 1에서 넣는 것 | 테스트 |
| --- | --- | --- | --- |
| `domain` | **새 컨벤션 플러그인 `duriseo.kotlin.jvm` 필요** (build-logic에 Kotlin JVM 플러그인이 없음, MODULES.md 1장·build-logic/CLAUDE.md가 단위 1에서 추가하라고 함). `org.jetbrains.kotlin.jvm` 적용 + 카탈로그 `jvmTarget` + `junit`·`coroutines-test`·`turbine` 테스트 의존성. KGP는 이미 build-logic 클래스패스에 있어 새 다운로드 없음 | 오류 모델(E절) `DataError`(`ApiError`, `Offline`), `ErrorCode`(API.md 5장 26개), `FieldError`/`FieldErrorReason`(`REQUIRED`·`TOO_LONG`·`INVALID`·`OUT_OF_RANGE`), 결과 타입, `NetworkMonitor` 인터페이스(G절). 의존성: `kotlinx-coroutines-core`(Flow), 필요하면 `javax.inject` | E·G절 |
| `data:repository` | `duriseo.android.library` + `duriseo.android.hilt` | 오프라인 쓰기 막기 도우미(G절 `runOnlineWrite`), Hilt 바인딩 모듈 자리. 리포지토리 구현은 기능 단위에서 | G절 |
| `data:local` | `duriseo.android.library` + `duriseo.android.hilt` | DataStore Preferences 제공(`DataStoreModule`, 파일 이름만). Room 의존성·KSP 설정. **`@Database` 클래스는 첫 엔티티가 생기는 단위에서** 만든다 (엔티티 없는 `@Database`는 컴파일 오류) → 결정 3 | `` `DataStore는 저장한 값을 다시 읽는다`() `` (임시 파일 `PreferenceDataStoreFactory`, Robolectric 없이) |

- 의존 방향: `feature:* → domain`, `data:repository → domain, data:remote, data:local`, `app → data:repository`(Hilt 바인딩을 모으기 위해). feature는 `data:*`를 모른다.
- `docs/MODULES.md` 1장의 세 줄을 "있음"으로, `feature:home`·`feature:menu` 줄을 `feature:calendar`·`feature:settings`로 고친다.

### E. 응답 형식 1.1·오류 변환 (`data:remote`, API.md 1장·1.1·5장)

| ID | 내용 | 테스트 (MockWebServer) |
| --- | --- | --- |
| API 1장 | 모든 경로는 `/api/v1` 아래. JSON camelCase, 날짜 `YYYY-MM-DD`(`LocalDate`), 일시 ISO 8601 UTC(`Instant`) 직렬화기 | `` `API 1장 일시는 ISO 8601 UTC 문자열을 Instant로 읽는다`() `` · `` `API 1장 날짜는 YYYY-MM-DD를 LocalDate로 읽는다`() `` |
| API 1.1 단건 | 봉투 없이 리소스 그대로 (200/201) | `` `API 1.1 단건 성공 응답은 봉투 없이 리소스로 읽는다`() `` |
| API 1.1 목록 | `ListResponse<T>(items)`, `PageResponse<T>(items, nextCursor)`, `CountedPageResponse<T>(items, nextCursor, totalCount)` (서버 `ApiResponses.kt`와 같은 모양) | `` `API 1.1 페이지 목록은 items와 nextCursor를 읽고 마지막 페이지는 nextCursor가 null이다`() `` · `` `API 1.1 페이지가 없는 목록은 items만 읽는다`() `` |
| API 1.1 204 | 본문 없는 성공 → `Unit` | `` `API 1.1 204 응답은 본문 없이 성공이다`() `` |
| API 1.1 오류 객체 | `{code, message, details, traceId}` → `ApiError(code, details, traceId)`. `message`는 담지 않고 디버그 로그에만 (ARCHITECTURE 3장). `code`는 원래 문자열을 두고 `ErrorCode`로 해석, 모르는 값은 `knownCode = null` (일반 오류로 처리, 새 코드를 만들지 않음) | `` `API 1.1 오류 응답은 code와 details와 traceId를 가진 ApiError로 바뀐다`() `` · `` `API 1.1 오류의 message는 ApiError에 담지 않는다`() `` · `` `API 1.1 모르는 code는 알려지지 않은 코드로 두고 원래 문자열을 남긴다`() `` |
| API 1.1 details | `VALIDATION_FAILED`·`DATE_OUT_OF_RANGE` → `fields[{field, reason}]`, `INVITE_REISSUE_TOO_SOON`·`INVITE_RATE_LIMITED` → `retryAfterSeconds`, 그 밖은 `null` | `` `API 1.1 VALIDATION_FAILED의 details.fields를 필드와 이유로 읽는다`() `` · `` `API 1.1 INVITE_RATE_LIMITED의 details.retryAfterSeconds를 읽는다`() `` · `` `API 1.1 그 밖의 code는 details가 null이다`() `` |
| API 5장 | `ErrorCode` enum 26개 (API.md main 기준, v0.7은 새 코드 없음): `VALIDATION_FAILED`, `DATE_OUT_OF_RANGE`, `INVALID_TARGET`, `TOKEN_EXPIRED`, `SESSION_INVALID`, `SESSION_REPLACED`, `SOCIAL_TOKEN_INVALID`, `NOT_AUTHOR`, `GUEST_NOT_ALLOWED`, `GUEST_ONLY`, `NOT_FOUND`, `INVITE_NOT_FOUND`, `SOCIAL_NOT_REGISTERED`, `NO_IMPORT_PENDING`, `INVITE_EXPIRED`, `INVITE_USED`, `INVITE_SELF`, `INVITE_UNAVAILABLE`, `ALREADY_COUPLED`, `NOT_COUPLE`, `SOCIAL_ALREADY_USED`, `IMAGE_TOO_LARGE`, `IMAGE_TYPE_NOT_ALLOWED`, `INVITE_RATE_LIMITED`, `INVITE_REISSUE_TOO_SOON`, `INTERNAL_ERROR` (API.md 5장 표 순서) | `` `API 5장 오류 코드 문자열을 모두 ErrorCode로 해석한다`() `` |
| ARCHITECTURE 3장 | 네트워크 오류(연결 없음, 시간 초과, `IOException`)는 `Offline` | `` `NFR-OFF-02 연결이 끊기면 Offline으로 바뀐다`() `` · `` `NFR-OFF-02 시간 초과는 Offline으로 바뀐다`() `` |
| NFR-SEC-01 | OkHttp 로깅은 debug만, `Authorization` 헤더 가림 (`redactHeader`, data/CLAUDE.md). 지금 `NetworkModule`에 없음 | `` `NFR-SEC-01 로그에 Authorization 헤더 값을 남기지 않는다`() `` |
| traceId | 디버그 로그에만 남김 (ARCHITECTURE 3장) | (ApiError 변환 테스트에 포함) |

- 인증(`Authenticator`, 토큰 갱신, `SESSION_REPLACED` 처리)은 단위 2. 단위 1의 Retrofit은 인증 없는 클라이언트 하나.
- `Json` 설정의 `isLenient = true`, `prettyPrint = true`는 끈다 (서버는 표준 JSON, 요청 크기만 늘어남).

### F. 빌드 타입별 서버 주소·설정값 (`local.properties`, SETUP.md 2장)

| ID | 내용 | 테스트 |
| --- | --- | --- |
| ARCHITECTURE 6장 | `app/build.gradle.kts`가 `local.properties`의 `duriseo.api.devUrl`(debug), `duriseo.api.prodUrl`(release), `duriseo.google.webClientId`, `duriseo.kakao.nativeAppKey`를 읽어 `BuildConfig.API_BASE_URL`·`GOOGLE_WEB_CLIENT_ID`·`KAKAO_NATIVE_APP_KEY`로. 없으면 `""` (CI). 값은 코드·`gradle.properties`에 쓰지 않음 | CI 빌드(값 없음)가 성공하는 것으로 확인 |
| app/CLAUDE.md | `data:remote`는 app의 `BuildConfig`를 볼 수 없으므로 `core:common`에 `data class AppConfig(apiBaseUrl, googleWebClientId, kakaoNativeAppKey)`, app의 Hilt 모듈이 제공 | `` `API 1장 서버 주소 뒤에 api v1 경로를 붙인 기본 주소를 만든다`() `` · `` `서버 주소가 비어 있으면 빈 설정으로 표시한다`() `` (결정 1) |
| ARCHITECTURE 7장 | 현재 시각용 `java.time.Clock` Hilt 제공 (`Clock.systemDefaultZone()`), IO 디스패처 한정자 `@IoDispatcher` (`core:common`) | — |
| 두 빌드 타입 | applicationId 둘 다 `com.duriseo.android` (지금 그대로). 릴리스 서명은 출시 단위 | — |

### G. 온라인 확인 (NFR-OFF-01·02, D-28)

| ID | 내용 | 테스트 |
| --- | --- | --- |
| NFR-OFF-02 | `domain`의 `interface NetworkMonitor { val isOnline: Flow<Boolean>; fun isOnlineNow(): Boolean }`, 구현 `AndroidNetworkMonitor`(`ConnectivityManager` 콜백, `NET_CAPABILITY_INTERNET`)는 `data:remote` | (구현은 Android API라 실기기 확인. 로직은 가짜로 테스트) |
| NFR-OFF-02 | `data:repository`의 `suspend fun <T> runOnlineWrite(block)`: 오프라인이면 API를 부르지 않고 `Offline` | `` `NFR-OFF-02 오프라인이면 쓰기 요청을 보내지 않고 Offline을 돌려준다`() `` (MockWebServer `requestCount == 0`) · `` `NFR-OFF-02 온라인이면 쓰기 요청을 보낸다`() `` |
| NFR-OFF-02, NFR-COPY-01 | 문구 "인터넷 연결이 필요해요"(PRD NFR-OFF-02 그대로, 해요체)를 `core:designsystem` `strings.xml`에. `Offline` → 이 문구 리소스로 바꾸는 도우미 | `` `NFR-OFF-02 Offline 오류는 인터넷 연결이 필요해요 문구로 바뀐다`() `` · `` `NFR-COPY-01 오프라인 안내 문구는 PRD 문구 그대로 해요체다`() `` (문자열 리소스 값 비교는 Robolectric이 필요하면 리소스 ID 비교로 대체). 보여 주는 방식은 확인 필요 7 |
| NFR-OFF-01 | 오프라인 refresh는 조용히 실패하고 Room 데이터는 그대로 | `` `NFR-OFF-01 오프라인이면 refresh는 조용히 실패하고 저장된 데이터는 그대로다`() `` — 첫 Room 엔티티가 생기는 단위에서 (단위 1에는 엔티티 없음) |

### H. 테스트 도구

| 내용 | 위치 |
| --- | --- |
| `kotlinx-coroutines-test`, Turbine, JUnit4를 `duriseo.android.feature`·`duriseo.kotlin.jvm` 컨벤션의 `testImplementation`에, `data:*`는 모듈 파일에 | build-logic, 각 `build.gradle.kts` |
| OkHttp MockWebServer는 `data:remote`·`data:repository` 테스트에만 | |
| `MainDispatcherRule`(`Dispatchers.setMain`) 공용 위치 → 결정 2 | |
| 확인 명령: `./gradlew assembleDebug testDebugUnitTest` (리드 환경은 Google Maven에 닿지 않으므로 PR CI로 확인) | |

### 라이브러리 추가 (MODULES.md 3장 단위 1분 + 필요한 것)

리드 환경은 Google Maven에 닿지 않고, 이 세션에서도 Maven Central·Google Maven·GitHub 모두 막혀 있어 버전을 직접 확인하지 못했다. 아래는 2026년 중반 기준 존재가 확실한 안정 버전이고 모두 **CI로 확인**한다.

| 용도 | 좌표 | 버전 | 저장소 | 비고 |
| --- | --- | --- | --- | --- |
| Kotlin JVM 플러그인 | `org.jetbrains.kotlin.jvm` | 2.2.10 (`kotlin`과 같게) | 이미 build-logic 클래스패스 | CI로 확인 |
| 코루틴 (domain Flow) | `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.10.2 | Maven Central | CI로 확인 |
| 코루틴 테스트 | `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.10.2 | Maven Central | CI로 확인 |
| Turbine | `app.cash.turbine:turbine` | 1.2.1 | Maven Central | CI로 확인 |
| MockWebServer | `com.squareup.okhttp3:mockwebserver` | 4.12.0 (`okhttp`과 같게) | Maven Central | CI로 확인 |
| Room | `androidx.room:room-runtime`, `room-ktx`, `room-compiler`(KSP) | 2.7.2 | Google Maven | CI로 확인. Room Gradle 플러그인은 AGP 9 호환 위험이 있어 쓰지 않고 스키마 경로는 `ksp { arg("room.schemaLocation", …) }`로 |
| DataStore | `androidx.datastore:datastore-preferences` | 1.1.7 | Google Maven | CI로 확인 |
| (선택) 주입 표기 | `javax.inject:javax.inject` | 1 | Maven Central | domain에 `@Inject` 유스케이스가 생길 때만 |
| (선택) 수명주기 Compose | `androidx.lifecycle:lifecycle-runtime-compose` | `lifecycleRuntimeKtx`와 같게 | Google Maven | `collectAsStateWithLifecycle`용. 지금은 다른 라이브러리에 딸려 들어옴 → 명시 권장 |

### 카탈로그 정리 (`gradle/libs.versions.toml`)

지금 버전은 main CI가 초록이라 모두 존재가 확인된 값이다. 최소한으로만 바꾼다.

1. `composeBom = "2024.09.00"`(Compose 1.7, 2024년 9월) → `2025.08.00`(Compose 1.9) 권장. 이유: shadow 토큰을 `dropShadow`로 그대로 표현, Kotlin 2.2·compileSdk 36과 시기가 맞음. **CI로 확인**. 실패하면 2024.09.00으로 두고 shadow는 elevation 근사.
2. 안 쓰는 것 삭제: `androidx-appcompat`·`material`(View용, `core:common`만 의존 — Compose 앱에 불필요), `androidx-espresso-core`·`androidx-junit`(androidTest 삭제 후). `androidx-compose-ui-test-junit4`·`ui-test-manifest`는 나중 UI 테스트용으로 둔다.
3. 중복 정리: `jetbrainsKotlinJvm = "2.2.10"` 삭제하고 `jetbrains-kotlin-jvm` 플러그인이 `kotlin`을 참조. 
4. `[versions]`의 `compileSdk`·`targetSdk`·`minSdk`·`jvmTarget`·`versionCode`·`versionName`은 정의만 있고 아무도 안 읽음 → build-logic과 `app`이 읽게 (정리-7).
5. 그대로 둠: AGP 9.0.1, Kotlin 2.2.10, KSP 2.2.10-2.0.2, Hilt 2.60.1, Navigation 2.9.3, Retrofit 2.11.0, OkHttp 4.12.0, serialization 1.9.0, coreKtx 1.17.0, activityCompose 1.10.1, lifecycle 2.8.7 (올릴 이유가 단위 1에 없음). `gradle.properties`의 `android.disallowKotlinSourceSets=false`(AGP 9 내장 Kotlin + KSP용)도 그대로, 깨진 한글 주석만 고친다.

## 서버 연동

| API | 개발 서버 배포 여부 | 확인 |
| --- | --- | --- |
| 없음 (PLAN.md 단위 1 "서버" 열 비어 있음) | 서버 키트 1(응답 형식 1.1)은 완료, 경로는 아직 없음 | 응답·오류 형식은 MockWebServer로, 서버 `ApiResponses.kt`의 모양과 맞춰 봄 |

## 결정

Android 레포 안에서 정할 수 있는 것. 리드가 범위 확인 때 함께 승인한다.

1. **기본 주소**: `local.properties` 값은 서버 호스트(`https://…`)이고 앱이 `/api/v1/`을 붙인다 (API.md 1장). 값이 비어 있으면(CI) Retrofit `baseUrl("")`이 앱 시작 때 예외를 내므로, Retrofit을 처음 쓸 때 만들고 빈 설정이면 요청 없이 실패하게 한다. (`config/environment.md`의 값 형식이 호스트만인지 리드가 확인)
2. **테스트 도우미 위치**: `MainDispatcherRule`을 둘 `core:testing`은 MODULES.md 계획에 없다. 안: 단위 1에서는 모듈마다 작은 파일로 두고, 세 곳 이상 필요해지면 `core:testing` 추가를 묻는다.
3. **Room**: 단위 1은 의존성·KSP 설정까지. `@Database`는 첫 엔티티 단위에서. 메모리 Room 테스트(CONVENTIONS.md "data:repository")에는 Robolectric이 필요한데 MODULES.md 3장에 없다 → 그 단위에서 추가를 묻는다.
4. **결과·오류 타입 위치**: ARCHITECTURE.md 1장은 "결과 타입"을 `core:common`에 두지만 `core:common`은 Android 라이브러리라 순수 Kotlin `domain`이 의존할 수 없다. 안: `DataError`·결과 타입은 `domain`에, ARCHITECTURE.md 표를 같은 PR에서 고친다.
5. **하단 탭 높이와 시스템 바**: 80은 내용 높이, 제스처·내비게이션 바 inset은 그 아래에 더한다 (디자인 프레임 390×844에는 시스템 바가 없음).
6. **letterSpacing이 없는 글자 스타일**: CSS 기본값과 같은 0으로 둔다 (확인 필요 6에서 다르게 정하면 따른다).

### 리드 결정 (2026-10-26, 사용자 승인)

- 결정 1~6은 위 안대로 한다. 결정 1: 값은 호스트만(`https://…`)이고 앱이 `/api/v1/`을 붙인다. 결정 4: 결과·오류 타입은 `domain`에 두고 ARCHITECTURE.md를 같은 PR에서 고친다.
- 확인 필요 4 (글꼴): **Google Downloadable Fonts**로 IBM Plex Sans KR(400·500·600·700)·IBM Plex Mono(500)를 쓴다 (`ui-text-google-fonts`, 인증서 배열 리소스). 받기 전·오프라인이면 시스템 글꼴. 템플릿 Pretendard 파일은 지운다. OFL 고지는 오픈소스 라이선스 화면이 생기는 단위에서.
- 확인 필요 7·8 (오프라인·공통 오류): **스낵바**로 보여 준다. 오프라인 "인터넷 연결이 필요해요"(NFR-OFF-02), 공통 오류 "문제가 생겼어요. 잠시 후 다시 시도해 주세요"(사용자 결정, Docs SCREENS 공통 표에 추가 예정). `core:designsystem`에 `DuriseoSnackbar`(토큰 색: 면 `ink-900`, 글자 흰색 계열은 Docs 반영 뒤 확정)
- 확인 필요 9: JSON이 아닌 오류 응답은 `ApiError(code = "INTERNAL_ERROR")`로 본다 (화면은 공통 오류 문구).
- 확인 필요 11: 앱 표시 이름은 **둘이서**.
- 확인 필요 1·2·3·5·6 (탭 수치·아이콘, 매핑 없는 M3 역할·글자 스타일): **캔버스 값으로 Docs를 보충하는 PR**을 리드가 따로 연다. 그 전까지 탭은 구조·동작·토큰에 있는 값으로만 만들고, 수치·아이콘·추가 매핑은 Docs PR 머지 뒤 같은 브랜치에 더한다.
- 확인 필요 10: Docs 보충 PR에서 design-system.md 패키지 이름을 고친다.
- 확인 필요 12: 단위 3에서 정한다.
- 라이브러리: 위 표의 버전으로 추가하고 CI로 확인한다. composeBom 올리기는 시도하고 CI가 실패하면 되돌린다.

## 확인 필요

문서끼리 다르거나 비어 있는 곳. Docs 수정은 Docs 레포 `/prd-sync`·`/design-sync`로.

1. **SCR-CAL-01 이미지에 하단 탭이 없음**: `design/screens/SCR-CAL-01.png`와 `design/source/Calendar.dc.html`(캔버스 38)에는 탭 바가 없고, 탭 바는 `Memo.dc.html` 104행과 옛 `Calendar_v1.dc.html` 98행에만 있다. SCREENS 공통 표·PRD D-30은 두 탭. FAB 위치("탭 있는 화면은 바닥에서 100")도 이미지와 다르다. 구현은 PRD대로 두 탭으로 하되 디자인 갱신 필요 (단위 4 전에).
2. **하단 탭 값이 토큰에 없음** (`Memo.dc.html` 104~112행): 알약 56×30·모서리 15, 아이콘 22(`size-icon`은 24), 선택 라벨 12/700(`caption`은 12/500), 위 테두리 1. 토큰 추가(`/design-sync`)인지, 보드 원본 값을 그대로 써도 되는지.
3. **탭 아이콘 원본**: design-system.md "아이콘"·"컴포넌트"가 말하는 `components/bundle.css`·컴포넌트 README·아이콘 파일이 Docs에 없다. 보드의 인라인 SVG(`Memo.dc.html` 106·110행)를 VectorDrawable로 옮겨도 되는지. 또 "켜진 상태만 채워요" 규칙과 달리 보드의 선택 탭 아이콘은 채우지 않은 선 아이콘이다 — 어느 쪽인지.
4. **글꼴 파일·라이선스**: tokens `font.sans` IBM Plex Sans KR(400·500·600·700), `font.mono` IBM Plex Mono(500). 레포에는 템플릿 Pretendard만 있다. (a) `res/font`에 넣을지(한글 글꼴이라 굵기마다 수 MB, APK 커짐) Downloadable Fonts(Google Play 서비스 필요)로 할지, (b) 파일은 누가 어디서 받는지(리드 환경 네트워크 제한), (c) SIL OFL 1.1 고지를 SCR-SET-01 "오픈소스 라이선스"에 넣는지.
5. **매핑 표에 없는 Material 3 역할** (design-system.md "Android (Jetpack Compose) 매핑"): `secondary`·`secondaryContainer`·`tertiary`·`background`·`surfaceContainerLowest/High/Highest`·`inverseSurface`·`inverseOnSurface`·`errorContainer` 등은 M3 기본 보라색으로 남는다. M3 `NavigationBar`(지시자 `secondaryContainer`), `Snackbar`(`inverseSurface`), `AlertDialog`(`surfaceContainerHigh`), `TextField`(`surfaceContainerHighest`)가 이 값을 쓴다. 매핑을 추가할지, M3 컴포넌트를 쓰지 않고 `core:designsystem` 컴포넌트로만 그릴지.
6. **매핑 없는 글자 스타일**: `display`·`title-sm`·`code-lg`·`code-md`는 Typography 대응이 없다 (M3에 `displayLarge`·`titleSmall` 자리는 있음). `title-sm` → `titleSmall`로 넣어도 되는지. 또 `title-md`~`micro`, `code-md`는 `letterSpacing`이 없다 — 0으로 봐도 되는지.
7. **오프라인 안내 방식**: PRD NFR-OFF-02의 문구 "인터넷 연결이 필요해요"는 있지만 SCREENS 공통 표에 어떻게 보여 주는지(스낵바·토스트·다이얼로그)가 없다. 스낵바라면 design-system "컴포넌트"에 Snackbar 명세(색·모서리·위치)도 없다. SCREENS 공통 "다른 기기 로그인"도 스낵바를 쓰므로 함께 필요. 오프라인으로 보는 중임을 알리는 표시(배너 등)가 있는지도 없다(NFR-OFF-01).
8. **공통 오류 문구**: API.md 5장 `INTERNAL_ERROR` "앱은 공통 오류 안내를 보여준다", Android ARCHITECTURE.md 3장 "모르는 code는 일반 오류 문구" — 그 문구가 SCREENS·PRD에 없다.
9. **JSON이 아닌 오류 응답**: API.md 1.1은 모든 오류가 오류 객체라고 하지만 서버 앞단(프록시)이나 서버가 꺼졌을 때 HTML·빈 본문 5xx가 올 수 있다. 이때 `ApiError(INTERNAL_ERROR)`로 볼지 일반 오류로 볼지.
10. **패키지 이름 불일치**: design-system.md 머리말 "패키지 `com.juco.duriseo`" ↔ Android applicationId `com.duriseo.android` (ARCHITECTURE.md 6장, app/CLAUDE.md). design-system.md를 고쳐야 함.
11. **앱 표시 이름**: `app_name`이 템플릿의 "Duriseo"다. 런처에 보일 이름("둘이서"?)이 PRD·SCREENS에 없다.
12. **모르는 팔레트 키**: 서버가 `pick-*` 12개 밖의 키를 보내면 어떻게 그릴지 정해져 있지 않다 (단위 3 전까지만 정하면 됨).

## 리뷰 기록

| 회차 | Critical | Major | 비고 |
| --- | --- | --- | --- |
| 1 | 0 | 1 | Minor 9. Major: 문구 주석 출처(SCREENS.md 공통 표). 수정: 주석 출처, 날짜 파싱 오류 -> `SerializationException`, 팔레트 라벨 `strings.xml`, ARCHITECTURE 그림(`core:designsystem -> domain`), settings 행·주석(단위 2부터), 견본 크기 토큰, `MainBottomBar` modifier, Docs PR #8 값 반영(`TODO(Docs PR)` 제거) |

## 남은 일

- 리뷰 1회차 수정 반영 완료 → CI 확인 뒤 사람이 머지 (`gh pr merge` 금지)
- 실기기 확인(CI 밖): 탭 전환 상태 저장·복원, 비행기 모드에서 `AndroidNetworkMonitor`, Google 글꼴 받기, 스낵바 위치(탭·FAB 위)
- 확인 필요 1·10·11·12와 Docs 쪽 수정은 Docs `/prd-sync`·`/design-sync`로
