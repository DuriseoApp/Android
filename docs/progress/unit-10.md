# 단위 10: 광고·빌드 3종·최근 로그인 수단

> 브랜치: `feat/10-ads` · PR: https://github.com/DuriseoApp/Android/pull/40 · 상태: PR 완료
> 기준: PRD v0.23, SCREENS.md(캔버스 버전 53), API.md(변경 없음) · 이슈 #39

## 범위

| ID | 내용 | 테스트 |
| --- | --- | --- |
| D-186 빌드 | 빌드 타입 `debug`(개발 서버 + Google 테스트 광고), `noAdsDebug`(`debug`와 같고 광고·광고 자리 없음, `initWith(debug)`), `release`(운영 서버 + 실제 광고). applicationId 모두 `com.duriseo.android`. 라이브러리 모듈은 `matchingFallbacks = listOf("debug")` | `AdsConfigTest: noAdsDebug는 광고가 꺼져 있다`(app 단위 테스트, BuildConfig 값으로 만든 `AdsConfig`) |
| D-186 ID | AdMob 앱 ID·광고 단위 ID 3개는 `local.properties`(`duriseo.admob.appId`, `duriseo.admob.memoListNativeId`, `duriseo.admob.anniversaryListNativeId`, `duriseo.admob.menuBottomBannerId`)에서 읽어 **release에만** 쓴다. `debug`·`noAdsDebug`는 Google 공개 테스트 ID(앱 `ca-app-pub-3940256099942544~3347511713`, 네이티브 `ca-app-pub-3940256099942544/2247696110`, 적응형 배너 `ca-app-pub-3940256099942544/9214589741`)를 코드에 둔다(공개 값). 앱 ID는 매니페스트 `com.google.android.gms.ads.APPLICATION_ID` 자리표시자 | `AdsConfigTest: release 값이 비면 광고를 끈다` |
| FR-AD-01 AC1·AC3 | SCR-MEMO-01 일반 목록(고정 영역 제외)에 메모 5개마다 광고 자리(5·10·15번째 뒤). 5개 미만 없음. 최신순·오래된순·즐겨찾기 모두(Q-24 ①) | `MemoAdSlotsTest: FR-AD-01 AC1 5개마다 자리를 넣는다`, `… 4개면 자리가 없다`, `… 10개면 5번째와 10번째 뒤`, `… 즐겨찾기 보기에도 같은 규칙` |
| FR-AD-01 AC2 | 한 줄 카드: "광고" 배지 + 광고주 이름, 제목, 설명 한 줄, 행동 버튼, AdChoices. 개수·정렬에 세지 않음 | `MemoAdSlotsTest: 광고는 개수에 들어가지 않는다`, Preview `MemoListPreview_Ad` |
| FR-AD-02 AC1·AC2 | SCR-ANNIV-01 다가오는 목록 아래·접힌 "지난 기념일" 위 큰 카드(미디어 포함). 다가오는 기념일이 없으면 자리 없음 | `AnniversaryListViewModelTest: FR-AD-02 AC2 다가오는 기념일이 없으면 광고 자리가 없다`, Preview `AnniversaryListPreview_Ad` |
| FR-AD-03 AC1·AC2 | SCR-SET-01 메뉴 목록 화면 아래 적응형 고정 배너(시스템 내비게이션 위), 메뉴는 배너 위에서 스크롤, 앱 정보 카드 안 가려짐. 하위 화면 없음(Q-24 ③) | Preview `SettingsPreview_Banner` |
| FR-AD-04 AC1 | 받지 못함·받는 중이면 자리 없음(빈 틀·로딩 없음), 받은 뒤 나타남, 한 화면에 있는 동안 바꾸지 않음(Q-24 ②), 오류 안내 없음 | `NativeAdSlotStateTest: 받기 전과 실패면 숨긴다`, `… 받은 광고는 화면에 있는 동안 바꾸지 않는다` |
| FR-AD-04 AC2 | "광고" 배지 `warning-50`/`warning-800`, 카드 `surface-50`, AdChoices, 낭독 때 "광고"가 먼저 | Preview 3종 |
| FR-AD-04 AC4 | `noAdsDebug`는 SDK 초기화·광고 요청을 하지 않는다 | `AdsInitializerTest: 광고가 꺼져 있으면 초기화하지 않는다` |
| FR-AUTH-10 AC1, Q-25 | 소셜 로그인·계정 연동·[그 계정으로 로그인] 성공 때 수단을 기기 단위로 기억(바뀌면 덮어씀). 로그아웃·세션 끊김에 남고, **앱에서 탈퇴 성공 때 지움** | `AuthRepositoryImplTest: FR-AUTH-10 AC1 소셜 로그인 성공이면 수단을 기억한다`, `… 연동 성공이면`, `… switch 성공이면`, `… 실패면 바꾸지 않는다`, `… 탈퇴하면 지운다`, `LocalDataCleanerTest: 최근 로그인 수단은 로그아웃 정리에서 남는다` |
| FR-AUTH-10 AC2 | SCR-START-01 기억한 버튼에 "최근 사용" 배지(알약, 위 −9·오른쪽 14, 안쪽 2·8, 11/16 600, `brand-500` + `on-brand`). 버튼 낭독에 ", 최근 사용" | `StartViewModelTest: FR-AUTH-10 AC2 기억한 수단을 상태에 싣는다`, Preview `StartPreview_LastUsedKakao` |
| FR-AUTH-10 AC3·AC4 | 다른 수단을 누르면 확인 다이얼로그(제목 "이 기기에서는 카카오로 로그인했어요"/"Google로", 본문 "다른 계정으로 로그인하면 새 계정이 만들어질 수 있어요. Google로 계속할까요?", [취소]·[Google로 계속], 확인 `brand-500`). 같은 수단·게스트는 바로 진행 | `StartViewModelTest: FR-AUTH-10 AC3 다른 수단은 다이얼로그를 띄운다`, `… 계속을 누르면 그 수단으로 로그인한다`, `… 취소하면 로그인하지 않는다`, `AC4 같은 수단은 바로 로그인한다`, Preview `StartPreview_OtherMethodDialog` |
| D-190 | 운영 팝업 세부 4가지는 단위 9 결정 1·2·17·18과 같다. 코드 변경 없음, 문서 대조만 | (리뷰어 대조) |

