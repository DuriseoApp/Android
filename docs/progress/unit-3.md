# 단위 3: 커플

> 브랜치: `feat/3-couple` · PR: #10(초안) · 상태: 구현 1부 완료(CI 313개 통과, 커밋 a84ad1b), **구현 2부 완료(PRD v0.9 반영, 빌드·테스트는 CI 확인 대기)**. 남은 것은 아래 "남은 일"
> 기준: PRD v0.9 (Docs main `728ea08`, Docs PR #10). 1부는 v0.8 (`afc3d1b`) 기준이었다. SCREENS.md(캔버스 버전 38, 2026-10-27 갱신), API.md(2026-10-27 갱신: 3.1 `Me`·`MeSpace`, 3.2, 4.4, 4.5, 5장, 6장), design-system.md(Android 매핑, 다이얼로그, 스낵바, 버튼), tokens.json
> 서버: Server 단위 2(커플) 머지됨, 개발 서버 배포는 사용자 확인 (아래 "서버 연동")

## 확인 필요 해결 (PRD v0.9, Docs PR #10)

확인 필요 1~18은 사용자가 추천안으로 정했고 Docs PRD v0.9(`728ea08`)로 반영됐다. 아래 표는 각 항목이 **어느 문서 위치로 정해졌는지**와 이 레포가 한 일이다. 문서와 이 기록의 추천안이 다르면 문서가 맞으므로 문서를 따랐고, 다른 곳은 "추천안과 다름"으로 표시했다.

| # | 정해진 곳 | 이 레포가 한 일 | 상태 |
| --- | --- | --- | --- |
| 1 개수 값 | D-93, API.md 3.1 `MeSpace.importCounts`(`ImportCounts{events,anniversaries,memos}`, `importPending`이 false면 null), FR-COUPLE-03 AC1·AC2, Q-13(0개도 보여 줌) | DTO·도메인·Room에 반영, 시트 문구와 가져오지 않기 다이얼로그 본문에 세 종류를 모두 보여 줌 | 완료 (서버는 Server 단위 3 PR에서 나옴. 가짜 응답으로 테스트) |
| 2 연결한 날 | D-94, API.md 3.1 `MeSpace.linkedAt`·3.2 `Space.linkedAt`, FR-SET-01 AC1 | DTO·도메인·Room, 연결 정보 "연결한 날" = `linkedAt`을 기기 시간대 날짜로. 서버가 안 주면(null) 그 행만 숨김 | 완료 |
| 3 오류·상태 문구 | D-95, SCREENS SCR-COUPLE-03 문구 표와 FR-COUPLE-02 "초대코드 오류 문구" 6줄, API.md 4.4의 12단계 대응표 | 6종 문구, 429 입력 제한(`retryAfterSeconds` 올림 분, 풀리면 문구 지우고 버튼 켬, 화면을 나가면 초기화), 오프라인, `ALREADY_COUPLED`·`GUEST_NOT_ALLOWED`는 공통 오류 + `GET /me` 재읽기. 코드 받는 중은 빈 칸 6개 | 완료. 이미지 대기 상태는 아래 표 |
| 4 COUPLE-01 뒤로 | D-96 | 앞 화면이 있을 때만 뒤로 버튼. 전체 메뉴·연결 정보 진입은 백스택을 남기는 `navigateToConnect()` | 완료 (추천안과 같음) |
| 5 연결 정보 버튼 | D-97, SCREENS "연결 정보 · 혼자 사용 중" | [초대코드 만들기] → SCR-COUPLE-02, [받은 초대코드 입력하기] → SCR-COUPLE-03. 코드 패널·해제 안내 없음 | 완료 |
| 6 연도·월 선택 | SCREENS "연도·월 선택": 이미지 예정(`SCR-SET-01--start-date-sheet-year-month.png`), **그때까지 구현은 Android가 정함(문서 밖)** | **만들지 않음.** 모양(연도 목록인지 월 격자인지 한 화면인지)을 명세 글만으로 정할 수 없다. 제목은 글자만, 이전·다음 달 버튼만 있음 | 이미지 대기 (리드에게 보고) |
| 7 COUPLE-04 날짜 칸 | D-98, FR-COUPLE-03 AC6, FR-COUPLE-04 AC4 | 칸을 누르면 날짜 시트가 한 겹 위에 뜨고 [저장]은 칸만 채움, [시작하기]에서 서버 저장, [나중에 하기]는 날짜 비움, 다시 뜨는 조건(확인한 공간 ID) | 완료 |
| 8 [내 프로필 수정]·연필 배지 | SCREENS SCR-SET-01 프로필 카드: 두 버튼이 **그대로 보인다**고 적음 → **추천안 A와 다름** | 그리지 않음. 갈 곳(SCR-SET-04)이 단위 8이라 눌러도 아무 일 없는 버튼이 된다. 카드 아래 버튼 하나는 가로 전체 | 문서와 다름 (리드에게 보고. 단위 8에서 더함) |
| 9 처음 만난 날 없는 커플 | D-99, FR-COUPLE-04 AC5. 프로필 카드는 D+N·"부터 함께하고 있어요" 줄을 숨김. 연결 정보는 "행을 숨기는지 라벨만 남기는지는 요청서 5번의 그림대로" | 프로필 카드는 문서대로. 연결 정보는 두 행을 숨김(그림 오기 전 임시) | 완료 + 연결 정보 이미지 대기 |
| 10 색 저장 시점 | D-100, SCREENS SCR-SET-03 "저장 시점", FR-CAL-04 AC2 | [완료]에서 바뀐 색만 한 번에 `PATCH /space/colors`, 성공하면 닫기, 바뀐 것 없으면 요청 없이 닫기, 실패·오프라인은 안내만, 뒤로 가기는 저장 없이 닫기 | 완료 |
| 11 되돌리기 값 | D-101, FR-CAL-04 AC7 | 나 주황·상대 파랑·함께 보라, 개인 공간은 나 주황. 저장은 [완료]에서 | 완료 |
| 12 보드 문구·샘플 | SCREENS 화면별 "보드 문구" 항목(보드 파일·행 표시), SCR-SET-03 샘플 일정과 "미리보기의 한 주는 오늘이 든 주" | 그대로 사용 | 완료 (추천안과 같음) |
| 13 공유·복사 | D-102 | 공유는 코드 6자리만, 복사는 "복사됨"으로 라벨 변경 | 완료 (1부에 이미 있음) |
| 14 토큰에 없는 수치 | SCREENS·design-system: 보드 값을 쓰되 이름 붙인 상수 + 보드 행 주석. 이미지 대기 상태는 기존 컴포넌트·토큰만 | 새 컴포넌트 `DuriseoCoupleMarks`·`DuriseoAvatar`·`DuriseoBottomSheet`의 수치는 모두 보드 값 상수. 점선 원 테두리의 점·간격(6·4)만 보드가 CSS `dashed`라 비슷하게 그린 값 | 완료 (점선 원 간격은 이미지와 맞는지 확인 필요) |
| 15 "재시작" | D-103, FR-COUPLE-05 AC3·AC5, API.md 4.5의 6단계·1단계 | 토큰·소셜 SDK는 두고 Room·설정을 지운 뒤 백스택을 SCR-COUPLE-01 하나로. `PATCH /space`·`PATCH /space/colors`·`POST /couple/import`·`DELETE /couple`의 `NOT_COUPLE`도 같은 초기화 | 완료 (추천안과 같음) |
| 16 시트 조건·저장 시점 | API.md 4.4의 8단계: 확인한 공간 ID 저장 시점 **3가지** — ① 개인 공간을 주는 모든 `GET /me`에서 그 ID ② 시트를 닫거나 [시작하기]를 마칠 때 커플 공간 ID ③ 앱 초기화 때 새 개인 공간 ID. 해제 감지는 4.5의 6단계: ID가 다르거나 Room 공간 `type`이 COUPLE→PERSONAL, **저장보다 먼저** → **추천안(시트 닫을 때만 갱신)과 다름** | 문서대로. 시트가 필요한 조건은 Room이 COUPLE이고 (`importPending` 또는 저장된 ID가 있고 현재 ID와 다름). 처음 로그인한 기기(저장값 없음)는 `importPending`만 본다 | 완료 (결정 5를 아래에서 고침) |
| 17 게스트 연결 정보 행 | SCREENS SCR-SET-01 "게스트의 연결 정보 행" → SCR-COUPLE-01(게스트 모양), FR-AUTH-04 AC2 | 게스트는 연결 정보 행과 [상대와 연결하기] 둘 다 SCR-COUPLE-01로 | 완료 |
| 18 모르는 팔레트 키 | API.md 3.2 "앱 동작", SCREENS SCR-SET-03 "모르는 팔레트 키": 오류로 보지 않고 값은 두고 기본 사람 색으로 그림 | 매퍼는 키 그대로 저장, `DuriseoAvatar`·색 점·마크가 기본 색으로 대신 그림 | 완료 |

기타 PRD v0.9에서 이 단위에 걸리는 것: Q-14(내 정보를 한 번도 못 받았을 때: 들어올 때마다 다시 불러오고 실패하면 공통 오류 스낵바, 계정 영역은 비움)는 전체 메뉴에 반영했다. 같은 항목이 계정 화면에는 단위 2에서 이미 있다.

### 디자인 반영 후 맞춤 (이미지 대기)

SCREENS가 "이미지 예정"으로 둔 7개 상태다 (COUPLE-02 코드 받는 중, COUPLE-03 오류와 입력 제한 둘, COUPLE-04 날짜를 고른 시트, SET-01 처음 만난 날 없는 메뉴와 연결 정보 둘, 연도·월 선택 = 7개를 아래 표 6줄에 담았다. COUPLE-03의 두 상태는 한 줄이다). 명세 글과 기존 컴포넌트·토큰만으로 만들었고 **새 수치를 만들지 않았다.** 이미지가 오면 모양만 맞추면 된다 (동작·문구는 문서대로라 바뀌지 않는다). 코드 주석과 Preview 이름에 "디자인 반영 후 맞춤(이미지 대기)"를 적어 두었다.

| 상태 | 이미지 예정 | 지금 모양 | 위치 / Preview |
| --- | --- | --- | --- |
| SCR-COUPLE-02 코드 받는 중 | `SCR-COUPLE-02--loading.png` | 코드 칸 6개를 비움, 문구·스피너 없음, [코드 복사]·[공유하기] 비활성(문서대로), 남은 시간 줄은 숨김(요청서 2번의 그림대로 정해질 부분) | `InviteCreateScreen` / `InviteCreateScreenLoadingPreview` |
| SCR-COUPLE-03 오류 · 입력 제한 | (오류 문구 표시 줄) | 코드 칸 아래 `danger-600` 한 줄(보드 `AddEventError` 규칙), 칸 테두리는 그대로, 입력 제한이면 [연결하고 시작하기] 비활성 | `InviteEnterScreen` / `InviteEnterScreenErrorPreview`, `...ExpiredPreview`, `...RateLimitedPreview` |
| SCR-COUPLE-04 처음 만난 날을 고른 시트 | 날짜가 채워진 필드 | 필드에 "2026년 10월 12일" 글자(`date_year_month_day`)와 달력 아이콘 | `LinkedSheetScreen` / `LinkedSheetDateSetPreview` |
| SCR-SET-01 처음 만난 날 없는 메뉴 | `SCR-SET-01--no-start-date.png` | D+N 글자와 "부터 함께하고 있어요" 줄만 빼고 "함께" 마크·[처음 만난 날 수정]은 그대로 | `SettingsScreen` / `SettingsScreenCoupleNoStartDatePreview` |
| SCR-SET-01 처음 만난 날 없는 연결 정보 | `SCR-SET-01--couple-no-start-date.png` | "처음 만난 날"·"함께한 지" 행을 숨김 | `ConnectionInfoScreen` / `ConnectionInfoScreenCoupleNoStartDatePreview` |
| 연도·월 선택 | `SCR-SET-01--start-date-sheet-year-month.png` | **만들지 않음** (확인 필요 6) | |

연도·월 선택만 명세 글로 모양을 정할 수 없어 만들지 않았다. 나머지 6개 상태는 만들었다.

단위 3은 두 사람을 잇는 단위다. 커플 연결 방법 선택(SCR-COUPLE-01), 초대코드 만들기·입력하기(SCR-COUPLE-02·03), 연결 완료 시트(SCR-COUPLE-04: 처음 만난 날, 기록 가져오기), 연결 해제·복구, 우리 색상(SCR-SET-03), 그리고 전체 메뉴(SCR-SET-01)의 커플 부분(프로필 카드, 꾸미기 행, 연결 정보 행과 하위 화면, 처음 만난 날 시트)을 만든다. 단위 2가 미뤄 둔 커플 연결 쪽 인증 AC도 여기서 잇는다.

**이 단위에서 하지 않는 것** (다른 단위 화면·기능이 있어야 하는 것)

| ID | 이유 | 단위 |
| --- | --- | --- |
| 동기화 신호 `SYNC SPACE/LINKED·UNLINKED` 수신 (FR-COUPLE-02 AC5의 "서버의 동기화 신호(FCM)", FR-COUPLE-05 AC3의 "해제 푸시 수신") | FCM·`push` 모듈이 단위 7. 서버도 SYNC 발송이 서버 단위 6 (Server PLAN 단위 2 "SYNC `LINKED` 발송은 단위 6"). 단위 3은 D-48의 5초 `GET /me` 확인, [연결 확인하기], 다음 실행 때 확인까지만 한다. **상대가 해제했을 때 앱이 켜져 있어도 단위 3에서는 다음 `GET /me`(앱 시작, 전체 메뉴·연결 정보 진입)에서야 안다** | 7 |
| 커플 연결 때 알림 권한 요청 (4.4의 11단계, D-70, FR-NOTI-01 AC1) | 권한 요청 구현이 단위 7. 문서에 "연결 때"라고만 있고 정확한 시점은 없다 (단위 7 범위 확인 때 정함) | 7 |
| SCR-CAL-01의 D-day·"처음 만난 날 입력하기 ›" 앱바 (FR-COUPLE-04 AC2), SCR-CAL-01 위에 시트를 띄우는 바탕 화면 | SCR-CAL-01은 단위 4. 단위 3의 시트는 단위 2의 캘린더 자리표시 화면 위에 뜬다 | 4 |
| 일정이 대상의 사람 색으로 보이는 것 (FR-CAL-04 AC1 뒷부분, FR-CAL-02 AC4) | 일정 화면이 단위 4. 단위 3은 사람 색을 서버 값으로 바꿔 넣는 `PersonColors` 제공까지 | 4 |
| 자동 기념일 재계산 (FR-COUPLE-04 AC1 뒷부분 "D-day와 자동 기념일이 다시 계산") | 서버가 계산(서버 단위 4), 화면은 단위 5. 단위 3은 `PATCH /space`와 `Me.space.startDate`까지 | 5 |
| SCR-SET-04 프로필 수정, 프로필 사진 | 단위 8 (확인 필요 8) | 8 |
| SCR-SET-02 알림 설정 행, 정보 섹션, 앱 정보 카드 | 단위 7·8 (단위 2 결정 그대로 그리지 않음) | 7·8 |
| FR-COUPLE-05 AC2 (30일 뒤 영구 삭제), FR-COUPLE-06 AC1의 복구 자체, 오입력 제한 계산 (NFR-SEC-03) | 서버가 한다. 앱은 응답을 보여 줄 뿐 | 서버 |

## 범위

### A. 기능·수용 기준 (PRD 6.2, 6.3 FR-CAL-04, 6.1 중 단위 2가 미룬 것)

테스트 이름은 CONVENTIONS.md "테스트" 형식. (VM) = ViewModel + 가짜 리포지토리 + Turbine, (Repo) = 가짜 remote + 메모리 Room(Robolectric), (Remote) = MockWebServer.

| ID | 내용 | 테스트 |
| --- | --- | --- |
| FR-COUPLE-01 AC1 | 개인 공간 회원이 SCR-COUPLE-02에 들어가면 `GET /invite-codes/current`(200이면 그대로, 404 `NOT_FOUND`면 `POST /invite-codes`)로 6자리 코드(대문자)·남은 시간 10분 표시, [코드 복사]·[공유하기] | (VM) `` `FR-COUPLE-01 AC1 들어오면 유효한 코드가 있으면 그대로 보여 준다`() `` · `` `FR-COUPLE-01 AC1 유효한 코드가 없으면 새 코드를 만든다`() `` · `` `FR-COUPLE-01 AC1 공유하기는 공유 시트에 코드를 넘긴다`() `` (확인 필요 13) |
| FR-COUPLE-01 AC2 | 만든 지 10분 지난 코드는 입력 쪽에서 `INVITE_EXPIRED`로 거부 (서버). 앱은 SCR-COUPLE-03 문구로 보여 줌 | (VM) 아래 FR-COUPLE-02 AC2 |
| FR-COUPLE-01 AC3 | [코드 새로 만들기] → `POST /invite-codes` → 새 코드. `reissueAvailableAt` 전에는 버튼 비활성 + "N초 뒤에 다시 만들 수 있어요". 429 `INVITE_REISSUE_TOO_SOON`이면 `retryAfterSeconds`로 같은 상태 | (VM) `` `FR-COUPLE-01 AC3 재요청이 되면 새 코드를 보여 준다`() `` · `` `FR-COUPLE-01 AC3 1분이 지나기 전에는 재요청 버튼이 꺼지고 남은 초를 보여 준다`() `` · `` `FR-COUPLE-01 AC3 429 INVITE_REISSUE_TOO_SOON이면 retryAfterSeconds만큼 기다린다`() `` · (Remote) `` `API POST invite-codes의 429는 retryAfterSeconds를 읽는다`() `` |
| FR-COUPLE-01 AC4 | 소문자 입력은 칸에 대문자로 보이고 같은 코드로 처리 (서버도 대문자로 바꿈) | (VM) `` `FR-COUPLE-01 AC4 소문자로 입력하면 대문자로 보인다`() `` · `` `FR-COUPLE-01 AC4 영문과 숫자가 아닌 글자는 넣지 않고 6자리까지만 받는다`() `` (확인 필요 3의 입력 규칙은 보드 `clean()` 그대로) |
| FR-COUPLE-01 AC5 | 남은 시간 0 → 앱이 새 코드를 받아(`current`를 먼저 확인, 404면 `POST`) 보여 주고 "시간이 지나 새 코드를 만들었어요. 새 코드를 다시 보내 주세요." 표시. 화면에 다시 들어왔을 때 만료돼 있어도 같음. 백그라운드(`STARTED` 아님)에서는 발급하지 않음 (D-42) | (VM) `` `FR-COUPLE-01 AC5 남은 시간이 0이 되면 새 코드와 안내를 보여 준다`() `` · `` `FR-COUPLE-01 AC5 다시 들어왔을 때 만료돼 있어도 새 코드를 보여 준다`() `` · `` `FR-COUPLE-01 AC5 백그라운드에서는 새 코드를 만들지 않는다`() `` |
| FR-COUPLE-02 AC1 | [연결하고 시작하기] → `POST /couple/join {code}` 200 → Room 갱신(`GET /me`) → SCR-CAL-01 + 시트(SCR-COUPLE-04). 6자리가 다 차기 전에는 버튼 비활성 | (VM) `` `FR-COUPLE-02 AC1 6자리가 차기 전에는 연결 버튼이 꺼져 있다`() `` · `` `FR-COUPLE-02 AC1 연결이 되면 캘린더로 가고 연결 완료 시트를 요청한다`() `` · (Repo) `` `FR-COUPLE-02 AC1 연결 응답 뒤 내 정보를 다시 읽어 Room에 넣는다`() `` · (Remote) `` `API POST couple join은 code를 보내고 CoupleJoinResult 세 필드를 읽는다`() `` |
| FR-COUPLE-02 AC2 | `INVITE_NOT_FOUND`·`INVITE_EXPIRED`·`INVITE_USED`·`INVITE_SELF`를 각각 다른 문구로 (만료는 "만료된 코드예요. 상대에게 새 코드를 받아 주세요."). 문구는 확인 필요 3 | (VM) `` `FR-COUPLE-02 AC2 코드 오류는 종류마다 다른 문구를 보여 준다`() `` (4코드 매개변수 테스트) · `` `FR-COUPLE-02 AC2 만료된 코드는 새 코드를 받아 달라는 문구다`() `` |
| FR-COUPLE-02 AC3 | `INVITE_UNAVAILABLE`(만든 사람이 그 사이 다른 사람과 연결) → "사용할 수 없는 코드" 계열 문구 | (VM) `` `FR-COUPLE-02 AC3 INVITE_UNAVAILABLE이면 사용할 수 없는 코드 문구를 보여 준다`() `` |
| FR-COUPLE-02 AC4 | 429 `INVITE_RATE_LIMITED` + `retryAfterSeconds` → 입력 제한 안내 (입력 막힘). 풀리는 시각 표시 형식은 확인 필요 3 | (VM) `` `FR-COUPLE-02 AC4 입력 제한이면 안내를 보여 주고 연결을 시도하지 않는다`() `` · (Remote) `` `API POST couple join의 429는 retryAfterSeconds를 읽는다`() `` |
| FR-COUPLE-02 AC5 | 코드를 만든 쪽: SCR-COUPLE-02가 열려 있는 동안(`STARTED`) 5초마다 `GET /me`, [연결 확인하기]도 같은 호출. 공간 종류가 `COUPLE`이면 SCR-CAL-01로 + 시트. 아직 개인 공간이면 스낵바 "아직 연결되지 않았어요"(바닥에서 112, 버튼 위). 앱이 꺼져 있었으면 다음 실행 때 시트 (FCM 신호는 단위 7) | (VM) `` `FR-COUPLE-02 AC5 5초마다 내 정보를 확인한다`() `` · `` `FR-COUPLE-02 AC5 공간이 커플이 되면 캘린더로 가고 시트를 요청한다`() `` · `` `FR-COUPLE-02 AC5 연결 확인하기를 누르면 바로 확인하고 아직이면 안내한다`() `` · `` `FR-COUPLE-02 AC5 화면이 보이지 않는 동안에는 확인하지 않는다`() `` · (Repo) `` `FR-COUPLE-02 AC5 앱 시작 때 공간이 달라졌으면 시트를 요청한다`() `` |
| FR-COUPLE-03 AC1 | 시트에서 [가져오기] → `POST /couple/import {import: true}` 204 | (VM) `` `FR-COUPLE-03 AC1 가져오기로 시작하면 import true를 보낸다`() `` · (Remote) `` `API POST couple import는 import 값을 보내고 204를 성공으로 본다`() `` |
| FR-COUPLE-03 AC2 | [가져오지 않기]로 시작 → 확인 다이얼로그 "혼자 쓴 기록을 삭제할까요?" [취소]/[삭제하고 시작] → `import: false` | (VM) `` `FR-COUPLE-03 AC2 가져오지 않기로 시작하면 확인 다이얼로그를 먼저 보여 준다`() `` · `` `FR-COUPLE-03 AC2 삭제하고 시작을 누르면 import false를 보낸다`() `` · `` `FR-COUPLE-03 AC2 다이얼로그에서 취소하면 아무것도 보내지 않는다`() `` |
| FR-COUPLE-03 AC3 | 각자 자기 기록만 고른다 (안내 "내 기록만 정해요. 상대의 기록은 상대가 정해요.") | Preview로 확인 |
| FR-COUPLE-03 AC4 | `importPending`이 true면 [시작하기] 전에 시트를 닫을 수 없다(밀어서 닫기·뒤로 가기 막음). 정하지 않고 앱을 끄면 다음 실행 때 다시 뜬다. 기록이 없으면(`importPending` false) 닫기 버튼으로 닫을 수 있다 | (VM) `` `FR-COUPLE-03 AC4 가져올 기록이 있으면 시작하기 전에는 시트를 닫을 수 없다`() `` · `` `FR-COUPLE-03 AC4 기록이 없으면 닫을 수 있다`() `` · (Repo) `` `FR-COUPLE-03 AC4 importPending이 true면 다음 실행 때 시트를 다시 요청한다`() `` |
| FR-COUPLE-03 AC5 | 시트를 처음 열면 [가져오기] 미리 선택 (D-76) | (VM) `` `FR-COUPLE-03 AC5 시트를 처음 열면 가져오기가 선택돼 있다`() `` |
| FR-COUPLE-04 AC1 (앱 부분) | 시트 또는 전체 메뉴 프로필 카드·연결 정보에서 `PATCH /space {startDate}` → 응답 `Space`로 Room 갱신 → 두 사람 화면에 반영(상대는 다음 `GET /me`). 지우기 없음(D-84) | (VM) `` `FR-COUPLE-04 AC1 처음 만난 날을 저장하면 PATCH space를 보내고 내 정보에 반영한다`() `` · (Repo) `` `FR-COUPLE-04 AC1 PATCH space 응답으로 Room의 처음 만난 날이 바뀐다`() `` · (Remote) `` `API PATCH space는 startDate만 보내고 Space를 읽는다`() `` · (VM) `` `FR-COUPLE-04 AC1 고른 날짜를 지우는 동작은 없다`() `` |
| FR-COUPLE-04 AC2 | SCR-CAL-01 입력 안내 | 단위 4 |
| FR-COUPLE-04 AC3 | 날짜는 1950-01-01 ~ 오늘만 고를 수 있다 (범위 밖 비활성). 건너뛰기 가능 ([나중에 하기]·고르지 않고 시작) | (VM) `` `FR-COUPLE-04 AC3 오늘보다 뒤 날짜는 고를 수 없다`() `` · `` `FR-COUPLE-04 AC3 1950년 1월 1일보다 앞 날짜는 고를 수 없다`() `` · `` `FR-COUPLE-04 AC3 처음 만난 날을 고르지 않고 시작해도 된다`() `` |
| FR-COUPLE-05 AC1 | 연결 정보의 [연결 해제] → 다이얼로그 "연결을 해제할까요?"(본문에 상대 닉네임) [취소]/[연결 해제] → `DELETE /couple` 204 → 앱 초기화 → SCR-COUPLE-01 | (VM) `` `FR-COUPLE-05 AC1 연결 해제 다이얼로그 본문에 상대 이름이 들어간다`() `` · `` `FR-COUPLE-05 AC1 연결 해제를 확인하면 DELETE couple을 보내고 앱을 초기화한다`() `` · `` `FR-COUPLE-05 AC1 취소하면 아무것도 보내지 않는다`() `` · `` `NFR-OFF-02 오프라인이면 연결 해제 요청을 보내지 않고 안내한다`() `` · (Remote) `` `API DELETE couple은 204를 성공으로 본다`() `` |
| FR-COUPLE-05 AC3 | 해제를 알면(해제 직후, 다음 실행 때 `GET /me`의 공간 ID가 저장값과 다름. 409 `NOT_COUPLE`도 이미 해제된 것) 토큰을 뺀 로컬 데이터를 지우고 SCR-COUPLE-01로. 푸시 수신 경로는 단위 7 | (Repo) `` `FR-COUPLE-05 AC3 해제하면 토큰은 남기고 Room과 설정을 지운다`() `` · `` `FR-COUPLE-05 AC3 NOT_COUPLE이면 이미 해제된 것으로 보고 초기화한다`() `` · `` `FR-COUPLE-05 AC3 다음 실행 때 공간이 커플에서 개인으로 바뀌었으면 초기화한다`() `` · (VM, `feature:main`) `` `FR-COUPLE-05 AC3 초기화가 끝나면 커플 연결 화면으로 간다`() `` |
| FR-COUPLE-05 AC4 | 가져오기를 정하기 전에 연결이 해제되면 혼자 쓴 기록이 원래 개인 공간으로 돌아온다 — 서버 처리. 앱은 해제 뒤 `GET /me`의 `importPending`이 false인 것을 그대로 받음 | (Repo) `` `FR-COUPLE-05 AC4 해제 뒤 내 정보의 importPending이 false면 시트를 요청하지 않는다`() `` |
| FR-COUPLE-06 AC1·AC2 | 유예 기간 안 재연결: 서버가 이전 공간 복구(`restored: true`). 앱은 같은 시트를 쓰고 `importPending`(해제 후 새로 쓴 기록)으로만 가져오기를 고른다. 복구 시트의 별도 디자인·문구 없음 | (Repo) `` `FR-COUPLE-06 AC2 복구된 연결도 importPending이 true면 가져오기를 고른다`() `` · (Remote) `` `API POST couple join은 restored true를 읽는다`() `` |
| FR-CAL-04 AC1 | 커플 공간은 사람 색 3개(나·상대·함께), 개인 공간은 나 하나. `Me.space`의 `myColor`·`partner.color`·`togetherColor`를 `PersonColors`로 바꿔 `DuriseoTheme`에 넣는다 | (VM) `` `FR-CAL-04 AC1 내 정보의 색으로 PersonColors를 만든다`() `` · `` `FR-CAL-04 AC1 개인 공간은 나 색만 쓴다`() `` · (Repo) `` `FR-CAL-04 AC1 팔레트 키를 색으로 바꾼다`() `` (모르는 키는 확인 필요 18) |
| FR-CAL-04 AC2 | SCR-SET-03에서 12색 중 나·상대·함께 색 선택 → `PATCH /space/colors {members:[{userId,color}], together}` → 응답 `Space`로 Room 갱신. 화면은 보는 사람 기준 나·상대·함께 라벨. 저장 시점은 확인 필요 10 | (VM) `` `FR-CAL-04 AC2 완료를 누르면 바꾼 색만 PATCH space colors로 보낸다`() `` · `` `FR-CAL-04 AC2 나 색은 내 userId로 상대 색은 상대 userId로 보낸다`() `` · `` `FR-CAL-04 AC2 바꾼 것이 없으면 요청하지 않는다`() `` · `` `NFR-OFF-02 오프라인이면 색 저장 요청을 보내지 않고 안내한다`() `` · (Remote) `` `API PATCH space colors는 바뀐 필드만 보내고 Space를 읽는다`() `` · (Repo) `` `FR-CAL-04 AC2 PATCH space colors 응답으로 Room의 색이 바뀐다`() `` |
| FR-CAL-04 AC3 | 기본값 나/만든 사람 주황, 입력한 사람 파랑, 함께 보라 — 서버가 정함. [기본 색으로 되돌리기]의 값은 확인 필요 11 | (VM) `` `FR-CAL-04 AC3 기본 색으로 되돌리기는 주황 파랑 보라로 돌린다`() `` |
| FR-CAL-04 AC4 | 두 대상이 같은 색이면 저장은 되고 경고 배너("같은 색을 쓰는 대상이 있어요. 캘린더에서 누구 일정인지 구분하기 어려울 수 있어요.") | (VM) `` `FR-CAL-04 AC4 같은 색이면 경고를 보여 주고 저장은 막지 않는다`() `` · `` `FR-CAL-04 AC4 개인 공간에는 경고가 없다`() `` |
| FR-CAL-04 AC5 | 개인 공간: "바꿀 대상"은 [나]만, 안내 "연결하면 상대와 함께 색도 고를 수 있어요.", 기본 주황 | (VM) `` `FR-CAL-04 AC5 개인 공간에서는 바꿀 대상이 나뿐이다`() `` · `` `FR-CAL-04 AC5 개인 공간에서는 together와 상대 색을 보내지 않는다`() `` |
| FR-AUTH-02 AC1 (단위 2에서 미룸) | 신규 소셜 회원 `isNewUser: true` → SCR-COUPLE-01 (D-31). 단위 2의 `AfterLogin.NewMember`를 `feature:main`이 SCR-COUPLE-01로 연결하고, `ImportPending`은 캘린더 + 시트로 연결 | (VM, `feature:main`) `` `FR-AUTH-02 AC1 신규 회원은 로그인 뒤 커플 연결 화면으로 간다`() `` · `` `FR-AUTH-02 AC1 가져올 기록이 남은 기존 회원은 캘린더에서 시트를 요청한다`() `` |
| FR-AUTH-03 AC4 | 게스트가 SCR-COUPLE-01(게스트)의 [로그인하고 연결하기] → 계정 화면 → 연동 후 SCR-COUPLE-01 회원 화면으로 돌아옴 (D-79). 이때 계정 화면의 "연동 성공 안내"는 보이지 않음 | (VM) `` `FR-AUTH-03 AC4 연결하려고 연동했으면 커플 연결 화면으로 돌아간다`() `` · `` `FR-AUTH-03 AC4 연결하려고 연동하면 성공 안내를 보여 주지 않는다`() `` |
| FR-AUTH-04 AC2 | 게스트가 커플 연결 메뉴를 누르면 연결 화면 대신 "연결하려면 로그인이 필요해요" 안내 + [로그인하고 연결하기] (SCR-COUPLE-01 게스트). 게스트 화면에는 "다른 계정으로 로그인"이 없음. 게스트 [혼자 계속 쓰기] → 캘린더 | (VM) `` `FR-AUTH-04 AC2 게스트에게는 카드 대신 로그인 안내가 보인다`() `` · `` `FR-AUTH-04 AC2 게스트에게는 다른 계정으로 로그인이 없다`() `` · `` `FR-AUTH-04 AC2 혼자 계속 쓰기를 누르면 캘린더로 간다`() `` |
| FR-AUTH-05 AC3 | 회원 SCR-COUPLE-01의 "다른 계정으로 로그인" → 로그아웃 확인 다이얼로그(`LogoutConfirmDialog`, 단위 2) → `AuthRepository.logout()` → SCR-START-01 | (VM) `` `FR-AUTH-05 AC3 다른 계정으로 로그인을 누르면 로그아웃 확인을 보여 준다`() `` · `` `FR-AUTH-05 AC3 확인하면 로그아웃하고 시작 화면으로 간다`() `` · `` `NFR-OFF-02 오프라인이면 다른 계정으로 로그인의 로그아웃 요청을 보내지 않고 안내한다`() `` |
| FR-COUPLE-01~02 (회원 SCR-COUPLE-01) | [초대코드 만들기]→02, [초대코드 입력하기]→03, [혼자 시작하기]→SCR-CAL-01 | (VM) `` `FR-COUPLE-01 회원에게는 만들기와 입력하기와 혼자 시작하기가 보인다`() `` · `` `FR-COUPLE-01 혼자 시작하기를 누르면 캘린더로 간다`() `` |
| FR-SET-01 AC1 (커플 부분) | 전체 메뉴의 프로필 카드(커플: 나·너 아바타, 이름, D+N, 처음 만난 날, [처음 만난 날 수정] / 개인·게스트: 나 + "아직 없음" + [상대와 연결하기]), 꾸미기 › 우리 색상(색 점 3개), 설정 › 연결 정보(연결됨/혼자 사용 중) → 연결 정보 화면 | (VM) `` `FR-SET-01 AC1 커플이면 프로필 카드에 두 사람과 함께한 날이 보인다`() `` · `` `FR-SET-01 AC1 혼자면 아직 없음과 상대와 연결하기가 보인다`() `` · `` `FR-SET-01 AC1 연결 정보 행은 연결됨 또는 혼자 사용 중을 보여 준다`() `` · `` `FR-SET-01 AC1 상대와 연결하기는 커플 연결 화면으로 간다`() `` · `` `FR-SET-01 AC1 게스트의 연결 정보는 커플 연결 게스트 화면으로 간다`() `` (확인 필요 17) |
| FR-SET-01 (연결 정보 화면) | 연결 상태 카드, 연결한 날(확인 필요 2)·처음 만난 날·함께한 지·함께 쓰는 것, 안내 문구, [연결 해제] / 혼자 사용 중: [초대코드 만들기]·[받은 초대코드 입력하기] (확인 필요 5) | (VM) `` `FR-SET-01 연결 정보는 처음 만난 날과 함께한 지를 보여 준다`() `` · `` `FR-SET-01 혼자 사용 중이면 초대코드 만들기와 입력하기가 보인다`() `` |
| NFR-OFF-02 | 초대코드 만들기·재요청·입력(연결)·가져오기 결정·처음 만난 날 저장·색 저장·연결 해제는 쓰기. `runOnlineWrite`로 감싸고 오프라인이면 요청 없이 스낵바 "인터넷 연결이 필요해요" (읽기인 `current`·`GET /me` 확인의 오프라인은 확인 필요 3) | `` `NFR-OFF-02 오프라인이면 초대코드 만들기 요청을 보내지 않고 안내한다`() `` · `` `NFR-OFF-02 오프라인이면 연결 요청을 보내지 않고 안내한다`() `` · `` `NFR-OFF-02 오프라인이면 가져오기 결정을 보내지 않고 안내한다`() `` · `` `NFR-OFF-02 오프라인이면 처음 만난 날 저장 요청을 보내지 않고 안내한다`() `` |
| D-88 | 서버 오류(`INTERNAL_ERROR`)·모르는 오류는 공통 오류 스낵바, 화면·상태 그대로 (SCREENS 공통 표) | (VM) `` `D-88 연결 요청이 서버 오류면 공통 오류를 보여 주고 화면은 그대로다`() `` |

### B. 화면과 상태 (SCREENS.md, `design/screens/`)

모든 화면 `디자인: 반영됨`(캔버스 버전 38). `변경 요청됨`인 화면은 이 단위에 없다 (SCR-SET-01 `변경 요청됨 (PRD v0.8)`은 계정 화면 이미지 3장으로 단위 2 범위). 이미지가 없는 상태는 "디자인 없음"으로 표시.

| 화면 | 상태 | 이미지 / 보드 | Preview |
| --- | --- | --- | --- |
| SCR-COUPLE-01 (`feature:couple`) | 회원: 뒤로, 두 원 그림, 제목, 설명, [초대코드 만들기](브랜드 카드)·[초대코드 입력하기](면 카드)·[혼자 시작하기](아웃라인 행), 하단 "다른 계정으로 로그인" | `SCR-COUPLE-01.png` / `Connect.dc.html` | `CoupleConnectScreenMemberPreview` |
| | 게스트: 안내 카드 "연결하려면 로그인이 필요해요", [로그인하고 연결하기], [혼자 계속 쓰기], "다른 계정으로 로그인" 없음 | `SCR-COUPLE-01--guest.png` / `ConnectGuest.dc.html` | `CoupleConnectScreenGuestPreview` |
| | 로그아웃 확인 다이얼로그 (단위 2 `LogoutConfirmDialog`) | `SCR-SET-01--logout-dialog.png`(재사용) | `CoupleConnectScreenLogoutDialogPreview` |
| | 요청 중(로그아웃): 버튼 비활성만 (SCREENS 공통 "요청 중") | 디자인 없음 (공통 규칙) | `CoupleConnectScreenLoggingOutPreview` |
| SCR-COUPLE-02 | 기본: 코드 6칸(IBM Plex Mono 26), "10분 동안 유효 · N분 N초 남음", [코드 복사][공유하기], "상대방을 기다리는 중이에요", [코드 새로 만들기], [연결 확인하기] | `SCR-COUPLE-02.png` / `InviteCreate.dc.html` | `InviteCreateScreenPreview` |
| | 재요청 대기: 버튼 비활성 + "N초 뒤에 다시 만들 수 있어요" | `SCR-COUPLE-02--cooldown.png` / `InviteCreateCooldown.dc.html` | `InviteCreateScreenCooldownPreview` |
| | 만료 후 자동 재발급: 코드 아래 안내 "시간이 지나 새 코드를 만들었어요. 새 코드를 다시 보내 주세요." | `SCR-COUPLE-02--renewed.png` / `InviteCreateRenewed.dc.html` | `InviteCreateScreenRenewedPreview` |
| | 아직 연결 전: 스낵바 "아직 연결되지 않았어요" (바닥에서 112) | `SCR-COUPLE-02--not-yet.png` / `InviteCreateNotYet.dc.html` | `InviteCreateScreenNotYetPreview` |
| | 코드를 받는 중(첫 진입): 문서에 없음 (확인 필요 3) | 디자인 없음 | `InviteCreateScreenLoadingPreview` (승인 시) |
| SCR-COUPLE-03 | 입력 전: 코드 칸 6개(현재 칸 `brand-500` 2px), [붙여넣기], [연결하고 시작하기] 비활성 | `SCR-COUPLE-03.png` / `InviteEnter.dc.html` | `InviteEnterScreenPreview` |
| | 6자리 입력: 버튼 활성 | `SCR-COUPLE-03--filled.png` / `InviteEnterFilled.dc.html` | `InviteEnterScreenFilledPreview` |
| | 오류 6종 + 입력 제한 + 요청 중: **디자인 없음, 문구 없음** (확인 필요 3) | 없음 | 승인 시 `InviteEnterScreenErrorPreview` |
| SCR-COUPLE-04 (`feature:couple`, 호스트는 `feature:main`) | 혼자 쓴 기록 있음: 제목 "[상대 이름]님과 연결됐어요", 처음 만난 날 필드 + [나중에 하기], 혼자 쓴 기록 카드 2장([가져오기] 미리 선택), [시작하기]. 닫을 수 없음 | `SCR-COUPLE-04.png` / `Calendar.dc.html` `linkedSheet` | `CoupleLinkedSheetWithRecordsPreview` |
| | 혼자 쓴 기록 없음: 기록 영역 없음, 제목 오른쪽 닫기 버튼 | `SCR-COUPLE-04--no-records.png` / `CalendarLinkedSheetNoRecords.dc.html` | `CoupleLinkedSheetNoRecordsPreview` |
| | 가져오지 않기 확인 다이얼로그 "혼자 쓴 기록을 삭제할까요?" [취소][삭제하고 시작] | `SCR-COUPLE-04--drop-dialog.png` / `CalendarLinkedSheetDialog.dc.html` | `CoupleLinkedSheetDropDialogPreview` |
| | 처음 만난 날 고름(필드에 날짜) / 복구(`restored`) / 요청 중: 디자인 없음 (확인 필요 7) | 없음 | 승인 시 |
| 처음 만난 날 선택 시트 (공용: SCR-COUPLE-04·SCR-SET-01 [처음 만난 날 수정], 이후 SCR-CAL-01·SCR-ANNIV-01에서 같은 시트 — `core:designsystem` 컴포넌트) | 월 달력, 연·월 제목, 이전·다음 달, 미래 날짜 비활성, 안내 "오늘 이후 날짜는 고를 수 없어요. 1950년 1월부터 고를 수 있어요.", [저장]. 연·월 제목을 누르면 연도·월 선택은 **디자인 없음** (확인 필요 6) | `SCR-SET-01--start-date-sheet.png` / `MenuStartDateSheet.dc.html`(→ `StartDateSheet.dc.html`) | `StartDatePickerSheetPreview` |
| SCR-SET-03 우리 색상 (`feature:settings`) | 커플: 미리보기(한 주 달력 + 일정 3개), 바꿀 대상 [나\|상대\|함께], 12색, "바꾼 색상은 두 사람 화면에 똑같이 적용돼요.", [기본 색으로 되돌리기] | `SCR-SET-03.png` / `ColorSettings.dc.html` | `ColorSettingsScreenCouplePreview` |
| | 같은 색 경고 배너 | 이미지 없음, 보드 `hasDup` 상태 (`ColorSettings.dc.html` 72~74행) | `ColorSettingsScreenSameColorPreview` |
| | 개인 공간: [나]만, 안내 "연결하면 상대와 함께 색도 고를 수 있어요." | `SCR-SET-03--solo.png` / `ColorSettingsSolo.dc.html` | `ColorSettingsScreenSoloPreview` |
| SCR-SET-01 전체 메뉴 — 커플 부분 (`feature:settings`, 단위 2 화면에 더함) | 커플: 프로필 카드(나·너, D+N, [처음 만난 날 수정]), 꾸미기 › 우리 색상 행, 설정 › 연결 정보 행(연결됨) | `SCR-SET-01.png` / `Menu.dc.html` | `SettingsScreenCouplePreview` (단위 2 `SettingsScreenMemberPreview`를 바꿈) |
| | 혼자(회원): 프로필 카드 "아직 없음" + [상대와 연결하기], 연결 정보 행 "혼자 사용 중" | `SCR-SET-01--solo.png` / `MenuSolo.dc.html` | `SettingsScreenSoloPreview` |
| | 게스트: 배너 + 프로필 카드(혼자 모양) + 연결 정보 행 | `SCR-SET-01--guest.png` / `MenuGuest.dc.html` | `SettingsScreenGuestPreview` (단위 2 것을 바꿈) |
| | 처음 만난 날 시트가 뜬 상태 | `SCR-SET-01--start-date-sheet.png` | `SettingsScreenStartDateSheetPreview` |
| | 처음 만난 날이 없는 커플의 프로필 카드: 디자인 없음 (확인 필요 9) | 없음 | 승인 시 |
| SCR-SET-01 연결 정보 (하위 화면) | 커플: 연결 상태 카드(나·너, "연결됨"), 연결한 날·처음 만난 날·함께한 지·함께 쓰는 것, 안내, [연결 해제] | `SCR-SET-01--couple.png` / `ConnectionInfo.dc.html` | `ConnectionInfoScreenCouplePreview` |
| | 혼자 사용 중: 카드("아직 없음", "혼자 사용 중"), 안내, [초대코드 만들기], [받은 초대코드 입력하기] | `SCR-SET-01--couple-solo.png` / `ConnectionInfoSolo.dc.html` | `ConnectionInfoScreenSoloPreview` |
| | 연결 해제 확인 다이얼로그 | `SCR-SET-01--unlink-dialog.png` / `ConnectionInfoDialog.dc.html` | `ConnectionInfoScreenUnlinkDialogPreview` |

공용 컴포넌트 (`core:designsystem`, 각각 `@Preview`): `DuriseoAvatar`(나·너·둘, 사람 색 + 휘도 글자색, 크기 28·36·56·72), `DuriseoCodeBoxes`(초대코드 칸, `code-lg`/`code-md` — 모노 글꼴 `font.mono`는 design-system에 정의돼 있고 코드에 아직 없으면 `/sync-tokens`로 가져옴), `StartDatePickerSheet`, `DuriseoBottomSheet`(위 모서리 24·손잡이, 흰 면 `surface-0`, design-system "바텀시트"), 안 읽음 점 등 단위 4 이후 것은 만들지 않는다. 기존 `DuriseoButton`·`DuriseoDialog`·`DuriseoDialogLayer`·`LogoutConfirmDialog`·`DuriseoTopAppBar`·`DuriseoGuestBanner`·`DuriseoMenuRow`·`DuriseoSnackbar`를 재사용한다. **보드에 있는데 design-system 컴포넌트 명세와 토큰이 없는 수치**(코드 칸 42×56/58, 큰 카드 136, 아바타 72, 두 원 그림 164×56, 시트 안쪽 여백 등)는 단위 2처럼 "보드 값으로 상수화 + 코드 주석에 출처 행" 방식으로 가고 Docs 후속에 적는다 (확인 필요 14).

문구 (SCREENS.md 그대로 `strings.xml`): SCR-COUPLE-01 — "상대방과 연결해 주세요", "초대코드 만들기", "초대코드 입력하기", "혼자 시작하기", "연결은 나중에 메뉴 › 연결 정보에서 할 수 있어요", "다른 계정으로 로그인", "연결하려면 로그인이 필요해요", "로그인하고 연결하기", "혼자 계속 쓰기". SCR-COUPLE-02 — "초대코드를 상대에게 보내주세요", "10분 동안 유효 · N분 N초 남음", "코드 복사", "공유하기", "상대방을 기다리는 중이에요", "연결되면 자동으로 캘린더로 넘어가요", "코드 새로 만들기", "N초 뒤에 다시 만들 수 있어요", "시간이 지나 새 코드를 만들었어요. 새 코드를 다시 보내 주세요.", "연결 확인하기", "아직 연결되지 않았어요". SCR-COUPLE-03 — "받은 초대코드를 입력해 주세요", "영문과 숫자 6자리예요.", "붙여넣기", "연결하고 시작하기", "만료된 코드예요. 상대에게 새 코드를 받아 주세요.". SCR-COUPLE-04 — "[상대 이름]님과 연결됐어요", "처음 만난 날", "날짜를 골라 주세요", "D-day와 기념일이 이 날을 1일로 계산돼요. 둘 중 누구든 나중에 바꿀 수 있어요.", "나중에 하기", "혼자 쓴 기록", "가져오기", "가져오지 않기", "내 기록만 정해요. 상대의 기록은 상대가 정해요.", "시작하기", "혼자 쓴 기록을 삭제할까요?", "취소", "삭제하고 시작". SCR-SET-03 — "우리 색상", "완료", "바꿀 대상", "<대상> 색상 고르기", 같은 색 경고, "바꾼 색상은 두 사람 화면에 똑같이 적용돼요.", "연결하면 상대와 함께 색도 고를 수 있어요.", "기본 색으로 되돌리기". SCR-SET-01 — "꾸미기", "연결 정보", "연결됨", "혼자 사용 중", "아직 없음", "상대와 연결하기", "처음 만난 날 수정", 연결 해제 다이얼로그("연결을 해제할까요?" + 본문, "취소", "연결 해제"), 처음 만난 날 시트("처음 만난 날", "D-day와 기념일이 이 날을 1일로 계산돼요.", "오늘 이후 날짜는 …", "저장"), 연결 정보 안내("연결을 해제하면 두 사람 모두 빈 화면에서 다시 시작해요. …"). 보드에만 있거나 문구가 없는 곳은 확인 필요 3·12.

### C. API (API.md 3.2, 4.4, 4.5, 5장)

경로는 모두 `/api/v1` 아래. 모두 인증 필요(`@Authed`, 단위 2). 오류는 `ApiCaller`가 `ApiError(code, details)`로 바꾸고 화면은 `code`로만 분기한다. `ErrorCode`에는 이 단위가 쓰는 코드(`ALREADY_COUPLED`, `NOT_COUPLE`, `NO_IMPORT_PENDING`, `INVITE_*` 7종)가 **이미 모두 있어** 더하지 않았다 (구현 때 확인). `details.retryAfterSeconds`는 기존 `ErrorDetails.RetryAfter`로 읽는다.

| API | 요청 → 성공 | 오류 코드 → 앱 동작 | 테스트 (Remote) |
| --- | --- | --- | --- |
| `POST /invite-codes` | 본문 없음 → 201 `InviteCode` {`code`, `expiresAt`, `reissueAvailableAt`} | 429 `INVITE_REISSUE_TOO_SOON`(`retryAfterSeconds`) → 대기 상태(문구 있음), 403 `GUEST_NOT_ALLOWED`·409 `ALREADY_COUPLED` → 확인 필요 3, `INTERNAL_ERROR` → 공통 오류 | `` `API POST invite-codes는 본문 없이 보내고 InviteCode 세 필드를 읽는다`() `` · `` `API POST invite-codes의 429는 retryAfterSeconds를 읽는다`() `` |
| `GET /invite-codes/current` | → 200 `InviteCode`. 유효한 코드가 없으면 404 `NOT_FOUND` | 404 `NOT_FOUND` → 오류가 아니라 "코드 없음". `POST /invite-codes`를 부름 (4.4의 1단계). 409 `ALREADY_COUPLED` → 확인 필요 3 | `` `API GET invite-codes current의 404는 코드 없음으로 읽는다`() `` · `` `API GET invite-codes current는 InviteCode를 읽는다`() `` |
| `POST /couple/join` | `{code}` → 200 `CoupleJoinResult` {`spaceId`, `restored`, `importPending`}. 서버가 공백 제거·대문자 변환(앱도 보내기 전에 대문자 6자리로 만듦) | `INVITE_NOT_FOUND`(404), `INVITE_SELF`·`INVITE_USED`·`INVITE_UNAVAILABLE`·`INVITE_EXPIRED`(409), `INVITE_RATE_LIMITED`(429 + `retryAfterSeconds`), `ALREADY_COUPLED`(409)·`GUEST_NOT_ALLOWED`(403)·`VALIDATION_FAILED`(`code`) → 문구는 확인 필요 3 | `` `API POST couple join은 code를 보낸다`() `` · `` `API couple join의 오류 코드 일곱 가지를 ApiError로 바꾼다`() `` |
| `POST /couple/import` | `{import: boolean}` → 204 | 409 `NO_IMPORT_PENDING` → 이미 정한 것으로 보고 시트를 닫음(조용히), 409 `NOT_COUPLE`·`VALIDATION_FAILED`, `INTERNAL_ERROR` → 공통 오류(시트 유지) | `` `API POST couple import는 import true와 false를 보낸다`() `` · `` `API couple import의 409 NO_IMPORT_PENDING은 ApiError다`() `` |
| `PATCH /space` | `{startDate: 날짜}` → 200 `Space` {`id`, `type`, `togetherColor`, `startDate`, `members[]`{`userId`, `nickname`, `color`, `hasProfileImage`}} | 409 `NOT_COUPLE` → 해제된 것으로 보고 앱 초기화 (확인 필요 15 결정), 400 `VALIDATION_FAILED`(`startDate`)·`DATE_OUT_OF_RANGE` → 앱이 범위를 막으므로 디버그 로그만, `INTERNAL_ERROR` → 공통 오류 | `` `API PATCH space는 startDate만 보낸다`() `` · `` `API PATCH space는 Space와 members를 읽는다`() `` |
| `PATCH /space/colors` | `{members?: [{userId, color}], together?}` → 200 `Space` | 409 `NOT_COUPLE` → 앱 초기화(커플 공간에서 together·상대 색을 보냈을 때), 400 `VALIDATION_FAILED`(`INVALID` 색 키·userId) → 디버그 로그 + 공통 오류, `INTERNAL_ERROR` → 공통 오류 | `` `API PATCH space colors는 바뀐 필드만 보내고 null을 보내지 않는다`() `` · `` `API PATCH space colors는 members와 together를 함께 보낼 수 있다`() `` |
| `DELETE /couple` | 본문 없음 → 204 | 409 `NOT_COUPLE` → 이미 해제된 것으로 보고 앱 초기화 (4.5의 1단계), `INTERNAL_ERROR` → 공통 오류(그대로) | `` `API DELETE couple은 204를 성공으로 본다`() `` · `` `API DELETE couple의 409 NOT_COUPLE은 ApiError다`() `` |
| `GET /me` (단위 2) | `Me.space`가 이 단위의 읽기 원천 | 변경 없음 | 단위 2 테스트 |
| `GET /space` | → 200 `Space` | **단위 3은 부르지 않는다.** `Me.space`에 필요한 값이 모두 있다 (결정 3). 메모 작성자 닉네임용은 단위 6 | — |

- 새 DTO: `InviteCodeDto`, `CoupleJoinResultDto`, `SpaceDto`, `SpaceMemberDto`, `UpdateStartDateRequest`, `UpdateColorsRequest`(선택 필드는 `explicitNulls = false`로 필드 생략), `ImportRequest`. 날짜(`startDate`)는 `YYYY-MM-DD`, 일시는 ISO 8601 UTC(`Instant`).
- 팔레트 키는 `pick-*` 12개(`DuriseoPickPalette`, 단위 1). 서버 순서와 같다.
- 연결 응답(`CoupleJoinResult`)의 `importPending`은 쓰지 않고 바로 `GET /me`로 Room을 채운다 (화면은 Room만, `Me.space.importPending`과 같은 값).

### D. Room에 둘 데이터 (ARCHITECTURE 2장)

- **새 엔티티 없음** (결정 3). `me` 행(단위 2)이 이미 `space_id`, `space_type`, `my_color`, `partner_*`, `together_color`, `start_date`, `import_pending`을 가진다. `PATCH /space`·`PATCH /space/colors` 응답 `Space`를 `me` 행의 공간 칸에 합쳐 넣는다 (`MeLocalDataSource`에 `applySpace(space)`).
- **`연결한 날`은 어디에도 없다** → 확인 필요 2.
- **DataStore** (사용자 설정, 로그아웃·초기화 때 지워짐): `acknowledgedSpaceId`(사용자가 알아본 공간 ID. 시트를 닫거나 시작하면 갱신), 초대코드는 **저장하지 않는다**(만료·재발급이 서버 기준이고 화면 진입 때 `current`로 읽는다).
- **앱 초기화** (FR-COUPLE-05 AC3, D-37): `SessionManager.clearUserData()`(Room·설정만 지움, 토큰·소셜 SDK 상태 유지, 단위 2)를 그대로 부르고 `feature:main`이 SCR-COUPLE-01로 보냄 (확인 필요 15).
- **공간이 바뀐 것을 아는 규칙** (API.md 4.4의 8단계, 4.5의 6단계): `refreshMe()`가 `GET /me` 결과를 저장하기 **전에** Room의 이전 `space_id`와 비교한다.

  | 이전 | 새 | 일 |
  | --- | --- | --- |
  | 없음(첫 읽기) | 아무거나 | 변화로 보지 않음. 단 `importPending`이 true면 시트 요청 |
  | `acknowledgedSpaceId`와 다름, 새 `type` = COUPLE | | 시트 요청 (`SpaceChange.Linked`) |
  | 이전 `type` = COUPLE, 새 `type` = PERSONAL | | 앱 초기화 요청 (`SpaceChange.Unlinked`) |
  | 같음 | `importPending` true | 시트 요청 |

  `MeRepository`가 `spaceChanges: Flow<SpaceChange>`를 낸다 (구독자 없을 때 사라지지 않게 `SessionManager.events`와 같은 버퍼 `Channel`). `feature:main`의 `MainViewModel`이 구독한다. 비교 대상은 `Me` 캐시가 아니라 `acknowledgedSpaceId`(시트를 닫기 전 앱을 껐을 때 다시 뜨게) — 결정 5.

### E. 모듈·내비게이션

- 새 모듈 `feature:couple` (`/new-feature-module`, MODULES.md 1장 "3" → "있음"으로). `feature:main`이 의존해 NavHost에 연결. `feature:settings`는 `feature:couple`에 의존하지 않는다 (SCR-SET-01 → SCR-COUPLE-01은 `feature:main`이 연결하는 콜백).
- 경로 `core:common` `navigation`: `CoupleRouteModel.Connect`(SCR-COUPLE-01), `CoupleRouteModel.InviteCreate`(02), `CoupleRouteModel.InviteEnter`(03), `SettingsRouteModel.Colors`(SET-03), `SettingsRouteModel.ConnectionInfo`, `SettingsRouteModel.Account(fromCouple: Boolean = false)`로 변경(FR-AUTH-03 AC4, 결정 6). SCR-COUPLE-04 시트는 경로가 아니라 `feature:main`이 `MainViewModel`의 `linkedSheetRequested` 상태로 캘린더 위에 띄운다 (결정 7).
- 이동 정리:

  | 어디서 | 무엇을 누르면 | 어디로 |
  | --- | --- | --- |
  | 로그인 직후 (`AfterLogin.NewMember`) | | SCR-COUPLE-01 (뒤로 가면 앱 종료, 백스택 비움) |
  | 로그인 직후 (`ImportPending`) | | 캘린더 + SCR-COUPLE-04 |
  | SCR-COUPLE-01 | 초대코드 만들기 / 입력하기 / 혼자 시작하기 | 02 / 03 / 캘린더 |
  | SCR-COUPLE-01 (게스트) | 로그인하고 연결하기 | 계정 화면(`fromCouple = true`) → 연동 뒤 SCR-COUPLE-01 회원 (D-79) |
  | SCR-COUPLE-01 | 다른 계정으로 로그인 → 확인 | SCR-START-01 |
  | SCR-COUPLE-02·03 | 연결됨 | 캘린더 + 시트 (백스택 비움) |
  | SCR-SET-01 프로필 카드 | 상대와 연결하기 | SCR-COUPLE-01 (게스트도 같은 화면의 게스트 모양) |
  | SCR-SET-01 | 우리 색상 / 연결 정보 / 처음 만난 날 수정 | SET-03 / 연결 정보 / 시트 |
  | 연결 정보(혼자) | 초대코드 만들기 / 받은 초대코드 입력하기 | 02 / 03 (확인 필요 5) |
  | 연결 해제 확인 | 연결 해제 | 앱 초기화 후 SCR-COUPLE-01 |

## 서버 연동

| API | 개발 서버 배포 여부 | 확인 |
| --- | --- | --- |
| `POST /invite-codes`, `GET /invite-codes/current`, `POST /couple/join`, `POST /couple/import`, `GET·PATCH /space`, `PATCH /space/colors`, `DELETE /couple` | Server 단위 2 PR 완료(머지됨). 개발 서버 배포는 사용자 확인으로 "배포됨". 다만 Server `docs/PLAN.md`는 "배포 1: 대기"이고 단위 2까지 묶어 배포한다고 적혀 있어 문서상 상태와 다르다 (단위 2 확인 필요 14와 같은 상황) | 단위 테스트는 MockWebServer. 맥에서 debug 빌드 **기기 2대**(또는 에뮬레이터 + 실기기)로 확인: 게스트 → 연동 → 로그인하고 연결하기, 코드 만들기 → 5초 확인·[연결 확인하기] → 입력 쪽 연결 → 양쪽 캘린더 + 시트(가져오기·안 가져오기·처음 만난 날) → 색 변경 반영 → 연결 해제 → 양쪽 SCR-COUPLE-01 → 30일 안 재연결 복구 → 오입력 10번 429 |
| 동기화 신호 (SYNC `SPACE`) | 서버 발송은 서버 단위 6 | 단위 7 |

기기 두 대가 필요하다. 상대가 해제한 쪽 기기는 단위 7 전에는 다음 실행·`GET /me`에서야 알게 되는 점을 확인한다.

## 라이브러리 (MODULES.md 3장 기준)

새 라이브러리 없음. `lifecycle-runtime-ktx`(`repeatOnLifecycle`로 "화면이 보이는 동안만" 5초 확인·코드 만료 타이머), 공유 시트와 클립보드는 Android 프레임워크, 날짜 달력은 `core:designsystem`에 직접 그린다(Compose Material3 `DatePicker`는 디자인과 달라 쓰지 않음). `lifecycle-runtime-compose`는 단위 2처럼 쓰지 않는다.

## 결정

Android 레포 안에서 정할 수 있는 것. 리드가 범위 확인 때 함께 승인한다 (문서에 있어야 할 결정은 아래 "확인 필요"에 따로 있다).

1. **이름**: 모듈 `feature:couple`, 패키지 `com.duriseo.android.feature.couple` (`connect`, `invitecreate`, `inviteenter`, `linkedsheet`). `feature:settings`에 `colors`, `connection` 패키지 추가.
2. **쓰기 순서 (시트 [시작하기])**: ① 처음 만난 날을 골랐으면 `PATCH /space` → ② 기록 선택이 있으면 `POST /couple/import` → ③ 시트 닫기 + `acknowledgedSpaceId` 갱신. 실패하면 거기서 멈추고 공통 오류, 시트 유지(다시 누르면 처음부터. 날짜 PATCH는 같은 값이라 무해). 기록 삭제는 되돌릴 수 없으니 가장 나중에 한다. `NO_IMPORT_PENDING`은 이미 정한 것으로 보고 ③으로. `NOT_COUPLE`은 해제와 겹친 것이라 앱 초기화.
3. **`GET /space`는 부르지 않는다**: API.md 3.2 설명은 "SCR-SET-01·SCR-SET-03용"이지만 `Me.space`에 `myColor`, `partner{id,nickname,color}`, `togetherColor`, `startDate`가 모두 있어 두 번 읽을 이유가 없다. `PATCH` 응답의 `Space`를 `me` 행에 합친다. (문서와 다르게 하는 점이 아니라 쓰지 않는 것이다. PR 본문에 적는다.)
4. **코드 남은 시간·재요청 시각 기준**: 응답의 `expiresAt`·`reissueAvailableAt`을 기기 `Clock`과 비교한다 (서버 시각을 따로 받지 않음). 기기 시계가 어긋나 "만료"로 보여도 `current`를 먼저 불러 200이면 그 코드를 그대로 유지하고 몇 초 뒤 다시 본다(함부로 `POST`하지 않음). 429 `INVITE_REISSUE_TOO_SOON`이면 `retryAfterSeconds`를 따른다.
5. **`acknowledgedSpaceId`**: DataStore에 둔다. **저장 시점은 API.md 4.4의 8단계 3가지**다 (v0.9, 이 기록의 1부 추천안 "시트 닫을 때만"을 고침): ① 개인 공간을 주는 모든 `GET /me`에서 그 ID, ② 시트를 닫거나 [시작하기]를 마칠 때 커플 공간 ID, ③ 앱 초기화 때 새 개인 공간 ID(①이 초기화 뒤 `GET /me`에서 같이 한다). 그래서 시트를 닫기 전에 앱을 끄면 저장값은 개인 공간 ID 그대로라 다음 실행 때 다시 뜬다. 처음 로그인한 기기(저장값 없음)는 변화로 보지 않고 `importPending`만 본다. 해제 감지(ID 다름 또는 Room `COUPLE`→`PERSONAL`)는 항상 ①보다 먼저다.
6. **계정 화면 출처 인자**: `SettingsRouteModel.Account(fromCouple)`. `fromCouple = true`면 연동(`POST /auth/link`) 204 + `GET /me` 뒤 성공 안내 없이 `feature:main`이 SCR-COUPLE-01(회원)로 보낸다. 뒤로 가기는 SCR-COUPLE-01(게스트)로. 연동 대신 switch(기존 계정 로그인)를 하면 단위 2 그대로 그 계정의 상태로 간다.
7. **시트 호스트**: `feature:couple`이 `CoupleLinkedSheetRoute`를 공개하고 `feature:main`의 `MainScreen`이 `MainViewModel.linkedSheetRequested`가 true인 동안 캘린더 위에 그린다 (뒤로 가기는 시트가 가로챔). 새 내비게이션 경로가 아니다.
8. **앱 초기화**: 토큰·소셜 SDK 상태는 두고 Room·설정을 지운 뒤 내비게이션 백스택을 SCR-COUPLE-01 하나로 다시 만든다 (프로세스 재시작은 하지 않는다. "재시작"은 앱 상태를 처음부터 다시 만드는 뜻으로 읽는다 — 확인 필요 15 A). 그 뒤 `refreshMe()`가 새 개인 공간을 채운다.
9. **D+N 계산**: `오늘(기기 `Clock` 날짜) − startDate + 1` (FR-CAL-01 AC3, NFR-TIME-01의 "처음 만난 날 = 1일"). `core:common`에 `startDateToDday(today, startDate)` 순수 함수를 두고 단위 5가 같이 쓴다.
10. **입력 쪽 코드 입력 위젯**: 보이지 않는 `BasicTextField` + 칸 6개 그리기(보드 `InviteEnter.dc.html` 구조 그대로: 입력 `clean()` = 대문자화 + `[^A-Z0-9]` 제거 + 6자 자르기). 서버의 31자 집합(0·O·1·I·L 제외) 밖 글자는 앱이 막지 않고 서버가 `INVITE_NOT_FOUND`로 답하게 둔다 (문서에 앱 쪽 검사가 없음).
11. **붙여넣기**: 클립보드 첫 항목을 같은 `clean()`으로 넣는다. 클립보드가 비면 아무 일도 없다.
12. **요청 중**: 눌린 버튼과 같은 화면의 다른 시작 버튼을 비활성 (SCREENS 공통 "요청 중"). 문구·스피너 없음. 5초 확인과 [연결 확인하기]의 요청 중은 버튼을 막지 않는다 (조용한 읽기).
13. **"둘" 아바타**: SET-03의 "함께" 원 글자 "둘"은 design-system "사람 호칭"에 있다 (아바타 "나/너", 함께 "둘").
14. **지금 `StartViewModel`·`MainNavHost`의 임시 분기 제거**: `AfterLogin.NewMember`/`ImportPending`을 캘린더로 보내던 곳(`MainNavHost.kt` 38~40행)을 위 이동표대로 바꾼다.
15. **Room `me` 스키마 버전 2는 파괴적 재생성**: `linked_at`·`import_events`·`import_anniversaries`·`import_memos` 4칸을 더하면서 버전을 2로 올렸다. `me`는 서버 값의 캐시일 뿐이고(`GET /me`가 다시 채움) 쓰지 않은 사용자 입력이 없어서 마이그레이션 없이 `fallbackToDestructiveMigration`을 쓴다 (DatabaseModule에 이미 있음). 스키마 JSON은 아직 커밋하지 않는다 (단위 2 결정 그대로).
16. **시트 필요 여부는 사건이 아니라 파생 값**: `observeLinkedSheetNeeded()` = Room 공간이 COUPLE이고 (`importPending` 또는 (저장된 ID가 있고 현재 ID와 다름)). 한 번만 오는 이벤트로 두면 앱이 꺼졌다 켜지면 사라지므로 Room·DataStore 두 값에서 매번 계산한다. 이벤트는 해제(`SpaceChange.UNLINKED`) 하나뿐이다.
17. **`NOT_COUPLE` 처리 위치**: PATCH space·colors·import는 저장소가 초기화(`resetAfterUnlink`)하고 **실패를 그대로 돌려준다**. 화면 ViewModel은 `NOT_COUPLE`이면 아무것도 보여 주지 않고 이동하지 않는다 (초기화가 비운 백스택에서 `popBackStack`하지 않으려고). 이동은 `UNLINKED`를 받는 `feature:main`만 한다. `DELETE /couple`은 `NOT_COUPLE`도 성공으로 본다 (API.md 4.5의 1단계).
18. **입력 제한 문구**: [N]은 `ceil(retryAfterSeconds/60)` (최소 1), 오류가 난 시점에 고정. 코드를 고쳐도 제한 문구는 유지하고(다른 코드 오류는 고치면 지움) 버튼은 `retryAfterSeconds`가 지나면 켜진다. 화면을 나가면 ViewModel과 함께 사라진다.
19. **연결 완료 시트의 상태 모양**: 서버 값(`observeMe()`)과 화면 안 선택(고른 날짜, 기록 선택)을 합치고, 요청 중에는 그 시점의 값을 얼려서 보여 준다(요청 중 서버가 바뀌어도 시트가 흔들리지 않게). 시트를 마치면 화면 안 선택을 모두 지운다.
20. **날짜 시트 호스트 공용화**: 처음 만난 날 시트 면은 `StartDatePickerSheet`(designsystem), 뒤 막·위치는 `DuriseoBottomSheetLayer`가 맡아 SET-01·COUPLE-04가 같이 쓴다. 저장 요청 중에는 뒤 막·뒤로 가기로 닫히지 않는다.

## 확인 필요

문서끼리 다르거나 비어 있는 곳. 번호는 답을 기다리는 순서이고 **막힘**은 답 전에는 해당 부분을 만들 수 없다는 뜻이다. Docs 수정은 Docs 레포 `/prd-sync`·`/design-sync`로 한다. 추천안은 모두 "문서에 없는 것을 새로 만들지 않는다" 원칙에 맞춘 안이다.

1. **막힘: SCR-COUPLE-04의 "혼자 쓴 기록" 개수를 줄 값이 없음.** SCREENS 234행(시트 "개수 안내"), 보드 `Calendar.dc.html` 160행 "일정 [N]개, 기념일 [N]개, 메모 [N]개가 있어요."와 다이얼로그 본문 176행 "가져오지 않으면 혼자 쓴 일정 [N]개, 기념일 [N]개, 메모 [N]개가 삭제되고 되돌릴 수 없어요."(본문은 SCREENS 244행에 제목과 버튼만 있고 본문 없음). 그런데 API.md 3.1 `MeSpace`에는 `importPending`(boolean)뿐이고 3.2에도 개수 필드가 없다. 대기 중인 개인 공간은 활성 공간이 아니라 `GET /events` 등으로도 셀 수 없다 (R-01, 4.4의 5단계). 앱이 Room에 가진 값으로 셀 수도 없다 (기록 화면은 단위 4~6, 연결 전에 열어 본 적이 없으면 비어 있음).
   - A) `/prd-sync`로 개수를 줄 곳을 정한다: `MeSpace.importPending`을 객체로 바꾸거나 `importCounts {events, anniversaries, memos}`를 더함 + 서버 변경 (**추천**: 서버 단위 2가 이미 머지돼 있으니 서버 후속 PR이 필요하다)
   - B) 개수 없는 문구로 SCREENS·디자인을 고친다 ("혼자 쓴 기록이 있어요")
   - C) 단위 3은 개수 자리를 "—"로 두고 A를 기다림 (문구를 지어내지 않는 대안)
