# 단위 4: 캘린더

> 브랜치: `feat/4-calendar` · PR: (링크) · 상태: **범위 확정 대기** (사람 확인 전, 코드 없음)
> 기준: PRD v0.9 (Docs main `728ea08`), SCREENS.md(캔버스 버전 38, 2026-11-01 갱신: SCR-CAL-01~03), API.md 3.3(일정), 1.1, 5장, 6장, design-system.md(FAB, 하단 탭, 스낵바, 다이얼로그), tokens.json
> 서버: Server 단위 3(캘린더: 일정, 일정 알림, 날짜 범위) 머지됨, 개발 서버 배포됨 (리드 확인, 아래 "서버 연동")

단위 4는 앱의 첫 화면과 일정이다. 월간 캘린더(SCR-CAL-01), 일정 상세(SCR-CAL-02), 일정 작성·수정(SCR-CAL-03), 일정 알림 설정(내 알림), 날짜 범위(1950-01 ~ 2050-12), 사람 색으로 보이는 일정, 처음 만난 날 D-day를 만든다. 지금 `feature:calendar`는 앱바에 전체 메뉴 버튼 하나만 있는 자리표시 화면이다.

## 범위 요약

| 항목 | 개수 |
| --- | --- |
| FR | 5개 (FR-CAL-01·02·03·05, FR-CAL-04 AC1 뒷부분) + 걸치는 AC: FR-COUPLE-04 AC2, FR-ANNIV-01 AC1·AC2의 앱바 D+N, FR-NOTI-04(보내기만), NFR-OFF-02·TIME-01·LIMIT-01·SYNC-02 |
| 화면 | 3개 (SCR-CAL-01·02·03), 이미지 15장 + 보드 문구 상태 |
| API 경로 | 7개 (`GET·POST /events`, `GET·PATCH·DELETE /events/{id}`, `PUT·DELETE /events/{id}/reminder`) |
| 확인 필요 | 14건 (아래) |

## 이 단위에서 하지 않는 것 (다른 단위에 기대는 것)

| ID | 이유 | 단위 |
| --- | --- | --- |
| FR-CAL-01 AC2의 기념일 카드·달력의 기념일 `brand-500` 고리, 앱바의 "함께한 지 D+N ›"와 개인 공간 "기념일 ›" 링크의 이동 (→ SCR-ANNIV-01) | SCR-ANNIV-01·`GET /anniversaries`가 단위 5. 한 날의 표시 최대 3개 규칙(고리 1 + 점 2)은 단위 5가 고리를 넣을 때 맞춘다 (확인 필요 4) | 5 |
| FR-CAL-01 AC2의 메모 카드("메모" 표시, → SCR-MEMO-03), 날짜 있는 메모의 달력 점, 작성 화면의 [일정\|메모] 탭 | SCR-MEMO-03·04, `GET /memos/dated`가 단위 6 (확인 필요 5) | 6 |
| FR-CAL-01 AC4 (안 읽은 알림 점, 알림 버튼 → SCR-NOTI-01) | SCR-NOTI-01, `unread-count`가 단위 7 (확인 필요 4) | 7 |
| 동기화 신호 `SYNC EVENT` 수신 (NFR-SYNC-01), 푸시·알림 목록에서 일정 상세 진입 (SCREENS 공통 "푸시 진입") | FCM·`push`가 단위 7. 서버 SYNC 발송은 서버 단위 6. 단위 4는 화면 진입·월 이동 때 `GET /events`로 다시 불러온다 (NFR-SYNC-02) | 7 |
| 일정 알림 푸시를 받는 것, 알림 권한 요청 (FR-NOTI-01 AC1, SCREENS 공통 "알림 권한": 일정 알림 설정 때) | 알림 스케줄러가 서버 단위 6, 권한 요청 구현이 앱 단위 7 (확인 필요 9) | 7 |
| FR-NOTI-04 (상대에게 푸시 보내기) | 앱은 `notifyPartner` 값을 보내기만 한다. 푸시는 서버 단위 6 | 서버 6 |
| FR-CAL-05 AC4·AC5·AC6의 알림 시각 계산·재계산·시간대 재계산 | 서버가 한다 (API.md 3.3 "알림 시각 계산"·"다시 계산"). 앱은 응답의 `reminder`를 그대로 보여 준다 | 서버 |
| 자동 기념일 재계산 화면 반영 | 단위 5 | 5 |

## 단위 3 진행 기록 "남은 일" 중 단위 4 몫

| 남은 일 (unit-3.md) | 단위 4에서 하는 일 | 상태 |
| --- | --- | --- |
| 서버 단위 3의 일시 형식: 일정 API는 `YYYY-MM-DDTHH:MM:SSZ`만 받는다 (밀리초·오프셋이면 400) | 요청 직전에 `Instant`를 초 단위로 자르고 UTC `Z`로 직렬화하는 직렬화기·테스트. 시간 일정만. 종일은 `YYYY-MM-DD` (API.md 3.3 `Event.start`) | 범위에 넣음 |
| 캘린더 앱바의 전체 메뉴 진입 (지금 임시 버튼, unit-2 확인 필요 3) | 임시 버튼을 지우고 SCREENS의 앱바(알림 버튼 옆 [전체 메뉴] 버튼, 접근성 "전체 메뉴", 보드 `Calendar.dc.html` 45행)로 정리. `CalendarRoute`의 `onOpenMenu`는 그대로 | 범위에 넣음 |
| `PersonColors` → `DuriseoTheme` 연결 (FR-CAL-04 AC1, unit-3 표 "단위 4·5") | `Me.space`의 `myColor`·`partner.color`·`togetherColor`(팔레트 키)를 `DuriseoPickPalette.colorOf`로 바꿔 `DuriseoTheme(personColors = …)`에 넣는다. 모르는 키는 기본 사람 색(단위 3 확인 필요 18 규칙). 개인 공간은 나 색만. 일정 색은 대상의 사람 색 (FR-CAL-02 AC4) | 범위에 넣음 |
| D+N 도우미 / 앱바 D-day (FR-COUPLE-04 AC2, FR-ANNIV-01 AC1, unit-3 "하지 않는 것" 표) | `StartDateRules.dayNumber`(단위 3에 이미 있음, `domain:couple`)를 쓴다. 시간대가 달라 0 이하가 되는 경우(unit-3 위험 5)는 단위 5와 함께 정하므로 이 단위는 그대로 표시하고 위험으로만 둔다 | 범위에 넣음 (도우미는 있음) |
| SCR-CAL-01 위 시트 바탕 화면 (SCR-COUPLE-04가 뜨는 캘린더) | `feature:main`이 시트를 띄우는 캘린더가 자리표시 화면에서 실제 SCR-CAL-01로 바뀐다. 시트의 호스트는 그대로 `feature:main` | 범위에 넣음 |
| 서버 `importCounts`·`linkedAt` 실기기 확인 (unit-3 위험 1) | 서버가 배포됐으니 이 단위 맥 확인 목록에 넣는다 (코드 변경 없음) | 확인 |
| 리뷰 3회차 m-7·m-8 (`acknowledgeSpace` 락, 오래된 `GET /me` 응답) | 단위 4 몫 아님. 그대로 남김 | 남김 |
| 리뷰 3회차 m-9 (연도·월 선택 접근성), 이미지 대기 6개 | 단위 4 몫 아님. 다만 날짜 선택 시트를 일정에 재사용하므로 범위 일반화 때 같은 파일을 건드린다 (확인 필요 1) | 남김 |
| 동기화 신호·알림 권한 시점 (단위 7) | 단위 7 | 남김 |