## 읽을 곳

| 묶음 | 문서·파일 | 위치 |
| --- | --- | --- |
| 공통 | `../Docs/docs/PRD.md` | FR-AUTH-10(259행 근처), 6.9 FR-AD-01~04, NFR-PRIV-04, D-184~D-190, Q-24·Q-25 (`grep -n "FR-AD-\|FR-AUTH-10\|D-18[4-9]\|D-190\|Q-2[45]"`) |
| 공통 | `../Docs/docs/SCREENS.md` | 50행(공통 "광고") |
| A 빌드·광고 기반 | 고칠 코드 | `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/duriseo/android/di/AppModule.kt`, `MyApp`, `core/common/.../config/AppConfig.kt`(옆에 `AdsConfig`), `gradle/libs.versions.toml`, `settings.gradle.kts`, `build-logic`(라이브러리 모듈 `noAdsDebug` fallback) |
| A | 문서 | `docs/ARCHITECTURE.md` 6장(빌드 타입 표), `docs/SETUP.md` 2장(새 키 4개), `docs/MODULES.md`(새 모듈 `core:ads`, 라이브러리 계획), `app/CLAUDE.md`(빌드 타입 줄) |
| B 광고 화면 | `../Docs/docs/SCREENS.md` | 203행(SCR-ANNIV-01 광고), 244~247행(SCR-MEMO-01 광고), 432행(SCR-SET-01 배너) |
| B | 보드·이미지 | `../Docs/design/source/AdDemoMemo.dc.html`, `AdDemoAnniversaries.dc.html`, `AdDemoMenu.dc.html` · `../Docs/design/screens/SCR-MEMO-01--ad.png`, `SCR-ANNIV-01--ad.png`, `SCR-SET-01--ad.png` |
| B | 고칠 코드 | `feature/memo/.../list/MemoList{Screen,Contract,ViewModel}.kt`, `feature/anniversary/.../list/AnniversaryList{Screen,Contract,ViewModel}.kt`, `feature/settings/.../menu/Settings{Screen,Contract,ViewModel}.kt` |
| C 최근 로그인 | `../Docs/docs/SCREENS.md` | 55~82행(SCR-START-01, 71·72행 배지·다이얼로그) |
| C | 보드·이미지 | `../Docs/design/source/Main.dc.html`(`lastUsed`·`otherDialog`), `../Docs/design/screens/SCR-START-01--last-used.png`, `SCR-START-01--other-method-dialog.png` · `../Docs/design/design-system.md` "다이얼로그"(283행~) |
| C | 고칠 코드 | `domain/.../auth/AuthRepository.kt`, `data/repository/.../auth/AuthRepositoryImpl.kt`, `data/local/.../cleanup/LocalDataCleaner.kt`(`DeviceLevelKeys`), 새 `data/local/.../auth/LastSignInLocalDataSource.kt`(튜토리얼 저장과 같은 방식), `feature/auth/.../start/Start{Screen,Contract,ViewModel}.kt`, `feature/auth/src/test/.../Fakes.kt` |