2. **막힘: 연결 정보의 "연결한 날"에 해당하는 필드가 없음.** SCREENS 298행·보드 `ConnectionInfo.dc.html` 72행("연결한 날 2026년 10월 12일")인데 API.md `Me`·`MeSpace`·`Space`에 연결 일시가 없다 (ERD `spaces`에도 연결 시각 칸이 없고 `space_members.joined_at`뿐). 복구된 공간은 "처음 연결한 날"인지 "다시 연결한 날"인지도 정해져 있지 않다.
   - A) `/prd-sync`로 `MeSpace`·`Space`에 `linkedAt`(일시, 복구 때는 다시 연결한 시각) 추가 + 서버 변경 (**추천**, 가입일 `createdAt`을 단위 2에서 추가한 선례)
   - B) SCREENS에서 "연결한 날" 행 삭제
   - C) 단위 3은 그 행만 숨기고 A를 기다림 (연결 정보의 나머지 3행은 만든다)
3. **디자인·문구 없음: SCR-COUPLE-01~03의 오류·상태.** SCREENS 227행은 "각각 다른 문구, 디자인 없음"이라 하고 문구는 만료 하나("만료된 코드예요. 상대에게 새 코드를 받아 주세요.")뿐이다. 나머지는 이름만 있다. 오류 표시 위치(칸 아래 문구 / 스낵바)도 없다.

   | 코드 | 상황 | 문서에 있는 것 |
   | --- | --- | --- |
   | `INVITE_NOT_FOUND`(404) | 없는·형식이 틀린 코드 | "존재하지 않는 코드" (PRD AC2 이름만) |
   | `INVITE_USED`(409) | 이미 쓴 코드 | "이미 사용된 코드" (이름만) |
   | `INVITE_SELF`(409) | 내가 만든 코드 | "자신이 만든 코드" (이름만) |
   | `INVITE_UNAVAILABLE`(409) | 만든 사람이 그 사이 연결됨 | "사용할 수 없는 코드" (PRD FR-COUPLE-02 AC3 이름만) |
   | `INVITE_RATE_LIMITED`(429) | 1시간 10회 실패 | "입력 제한 안내" (문구·풀리는 시각 형식 없음, `retryAfterSeconds`는 있음) |
   | `ALREADY_COUPLED`(409) | 로컬은 개인인데 서버는 이미 커플 (다른 기기에서 연결, 단위 7 전엔 몰랐음) | 없음 |
   | `GUEST_NOT_ALLOWED`(403) | 로컬이 낡아 게스트로 알고 있던 것 | 없음 (앱은 게스트에게 이 화면을 안 보여 줌) |
   | 오프라인 | 코드 만들기·입력(쓰기)은 "인터넷 연결이 필요해요" (NFR-OFF-02). 읽기인 `current`·5초 확인·[연결 확인하기]의 오프라인은 문서 없음 | |
   | 첫 코드 받는 중 | SCR-COUPLE-02 진입 직후 코드가 오기 전 모양 | 없음 |

   - A) `/prd-sync`로 위 문구·위치(와 `/design-sync`로 오류 모양)를 먼저 정한다 (**추천**. CONVENTIONS "없는 문구는 만들지 않는다"). 임시 제안: 칸 아래 `danger-600` 12/500 한 줄(보드 `AddEventError`의 오류 표시와 같은 규칙), `INVITE_NOT_FOUND` "없는 코드예요. 코드를 다시 확인해 주세요.", `INVITE_USED` "이미 사용된 코드예요. 상대에게 새 코드를 받아 주세요.", `INVITE_SELF` "내가 만든 코드예요. 상대에게 받은 코드를 입력해 주세요.", `INVITE_UNAVAILABLE` "사용할 수 없는 코드예요. 상대에게 새 코드를 받아 주세요.", `INVITE_RATE_LIMITED` "코드를 너무 많이 틀렸어요. N분 뒤에 다시 입력해 주세요.", 나머지 코드와 읽기 오프라인은 기존 공통 오류·"인터넷 연결이 필요해요" 재사용, 첫 코드 받는 중은 코드 칸 자리를 빈 칸으로.
   - B) 위 임시 문구를 승인해 PR 본문에 "문서에 없는 문구"로 적고 Docs에 따로 반영 요청
   - C) 단위 3은 문구가 있는 만료 하나만 구분해 보여 주고 나머지는 공통 오류
