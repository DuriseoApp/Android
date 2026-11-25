# 단위 9: 운영 — 공지 팝업·업데이트 팝업·기기 정보

> 브랜치: `feat/9-app-ops` · PR: #38 · 상태: 진행 중
> 기준: PRD v0.22, SCREENS.md(캔버스 버전 51), API.md 1장(공통 요청 헤더)·1.1·3.1 `PUT /me/devices`·3.7·4.9·5장 426·R-20
> 디자인 이슈: #37 (Closes)

## 범위

### 묶음 A — 규칙과 데이터 (domain · data)

| ID | 내용 | 테스트 |
| --- | --- | --- |
| FR-APP-03 AC1, D-182 | 모든 API 요청(로그인 전·공개 조회 포함)에 `X-Platform: ANDROID`, `X-App-Version`(versionName), `X-App-Build`(versionCode), `X-OS-Version`(`Build.VERSION.SDK_INT`) | `CommonHeadersInterceptorTest: 인증 없는 요청과 인증 요청 모두 네 헤더를 붙인다` |
| FR-APP-03 AC2, API 3.1 | `PUT /me/devices`에 `osVersion`(API 레벨 문자열)·`appVersion`·`appBuild` | `DeviceRegistrarTest: 기기 등록에 OS 버전·앱 버전·빌드 번호를 보낸다` |
| API 3.7 | `GET /app-notices`, `GET /app-update-rules` (토큰 없음, 공통 헤더만) | `AppOpsRemoteDataSourceTest: 공지·규칙 목록을 읽는다`, `…: 모르는 필드는 무시한다` |
| D-176·D-177, API 3.7 마지막 문단 | Remote Config 키 `app_notices`·`app_update_rules`(값은 `{"items":[…]}` JSON 문자열)를 서버와 같은 파서로 읽음. 가져오기 간격 5분, 실시간 리스너(`addOnConfigUpdateListener` → `activate`) | `AppOpsJsonParserTest: Remote Config 값과 서버 응답을 같은 모양으로 읽는다` |
| SCREENS 운영 "값 거르기", D-183 | 제목 없는데 `https://` 이미지·`imageAlt` 없음, 제목 없이 본문만, 빈 문자열 → 그 항목 건너뜀. `linkUrl`이 `https://` 아니면 링크 숨김, `image`가 preset 4종·`https://` 아니면 이미지 없음. 규칙의 `mode`·`platform`·`build.op` 모름·필수값 없음 → 건너뜀 | `AppOpsJsonParserTest: D-183에 맞지 않는 항목은 건너뛴다`, `…: https가 아닌 링크는 숨긴다`, `…: 모르는 이미지 값은 이미지 없음이다`, `…: 읽을 수 없는 규칙은 건너뛴다` |
| FR-APP-01 AC1, API 3.7 "지금 유효함" | `(startsAt null 또는 ≤ now) AND (endsAt null 또는 now < endsAt)`, `platform` null 또는 `ANDROID` | `AppNoticeFilterTest: 기간과 플랫폼으로 거른다` |
| FR-APP-02 AC1, API 3.7 "업데이트 규칙 맞춤 판단" | 플랫폼 = ANDROID, 빌드 `EQ`·`LTE`·`GTE`·`RANGE`(양 끝 포함)·`IN`, `osVersion` min·max(양 끝 포함, null이면 통과) | `AppUpdateRuleMatcherTest: EQ LTE GTE RANGE IN`, `…: OS 버전 조건은 양 끝을 포함한다`, `…: 다른 플랫폼은 맞지 않는다` |
| FR-APP-01 AC5, FR-APP-02 AC5, D-178 | 같은 `id`는 Remote Config 값, 순서 강제 → 앱을 막는 공지 → 권장 → 알리는 공지, 같은 종류는 RC 목록 순 → 서버 목록 순. 공지와 규칙은 `id` 공간이 따로 | `AppPopupResolverTest: 같은 id는 Remote Config 값을 쓴다`, `…: D-178 순서로 하나를 고른다`, `…: 공지와 규칙의 같은 id는 서로 다른 것이다` |
| D-179, FR-APP-02 AC4 | "오늘 하루 안 보기"를 `id`별로 기기 시간대 오늘 날짜로 기억(기기 단위 키, 로그아웃·탈퇴로 안 지움). 강제는 숨기지 않음 | `AppPopupResolverTest: 오늘 숨긴 id는 건너뛰고 다음 날 다시 고른다`, `…: 강제는 오늘 하루 안 보기와 상관없다`, `PopupHiddenTodayLocalDataSourceTest: 로그아웃 정리에도 남는다` |
| FR-APP-01 AC6, 4.9 2단계 | 실행 때 RC와 서버 동시 요청, 서버는 3초까지만, 실패·시간 초과·오프라인이면 RC 값만, RC 못 받으면 마지막 활성 값, 둘 다 없으면 팝업 없음 | `AppPopupRepositoryTest: 서버가 3초 안에 답하지 않으면 Remote Config 값만 쓴다`, `…: 두 곳 모두 없으면 팝업이 없다` |
| FR-APP-02 AC6, 4.9 7단계, 5장 426 | 어느 요청이든 426 `APP_VERSION_UNSUPPORTED` → `details`(`ruleId`·`title`·`body`·`image`·`imageAlt`)로 강제 팝업 신호. 토큰·로컬 데이터 안 지움, 공통 오류 스낵바 없음. `ErrorCode`에 `APP_VERSION_UNSUPPORTED` 추가 | `ForceUpdateInterceptorTest: 426을 받으면 강제 업데이트 신호를 낸다`, `…: details가 비면 기본 문구 표시용 null이다`, `DataErrorMessageTest: APP_VERSION_UNSUPPORTED는 스낵바 문구가 없다` |
| Q-23 | `https://` 이미지는 띄우기 전에 받음. 실행 때는 확인 시작부터 3초 안(서버 대기와 합쳐), 그 밖에는 이미지만 3초. 못 받으면 이미지 없이(이미지만 넣은 팝업은 `imageAlt`를 제목 자리에) | `AppPopupCoordinatorTest: 3초 안에 이미지를 못 받으면 이미지 없이 띄운다` |

