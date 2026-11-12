# 단위 6: 메모 — SCR-MEMO-01~04, 날짜, 고정, 즐겨찾기, 캘린더 메모 표시

> 브랜치: `feat/6-memo` · PR: #17 · 상태: 진행 중 (2026-11-12 범위 승인, 확인 필요 1~4 추천안대로)
> 기준: PRD v0.12 (FR-MEMO-01·02·03·05·06, FR-CAL-01 AC2·AC8, FR-NOTI-04, NFR-TIME-02, D-57·D-115·D-118·D-119·D-121·D-123·D-126·D-129~D-133), SCREENS.md(캔버스 버전 43) SCR-MEMO-01~04·SCR-CAL-01·SCR-CAL-03·공통 "날짜·시각 표기", API.md 1.1·3.4·3.3 "커서 정렬 키"·5장
> Docs 기준 커밋: DuriseoApp/Docs main `cee0885`
> 앞 단위: 단위 5 머지됨 (main `057ed33`). 서버 메모 API(서버 단위 5)는 개발 서버 배포됨

## 요약

| 묶음 | 내용 |
| --- | --- |
| A. 데이터 | `domain/memo`, `MemoApi`(API.md 3.4의 11개 경로), Room(보기별 목록·고정 목록·날짜 메모 범위·상세, 개수), `MemoRepository`. Room 버전 5. 단위 5의 캐시 세대(`LocalCacheGeneration`)·`runOnlineWrite`·`refreshRange` 방식 그대로 |
| B. 화면 | 새 모듈 `feature:memo`: SCR-MEMO-01(하단 탭 자리표시 교체), SCR-MEMO-02, SCR-MEMO-03, SCR-MEMO-04. 로딩 덮개(D-115: 저장·삭제·고정·즐겨찾기), D-121 오류, 스낵바(D-126) |
| C. 캘린더 | SCR-CAL-01 메모 카드(맨 아래, "메모" 표시, → SCR-MEMO-03)·메모 점(작성자 색, 종일 → 시간 → 메모, D-118), 월 조회에 `GET /memos/dated`(앞뒤 하루, Q-19 ②). SCR-CAL-03 [일정 \| 메모] 종류 탭(D-133) |
| D. 공통 | NFR-TIME-02 작성일 표기(D-129) 공용 함수, 다른 해 메모 날짜 칩(이슈 #15 단위 6분) |

## 확인 필요

| # | 빈 곳 | 추천안 | 근거 |
| --- | --- | --- | --- |
| 1 | 핀 카드의 "시각"이 작성 시각인지 고정 시각인지 (SCREENS "작성자 아바타·작성자 닉네임·시각") | **작성 시각**(`createdAt`)을 D-129 표기로. 메모 행·상세와 같은 규칙. 정렬만 고정 시각(`pinnedAt`)순 | SCREENS 목록 항목 "작성자, 작성일", D-129, 그림(`SCR-MEMO-01.png` "10월 8일") |
| 2 | 메모가 하나도 없을 때(최신순·오래된순) 빈 상태 문구가 없음 | 새 문구 없이 "전체 메모 0개"와 빈 목록. 고정 없음 점선 상자는 SCREENS대로 | SCREENS는 고정 없음·즐겨찾기 없음만 정함 |
| 3 | SCR-CAL-03 [메모] 탭은 "같은 화면"인데 SCR-MEMO-04는 `feature:memo`, SCR-CAL-03은 `feature:calendar` (feature끼리 의존 금지) | 메모 입력 부분을 `feature:memo`의 Composable·ViewModel로 두고, `feature:main`의 `MainNavHost`가 작성 화면에 슬롯(람다)으로 넣어요. 새로 쓸 때만 탭, 탭마다 입력 보존, 저장은 현재 탭(D-133). 수정은 탭 없이 `feature:memo` 화면 | ARCHITECTURE 의존 규칙, MODULES 화면 → 모듈 표 |
| 4 | 메모 동기화 신호(SYNC `MEMO`) 수신과 푸시·알림 목록에서 메모 상세로 들어오는 길 | 단위 7(동기화 신호 수신·알림)에서. 이 단위는 화면 진입·당겨서 새로고침·쓰기 성공 뒤 다시 불러오기 | PLAN 단위 7 |

## 범위

테스트 이름에 ID를 넣는다. Preview 이름은 `<화면>Preview_<상태>`.

### A. 데이터 (API.md 3.4)

| ID | 내용 | 테스트 |
| --- | --- | --- |
| API `GET /memos?view=&cursor=&size=` | 보기별(`newest`·`oldest`·`favorites`) 첫 페이지 교체·다음 페이지 이어 붙이기, `totalCount`·`favoriteCount` 저장. 오프라인이면 저장된 보기 그대로 | `MemoRepositoryTest: GET memos는 보기별로 첫 페이지를 교체하고 다음 페이지를 이어 붙인다` · `…: totalCount와 favoriteCount를 저장한다` |
| API `GET /memos/pinned` | 고정 전체, 서버 순서(최근 고정순) 위치 저장 | `MemoRepositoryTest: pinned는 응답 순서대로 저장한다` |
| API `GET /memos/dated?from=&to=` | 캘린더 범위, 범위 교체, 앞뒤 하루(Q-19 ②) | `MemoRepositoryTest: dated는 범위를 교체하고 이웃 달을 지우지 않는다` |
| API `GET /memos/{id}` | 상세, 404면 기기에서 지우기 | `MemoRepositoryTest: 상세가 404면 기기에서 지운다` |
| API `POST`·`PATCH`·`DELETE /memos` | 온라인 쓰기, `PATCH`는 바꾼 필드만(같은 값이면 요청 없이 닫기), 성공 뒤 목록·고정·보이는 달 다시 불러오기. `notifyPartner`는 `POST`에만 | `MemoRepositoryTest: PATCH는 바꾼 필드만 보낸다` · `…: 쓰기 성공 뒤 목록 고정 달을 다시 불러온다` |
| API `PUT`·`DELETE /memos/{id}/pin`, `/favorite` | 성공 뒤 목록·고정·개수 다시 불러오기(멱등) | `MemoRepositoryTest: 고정과 즐겨찾기 뒤 목록과 개수를 다시 불러온다` |
| 캐시 세대 | 해제·로그아웃·공간 변경 뒤 늦은 응답 버리기(단위 5 방식) | `MemoRepositoryTest: 캐시를 지운 뒤 도착한 이전 응답은 저장하지 않는다` |

### B. 화면 (feature:memo)

| ID | 내용 | 테스트 · Preview |
| --- | --- | --- |
| FR-MEMO-01 AC1·AC2 (D-130) | 보기 칩 최신순·오래된순·★ 즐겨찾기 N, 고정 메모는 보기에 없음, 스크롤 끝에서 다음 페이지 | `MemoListViewModelTest: FR-MEMO-01 AC2 보기를 바꾸면 그 보기의 첫 페이지를 불러온다` · Preview `MemoListPreview_Default`, `_Favorites` |
| FR-MEMO-01 AC3 (D-57, D-131) | 항목: 제목(없으면 본문 첫 줄 50자·49+"…"), 미리보기, 사람 색 점·닉네임·작성일(D-129), 메모 날짜 | `MemoTextsTest: D-131 제목이 없으면 본문 첫 비어 있지 않은 줄을 쓰고 50자를 넘으면 49자와 말줄임표다` |
| SCR-MEMO-01 | 고정 3개 이하 모두, 4개 이상 최근 3개 + [고정 메모 전체 보기 (N) ›], 고정 없음 점선 상자, 즐겨찾기 없음 문구, 메모 없음 `<확인 필요 2>`, 핀 카드 시각 `<확인 필요 1>`, 스낵바 바닥 96·FAB 161 | Preview `MemoListPreview_PinsMore`, `_NoPins`, `_FavoritesEmpty`, `_Empty` |
| SCR-MEMO-02 | "고정된 메모 N", 안내, 최근 고정순, 비었을 때 문구, 핀 누르면 해제, 스낵바 위 40 | `PinnedMemosViewModelTest: FR-MEMO-05 핀을 누르면 고정을 푼다` · Preview `PinnedMemosPreview_Default`, `_Empty` |
| FR-MEMO-05 AC1·AC5 (D-134) | 누구든 고정·해제, 개수 제한 없음 | `MemoListViewModelTest: FR-MEMO-05 AC1 상대 메모도 고정할 수 있다` |
| FR-MEMO-06 AC1·AC2 | 즐겨찾기는 나에게만, 멱등 | `MemoListViewModelTest: FR-MEMO-06 별을 누르면 즐겨찾기를 바꾸고 개수를 다시 읽는다` |
| SCR-MEMO-03 (FR-MEMO-03) | 제목·본문·메모 날짜 칩(다른 해면 연도, D-129)·작성자·작성일·핀·별. 내 메모만 수정·삭제(삭제 확인), 상대 메모는 고정·즐겨찾기만 | `MemoDetailViewModelTest: FR-MEMO-03 AC2 상대 메모에는 수정 삭제가 없다` · Preview `MemoDetailPreview_Mine`, `_Partner`, `_OtherYear`, `_DeleteDialog` |
| FR-MEMO-03 AC3 (D-121) | 404: 기기에서 지우고 닫고 공통 오류. 403: 공통 오류 + 다시 불러오기 | `MemoDetailViewModelTest: D-121 404면 기기에서 지우고 닫는다` · `…: D-121 403이면 공통 오류를 보이고 다시 불러온다` |
| SCR-MEMO-04 (FR-MEMO-02) | 제목(50)·본문(5,000) 입력 제한(오류 문구 없음), 날짜 [날짜 추가] → 날짜 행·[날짜 빼기], 1950~2050, 둘 다 비면 [저장] 비활성(D-132), 커플 새 메모만 "상대에게 알림 보내기"(기본 켜짐) | `MemoEditViewModelTest: FR-MEMO-02 AC2 제목과 본문이 공백뿐이면 저장할 수 없다` · `…: FR-NOTI-04 AC3 개인 공간과 수정에서는 알림 스위치가 없다` · Preview `MemoEditPreview_Default`, `_Date`, `_Edit` |
| FR-MEMO-03 AC4 (D-123) | 바꾼 것이 없으면 요청 없이 닫기 | `MemoEditViewModelTest: FR-MEMO-03 AC4 바꾼 것이 없으면 요청 없이 닫는다` |
| SCR-MEMO-04 D-121 | 404: 수정 화면과 상세를 함께 닫고 공통 오류. 403: 수정 화면만 닫고 상세 다시 불러오기 | `MemoEditViewModelTest: D-121 수정이 404면 수정과 상세를 함께 닫는다` |
| D-115 | 저장·삭제·고정·즐겨찾기 중 로딩 덮개 | 화면별 `…: D-115 요청 중에는 loading이 true이고 끝나면 false다` |
| NFR-OFF-02 | 오프라인 쓰기는 요청 없이 "인터넷 연결이 필요해요" | `MemoEditViewModelTest: 오프라인이면 요청하지 않는다` |
| D-126 | SCR-MEMO-01 바닥 96·FAB 161, SCR-MEMO-02~04 위 40 | `SnackbarPositionTest: D-126 메모 탭은 바닥 96이고 메모 하위 화면은 위 40이다` |

### C. 캘린더

| ID | 내용 | 테스트 · Preview |
| --- | --- | --- |
| FR-CAL-01 AC2 (D-119) | 날짜 목록 맨 아래 메모(작성 최신순, 같으면 `id`), "메모" 표시, 제목 규칙(D-131), 누르면 SCR-MEMO-03 | `CalendarViewModelTest: FR-CAL-01 AC2 메모는 날짜 목록 맨 아래 작성 최신순이다` · Preview `CalendarPreview_Memo` |
| FR-CAL-01 AC8 (D-118) | 점: 서로 다른 사람 색만, 종일 → 시간 → 메모(작성자 색), 고리 있으면 점 2개 | `CalendarDayMarksTest: FR-CAL-01 AC8 메모 점은 작성자 색이고 시간 일정 뒤다` |
| Q-19 ② | 월 조회에 `GET /memos/dated` 앞뒤 하루, 당겨서 새로고침·달 넘김·쓰기 뒤 함께 | `CalendarViewModelTest: Q-19 월 조회는 메모도 앞뒤 하루를 더해 요청한다` |
| SCR-CAL-03 (D-133) | 새로 쓸 때만 [일정 \| 메모] 탭, 탭마다 입력 보존, 저장은 현재 탭, 메모 날짜는 미리 채우지 않음, 수정은 탭 없음 `<확인 필요 3>` | `EventEditViewModelTest: D-133 수정할 때는 종류 탭이 없다` · `MemoEditViewModelTest: D-133 캘린더에서 열어도 메모 날짜는 비어 있다` · Preview `EventEditPreview_MemoTab` |

### D. 공통

| ID | 내용 | 테스트 |
| --- | --- | --- |
| NFR-TIME-02 (D-129) | 작성일: 오늘 "오늘 오전 9:12", 어제 "어제", 올해 "M월 d일", 다른 해 "yyyy년 M월 d일"(기기 시간대). 메모 날짜 칩: 올해 "M월 d일 (요일)", 다른 해 "yyyy년 M월 d일 (요일)" | `DateTextsTest: NFR-TIME-02 작성일은 오늘 어제 올해 다른 해로 나눈다` · `…: D-129 다른 해 메모 날짜 칩에는 연도가 붙는다` |

## 서버 연동

| API | 개발 서버 배포 여부 | 확인 |
| --- | --- | --- |
| 메모 11개 경로 (서버 단위 5) | 배포됨 | 실기기 (맥) |

## 범위 밖

- SYNC `MEMO` 수신, 푸시·알림 목록에서 메모 상세로 → 단위 7 `<확인 필요 4>`
- 프로필 사진 아바타 → 단위 8 (지금 있는 글자 아바타를 씀)

## 결정

구현하면서 정한 것 중 문서에 없는 것. 문서에 있어야 할 결정이면 여기 적지 말고 작업을 멈추고 보고한다.

묶음 A·D (데이터·공통 표기):

1. **Room 테이블**: `memo` 한 테이블에 `list_type`(`NEWEST`·`OLDEST`·`FAVORITES`·`PINNED`·`DATED`·`DETAIL`)과 응답 위치(`position`)를 담는다 (단위 5 `anniversary_occurrence`와 같은 방식). 보기별 이어 받기 상태와 `favoriteCount`는 `memo_page_state`(키 `NEWEST`·`OLDEST`·`FAVORITES`, 그리고 `COUNTS`)에 둔다. 상태 행이 있으면 그 보기를 받은 적이 있다는 뜻이고, `MemoList.isLoaded`로 화면에 알린다 (빈 목록이 "메모 없음"인지 "아직 안 받음"인지 구분).
2. **상세 `observeMemo(id)`**: 그 메모를 담은 행 중 가장 나중에 저장한 것(행 `id` 최대)을 쓴다. 목록에서 받은 값과 상세로 받은 값 중 최신이 보인다. 상세 화면은 들어갈 때 `refreshMemo(id)`를 불러 404(D-121)를 알아챈다.
3. **쓰기 뒤 다시 불러오기 범위**: "현재 보기들" = 지금까지 한 번이라도 받은(상태 행이 있는) 보기 전부의 첫 페이지 + `GET /memos/pinned` + `setVisibleRange`로 알린 달. 고정·즐겨찾기는 204라 응답에 메모가 없으므로, 그 메모의 상세를 저장해 둔 적이 있으면 상세도 함께 다시 받는다(API.md 3.4에는 없는 추가 요청 1건). 생성·수정은 응답의 `Memo`를 상세로 저장한다.
4. **삭제 성공(204)**: 서버가 성공했으니 기기에서도 바로 지우고(그 보기의 `totalCount`·`favoriteCount`를 하나씩 줄임) 다시 불러오기로 맞춘다. 다시 불러오기가 실패해도 지운 메모가 남아 보이지 않게 하려는 것이다.
5. **`MemoPatch`**: `FieldChange.Keep`(보내지 않음)과 `FieldChange.Set(value?)`(null이면 JSON `null`로 지움)로 세 필드를 나타낸다. 비교는 `MemoRules.patchOf(원래 Memo, 입력 제목, 입력 본문, 입력 날짜)`가 정리한 값(D-132)끼리 한다. 본문은 공백·줄바꿈까지 비교한다. `NewMemo`는 입력 그대로 받고 보내기 직전에 정리한다.
6. **작성일 표기 분리 (D-129)**: 계산은 `domain/format/DateTexts`(순수 함수, `java.time.Clock`을 받아 그 시간대로 가름)가 `WrittenAt`·`MemoDateChip`을 돌려주고, 문구는 `core:designsystem`의 `format/DateTextFormat`(`Resources.writtenAtText`·`memoDateChipText`와 Composable 같은 이름)이 `strings.xml` 조각으로 붙인다. 요일은 기존 `weekday_short` 배열, 다른 해 연월일은 기존 `date_year_month_day`를 다시 쓴다. 문자열부는 Robolectric이 없는 모듈이라 단위 테스트 없이 Preview로 대조한다.
7. 미리보기(본문 줄 수 자르기)는 규칙이 문서에 없어 화면(묶음 B)이 2줄 말줄임으로 처리한다. 도메인 함수는 두지 않았다.

묶음 B (메모 화면, `feature:memo`):

8. **미리보기 글**: 줄바꿈·연속 공백을 공백 하나로 접은 한 줄이고 200자까지 상태에 담는다(`MemoPreview`). 제목이 없어 본문 첫 줄이 제목 자리에 올라가면 그 줄은 빼고 나머지를 쓴다(같은 글이 두 번 나오지 않게). 핀 카드는 2줄, 메모 행은 1줄 말줄임(결정 7).
9. **메모 입력 부분의 공개 부품 (확인 필요 3)**: `MemoEditViewModel`(Hilt, `uiState`·`effects`·`onAction`)과 Composable `MemoForm`(제목·날짜 영역·본문·알림 스위치, 앱바·저장·스크롤 없음)·`MemoDateSheet`(날짜 시트, 화면 전체 덮개라 바깥이 `DuriseoDialogLayer`의 `dialog`에 둠)를 공개한다. 메모 id는 `SavedStateHandle`의 `memoId` 키(`MemoRouteModel.Edit::memoId`와 같은 이름, `MemoEditViewModel.ARG_MEMO_ID`)에서 읽는다: 키가 없는 항목에서 만들면 새 메모라서 SCR-CAL-03 항목에서 만들어도 메모 날짜는 비어 있다(D-133). 탭마다 입력 보존은 그 ViewModel이 항목에 살아 있는 동안 상태를 들고 있어서 된다.
10. **FAB의 새 메모는 [메모] 탭이 골라진 작성 화면으로 연다 (묶음 C에서 고침)**: SCREENS SCR-MEMO-04 진입에 "SCR-MEMO-01의 FAB"이 있고 이미지는 [일정|메모] 탭이 있는 SCR-CAL-03 모양이다. 처음(묶음 B)에는 `feature:memo`가 일정 폼을 몰라 탭 없는 `MemoRouteModel.Edit()`로 연결했으나, 묶음 C에서 슬롯(결정 16)이 생겨 `CalendarRouteModel.EventEdit(startOnMemo = true)`로 바꿨다(`MainNavigator.navigateToMemoCreate()`). 탭 없는 새 메모 화면 경로는 쓰지 않는다: `navigateToMemoCreate`(feature:memo)를 없애고 `MemoRouteModel.Edit`은 `memoId`가 필수인 수정 전용이다.
11. **새 경로 `MemoRouteModel`**(`core:common`): `Pinned`·`Detail(memoId)`·`Edit(memoId = null)`. 수정이 404면 `NavController.closeMemoEditAndDetail()`이 `Detail`까지 포함해 되돌린다(상세가 메모 탭이든 캘린더든 그 앞으로). 403이면 수정 화면만 닫고 상세는 `ON_START`의 `Shown`과 저장소의 다시 불러오기로 맞춘다.
12. **날짜 시트**: [날짜 추가]·날짜 행은 `StartDatePickerSheet`(범위 1950~2050, 제목 "날짜", 설명·안내 없음, 버튼 "완료")를 연다. 처음 열 때 고른 날은 메모 날짜가 있으면 그 날, 없으면 오늘이고, [완료]를 눌러야 메모 날짜가 붙는다(닫으면 그대로).
13. **화면이 보일 때 다시 불러오기**: 메모 목록(고정 + 지금 보기 첫 페이지)·고정 전체·상세는 `ON_START`에 조회한다(캘린더·기념일과 같다). 당겨서 새로고침은 SCREENS SCR-MEMO-01에 없어 넣지 않았다(PRD Q-16·NFR-SYNC-02는 캘린더 기준이다).
14. **로딩 덮개**: 핀·별(목록·고정 전체·상세), 삭제·저장은 `loading`을 `DuriseoLoadingLayer`/`DuriseoDialogLayer`에 넘긴다. 요청 중에는 다른 쓰기를 받지 않고, 결과 안내는 덮개를 걷은 뒤에 보낸다(D-115).
15. **아이콘**: 핀·별(꺼짐 선, 켜짐 채움)·달력+더하기를 `core:designsystem`에 벡터로 추가했다(`ic_pin`, `ic_pin_filled`, `ic_star`, `ic_star_filled`, `ic_calendar_plus`). 보드 인라인 SVG 그대로다. 켜짐 색은 핀 `brand-500`, 별 `favorite-600`, 꺼짐은 목록 `ink-400`·상세 `ink-600`(보드).

묶음 C (캘린더 메모 표시, SCR-CAL-03 종류 탭):

16. **[메모] 탭 슬롯**: `feature:calendar`의 `EventEditMemoTab`(저장 활성·로딩·날짜 시트 표시·저장 클릭·`form`·`dateSheet` 람다)과 `EventEditMemoTabProvider`(Composable)를 `EventEditRoute`·`calendarNavGraph(memoTab = …)`가 받는다. `feature:main`의 `rememberEventEditMemoTab`이 작성 화면 내비게이션 항목 안에서 `MemoEditViewModel`(hiltViewModel)을 만들어 채운다. 그래서 일정·메모 두 ViewModel이 같은 항목에 살아 탭마다 입력이 남는다. 메모 ViewModel은 새로 쓸 때만 만든다(수정은 슬롯을 부르지 않음). 항목 인자에 `memoId`가 없어 메모 날짜는 비어 있다(D-133). 현재 탭은 `EventEditUiState.kind`(`EditKind`)가 들고, 앱바 [저장]·로딩 덮개·날짜 시트는 그 탭 기준이다. 앱바 제목은 두 탭 모두 "새로 추가"다. 메모 저장이 성공하면 `MemoEditSideEffect.Close`로 작성 화면을 닫고, 오류 안내는 스낵바로 보낸다. 탭 모양은 보드 `AddEvent.dc.html` 28~30·216·217행(높이 40, 모서리 11, 바깥 4·14, 고른 탭 `surface-0`+`shadow-sm`+700 `ink-900`, 안 고른 탭 500 `ink-600`)이다.
17. **펼친 달력(크게 보기)의 메모**: 펼친 칸의 막대·"+N"은 기념일·일정만이다. 메모 막대는 SCREENS·design-system에 없어 만들지 않았다(접힌 달력의 점과 날짜 목록의 메모 카드·행만). `<Docs 후속>`
18. **메모 카드는 제목 한 줄**: SCREENS 구성 "메모 카드: '메모' 표시, 제목"대로 "메모" + 제목(D-131) + 작성자 태그(나·상대)다. `SCR-CAL-01.png`에는 제목 아래 본문 한 줄이 더 보이나 SCREENS 글에 없고 줄 수 규칙이 없어 넣지 않았다. `<Docs 후속>`
19. **날짜 목록·점의 메모 범위**: 보이는 달의 날짜만 그린다(앞뒤 하루로 받은 이웃 달 메모는 거르고 달력에 그리지 않는다, 기념일과 같다). 정렬은 `groupMemosByDay`가 작성 최신순(`createdAt` 내림차순), 같으면 `id` 오름차순으로 한다. 월 조회·달 넘김·당겨서 새로고침은 `refreshDated`를 일정·기념일과 함께 부르고 `setVisibleRange`도 함께 알린다. 날짜 칸 읽기 라벨은 "일정·메모 N개"에 메모를 일정 뒤에 합친다.

## 리뷰 기록

| 회차 | Critical | Major | 비고 |
| --- | --- | --- | --- |

## 남은 일

- 맥: Room 스키마 JSON(버전 5), Preview 대조 (묶음 C Preview: `CalendarPreview_Memo`, `EventEditPreview_KindTabs`(calendar), `EventEditPreview_MemoTab`(main))
- 묶음 C 확인 요청: 이 환경은 Gradle을 못 돌려 컴파일·테스트를 눈으로만 확인했다. CI에서 `assembleDebug testDebugUnitTest` 확인
- Docs 후속: SCR-MEMO-04 수정 화면 앱바 제목("메모 수정"으로 씀, 문서에 없음)
- Docs 후속: 펼친 달력의 메모 표시(결정 17), 캘린더 메모 카드의 본문 줄(결정 18)