4. **SCR-COUPLE-01 뒤로 버튼이 갈 곳**: SCREENS 206행 "뒤로 버튼"만 있고, 보드 `Connect.dc.html` 20행의 뒤로 링크는 `Main.dc.html`(시작 화면)이다. 그런데 이 화면은 진입이 여럿이고 일부는 앞 화면이 없다 — 신규 회원 로그인 직후(D-31), 연결 해제 뒤 앱 초기화(D-37), 게스트 연동 뒤 복귀(D-79)는 백스택이 비어 있고, 시작 화면으로 돌려보내면 로그인한 사람이 로그인 화면을 본다. 설정(전체 메뉴·연결 정보)·프로필 카드에서 들어온 경우는 직전 화면이 있다.
   - A) 직전 화면이 있으면 그곳으로, 없는 진입(로그인 직후·해제 뒤)은 뒤로 버튼을 숨기고 시스템 뒤로 가기는 앱 종료 (**추천**, 문서 변경 없이 보드 링크를 쓰지 않음)
   - B) 앞 화면이 없는 진입은 뒤로 버튼이 [혼자 시작하기]와 같이 캘린더로 (D-31 "혼자 시작하기로 건너뛸 수 있음"과 어울림. 신규 회원이 연결을 고민하다 되돌아가는 느낌)
   - C) `/design-sync`로 진입별 모양을 먼저 정함