## 범위

테스트 이름은 CONVENTIONS.md "테스트" 형식. (VM) = ViewModel + 가짜 리포지토리 + Turbine, (Repo) = 가짜 remote + 메모리 Room(Robolectric), (Remote) = MockWebServer, (Domain) = 순수 JVM.

### A. 기능·수용 기준 (PRD 6.3)

| ID | 내용 | 테스트 |
| --- | --- | --- |
| FR-CAL-01 AC1 | 앱을 열면 SCR-CAL-01이 오늘이 속한 달, 오늘 날짜 선택 | (VM) `` `FR-CAL-01 AC1 처음 열면 오늘이 속한 달이 보이고 오늘이 선택돼 있다`() `` |
| FR-CAL-01 AC2 (일정 부분) | 일정이 있는 날에 사람 색 점(최대 3), 날짜를 누르면 아래 목록에 그 날 항목. 순서: 종일 일정 → 시간 일정(시작 시간순) (기념일은 앞, 메모는 뒤에 단위 5·6이 끼움). 선택한 날 제목 + "오늘" 배지 + 개수 | (VM) `` `FR-CAL-01 AC2 일정이 있는 날에 사람 색 점이 보인다`() `` · `` `FR-CAL-01 AC2 날짜를 누르면 그 날의 일정이 종일 먼저 시작 시간순으로 나온다`() `` · `` `FR-CAL-01 AC2 선택한 날에 일정이 없으면 빈 안내가 보인다`() `` · (Domain) `` `FR-CAL-01 AC2 한 날의 점은 최대 3개다`() `` |
| FR-CAL-01 AC3 · FR-ANNIV-01 AC1 | 커플이고 처음 만난 날이 있으면 앱바에 "함께한 지 D+N" (처음 만난 날 = 1일) | (VM) `` `FR-CAL-01 AC3 커플이고 처음 만난 날이 있으면 D+N이 보인다`() `` · `` `FR-ANNIV-01 AC1 처음 만난 날 당일은 D+1이다`() `` |
| FR-CAL-01 AC5 · FR-ANNIV-01 AC2 | 개인 공간: 앱바에 내 아바타, "나의 캘린더", D-day 없음 ("기념일 ›" 링크는 확인 필요 4) | (VM) `` `FR-CAL-01 AC5 개인 공간은 내 아바타와 나의 캘린더가 보인다`() `` · `` `FR-ANNIV-01 AC2 개인 공간에는 D-day가 없다`() `` |
| FR-CAL-01 AC6 | 1950년 1월 ~ 2050년 12월만 넘겨 볼 수 있다. 일정 날짜도 이 안에서만 고른다. 서버가 쓰기에서 `DATE_OUT_OF_RANGE`로 거부하고 조회는 잘라서 준다 | (Domain) `` `FR-CAL-01 AC6 1950년 1월 이전과 2050년 12월 이후로는 넘길 수 없다`() `` · `` `FR-CAL-01 AC6 날짜 선택 범위는 1950-01-01부터 2050-12-31까지다`() `` · (VM) `` `FR-CAL-01 AC6 1950년 1월에서는 이전 달 버튼이 꺼지고 2050년 12월에서는 다음 달 버튼이 꺼진다`() `` · (Repo) `` `FR-CAL-01 AC6 범위 조회는 1950-01-01 ~ 2050-12-31 안으로 잘라서 요청한다`() `` · (Remote) `` `API POST events의 DATE_OUT_OF_RANGE는 start end 필드를 읽는다`() `` |
| FR-COUPLE-04 AC2 | 커플 + 처음 만난 날 없음: 앱바 D-day 자리에 "처음 만난 날 입력하기 ›", 누르면 처음 만난 날 선택 시트 → 저장 `PATCH /space {startDate}` (단위 3 경로) | (VM) `` `FR-COUPLE-04 AC2 처음 만난 날이 없는 커플은 앱바에 입력 안내가 보인다`() `` · `` `FR-COUPLE-04 AC2 입력 안내를 누르면 처음 만난 날 시트를 연다`() `` · `` `FR-COUPLE-04 AC2 시트에서 저장하면 PATCH space를 보내고 D+N이 보인다`() `` |
| FR-CAL-02 AC1 | 제목·시작·종료를 입력하고 저장하면 `POST /events`. 제목은 앞뒤 공백을 지운 값(D-89), 공백만 있으면 제목 없음. 시작 행·종료 행이 따로, 종료 날짜를 바꿔 여러 날 | (VM) `` `FR-CAL-02 AC1 제목과 시작 종료를 넣고 저장하면 POST events를 보낸다`() `` · `` `FR-CAL-02 AC1 D-89 제목은 앞뒤 공백을 지워 보낸다`() `` · `` `FR-CAL-02 AC1 D-89 공백만 있는 제목은 저장할 수 없다`() `` · (Remote) `` `API POST events는 title isAllDay start end를 보내고 Event를 읽는다`() `` · (Domain) `` `NFR-TIME-01 시간 일정의 시작과 종료는 초 단위 UTC Z 문자열로 보낸다`() `` |
| FR-CAL-02 AC2 | 종일을 켜면 시간 없이 시작일·종료일만 (`YYYY-MM-DD`) | (VM) `` `FR-CAL-02 AC2 종일 일정은 날짜만 보낸다`() `` · `` `FR-CAL-02 AC2 하루 종일을 켜면 시작 종료 행의 시간이 사라진다`() `` |
| FR-CAL-02 AC3 | 종료 < 시작이면 저장하지 않고 오류 "종료는 시작보다 뒤여야 해요." (종료 칩 `danger-600` 테두리·글자 + 종료 행 아래 한 줄). 같으면 저장 | (VM) `` `FR-CAL-02 AC3 종료가 시작보다 이르면 저장하지 않고 오류를 보여 준다`() `` · `` `FR-CAL-02 AC3 종료가 시작과 같으면 저장한다`() `` |
| FR-CAL-02 AC4 | 대상 [나\|함께] 기본 나 (상대는 못 고름), 개인 공간은 대상 선택 없음·항상 나, 일정 메모(선택), 내 알림 기본 없음, 일정 색은 대상의 사람 색 | (VM) `` `FR-CAL-02 AC4 대상은 기본이 나이고 상대는 고를 수 없다`() `` · `` `FR-CAL-02 AC4 함께를 고르면 isTogether true를 보낸다`() `` · `` `FR-CAL-02 AC4 개인 공간에는 대상 선택이 없고 isTogether를 보내지 않는다`() `` · (Domain) `` `FR-CAL-02 AC4 일정 색은 대상의 사람 색이다`() `` (나/상대/함께/개인 4경우) |
| FR-CAL-02 AC5 | 여러 날 일정은 시작일~종료일 모든 날짜에 표시 | (VM) `` `FR-CAL-02 AC5 여러 날 일정은 걸친 모든 날에 표시된다`() `` · (Domain) `` `FR-CAL-02 AC5 시간 일정은 기기 시간대 날짜로 걸친 날을 센다`() `` |
| NFR-LIMIT-01 · D-89 | 제목 50자, 메모 1,000자(문자 단위, 코드 포인트, 지운 뒤 센다) | (VM) `` `NFR-LIMIT-01 제목은 50자까지만 받는다`() `` · `` `NFR-LIMIT-01 일정 메모는 1000자까지만 받는다`() `` (확인 필요 8) |
| FR-NOTI-04 (앱 부분) | 커플 공간 작성에서 "상대에게 알림 보내기" 스위치(기본 켜짐)를 `notifyPartner`로 보낸다. 개인 공간·수정(상대가 수정 포함)에는 스위치도 값도 없다 | (VM) `` `FR-NOTI-04 커플 공간 새 일정은 상대에게 알림 보내기가 기본 켜짐이다`() `` · `` `FR-NOTI-04 끄면 notifyPartner false를 보낸다`() `` · `` `FR-NOTI-04 수정 화면에는 상대에게 알림 보내기가 없다`() `` · `` `FR-NOTI-04 개인 공간에는 상대에게 알림 보내기가 없다`() `` |
| FR-CAL-03 AC1 · AC5 | 내가 만든 일정(대상이 나 또는 함께)은 수정·삭제 가능 | (VM) `` `FR-CAL-03 AC1 내가 만든 일정은 수정과 삭제가 보인다`() `` · (Domain) `` `FR-CAL-03 AC5 권한 매트릭스대로 수정 삭제 여부를 정한다`() `` (작성자/상대 × 나/함께 4경우) |
| FR-CAL-03 AC2 | 상대가 만든 '함께'가 아닌 일정: 수정·삭제 메뉴 없음 + 안내 "상대가 만든 일정은 상대만 고치거나 지울 수 있어요." (서버는 403 `NOT_AUTHOR`) | (VM) `` `FR-CAL-03 AC2 상대가 만든 나 일정에는 수정과 삭제가 없다`() `` · `` `FR-CAL-03 AC2 상대가 만든 일정에는 상대만 고칠 수 있다는 안내가 보인다`() `` |
| FR-CAL-03 AC3 | SCR-CAL-02에 작성자 닉네임 ("작성" 줄). `authorId`로 `Me`·`partner`에서 찾는다 (API.md `Event.authorId`) | (VM) `` `FR-CAL-03 AC3 작성자 닉네임이 보인다`() `` · `` `FR-CAL-03 AC3 상대가 만든 일정은 상대 닉네임을 보여 준다`() `` |
| FR-CAL-03 AC4 | '함께' 일정은 작성자·상대 모두 수정·삭제. 단 대상은 작성자만 바꾼다. 상대가 수정할 때는 읽기 전용 "누구의 일정 · 함께"와 "함께 일정의 대상은 만든 사람만 바꿀 수 있어요."를 보이고 `isTogether`를 보내지 않는다 (R-13) | (VM) `` `FR-CAL-03 AC4 함께 일정은 상대도 수정과 삭제가 보인다`() `` · `` `FR-CAL-03 AC4 상대가 함께 일정을 고칠 때 대상은 읽기 전용이다`() `` · `` `FR-CAL-03 AC4 상대가 고칠 때는 isTogether를 보내지 않는다`() `` · `` `FR-CAL-03 AC4 작성자는 대상을 나에서 함께로 바꿀 수 있다`() `` · (Remote) `` `API PATCH events는 바뀐 필드만 보내고 Event를 읽는다`() `` |
| FR-CAL-03 삭제 | [일정 삭제] → 다이얼로그 "일정을 삭제할까요?" [취소]/[삭제](`danger-700` 채움) → `DELETE /events/{id}` 204 → Room에서 지우고 이전 화면 (본문 문구는 확인 필요 7) | (VM) `` `FR-CAL-03 삭제를 확인하면 DELETE events를 보내고 닫는다`() `` · `` `FR-CAL-03 삭제 다이얼로그에서 취소하면 아무것도 보내지 않는다`() `` · (Repo) `` `FR-CAL-03 DELETE events 성공이면 Room에서 일정이 사라진다`() `` · (Remote) `` `API DELETE events는 204를 성공으로 본다`() `` |
| FR-CAL-05 AC1 · AC3 | 내 알림: 시간 일정 없음·정시·10분 전·1시간 전·1일 전, 종일 일정 없음·당일 오전 9시·1일 전 오전 9시. 칩을 누르면 `PUT /events/{id}/reminder {option}`(`AT_TIME`·`MIN_10`·`HOUR_1`·`DAY_1`·`ALLDAY_9AM`·`ALLDAY_PREV_9AM`), "없음"은 `DELETE …/reminder`(멱등 204). 상대의 일정에도 설정 가능 | (VM) `` `FR-CAL-05 AC1 시간 일정은 다섯 가지 선택지가 보인다`() `` · `` `FR-CAL-05 AC3 종일 일정은 세 가지 선택지가 보인다`() `` · `` `FR-CAL-05 AC1 선택지를 고르면 PUT reminder를 option 값으로 보낸다`() `` · `` `FR-CAL-05 AC1 없음을 고르면 DELETE reminder를 보낸다`() `` · `` `FR-CAL-05 AC1 상대가 만든 일정에도 내 알림을 설정할 수 있다`() `` · (Remote) `` `API PUT events reminder는 option을 보내고 Event를 읽는다`() `` · `` `API DELETE events reminder는 204를 성공으로 본다`() `` |
| FR-CAL-05 AC2 | 내 알림만 보인다 (서버가 요청자 것만 `reminder`로 줌). 알림 기본 없음(D-59) | (Repo) `` `FR-CAL-05 AC2 응답의 reminder를 그대로 Room에 저장한다`() `` · (VM) `` `FR-CAL-05 AC2 새 일정의 내 알림은 없음이다`() `` |
| FR-CAL-05 AC4 (앱 부분) | 종일 ↔ 시간 일정으로 바꿔 저장하면 응답 `reminder: null`이라 "없음"으로 보임 (새 안내 문구 없음). 일시만 바뀌면 응답의 `reminder`를 따른다 | (Repo) `` `FR-CAL-05 AC4 종일을 바꿔 저장한 응답의 reminder가 null이면 알림이 없음으로 바뀐다`() `` · (VM) `` `FR-CAL-05 AC4 수정에서 하루 종일을 바꾸면 내 알림이 없음으로 보인다`() `` |
| FR-CAL-05 AC5 (앱 부분) | 알림 시각이 이미 지났어도(끝난 일정 포함) 설정은 저장되고 선택은 유지된다. `fireAt`이 과거여도 오류로 보지 않는다 | (Repo) `` `FR-CAL-05 AC5 fireAt이 과거여도 알림 설정을 그대로 보여 준다`() `` |
| NFR-OFF-01 · NFR-SYNC-02 | 화면은 Room만 구독. 화면에 들어갈 때·월을 넘길 때 그 월의 `GET /events`(`from`=1일, `to`=말일, 최대 3개월). 받으면 그 범위에 걸친 일정을 Room에서 지우고 새로 넣는다 (ARCHITECTURE 2장). 오프라인이면 조용히 실패하고 저장된 데이터를 보여 줌 | (Repo) `` `NFR-SYNC-02 범위를 다시 불러오면 서버에서 지워진 일정이 Room에서 사라진다`() `` · `` `NFR-SYNC-02 범위를 다시 불러와도 범위 밖 일정은 그대로다`() `` · `` `NFR-OFF-01 오프라인이면 다시 불러오기는 조용히 실패하고 저장된 일정이 남는다`() `` · (VM) `` `NFR-SYNC-02 월을 넘기면 그 월을 다시 불러온다`() `` · (Remote) `` `API GET events는 from to를 보내고 items를 읽는다`() `` |
| NFR-OFF-02 | 일정 작성·수정·삭제·알림 설정은 쓰기. `runOnlineWrite`로 감싸고 오프라인이면 요청 없이 스낵바 "인터넷 연결이 필요해요" (스낵바 위치는 확인 필요 6) | `` `NFR-OFF-02 오프라인이면 일정 저장 요청을 보내지 않고 안내한다`() `` · `` `NFR-OFF-02 오프라인이면 일정 삭제 요청을 보내지 않고 안내한다`() `` · `` `NFR-OFF-02 오프라인이면 일정 알림 설정 요청을 보내지 않고 안내한다`() `` |
| D-88 · 오류 | `INTERNAL_ERROR`·모르는 오류는 공통 오류 스낵바, 화면 그대로. `NOT_FOUND`·`NOT_AUTHOR`·`INVALID_TARGET`·`VALIDATION_FAILED`는 문구가 없어 확인 필요 10 | (VM) `` `D-88 일정 저장이 서버 오류면 공통 오류를 보여 주고 화면은 그대로다`() `` · (Repo) `` `FR-CAL-03 404 NOT_FOUND이면 Room에서 그 일정을 지운다`() `` (확인 필요 10) |
| FR-CAL-04 AC1 (뒷부분) | 일정은 대상의 사람 색(나·상대·함께), 개인 공간은 나 색. `Me.space` 색 키 → `PersonColors` → `DuriseoTheme` | (VM, `feature:main` 또는 `app`) `` `FR-CAL-04 AC1 내 정보의 색으로 DuriseoTheme에 PersonColors를 넣는다`() `` · `` `FR-CAL-04 AC1 색을 바꾸면 캘린더 일정 색이 바뀐다`() `` · (Domain) `` `FR-CAL-04 AC1 모르는 팔레트 키는 기본 사람 색으로 그린다`() `` |
| FR-CAL-04 AC6 | 사람 색은 라벨(나·상대·함께)이나 아바타 글자와 함께. 일정 카드 사람 태그, 상세의 대상 칩에 라벨이 있다 | Preview로 확인 + (VM) `` `FR-CAL-04 AC6 일정 카드에 대상 라벨이 함께 보인다`() `` |