### 묶음 B — 팝업 화면과 동작 (feature:appops 새 모듈 · core:designsystem · core:common)

| ID | 내용 | 테스트 / Preview |
| --- | --- | --- |
| SCR-APP-01 | 앱을 막는 공지 [앱 종료], 알리는 공지 [오늘 하루 안 보기 \| 확인], 링크 버튼(본문 아래 아웃라인, 브라우저로 열고 팝업 유지), 이미지 preset 112 원형 / `https://` 폭 280 원래 비율 모서리 16, 이미지만 넣은 팝업, 긴 본문은 내용 영역만 스크롤(넘치면 아래 선 `line-100`) | `AppNoticePreview_Block`, `_BlockNoImage`, `_BlockLink`, `_BlockImageOnly`, `_LongBody`, `_Info`, `_InfoNoImage`, `_InfoLink`, `_InfoImageOnly`, `_ImageOnlyFailed`(imageAlt가 제목 자리) |
| SCR-APP-02 | 강제 [앱 종료 \| 업데이트], 권장 [업데이트] → [오늘 하루 안 보기 \| 닫기], 이미지 없으면 `preset:update`, 426 기본 문구 | `AppUpdatePreview_Force`, `_ForceDefault`, `_ForceImageOnly`, `_Recommend`, `_RecommendImageOnly` |
| D-179, Q-21 | 앱을 막는 공지·강제: 뒤로 가기·밖 누르기 무시. 알리는 공지·권장: 뒤로 가기 = [확인]/[닫기](이번만), 밖 누르기 무시 | `AppPopupViewModelTest: 닫을 수 없는 팝업은 뒤로 가기를 무시한다`, `…: 뒤로 가기는 확인과 같고 오늘 하루 안 보기를 기억하지 않는다` |
| D-178 | 닫으면 다음 팝업, 앞 순서가 생기면 바꿈, 떠 있던 공지가 내려가면 바로 닫음 | `AppPopupViewModelTest: 닫으면 다음 팝업을 보인다`, `…: 앞 순서 팝업이 생기면 바꾼다`, `…: 공지가 내려가면 닫는다` |
| 확인 시점 (D-178) | 앱 실행(Activity 생성), 앱으로 돌아올 때(포그라운드 복귀), RC 값 변경, 426 | `AppPopupViewModelTest: 포그라운드 복귀 때 다시 확인한다`, `…: Remote Config가 바뀌면 다시 확인한다`, `…: 426 신호가 오면 강제 팝업을 바로 띄운다` |
| FR-APP-02 AC2·AC3 | [업데이트] = Play 인앱 **즉시(IMMEDIATE)** 업데이트, 못 쓰면 Play 스토어 페이지. 취소하고 돌아오면 팝업 다시. 메뉴의 유연 업데이트(FR-SET-02 AC4, AC8)는 그대로 | `AppPopupViewModelTest: 즉시 업데이트를 쓸 수 없으면 스토어를 연다` |
| D-179 [앱 종료] | 앱 종료, 다음 실행 때 다시 확인 | `AppPopupViewModelTest: 앱 종료를 누르면 종료 효과를 낸다` |
| SCREENS 공통 "앱 전체 팝업" | 탭·시작 화면(로그인 전)·시트·다이얼로그·튜토리얼·요청 중 덮개 위에 뜸 (`MainActivity` 맨 위층) | 실기기 확인 |
| 접근성 | `alertdialog`, 제목·본문 읽기, preset 이미지는 읽지 않음, 이미지만 넣은 팝업은 `imageAlt`가 이름 | Preview + 실기기 TalkBack |
| D-181 | 기본 이미지 4종 VectorDrawable(`design/illustrations/popup-*.svg` 그대로) | `PopupPresetDrawableTest`(키 4개 → 리소스) |
| 문구 | "앱 종료", "업데이트", "확인", "오늘 하루 안 보기", "닫기", "업데이트가 필요해요", "새 버전으로 업데이트해야 계속 쓸 수 있어요." (`strings.xml`) | — |