5. **SCR-SET-01 연결 정보 · 혼자 사용 중의 [초대코드 만들기] 동작이 보드와 다름.** SCREENS 299행은 "[초대코드 만들기], [받은 초대코드 입력하기]"만 적었다. 보드 `ConnectionInfo.dc.html` 96~111행은 [초대코드 만들기]를 누르면 **그 자리에 코드 패널**이 생기는데 그 패널이 "24시간 동안 유효 · 23시간 59분 남음"(97~110행, v0.3 값. PRD D-05는 6자리·10분)이라 현재 문서와 맞지 않는다. 같은 보드의 `justDisconnected` 안내("연결을 해제했어요. 이제 혼자 사용하고 있어요.", 29행)도 D-37(해제 후 SCR-COUPLE-01로 감)과 다르다. 이미지 `SCR-SET-01--couple-solo.png`에는 버튼 상태만 있다.
   - A) [초대코드 만들기] → SCR-COUPLE-02, [받은 초대코드 입력하기] → SCR-COUPLE-03으로 이동 (**추천**: 코드 화면을 한 곳에 두고 D-05·D-42·5초 확인을 한 번만 구현. 진입 근거 SCREENS 205행 "설정의 커플 관리"). "연결을 해제했어요" 안내는 만들지 않음
   - B) 보드대로 인라인 패널 (10분·6자리로 고쳐 구현. 5초 확인·재발급 규칙을 두 화면에 중복)
