# 단위 5: 기념일 — SCR-ANNIV-01~02, 캘린더 기념일 표시, 디자인 맞추기(이슈 #12·#15 단위 5분)

> 브랜치: `feat/5-anniversary` · PR: #16 · 상태: 진행 중 (2026-11-10 범위 승인, 확인 필요 1~6 추천안대로)
> 기준: PRD v0.12 (FR-ANNIV, FR-CAL-01 AC2·AC3·AC8, D-115~D-122, D-125, D-126, Q-15·Q-16·Q-18·Q-19), SCREENS.md(캔버스 버전 43), API.md 1장·3.3(기념일 6개 경로, "같은 날 순서", "커서 정렬 키", 리드 보충)·5장
> Docs 기준 커밋: DuriseoApp/Docs main `cee0885`

## 요약

일이 네 묶음이에요. 구현은 이 순서로 나눠 맡겨요(같은 파일을 동시에 고치지 않게).

| 묶음 | 내용 |
| --- | --- |
| A. 기념일 | `domain/anniversary`, `AnniversaryApi`(6개 경로)·Room 테이블·`AnniversaryRepository`, 새 모듈 `feature:anniversary`(SCR-ANNIV-01·02) |
| B. 캘린더 기념일 표시 | SCR-CAL-01 기념일 카드·`brand-500` 고리·펼친 달력 막대, 앱바 "함께한 지 D+N ›"·개인 공간 "기념일 ›" → SCR-ANNIV-01, 날짜 목록 순서(D-119), 점 규칙(D-118), D+N 최소 D+1(D-116) |
| C. v0.12 규칙을 기존 화면에 | 로딩 덮개(D-115) 모든 쓰기·계정 요청, 당겨서 새로고침(Q-16), SCR-CAL-03 403이면 닫기(D-121), 스낵바 위 40(D-126) 남은 화면, 해제 초기화 뒤 이동처(D-125), 시간대 어긋남(Q-19 ①②) |
| D. 디자인 맞추기 | 이슈 #12 나머지(SCR-CAL-02 개인 공간, SCR-CAL-03 날짜 시트·시간 휠·알림 시트, SCR-COUPLE-02~04 상태, SCR-SET-01 상태), 이슈 #15 단위 5분(점 규칙, SCR-COUPLE-02 스낵바, 시간 휠 둘째 줄 `ink-400` D-128) |
| E. 코드 정리 | PLAN "앞 단위에서 넘어온 일" 5번 4건 |

## 확인 필요

