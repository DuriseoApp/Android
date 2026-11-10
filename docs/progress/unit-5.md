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

**묶음 B (캘린더 기념일 표시)**

14. **월 조회 범위**: 일정·기념일 모두 `calendarMonthRange(month)`(앞뒤 하루)로 `refreshRange`를 부른다(Q-19 ②). 구독(`observe…`)은 일정이 그 달 1일~말일, 기념일이 앞뒤 하루 범위이고, 달력에는 보이는 달의 날짜만 그린다(이웃 달 가장자리 기념일은 `groupAnniversariesByDay`가 뺀다). 일정·기념일 요청은 병렬이고 한쪽이 실패해도 다른 쪽은 그대로다.
15. **당겨서 새로고침(Q-16)**: `CalendarUiState.refreshing`은 당겼을 때만 켜고(화면 진입·달 넘김에는 켜지 않는다, D-112), 두 요청이 끝나면 끈다. 오프라인·실패는 안내 없이 끝난다(문서에 없어 D-112와 같게). 요청 중에 또 당겨도 한 번만. 목록이 비어 있어도 당길 수 있게 `EmptyDay`를 스크롤 영역으로 뒀다. 당기는 동안의 모양은 보드에 없어서 끄는 만큼 원판이 나타나게만 했다(알파).
16. **기념일 카드 보조 줄**: SCREENS 글에는 없지만 이미지(`SCR-CAL-01--anniversary.png`, `--solo.png`)와 보드 `sub`에 있는 "자동 기념일"·"내가 만든 기념일 · 매년"·"상대가 만든 기념일"을 그대로 그렸고, 직접 기념일의 태그는 일정 카드와 같은 사람 태그(점 + 나·상대), 자동은 점 없는 "D+N" 알약이다. **SCREENS 글(기념일 카드 구성)과 이미지가 달라 Docs 후속에 올린다.** 필요 없다면 `subText`·`AnniversaryTag`만 지우면 된다.
17. **날짜 목록 정렬**: 시간 일정은 목록 카드에 보이는 시각(분 단위, 기기 시간대) → 시작 일시 → id 순(D-119)이라 `EventRules.sortForDay`(시작 일시순) 대신 `CalendarViewModel.sortForCalendarDay`를 새로 썼다. 종일은 시작 날짜 → id. `EventRules.sortForDay`와 그 도메인 테스트는 E 묶음에서 지웠다.
18. **점 구분 기준**: "서로 다른 사람 색"을 대상(나·상대·함께)으로 구분한다(사람 색은 대상마다 하나). 메모는 단위 6에서 시간 일정 뒤에 더한다 (`calendarDayMarks`).
19. **앱바**: 두 줄(제목 + 링크)을 하나의 버튼(최소 높이 44)으로 묶었고 접근성 라벨은 보드 aria-label("기념일 보기, 함께한 지 D+N", "나의 캘린더, 기념일 보기")이다. 처음 만난 날이 없는 커플은 그대로. 설정 프로필 카드는 `StartDateRules.dayNumber`를 계속 쓴다(묶음 D).
20. **Preview 이름**: 캘린더 Preview를 `CalendarPreview_<상태>`로 모두 바꿨다(`_Default`, `_Anniversary`, `_AnniversaryCustom`, `_Expanded`, `_ExpandedAnniversary`, `_Solo`, `_Refresh`, `_NoStartDate`, `_EmptyDay`, `_StartDateSheet`, `_Cards`).

**묶음 C (v0.12 규칙을 기존 화면에)**

