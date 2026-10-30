# 모듈 계획

모듈은 필요한 작업 단위에서 만든다. 지금 있는 모듈은 `settings.gradle.kts`가 기준이다. 모듈을 만들거나 계획이 바뀌면 같은 PR에서 이 문서를 고친다. 단위 번호는 `docs/PLAN.md`.

## 1. 모듈

| 모듈 | 플러그인 | 생기는 단위 | 비고 |
| --- | --- | --- | --- |
| `app` | application, compose, hilt | 있음 | `local.properties` 설정값 -> `BuildConfig` -> `AppConfig` 제공 |
| `feature:main` | feature | 있음 | 하단 탭 2개(캘린더·메모, D-30). 메모 탭은 `feature:memo`(단위 6) 전까지 빈 자리표시. 단위 2에서 앱 시작 분기(토큰 있으면 캘린더, 없으면 시작 화면)와 세션 끊김 구독(`MainViewModel`) |
| `feature:calendar` | feature | 있음 | 템플릿 `feature:home`에서 이름 변경 (단위 1). 화면은 단위 4 |
| `feature:settings` | feature | 있음 | 템플릿 `feature:menu`에서 이름 변경 (단위 1). 하단 탭이 아님, 진입은 캘린더 앱바(단위 4 전까지 임시 버튼). 단위 2에서 SCR-SET-01 전체 메뉴의 계정 부분과 계정 화면(게스트·회원)·다이얼로그 4종 |
| `core:designsystem` | library, compose | 있음 | 단위 1에서 토큰 반영 (`/sync-tokens`), 하단 탭·스낵바 컴포넌트, 오류 -> 문구 도우미. 단위 2에서 `DuriseoButton`·`DuriseoDialog`·`LogoutConfirmDialog`·`DuriseoTopAppBar`·`DuriseoGuestBanner`·`DuriseoMenuRow`, 아이콘·로고 VectorDrawable, 세션 종료 코드는 문구 없음(`toMessageResOrNull`) |
| `core:common` | library, hilt | 있음 | 로거, `AppConfig`, `Clock`·`@IoDispatcher`·`@ApplicationScope` 제공, 내비게이션 경로. 단위 2부터 `social` 패키지: Google(Credential Manager)·카카오 로그인 래퍼 `SocialTokenProvider`, 소셜 SDK 정리, 카카오 SDK 초기화 (`feature:auth`·`feature:settings`가 함께 씀, unit-2 확인 필요 16) |
| `domain` | Kotlin JVM (`duriseo.kotlin.jvm`) | 있음 | 순수 Kotlin. Android 의존 없음. `DataError`·`DataResult`(결과·오류 타입), `ErrorCode`, `NetworkMonitor`. 단위 2부터 인증·내 정보 모델과 `AuthRepository`·`MeRepository`·`TokenStorage` 인터페이스 |
| `data:remote` | library, hilt | 있음 | 단위 1에서 응답 형식 1.1·오류 변환(`ApiCaller`), `AndroidNetworkMonitor`. 단위 2에서 인증 없음(`@NoAuth`)·인증(`@Authed`) 클라이언트, `AuthApi`·`AuthSessionApi`·`MeApi`, 인증 헤더 인터셉터, 토큰 갱신 `Authenticator` |
| `data:local` | library, hilt | 있음 | 단위 1은 DataStore 제공과 Room 의존성까지. 단위 2에서 첫 `@Database`(`MeEntity`), Tink로 암호화한 토큰 저장소, 로그아웃·세션 끊김 때 지우기(`LocalDataCleaner`) |
| `data:repository` | library, hilt | 있음 | 단위 1은 `runOnlineWrite`와 바인딩 모듈 자리. 단위 2에서 `AuthRepository`·`MeRepository` 구현, `SessionManager`(세션 끊김·정리), `SessionStartup`(앱 시작 때 로그인 상태·내 정보·시간대) |
| `feature:auth` | feature | 있음 | 단위 2. SCR-START-01 시작 화면(게스트 시작, Google·카카오 로그인). `feature:main`이 의존해 NavHost에 연결 |
| `feature:couple` | feature | 3 | |
| `feature:anniversary` | feature | 5 | |
| `feature:memo` | feature | 6 | |
| `push` | library, hilt | 7 | FCM, 동기화 신호 |
| `feature:notification` | feature | 7 | |