## 서버 연동

| API | 개발 서버 배포 여부 | 확인 |
| --- | --- | --- |
| 없음 | — | 광고·최근 로그인 수단 모두 서버 변경 없음 (API.md 9장 추적표 "API 변경 없음") |

## 결정

범위 확인 때 사용자에게 승인받을 것 (문서에 없는 구현 방식):

1. **SDK**: Google Mobile Ads SDK(`com.google.android.gms:play-services-ads`, 25.x)를 쓴다. 새 Next-Gen SDK(1.x)가 아니라 자료가 많은 기존 SDK. minSdk 26이라 요구(23)를 만족한다.
2. **모듈**: 새 라이브러리 모듈 `core:ads`에 SDK 의존, 초기화, 광고 카드·배너 Compose 부품(`AndroidView`로 `NativeAdView`·`AdView`를 감쌈)을 둔다. feature(memo·anniversary·settings)는 `core:ads`만 안다. domain·data는 광고를 모른다.
3. **설정 전달**: app이 `BuildConfig`로 `AdsConfig(enabled, memoListNativeId, anniversaryListNativeId, menuBottomBannerId)`를 Hilt로 넘긴다(`AppConfig`와 같은 방식). `noAdsDebug`는 `enabled = false`.
4. **release인데 ID가 비면** 광고를 끄고(`enabled = false`) 빌드는 된다. 매니페스트 앱 ID는 비면 SDK가 앱 시작 때 죽으므로, 비어 있을 때만 테스트 앱 ID를 넣고 광고는 끈다. → 출시 전 `local.properties`에 값이 있는지 사람이 확인(SETUP.md).
5. **초기화**: `enabled`일 때만 `MobileAds.initialize`를 앱 시작 때 백그라운드 스레드에서 한 번. 광고 요청은 초기화를 기다리지 않아도 된다(SDK 규칙).
6. **메모 목록 광고 여러 개**: 자리마다 광고 하나를 따로 받는다(같은 광고를 두 자리에 쓰지 않음). 화면(ViewModel 범위)에 받은 광고를 자리 번호별로 들고 있다가 화면을 떠나면 `destroy()`. 받지 못한 자리는 표시하지 않는다.
7. **최근 로그인 수단 저장**: 설정 DataStore의 기기 단위 문자열 키 `last_sign_in_provider`(`GOOGLE`·`KAKAO`). `DeviceLevelKeys`에 넣어 로그아웃 정리에서 남기고, 탈퇴 성공 때만 지운다. Kakao 연결 끊기(D-87)·다른 기기 로그인(D-51)으로 끝난 세션은 지우지 않는다(Q-25).