| # | 빈 곳 | 추천안 | 근거 |
| --- | --- | --- | --- |
| 1 | **매년 반복 기념일을 고칠 때 등록한 날짜를 알 방법이 없음.** `GET /anniversaries/{id}`가 없고, 목록·캘린더 응답(`AnniversaryOccurrence`)의 `date`는 그 해의 발생일이에요. 등록일이 2025-11-20인 "생일"을 2026년 항목에서 열면 날짜 칸에 2026-11-20이 보이고, 그대로 `date`를 보내면 반복이 2026년부터 시작돼 2025년 캘린더에서 사라져요 | **앱만 고침(새 API 없음)**: 수정 화면의 날짜 칸은 누른 발생일을 보여 주고, `PATCH`에는 **사용자가 바꾼 필드만** 보내요. 날짜를 안 바꾸면 `date`를 보내지 않아 등록일이 그대로예요. 날짜를 바꾸면 고른 날이 새 등록일이에요. 바꾼 것이 없으면 요청 없이 닫아요. Docs 후속에 올려 API.md에 적게 해요. (다른 안: Docs·서버에 `GET /anniversaries/{id}`나 응답 필드를 더하기 — 서버 작업이 생겨 이 단위가 기다려야 해요) | API.md 3.3 `Anniversary.date` "등록한 날짜", 경로 표 |
| 2 | SCR-ANNIV-01 커플 화면 하단 안내 문구가 SCREENS에 글로 없음 | 그림(`SCR-ANNIV-01.png`, `--no-start-date.png`) 문구 그대로: "100일 단위와 매년 돌아오는 날은 처음 만난 날로 자동 계산돼요. 직접 만든 기념일은 만든 사람만 고칠 수 있어요." 개인 공간은 SCREENS 문구 | 디자인 이미지 |
| 3 | 다가오는 목록에서 **상대가 만든** 직접 기념일을 누르면 | 반응 없음(› 없음). 지난 기념일 규칙("내가 만든 항목에만 ›")과 같게 | SCREENS SCR-ANNIV-01 "권한", D-71 |
| 4 | SCR-ANNIV-02에서 403(`NOT_AUTHOR`)일 때 화면을 닫는지 (SCREENS는 "공통 오류 + 다시 불러와요", PRD D-121은 "수정 화면이면 닫고 돌아간다") | PRD대로 닫고 SCR-ANNIV-01로 돌아가 공통 오류 + 기념일 다시 불러오기. "다시 불러오기"는 SCR-ANNIV-01 목록과 지난 기념일 개수, 보이는 캘린더 달의 기념일 | PRD가 단일 출처, D-121 |
| 5 | SCR-ANNIV-02 저장 뒤 알림 권한 요청 (SCREENS "알림 권한") | 단위 7로 미뤄요. 일정 알림의 권한 요청도 단위 7에서 시점을 정하기로 했고(PLAN 단위 7), 알림 채널(D-136)·FCM이 단위 7이에요 | PLAN 단위 7 |
| 6 | PLAN·이슈에 없는 v0.12 결정 중 기존 화면에 닿는 것: D-125(해제 초기화 뒤 `GET /me`로 이동처), Q-19 기본값 ①(앱으로 돌아올 때 시간대가 다르면 다시 알림) ②(월 조회 앞뒤 하루 여유, 일정·기념일 모두) | 이 단위에 넣어요 (C 묶음). 작은 변경이고 기념일 월 조회(②)와 같은 코드예요 | PRD D-125, Q-19 |

## 범위

테스트 이름에 ID를 넣는다. Preview 이름은 `<화면>Preview_<상태>`.

### A. 기념일 (FR-ANNIV, SCR-ANNIV-01·02)