### 컨벤션 플러그인 (`build-logic`)

| 플러그인 ID | 하는 일 | 쓰는 곳 |
| --- | --- | --- |
| `duriseo.android.application` | 앱 SDK·JVM 설정 (카탈로그 값) | `app` |
| `duriseo.android.library` | 라이브러리 SDK·JVM 설정 (카탈로그 값) | 모든 Android 라이브러리 |
| `duriseo.android.compose` | Compose, Material 3, Preview 도구 | `app`, `core:designsystem`, feature |
| `duriseo.android.hilt` | Hilt + KSP | Hilt를 쓰는 모듈 |
| `duriseo.android.feature` | library + compose + hilt, 내비게이션, 테스트 도구(JUnit, 코루틴 테스트, Turbine) | `feature:*` |
| `duriseo.kotlin.jvm` | 순수 Kotlin(JVM) 설정 (카탈로그 `jvmTarget`), 테스트 도구 | `domain` |

SDK·JVM 숫자는 플러그인 코드가 아니라 `gradle/libs.versions.toml`의 `[versions]`에서 읽는다.

## 2. 화면 → 모듈

화면 내용은 `../Docs/docs/SCREENS.md`. 이 표는 어느 모듈에 두는지만 정한다.

| 화면 | 모듈 |
| --- | --- |
| SCR-START-01 | `feature:auth` |
| SCR-CAL-01, SCR-CAL-02, SCR-CAL-03 | `feature:calendar` |
| SCR-ANNIV-01, SCR-ANNIV-02 | `feature:anniversary` |
| SCR-MEMO-01, SCR-MEMO-02, SCR-MEMO-03, SCR-MEMO-04 | `feature:memo` |
| SCR-COUPLE-01, SCR-COUPLE-02, SCR-COUPLE-03, SCR-COUPLE-04 | `feature:couple` |
| SCR-NOTI-01 | `feature:notification` |
| SCR-SET-01, SCR-SET-02, SCR-SET-03, SCR-SET-04 | `feature:settings` |

PRD에 화면이 추가되면 이 표에 먼저 넣고 모듈을 정한다.

## 3. 라이브러리 추가 계획

추가할 때 최신 안정 버전을 확인하고 `gradle/libs.versions.toml`에 넣는다. 아래에 없는 라이브러리는 PR 본문에 이유를 적는다.

| 용도 | 라이브러리 | 단위 |
| --- | --- | --- |
| 로컬 DB | Room (KSP) | 1 (추가됨) |
| 설정 저장 | DataStore Preferences | 1 (추가됨) |
| 토큰 암호화 | Tink (`tink-android` 1.18.0, Android Keystore) | 2 (추가됨, 버전은 CI로 확인) |
| Google 로그인 | Credential Manager (`credentials`, `credentials-play-services-auth` 1.5.0) + Google ID (`googleid` 1.1.1) | 2 (추가됨, 버전은 CI로 확인) |
| 카카오 로그인 | Kakao SDK (`v2-user` 2.21.0). `settings.gradle.kts`에 `devrepo.kakao.com` 저장소를 `com.kakao.*` 그룹만 받도록 추가 | 2 (추가됨, 버전은 CI로 확인) |
| 테스트: 메모리 Room | Robolectric 4.15.1 + `androidx.test:core` 1.6.1 (`data:local`, `data:repository`의 `testImplementation`, unit-2 확인 필요 15) | 2 (추가됨) |
| 푸시 | Firebase Messaging (BOM) | 7 |
| 프로필 이미지 | Coil | 8 |
| 테스트 | kotlinx-coroutines-test, Turbine, OkHttp MockWebServer | 1 (추가됨) |