8. **광고 카드 보드 값**은 `core:ads`의 `AdDimens`에 둔다(아이콘 48·32, 버튼 34, 미디어 180, 패딩 14 등. 화면 feature 코드에는 두지 않는다). 배지 모서리는 design-system 원칙 "배지 6"에 따라 토큰 `Xs`(6)와 같다.
9. **광고 카드 글꼴**: 카드는 `NativeAdView`에 등록한 Android 뷰라서 광고 내용은 시스템 글꼴(굵기만 맞춤)이다. 받아들인다.
10. **광고 객체 위치**: ViewModel은 SDK 객체(`NativeAd`)를 들지 않는다(feature 테스트가 SDK 없이 돌고, 화면을 떠날 때 destroy를 Compose 수명에 맞추려고). ViewModel은 `AdsConfig`만 받아 UiState에 단위 ID(`adUnitId: String?`, 광고가 꺼져 있거나 ID가 비면 null)를 싣고, 화면(Route)이 `rememberNativeAdSlots(adUnitId)`로 `NativeAdSlots`를 들고 `DisposableEffect`로 화면을 떠날 때 `clear()`(destroy)한다. `adUnitId == null`이면 요청도 자리도 없다. 화면 회전 같은 구성 변경에서는 보관함이 다시 만들어져 광고를 새로 받는다(받아 둔 광고를 한 화면 안에서 바꾸지 않는다는 규칙은 같은 컴포지션 안에서 지킨다).

## 다음 단계용 메모

묶음 A(빌드 3종·광고 기반) 끝. SDK가 없는 환경이라 컴파일은 CI로 확인해야 한다 (SDK API 이름은 Google 표준 예제 형태).

- 만든 것: `core/ads/`(새 모듈), `core/common/.../config/AdsConfig.kt`, `app/src/noAdsDebug/AndroidManifest.xml`(광고 자동 시작 Provider 제거), `app/src/test/.../di/AdsConfigTest.kt`. 고친 것: `app/build.gradle.kts`(noAdsDebug·BuildConfig `ADS_ENABLED`·`ADMOB_MEMO_LIST_NATIVE_ID`·`ADMOB_ANNIVERSARY_LIST_NATIVE_ID`·`ADMOB_MENU_BOTTOM_BANNER_ID`·매니페스트 자리표시자 `admobAppId`), `AndroidManifest.xml`, `AppModule`(`AdsConfig` 제공), `MyApp`(`adsInitializer.start()`), `libs.versions.toml`, `settings.gradle.kts`, 문서 4곳.
- 묶음 B가 쓸 API (모두 `com.duriseo.android.core.ads`, feature는 `projects.core.ads`와 `projects.core.common`을 `implementation`에 추가):
  - `NativeAdSlots<T>`: `request(slot: Int)`(자리마다 한 번만 받음), `ads: StateFlow<Map<Int, T>>`(받은 것만, 받는 중·실패는 없음 = 자리를 그리지 않음), `clear()`(destroy). 메모 목록은 자리 번호 = 5·10·15…번째 메모 뒤 순번, 기념일은 0 하나. 광고가 꺼져 있으면 `enabled`가 false이니 자리 계산을 건너뛴다.
  - `NativeAdCard(ad: NativeAd, style: NativeAdCardStyle.Compact | Large, modifier)`: 폭을 채운다. `AnchoredBannerAd(adUnitId, modifier)`: 폭을 채우고 받기 전·실패면 높이 0. 시스템 내비게이션 위·메뉴 스크롤 영역 배치는 화면이 한다.
  - Preview 전용: `NativeAdCardPreviewSample(style)`, `AnchoredBannerAdPreviewSample()`.