### B. 화면과 상태 (SCREENS.md, `design/screens/`)

모든 화면 `디자인: 반영됨`(캔버스 버전 38). 이미지가 없는 상태는 "디자인 없음"으로 표시했다. 문구는 모두 해요체(NFR-COPY-01)이고 보드 문구는 SCREENS 본문과 같다.

| 화면 | 상태 | 이미지 / 보드 | Preview |
| --- | --- | --- | --- |
| SCR-CAL-01 (`feature:calendar`) | 기본(커플): 앱바(나·너 아바타 겹침, "우리 둘", "함께한 지 D+N"), 알림 버튼*, [전체 메뉴], 월 제목 + 이전·다음 달 + 크게 보기 토글, 요일 행(일요일 `danger-600`), 월 달력(오늘 `ink-900` 테두리, 선택 `brand-500` 채움, 사람 색 점), 손잡이, 선택한 날 제목 + "오늘" + 개수, 일정 카드(시간, 제목, 메모, 사람 태그), FAB, 하단 탭 | `SCR-CAL-01.png` / `Calendar.dc.html` | `CalendarScreenPreview` |
| | 펼친 상태: 날짜 칸에 일정 막대(최대 2개 + "+N"), 목록은 한 줄 행 | `SCR-CAL-01--expanded.png` / `CalendarExpanded.dc.html` | `CalendarScreenExpandedPreview` |
| | 개인 공간: 내 아바타, "나의 캘린더", ("기념일 ›"* 확인 필요 4), D-day 없음 | `SCR-CAL-01--solo.png` / `CalendarSolo.dc.html` | `CalendarScreenSoloPreview` |
| | 커플 + 처음 만난 날 없음: "처음 만난 날 입력하기 ›" → 시트 | `SCR-CAL-01--no-start-date.png` / `CalendarNoStartDate.dc.html` | `CalendarScreenNoStartDatePreview` |
| | 선택한 날 비어 있음: "아직 일정이나 메모가 없어요" + "+ 이 날에 추가하기" | 보드 문구만 (`Calendar.dc.html` 98·99행), 이미지 없음 | `CalendarScreenEmptyDayPreview` |
| | 기념일이 있는 날: 기념일 카드·고리 | `SCR-CAL-01--anniversary.png` — **단위 5** | (단위 5) |
| | 연결 직후: 위에 SCR-COUPLE-04 시트 | `SCR-COUPLE-04.png` (단위 3 시트 `feature:main`이 이 화면 위에 띄움) | 단위 3 Preview 유지 |
| | 요청 중·불러오는 중·오프라인·불러오기 실패 | 디자인 없음 (확인 필요 11) | 승인 시 `CalendarScreenOfflinePreview` |
| SCR-CAL-02 일정 상세 (`feature:calendar`) | 내가 만든 '함께' 일정: 대상 칩, 제목, "시작"·"종료" 줄, "작성" 줄, "메모"(있을 때), "내 알림" 칩 5개 + "나에게만 와요", [수정], [일정 삭제] | `SCR-CAL-02.png` / `EventDetail.dc.html` | `EventDetailScreenPreview` |
| | 상대가 만든 일정: [수정]·[일정 삭제] 없음 + 안내 | `SCR-CAL-02--partner.png` / `EventDetailPartner.dc.html` | `EventDetailScreenPartnerPreview` |
| | 여러 날 일정: 시작 줄·종료 줄에 날짜와 시간 | `SCR-CAL-02--multi-day.png` / `EventDetailMultiDay.dc.html` | `EventDetailScreenMultiDayPreview` |
| | 삭제 확인 다이얼로그 | `SCR-CAL-02--delete-dialog.png` / `EventDetailDialog.dc.html` | `EventDetailScreenDeleteDialogPreview` |
| | 종일 일정(날짜만, 알림 3개) | 이미지 없음, 보드 `allDay` 상태 (`EventDetail.dc.html` 106행) | `EventDetailScreenAllDayPreview` |
| | 개인 공간 상세 | 디자인 없음 (확인 필요 7) | 승인 시 `EventDetailScreenSoloPreview` |
| SCR-CAL-03 일정 작성·수정 (`feature:calendar`) | 새로 추가(커플): 앱바(닫기, "새로 추가", 저장), 종류 탭*, 제목, "누구의 일정인가요?" + 색상 변경, [나\|함께], 안내, 하루 종일, 시작·종료 행, 내 알림, 메모, "상대에게 알림 보내기" | `SCR-CAL-03.png` / `AddEvent.dc.html` | `EventEditScreenCouplePreview` |
| | 개인 공간: 대상 선택·알림 스위치 없음 | `SCR-CAL-03--solo.png` / `AddEventSolo.dc.html` | `EventEditScreenSoloPreview` |
| | 하루 종일: 시간 칩 없음 | `SCR-CAL-03--all-day.png` / `AddEventAllDay.dc.html` | `EventEditScreenAllDayPreview` |
| | 내 일정 수정: "일정 수정", 종류 탭 없음, 대상 선택 있음, 알림 스위치 없음 | `SCR-CAL-03--edit.png` / `AddEventEdit.dc.html` | `EventEditScreenEditPreview` |
| | 상대가 만든 '함께' 일정 수정: 읽기 전용 "누구의 일정 · 함께" + 안내 | `SCR-CAL-03--partner-edit.png` / `AddEventPartnerEdit.dc.html` | `EventEditScreenPartnerEditPreview` |
| | 오류: 종료 < 시작 | `SCR-CAL-03--error.png` / `AddEventError.dc.html` | `EventEditScreenEndErrorPreview` |
| | 날짜 선택 / 시간 선택 / 내 알림 선택 | **디자인 없음** (확인 필요 1·2) | 승인 시 `…PickerPreview` |
| | 저장 중(요청 중): 저장 버튼 비활성 (SCREENS 공통 "요청 중") | 디자인 없음 (공통 규칙) | `EventEditScreenSavingPreview` |