6. **디자인 없음: 처음 만난 날 시트의 "연·월 제목을 누르면 연도·월 선택".** SCREENS 321행에는 있는데 보드 `StartDateSheet.dc.html` 27행은 제목 옆 ⌄ 아이콘 버튼만 있고 연도·월 고르는 화면이 없다 (이미지에도 없음). 1950년부터 76년이라 이전·다음 달 버튼만으로는 쓰기 어렵다.
   - A) `/design-sync`로 연도·월 선택 상태를 그려 받은 뒤 구현 (**추천**. 단위 3의 시트·SET-01 처음 만난 날은 그때까지 만들되 연·월 제목 버튼은 접근성 라벨만 두고 동작 없음 — 또는 단위 3 끝에 같이)
   - B) 임시로 연도 목록 + 월 12칸 (디자인 없이, Docs에 후속 요청)
   - C) 이전·다음 달만 (연·월 선택은 단위 4 전 디자인 후)
7. **SCR-COUPLE-04 처음 만난 날 필드의 동작이 문서에 없음.** (a) "날짜를 골라 주세요" 필드를 누르면 무엇이 뜨는지(보드에 연결 없음. 같은 시트 위에 처음 만난 날 시트를 한 겹 더 띄우는 안이 자연스럽다), 시트의 [저장]이 필드에 값만 채우는지 바로 `PATCH`인지. (b) [나중에 하기]가 하는 일: 고른 날짜를 비우는지, 구역을 접는지(SCREENS 235행 "[나중에 하기]를 누르거나 고르지 않고 시작하면 입력 안내"는 결과만). (c) 이미 한쪽이 날짜를 정했거나 복구된 공간에 날짜가 있으면(`Me.space.startDate != null`) 필드에 그 날짜를 보여 주는지·[나중에 하기]를 숨기는지 (D-84 지우기 없음). (d) 복구(`restored`)·요청 중·실패 모양.
   - A) (a) 날짜 선택 시트를 위에 한 겹, 저장은 필드 채우기만, 실제 `PATCH`는 [시작하기]에서 (결정 2) (b) 고른 날짜를 비움 (c) 날짜가 이미 있으면 필드에 보여 주고 [나중에 하기]는 숨김 (d) 복구도 같은 시트, 요청 중은 공통 규칙 (**추천**, 모두 기존 규칙을 늘린 것)
   - B) `/design-sync`·`/prd-sync`로 먼저 정함