| ID | 내용 | 테스트 · Preview |
| --- | --- | --- |
| API `GET /anniversaries?from=&to=` | 캘린더용. 월 앞뒤 하루 여유(Q-19 ②), 응답 순서 그대로 Room에 위치와 함께 저장(리드 보충), 범위 새로고침(그 범위를 지우고 넣기) | `AnniversaryRemoteDataSourceTest` · `AnniversaryRepositoryTest: GET anniversaries는 응답 순서를 위치로 저장하고 범위를 교체한다` |
| API `GET /anniversaries/upcoming` | 커서 페이지, 화면 진입 때 첫 페이지부터 교체, 스크롤 끝에서 다음 페이지 이어 붙임 | `AnniversaryRepositoryTest: upcoming 첫 페이지는 목록을 교체하고 다음 페이지는 이어 붙인다` |
| API `GET /anniversaries/past` | `totalCount`로 "지난 기념일 N개", 0이면 행 숨김, 펼칠 때 목록 | `AnniversaryRepositoryTest: past totalCount를 저장하고 0이면 행이 없다` |
| API `POST`·`PATCH`·`DELETE /anniversaries` | 온라인 쓰기(`runOnlineWrite`), 성공 뒤 upcoming·past·보이는 달 다시 불러오기(자동 계산은 서버). `PATCH`는 바꾼 필드만 `<확인 필요 1>` | `AnniversaryRepositoryTest: PATCH는 바꾼 필드만 보낸다` · `…: 쓰기 성공 뒤 목록과 달을 다시 불러온다` |
| FR-ANNIV-01 AC1~AC3 | 함께한 날 카드 D+N(처음 만난 날 = D+1), 개인 공간 없음, 1보다 작으면 D+1 (D-116) | `DayCountTest: FR-ANNIV-01 AC1 처음 만난 날 당일은 D+1이다` · `…AC3 계산값이 1보다 작으면 D+1이다` |
| FR-ANNIV-02 AC1~AC6 | 자동 기념일은 서버 응답 그대로 표시, 누를 수 없음, 처음 만난 날을 바꾸면 다시 불러오기 | `AnniversaryListViewModelTest: FR-ANNIV-02 AC3 자동 기념일은 눌러도 이동하지 않는다` · `…: FR-ANNIV-02 AC3 처음 만난 날이 바뀌면 목록을 다시 불러온다` |
| FR-ANNIV-03 AC1 | 이름·날짜 필수, 이름 trim, "매년 반복" | `AnniversaryEditViewModelTest: FR-ANNIV-03 AC1 저장하면 앞뒤 공백을 지운 이름으로 POST한다` |
| FR-ANNIV-03 AC2 | 작성자만 수정·삭제 (상대 항목 누를 수 없음) `<확인 필요 3>` | `AnniversaryListViewModelTest: FR-ANNIV-03 AC2 상대가 만든 기념일은 눌러도 이동하지 않는다` |
| FR-ANNIV-03 AC3 | 2월 29일 → 서버 계산 그대로 표시 | (서버 몫, 앱은 응답 날짜 그대로) |
| FR-ANNIV-03 AC4 (D-122) | 새로 열면 오늘·매년 반복 켜짐, 이름 비면 [저장] 비활성, 30자에서 막음, 글자 수 표시 | `AnniversaryEditViewModelTest: FR-ANNIV-03 AC4 새 기념일은 오늘 날짜이고 매년 반복이 켜져 있다` · `…AC4 이름이 공백뿐이면 저장할 수 없고 30자를 넘겨 입력되지 않는다` |
| FR-ANNIV-03 AC5 (D-121) | 404: 기기에서 지우고 닫고 공통 오류. 403: 닫고 공통 오류, 다시 불러오기 `<확인 필요 4>` | `AnniversaryEditViewModelTest: FR-ANNIV-03 AC5 404면 기기에서 지우고 화면을 닫는다` · `…AC5 403이면 화면을 닫고 공통 오류를 보이고 다시 불러온다` |
| FR-ANNIV-03 AC6 | 같은 값 저장 → 앱은 바뀐 필드가 없으면 요청 없이 닫기 | `AnniversaryEditViewModelTest: FR-ANNIV-03 AC6 바꾼 것이 없으면 요청 없이 닫는다` |
| FR-ANNIV-04 AC1 | 오늘~2050 가까운 순, D-N 배지(오늘 D-day, 30일 이내 `brand-100`/`brand-700`) | `AnniversaryListViewModelTest: FR-ANNIV-04 AC1 daysLeft 0은 D-day이고 30일 이내는 강조한다` |
| FR-ANNIV-04 AC2 | 개인 공간: 카드 없이 직접 기념일만, 빈 상태 | Preview `AnniversaryListPreview_Solo`, `_SoloEmpty` |
| FR-ANNIV-04 AC3 (D-71) | 지난 기념일 접힘·펼침, 내 것만 › → SCR-ANNIV-02 | `AnniversaryListViewModelTest: FR-ANNIV-04 AC3 지난 기념일은 접혀 있고 내 항목만 수정으로 간다` · Preview `_PastOpen` |
| FR-ANNIV-04 AC4 | 커플 + 처음 만난 날 없음: 안내 카드 + [처음 만난 날 입력] → 처음 만난 날 시트(SCR-SET-01 소속, `PATCH /space`) | `AnniversaryListViewModelTest: FR-ANNIV-04 AC4 처음 만난 날이 없는 커플은 안내 카드를 보인다` · Preview `_NoStartDate` |
| FR-ANNIV-04 AC5 | 같은 날 순서는 서버 응답 순서 그대로 (Q-15) | Repository 위치 저장 테스트 |
| SCR-ANNIV-01 | 상태: 커플, 개인 공간, 개인 공간 빈 화면, 지난 펼침, 처음 만난 날 없음. 하단 안내 `<확인 필요 2>`. 오프라인·실패는 저장된 목록 그대로(D-112와 같은 방식). 스낵바 위 40 | Preview `AnniversaryListPreview_Couple`, `_Solo`, `_SoloEmpty`, `_PastOpen`, `_NoStartDate` |
| SCR-ANNIV-02 | 상태: 추가(커플·개인 문구), 수정, 삭제 확인 다이얼로그. 날짜 시트 1950~2050(D 묶음 새 날짜 시트), 로딩 덮개, 스낵바 위 40, 알림 권한은 단위 7 `<확인 필요 5>` | `AnniversaryEditViewModelTest: 삭제 확인 뒤 DELETE하고 닫는다` · Preview `AnniversaryEditPreview_Couple`, `_Solo`, `_Edit`, `_DeleteDialog` |
| 오류 | 400 `VALIDATION_FAILED`·`DATE_OUT_OF_RANGE`(앱이 막지만 오면 공통 오류), 오프라인 "인터넷 연결이 필요해요" | `AnniversaryEditViewModelTest: 오프라인이면 요청하지 않고 인터넷 연결 안내를 보인다` |