\* 표시한 알림 버튼·"기념일 ›"·종류 탭은 확인 필요 4·5에서 그릴지 정한다.

공용 컴포넌트 (`core:designsystem`): 일정 카드, 달력 셀·막대, 일정 알림 칩, 하루 종일 스위치, 시작·종료 행. 기존 `DuriseoBottomNav`·FAB·`DuriseoAvatar`·스낵바·다이얼로그 재사용. 수치는 보드 값을 이름 붙인 상수로 쓰고 보드 파일·행을 주석에 적는다 (단위 3 확인 필요 14와 같은 방식).

### C. API (API.md 3.3 "일정", 5장)

| API | 요청·응답 | 테스트 |
| --- | --- | --- |
| `GET /events?from=&to=` | `from`·`to` 필수 `YYYY-MM-DD`, `to` 포함, 최대 3개월 → 200 `{items: Event[]}`. 서버는 범위 밖을 잘라 조회. 오류 400 `VALIDATION_FAILED`(`from`/`to` `REQUIRED`·`INVALID`) | (Remote) `` `API GET events는 from to를 보내고 items를 읽는다`() `` |
| `POST /events` | `{title, note?, isAllDay, start, end, isTogether?, notifyPartner?}` → 201 `Event`(`reminder` 항상 null). 400 `VALIDATION_FAILED`(`title` `REQUIRED`·`TOO_LONG`, `note` `TOO_LONG`, `start`/`end` `INVALID`·`REQUIRED`), 400 `DATE_OUT_OF_RANGE`, 400 `INVALID_TARGET`(개인 공간 '함께') | (Remote) 위 FR-CAL-02 AC1 · `` `API POST events 400 VALIDATION_FAILED는 필드 이름을 읽는다`() `` |
| `GET /events/{id}` | → 200 `Event`. 404 `NOT_FOUND` | (Remote) `` `API GET events id는 Event를 읽는다`() `` |
| `PATCH /events/{id}` | 부분 수정 `{title?, note?, isAllDay?, start?, end?, isTogether?}` → 200 `Event`. `note: null`이면 메모 지움, 그 밖의 `null`은 `REQUIRED`. `isAllDay`를 바꾸면 `start`·`end`를 같이 보냄. 403 `NOT_AUTHOR`, 404 `NOT_FOUND` | (Remote) 위 FR-CAL-03 AC4 · `` `API PATCH events는 메모를 지울 때 note null을 보낸다`() `` · `` `API PATCH events는 종일을 바꿀 때 start와 end를 함께 보낸다`() `` |
| `DELETE /events/{id}` | → 204. 403 `NOT_AUTHOR`, 404 `NOT_FOUND` | (Remote) 위 FR-CAL-03 삭제 |
| `PUT /events/{id}/reminder` | `{option}` → 200 `Event`(바뀐 `reminder`). 400 `VALIDATION_FAILED`(`option` `INVALID`·`REQUIRED`) | (Remote) 위 FR-CAL-05 AC1 |
| `DELETE /events/{id}/reminder` | → 204 (알림이 없어도 204, 멱등) | (Remote) 위 FR-CAL-05 AC1 |