### 묶음 C — 앞에서 넘어온 일

| ID | 내용 | 테스트 |
| --- | --- | --- |
| 카카오 SDK 초기화 보호 (2026-11-25 사용자 결정) | 카카오 앱 키가 비어 SDK를 초기화하지 않은 빌드에서 카카오 로그인을 눌러도 앱이 죽지 않고 공통 오류로 끝남 | `KakaoTokenSourceTest: SDK를 초기화하지 않았으면 실패를 돌려준다` |

## 읽을 곳

| 묶음 | 문서·파일 | 위치 |
| --- | --- | --- |
| A·B | `../Docs/docs/PRD.md` | 6.8 FR-APP-01~03 (512~539행), D-176~D-183 (838~845행), Q-21·Q-23 (865·867행), D-151 (813행) |
| A | `../Docs/docs/API.md` | 1장 공통 요청 헤더 (21~28행), 1.1 `APP_VERSION_UNSUPPORTED` details (94행), R-20 (124행), 3.1 `PUT /me/devices` (154행), 3.7 전체 (554~647행), 4.9 (784~797행), 5장 426 (829행) |
| B | `../Docs/docs/SCREENS.md` | 41·46행(공통), "운영" 절 (511~614행) |
| B | `../Docs/design/design-system.md` | "공지·업데이트 팝업" (313~340행), 위의 "다이얼로그" 표 |
| B | 보드·이미지 | `design/source/AppNotice.dc.html`, `AppUpdate.dc.html`, `PopupImage.dc.html`, `design/illustrations/popup-*.svg`, `design/screens/SCR-APP-0*.png` (14장) |
| A | 고칠 코드 | `data/remote/.../NetworkModule.kt`(인터셉터), `domain/.../error/ErrorCode.kt`·`DataErrorExt.kt`, `core/designsystem/.../message/`(`toMessageResOrNull`), `data/repository/.../DeviceRegistrarImpl.kt`, `data/remote/.../me/`(`putDevice` DTO), `data/local/.../cleanup/LocalDataCleaner.kt`(`DeviceLevelKeys`), `gradle/libs.versions.toml`(`firebase-config`), `core/push/.../FcmAvailability.kt`(Firebase 있는지 판단 재사용) |
| B | 고칠 코드 | `core/common/.../update/`(`AppUpdateSource`에 즉시 업데이트·스토어 열기), `core/designsystem/.../component/DuriseoOverlayWindow.kt`·`DuriseoDialog.kt`(창 순서·다이얼로그 모양), `app/.../MyApp.kt`(`ForegroundCallbacks`), `feature/main/.../MainActivity.kt`, `settings.gradle.kts`, `docs/MODULES.md` |
| C | 고칠 코드 | `core/common/.../social/KakaoTokenSource.kt`, `SocialSdkInitializer.kt` |

