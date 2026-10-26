# 규칙

## 이름

- 패키지: `com.duriseo.android.<레이어>.<모듈>` (예: `com.duriseo.android.feature.calendar`). 모듈 `namespace`도 같다.
- 화면 파일: `<이름>Route`(ViewModel 연결) + `<이름>Screen`(상태만 받는 화면) + `<이름>ViewModel` + `<이름>Contract`(UiState·Action·SideEffect).
- API·DB 이름은 문서 그대로: JSON은 camelCase, Kotlin DTO 필드도 같은 이름. Room 컬럼은 snake_case.
- 화면 ID는 코드 주석과 테스트 이름에 쓴다 (`// SCR-CAL-01`).

## MVI

```kotlin
data class CalendarUiState(...)              // 화면에 그릴 모든 것. 불변
sealed interface CalendarAction { ... }      // 사용자가 한 일 (클릭, 입력, 당겨서 새로고침)
sealed interface CalendarSideEffect { ... }  // 한 번만 일어나는 일 (이동, 스낵바, 권한 요청)
```

- ViewModel은 `StateFlow<UiState>`와 `Flow<SideEffect>`(Channel)를 내보내고 `onAction(Action)` 하나로 입력을 받는다.
- UiState는 Room Flow를 `combine`/`map`해서 만든다. 서버 응답을 UiState에 직접 넣지 않는다 (`docs/ARCHITECTURE.md` 2장).
- 로딩·빈 화면·오류·오프라인 상태를 UiState에 명시한다. SCREENS.md에 그 상태가 있으면 그대로 그린다.

## Compose

- `Screen`은 상태와 `onAction`만 받는다. ViewModel, NavController를 받지 않는다. 상태별 `@Preview`를 둔다.
- 색·글꼴·간격·모서리는 `DuriseoTheme`와 토큰으로만 쓴다. 숫자 dp·sp·Color를 화면에 직접 쓰지 않는다. 토큰에 없는 값이 필요하면 멈추고 보고한다.
- 사람 색(나·상대·함께)은 `PersonColors`로 쓰고, 색만으로 뜻을 전하지 않는다 (라벨이나 아바타 글자를 함께, FR-CAL-04).
- 터치 영역은 `size-touch` 이상, 아이콘 버튼에는 `contentDescription`을 넣는다.
- 수정·삭제 메뉴는 PRD 7장 권한 매트릭스대로 보이거나 숨긴다.

## 문구

- 모든 문구는 `strings.xml`에 둔다. 해요체 (NFR-COPY-01, D-58).
- 문구는 SCREENS.md에 적힌 그대로 쓴다. 없는 문구는 만들지 않고 멈추고 보고한다.
- 서버 오류 문구는 오류 `code`별로 정한다. 서버 `message`를 화면에 쓰지 않는다.

## 테스트

- AC마다 테스트 하나 이상. 테스트 이름에 ID를 넣는다: `` `FR-AUTH-01 AC1 게스트 시작을 누르면 캘린더로 간다`() ``
- ViewModel: 가짜 리포지토리 + `kotlinx-coroutines-test` + Turbine으로 상태·사이드 이펙트를 확인한다.
- data:remote: MockWebServer로 요청 경로·필드와 오류 변환(API.md 1.1)을 확인한다.
- data:repository: 가짜 remote + 메모리 Room으로 refresh·쓰기 흐름을 확인한다 (오프라인 쓰기는 실행하지 않음 포함).
- 단위 테스트는 `./gradlew testDebugUnitTest`로 돈다. UI 테스트(androidTest)는 CI에서 돌리지 않는다.

## 커밋과 PR

- 커밋 메시지: `feat(calendar): 월 일정 보기 (FR-CAL-01, SCR-CAL-01)`처럼 `type(모듈): 내용 (ID)`. type은 feat, fix, chore, test, docs, refactor.
- PR 하나 = 작업 단위 하나. 본문에 다룬 FR·AC·SCR ID와 테스트 이름 표, 리뷰 결과, 문서와 다르게 한 점(없어야 정상), 화면 캡처나 Preview 목록, 남은 일을 쓴다.
- 커밋 메시지 끝에 세션이 알려 준 저작자 줄을 붙인다.