`Event` 필드: `id`, `title`, `note`, `isAllDay`, `start`·`end`(종일 `YYYY-MM-DD` / 아니면 UTC 일시), `isTogether`, `authorId`, `updatedBy`, `reminder`(`EventReminder{option, fireAt}`|null), `createdAt`, `updatedAt`. 도메인은 종일을 `LocalDate`, 시간 일정을 `Instant`로 나눈다 (ARCHITECTURE 7장). `message`는 화면에 쓰지 않는다 (API.md 1.1).

### D. Room에 둘 데이터

| 테이블 | 내용 | 규칙 |
| --- | --- | --- |
| `event` (새) | `Event` 필드 전부. `reminder`는 `reminder_option`, `reminder_fire_at` 두 칸 (요청자 본인 것만 오므로 별도 테이블 없음). 종일/시간 구분용으로 `start`·`end`를 문자열 하나씩 또는 날짜·일시 칸으로 나눔(구현 때 정함, 테스트로 고정) | 화면은 `observeEvents(from, to)` Flow만 구독 (걸치는 일정: `start_date <= to AND end_date >= from`, 시간 일정은 기기 시간대로 바꾼 날짜 범위). 쓰기 성공 응답을 트랜잭션으로 반영, 낙관적 갱신 없음 |
| 범위 refresh | `GET /events` 응답을 받으면 같은 트랜잭션에서 그 `from`~`to`에 걸친 행을 지우고 응답을 넣는다 (ARCHITECTURE 2장). 오프라인·실패면 그대로 | `FR-CAL-01`·`NFR-SYNC-02` 테스트 |
| DB 버전 | `DuriseoDatabase` 버전 2 → 3. 출시 전이라 파괴적 재생성 (단위 3 결정 15와 같음). 스키마 JSON은 단위 2 결정대로 아직 커밋하지 않는다 | 확인 필요 아님 (결정) |
| 정리 | `LocalDataCleaner`의 `clearAllTables`가 새 테이블도 지운다 (로그아웃·탈퇴·해제 초기화) | (Repo) `` `FR-COUPLE-05 AC3 해제하면 일정도 Room에서 지운다`() `` |

## 서버 연동

| API | 개발 서버 배포 여부 | 확인 |
| --- | --- | --- |
| `GET·POST /events`, `GET·PATCH·DELETE /events/{id}`, `PUT·DELETE /events/{id}/reminder` | Server `docs/PLAN.md` 단위 3 "캘린더: 일정, 일정 알림, 날짜 범위" **PR 완료**(머지됨). 개발 서버 배포는 리드 확인("배포됨"). Server PLAN의 "배포" 열 상태는 이 단계에서 다시 보지 않음 | 맥 실기기 확인 목록: 월 조회, 작성(시간·종일·여러 날), 수정, 삭제, 알림 칩, 상대 기기에서 보이는 것(기기 2대) |
| 동기화 신호 (SYNC `EVENT`) | 서버 발송은 서버 단위 6 | 단위 7 |
| `importCounts`·`linkedAt` (`GET /me`, 단위 3 PR) | Server 단위 3 PR로 배포됨(리드 확인) | 맥 확인 목록에 추가 (unit-3.md 위험 1) |