### B. 캘린더 기념일 표시 (SCR-CAL-01)

| ID | 내용 | 테스트 · Preview |
| --- | --- | --- |
| FR-CAL-01 AC2 (D-119, D-117) | 날짜 목록 순서: 기념일(서버 순서) → 종일(시작 날짜순) → 시간(보이는 시각순, 같으면 시작 일시순). 기념일 카드: "기념일" 표시, 이름, 자동은 그날의 D+N(날짜 − 처음 만난 날 + 1), 직접은 만든 사람 색 점. 누르면 SCR-ANNIV-01 | `CalendarViewModelTest: FR-CAL-01 AC2 날짜 목록은 기념일 종일 시간 순이다` · `…AC2 자동 기념일 카드는 그날의 D+N이다` · Preview `CalendarPreview_Anniversary` |
| FR-CAL-01 AC2 (D-65) | 달력: `brand-500` 고리(지름 6, 선 1.5), 한 날 표시 최대 3개(고리 1 + 점 2), 펼친 달력은 `brand-50` 면 + `brand-500` 테두리 막대 | `CalendarDayMarksTest: 기념일이 있으면 고리 하나와 점 둘까지다` · Preview `_Expanded` |
| FR-CAL-01 AC8 (D-118) | 점: 서로 다른 사람 색만 앞에서부터, 종일 → 시간 (메모는 단위 6) | `CalendarDayMarksTest: FR-CAL-01 AC8 같은 색 점은 한 번만 종일 시간 순이다` · Preview `_Solo` (`SCR-CAL-01--solo.png`) |
| FR-CAL-01 AC3, FR-ANNIV-01 AC3 | 앱바 "함께한 지 D+N ›" → SCR-ANNIV-01, 최소 D+1 (D-116). 개인 공간 "기념일 ›" | `CalendarViewModelTest: FR-ANNIV-01 AC3 앱바 D+N은 최소 D+1이다` |
| FR-ANNIV-02 AC3 | 처음 만난 날을 바꾸면 보이는 달의 기념일 다시 불러오기 | `CalendarViewModelTest: FR-ANNIV-02 AC3 처음 만난 날 저장 뒤 기념일을 다시 불러온다` |

### C. v0.12 규칙을 기존 화면에