21. **로딩 덮개 부품**: `core:designsystem`의 `DuriseoLoadingOverlay`(덮개 + 판 84 + 원형 44/선 4, 뒤로 가기·터치 막기)와 `DuriseoLoadingLayer(loading) { 화면 }`(덮개 + 뒤 화면 접근성 가리기). `DuriseoDialogLayer`에는 `loading` 인자를 더해 다이얼로그·시트 위까지 덮는다. 84·선 4는 토큰에 없는 수치지만 design-system.md "로딩 표시" 표가 값을 직접 정해(84 = 44 + 좌우 `space-5`) 그 파일 안의 상수로 뒀다. `trackColor`는 `Line100`.
22. **`loading` 상태**: 각 화면 상태에 `loading`을 뒀다. 이미 있는 `saving`·`requesting`이 곧 요청 중인 화면(전체 메뉴·우리 색상·연결 정보·커플 연결·코드 입력·연결 완료 시트·캘린더·기념일 목록)은 `val loading get() = …`로 읽는다. 소셜 SDK 창이 떠 있는 동안은 서버 요청이 아니라 덮지 않아야 해서 SCR-START-01·계정은 `loading`을 따로 뒀다(`requesting`은 버튼 잠금용으로 그대로). 일정 작성·수정(SCR-CAL-03)·기념일 작성·수정(SCR-ANNIV-02)·일정 상세(SCR-CAL-02)는 성공하면 화면이 닫힐 때까지 `saving`/`deleting`을 켜 둬서(중복 방지) 덮개만 닫기 직전에 걷는다. 결과 안내는 덮개를 걷은 뒤에 보낸다.
23. **덮개 범위**: 화면 하나의 내용 영역이다. 캘린더·메모 탭 루트(처음 만난 날 저장)에서는 하단 탭까지는 덮지 않는다(탭 막대는 `MainScaffold` 소속). 요청 중 탭을 눌러도 요청은 그대로 끝나고 이동만 일어난다. 문서가 "화면 전체"라 하므로 필요하면 `MainScaffold` 위로 올린다.
24. **D-121(SCR-CAL-03)**: 403 `NOT_AUTHOR`면 404처럼 수정 화면을 닫는다. 상세 다시 불러오기는 저장소가 이미 하고(`EventRepositoryImpl.thenRereadIfNotAuthor`, `EventRepositoryTest`가 확인) 상세도 `ON_START`에 `Shown`으로 다시 읽는다. 앞 단위의 "403이면 화면 그대로" 테스트는 D-121에 맞춰 바꿨다.
25. **D-126**: 위치 규칙을 목록에서 뒤집었다. 탭 루트(캘린더·메모)가 아니면 모두 위에서 40, 연결 완료 시트가 떠 있는 동안은 탭 루트여도 위(`isSnackbarAtTop(linkedSheetVisible, …)`). 그래서 전체 메뉴·계정·우리 색상·연결 정보도 위로 옮겨졌다(SCREENS SCR-SET-01~03 "스낵바 위에서 40"). SCR-COUPLE-02는 자기 호스트의 바닥 112를 위 40으로 고쳤다(그림 대조는 묶음 D). 시트가 떠 있으면 FAB은 스낵바 위로 올라가지 않는다.
26. **D-125**: 알림은 저장소가 `GET /me`를 Room에 저장한 뒤에 낸다(`resetAfterUnlink`, 다음 실행의 `refreshMe` 모두). `MainViewModel`이 알림을 받아 Room의 내 정보가 커플 공간이면 `MainEffect.SpaceRelinked`(캘린더로 이동, 시트는 `linkedSheetNeeded`가 띄움), 개인 공간이거나 내 정보가 없으면(GET /me 실패·오프라인) 기존 `SpaceUnlinked`(SCR-COUPLE-01)다. `resetAfterUnlink`는 지우기 전의 커플 공간 ID를 기억했다가 다른 커플 공간이 오면 확인한 공간 ID로 되살려 시트가 뜨게 한다(`refreshMe` 경로의 기존 규칙과 같다). `refreshMe` 경로는 "커플 X → 커플 Y"일 때 기존대로 알림을 내지 않고 시트만 뜬다(앞 단위 리드 결정, `SpaceChangeTest`). 이때 사용자가 캘린더 밖에 있으면 이동은 없다(Docs 후속).
27. **Q-19 ①**: `lifecycle-process`는 MODULES.md 3장에 없어 쓰지 않고, `MyApp`이 `ActivityLifecycleCallbacks`로 "보이는 Activity가 0 → 1"을 앱으로 돌아온 순간으로 본다(첫 시작은 건너뜀). `SessionStartup.onForeground()`가 로그인 상태에서 `MeRepository.syncTimeZone(ZoneId.systemDefault())`를 부른다(같으면 요청 없음). 주입된 `Clock`은 `Clock.systemDefaultZone()`이라 앱이 만들어질 때의 시간대에 묶여 있어 이 경로는 `ZoneId.systemDefault()`를 직접 읽는다.