## 라이브러리 (MODULES.md 3장 기준)

새 라이브러리 없음. 달력은 `core:designsystem`에 직접 그린다(단위 3 날짜 시트와 같은 방식). 시간 선택은 확인 필요 1의 결정에 따라 Compose Material3 `TimePicker`(이미 `material3`에 있음)를 쓸 수 있다.

## 결정

구현하면서 정한 것 중 문서에 없는 것. 지금은 범위 확정 단계라 아래는 **제안**이고 승인 뒤 확정한다. 문서에 있어야 할 결정은 "확인 필요"에 올렸다.

1. 요청 범위는 보이는 월의 1일 ~ 말일 하나다 (최대 3개월 한도 안). 보드의 달력은 앞뒤 달 날짜 칸을 비활성으로 그려 눌리지 않으므로 그 칸의 일정은 필요 없다 (`Calendar.dc.html` 108행 `disabled`).
2. 화면에 들어올 때(`STARTED`)와 월을 넘길 때 그 월을 다시 불러온다. 여러 월을 미리 받지 않는다.
3. 앱을 새로 열면 오늘이 속한 달, 오늘이 선택된다 (FR-CAL-01 AC1). 같은 실행 중 다른 탭에 갔다 돌아오면 보던 달·날짜를 유지한다.
4. "크게 보기" 토글 버튼, 손잡이 탭, 손잡이를 40 이상 끌기가 같은 동작이다 (보드 `Calendar.dc.html`의 `toggle`·`dragEnd`). 접근성 라벨 "달력 크게 보기"·"달력 작게 보기"는 보드 그대로.
5. 대상 라벨은 보는 사람 기준: `isTogether`면 함께, 아니면 `authorId`가 나면 나, 아니면 상대 (D-34: 상대는 대상으로 고를 수 없어 작성자 = 대상). 개인 공간은 항상 나.
6. 개인 공간에서는 `isTogether`·`notifyPartner`를 보내지 않는다 (API.md: `INVALID_TARGET`을 피함, `notifyPartner`는 커플에서만 의미).
7. 목록의 "개수"는 그 날 목록 항목 수다 (보드 `countLabel`). 단위 4에서는 일정만 세고 단위 5·6이 기념일·메모를 더한다.
8. 시간 일정의 날짜는 기기 시간대로 정한다 (NFR-TIME-01). 서버의 `GET /events` 경계는 서버가 가진 `users.time_zone` 기준이다 (위험 3).
9. 공간이 바뀌면(연결, 가져오기, 해제, 앱 초기화) Room의 일정을 지우고 현재 월을 다시 불러온다 (확인 필요 12에서 사람이 확인).

## 리뷰 기록

| 회차 | Critical | Major | 비고 |
| --- | --- | --- | --- |

## 확인 필요

문서끼리 다르거나 비어 있는 곳이다. 추측하지 않고 선택지와 추천을 적었다. Docs 수정이 필요하면 Docs 레포의 `/prd-sync`로 한다. 1~8은 **디자인이나 문구가 문서에 없어** 막히는 곳이고, 9~14는 단위 순서·구현 방침이라 추천대로 가도 되는 곳이다.

1. **일정의 날짜 선택·시간 선택 화면 디자인이 없다.**
   - 근거: SCR-CAL-03 시작·종료 행의 날짜 칩·시간 칩은 보드에 버튼 모양만 있고(`AddEvent.dc.html` 103~104, 110~111행) 누른 뒤 화면은 SCREENS(110~111행)에도 `design/screens/`에도 없다. SCREENS 공통 표(38행)는 "모든 날짜 선택은 1950-01-01 ~ 2050-12-31"이라고만 한다. 단위 3의 날짜 시트(`StartDatePickerSheet`)는 1950-01-01 ~ **오늘**로 고정이고 `StartDateRules`가 이를 가진다 (`StartDatePickerSheet.kt` 105행, `StartDateRules.kt` 14·25·29·33행). 시간 선택 화면은 어디에도 없다.
   - A (추천): 날짜는 `StartDatePickerSheet`를 "고를 수 있는 범위"를 인자로 받게 일반화(일정은 1950-01-01 ~ 2050-12-31, 미래 허용, 안내 문구는 호출 쪽이 넘김)하고, 시간은 Material3 `TimePicker`를 `DuriseoBottomSheet`에 담아 쓴다. 코드 주석과 Preview 이름에 "디자인 반영 후 맞춤(이미지 대기)"을 적는다 (단위 3 확인 필요 6 선례). 처음 만난 날 시트 안내 문구 "오늘 이후 날짜는 고를 수 없어요. …"는 일정에 쓸 수 없으므로 일정용 안내는 **문구를 넣지 않는다**(안내 줄 없음).
   - B: Docs에 요청서를 만들어 디자인이 나올 때까지 SCR-CAL-03의 날짜·시간 선택을 만들지 않는다 (작성 화면을 못 씀).
   - 단위 3에서 날짜 시트의 이미지 대기 6개가 아직 안 풀린 채라 A는 같은 파일을 두 번 건드린다. A로 가면 SCR-SET-01·SCR-COUPLE-04 쪽 동작·Preview가 바뀌지 않는 것을 테스트로 고정한다.

2. **SCR-CAL-03 "내 알림" 행을 눌렀을 때의 선택 화면이 없다.**
   - 근거: 행(`AddEvent.dc.html` 117~120행)은 버튼이고 현재 값 "없음"만 있다. 선택 UI는 SCR-CAL-02에만 칩 5개/3개로 있다 (`EventDetail.dc.html` 58~62행). SCREENS 110·112행은 "내 알림 기본은 없음"뿐이다.
   - A (추천): 행을 누르면 `DuriseoBottomSheet`에 SCR-CAL-02와 같은 칩 선택지(시간 일정 5개, 종일 3개, 문구 동일)를 보여 준다. 이미지 대기로 표시.
   - B: 작성 화면의 행은 값만 보여 주고(누르기 없음) 알림은 저장 후 SCR-CAL-02에서만 정한다. 문서(FR-CAL-02 AC4 "일정 알림 … 선택 항목")와 어긋난다.

3. **일정 만들기와 알림 설정이 요청 둘이라 중간에 실패할 수 있다.**
   - 근거: `POST /events`의 `reminder`는 항상 null이고 알림은 `PUT /events/{id}/reminder`로 따로 정한다 (API.md 3.3 244·248행). 수정에서 `PATCH`로 종일을 바꾸면 서버가 알림을 지우고 `reminder: null`을 준다 (330행). 두 요청이 같은 트랜잭션이 아니고 부분 실패 처리 규칙이 문서에 없다.
   - A (추천): 작성 화면에서 알림을 골랐으면 `POST` 성공 뒤 이어서 `PUT`한다. `PUT`이 실패하면 일정은 저장된 상태로 두고 화면을 닫은 뒤 공통 오류 스낵바를 보여 준다(새 문구 없음). 일정이 중복 생성되지 않는다. 수정 화면에서 알림을 바꾼 경우도 `PATCH` 뒤 `PUT`/`DELETE`.
   - B: 실패 시 작성 화면에 남겨 다시 저장하게 한다. 이미 생성된 일정이 있어 재시도가 중복 생성이 될 수 있어 별도 상태 관리가 필요하다.