| ID | 내용 | 테스트 · Preview |
| --- | --- | --- |
| D-115, Q-18 | 쓰기·계정 요청 중 `scrim` 덮개 + 흰 판 84(모서리 16, shadow-dialog) + `brand-500` 원형 44/선 4, 터치 막기, 문구 없음, 바로 표시. 대상: 로그인, 연동, 로그아웃, 탈퇴, 커플 연결·해제, 가져오기, 처음 만난 날, 우리 색상, 일정·알림·기념일 저장·삭제. 조회·SCR-COUPLE-02 자동 요청은 제외 | 공용 `DuriseoLoadingOverlay` · 각 ViewModel `…: 요청 중에는 loading이 true이고 끝나면 false다` (화면별 1개) · Preview `LoadingOverlayPreview` |
| Q-16, NFR-SYNC-02 | SCR-CAL-01 당겨서 새로고침(원판 44 + 원형 24/2): 보이는 달의 일정·기념일 | `CalendarViewModelTest: NFR-SYNC-02 당겨서 새로고침하면 보이는 달을 다시 불러온다` · Preview `CalendarPreview_Refresh` |
| D-121 (SCR-CAL-03) | 수정 저장 403 `NOT_AUTHOR`면 수정 화면을 닫고 상세로, 공통 오류, 상세는 다시 불러오기 | `EventEditViewModelTest: D-121 수정 저장이 403이면 화면을 닫고 상세를 다시 불러온다` |
| D-126 | 스낵바: SCR-COUPLE-01·02·03, 연결 완료 시트 위, SCR-ANNIV-01·02는 위 40 | `SnackbarPositionTest: D-126 탭이 없는 화면과 시트 위는 위에서 40이다` |
| D-125 | 해제를 알고 초기화한 뒤 `GET /me`로 이동처: 개인 공간 → SCR-COUPLE-01, 이미 다른 커플 → SCR-CAL-01 + 연결 완료 시트 | `MainViewModelTest: D-125 초기화 뒤 이미 다른 사람과 연결돼 있으면 캘린더로 가서 연결 완료 시트를 띄운다` |
| Q-19 ① | 앱으로 돌아올 때(프로세스 ON_START) 기기 시간대가 저장값과 다르면 `PATCH /me` 다시 | `SessionStartupTest: Q-19 앱으로 돌아올 때 시간대가 다르면 다시 알린다` |
| Q-19 ② | 월 조회 앞뒤 하루 여유(일정 `GET /events`, 기념일 `GET /anniversaries`), 3개월 상한 안 | `CalendarViewModelTest: Q-19 월 조회는 앞뒤 하루를 더해 요청한다` |

### D. 디자인 맞추기 (이슈 #12, #15 단위 5분)

| 화면 | 바꿀 것 | Preview |
| --- | --- | --- |
| SCR-CAL-02 | 개인 공간 상세·삭제 확인(`--solo.png`, `--solo-delete-dialog.png`), 제목 24/700 | `EventDetailPreview_Solo`, `_SoloDeleteDialog` |
| SCR-CAL-03 | 날짜 시트(안내 문구, 연도·월 선택, `--date-picker.png`) — 공용으로 만들어 SCR-ANNIV-02·처음 만난 날 시트도 같은 부품, 시간 휠 3줄 직접 구현(오전/오후·시·분, 분 1분 단위, `surface-100` 띠, 둘째 줄 `ink-400` D-128, `--time-picker.png`), 내 알림 시트(`--reminder-sheet.png`), 비활성 [저장] 글자색·범위 끝 이전·다음 달 버튼 색 대조 | `EventEditPreview_DatePicker`, `_TimePicker`, `_ReminderSheet`, `_Loading` |
| SCR-COUPLE-02 | 코드 받는 중(`--loading.png`), 스낵바 위 40(`--not-yet.png`, D-126) | `InviteCreatePreview_Loading`, `_NotYet` |
| SCR-COUPLE-03 | 오류·입력 제한(`--error.png`, `--rate-limited.png`) | `InviteEnterPreview_Error`, `_RateLimited` |
| SCR-COUPLE-04 | 처음 만난 날 고른 시트(`--date-set.png`), 점선 원 점·간격 6·4 대조 | `LinkedSheetPreview_DateSet` |
| SCR-SET-01 | 처음 만난 날 없는 메뉴·연결 정보(`--no-start-date.png`, `--couple-no-start-date.png`), 혼자 사용 중 대조, 게스트 ID, 연도·월 선택(`--start-date-sheet-year-month.png`, 접근성 문구·화살표 중복), 프로필 카드 D+N 최소 D+1(D-116) | `SettingsPreview_NoStartDate`, `ConnectionInfoPreview_NoStartDate`, `StartDateSheetPreview_YearMonth` |