8. **SCR-SET-01 프로필 카드의 [내 프로필 수정]·연필 배지는 SCR-SET-04(단위 8)를 가리킴.** SCREENS 295·296행은 두 버튼이 그대로 보인다고 한다. 단위 3 시점엔 갈 곳이 없다.
   - A) 단위 3은 그 버튼·연필 배지를 그리지 않음 (카드에는 [처음 만난 날 수정]/[상대와 연결하기]만), 단위 8에서 더함 (**추천**, 단위 2가 계정 부분만 그린 것과 같은 방식)
   - B) 보이되 눌러도 아무 일 없음 (죽은 버튼, 비추천)
   - C) 단위 8 화면의 빈 껍데기 경로를 만들어 연결
9. **디자인·문구 없음: 처음 만난 날이 없는 커플의 SCR-SET-01 프로필 카드·연결 정보 화면.** "D+[N]"과 "[처음 만난 날]부터 함께하고 있어요", 연결 정보의 "처음 만난 날"·"함께한 지" 행 값, [처음 만난 날 수정] 버튼 문구가 날짜 없음일 때 어떻게 되는지 SCREENS에 없다 (SCR-CAL-01·SCR-ANNIV-01·SCR-COUPLE-04에만 이 상태가 있음).
   - A) 프로필 카드는 D+N·"부터 함께하고 있어요" 줄을 숨기고 버튼은 "처음 만난 날 수정" 그대로, 연결 정보는 두 행의 값 자리를 숨김 (**추천**: 새 문구 없음, `/prd-sync`에 상태 추가를 후속으로)
   - B) `/prd-sync`·`/design-sync`로 상태를 먼저 정함 (예: 버튼 "처음 만난 날 입력", 값 "정하지 않았어요")
