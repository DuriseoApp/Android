# 모듈 계획

모듈은 필요한 작업 단위에서 만든다. 지금 있는 모듈은 `settings.gradle.kts`가 기준이다. 모듈을 만들거나 계획이 바뀌면 같은 PR에서 이 문서를 고친다. 단위 번호는 `docs/PLAN.md`.

## 1. 모듈

| 모듈 | 플러그인 | 생기는 단위 | 비고 |
| --- | --- | --- | --- |
| `app` | application, compose, hilt | 있음 | `local.properties` 설정값 -> `BuildConfig` -> `AppConfig` 제공. `data:repository`를 의존성에 넣어 Hilt가 구현을 연결. `app/google-services.json`이 있을 때만 `com.google.gms.google-services` 플러그인을 적용(없으면 FCM 꺼짐, docs/SETUP.md). `MyApp`이 `PushInitializer.start()`를 부르고 Activity 생성을 `ProcessUiState`에 알림 |
| `feature:main` | feature | 있음 | 하단 탭 2개(캘린더·메모, D-30). 메모 탭은 `feature:memo`의 SCR-MEMO-01(단위 6, 예전 빈 자리표시 `MemoTabPlaceholder`는 없앰). `MainViewModel`: 앱 시작 분기(토큰 있으면 캘린더, 없으면 시작 화면), 세션 끊김 구독, 커플 해제 감지(`SpaceUnlinked`), 연결 완료 시트(SCR-COUPLE-04) 필요 여부, 사람 색(`personColorsOf`)을 `DuriseoTheme`에 넣기. `MainNavHost`·`MainNavigator`가 모든 feature 그래프를 연결하고 화면별 스낵바 위치(SCR-CAL-01·SCR-MEMO-01은 하단 탭 위, SCR-CAL-02·03·SCR-MEMO-02·03·04·SCR-ANNIV-01·02는 위에서 40)를 정함. 연결 완료 시트는 캘린더 탭 위에 이 모듈이 띄움. 단위 7: `SyncSpaceSignals`의 `LINKED`·`UNLINKED`를 들어 `GET /me` 뒤 기존 경로(D-125)로 감. `MainActivity`에 푸시 Intent를 받는 자리 `handlePushIntent`(묶음 D가 채움) |
| `feature:calendar` | feature | 있음 | 템플릿 `feature:home`에서 이름 변경 (단위 1). 단위 4: 캘린더 홈(SCR-CAL-01: 월 달력, 날짜별 일정 목록, 앱바의 처음 만난 날 D+N·전체 메뉴), 일정 상세(SCR-CAL-02: 내 알림 선택, 삭제), 일정 작성·수정(SCR-CAL-03: 날짜·시간·알림 시트). 캘린더 전용 부품은 이 모듈 안(`component/`)에 둔다. 기념일·메모 표시는 단위 5·6 |
| `feature:settings` | feature | 있음 | 템플릿 `feature:menu`에서 이름 변경 (단위 1). 하단 탭이 아님, 진입은 SCR-CAL-01 앱바의 [전체 메뉴]. `menu/` 전체 메뉴(SCR-SET-01: 우리 프로필 카드·처음 만난 날 시트 호스트·꾸미기·설정 행·로그아웃), `account/` 계정 화면(게스트·회원)·다이얼로그 4종, `connection/` 연결 정보(해제 포함), `colors/` 우리 색상(SCR-SET-03). 알림 설정 행·SCR-SET-02는 단위 7, SCR-SET-04·정보 섹션은 단위 8 |
| `core:designsystem` | library, compose | 있음 | 토큰 테마(`theme/`: 색·글꼴·간격·모서리·그림자, `PersonColors`·`personColorsOf`), 오류 -> 문구 도우미(`message/`: `toMessageRes`, 세션 종료 코드는 문구 없음 `toMessageResOrNull`). 공용 컴포넌트(`component/`): 하단 탭(`DuriseoBottomNav`), 스낵바(`DuriseoSnackbar`·`DuriseoSnackbarHost`·`DuriseoTopSnackbarHost`), 버튼(`DuriseoButton`·`DuriseoLargeButton`), 다이얼로그(`DuriseoDialog`·`DuriseoDialogLayer`: 뒤 화면을 접근성에서 가림·`LogoutConfirmDialog`), 아래쪽 시트(`DuriseoSheetPanel`·`DuriseoBottomSheetLayer`), 날짜 시트(`StartDatePickerSheet`, 기념일 작성도 같은 부품, 고를 수 있는 범위는 `DatePickerRange`: 처음 만난 날 1950-01-01 ~ 오늘, 일정 1950-01-01 ~ 2050-12-31), 앱바(`DuriseoTopAppBar`(오른쪽 행동 버튼 선택)·`DuriseoCloseTopAppBar`), 메뉴 행(`DuriseoMenuRow`·`DuriseoSectionTitle`), 게스트 배너, 아바타(`DuriseoAvatar`), 커플 표시(`DuriseoTogetherMark`·`DuriseoDashedLink`·`DuriseoEmptyPartnerSlot`). 아이콘·로고 VectorDrawable. 토큰 반영은 `/sync-tokens` |
| `core:common` | library, hilt | 있음 | 로거, `AppConfig`, `Clock`·`@IoDispatcher`·`@ApplicationScope` 제공, 내비게이션 경로(`RouteModel`). `social` 패키지: Google(Credential Manager)·카카오 로그인 래퍼 `SocialTokenProvider`(Route가 `Context.socialTokenProvider()`로 얻고 `Context.findActivity()`로 Activity를 찾음), 소셜 SDK 정리, 카카오 SDK 초기화 (`feature:auth`·`feature:settings`가 함께 씀, feature끼리 의존할 수 없어서 여기에 둠). `push` 패키지: 푸시 Intent 엑스트라 이름 `PushIntentExtras`와 `Intent.toPushTapTarget()` (`core:push`가 넣고 `feature:main`이 읽음) |
| `domain` | Kotlin JVM (`duriseo.kotlin.jvm`) | 있음 | 순수 Kotlin. Android 의존 없음. `error`·`result`: `DataError`·`DataResult`·`ErrorCode`. `network`: `NetworkMonitor`. `auth`: 인증 모델과 `AuthRepository`·`TokenStorage`. `me`: `Me`·`MeRepository`(공간 변경·해제 감지 `SpaceChange`). `couple`: `CoupleRepository`, `InviteCodeInput`, `InviteRateLimit`, `StartDateRules`. `event`: `EventRepository`, 모델, `EventRules`(날짜 범위·글자 수). `anniversary`: `AnniversaryRepository`, 모델(`AnniversaryOccurrence`·`PastAnniversary`·`Anniversary`), `AnniversaryRules`(이름 30자·D-N 강조), `DayCount`(함께한 지 D+N 최소 1, 자동 기념일 카드 D+N), `calendarMonthRange`(월 조회 앞뒤 하루). `memo`: 메모 저장소·모델. `notification`(단위 7): `NotificationRepository`(첫 페이지 교체·다음 페이지·안 읽은 개수·`markRead`(기기 먼저, D-127)·`setListOpen`/`isListOpen`), `AppNotification`·`NotificationKind`·`NotificationTargetType`·`PushTapTarget`. `device`: `DeviceRegistrar`·`PushTokenProvider`(FCM 토큰을 얻는 곳, 구현은 `core:push`). `sync`: `SyncSignal`·`SyncSignalReceiver`(push가 부름)·`SyncSpaceSignals`(`MainViewModel`이 `LINKED`·`UNLINKED`를 들음). `EventRepository.forget`·`MemoRepository.forget`: 요청 없이 기기에서만 지움(동기화 `DELETED`) |
| `data:remote` | library, hilt | 있음 | 응답 형식 1.1·오류 변환(`ApiCaller`), `AndroidNetworkMonitor`. 인증 없음(`@NoAuth`)·인증(`@Authed`) 클라이언트, 인증 헤더 인터셉터, 토큰 갱신 `Authenticator`. API: `AuthApi`·`AuthSessionApi`·`MeApi`(`auth/`, `me/`), `CoupleApi`(API.md 3.2의 7개 경로), `EventApi`(일정 7개 경로, 일시는 초 단위 UTC `Z`), `AnniversaryApi`(기념일 6개 경로, 커서 페이지·`PATCH`는 바꾼 필드만). `NotificationApi`(알림 3개 경로, 모르는 `type`은 건너뜀)와 `MeRemoteDataSource.putDevice`(`PUT /me/devices`). 각 `*RemoteDataSource`가 Retrofit·DTO를 감싸 도메인 모델만 내보냄 |
| `data:local` | library, hilt | 있음 | DataStore 제공, Room(`DuriseoDatabase` 버전 6: `me`·`event`·`anniversary_occurrence`(캘린더 범위·다가오는 목록, 응답 위치 저장)·`anniversary_past`·`anniversary_page_state`(다음 커서·지난 기념일 개수) 테이블과 단위 6 `memo`·`memo_page_state`, 단위 7 `notification`·`notification_state`(응답 위치 저장, 다음 커서·안 읽은 개수, 일시는 epoch 밀리초). 알림은 사용자 것이라 공간이 바뀌어도 지우지 않고 로그아웃·계정 전환 때 `clearAllTables`가 지움. 출시 전이라 destructive 재생성), Tink로 암호화한 토큰 저장소, 확인한 공간 ID(`AcknowledgedSpaceLocalDataSource`), 로그아웃·세션 끊김 때 지우기(`LocalDataCleaner`) |
| `data:repository` | library, hilt | 있음 | `runOnlineWrite`(오프라인이면 API를 부르지 않음). `AuthRepository`·`MeRepository`·`CoupleRepository`·`EventRepository`·`AnniversaryRepository` 구현. `SessionManager`(세션 끊김·정리), `SessionStartup`(앱 시작 때 로그인 상태·내 정보·시간대), 해제 감지(`MeWriteLock`으로 직렬화), 일정은 범위 새로고침(그 범위를 지우고 새로 넣음)이고 공간이 바뀌면 일정·기념일 캐시를 비움. 기념일은 응답 순서를 위치로 저장하고, 쓰기 성공 뒤 upcoming·past 첫 페이지와 마지막 캘린더 범위를 다시 불러옴. 커플 쓰기는 성공 뒤 `GET /me`로 Room 갱신. 단위 7: `NotificationRepositoryImpl`(알림 30일 경계 D-141: 오프라인 안 읽은 개수는 기기 값, 30일 지난 행 삭제), `DeviceRegistrarImpl`(토큰 저장소에 토큰이 있을 때만 `PUT /me/devices`, 로그인 직후 `AuthRepositoryImpl.afterLogin`이 뒤에서 호출), `sync/SyncDispatcher`(`SyncSignalReceiver`+`SyncSpaceSignals` 구현, `SyncBatcher`가 처음 신호 기준 1초 고정 창으로 모으고 같은 리소스·id는 마지막 것만 처리, 리소스별 처리는 API.md 6장) |
| `feature:auth` | feature | 있음 | 단위 2. SCR-START-01 시작 화면(게스트 시작, Google·카카오 로그인). `feature:main`이 의존해 NavHost에 연결 |
| `feature:couple` | feature | 있음 | 단위 3. SCR-COUPLE-01 커플 연결(회원·게스트), SCR-COUPLE-02 초대코드 만들기(남은 시간·재요청 대기·5초 확인), SCR-COUPLE-03 초대코드 입력(오류·입력 제한), SCR-COUPLE-04 연결 완료 시트(`linkedsheet/`, 띄우는 곳은 `feature:main`). `feature:main`이 의존해 NavHost에 연결 |
| `feature:anniversary` | feature | 있음 | 단위 5. SCR-ANNIV-01 기념일 목록(`list/`: 함께한 날 카드·처음 만난 날 안내 카드(처음 만난 날 시트는 `core:designsystem`의 `StartDatePickerSheet`를 재사용하고 저장은 `CoupleRepository.updateStartDate`)·다가오는 목록 커서 이어 붙이기·접힌 지난 기념일), SCR-ANNIV-02 직접 기념일 작성·수정·삭제(`edit/`: 날짜 시트는 일정과 같은 부품 1950~2050). 캘린더에서 들어가는 길은 `navigateToAnniversaries`(`feature:main`의 `MainNavigator`)를 부른다. `feature:main`이 의존해 NavHost에 연결 |
| `feature:memo` | feature | 있음 | 단위 6 묶음 B. SCR-MEMO-01 메모 목록(`list/`: 고정 영역(3개까지 + 더보기)·보기 칩·무한 스크롤·핀·별, FAB), SCR-MEMO-02 고정 메모 전체(`pinned/`), SCR-MEMO-03 메모 상세(`detail/`: 삭제 확인, D-121), SCR-MEMO-04 메모 작성·수정(`edit/`). `edit/`의 `MemoEditViewModel`·`MemoForm`·`MemoDateSheet`는 공개 부품이라 SCR-CAL-03의 [메모] 탭 슬롯(`feature:calendar`의 `EventEditMemoTab`)을 `feature:main`의 `rememberEventEditMemoTab`이 채워 같이 쓴다(확인 필요 3, 묶음 C). 새 메모는 이 모듈의 화면이 아니라 그 탭이고, 이 모듈의 `edit/` 화면은 수정 전용이다. `component/`: 핀 카드·메모 행·메모 날짜 칩·핀/별 버튼. `navigation/`: `memoNavGraph`(탭 루트 + 하위 3개)와 `navigateTo…` 함수. 핀·별·삭제·저장은 로딩 덮개(D-115). 핀·별 아이콘은 `core:designsystem` |
| `core:push` | library, hilt | 7 (묶음 A·B) | 템플릿 계획의 `push`를 `core:push`로 옮김(feature가 아니라 앱 전체가 쓰는 부품이라). FCM 수신 `DuriseoMessagingService`(`onNewToken` → 기기 등록, `onMessageReceived` → `type`으로 `PUSH`는 앱이 알림으로 그리고 `SYNC`는 프로세스가 살아 있을 때만 `SyncSignalReceiver`에 넘김, `ProcessUiState`), 파싱 순수 함수 `PushMessageParser`, 알림 채널 5개 `NotificationChannels`(ID `event_reminder`·`partner_created`·`memo_pinned`·`anniversary`·`couple`, 이름은 D-136, 중요도 기본), `PushNotifier`(작은 아이콘 `ic_stat_duriseo` 흰 실루엣 + 색 brand-500, 누르면 `MainActivity`로 `notificationId`·`kind`·`targetType`·`targetId` 엑스트라), `FirebasePushTokenProvider`(`PushTokenProvider` 구현), `PushInitializer`(앱 시작: 채널 + 토큰 등록), `FcmAvailability`(`app/google-services.json`이 없으면 FCM을 끈 채 동작, 경고 로그 한 줄). 엑스트라 이름·`Intent.toPushTapTarget()`은 `core:common`의 `push` 패키지 |
| `feature:notification` | feature | 7 (묶음 C 화면) | SCR-NOTI-01. 아직 없음 (데이터 부분 `NotificationRepository`는 아래 `domain`·`data:*`에 있음) |

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
| 테스트: 메모리 Room | Robolectric 4.15.1 + `androidx.test:core` 1.6.1 (`data:local`, `data:repository`의 `testImplementation`. `feature:settings`도: `SavedStateHandle.toRoute` 테스트) | 2 (추가됨) |
| 푸시 | Firebase Messaging (`firebase-bom` 34.4.0 + `firebase-messaging`)와 google-services 플러그인 4.4.3. 플러그인은 `app/google-services.json`이 있을 때만 적용 | 7 (추가됨, 버전은 CI로 확인) |
| 프로필 이미지 | Coil | 8 |
| 테스트 | kotlinx-coroutines-test, Turbine, OkHttp MockWebServer | 1 (추가됨) |