- 주의: 카드는 Compose가 아니라 Android 뷰(`NativeAdView`에 자산 등록)로 그려서 IBM Plex 글꼴이 아니라 시스템 글꼴(굵기만 맞춤)이다. 보드에만 있고 토큰에 없는 dp(48·34·180·14 등)는 `core/ads/.../AdDimens.kt` 한 곳에 모았다. `core:ads`는 SDK를 `api`로 열어 feature가 `NativeAd` 타입을 쓴다.
- 확인 필요(CI): `noAdsDebug` 컴파일은 `assembleNoAdsDebug`로만 확인된다 (현재 CI는 `assembleDebug testDebugUnitTest`).
- **묶음 C (최근 로그인 수단)**: domain `LastSignInRepository`(`lastProvider: Flow<SocialProvider?>`, `AuthRepositoryImpl`이 같이 구현, `RepositoryModule`에 바인딩). `data:local/auth/LastSignInLocalDataSource`(키 `DeviceLevelKeys.LAST_SIGN_IN_PROVIDER`, `isDeviceLevel`이 이름으로 가림). `AuthRepositoryImpl` 생성자에 `lastSignIn` 추가(signIn·link·switch 성공 때 저장, withdraw 성공 때 정리 뒤 지움, 로그아웃·세션 끊김은 그대로). `StartViewModel`은 `LastSignInRepository`를 더 받고, 상태에 `lastProvider`·`otherMethodDialog`, 액션 `OtherMethodConfirmed`·`OtherMethodDismissed`. `StartScreen`은 `DuriseoLoadingLayer`를 `DuriseoDialogLayer(loading=)`로 바꿨고 `SocialButton`(배지는 버튼 밖 형제로 겹쳐 잘리지 않음). 테스트: `AuthRepositoryImplTest`(새 파일, 로그아웃은 남는다 하나 더), `data/local/.../cleanup/LocalDataCleanerTest`. SDK가 없어 컴파일은 CI로 확인할 것.

### 묶음 B (광고 화면 세 곳)

- 새 API(`core:ads`): `rememberNativeAdSlots(adUnitId: String?)`, `NativeAdSlotItem(slots, slot, style, modifier)`(요청+받은 뒤에만 그림). `AdDimens.BadgeRadius` 6.
- memo: `list/MemoAdSlots.kt`(`MemoListRow.Memo|Ad`, `memoRowsWithAds`, `MEMO_AD_INTERVAL=5`), `MemoListUiState.adUnitId`·`rows`, `MemoListScreen(adSlot = …)`, Preview `MemoListPreview_Ad`. 테스트 `MemoAdSlotsTest`.
- anniversary: `AnniversaryListUiState.adUnitId`·`showAd`(광고 켜짐 + 다가오는 기념일 있음), "past-row" 위 `item("ad")`, `AnniversaryListScreen(adCard = …)`, Preview `AnniversaryListPreview_Ad`. 테스트는 `AnniversaryListViewModelTest`에 FR-AD-02 AC1·AC2, FR-AD-04 AC4.
- settings: `SettingsUiState.adUnitId`, `SettingsScreen(banner = …)`(메뉴 스크롤 영역 아래, `padding`의 내비게이션 영역 위), Preview `SettingsPreview_Banner`. 테스트는 `SettingsViewModelTest`에 FR-AD-03 AC1, FR-AD-04 AC4.
- 세 ViewModel 생성자에 `AdsConfig`가 마지막 인자로 추가됐다(테스트는 `AdsConfig.Disabled`). 각 feature `build.gradle.kts`에 `projects.core.ads`.
- 주의: 컴파일은 CI로 확인. 메모 광고 줄에는 보드의 아래 구분선을 넣지 않았다(카드 위아래 여백 12만).

## 리뷰 기록

| 회차 | Critical | Major | 비고 |
| --- | --- | --- | --- |
| 1회차 | 0 | 1 | CI가 `noAdsDebug` 갈래 테스트를 안 돌림 → ci.yml에 `assembleNoAdsDebug`·`testNoAdsDebugUnitTest` 추가. Minor 6건(Logger, 안 쓰는 factory 삭제, Compact 버튼 터치 48, 배너 pause/resume, 배지 값 주석, SETUP 한 줄) 모두 수정 |
| 1회차 수정 확인 | 0 | 0 | 리드가 diff로 확인(재리뷰 없음). 묶음 B 첫 CI 실패는 Maven Central 429(일시)라 재실행 |

## 남은 일

- 사람(맥): `local.properties`에 AdMob 키 4개 넣기, `noAdsDebug` 빌드 변형으로 실행해 광고가 없는지, `debug`에서 테스트 광고 3곳.
- 사람(출시 전): 개인정보처리방침·Play 데이터 보안 양식(광고 ID), `app-ads.txt`, AdMob 앱을 Play 스토어와 연결.