NFR-TIME-02(작성일·받은 시각 표기)는 이 단위 화면에 쓰는 곳이 없어 단위 6·7에서 해요.

### E. 코드 정리 (PLAN 넘어온 일 5)

| 할 일 | 테스트 |
| --- | --- |
| `MeRepositoryImpl.acknowledgeSpace`의 Room 읽기·확인한 공간 ID 저장을 `MeWriteLock` 아래로 | `MeRepositoryTest: acknowledgeSpace는 MeWriteLock 아래에서 읽고 저장한다` |
| 초기화 전에 받은 오래된 `GET /me` 응답 버리기(요청 번호) | `MeRepositoryTest: 초기화 전에 시작한 GET me 응답은 저장하지 않는다` |
| `PersonColorsTest`의 상대 경로 정리 | 기존 테스트 통과 |
| Room 스키마 JSON | 클라우드에서 빌드할 수 없어 **맥에서**(사람). DB 버전이 오르면 그 버전 JSON도 |

## 서버 연동

| API | 개발 서버 배포 여부 | 확인 |
| --- | --- | --- |
| 기념일 6개 경로 (서버 단위 4) | 배포됨 | 실기기 (맥) |
| `GET /me`, `PATCH /me`, `PATCH /space`, 일정 경로 | 배포됨 | 기존 |

## 범위 밖

- 기념일 푸시와 알림 목록에서 들어오는 길, 알림 권한 요청 → 단위 7
- 캘린더 메모 카드·메모 점, 작성 화면 [일정|메모] 탭 → 단위 6
- 옛 디자인 이슈 #1~#4의 기념일 화면은 이 단위로 만들어지지만, 이슈에 메모·알림 화면도 있어 닫지 않아요

## 결정

구현하면서 정한 것 중 문서에 없는 것. 문서에 있어야 할 결정이면 여기 적지 말고 작업을 멈추고 보고한다.

**묶음 A (기념일)**

