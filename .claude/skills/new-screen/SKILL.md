---
name: new-screen
description: SCREENS.md의 화면 하나(SCR ID)를 Compose 화면 + MVI ViewModel로 만든다. "SCR-CAL-02 만들어줘", "화면 추가" 같은 요청이나 /dev-unit 중 화면을 만들 때 쓴다.
---

# 화면 추가

화면 하나를 만든다. 모듈이 없으면 먼저 `/new-feature-module`을 한다.

## 1. 기준 읽기

1. `../Docs/docs/SCREENS.md`에서 그 SCR 항목 전체: 구성, 상태, 문구, 동작, 근거 ID, `디자인:` 표시.
   - `디자인: 변경 요청됨`이고 이미지가 없으면 **만들지 않고 멈춘다.**
   - `차이:`가 있으면 PRD를 따른다.
2. 이미지: `../Docs/design/screens/<SCR>.png`와 상태별 `<SCR>--<상태>.png`. 레이아웃 확인용이다.
3. 정확한 크기·간격·색: `../Docs/design/canvas-map.md`에서 보드를 찾아 `../Docs/design/source/`의 원본을 읽는다.
4. 근거 FR·AC(PRD), 쓰는 API(API.md), 권한(PRD 7장 권한 매트릭스).
5. 어느 모듈인지는 `docs/MODULES.md` 2장.

## 2. 파일

`feature/<모듈>/src/main/java/com/duriseo/android/feature/<모듈>/<화면>/`

| 파일 | 내용 |
| --- | --- |
| `<이름>Contract.kt` | `UiState`(로딩·빈 화면·오류·오프라인 포함), `Action`, `SideEffect` |
| `<이름>ViewModel.kt` | `@HiltViewModel`. Room Flow를 구독해 UiState를 만들고, 들어올 때 refresh. `onAction` 하나로 입력 처리 |
| `<이름>Screen.kt` | `<이름>Route`(ViewModel·SideEffect 연결)와 `<이름>Screen`(상태·`onAction`만). 상태별 `@Preview` |
| `navigation/<이름>Navigation.kt` | `@Serializable` typed route, `NavGraphBuilder.<이름>Screen(...)`, `NavController.navigateTo<이름>()` |

화면이 작으면 Contract를 ViewModel 파일에 함께 둬도 된다.

## 3. 규칙

- 색·글꼴·간격·모서리는 `DuriseoTheme`와 토큰만 쓴다. 원본의 값이 토큰에 없으면 멈추고 보고한다 (토큰 추가는 `/design-sync` → `/sync-tokens`).
- 공용 모양(버튼, 다이얼로그, 앱바, 배너, 아바타)은 `core:designsystem` 컴포넌트를 쓴다. 없으면 거기에 만든다 (design-system.md "컴포넌트").
- 문구는 SCREENS.md 그대로 `strings.xml`에. 자리표시(`[상대 이름]`, `D+189`)는 실제 값으로.
- 사람 색은 `PersonColors`. 색만으로 뜻을 전하지 않는다.
- 쓰기 동작(저장·삭제·고정)은 오프라인이면 실행하지 않고 "인터넷 연결이 필요해요"를 보여 준다 (NFR-OFF-02).
- 수정·삭제 메뉴는 권한 매트릭스대로만 보인다.
- 날짜 선택은 1950-01-01 ~ 2050-12-31 (처음 만난 날은 오늘까지).
- 접근성: 터치 영역 `size-touch` 이상, 아이콘 버튼 `contentDescription`, 글자 확대 시 잘리지 않게.

## 4. 연결

1. 화면을 그 모듈의 내비게이션 그래프에 넣고, 다른 화면에서 오는 길은 `feature:main`의 `MainNavHost`에서 연결한다.
2. 푸시·알림 목록에서 들어오는 화면이면(SCREENS.md "푸시 진입") 그 route로 갈 수 있게 인자를 정한다.

## 5. 테스트

- ViewModel: AC마다 상태·SideEffect 테스트 (가짜 리포지토리, Turbine). 오프라인 쓰기 거부, 서버 오류 `code`별 문구 선택을 포함한다.
- Preview: SCREENS.md에 있는 상태마다 하나씩. 이름에 상태를 넣는다 (`CalendarScreenEmptyPreview`).
- `./gradlew :feature:<모듈>:testDebugUnitTest assembleDebug`

## 6. 확인 목록

- [ ] SCREENS.md의 구성·상태·문구가 모두 있다
- [ ] 하드코딩한 색·dp·sp·문구가 없다
- [ ] 로딩·빈 화면·오류·오프라인을 그렸다
- [ ] 권한에 따라 메뉴가 보이거나 숨는다
- [ ] 테스트 이름에 FR·AC 또는 SCR ID가 있다