10. **SCR-SET-03 색 저장 시점**: 보드는 색을 누르면 즉시 화면에 바뀌고 앱바에 [완료]가 있다(링크). FR-CAL-04 AC4 "저장하면"과 API는 `PATCH /space/colors` 한 번이다. SCREENS는 "완료"만 적었다. 뒤로 가기 때 바꾼 것이 있으면 어떻게 하는지도 없다.
    - A) 탭하면 화면(미리보기)만 바뀌고 [완료]에서 **바뀐 필드만** 한 번 `PATCH`, 성공하면 닫기. 실패·오프라인이면 화면 유지 + 스낵바. 뒤로 가기는 저장하지 않고 닫기(확인 없음) (**추천**)
    - B) 탭마다 `PATCH`, [완료]는 닫기만 (요청이 많고 실패 처리가 탭마다)
11. **[기본 색으로 되돌리기]의 값**: FR-CAL-04 AC3 기본값은 "초대코드를 만든 사람 주황, 입력한 사람 파랑"인데 `Me`·`Space`에는 누가 코드를 만들었는지가 없다. 보드 `reset`은 나 주황·상대 파랑·함께 보라 고정이다 (입력한 쪽도 나=주황).
    - A) 보드대로 나 주황 / 상대 파랑 / 함께 보라, 개인 공간은 나 주황 (**추천**, 문서 변경 없음. 입력한 쪽에겐 서버의 초기값과 반대일 수 있음을 PR 본문에 적음)
    - B) `/prd-sync`로 역할 필드 추가 + 서버 변경
12. **보드에만 있는 문구·샘플** (SCREENS가 요소를 가리키지만 문구가 없는 곳. 단위 2 확인 필요 5와 같은 유형). SCR-COUPLE-01: 설명 "둘 중 한 명이 초대코드를 만들고,\n다른 한 명이 입력하면 연결돼요.", 카드 부제 "코드를 만들어 상대에게 보내요"·"상대에게 받은 코드를 입력해요", 게스트 안내 본문 "게스트는 상대와 연결할 수 없어요. Google이나 카카오로 로그인하면 지금까지 쓴 기록을 그대로 두고 연결할 수 있어요.". SCR-COUPLE-02: 부제 "상대가 이 코드를 입력하면 바로 연결돼요.", 접근성 "내 초대코드"·"초대코드 K7Q29X"·"연결 상태", 복사 뒤 버튼 라벨 "복사됨". SCR-COUPLE-03: 칸 라벨 "초대코드". SCR-COUPLE-04: 카드 부제 "함께 쓰는 캘린더와 메모로 옮겨요"·"혼자 쓴 기록은 삭제돼요", 다이얼로그 본문(확인 필요 1), "혼자 쓴 기록 가져오기"(접근성), 닫기. SCR-SET-03: 미리보기 "미리보기"·샘플 일정("치과 예약"·"저녁 데이트"·"선물 리스트 정리", 14:00·19:30·"메모", 고정된 한 주 11~17일). SCR-SET-01: 프로필 카드 "아직 없음", "[처음 만난 날]부터 함께하고 있어요"(날짜 형식), 연결 정보 "연결 상태"·"함께 쓰는 것"·값 "캘린더 · 메모"·소개문 "상대를 초대하면 캘린더와 메모를 함께 쓸 수 있어요.\n혼자 쓴 기록은 연결할 때 가져올지 고를 수 있어요.", 접근성 "함께한 지 D+[N]"·"현재 색상: 나, 상대, 함께".
    - A) SCREENS가 요소를 가리키는 곳은 보드 문구를 그대로 쓰고 Docs 반영을 후속으로 (**추천**, 단위 2 결정과 같음). 날짜는 단위 2 가입일 형식("2026년 10월 12일", 요일 없음)으로, 미리보기 샘플은 문구는 보드 그대로 두고 주는 **오늘이 든 주**로
    - B) `/prd-sync`로 SCREENS에 먼저 적은 뒤 구현
13. **[공유하기]와 [코드 복사]의 동작**: SCREENS 214행에 버튼 이름뿐이다. 공유 내용(코드만? 안내 문구 포함?)이 없다. 복사 후 피드백은 보드에서 버튼 라벨이 "복사됨"으로 바뀌는 것(`InviteCreate.dc.html` 96행, 상태 유지 시간 없음)이다.
    - A) 공유 = Android 공유 시트(`ACTION_SEND`, `text/plain`)에 **코드 6자리만** 넣음(안내 문구를 지어내지 않음). 복사 = 클립보드에 코드, 라벨은 보드대로 "복사됨"으로 바뀌고 새 코드를 받거나 화면을 떠나면 원래대로 (**추천**)
    - B) `/prd-sync`로 공유 문구("[앱 이름] 초대코드: K7Q29X" 등)를 먼저 정함 (공유받는 사람이 앱 설치 안내를 못 받는 문제가 있어 출시 전에는 필요할 수 있음)
14. **토큰·design-system에 없는 수치** (단위 2 확인 필요 7과 같은 유형). 보드 값: SCR-COUPLE-01 두 원 그림 164×56(원 지름 52, 점선), 카드 최소 높이 136·안쪽 20/22·모서리 24, 카드 제목 20/700·부제 14, 혼자 시작하기 행 72·모서리 20; SCR-COUPLE-02 코드 칸 42×56·모노 26, 패널 안쪽 24/20/20·모서리 24·내부 간격 14, 상태 카드 안쪽 16/18·모서리 18, 하단 버튼 56·모서리 16(**`size-button-lg` 56 같음, 모서리 16은 `radius-md` 14와 다름**), 바닥 스낵바 112; SCR-COUPLE-03 칸 높이 58·flex 1·모노 24, 현재 칸 2px; SCR-COUPLE-04 시트 위 10·좌우 24·아래 28·모서리 24, 손잡이 36×4, 제목 22/700, 필드 높이 52·모서리 14, 카드 선택 2px `ink-900`; 공용 시트: 달력 행 40·날짜 원 36, 요일 헤더 28; SCR-SET-03 미리보기 패널 안쪽 16·모서리 20, 대상 카드 높이 112(개인 64), 색 원 44·선택 링 3+2, 아바타 28; SCR-SET-01 프로필 카드 안쪽 22/16/16·모서리 24, 아바타 72, 연필 배지 28, 연결 정보 카드 안쪽 24/16/20·아바타 56.
    - A) 단위 2처럼 보드 값을 이름 붙인 상수로 쓰고 코드 주석에 보드 파일·행을 적은 뒤 design-system 보충을 Docs 후속으로 (**추천**, 단위 2 결정 "캔버스 값으로 보충")
    - B) Docs 보충 PR(`/design-sync`)을 먼저 받고 시작 (단위 3 시작이 미뤄짐)
15. **"재시작해 자동 로그인"의 뜻** (FR-COUPLE-05 AC3, D-37, API 4.5의 6단계): 프로세스를 죽였다가 다시 띄우는 것인지, 앱 상태만 처음부터 다시 만드는 것인지 분명하지 않다. 또 `PATCH /space`·`PATCH /space/colors`·`POST /couple/import`가 `NOT_COUPLE`을 줄 때(상대가 먼저 해제) 문서가 정한 것은 `DELETE /couple`뿐이다.
    - A) 토큰·소셜 SDK 상태는 두고 Room·설정만 지운 뒤 내비게이션을 SCR-COUPLE-01 하나로 다시 만듦(프로세스 유지). `DELETE /couple`외의 `NOT_COUPLE`도 같은 초기화로 처리 (**추천**, 결정 8)
    - B) 프로세스 재시작(`Activity` 재생성 + `exitProcess` 등). Android에서 권장되지 않고 테스트하기 어려움
16. **"다음 실행 때 시트" 조건의 정의** (SCREENS 233·FR-COUPLE-02 AC5): "기기에 저장된 값과 다르면"의 저장값을 언제 갱신하는지 문서에 없다 (이 단위의 결정 5가 해석). 서버는 시트를 봤는지 저장하지 않는다 (4.4의 8단계).
    - A) 결정 5대로 시트를 닫을 때 갱신 (**추천**) / B) `GET /me`를 저장하는 즉시 갱신 (시트를 닫기 전에 앱을 끄면 기록 없는 쪽은 다시 안 뜸)
17. **게스트의 SCR-SET-01 [연결 정보] 행**: 게스트가 누르면 어디로 가는지 SCREENS에 없다 ([상대와 연결하기]는 SCR-COUPLE-01 게스트로 간다고 있음, 296행). FR-AUTH-04 AC2는 "커플 연결 메뉴를 누르면 연결 화면 대신 로그인 안내"라고 한다.
    - A) 게스트의 연결 정보 행도 SCR-COUPLE-01(게스트 모양)로 (**추천**, FR-AUTH-04 AC2·D-07) / B) 연결 정보 화면(혼자 모양)을 열고 버튼에서 막음
18. **모르는 팔레트 키** (unit-1 확인 필요 12가 단위 3으로 넘김): 서버가 `pick-*` 12개 밖의 키를 보냈을 때. 단위 2는 `Me`의 모르는 enum 값을 공통 오류로 보기로 했다.
    - A) 매퍼는 키를 그대로 저장하고, 그리는 쪽이 모르는 키면 토큰 기본색(`person-me/partner/together`)으로 대신 그리며 디버그 로그만 남김 (**추천**: 색 하나 때문에 화면 전체가 막히지 않고, 값을 지어내지 않는다)
    - B) `Me` 파싱 실패로 보고 공통 오류 (단위 2 규칙 그대로. 서버가 팔레트를 늘리면 구버전 앱이 멈춘다)

## 구현 현황 (1부: 문서로 확정된 부분)

1부는 PRD v0.9 머지 전에 **이미 문서에 있던 부분만** 만든 것이다 (CI 313개 통과, 커밋 a84ad1b). 이 절의 아래 표는 그 기록이고, 1부가 비워 둔 것은 바로 아래 "2부"에서 채웠다.

### 만든 것

| 영역 | 내용 | 테스트 |
| --- | --- | --- |
| `domain/couple` | `CoupleRepository`, `CoupleModels`, `InviteCodeInput`(6자리·대문자·영숫자), `StartDateRules`(1950-01-01 ~ 오늘, 달 이동) | `StartDateRulesTest`, `InviteCodeInputTest`, `ColorChangeTest` |
| `data:remote/couple` | `CoupleApi`(7개 경로), DTO, `CoupleRemoteDataSource`(`current` 404 `NOT_FOUND` = 코드 없음), `NetworkModule`에 `provideCoupleApi` | `CoupleRemoteDataSourceTest` (경로·본문, 429 `retryAfterSeconds`, 가입 오류 7종, import true/false, 바뀐 필드만 PATCH, 204, `NOT_COUPLE`) |
| `data:repository/couple` | `CoupleRepositoryImpl`: 쓰기는 `runOnlineWrite`, 성공 뒤 `GET /me`로 Room 갱신(갱신 실패여도 쓰기는 성공), `unlink`는 204와 `NOT_COUPLE` 모두 `clearUserData()`(토큰 유지) | `CoupleRepositoryTest` (Robolectric, `RepoHarness`에 `coupleRepository` 추가) |
| `core:common` | `CoupleRouteModel.Connect/InviteCreate/InviteEnter`, `SettingsRouteModel.Colors` | |
| `core:designsystem` | `DuriseoLargeButton`, `StartDatePickerSheet`(면만, 호스트·뒤 막 없음), `DuriseoMenuRow.trailing`, `ic_palette`, 시트 문구 | Preview: `StartDatePickerSheet` 4종, `DuriseoLargeButton` |
| `feature:couple` (새 모듈) | SCR-COUPLE-01(회원·게스트·로그아웃 확인), 02(남은 시간·재요청 대기·만료 재발급 안내·5초 확인·[연결 확인하기]·복사·공유), 03(코드 칸·붙여넣기·[연결하고 시작하기]·만료 문구) | `ConnectViewModelTest`, `InviteCreateViewModelTest`(가상 시간: FR-COUPLE-01 AC1·AC3·AC5, FR-COUPLE-02 AC5, NFR-OFF-02, D-88), `InviteEnterViewModelTest`(FR-COUPLE-01 AC4, FR-COUPLE-02 AC1·AC2 만료, NFR-OFF-02) |
| `feature:main` | `feature:couple` 연결, `AfterLogin.CoupleConnect` → SCR-COUPLE-01, `MainNavigator` 이동 함수 | |
| `feature:settings` | SCR-SET-03(미리보기·대상 카드·12색·같은 색 경고·기본 색으로 되돌리기), SCR-SET-01 "꾸미기 › 우리 색상" 행(색 점) | `ColorsViewModelTest`, `SettingsViewModelTest`에 `colorKeys` 2건 |

Preview: `ConnectScreen` 6종(`Member`·`Guest`·`NoBack`·`LogoutDialog`·`LoggingOut`·`Offline`), `InviteCreateScreen` 7종(기본·`Cooldown`·`Renewed`·`NotYet`·`Copied`·`Loading`·`Offline`), `InviteEnterScreen` 5종(기본·`Typing`·`Filled`·`Expired`·`Requesting`), `ColorsScreen` 4종(`Couple`·`SameColor`·`PartnerTarget`·`Solo`), 설정 메뉴 2종 추가, `StartDatePickerSheet` 4종.