4. **앱바의 알림 버튼·"함께한 지 D+N ›"·개인 "기념일 ›"는 가는 화면이 단위 5·7이다.**
   - 근거: 알림 버튼 → SCR-NOTI-01(단위 7), D-day 줄·"기념일 ›" → SCR-ANNIV-01(단위 5). SCREENS 78·79·80행은 이 버튼들이 보인다고만 쓴다. PLAN 18행은 단위 4~6이 순서를 바꿀 수 있다고 한다. 단위 3 확인 필요 8(갈 화면이 다른 단위인 버튼은 그리지 않음, 리드 승인)이 선례다.
   - A (추천, 선례): 알림 버튼과 개인 공간의 "기념일 ›" 링크는 그리지 않고, 단위 7·5에서 추가한다(`PLAN.md` 단위 5·7에 한 줄씩 적음). 커플의 "함께한 지 D+N"은 AC(FR-CAL-01 AC3)라 보이되 "›"와 이동은 없다. "처음 만난 날 입력하기 ›"는 시트가 있어 그대로 동작한다.
   - B: 보드대로 모두 그리고 누르면 아무 일도 없게 둔다(문서와는 같지만 죽은 버튼). 단위 5·7에서 연결한다.
   - 단위 4가 5·7보다 먼저가 아니게 순서를 바꾸면 이 항목은 필요 없어진다.

5. **작성 화면의 종류 탭 [일정 | 메모]와 FAB 라벨 "일정 또는 메모 추가".**
   - 근거: SCREENS 110행: 메모를 고르면 SCR-MEMO-04(단위 6). 보드 FAB 접근성 라벨 "일정 또는 메모 추가" (`Calendar.dc.html` 133행).
   - A (추천): 단위 6 전에는 종류 탭을 그리지 않는다(SCR-CAL-03은 일정 작성만, 제목 위에 탭 없음). FAB 라벨은 보드 그대로 둔다. 단위 6에서 탭을 추가한다.
   - B: 탭을 그리되 [메모]는 비활성. 비활성 상태의 디자인이 없다.

6. **스낵바 위치(탭 있는 화면)가 정해지지 않았다.**
   - 근거: design-system.md 155행 "탭이 있는 화면(FAB가 있는 화면)에서의 위치를 그린 보드는 없어요. 정해질 때까지 정하지 않은 값이에요". SCREENS 공통 표(42행)도 "탭 있는 화면에서의 위치는 아직 보드가 없어요". SCR-CAL-01의 "인터넷 연결이 필요해요"·공통 오류가 이 위치를 쓴다. SCR-CAL-02·03은 탭이 없어 보드에 있는 값(위에서 40 또는 바닥에서 112) 중 고르면 된다.
   - A (추천): SCR-CAL-01은 임시로 보드에 있는 "위에서 40"(`Main.dc.html` 51행)을 쓰고 이미지 대기로 표시, SCR-CAL-02·03도 같은 값. 새 수치는 만들지 않는다.
   - B: Docs에 요청서를 만들어 보드가 나올 때까지 CAL-01의 스낵바 위치를 비워 둔다(안내가 안 보일 수 있어 NFR-OFF-02를 못 지킴).

7. **개인 공간 일정 상세와 삭제 다이얼로그 본문.**
   - 근거: 삭제 다이얼로그 본문 "삭제하면 상대 캘린더에서도 사라지고 되돌릴 수 없어요."는 보드에만 있고(`EventDetail.dc.html` 78행) SCREENS 103행은 "삭제 확인"이라고만 쓴다. 개인 공간에서는 "상대"가 없어 어색하지만 개인 공간용 문구는 어디에도 없다. SCR-CAL-02의 개인 공간 상태 이미지도 없다 (대상 칩 "나", 알림 칩 등은 같은 화면 규칙으로 그릴 수 있음).
   - A (추천): 커플·개인 모두 보드 문구 그대로 쓰고, 개인 공간 본문은 Docs에 요청서로 올린다 (문구 신설 없음, 이미지 대기).
   - B: 개인 공간은 본문에서 앞 절을 뺀 문구로 쓴다("삭제하면 되돌릴 수 없어요."). 문서에 없는 문구라 승인이 필요하다.

8. **입력 검증 문구와 표시가 없다 (제목 비어 있음, 50자·1,000자 초과).**
   - 근거: SCREENS 오류는 종료 < 시작 하나뿐이다(122행). 서버는 제목 앞뒤 공백 제거 후 빈 값이면 `REQUIRED`, 50자 초과면 `TOO_LONG`, 메모 1,000자 초과면 `TOO_LONG` (API.md 286·287행). NFR-LIMIT-01은 한도만 있고 화면 규칙이 없다. 글자 수는 문자 단위(D-82).
   - A (추천): 제목이 공백뿐이면 [저장]을 비활성으로 두고 오류 문구는 만들지 않는다. 입력은 한도에서 더 받지 않는다(제목 50, 메모 1,000, 코드 포인트로 센다). 새 문구 없음. 한도에서 막혔다는 표시는 없다.
   - B: Docs에 오류 문구·글자 수 표시를 요청한다.

9. **알림 권한 요청 시점이 단위 7인데, SCREENS 공통 표는 "일정 알림 설정 때"라고 쓴다.**
   - 근거: SCREENS 37행 "Android 13 이상에서 처음 알림이 필요한 기능(일정 알림 설정, …)을 쓸 때 요청". 단위 3 "하지 않는 것" 표는 권한 요청 구현을 단위 7로 미뤘다. 단위 4에서 알림 칩을 눌러도 푸시가 오려면 FCM·권한이 필요하다(FCM은 단위 7).
   - A (추천): 단위 4는 알림 설정을 서버에 저장하기만 하고 권한은 요청하지 않는다. 단위 7에서 "일정 알림 설정 때" 요청을 연결한다 (`PLAN.md` 단위 7에 적음). 단위 4 PR 본문에 "푸시는 단위 7 전까지 오지 않음"을 적는다.
   - B: 단위 4에서 `POST_NOTIFICATIONS` 권한 요청만 먼저 구현한다. 요청 후 거부 처리·재요청 규칙이 문서에 없다.

10. **일정 쓰기 오류 코드의 화면 문구가 없다.**
    - 근거: `NOT_FOUND`(없거나 다른 공간 = 이미 지워진 일정), `NOT_AUTHOR`(권한 없음, 앱이 메뉴를 숨겨 보통 도달하지 않음), `INVALID_TARGET`, `VALIDATION_FAILED`, `DATE_OUT_OF_RANGE`. SCREENS 공통 표는 `INTERNAL_ERROR`·모르는 `code`만 공통 오류로 정한다(44행). ARCHITECTURE 5장은 "404는 없어진 항목으로 처리하고 Room에서도 지운다"고만 한다.
    - A (추천): `NOT_FOUND`면 Room에서 그 일정을 지우고 이전 화면으로 돌아가 공통 오류 스낵바를 보인다. `NOT_AUTHOR`면 공통 오류 스낵바 + `GET /events/{id}`로 다시 읽는다(낡은 권한 표시 갱신). 나머지(필드 오류·범위·대상)는 앱이 입력을 미리 막으므로 공통 오류 스낵바로 처리한다. 새 문구 없음.
    - B: Docs에 "이미 삭제된 일정이에요" 같은 문구를 요청한다.