## 서버 연동

| API | 개발 서버 배포 여부 | 확인 |
| --- | --- | --- |
| 공통 요청 헤더, `PUT /me/devices` 새 필드 | 배포됨 (Server 단위 8, PR #18, 2026-11-25 dev 배포 성공) | MockWebServer + 실기기 |
| `GET /app-notices`, `GET /app-update-rules`, `GET /app-notice-images/{fileName}` | 배포됨 | MockWebServer + 실기기(관리자 API로 넣은 값) |
| 426 `APP_VERSION_UNSUPPORTED` (R-20) | 배포됨 | MockWebServer + 실기기(FORCE 규칙을 넣어 확인) |

## 결정

범위 확인 때 사용자가 승인한 것 (2026-11-25, 문서에 없는 구현 방식):

1. **Remote Config 기다리는 시간**: 문서는 서버만 3초다. Remote Config 가져오기도 실행 때 3초까지만 기다리고, 늦으면 마지막으로 받아 둔 값으로 먼저 판단한다. 나중에 도착해 값이 바뀌면 "값이 바뀔 때"처럼 다시 판단한다 (Firebase 기본 시간 초과 60초를 기다리지 않게).
2. **426으로 띄운 강제 팝업**: 앱 프로세스가 살아 있는 동안 유지한다. 다음 확인(돌아올 때 등)에서 서버 목록을 받았는데 그 `ruleId` 규칙이 없고 다른 강제 규칙도 없을 때만 닫는다.
3. **팝업 이미지 요청에는 토큰·공통 헤더를 붙이지 않는다.** `https://` 이미지는 우리 서버가 아닐 수도 있어서(Remote Config) 인증 없는 별도 이미지 로더로 받는다. 공통 헤더 4개는 우리 API 서버 요청(Retrofit 두 클라이언트, 프로필 사진 포함)에만 붙인다.
4. **[앱 종료]는 `Activity.finishAndRemoveTask()`** 로 화면을 모두 닫는다(프로세스를 강제로 죽이지 않음). 다시 열면 Activity 생성 때 확인한다.
5. **Firebase 설정이 없는 빌드**(`google-services.json` 없음, CI)는 Remote Config 없이 서버 값만으로 판단한다.
6. **Play 스토어 페이지**: `market://details?id=com.duriseo.android`, 열 앱이 없으면 `https://play.google.com/store/apps/details?id=com.duriseo.android`.
7. **모듈**: 팝업 화면은 새 모듈 `feature:appops`(`feature:main`이 맨 위층에 둠), 규칙·순서 판단은 `domain`의 `appops` 패키지(순수 Kotlin), Remote Config·서버 읽기는 `data:remote`의 `appops/`, "오늘 하루 안 보기"는 `data:local`의 기기 단위 키.

묶음 A를 구현하며 문서에 없어 구현 담당이 정한 것 (리드 확인 필요):

8. **합친 뒤에 거른다**: 같은 `id`는 Remote Config 값으로 하나로 합친 다음 기간·플랫폼·규칙 맞춤을 거른다. Remote Config 쪽 값이 기간 밖이면 서버에 같은 `id`가 유효해도 안 뜬다(문서의 "같은 id는 Remote Config 값"을 그대로 따름).
9. **"오늘 하루 안 보기"는 버튼이 있는 종류(알리는 공지·권장)에만 적용**한다. 앱을 막는 공지·강제는 기록이 있어도 숨기지 않는다. 저장은 설정 DataStore에 `id`마다 문자열 키 `popup_hidden_notice_<id>`·`popup_hidden_rule_<id>`, 값은 숨긴 날짜(`YYYY-MM-DD`, 기기 시간대). `DeviceLevelKeys.isDeviceLevel(key)`가 이름 접두어로 로그아웃 정리에서 남긴다(`ALL`은 Boolean 고정 키 그대로). 지난·읽을 수 없는 값은 `hiddenIds()`를 부를 때 지운다.
10. **서버 값은 공지와 규칙 두 조회가 모두 성공해야** 쓴다. 하나라도 실패하면 `server = null`(Remote Config 값만).
11. **값 거르기 추가 건너뛰기**: 공지의 `id` 규칙(`[a-z0-9_-]` 1~50자) 위반, `closable` 없음·boolean 아님, `platform`이 `ANDROID`·`IOS`·null이 아님, 일시 형식 틀림, 규칙의 `osVersion`이 객체지만 `min`·`max`가 둘 다 null이거나 `min > max`, `build`가 음수·`RANGE` `min > max`·`IN` 빈 목록은 그 항목을 건너뛴다(문서가 명시한 "필수값 없음·모르는 enum"의 연장).
12. **426 신호**: `ForceUpdateSignalHub`(data:remote, `ForceUpdateSignals`로 노출)가 마지막 신호를 `StateFlow`로 프로세스 동안 들고 있다(늦게 구독해도 받음). 두 OkHttp 모두의 가장 바깥(서버 주소 검사 바로 뒤) 인터셉터가 `peekBody`로 읽는다. `details`가 비면 문구·이미지 없는 신호, 이미지만 넣은 규칙(`title` null)이 `https://` 이미지·`imageAlt`를 못 채우면 문구·이미지를 비워 기본 문구로 보이게 한다. 결정 2의 닫기 판단은 `AppPopupCoordinator`가 한다.
13. **이미지 로더**: 팝업 전용 Coil `ImageLoader`를 `@PopupImageLoader`(`core:common`의 한정자)로 제공한다. 인터셉터 없는 별도 OkHttp(`https`→`http` 이동 안 따름, 10초 제한). 미리 받기는 이 로더의 캐시를 채우므로 화면도 같은 로더를 써야 한다.
14. **`OsVersion(apiLevel)`** (`core:common` `config/`)을 새로 두고 app이 `Build.VERSION.SDK_INT`로 제공한다. 공통 헤더와 기기 등록이 `AppVersion`과 함께 쓴다. `ErrorCode` 개수 문서는 27개로 고쳤다.

15. **묶음 B 구현 판단 (구현 담당이 정함, 리드 확인 필요)**:
    - 팝업 틀은 `core:designsystem`의 `DuriseoPopupDialog`(feature는 `OverlayRulesTest` 때문에 scrim·창 API를 못 쓴다). 항상 맨 위는 `DuriseoOverlayWindow(alwaysOnTop = true)`: 보통 창이 새로 열릴 때마다 `DuriseoOverlayWindows.generation`이 올라가 팝업 창을 다시 만든다(나중에 만든 창이 위). 팝업이 있으면 보통 창의 내용은 접근성에서 뺀다. 팝업 창은 스낵바를 그리지 않는다.
    - `AppPopupCoordinator`에 `dismissForNow(popup)`(이번에만 닫음, 다음 `check`에서 초기화)·`next()`(마지막으로 읽은 값으로 다시 고름, 서버 안 부름)를 더했다. `check(isLaunch)`가 닫은 목록을 비운다.
    - "돌아옴" 신호는 `core:common`의 `AppForegroundTracker`(`AppForegroundEvents`): 백그라운드(700ms 지연 판정)를 거친 뒤의 첫 포그라운드만 알린다. 회전은 알리지 않는다. `MyApp`의 `ForegroundCallbacks`가 이 트래커를 부른다. 실행 확인은 `AppPopupViewModel` 생성 때(Activity 생성, 회전에는 ViewModel이 남아 다시 안 함)이고, 실행 확인이 도는 중에 돌아옴 신호가 오면 건너뛴다.
    - 규칙에 `image`가 없으면 파서가 `preset:update`로 채운다(모르는 키는 이미지 없음 그대로). 426 `details`도 같다(이미지 없음·문구 없음 → `preset:update` + 기본 문구, 이미지만 넣은 규칙이 조건을 못 채우면 기본 문구). 그래서 화면은 "이미지 `None` = 이미지 자리 없음"만 안다.
    - `AppUpdateSource.startImmediateUpdate(activity)`가 `ImmediateUpdateResult`(STARTED·CANCELED·UNAVAILABLE)를 돌려준다. UNAVAILABLE(Play 미설치·새 버전 없음·즉시 방식 불가·실패)만 스토어를 연다. 취소는 팝업 그대로.
    - 접근성: 패널 이름(`paneTitle`)은 제목, 이미지만 넣은 팝업은 `imageAlt`. 이미지는 `contentDescription = null`. Compose에 `alertdialog` 역할이 없어 기존 `DuriseoDialog`와 같은 방식(`paneTitle` + 초점)을 썼다. 새 문구 없음.
16. **묶음 C**: `KakaoSdkState`(`core:common`)가 `KakaoSdk.init` 성공 여부를 들고, `KakaoTokenSource`·`DefaultSocialSessionCleaner`가 키 비어 있음 또는 미초기화면 SDK를 부르지 않는다. `KakaoTokenSource.request`는 SDK 예외도 `Failed`로 바꾼다.

17. **링크 버튼으로 나갔다 돌아오는 한 번은 "앱으로 돌아옴" 확인으로 세지 않는다** (리드 결정, SCREENS 560행: 링크를 열었다 돌아와도 팝업은 그대로). `AppPopupViewModel`의 `skipNextReturn`이 [링크] 누름 때 켜지고 돌아옴 신호 한 번을 건너뛰며 꺼진다. 열 앱이 없어 못 나갔으면 `onLinkOpenFailed()`가 거둔다. 그다음 돌아옴부터는 다시 확인한다.
18. **돌아올 때 서버 요청이 실패하면 서버에만 있는 공지를 "내려갔다"고 보지 않는다** (리드 결정). 떠 있던 팝업을 닫는 경우는 Remote Config 값이 바뀌어 항목이 사라졌을 때와 서버 목록을 받았는데 그 항목이 없을 때뿐이다. 서버가 실패한 동안 떠 있는 서버 공지는 그대로 두고, 새로 고르는 것은 Remote Config 값만 쓴다(API 4.9 2단계). 구현은 `AppPopupCoordinator.check(isLaunch, current)`의 `keep`·`lastReceivedServer`.

## 다음 단계용 메모

리뷰 1회차 수정 (Major 6, Minor 9):

- **순서 보장(M1·M4)**: `AppPopupViewModel`이 결과마다 순번을 받고(확인은 시작 때, 변경·426·다음 팝업은 도착 때) 나중 번호를 이미 보였으면 이전 번호의 늦은 결과를 버린다. 426 강제가 떠 있고 신호가 남아 있으면(`coordinator.hasForceSignal`) 강제가 아닌 결과로 바꾸지 않는다. [확인]·[닫기]·[오늘 하루 안 보기]는 먼저 팝업을 내리고(`AppPopupUiState()`) 다음 팝업은 이미지를 받은 뒤 보인다(내려간 뒤라 중복 누름은 무시됨).
- **426 닫기(M2)**: 닫기는 `check()`가 방금 받은 서버 목록으로만 한다. `changes`·`next`는 `lastReceivedServer`로 합치되 강제를 닫지 않는다. `AppOpsRepositoryImpl.changes`는 `server = null`로 낸다(합침은 코디네이터).
- **426 스낵바(M3)**: feature ViewModel 23곳의 `toMessageRes()`를 `toMessageResOrNull()?.let`으로 바꿨다. 되돌아가지 않게 `ErrorMessageRulesTest`(core:designsystem)가 feature·app 소스에서 `.toMessageRes()` 직접 호출을 찾아 실패시킨다(`ProfileEdit`의 사진 오류 변환은 다른 함수). 대표 화면 `MemoListViewModelTest`에 "426이면 공통 오류 스낵바를 띄우지 않는다".
- **창·이미지(M5·M6)**: `DuriseoPopupDialog`는 `windowInsetsPadding(systemBars ∪ displayCutout)` 안쪽에서 여백 24를 둔다. 미리 받기·`AsyncImage`가 `core:common`의 `popupImageRequest(context, url)`(크기 `Size.ORIGINAL`)을 같이 써서 메모리 캐시 키가 맞는다(`PopupImageRequestTest`). `core:common`에 `coil-core` 의존 추가(MODULES.md 3장에 있는 라이브러리).
- **Minor**: 초점 효과를 창 안으로 옮겨 창이 다시 만들어질 때마다 돈다, `AppPopupHost`가 `key(popup.identity)`로 팝업마다 상태를 새로 시작, Remote Config 읽기·해석은 `@IoDispatcher`, `RemoteConfigGateway`는 설정(`setConfigSettingsAsync`)을 기다린 뒤 가져오고 활성화 결과가 true일 때만 변경을 알린다, 112dp는 `PopupPresetImageSize`(core:designsystem), `Surface100` 직접 사용은 `colorScheme.surfaceVariant`로, `DuriseoPopupDialog` Preview, `PopupImageClientTest`(MockWebServer, Authorization·X-* 없음), `MainActivity` import 순서.
- 새 테스트: `AppPopupViewModelTest`(D-178 이미지를 받는 중에 426이 오면 강제 팝업이 남는다, 늦게 끝난 확인이 앞 순서 팝업을 덮지 않는다, 확인을 누르면 지금 팝업이 먼저 내려간다, 결정 17), `AppPopupCoordinatorTest`(결정 2 426은 방금 받은 서버 목록으로만 닫는다, 결정 18).


묶음 B·C (구현 완료, SDK 없는 환경이라 빌드는 CI 확인 필요):

- **feature:appops**(새 모듈): `AppPopupHost`(`MainActivity`가 `MainScreen`을 감쌈)·`AppPopupViewModel`(코디네이터·`AppUpdateSource`·`AppForegroundEvents`·`@PopupImageLoader ImageLoader`)·`AppPopupContract`·`AppPopupScreen`·`AppPopupPreviews`(Preview 15개)·`strings.xml`(범위 표 문구). `settings.gradle.kts`·`feature:main` 의존·`MODULES.md` 1·2장·3장 갱신.
- **core:designsystem**: `DuriseoPopupDialog`, `popupPresetDrawable`, VectorDrawable `popup_{maintenance,error,update,notice}`, `DuriseoOverlayWindow`의 `alwaysOnTop`.
- **core:common**: `AppForegroundTracker`·`AppForegroundEvents`, `AppUpdateSource.startImmediateUpdate`(+`PlayAppUpdateSource` 구현), `KakaoSdkState`.
- **domain**: `AppPopupCoordinator`의 `dismissForNow`·`next`.
- 확인할 것(실기기): 시트·다이얼로그·튜토리얼·요청 중 덮개가 열린 상태에서 팝업이 위에 다시 올라오는지(창 다시 만들기 방식), 로그인 전 화면 위, TalkBack(제목·본문 읽기, 이미지만 넣은 팝업은 imageAlt), 즉시 업데이트·취소 후 팝업 유지, [앱 종료], 회전 때 중복 확인이 없는지, 돌아올 때 확인.
- 주의: 창을 다시 만들 때 팝업이 한 프레임 깜박일 수 있다. 스크롤 위치는 창 밖 상태라 남는다. `OverlayRulesTest`는 그대로 통과해야 한다(feature에 scrim·창 API 없음).

묶음 A (구현 완료, 빌드는 SDK 없는 환경이라 CI 확인 필요):

- **domain `appops/`**: `AppOpsModels.kt`(`AppNotice`·`AppUpdateRule`·`BuildCondition`·`OsCondition`·`PopupImage`(Preset/Web/None)·`PopupLink`·`ForceUpdateSignal`·`AppEnvironment`·`AppOpsData`), `AppOpsRules.kt`(`isActive`·`filterActive`·`AppUpdateRule.matches`), `AppPopup.kt`(`PopupKind` 선언 순서 = 표시 순서, `AppPopup` 화면용 값), `AppPopupResolver.kt`(`merge`·`resolveAll`·`resolve`, `HiddenTodayIds`), `AppOpsRepository.kt`(`AppOpsRepository`·`ForceUpdateSignals`·`PopupHiddenTodayStore`·`PopupImagePrefetcher`·`AppOpsSnapshot`), `AppPopupCoordinator.kt`.
- **화면(묶음 B)이 쓸 것은 `AppPopupCoordinator`(Hilt `@Singleton`, `data:repository`의 `AppOpsProvidesModule`이 만듦)** 하나다: `check(isLaunch)`(Activity 생성 true, 포그라운드 복귀 false) → `AppPopup?`, `changes: Flow<AppPopup?>`(Remote Config 변경), `forceUpdates: Flow<AppPopup>`(426), `hideToday(popup)`. 이미지는 `https://`를 못 받으면 이미 `PopupImage.None`으로 바뀌어 온다(이미지만 넣은 팝업은 `title == null`이면 `imageAlt`를 제목 자리에). 업데이트 팝업의 이미지가 `None`이면 `preset:update`, 426의 `title`·`body`가 null이면 기본 문구는 화면이 정한다. `AppPopup.kind`: `FORCE_UPDATE`·`BLOCK_NOTICE`·`RECOMMEND_UPDATE`·`INFO_NOTICE`(`canHideToday`).
- 결정 2(426 팝업 유지·닫기)는 코디네이터가 `check`·`changes`에서 판단하고 `ForceUpdateSignals.clear()`를 부른다. 화면은 `forceUpdates`를 구독하면 된다.
- **data:remote**: `network/CommonHeadersInterceptor`·`ForceUpdateInterceptor`·`ForceUpdateSignalHub`, `appops/`(`AppOpsApi`, `AppOpsRemoteDataSource`, `AppOpsJsonParser`(서버·RC 공통), `RemoteConfigGateway`(Firebase 없으면 `NoRemoteConfigGateway`), `AppOpsModule`(`@PopupImageLoader` Coil·`PopupImagePrefetcher`)). `createNoAuthClient`·`createAuthedClient`에 헤더·426 인터셉터 인자 추가(기본 null이라 기존 테스트 그대로).
- **data:local**: `appops/PopupHiddenTodayLocalDataSource`, `DeviceLevelKeys.isDeviceLevel`. **data:repository**: `appops/AppOpsRepositoryImpl`, `di/AppOpsModule.kt`, `DeviceRegistrarImpl`이 `osVersion`·`appVersion`·`appBuild`를 보냄(생성자에 `AppVersion`·`OsVersion` 추가, `RepoHarness` 갱신).
- 주의: `google-services.json`이 없는 빌드는 Remote Config 없이 서버 값만 쓴다(`FirebaseApp.getApps` 검사). Firebase 있는 빌드에서만 실제 Remote Config 경로가 돈다(실기기 확인 필요). `firebase-config` 버전은 BoM 34.4.0이 정한다(CI로 확인). 화면 B는 `@PopupImageLoader ImageLoader`를 Coil `AsyncImage`에 넘겨야 미리 받은 캐시를 쓴다(`feature`가 `coil-compose`를 가져야 하면 MODULES.md 3장에 이미 있는 라이브러리).
- 테스트 이름이 요구 표와 다른 곳: `DataErrorMessageTest`는 `core:designsystem`에 새 파일(`ErrorMessagesTest`와 별개). 모든 테스트 이름 앞에 AC·결정 ID를 붙였다.

## 리뷰 기록

| 회차 | Critical | Major | 비고 |
| --- | --- | --- | --- |
| 1회차 | 0 | 6 | 팝업 결과 순서·426 보호(M1), 426 닫기는 새 서버 목록으로만(M2), 426 공통 스낵바(M3), 다음 팝업 이미지 동안 닫기(M4), 시스템 바 inset(M5), 이미지 요청 캐시 키(M6), Minor 9건 모두 수정 |

## 남은 일

- 사람(맥): Room 변경 없음(DataStore만). Firebase 콘솔에 Remote Config 키 `app_notices`·`app_update_rules` 만들기(값 `{"items":[]}`).
- 사람(실기기): 관리자 API로 공지·규칙을 넣고 앱 실행·복귀·Remote Config 변경 때 뜨는지, 426, Play 설치 앱의 즉시 업데이트, 시트·다이얼로그 위에 뜨는지, TalkBack.