### 2부: PRD v0.9 반영 (이 절이 1부의 "v0.9 반영 대기" 표를 대신한다)

1부가 "반영 대기"로 둔 것을 문서대로 모두 채웠다. 남은 것은 이 표 맨 아래 "아직 안 한 것"뿐이다. `./gradlew assembleDebug testDebugUnitTest`는 이 환경에 Gradle·Android SDK가 없어 **돌리지 못했다** (리드가 CI로 확인).

| 영역 | 내용 | 테스트 / Preview |
| --- | --- | --- |
| `domain` | `ImportCounts`, `MeSpace.importCounts`·`linkedAt`, `Space.linkedAt`, `SpaceChange.UNLINKED`, `MeRepository`에 `spaceChanges`·`observeLinkedSheetNeeded()`·`acknowledgeSpace()`·`resetAfterUnlink()`, `InviteRateLimit.minutesFor`(올림, 최소 1), `StartDateRules.dayNumber`, `DataError.hasCode` | `StartDateRulesTest`(dayNumber) |
| `data:remote` | `MeSpaceDto`·`SpaceDto`에 `linkedAt`·`importCounts` (없거나 null이면 null) | `MeRemoteDataSourceTest` +2 |
| `data:local` | `me` 행에 `linked_at`·`import_*` 4칸, **DB 버전 2(파괴적 재생성, 결정 15)**, `AcknowledgedSpaceLocalDataSource`(DataStore `acknowledged_space_id`) | |
| `data:repository` | `MeRepositoryImpl`: 해제 감지(ID 다름 또는 Room COUPLE→PERSONAL, **저장보다 먼저**) → 사용자 데이터 삭제(토큰 유지) → 새 내 정보 저장 → ID 저장 → `UNLINKED`; 확인한 공간 ID 저장 3곳; `CoupleRepositoryImpl`: PATCH space·colors·import의 `NOT_COUPLE`은 초기화 + 실패 반환, `unlink`는 `NOT_COUPLE`도 초기화 후 성공, `ALREADY_COUPLED`·`GUEST_NOT_ALLOWED`는 `GET /me` 재읽기 | `SpaceChangeTest`(importCounts 왕복, 저장 3곳, 시트 필요 조건, 해제 감지 ①②, NOT_COUPLE 3경로, 직접 해제는 한 번만, 재읽기) |
| `core:designsystem` | `DuriseoBottomSheet`(`DuriseoSheetPanel`·`DuriseoBottomSheetLayer`), `DuriseoAvatar`, `DuriseoCoupleMarks`(`DuriseoTogetherMark`·`DuriseoDashedLink`·`DuriseoEmptyPartnerSlot`), 아이콘 `ic_link`·`ic_calendar`(couple)·`ic_plus`, `ic_ticket_plus`·`ic_keyboard`(couple에서 옮김) | Preview: 각 컴포넌트 |
| `feature:couple` | 입력 오류 6종 문구와 입력 제한(올림 분, 풀리면 지우고 버튼 켬), 코드 받는 중(빈 칸), **SCR-COUPLE-04 연결 완료 시트**(`linkedsheet/`: 개수 문구·0개도 표시, 처음 만난 날 칸과 한 겹 위 날짜 시트, 가져오기·가져오지 않기·삭제 확인 다이얼로그, [시작하기] 순서 ①PATCH ②import ③저장, 기록 고르기 전에는 닫히지 않음) | `InviteEnterViewModelTest`(오류 6종, 제한 타이밍), `LinkedSheetViewModelTest`(FR-COUPLE-03 AC1·AC2·AC4·AC6, FR-COUPLE-04 AC4, D-93, D-98, Q-13 …) · Preview: `InviteEnterScreenErrorPreview`·`...ExpiredPreview`·`...RateLimitedPreview`, `InviteCreateScreenLoadingPreview`, `LinkedSheetWithRecordsPreview`·`NoRecordsPreview`·`DropDialogPreview`·`DatePickedPreview`·`DateSetPreview`·`DatePickerPreview`·`RequestingPreview` |
| `feature:main` | `MainViewModel`: `linkedSheetNeeded`, `spaceChanges` → `MainEffect.SpaceUnlinked` → SCR-COUPLE-01(백스택 비움). `MainScreen`이 캘린더 탭 위에 시트를 띄움. `navigateToConnect()`(백스택 유지), `navigateToConnectionInfo()` | `MainViewModelTest` +3 |
| `feature:settings` 전체 메뉴 | 우리 프로필 카드(커플·혼자·게스트, D+N, "부터 함께하고 있어요", [처음 만난 날 수정] → 날짜 시트 → `PATCH /space`, [상대와 연결하기]), 설정 › 연결 정보 행(연결됨·혼자 사용 중, 게스트는 COUPLE-01), Q-14 스낵바 | `SettingsViewModelTest` +12 (Q-14 둘, FR-SET-01 AC1 셋, D-94, D-99, FR-COUPLE-04 AC1·AC3, NFR-OFF-02, FR-COUPLE-05 AC5) · Preview: `SettingsScreenCouplePreview`·`AlonePreview`·`GuestProfilePreview`·`CoupleNoStartDatePreview`·`StartDateSheetPreview` |
| `feature:settings` 연결 정보 | `connection/`: 커플(상태 카드, 연결한 날·처음 만난 날·함께한 지·함께 쓰는 것, 안내, [연결 해제] + 확인 다이얼로그) / 혼자(소개문, [초대코드 만들기]→COUPLE-02, [받은 초대코드 입력하기]→COUPLE-03). 들어올 때 `GET /me` | `ConnectionInfoViewModelTest`(13건: FR-COUPLE-05 AC1·AC3, FR-SET-01, D-94, D-97, D-99, NFR-OFF-02) · Preview: `ConnectionInfoScreenCouplePreview`·`SoloPreview`·`UnlinkDialogPreview`·`UnlinkRequestingPreview`·`CoupleNoStartDatePreview` |
| `feature:settings` 우리 색상 | [완료]에서 바뀐 색만 `PATCH /space/colors`(내 색은 내 userId, 상대 색은 상대 userId, 함께는 `together`), 바뀐 것 없으면 요청 없이 닫기, 실패·오프라인은 안내만, `NOT_COUPLE`은 조용히, 요청 중에는 [완료]·색 고르기 막음 | `ColorsViewModelTest` +8 (FR-CAL-04 AC2·AC5·AC7, D-100, D-101, NFR-OFF-02, FR-COUPLE-05 AC5) |
| 내비게이션 | `SettingsRouteModel.ConnectionInfo`, `settingsNavGraph`에 연결 정보·연결 콜백 5개 | |

**아직 안 한 것**

| 항목 | 이유 |
| --- | --- |
| 연도·월 선택 | 이미지 대기, 모양을 명세 글로 정할 수 없음 (확인 필요 6) |
| [내 프로필 수정]·연필 배지 | SCR-SET-04가 단위 8 (확인 필요 8, 문서와 다름) |
| FR-AUTH-03 AC4 `Account(fromCouple)` 복귀 | 계정 화면의 연동 성공 뒤 복귀 흐름이 단위 2 화면을 고쳐야 하고 이 2부 범위(리드 지시)에 없었다. 게스트의 [로그인하고 연결하기]는 계정 화면 이동까지 |
| `PersonColors` → `DuriseoTheme` 연결(FR-CAL-04 AC1), SCR-CAL-01 위 D-day | 단위 4·5 |
| 동기화 신호 수신 | 단위 7 |

### 가정한 것 (PR 본문에 적을 것)

- 뒤로 버튼은 직전 화면이 없으면 숨김 (확인 필요 4 A). 공유는 코드 6자리만 (13 A). 되돌리기 값은 보드대로 나 주황·상대 파랑·함께 보라, 개인 공간은 나 주황 (11 A). 모르는 팔레트 키는 토큰 기본색 (18 A). 미리보기 주는 오늘이 든 주 (12 A).
- 스낵바 위치: SCR-COUPLE-02는 바닥에서 112(문서), SCR-COUPLE-01·03은 위쪽 40(문서에 없어 임시).
- 우리 색상 행의 색 점은 개인 공간이면 1개.
- 달력에서 비활성 날짜는 `ink-300`, 끝에서 꺼진 달 이동 버튼도 `ink-300` (보드에 모양 없음).
- 보드 수치는 이름 붙은 상수로 두고 주석에 보드 행을 적었다 (확인 필요 14 A).
- (2부) 연결 완료 시트 위 스낵바 위치는 위 40, 시트 위에 한 겹 더 뜨는 날짜 시트는 같은 뒤 막 위에 쌓는다 (문서에 위치 없음, 디자인 반영 후 맞춤).
- (2부) 연결 해제 실패·오프라인이면 다이얼로그를 닫고 안내만 한다 (로그아웃 실패와 같은 방식, D-86. SCREENS에 해제 실패 규칙이 없음). 연결은 그대로.
- (2부) 연결 정보 · 혼자 사용 중의 두 버튼은 이미지 `SCR-SET-01--couple-solo.png`(반영됨)대로 카드·행 모양이다. 아이콘 `ic_ticket_plus`·`ic_keyboard`는 두 화면이 같이 쓰도록 `core:designsystem`으로 옮겼다.
- (2부) 이름이 길면 말줄임, 글자 크기를 크게 하면 연결 정보 아래쪽 안내·버튼이 화면 밖으로 밀릴 수 있다(스크롤 없음, 보드도 고정 높이). 단위 8 접근성 점검 때 확인.
- (2부) 점선 원("아직 없음")의 점 6·간격 4는 보드가 CSS `dashed`라 브라우저가 정하는 값이다. 이미지와 맞는지 확인 필요.

## 리뷰 기록

| 회차 | Critical | Major | 비고 |
| --- | --- | --- | --- |

## 남은 일

단위 2 진행 기록 "남은 일"에서 단위 3 몫: **없음**(Room 스키마 JSON 커밋, FR-SET-02→8, 실기기 확인은 단위 2·8 몫). 단위 2가 단위 3으로 넘긴 것은 위 "하지 않는 것" 표와 A절의 FR-AUTH-02 AC1·FR-AUTH-03 AC4·FR-AUTH-04 AC2·FR-AUTH-05 AC3·`importPending`→SCR-COUPLE-04에 모두 넣었다. 단위 2 PR #9 Docs 후속 3건(design-system 여백, 게스트 ID 접근성 이름, 첫 `GET /me` 실패 상태)은 Docs 레포 몫이다.

- Room 스키마: `me` 버전 2(파괴적 재생성, 결정 15). 스키마 JSON은 단위 2 결정대로 아직 커밋하지 않는다.
- 서버: `importCounts`·`linkedAt`은 Server 단위 3 PR에서 나온다. 그 전에는 가짜 응답으로만 확인했으므로 서버가 배포되면 실기기로 연결 직후 시트의 개수와 연결 정보의 연결한 날을 확인한다.
- 이미지가 오면(디자인 반영 후 맞춤): 위 "이미지 대기" 5줄의 모양, 점선 원 간격, 연도·월 선택(새로 만듦).
- 단위 8: [내 프로필 수정]·연필 배지 추가. 단위 2 계정 화면의 `Account(fromCouple)` 복귀(FR-AUTH-03 AC4).
- 서버 단위 6(SYNC) → Android 단위 7: `LINKED`·`UNLINKED` 수신으로 5초 확인과 "다음 실행 때"를 보완한다. 알림 권한 요청 시점도 그때 정한다.
- PR 전: `docs/MODULES.md`(`feature:couple` "있음", SCR-SET-03 단위 3 표기), `docs/PLAN.md` 단위 3 "PR 완료".

## 사람이 할 일

1. (완료) 확인 필요 1~18은 PRD v0.9로 정해졌다. 위 "확인 필요 해결" 표에서 문서가 추천안과 다른 8·16과, 이미지 대기 7개를 확인한다.
2. 기기 2대 확인 준비 (두 기기에 서로 다른 Google·카카오 계정, 같은 개발 서버 주소). `local.properties` 새 키 없음.
3. 맞으면 **start**.

## 위험

1. **서버 `importCounts`·`linkedAt`이 아직 배포 전이다** (Server 단위 3 PR). 그 전까지 서버 응답에 두 필드가 없으면 시트는 개수 없이 "가져올 기록이 없는 것"으로, 연결 정보는 "연결한 날" 행 없이 보인다 (둘 다 null 처리). 서버가 나오면 실기기로 확인한다.
2. **연결·해제는 두 기기 흐름이라 JVM 테스트로 확인되지 않는 부분이 크다.** 5초 확인·`repeatOnLifecycle`·시트 뒤로 가기 차단·앱 초기화는 가짜 리포지토리 + 가상 시간으로 로직만 확인하고 맥 실기기 확인 목록을 PR에 적는다.
3. **상대가 해제했을 때 켜져 있는 앱은 단위 7 전에는 모른다.** 그때 상대 쪽 화면이 낡은 커플 공간을 보여 주고, 쓰기는 `NOT_COUPLE`/404로 실패한다. 전체 메뉴·연결 정보 진입 때 `refreshMe()`가 초기화를 부르도록 해 둔다.
4. **기기 시계 어긋남**: 코드 남은 시간과 재요청 대기는 기기 시계 기준이다 (결정 4). 심하게 어긋나면 표시가 서버와 다르지만 서버가 최종 판단(`INVITE_EXPIRED`·429)한다.
5. **D+N이 0이 되는 경우**: 처음 만난 날 검증은 요청자의 시간대 기준 "오늘"이라, 상대가 더 서쪽 시간대면 상대 기기에서 D+0이 보일 수 있다. 문서에 처리 규칙이 없어 단위 5와 함께 정한다 (확인 필요에 올릴 만큼 흔하지 않아 위험으로만 적음).
6. **카카오·Google 계정 2개 필요**: 기기 확인은 계정 2개가 있어야 한다 (게스트는 연결 불가, D-07).