1. **수정 화면이 항목을 찾는 방법**: `GET /anniversaries/{id}`가 없어서 경로에 `anniversaryId`와 누른 발생일(`date`)을 싣고, 수정 화면이 Room의 upcoming(id·날짜가 같은 것) → past(id가 같은 것) 순으로 찾는다. 없거나 내가 만든 것이 아니면 공통 오류 + 닫기. 지난 기념일은 `repeatsYearly = false`(1회성)다.
2. **보이는 달 다시 불러오기**: 저장소가 마지막으로 성공한 캘린더 범위(`refreshRange`)를 기억하고, 쓰기 성공·403 뒤 `refreshAll`이 upcoming·past 첫 페이지와 그 범위를 다시 불러온다. 캘린더(묶음 B)는 월을 열 때 `refreshRange(calendarMonthRange(month))`를 부르면 된다.
3. **404·403 처리는 저장소가 한다**: 404면 발생일·지난 항목을 Room에서 지우고(지난 기념일 개수도 하나 줄임), 403이면 `refreshAll`. 화면은 둘 다 닫고 공통 오류를 보인다(확인 필요 4). 화면 모델이 닫혀도 요청이 끊기지 않게 저장소 안에서 처리했다.
4. **페이지 상태를 Room에**: `anniversary_page_state`에 다음 커서와 past `totalCount`를 저장해 `hasMore`를 Flow로 낸다. 커서는 해석하지 않는다. 다음 페이지 요청은 목록별 `Mutex`로 겹치지 않게 한다. 화면 진입 때는 첫 페이지만 교체하고(past도 첫 페이지), 스크롤 끝(마지막 3칸 안)에서 이어 붙인다.
5. **`DayCount`**: 함께한 지 D+N(최소 1, D-116)은 `domain/anniversary/DayCount.of`다. 기존 `StartDateRules.dayNumber`(최소 1이 아님)는 캘린더 앱바·SCR-SET-01 프로필 카드가 쓰고 있어 이번에 건드리지 않았다. 묶음 B(앱바)·D(프로필 카드)가 `DayCount.of`로 바꾼다.
6. **디자인 시스템 추가**: `DuriseoTopAppBar`에 오른쪽 행동 버튼(선택), `DuriseoButtonSize.Small`(높이 44, 14/700, 보드 `Anniversaries.dc.html`의 [처음 만난 날 입력]·[기념일 추가]), `ic_repeat`(매년 반복 아이콘, 보드 `AnniversaryEdit.dc.html`).
7. **날짜 시트 제목**: SCR-ANNIV-02의 날짜 시트는 일정과 같은 부품이고 새 문구를 만들지 않으려고 제목을 날짜 칸 이름 "날짜"로, 안내 줄은 없앴다(묶음 D의 새 날짜 시트에서 교체).
8. **처음 만난 날이 없는 커플이 직접 기념일도 없을 때**(문서에 없는 모서리): 안내 카드 아래에 개인 공간과 같은 빈 화면("아직 기념일이 없어요" + [기념일 추가])을 보인다. SCREENS는 개인 공간의 빈 상태만 적었다. Docs 후속에 올려 확인한다.
9. **삭제 확인 문구의 조사**: SCREENS 그대로 "[기념일 이름]이 캘린더와 기념일 목록에서 사라져요."로 "이"를 고정했다. 받침 없는 이름("생일")은 어색할 수 있다. 문구는 문서가 정하므로 그대로 두고 Docs 후속에 올린다.
10. **스낵바 위 40**: `MainNavigator`의 위쪽 스낵바 목록에 SCR-ANNIV-01·02를 더하고 `SnackbarPositionTest`에 두 줄을 더했다(D-126 나머지 화면은 묶음 C).
11. **편집 화면 스크롤 없음**: SCR-ANNIV-02는 내용이 짧아 스크롤 없이 [기념일 삭제]를 화면 아래에 붙였다(보드 `margin-top: auto`).
12. **범위 표 테스트 이름과 다른 점**: `…AC5 404면 기기에서 지우고 화면을 닫는다`·`…AC5 403이면 … 다시 불러온다`는 가짜 저장소가 저장소 계약(404면 지움, 403이면 다시 불러옴)을 흉내 내고, 진짜 동작은 `AnniversaryRepositoryTest`의 `FR-ANNIV-03 AC5 404면 기기에서 지운다`·`…403이면 목록과 달을 다시 불러온다`가 확인한다.
13. **Room 스키마 JSON**: 버전이 4가 됐다. 클라우드에서 빌드할 수 없어 맥에서 만들어 커밋한다 (E 묶음 항목).

## 리뷰 기록

| 회차 | Critical | Major | 비고 |
| --- | --- | --- | --- |

## 남은 일

- 묶음 A: 클라우드에서 `./gradlew`가 돌지 않아(Gradle 배포판을 받을 수 없음) 눈으로만 확인했다. PR #16 CI에서 `assembleDebug testDebugUnitTest` 확인 필요. Preview는 Android Studio에서 확인(맥)
- 묶음 B가 알아야 할 것: 캘린더 월 조회는 `AnniversaryRepository.refreshRange(from, to)`와 `observeRange(from, to)`(범위는 `calendarMonthRange(month)`), SCR-ANNIV-01 이동은 `MainNavigator.navigateToAnniversaries()`(`feature:main`)이고 `calendarNavGraph`에 `onOpenAnniversaries` 콜백을 더해 연결, D+N은 `DayCount.of`·`DayCount.onDate`
- Docs 후속: 결정 8(처음 만난 날도 직접 기념일도 없는 커플의 빈 화면), 결정 9(삭제 문구 조사), 확인 필요 1(API.md에 수정 화면의 날짜 처리)