11. **불러오는 중·불러오기 실패·오프라인 상태의 표시와 당겨서 새로고침.**
    - 근거: ARCHITECTURE 2장은 다시 불러오는 때에 "당겨서 새로고침"을 넣고(40행) NFR-SYNC-02도 같다. SCREENS SCR-CAL-01에는 새로고침 동작·로딩 표시가 없다. 오프라인 읽기는 "저장된 데이터를 보여 줌"이라 화면 안내는 필요 없다. 공통 표의 "요청 중"은 쓰기 버튼만 정한다.
    - A (추천): 로딩 표시와 당겨서 새로고침은 만들지 않는다. 화면에 들어올 때·월을 넘길 때 다시 불러오고, Room 데이터를 먼저 보여 준다(조용히 실패). NFR-SYNC-02의 "당겨서 새로고침"은 단위 7(SYNC)과 함께 정하거나 Docs에서 뺀다. PR 본문에 "문서와 다르게 한 점"으로 적는다.
    - B: Material3 `PullToRefreshBox`를 목록에 붙인다(디자인 없음, 스피너 모양 새 요소).

12. **공간이 바뀔 때 Room의 일정을 어떻게 정리하나 (ARCHITECTURE 규칙 없음).**
    - 근거: 커플 연결 후 `POST /couple/import`로 개인 공간의 일정이 커플 공간으로 옮겨지거나 삭제되고(API.md 4.4), 해제 뒤에는 새 개인 공간이 되는데(4.5), `event` 테이블은 `space_id`를 갖지 않는다(`GET /events`가 활성 공간만 줌). 단위 3의 `SpaceChange`·`LocalDataCleaner`는 새 테이블을 모른다.
    - A (추천): 공간 ID가 바뀌는 `GET /me`(연결·해제·초기화·복구), 가져오기 결정 직후에 `event` 테이블을 비우고 현재 월을 다시 불러온다. `docs/ARCHITECTURE.md` 2장에 한 줄을 이 레포 PR에서 더한다(Docs 문서가 아니라 이 레포 문서).
    - B: `event`에 `space_id` 칸을 두고 현재 공간 것만 보여 준다. 공간 전환 때 지울 필요는 줄지만 오래된 행이 쌓이고 DAO 조건이 늘어난다.

13. **여러 날 시간 일정을 목록에서 어떻게 보이나, 시간 서식.**
    - 근거: 목록의 일정 카드 시간 칸은 보드에서 24시간 "19:30"(`Calendar.dc.html` 소스의 `time`), 상세·작성은 "오후 7:30"이다(`EventDetail.dc.html` 100~101, `AddEvent.dc.html` 209행). 종일은 "종일". SCREENS에는 서식 규정이 없고(98행은 상세 "날짜와 시간"만), 여러 날 시간 일정의 **중간 날·마지막 날** 목록 시간 칸은 보드에도 문서에도 없다 (종일 여러 날은 보드가 "종일"로 그린다, 17·18일).
    - A (추천): 보드 그대로(목록은 24시간 "HH:mm", 상세·작성은 한국어 오전/오후 12시간), 여러 날 시간 일정은 걸친 모든 날의 카드에 **시작 시각**을 그대로 쓴다(새 문구 없음). 이미지 대기 표기.
    - B: Docs에 규칙을 요청한다.

14. **새 일정 작성의 기본 날짜·시간이 정해져 있지 않다.**
    - 근거: 날짜는 FAB·"+ 이 날에 추가하기"를 누른 시점의 선택된 날로 보는 것이 자연스럽지만 문서에 없다. 시간은 보드 샘플이 "오후 7:30 – 9:30"(`AddEvent.dc.html` 209·210행)이고 샘플 값은 자리표시다(SCREENS 18행). 시작·종료는 필수라 비워 둘 수 없다 (FR-CAL-02 AC1, API.md 283행). 종일을 켰다 끄는 전환 때 시간 값이 어떻게 되는지도 문서에 없다.
    - A (추천): 날짜는 선택된 날(없으면 오늘). 시간은 기기 시계의 다음 정시부터 1시간. 종일을 껐다 켜도 마지막 시간 값을 유지한다. 새 문구는 없고 규칙만이라 Docs에 확인을 요청해 정해지면 맞춘다.
    - B: Docs `/prd-sync`에서 기본값을 정한 뒤 만든다.

## 남은 일

- 단위 5: 기념일 카드·달력의 기념일 고리·한 날 3개 표시 규칙, "함께한 지 D+N ›"와 "기념일 ›" 연결 (확인 필요 4)
- 단위 6: 메모 카드·메모 날짜 점, 작성 화면 종류 탭 (확인 필요 5)
- 단위 7: `SYNC EVENT` 수신, 알림 버튼·점, 일정 알림 권한 요청, 푸시·알림 목록에서 일정 상세 진입 (확인 필요 4·9)
- 이미지가 오면(디자인 반영 후 맞춤): 확인 필요 1·2·6·13의 이미지 대기 부분
- PR 전: `docs/MODULES.md`(`feature:calendar` "있음" 문구에서 "화면은 단위 4" 제거, 새 `data` 클래스), `docs/PLAN.md` 단위 4 "PR 완료"

## 사람이 할 일

1. 위 범위 표와 확인 필요 14건을 본다. 1~8은 답이 필요하고 9~14는 추천대로 가면 된다.
2. 기기 2대 확인 준비 (단위 3과 같음). `local.properties` 새 키 없음.
3. 맞으면 **start**.

## 위험

1. **디자인 없음이 많다.** 날짜·시간·알림 선택(확인 필요 1·2)은 임시 모양으로 만들고 이미지 대기로 표시한다. 이미지가 오면 모양만 맞춘다.
2. **달력 셀 수치가 많다.** 보드의 5주 고정 격자(`Calendar.dc.html` 108행)와 달리 실제 달은 4~6주다. 높이(접힌 52, 펼친 88)·원 지름(34/26)은 보드 값이고 6주일 때 화면 높이를 넘는지 Preview로 확인한다.
3. **시간대 경계**: 서버는 `GET /events`의 시간 일정 경계를 서버가 가진 `users.time_zone`으로 정한다 (API.md 276행). 앱은 기기 시간대로 날짜를 나눈다. `syncTimeZone`(단위 2)이 실패해 서버 값이 낡으면 자정 근처 일정이 한 달 끝에서 빠질 수 있다.
4. **D+N이 0 이하가 되는 경우**는 단위 5와 함께 정한다 (unit-3.md 위험 5).
5. **일정 알림은 단위 7 전에 오지 않는다.** 설정은 저장되고 상세에도 보이지만 푸시는 FCM이 들어오는 단위 7부터다.
6. **상대가 바꾼 일정은 단위 7 전에는 다음 진입에서야 보인다** (SYNC 없음). 월 이동·화면 진입 때 다시 불러오는 것으로 대신한다.
7. **계정 2개 필요**: 상대 일정·'함께' 일정 확인은 커플 연결된 기기 2대가 필요하다 (게스트는 연결 불가).