**묶음 D (디자인 맞추기)**

28. **SCR-CAL-02 제목 24/700**: 토큰에 24가 없어(`title-lg` 22) 보드 값(24, 행간 1.35, 자간 -0.01em)을 `EventDetailScreen`의 상수 `DetailTitleStyle`로 뒀다. design-system.md에 이 값이 없으니 Docs 후속(토큰 추가 또는 22로 정리). 정보 줄 아이콘(달력·사람)·알림 줄 종 아이콘·삭제 버튼 아래 막대(위 선 1)도 보드대로 바꿨다.
29. **날짜 시트는 이미 공용**: `StartDatePickerSheet` 하나를 캘린더·설정·연결 완료 시트·SCR-CAL-03·SCR-ANNIV-01 처음 만난 날 시트·SCR-ANNIV-02가 쓴다. 이번에 연도·월 선택을 보드대로 다시 만들고(연도 줄 `‹ 2026년 ›` + 월 칩 3열 4줄, 연도 화살표 범위 끝 `ink-300`, 연도 목록 단계 없음), 접근성 문구를 보드대로 정리했다("연도와 월 고르기, 2026년 4월", 묶음 "연도와 월 고르기", "이전 연도"·"다음 연도"). 버튼 글자는 `confirmText`로 받는다: 처음 만난 날 "저장", 일정·기념일 날짜 "완료". SCR-ANNIV-02는 기념일용 문구가 문서에 없어 묶음 A 결정 7(제목 "날짜", 안내 줄 없음)을 그대로 두고 버튼만 [완료]로 맞췄다.
30. **종료 날짜·시간 시트 제목**: 문서에 "시작 날짜"·"시작 시간"만 있고 종료는 "그림에 없어요"다. 시작과 짝으로 "종료 날짜"·"종료 시간"을 썼다(추측). Docs 후속에서 확정 필요.
31. **시간 휠**: `core:designsystem`의 `DuriseoTimeWheel`(LazyColumn 3줄 + 스냅, 줄 44·5줄, 가운데 `surface-100` 띠, 가운데 `titleLarge`/`ink-900`, 첫 줄 `ink-500`, 둘째 줄 `ink-400`(D-128), 시 1~12, 분 0~59). 값 계산은 순수 `TimeWheelValues`(스크롤 → 줄, 12↔24시간)이고 `TimeWheelValuesTest`가 확인한다. 시트(제목 + 휠 + [완료])는 `EventEditScreen`의 `TimeSheet`. 휠은 끝에서 이어지지 않는다(보드에 이어짐 표시가 없음). 스크롤 도중 [완료]를 누르면 그때 가장 가까운 줄의 값이 들어간다.
32. **내 알림 시트**: 설명 "나에게만 와요", 칩 글자 `ink-700`, 맨 아래 [완료]. 칩을 눌러도 시트가 닫히지 않고 [완료]가 닫는다(그림에 [완료]가 있어서). `EventEditViewModelTest`의 해당 테스트를 바꿨다.
33. **비활성 [저장] 글자색**: 보드(`AddEvent.dc.html` 24행)에 비활성 모양이 없다. 지금 값(`ink-500`)은 비활성 큰 버튼·초대코드 비활성 버튼 글자와 같은 조합이라 그대로 뒀다. 범위 끝 이전·다음 달 버튼은 `ink-300`(연도 화살표와 같은 규칙)이다.
34. **DuriseoButton 비활성 글자 `ink-600` → `ink-500`**: design-system "초대코드 만들기 코드를 받기 전 모양"(보드 95행)이 `line-100` 면 + `ink-500` 글자고 `DuriseoLargeButton` 비활성과 같다. 공용 부품이라 비활성 `DuriseoButton` 모두 바뀐다.
35. **SCR-COUPLE-03 오류 줄**: 아이콘 16(`ic_alert_circle`) + 글자 13/18/600 `danger-600`, 칸 아래 8(design-system 글은 "아래 4"이지만 보드는 칸 사이 12에서 위 여백 -4라 8이고 그림도 8이다 → Docs 후속). 없는 코드 같은 값 오류는 채운 칸 테두리가 `danger-600`, 입력 제한(`RateLimited`)은 그대로.
36. **SCR-COUPLE-02 코드 받는 중**: 접근성 문구를 보드대로 "초대코드"(코드 없음)로 하고, 비활성 버튼 색은 결정 34. 스낵바는 이미 위 40이라 그림과 대조만 했다.
37. **점선 원(아직 없음 자리)**: 둘레를 정수 조각으로 나눠 점 6 : 간격 4 비율로 그린다(`ringDashIntervals`, 지름 56에 17조각, `RingDashTest`). 그림 `SCR-SET-01--couple-solo.png`와 눈으로 대조했다(맥 Preview에서 한 번 더 확인).
38. **SCR-SET-01**: 프로필 카드 D+N은 `DayCount.of`(최소 D+1, `SettingsViewModelTest`). 처음 만난 날 없는 커플은 가운데 마크를 아바타 높이(72) 가운데에 둔다. 연결 정보는 이미 두 행을 숨기고 있었다. 게스트 ID(계정 화면)는 보드와 같아 바꾸지 않았다(보드의 `word-break: break-all`은 Compose에 없어 하이픈에서 줄바꿈). 아바타 연필 배지·[내 프로필 수정]은 단위 8(SCR-SET-04)이라 그대로 뺐다.
39. **로딩 덮개 화면 전체(결정 23 보완)**: `DuriseoLoadingHost`(+`DuriseoLoadingHostState`, `LocalLoadingHost`)를 `MainScreen` 최상단에 두었다. `DuriseoLoadingLayer`·`DuriseoDialogLayer`는 호스트가 있으면 요청 중임을 호스트에 알리기만 하고 덮개는 호스트가 가장 위(하단 탭·연결 완료 시트 위)에 하나만 그린다. 호스트가 없으면(Preview, 단독 화면) 예전처럼 직접 그린다. `DuriseoLoadingHostStateTest`, Preview `MainScreenCalendarTabPreview_Loading`, `LoadingOverlayPreview_HostCoversBottomBar`.
40. **시간대(Q-19 ① 보완)**: `core:common`의 `DeviceZoneClock`이 `Clock`을 대신 제공한다(시각은 시스템, 시간대는 `getZone()`을 부를 때마다 `ZoneId.systemDefault()`). 이전 `Clock.systemDefaultZone()`은 만들 때 시간대에 묶였다. `SessionStartup.syncTimeZoneIfSignedIn`의 기본 인자와 로그인 때 `syncTimeZone`이 이 시계의 `zone`을 쓴다. 표시(캘린더·상세·작성·설정)도 모두 `clock.zone`을 그때그때 읽으므로 기기 시간대를 따른다. 다만 이미 그려진 목록은 Room이 새로 내보낼 때까지 다시 계산되지 않는다(앱으로 돌아올 때 월 다시 불러오기가 Room을 갱신해 반영됨, 화면 강제 갱신은 하지 않음). `DeviceZoneClockTest`, `SessionStartupTest: Q-19 앱이 떠 있는 동안 기기 시간대가 바뀌면 바뀐 시간대를 보낸다`. Android가 시간대 변경 때 JVM 기본 시간대를 갱신한다는 점에 기댄다(맥 실기기에서 확인).
41. **묶음 E 정리**: `acknowledgeSpace`의 Room 읽기·저장은 `MeWriteLock` 아래에서 한다. `refreshMe`는 요청 전에 캐시 세대(결정 42)를 적어 두었다가 락을 얻어 저장할 때 값이 바뀌었으면(초기화 전에 시작한 응답) 저장하지 않고 성공으로 돌려준다. `resetAfterUnlink`는 락을 기다리기 전에 세대를 올리고, 안에서 쓰는 `GET /me`는 세대 확인 없이 저장한다. 지우기 전 커플 ID를 기억하는 D-125 로직은 그대로다. `PATCH /me`(`syncTimeZone`) 응답도 같은 확인을 한다(리뷰 1회차). `PersonColorsTest`는 작업 디렉터리가 모듈이든 레포 루트든 `strings.xml`을 찾고, 없으면 찾은 곳을 알려 주며 실패한다(build 설정 변경 없음).
42. **캐시 세대 `LocalCacheGeneration`**(리뷰 1회차, `data:repository` 싱글턴): 캐시를 지우는 쪽(`SessionManager`의 로컬 데이터 지우기, 공간 변경·가져오기 결정의 일정·기념일 비우기, `resetAfterUnlink` 맨 앞)이 세대를 올리고, `GET /me`·`PATCH /me`·일정 범위·기념일 범위·upcoming·past 응답은 요청 전에 적어 둔 세대가 같을 때만 저장한다(다르면 버리고 성공). 기념일·일정은 확인과 저장, 올림과 지우기가 같은 Mutex 아래라 끼어들지 못한다(`saveIfCurrent`, `clearing`). `MeRepositoryImpl`의 `resetGeneration`은 이것으로 합쳤다. 락 순서는 `MeWriteLock` → 목록 락(`upcomingLock`·`pastLock`·`rangeLock`) → 캐시 Mutex(맨 안쪽, 블록 안에서 다른 락을 잡지 않고 같은 Mutex를 다시 부르지 않는다). 남은 틈: 내 정보 저장의 확인은 `MeWriteLock` 아래인데 로그아웃·세션 끊김의 지우기는 그 락 밖이라, 확인 직후 지우기가 끼면 내 정보 한 건이 남을 수 있다(세대는 올라가므로 다음 요청부터는 막힌다).
43. **보이는 달**(리뷰 1회차): `AnniversaryRepository.setVisibleRange(from, to)`를 더했다. `CalendarViewModel`이 달이 바뀔 때(`refreshVisibleMonth`, 다른 달 날짜 선택) 알리고, 이전 새로고침 Job은 취소한다. 쓰기 뒤 `refreshAll`은 알려 준 범위를 다시 불러오며, 알린 뒤 캐시가 지워졌으면(세대가 바뀜) 비운 것으로 본다. `refreshRange`는 더 이상 "마지막 성공 범위"를 기억하지 않는다. 겹치는 더 새 범위 응답이 이미 저장됐으면 오래된 응답은 겹치는 날을 빼고 나머지만 저장한다(요청 번호, 가운데가 겹치면 저장하지 않음).
44. **기타 정리**(리뷰 1회차): `appendPast`는 다음 페이지에 `totalCount`가 없으면 저장된 값을 유지한다. `refreshAll`은 upcoming·past·범위를 병렬로 부르고, 쓰기 성공 뒤 다시 불러오기에서 난 예외는 삼키고 클래스 이름만 로그에 남긴다(쓰기 결과는 성공). `AnniversaryListViewModel`은 겹치는 LazyColumn key 뒤에 `#순번`을 붙인다. `EventEditViewModel`은 `finishLoading()`을 `ShowMessage`보다 먼저 한다(D-115).

