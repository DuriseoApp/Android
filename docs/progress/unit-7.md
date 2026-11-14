# 단위 7: 알림 — FCM, 동기화 신호, 알림 권한, SCR-NOTI-01, SCR-SET-02

> 브랜치: `feat/7-notification` · PR: #18 · 상태: 진행 중 (2026-11-14 범위 승인, 확인 필요 1~5 추천안대로)
> 기준: PRD v0.12 (FR-NOTI-01~05, FR-CAL-01 AC4, NFR-SYNC-01·02, D-39·D-43·D-70·D-85·D-127·D-129·D-136·D-141~D-145), SCREENS.md 공통 표(푸시 진입·알림 목록·알림 권한·날짜·시각 표기·요청 중 35~46행)·SCR-CAL-01 알림 버튼·SCR-NOTI-01·SCR-SET-02, API.md 1.1·3.1(`PUT /me/devices`, `/me/notification-settings`)·3.5·6장(SYNC 수신 시 앱 동작)·7장(푸시 형식)
> Docs 기준 커밋: DuriseoApp/Docs main `cee0885`
> 앞 단위: 단위 6 머지됨 (main `ed18952`). 서버 단위 6(알림·SYNC·푸시)·7(사진) 머지됨

## 요약

| 묶음 | 내용 |
| --- | --- |
| A. FCM·기기 | 새 모듈 `core:push`(MODULES.md의 `push`): Firebase Messaging(BOM), `FirebaseMessagingService`, 토큰 → `PUT /me/devices`(로그인·토큰 갱신·앱 시작), 알림 채널 5개(D-136), data 메시지 `PUSH`를 앱이 직접 그리기 |
| B. 동기화 신호 | `SYNC` 수신(프로세스가 살아 있을 때만, 1초 묶기): `EVENT`·`MEMO` 단건 `GET`/`DELETED`는 지우기, `ANNIVERSARY` 범위·upcoming·past, `SPACE` `UPDATED` → `GET /me`·`GET /space`·기념일, `LINKED`·`UNLINKED` → `GET /me` 뒤 D-125 경로, `NOTIFICATION` → 안 읽은 개수(+열린 목록 첫 페이지) |
| C. 알림 목록 | `NotificationRepository`(Room, 커서 페이지, 안 읽은 개수, 읽음은 기기 먼저 D-127), 새 모듈 `feature:notification`(SCR-NOTI-01), SCR-CAL-01 알림 버튼 점(FR-CAL-01 AC4) |
| D. 푸시 진입 | 푸시 탭 → 읽음 처리 → 홈을 뒤에 두고 대상 화면(D-142·D-143), 삭제된 대상은 스낵바, 세션 없으면 SCR-START-01. 알림 목록에서 누른 것도 같은 이동 |
| E. 알림 권한·설정 | 알림 권한(Android 13+, 세 시점 공유 기록 D-144), SCR-SET-02(`GET`·`PATCH /me/notification-settings`, D-145, 이슈 #15 단위 7분) |

## 확인 필요

| # | 빈 곳 | 추천안 | 근거 |
| --- | --- | --- | --- |
| 1 | `google-services.json`이 레포에 없고(SETUP.md), CI에도 아직 없음. 없으면 google-services 플러그인이 빌드를 깨요 | **파일이 있을 때만** 플러그인을 적용하고, 없으면 FCM을 끈 채(토큰 등록·수신 없음, 경고 로그) 빌드돼요. CI는 Secret `GOOGLE_SERVICES_JSON`(base64)이 있으면 파일을 만들고, 없어도 초록불. **Secret 등록은 사람이** 해요(SETUP.md에 절차) | environment.md "Android CI는 GitHub Secrets (단위 7부터)" |
| 2 | 상태바 알림 작은 아이콘·색이 디자인에 없음 | 앱 아이콘 모양을 흰 실루엣 벡터로 쓰고 색은 `brand-500`. Docs 후속(`/design-sync`)에 올려요 | Android 알림 규칙 |
| 3 | 푸시로 들어갈 때 "홈(캘린더 또는 메모 탭)"을 어느 쪽으로 | 메모 대상(상대 메모 추가·메모 고정)은 메모 탭, 그 밖(일정·기념일·연결·해제)은 캘린더 탭 | SCREENS 공통 "푸시 진입" |
| 4 | 알림 채널 ID와 중요도 (D-136은 이름만) | ID는 영어 고정값 5개(`event_reminder`, `partner_created`, `memo_pinned`, `anniversary`, `couple`), 중요도 기본(소리·배지 있음, 헤드업은 시스템 기본) | API.md 7장 "채널 ID … 정하지 않는다" |
| 5 | 앱이 꺼진 상태(프로세스 없음)에서 온 `SYNC` | 문서대로 무시(다음 진입 때 다시 읽기). `PUSH`는 프로세스가 없어도 `FirebaseMessagingService`가 그려요 | API.md 6장 "SYNC 수신 시 앱 동작" |

## 범위

테스트 이름에 ID를 넣는다. Preview 이름은 `<화면>Preview_<상태>`.

### A. FCM·기기

| ID | 내용 | 테스트 |
| --- | --- | --- |
| API `PUT /me/devices` | 로그인 직후·토큰 갱신(`onNewToken`)·앱 시작(로그인 상태)에 토큰 등록, 실패는 다음 시작 때 다시. 토큰은 로그에 남기지 않음(NFR-SEC-01) | `DeviceRegistrarTest: 로그인 상태에서 토큰이 바뀌면 PUT me devices를 부른다` · `…: 로그아웃 상태면 등록하지 않는다` |
| D-136 | 채널 5개 만들기(이름은 알림 종류 이름) `<확인 필요 4>` | `NotificationChannelsTest: D-136 채널 5개를 종류 이름으로 만든다` |
| API.md 7장 푸시 형식 | `type=PUSH` data → `kind`별 채널로 알림 그리기(`title`·`body`), 누르면 D 묶음 진입 Intent(`notificationId`·`kind`·`targetType`·`targetId`). 작은 아이콘 `<확인 필요 2>` | `PushMessageParserTest: PUSH data를 종류 제목 본문 대상으로 읽고 모르는 kind는 버린다` |
| `<확인 필요 1>` | `google-services.json` 있을 때만 플러그인, CI Secret | CI 초록불 (파일 없이) |

### B. 동기화 신호 (API.md 6장)

| ID | 내용 | 테스트 |
| --- | --- | --- |
| NFR-SYNC-01 | `type=SYNC` 파싱, 1초 안의 신호는 모아서 한 번(같은 리소스·id 중복 제거) | `SyncDispatcherTest: NFR-SYNC-01 1초 안에 온 신호는 한 번만 처리한다` |
| `EVENT`·`MEMO` | `id` 단건 `GET`(404면 지움), `DELETED`는 기기에서 지움 | `SyncDispatcherTest: EVENT UPDATED는 그 일정을 다시 읽고 DELETED는 기기에서 지운다` · `…: MEMO 신호는 메모를 다시 읽고 목록 고정을 갱신한다` |
| `ANNIVERSARY` | 보이는 범위·upcoming·past 다시 조회 | `SyncDispatcherTest: ANNIVERSARY 신호는 기념일을 다시 불러온다` |
| `SPACE` `UPDATED` | `GET /me`·`GET /space`·기념일 | `SyncDispatcherTest: SPACE UPDATED는 내 정보 공간 기념일을 다시 읽는다` |
| `SPACE` `LINKED`·`UNLINKED` (D-125, 4.4) | `GET /me` 뒤 기존 해제·연결 경로(MainViewModel). SCR-COUPLE-02 5초 확인은 그대로 두되 신호가 오면 바로 확인 | `MainViewModelTest: NFR-SYNC-01 LINKED 신호를 받으면 GET me로 확인하고 캘린더와 연결 완료 시트로 간다` · `…: UNLINKED 신호를 받으면 D-125 경로로 간다` |
| `NOTIFICATION` | 안 읽은 개수 다시, 목록이 열려 있으면 첫 페이지도 | `SyncDispatcherTest: NOTIFICATION 신호는 안 읽은 개수를 다시 읽는다` |
| `<확인 필요 5>` | 프로세스가 없으면 무시 | — |

### C. 알림 목록 (SCR-NOTI-01, FR-NOTI-05)

| ID | 내용 | 테스트 · Preview |
| --- | --- | --- |
| API `GET /notifications`, `GET /notifications/unread-count` | Room 저장, 첫 페이지 교체·다음 페이지 이어 붙이기, 캐시 세대(단위 5 방식) | `NotificationRepositoryTest: 첫 페이지는 교체하고 다음 페이지는 이어 붙인다` · `…: 캐시를 지운 뒤 도착한 이전 응답은 저장하지 않는다` |
| FR-NOTI-05 AC2 (D-141) | 최근 30일 최신순(서버 순서), 항목: 종류 아이콘·제목·본문·받은 시각(D-129, 단위 6 `writtenAtText`)·안 읽음 점 | `NotificationListViewModelTest: FR-NOTI-05 AC2 목록은 서버 순서이고 받은 시각은 D-129 표기다` · Preview `NotificationListPreview_Default`, `_Read`, `_Withdrawn`, `_Empty` |
| FR-NOTI-05 AC6 (D-127) | 누르면 기기에서 먼저 읽음, 바로 이동, 읽음 요청은 덮개 없이 뒤에서(실패·오프라인도 안내 없음, 다음 불러오기에서 서버 값으로) | `NotificationListViewModelTest: FR-NOTI-05 AC6 누르면 기기에서 먼저 읽음으로 바꾸고 바로 이동한다` · `…: 읽음 요청이 실패해도 이동하고 안내하지 않는다` |
| FR-NOTI-05 AC5 (D-43) | 목록에서 누른 대상이 삭제됐으면 읽음 + "삭제된 일정이에요"/"삭제된 메모예요", 목록에 머묾 | `NotificationListViewModelTest: FR-NOTI-05 AC5 대상이 삭제됐으면 안내하고 목록에 머문다` |
| SCR-NOTI-01 | 앱바 "알림", 스낵바 위 40(D-126), 무한 스크롤, 화면 진입 때 다시 불러오기 | `SnackbarPositionTest: D-126 알림 목록과 알림 설정은 위 40이다` |
| FR-CAL-01 AC4 | SCR-CAL-01 알림 버튼 점(안 읽은 개수 > 0), 접근성 라벨, 누르면 SCR-NOTI-01, 캘린더 진입·당겨서 새로고침 때 개수 다시 | `CalendarViewModelTest: FR-CAL-01 AC4 안 읽은 알림이 있으면 알림 버튼에 점이 보인다` · Preview `CalendarPreview_UnreadDot` |

### D. 푸시 진입 (FR-NOTI-03, D-142·D-143)

| ID | 내용 | 테스트 |
| --- | --- | --- |
| FR-NOTI-03 AC1·AC2 | 푸시 탭 → 그 알림 읽음(D-127과 같이 기기 먼저) → 대상: 일정 알림·상대 일정 추가 → SCR-CAL-02, 상대 메모 추가·메모 고정 → SCR-MEMO-03, 기념일 → SCR-ANNIV-01, 연결·해제·탈퇴 → SCR-SET-01 | `PushEntryTest: FR-NOTI-03 AC1 종류마다 정해진 화면으로 간다` |
| FR-NOTI-03 AC3 | 홈을 뒤에 두고 이동 `<확인 필요 3>`, 대상이 삭제됐으면 읽음 + 스낵바 + 홈에 머묾, 세션 없으면 SCR-START-01 | `PushEntryTest: FR-NOTI-03 AC3 일정이 삭제됐으면 캘린더에 머물며 삭제된 일정이에요를 보인다` · `…: 세션이 없으면 시작 화면으로 간다` |

### E. 알림 권한·설정

| ID | 내용 | 테스트 · Preview |
| --- | --- | --- |
| FR-NOTI-01 AC1~AC3 (D-70, D-144) | Android 13+에서 처음 알림이 필요한 기능 때 한 번: 일정 알림 설정(SCR-CAL-02·03 알림 시트에서 "없음"이 아닌 값을 고를 때), 연결 완료 시트를 닫은 직후, 직접 기념일 저장 성공 직후. "이미 물었는지" 기록 하나 공유(DataStore), 거부 뒤 다시 묻지 않음 | `NotificationPermissionGateTest: FR-NOTI-01 AC3 세 시점이 기록 하나를 함께 쓰고 한 번만 묻는다` · ViewModel별 `…: FR-NOTI-01 AC1 … 뒤에 권한 요청 효과를 낸다` |
| FR-NOTI-02 AC1·AC2 (D-145, D-62) | SCR-SET-02: 스위치 4개, 안내 2줄, 누르면 바뀐 종류만 `PATCH`(로딩 덮개 D-115), 실패·오프라인이면 원래 값, 들어올 때마다 서버 값, 한 번도 못 받으면 비활성 + 공통 오류(이슈 #15). SCR-SET-01 "알림 설정" 행 연결 | `NotificationSettingsViewModelTest: FR-NOTI-02 AC2 스위치를 누르면 바뀐 종류만 저장하고 실패하면 원래 값이다` · `…: D-145 한 번도 받지 못하면 스위치가 비활성이다` · Preview `NotificationSettingsPreview_Default`, `_Mixed`, `_LoadFailed` |

## 서버 연동

| API | 개발 서버 배포 여부 | 확인 |
| --- | --- | --- |
| 알림 3개, 알림 설정 2개, `PUT /me/devices`, SYNC·푸시 (서버 단위 6) | 배포됨 | 실기기 2대 (맥, Firebase 키 등록 뒤) |

## 범위 밖

- SCR-SET-04 프로필(사진 업로드·표시) → 단위 8
- iOS

## 결정

구현하면서 정한 것 중 문서에 없는 것. 문서에 있어야 할 결정이면 여기 적지 말고 작업을 멈추고 보고한다.

묶음 A·B·C(데이터 부분) 구현 결정:

1. **모듈 이름**: MODULES.md 계획의 `push`를 `core:push`로 만들었다 (feature가 아니라 앱 전체가 쓰는 부품). `MODULES.md` 1장을 고침.
2. **google-services (확인 필요 1)**: `app/build.gradle.kts`가 `app/google-services.json`이 있을 때만 플러그인을 적용한다. 없으면 `FcmAvailability`가 false라 토큰 등록·`PushInitializer`의 등록을 건너뛰고 경고 로그 한 줄. CI는 Secret `GOOGLE_SERVICES_JSON`(base64)이 있을 때만 파일을 만든다. Firebase BoM 34.4.0·google-services 4.4.3은 Gradle을 못 돌리는 환경이라 **버전 존재를 못 확인**했다 (CI로 확인).
3. **`SPACE` `UPDATED`는 `GET /me`·`GET /space`·기념일을 다시 읽는다** (API.md 6장, 리뷰 반영): `MeApi`·`MeRemoteDataSource`에 `GET /space`(3.2 `Space`)를 더하고 `MeRepository.refreshSpace()`가 응답을 Room의 내 정보에 합친다(구성원 닉네임·사람 색·사진 유무, 함께 색, 처음 만난 날, 연결한 날). `GET /me` 뒤에 부르고, 저장된 내 정보의 공간 ID와 다르면(그 사이 공간이 바뀜) 저장하지 않는다(공간이 바뀐 것은 `GET /me`가 처리). `MeWriteLock`·캐시 세대로 늦은 응답을 버린다. **사진 버전은 Room에 필드가 없어 반영하지 못한다**(단위 8의 사진 화면 때 더한다).
4. **1초 묶기**: 처음 신호가 온 뒤 1초 고정 창(디바운스처럼 계속 늘리지 않음, 지연 최대 1초). 같은 리소스·id는 마지막 신호만 처리한다 (`EVENT` `UPDATED` 뒤 `DELETED`면 `DELETED`). 신호가 올 때만 코루틴이 생긴다.
5. **프로세스가 살아 있을 때만 (확인 필요 5)**: 이 프로세스에서 Activity가 한 번이라도 만들어졌으면(`ProcessUiState`, `MyApp`이 표시) 살아 있던 앱으로 보고 `SYNC`를 처리한다. FCM이 깨우려고 새로 만든 프로세스(화면 없음)의 `SYNC`는 무시한다. `PUSH`는 어느 쪽이든 그린다.
6. **`LINKED`·`UNLINKED`**: `SyncDispatcher`는 `SyncSpaceSignals`로 알리기만 하고, `GET /me`와 이동은 `MainViewModel`이 한다. 해제는 `refreshMe`가 저장 전에 감지해 기존 `spaceChanges` → D-125 경로로 가고, 이미 초기화돼 있으면(Room이 개인 공간) 아무 일도 없다. 연결은 "개인 공간 → 커플 공간"으로 바뀐 기기만 캘린더로 보내고(`SpaceRelinked`, 시트는 기존 상태가 띄움), 이미 커플이던 기기(내가 연결한 쪽)는 이동하지 않는다 (백스택을 또 비우지 않게). 구독자(`MainViewModel`)가 없을 때 온 신호는 버려진다.
7. **알림 안 읽은 개수 (D-141, FR-NOTI-05 AC7)**: `observeUnreadCount`는 온라인이고 서버 개수를 받은 적이 있으면 서버 값, 아니면 기기에 저장된 30일 안(`created_at > now − 30일`)의 안 읽은 알림 개수다. 30일 지난 행은 목록에서 빼고 불러오기 때마다 지운다. 일시는 Room에 epoch 밀리초로 저장한다.
8. **알림 지우기**: 알림은 사용자 것이라 공간이 바뀌어도 지우지 않는다(`MeRepositoryImpl`을 고치지 않음). 로그아웃·계정 전환·세션 끊김은 `clearAllTables`가 지운다. 해제 때 서버가 알림을 정리(D-45)하므로 화면이 들어올 때 첫 페이지를 다시 받아 맞춘다 (`SyncDispatcher`는 `LINKED`·`UNLINKED`에서 알림을 따로 다시 읽지 않고, 서버가 보내는 `NOTIFICATION` 신호에 맡긴다).
9. **`markRead`**: 기기 Room을 먼저 읽음으로 바꾸고(안 읽음이었으면 개수 -1) 바로 돌아온다. 오프라인이면 요청을 보내지 않고, 온라인이면 `@ApplicationScope`에서 `PUT`을 보낸다. 실패(404 포함)는 로그만. 성공하면 `unread-count`를 서버 값으로 다시 읽는다. 목록에 없는 id(푸시로 들어옴, D-142)도 서버에는 보낸다. 서버 값이 오기 전에 첫 페이지를 다시 받으면 잠깐 안 읽음으로 보일 수 있다 (D-127 "다음 불러오기에서 서버 값으로").
10. **기기 등록**: 로그인 직후는 `AuthRepositoryImpl.afterLogin`이 뒤에서(`@ApplicationScope`) 부르고(게스트·소셜·switch 공통, 토큰 얻기가 로그인을 늦추지 않게), 앱 시작은 `MyApp` → `PushInitializer.start()`, 토큰 갱신은 `DuriseoMessagingService.onNewToken`. 모두 `DeviceRegistrarImpl`이 토큰 저장소에 토큰이 있을 때만 보낸다. 앱 시작마다 `PUT`이 한 번 나간다 (문서: "앱 실행 때 부른다").
11. **푸시 Intent (확인 필요 2·D 묶음 연결)**: 컴포넌트는 런처 Intent에서 찾아 `MAIN`·`LAUNCHER` 없이 `NEW_TASK | CLEAR_TOP | SINGLE_TOP`으로 보낸다 (이미 떠 있는 앱에 엑스트라가 전달되도록 `onNewIntent`). 엑스트라 이름 `notificationId`·`kind`·`targetType`·`targetId`와 읽기 `Intent.toPushTapTarget()`은 `core:common`의 `push` 패키지(`PushIntentExtras`). `MainActivity.handlePushIntent`가 D가 채울 자리다. 알림 PendingIntent 요청 코드는 알림 ID의 해시, 알림은 태그(알림 ID)로 구분한다.
12. **작은 아이콘 (확인 필요 2)**: 앱의 `ic_launcher_foreground` 모양을 흰 한 색으로 옮긴 `ic_stat_duriseo`, 강조색은 `NotificationAccentArgb`(= `Brand500`, `core:designsystem`). 지금 런처 아이콘이 아직 템플릿 모양(안드로이드 로봇)이라 알림 아이콘도 그 모양이다 — 앱 아이콘이 확정되면 같이 바꿔야 한다 (Docs `/design-sync` 후속).
13. **채널 (확인 필요 4)**: ID `event_reminder`·`partner_created`·`memo_pinned`·`anniversary`·`couple`, 중요도 기본. `COUPLE_LINKED`·`COUPLE_UNLINKED`는 `couple` 채널 하나.
14. **`POST_NOTIFICATIONS`**: 매니페스트에 선언만 했다 (없으면 Android 13+에서 알림이 안 그려진다). 요청 시점은 E 묶음. 권한이 없으면 `PushNotifier.show`는 그리지 않고 false를 돌려준다 (알림 목록에는 영향 없음).
15. **`forget`**: `EventRepository`·`MemoRepository`에 `forget(id)`(요청 없이 기기에서만 지움)를 더했다 (`SYNC` `DELETED`용). 단건 새로고침 `refreshEvent`·`refreshMemo`와 404 지우기는 이미 있었다. 이 인터페이스를 구현하는 feature 테스트 가짜 3곳(`feature:calendar`·`feature:memo`의 `Fakes.kt`)에 함수를 더했다.
16. **Room 버전 6**: `notification`·`notification_state`. 스키마 JSON(`data/local/schemas/6.json`)은 빌드가 만들어 주므로 맥에서 커밋해야 한다 (사람).

묶음 C(화면)·D·E 구현 결정:

17. **누른 알림 처리 `NotificationTapHandler`**(domain 인터페이스, `data:repository` 구현): `markRead`(기기 먼저) → 갈 곳 확인. 일정·메모는 Room에 있으면 그대로 이동, 없으면 온라인일 때 단건 `GET`을 해 보고 404 `NOT_FOUND`면 삭제된 대상, 오프라인·그 밖의 오류면 그냥 이동(대상 화면이 자기 오류를 처리). 알림 목록과 푸시 진입이 같은 함수를 쓰고 홈을 뒤에 두는지만 다르다(`MainNavigator.openFromPush` / `openFromNotificationList`). 대상이 Room에 없을 때만 목록 클릭이 단건 `GET` 한 번을 기다린다(덮개 없음).
18. **알림 목록 문구**: 안내 문장 "최근 30일 알림이에요. 누르면 관련 화면으로 가요."·빈 상태 "최근 30일 동안 받은 알림이 없어요"·종류 이름 7개(일정 알림, 상대 일정 추가, 상대 메모 추가, 메모 고정, 기념일, 연결, 연결 해제)는 SCREENS.md에는 없고 보드·이미지(`Notifications.dc.html`)에만 있다. 보드 그대로 썼다. `PARTNER_CREATED`는 대상이 메모면 "상대 메모 추가"(메모 아이콘), 아니면 "상대 일정 추가"(달력 아이콘). 보드의 접근성 라벨(`안 읽음, 종류, 시각`은 제목·본문을 가린다) 대신 항목을 하나로 합쳐 읽고 안 읽음은 상태 설명("안 읽음")으로 읽게 했다.
19. **받은 적 없는 알림 목록**: `NotificationList.isLoaded`가 false인 빈 목록(첫 페이지를 한 번도 못 받음)은 빈 상태 문구를 보이지 않고 앱바만 보인다. 문서에 정해진 것이 없는 자리다.
20. **알림 설정 마지막 값 저장**: 설정 DataStore에 4개 boolean(`notification_setting_*`)로 저장하고 4개가 다 있어야 "받은 적 있음"이다. 로그아웃·탈퇴·세션 끊김 때 `LocalDataCleaner`가 설정을 지우므로 계정이 바뀌면 처음부터다. 읽기 실패는 저장된 값이 없을 때만 공통 오류 스낵바(오프라인이어도 공통 오류, 문서 그대로). 쓰기 실패는 `toMessageResOrNull`(오프라인은 "인터넷 연결이 필요해요").
21. **알림 권한 "이미 물었는지"**: 설정 DataStore의 `notification_permission_asked` 하나. 한 번의 `edit`에서 읽고 쓰므로 동시에 불러도 `true`는 한 번이다. **기기 단위 기록이라 로그아웃·탈퇴·세션 끊김에도 남긴다**(리뷰 반영): `LocalDataCleaner.clearUserData()`가 같은 `edit` 안에서 `DeviceLevelKeys`(이 키 하나)를 빼고 지운다. 별도 DataStore 파일을 만들지 않은 까닭은 파일·Hilt 제공 코드가 늘어서다. 계정이 바뀌어도 같은 기기에서는 다시 묻지 않는다(시스템 권한창을 반복해 띄우지 않음, D-144).
22. **알림 권한 요청 위치**: `core:designsystem`의 `ProvideNotificationPermissionRequester`를 `MainActivity`가 맨 위층에 한 번 두고(`LocalNotificationPermissionRequester`), ViewModel은 `RequestNotificationPermission` 효과만 낸다. 화면이 곧 닫혀도(기념일 저장 뒤) 요청이 끊기지 않게 맨 위층의 런처를 쓴다. Android 13 미만·이미 허용이면 요청하지 않는다. 결과는 쓰지 않는다.
23. **세 시점**: SCR-CAL-02는 "없음"이 아닌 알림을 서버에 저장한 성공 뒤, SCR-CAL-03은 시트에서 "없음"이 아닌 칩을 누른 순간(시트는 [완료]까지 열려 있음. 저장 전이지만 "고른 뒤"로 봄), 연결 완료 시트는 닫는 순간(`[시작하기]`·닫기·뒤로 가기 모두, 시트가 사라지면 효과 수집도 멈추므로 `acknowledgeSpace` 직전에 효과를 냄), 직접 기념일은 작성·수정 저장 성공 직후(삭제는 아님, 화면이 닫히기 직전에 효과를 냄).
24. **푸시 진입 홈 탭(확인 필요 3)**: 메모 대상(상대 메모 추가·메모 고정, 삭제된 메모)은 메모 탭, 그 밖은 캘린더 탭. 푸시마다 백스택을 비우고 홈부터 다시 쌓는다(작성 중이던 화면은 닫힘). 세션 없으면 읽음 처리도 하지 않고 SCR-START-01. 처음 켤 때는 토큰 저장소를 다 읽은 뒤(`authState != Unknown`)에 판단한다. 효과는 `Channel`이라 NavHost가 준비되기 전에 와도 쌓였다가 처리된다.
25. **알림 버튼 점**: `NotificationRepository.observeUnreadCount() > 0`. 접근성 라벨은 보드 그대로 "알림" / "알림, 안 읽은 알림 있음". 점 위치는 보드(위 11·오른쪽 12, 지름 8, 흰 고리 2)이고 고리는 점 바깥에 붙어 보드보다 2씩 안쪽이다.
26. **알림 설정 스위치**: Material3 `Switch`(꺼짐 트랙 `line-300`은 design-system 매핑). 비활성(값을 한 번도 못 받음)은 보드대로 트랙 `line-100`·손잡이 `surface-0`+`line-200` 테두리. 줄 전체를 `toggleable(role = Switch)`로 만들어 글이 이름으로 읽힌다.
27. **빌드 확인 불가**: Gradle을 돌리지 않았다. import·시그니처는 눈으로만 확인했으므로 CI 결과를 봐야 한다.
28. **푸시는 로그인 세션이 있을 때만 그린다** (리뷰 Critical): `core:push`의 `PushDrawer`가 토큰 저장소 상태를 보고(읽는 중이면 최대 5초 기다림) `Present`일 때만 `PushNotifier.show`를 부른다. `DuriseoMessagingService`는 `PushDrawer`를 쓴다(FCM 작업 스레드라 `runBlocking`).
29. **로그아웃·탈퇴·세션 끊김·계정 전환 때 그려진 알림을 모두 지운다** (리뷰 Critical): domain `DrawnNotificationCleaner.cancelAll()`(Android를 몰라서 인터페이스), 구현 `core:push`의 `PushNotificationCleaner`(`NotificationManagerCompat.cancelAll`). `SessionManager.clearAll`·`clearUserDataOrFail`과 앱 시작 때 토큰이 없을 때(`SessionStartup`)에 부른다. 연결 해제 초기화(`resetLocked`, 로그인 유지)에서는 부르지 않는다.
30. **푸시 Intent 신뢰 표시** (리뷰 Minor): `MainActivity`는 런처 때문에 외부에 열려 있어 다른 앱이 엑스트라를 위조할 수 있다. **기기에 한 번 만든 무작위 값**을 앱 전용 SharedPreferences에 두고(`core:common`의 `PushIntentTrust`) 알림 Intent에 `pushTrust` 엑스트라로 넣어, `MainActivity`가 같은 값일 때만 처리한다. 프로세스마다 새 값을 쓰는 방식은 프로세스가 죽은 뒤 알림을 눌렀을 때 값이 달라 쓸 수 없어 버렸다(비공개 트램펄린 Activity는 매니페스트·화면이 늘어 더 복잡하다). 최근 앱 기록에서 다시 연 Intent(`LAUNCHED_FROM_HISTORY`)는 무시하고, 처리했거나 걸러낸 Intent는 푸시 엑스트라를 지운다.
31. **누른 알림의 삭제 확인** (리뷰 Major): 온라인이면 Room에 있어도 단건 `GET`을 항상 한다. 404 `NOT_FOUND`면 삭제로 보고(단건 읽기가 Room에서도 지움) 오프라인이거나 그 밖의 오류면 확인 없이 이동한다.
32. **연결 신호 중복 이동 방지** (리뷰 Minor): `MainViewModel`은 같은 공간으로 `SpaceRelinked`를 한 번만 낸다(해제하면 초기화). `MainNavigator.navigateToHomeIfNeeded`는 이미 캘린더 하나만 남았으면 다시 열지 않는다(연결 코드 화면의 5초 확인과 겹칠 때).
33. **알림 설정 응답 번호** (리뷰 Minor): 늦게 온 옛 `GET` 응답이 방금 한 `PATCH` 값을 덮지 않게 `NotificationSettingsRepositoryImpl`이 요청 번호를 매긴다(`GET`은 그 뒤에 시작한 요청이 없을 때만 저장, `PATCH`는 더 새 `PATCH`가 없을 때 저장). 화면(`NotificationSettingsViewModel`)은 Room 값만 구독하므로 덮어쓰기가 일어나는 저장소에서 막았다.

## 리뷰 기록

| 회차 | Critical | Major | 비고 |
| --- | --- | --- | --- |
| 1 (sonnet) | 1 | 3 | Critical: 로그아웃 뒤에도 알림창에 푸시가 남음(결정 28·29). Major: 권한 기록이 로그아웃에 지워짐(21), `SPACE UPDATED`가 `GET /space`를 안 부름(3), 알림 누를 때 Room만 보고 삭제 판단(31). Minor 4건(30·32·33 외 `LAUNCHED_FROM_HISTORY`)은 반영, 재리뷰 대기 |

## 남은 일

- 사람: Android 레포 GitHub Secret `GOOGLE_SERVICES_JSON` 등록, 맥 `app/google-services.json` 배치, Room 스키마 JSON
- 사람: 서버 Parameter Store Firebase 키 확인 뒤 실기기 2대로 푸시·SYNC 확인