## 리뷰 기록

| 회차 | Critical | Major | 비고 |
| --- | --- | --- | --- |
| 1 (sonnet) | 0 | 4 | `PATCH /me` 응답 세대 확인, 해제·로그아웃 뒤 늦은 응답이 일정·기념일 캐시를 되살리는 경쟁(캐시 세대), 보이는 달 기억과 이전 달 새로고침 취소, 기념일 동시성 테스트. Minor 5건 같이 고침 |

## 남은 일

- 묶음 A: 클라우드에서 `./gradlew`가 돌지 않아(Gradle 배포판을 받을 수 없음) 눈으로만 확인했다. PR #16 CI에서 `assembleDebug testDebugUnitTest` 확인 필요. Preview는 Android Studio에서 확인(맥)
- 묶음 B가 알아야 할 것: 캘린더 월 조회는 `AnniversaryRepository.refreshRange(from, to)`와 `observeRange(from, to)`(범위는 `calendarMonthRange(month)`), SCR-ANNIV-01 이동은 `MainNavigator.navigateToAnniversaries()`(`feature:main`)이고 `calendarNavGraph`에 `onOpenAnniversaries` 콜백을 더해 연결, D+N은 `DayCount.of`·`DayCount.onDate`
- 묶음 B: 클라우드에서 `./gradlew`를 돌릴 수 없어 눈으로만 확인했다(PR CI 확인 필요). 특히 `PullToRefreshBox`(BOM 2025.08의 material3에서 실험 API라 `@OptIn(ExperimentalMaterial3Api::class)`를 붙였다)·`CircularProgressIndicator`의 `trackColor`·`strokeCap`, 도는 호가 보드(1/4 호)와 다른지는 맥 Preview에서 확인
- 묶음 C(로딩 덮개)가 알아야 할 것: 캘린더 처음 만난 날 시트 저장(`saveStartDate`)이 덮개 대상이다. `CalendarStartDateSheet.saving`이 이미 있어 `loading`은 거기서 읽으면 된다. 캘린더 당겨서 새로고침(Q-16)·월 조회는 덮개 제외(조회). Q-19 ②는 이 묶음에서 끝났다(C 표의 Q-16·Q-19 ② 행)
- 묶음 C: 클라우드에서 `./gradlew`를 돌릴 수 없어 눈으로만 확인했다(PR CI 확인 필요). Preview는 `LoadingOverlayPreview` 외에 로딩 상태가 있는 화면 Preview(`StartScreenLoadingPreview` 등)에 `loading`이 켜졌는지 맥에서 확인
- 묶음 D가 알아야 할 것: SCR-CAL-03 `_Loading` Preview는 `EventEditUiState(loading = true)`로 그린다(`DuriseoDialogLayer(loading)`가 덮음). SCR-COUPLE-02 스낵바는 위 40으로 이미 옮겼고 `InviteCreatePreview_NotYet`만 그림과 대조하면 된다. 캘린더가 쓰는 `Clock`의 시간대는 앱이 만들어질 때 고정돼서(`Clock.systemDefaultZone()`) 앱이 떠 있는 동안 기기 시간대가 바뀌면 달력 표시는 앱을 다시 켤 때까지 그대로다(Q-19 ③은 "표시는 기기 시간대 유지"라 문서와는 맞지만 후속 점검 거리)
- Docs 후속: 결정 16(기념일 카드 보조 줄·태그가 SCREENS 글에 없음), 결정 8(처음 만난 날도 직접 기념일도 없는 커플의 빈 화면), 결정 9(삭제 문구 조사), 확인 필요 1(API.md에 수정 화면의 날짜 처리)
- 묶음 D: 클라우드에서 `./gradlew`를 돌릴 수 없어 눈으로만 확인했다(PR CI 확인 필요). 시간 휠의 스냅·스크롤 값(`rememberSnapFlingBehavior`, 콘텐츠 패딩 88)과 Preview 전반(`EventEditPreview_DatePicker/_TimePicker/_ReminderSheet/_Loading`, `EventDetailPreview_Solo/_SoloDeleteDialog`, `InviteCreatePreview_Loading/_NotYet`, `InviteEnterPreview_Error/_RateLimited`, `LinkedSheetPreview_DateSet`, `SettingsPreview_NoStartDate`, `ConnectionInfoPreview_NoStartDate`, `StartDateSheetPreview_YearMonth`)는 맥에서 그림과 대조 필요
- Docs 후속(묶음 D): 결정 28(제목 24/700 토큰), 결정 30(종료 시트 제목), 결정 35(오류 줄 위치 4 → 8)
- 묶음 E(사람, 맥): Room 스키마 JSON을 만들어 커밋한다. DB 버전이 4라 `4.json`이 필요하다(이전 버전 JSON이 없으면 함께). 클라우드에서 `./gradlew`를 돌릴 수 없어 묶음 E도 눈으로만 확인했다(PR CI 확인 필요).
