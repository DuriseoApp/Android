# 단위 2: 인증

> 브랜치: `feat/2-auth` · PR: https://github.com/DuriseoApp/Android/pull/9 · 상태: 리뷰 1회차 수정 반영 (CI 202 통과 이후 수정분은 CI 재확인 대기)
> 기준: PRD v0.8 (Docs main, Docs PR #9 머지), SCREENS.md(캔버스 버전 38, 2026-10-26 갱신), API.md(2026-10-26 갱신: 1장 토큰 형식·인증 실패 순서, 3.1 `AuthTokens`·`Me`, 4.1~4.3·4.6·4.7, 5장), design-system.md(Android 매핑, 스낵바), tokens.json
> 서버: Server 단위 1(계정) 머지됨. 개발 서버 배포는 사용자 확인 (아래 "서버 연동")

단위 2는 로그인 상태를 만들고 지키는 단위다. 시작 화면(SCR-START-01), 게스트 시작, Google·카카오 로그인, 게스트 연동과 기존 계정 로그인, 토큰 암호화 저장·자동 갱신·세션 끊김, 로그아웃, 탈퇴, 내 정보(`GET /me`) 캐시, 시간대 갱신, 전체 메뉴(SCR-SET-01)의 계정 부분을 만든다.

**이 단위에서 하지 않는 것** (다른 단위 화면이 있어야 하는 AC)

| ID | 이유 | 단위 |
| --- | --- | --- |
| FR-AUTH-02 AC1의 "SCR-COUPLE-01이 표시된다" | SCR-COUPLE-01은 단위 3 화면. 단위 2는 `isNewUser`로 갈 곳을 고르는 분기까지 (확인 필요 2) | 3 |
| FR-AUTH-03 AC4 (D-79), FR-AUTH-04 AC2 (D-07) | 커플 연결 메뉴의 [로그인하고 연결하기]·게스트 안내는 SCR-COUPLE-01 | 3 |
| FR-AUTH-05 AC3 (D-80) | "다른 계정으로 로그인"은 SCR-COUPLE-01. 로그아웃 동작(리포지토리)은 단위 2에서 만들어 단위 3이 그대로 부른다 | 3 |
| FR-AUTH-06 AC3 | Kakao 연결 끊기는 서버가 한다 (API 4.6). 앱 할 일 없음 | 서버 |
| FR-AUTH-07 | 닉네임·사진 수정은 SCR-SET-04 | 8 |
| FR-AUTH-09 | 앱 밖 웹 페이지. 앱 기능 없음 | 출시 |
| `PUT /me/devices` (API 4.2 5단계, 4.7 6단계) | FCM 토큰이 단위 7에서 생긴다. 단위 7이 로그인 3종 직후 호출을 더한다 | 7 |
| `importPending` → SCR-COUPLE-04 | 연결 완료 시트는 단위 3 | 3 |

## 범위

### A. 기능·수용 기준 (PRD 6.1, 6.7)

테스트 이름은 CONVENTIONS.md "테스트" 형식. (VM) = ViewModel + 가짜 리포지토리 + Turbine, (Repo) = 가짜 remote + Room, (Remote) = MockWebServer.

| ID | 내용 | 테스트 |
| --- | --- | --- |
| FR-AUTH-01 AC1 | "게스트로 시작하기" → `POST /auth/guest` → 토큰 암호화 저장 → `GET /me`를 Room에 → SCR-CAL-01 | (VM) `` `FR-AUTH-01 AC1 게스트로 시작하면 캘린더로 간다`() `` · (Repo) `` `FR-AUTH-01 AC1 게스트 시작 응답의 토큰을 저장하고 내 정보를 Room에 넣는다`() `` |
| FR-AUTH-01 AC2 | 저장된 토큰이 있으면 시작 화면 없이 캘린더 (요청 없이 바로, 내 정보는 뒤에서 refresh) | (VM, `feature:main`) `` `FR-AUTH-01 AC2 저장된 토큰이 있으면 시작 화면 없이 캘린더로 간다`() `` |
| FR-AUTH-01 AC3 | 토큰이 없거나 복호화할 수 없으면(재설치·기기 변경·백업 복원) SCR-START-01. 복호화 실패면 저장소를 비운다 | `` `FR-AUTH-01 AC3 저장된 토큰이 없으면 시작 화면을 보여 준다`() `` · `` `FR-AUTH-01 AC3 토큰을 복호화할 수 없으면 저장소를 비우고 시작 화면을 보여 준다`() `` |
| FR-AUTH-02 AC1 | 신규 가입(`isNewUser: true`) → SCR-COUPLE-01 (D-31). 단위 2는 분기 결과 `NewMember`까지 (확인 필요 2) | `` `FR-AUTH-02 AC1 isNewUser가 true면 커플 연결로 갈 곳을 고른다`() `` |
| FR-AUTH-02 AC2 | 기존 회원 → `GET /me`로 그 계정의 현재 상태 → 캘린더 | `` `FR-AUTH-02 AC2 기존 회원은 로그인 뒤 내 정보를 다시 읽고 캘린더로 간다`() `` |
| FR-AUTH-02 AC3 | 다른 기기 로그인으로 끊긴 기기: 다음 요청(또는 refresh)이 401 `SESSION_REPLACED` → 로컬 데이터 삭제 → SCR-START-01 + 스낵바 "다른 기기에서 로그인해서 로그아웃됐어요" (D-51, API 1장) | (Remote) `` `FR-AUTH-02 AC3 요청이 401 SESSION_REPLACED면 세션을 끝내고 REPLACED를 알린다`() `` · `` `FR-AUTH-02 AC3 refresh가 401 SESSION_REPLACED여도 REPLACED를 알린다`() `` · (Repo) `` `FR-AUTH-02 AC3 세션이 끝나면 토큰과 Room과 설정을 모두 지운다`() `` · (VM) `` `FR-AUTH-02 AC3 REPLACED로 시작 화면에 오면 안내 스낵바를 보여 준다`() `` · `` `FR-AUTH-02 AC3 SESSION_INVALID면 안내 없이 시작 화면으로 간다`() `` |
| FR-AUTH-02 (Google) | Credential Manager로 ID 토큰 → `POST /auth/social {provider: "GOOGLE", token}` | (Remote) `` `FR-AUTH-02 Google 로그인은 ID 토큰을 provider GOOGLE로 보낸다`() `` |
| FR-AUTH-02 (카카오) | 카카오 SDK 액세스 토큰 → `{provider: "KAKAO", token}` | (Remote) `` `FR-AUTH-02 카카오 로그인은 액세스 토큰을 provider KAKAO로 보낸다`() `` |
| FR-AUTH-02 (취소) | 사용자가 Google 선택창·카카오 로그인을 닫으면 요청 없이 그 화면에 머문다 (안내 없음) | (VM) `` `FR-AUTH-02 소셜 로그인을 취소하면 요청하지 않고 화면에 머문다`() `` |
| FR-AUTH-03 AC1 | 계정 · 게스트에서 [Google로 로그인]/[카카오로 로그인] → `POST /auth/link` 204 → `GET /me` → 같은 화면이 계정 · 회원으로 바뀜. 토큰은 그대로 (API 4.3 5단계) | (VM) `` `FR-AUTH-03 AC1 연동이 끝나면 내 정보를 다시 읽어 회원 계정 화면이 된다`() `` · (Repo) `` `FR-AUTH-03 AC1 연동은 토큰을 바꾸지 않는다`() `` |
| FR-AUTH-03 AC2 | 409 `SOCIAL_ALREADY_USED` → 다이얼로그 ① "이미 가입된 계정이에요". 연동하지 않음 | (VM) `` `FR-AUTH-03 AC2 SOCIAL_ALREADY_USED면 이미 가입된 계정 다이얼로그를 보여 준다`() `` |
| FR-AUTH-03 AC3 | ① [그 계정으로 로그인] → ② "그 계정으로 로그인할까요?" → [삭제하고 로그인] → 같은 소셜 토큰으로 `POST /auth/switch` → 게스트 토큰·로컬 데이터 삭제, 새 토큰 저장 → `GET /me` → 그 계정의 현재 상태(캘린더). 어느 다이얼로그든 [취소]면 요청 없음 (API 4.7) | (VM) `` `FR-AUTH-03 AC3 그 계정으로 로그인을 누르면 삭제 확인 다이얼로그를 보여 준다`() `` · `` `FR-AUTH-03 AC3 삭제하고 로그인을 누르면 같은 소셜 토큰으로 switch를 보낸다`() `` · `` `FR-AUTH-03 AC3 두 다이얼로그에서 취소하면 아무 요청도 보내지 않는다`() `` · (Repo) `` `FR-AUTH-03 AC3 switch가 성공하면 게스트 데이터를 지우고 새 토큰을 저장한다`() `` |
| FR-AUTH-04 AC1 | 게스트가 SCR-SET-01을 열면 맨 위 배너 "게스트로 사용 중이에요 / 앱을 삭제하거나 기기를 바꾸면 데이터를 복구할 수 없어요." + [로그인해서 기록 지키기] → 계정 화면 | (VM) `` `FR-AUTH-04 AC1 게스트는 전체 메뉴 맨 위에 안내 배너와 로그인 버튼이 보인다`() `` · `` `FR-AUTH-04 AC1 회원에게는 게스트 배너가 없다`() `` |
| FR-AUTH-05 AC1 | 로그아웃 확인 → `POST /auth/logout` 204 → 토큰·Room·설정 삭제 → SCR-START-01 (D-50은 서버) | (VM) `` `FR-AUTH-05 AC1 로그아웃을 확인하면 로컬 데이터를 지우고 시작 화면으로 간다`() `` · (Repo) `` `FR-AUTH-05 AC1 로그아웃 204 뒤 토큰과 Room을 지운다`() `` · `` `NFR-OFF-02 오프라인이면 로그아웃 요청을 보내지 않고 안내한다`() `` (확인 필요 10) |
| FR-AUTH-05 AC2 | 게스트에게는 전체 메뉴·계정 화면 모두 로그아웃이 없다 | (VM) `` `FR-AUTH-05 AC2 게스트에게는 로그아웃이 보이지 않는다`() `` |
| FR-AUTH-06 AC1 | 개인 공간(게스트 포함) 탈퇴 확인 → `DELETE /me` 204 → 로컬 데이터 삭제 → SCR-START-01 | (VM) `` `FR-AUTH-06 AC1 탈퇴를 확인하면 로컬 데이터를 지우고 시작 화면으로 간다`() `` · `` `FR-AUTH-06 AC1 개인 공간이면 바로 삭제된다는 탈퇴 문구를 쓴다`() `` · `` `NFR-OFF-02 오프라인이면 탈퇴 요청을 보내지 않고 안내한다`() `` |
| FR-AUTH-06 AC2 | 커플 공간이면 다이얼로그 본문에 상대 닉네임(`Me.space.partner.nickname`). 서버가 해제 후 삭제 | (VM) `` `FR-AUTH-06 AC2 커플 공간이면 상대 이름이 들어간 탈퇴 문구를 쓴다`() `` |
| FR-AUTH-08 AC1 | 시작 버튼 아래 동의 문구 + "이용약관"·"개인정보처리방침" 링크 모양. 나이는 묻지 않음 | Preview로 확인. `` `FR-AUTH-08 AC1 동의 문구는 strings의 만 14세 문구다`() `` (리소스 ID 확인) |
| FR-AUTH-08 AC2 | 링크는 웹 페이지를 연다 — 주소는 Q-03 미정 (확인 필요 9) | 주소가 정해진 뒤 |
| FR-SET-01 AC1 (계정 부분) | 설정 › 계정 행 값: 회원은 로그인 수단, 게스트는 "게스트 · 로그인하기"(`brand-700`). 로그아웃은 회원만. 계정 · 회원의 로그인 카드·로그인 방식은 `Me.providers` | (VM) `` `FR-SET-01 AC1 계정 행은 회원이면 로그인 수단을 게스트면 게스트 로그인하기를 보여 준다`() `` |
| FR-SET-01 AC2 | 탈퇴는 전체 메뉴가 아니라 계정 화면 안 (D-44) | Preview·화면 구조로 확인 |
| NFR-SEC-01 | 토큰은 Tink AEAD로 암호화해 저장(평문 저장 없음), 로그에 토큰·`Authorization`·인증 본문 없음 (단위 1 로깅은 HEADERS + 가림) | `` `NFR-SEC-01 저장소에는 토큰 평문이 남지 않는다`() `` · `` `NFR-SEC-01 AuthTokens toString은 토큰 값을 찍지 않는다`() `` |
| NFR-OFF-02 | 로그인 3종·연동·로그아웃·탈퇴는 쓰기로 보고 `runOnlineWrite`로 감싼다. 오프라인이면 요청 없이 스낵바 "인터넷 연결이 필요해요" | `` `NFR-OFF-02 오프라인이면 게스트 시작 요청을 보내지 않고 안내한다`() `` · `` `NFR-OFF-02 오프라인이면 연동 요청을 보내지 않고 안내한다`() `` |
| NFR-TIME-01 (ERD 1장, API 3.1 `PATCH /me`) | 앱 실행 때(로그인 상태 복원 직후)와 로그인·switch 직후 `GET /me` 뒤, 기기 시간대(`ZoneId.systemDefault().id`)가 `Me.timeZone`과 다르면 `PATCH /me {timeZone}`. 응답 `Me`를 Room에. 오프라인·실패는 조용히 건너뜀 (사용자 동작이 아님) | (Repo) `` `NFR-TIME-01 기기 시간대가 내 정보와 다르면 PATCH me timeZone을 보낸다`() `` · `` `NFR-TIME-01 시간대가 같으면 보내지 않는다`() `` · `` `NFR-TIME-01 오프라인이면 조용히 건너뛴다`() `` |
| API 1장 401 처리 | `TOKEN_EXPIRED`만 refresh. `SESSION_REPLACED`·`SESSION_INVALID`는 refresh 없이 세션 종료. 오류 응답 `code`로만 판단 | 아래 C절 |

### B. 화면과 상태 (SCREENS.md, `design/screens/`)

| 화면 | 상태 | 이미지 / 보드 | Preview |
| --- | --- | --- | --- |
| SCR-START-01 (`feature:auth`) | 기본 | `SCR-START-01.png` / `Main.dc.html` | `StartScreenPreview` |
| | 다른 기기 로그인으로 로그아웃: 위에서 40에 스낵바 (design-system "스낵바" 위치) | `SCR-START-01--session-replaced.png` / `MainSessionReplaced.dc.html` | `StartScreenSessionReplacedPreview` |
| | 로그인 요청 중: 버튼 비활성 (문서에 없음, 확인 필요 6) | 없음 | `StartScreenLoadingPreview` (승인 시) |
| SCR-SET-01 전체 메뉴 (`feature:settings`) — 단위 2 부분만 (확인 필요 4) | 게스트: 맨 위 게스트 배너 + [로그인해서 기록 지키기], 계정 행 "게스트 · 로그인하기", 로그아웃 없음 | `SCR-SET-01--guest.png` / `MenuGuest.dc.html`(→ `Menu.dc.html`) | `SettingsScreenGuestPreview` |
| | 회원: 계정 행 로그인 수단, 로그아웃(`danger-700`) | `SCR-SET-01.png` / `Menu.dc.html` | `SettingsScreenMemberPreview` |
| | 로그아웃 확인 다이얼로그 | `SCR-SET-01--logout-dialog.png` / `MenuLogoutDialog.dc.html` | `SettingsScreenLogoutDialogPreview` |
| SCR-SET-01 계정 (하위 화면, `feature:settings`) | 게스트: 배너, [Google로 로그인] [카카오로 로그인], 안내, 게스트 ID, [탈퇴하기] | `SCR-SET-01--account-guest.png` / `Account.dc.html` | `AccountScreenGuestPreview` |
| | 이미 가입된 계정 ① | `SCR-SET-01--account-guest-existing.png` / `AccountGuestExisting.dc.html` | `AccountScreenAlreadyUsedDialogPreview` |
| | 그 계정으로 로그인 확인 ② (`danger-700` 확인) | `SCR-SET-01--account-guest-existing-confirm.png` / `AccountGuestExistingConfirm.dc.html` | `AccountScreenSwitchConfirmDialogPreview` |
| | 회원: 로그인 카드("Google로 로그인 중"·"카카오로 로그인 중" + 닉네임), 로그인 방식, 가입일(확인 필요 1), [로그아웃], [탈퇴하기] | `SCR-SET-01--account-member.png` / `AccountMember.dc.html` | `AccountScreenMemberGooglePreview`, `AccountScreenMemberKakaoPreview` |
| | 탈퇴 확인 (커플 공간 문구 / 개인 공간 문구) | `SCR-SET-01--withdraw-dialog.png` / `AccountWithdraw.dc.html` | `AccountScreenWithdrawDialogCouplePreview`, `AccountScreenWithdrawDialogPersonalPreview` |
| | 로그아웃 확인 (계정 화면에서) | 보드 `Account.dc.html` `logoutOpen` | `AccountScreenLogoutDialogPreview` |

공용 컴포넌트 (`core:designsystem`, design-system "컴포넌트", 각각 `@Preview`): `DuriseoButton`(채움 `secondary`=ink-900 / 아웃라인 `line-200` / 면 `surface-100` / 파괴적 `danger-700` / 브랜드 `brand-500`), `DuriseoDialog`(폭 최대 320, 모서리 24, `shadow-dialog`, 버튼 2개), `DuriseoTopAppBar`(64, 뒤로·닫기), `DuriseoGuestBanner`(`warning-50`/`warning-800`, 정보 아이콘), `DuriseoMenuRow`. 로고 마크 VectorDrawable (확인 필요 8).

문구 (SCREENS.md 그대로 `strings.xml`): 시작 — "둘이서", "일정도, 공지도, 메모도\n우리 둘만 보는 공간에.", "간편하게 시작하기", "Google로 계속하기", "카카오로 계속하기", "또는", "게스트로 시작하기", 게스트 설명, 동의 문구, "다른 기기에서 로그인해서 로그아웃됐어요". 전체 메뉴·계정 — 게스트 배너 2줄, "로그인해서 기록 지키기", "게스트 · 로그인하기", "Google로 로그인", "카카오로 로그인", 기존 계정 안내, "탈퇴하기", "Google로 로그인 중"·"카카오로 로그인 중", "로그아웃", 다이얼로그 4종의 제목·본문·버튼("취소", "로그아웃", "탈퇴하기", "그 계정으로 로그인", "삭제하고 로그인"). 보드에만 있는 문구는 확인 필요 5.

### C. API (API.md 3.1, 4장, 5장)

경로는 모두 `/api/v1` 아래. 오류는 `ApiCaller`가 `ApiError(code)`로 바꾸고 화면은 `code`로만 분기한다. "인증 없음" 클라이언트는 `Authorization`을 붙이지 않는다.

| API | 요청 → 성공 | 오류 코드 → 앱 동작 | 클라이언트 | 테스트 (Remote) |
| --- | --- | --- | --- | --- |
| `POST /auth/guest` | 본문 없음 → 201 `AuthTokens` (`isNewUser: true`) | `INTERNAL_ERROR` → 공통 오류 | 인증 없음 | `` `API POST auth guest는 본문 없이 보내고 AuthTokens 다섯 필드를 읽는다`() `` |
| `POST /auth/social` | `{provider: "GOOGLE"\|"KAKAO", token}` → 200 `AuthTokens` | `SOCIAL_TOKEN_INVALID`(401) → 공통 오류(확인 필요 6), `INTERNAL_ERROR`(제공자 장애 포함) → 공통 오류 | 인증 없음 | `` `API POST auth social은 provider와 token을 보낸다`() `` · `` `API auth social의 401 SOCIAL_TOKEN_INVALID는 refresh 하지 않는다`() `` |
| `POST /auth/link` | `{provider, token}` + 게스트 access → 204 | `SOCIAL_ALREADY_USED`(409) → 다이얼로그 ①, `SOCIAL_TOKEN_INVALID`(401) → 공통 오류, `GUEST_ONLY`(403) → 공통 오류 + `GET /me` 다시 읽기, `INTERNAL_ERROR` → 공통 오류 | 인증 | `` `API POST auth link는 204를 성공으로 본다`() `` · `` `API auth link의 409는 SOCIAL_ALREADY_USED ApiError다`() `` |
| `POST /auth/switch` | `{provider, token}` + 게스트 access → 200 `AuthTokens` (`isNewUser: false`) | `SOCIAL_NOT_REGISTERED`(404) → 확인 필요 6, `SOCIAL_TOKEN_INVALID`·`GUEST_ONLY`·`INTERNAL_ERROR` → 공통 오류 (게스트 그대로) | 인증 | `` `API POST auth switch는 게스트 access 토큰과 함께 보내고 AuthTokens를 읽는다`() `` |
| `POST /auth/refresh` | `{refreshToken}` (access 없음) → 200 `AuthTokens` (새 refresh) | `SESSION_REPLACED`(401) → 세션 종료(안내), `SESSION_INVALID`(401) → 세션 종료(안내 없음), 네트워크 오류 → `Offline`(토큰 유지), 그 밖 → 원래 요청 실패 | 인증 없음 (Authenticator 전용 OkHttp) | D절 테스트 |
| `POST /auth/logout` | 본문 없음 → 204 | `GUEST_NOT_ALLOWED`(403) → 게스트 화면엔 메뉴가 없으므로 공통 오류, `SESSION_*` → 세션 종료 처리, 그 밖 → 공통 오류(로그인 유지, 확인 필요 10) | 인증 | `` `API POST auth logout은 204를 성공으로 본다`() `` |
| `GET /me` | → 200 `Me` {`id`, `accountType`, `nickname`, `timeZone`, `providers[]`, `hasProfileImage`, `space`{`id`, `type`, `myColor`, `partner`{`id`, `nickname`, `color`, `hasProfileImage`}\|null, `togetherColor`, `startDate`, `importPending`}} | 오프라인 → 조용히(캐시 표시) | 인증 | `` `API GET me는 Me와 MeSpace와 MePartner를 읽는다`() `` · `` `API GET me의 개인 공간은 partner와 togetherColor와 startDate가 null이다`() `` |
| `PATCH /me` (`timeZone`만) | `{timeZone}` → 200 `Me`. `nickname`은 보내지 않음(필드 생략, `null` 금지) | `VALIDATION_FAILED`(`timeZone`, `INVALID`) → 디버그 로그만 | 인증 | `` `API PATCH me는 timeZone만 보내고 nickname 필드를 넣지 않는다`() `` |
| `DELETE /me` | 본문 없음 → 204 | `SESSION_*` → 세션 종료 처리, 그 밖 → 공통 오류 | 인증 | `` `API DELETE me는 204를 성공으로 본다`() `` |

- `AuthTokens`: `accessToken`, `accessTokenExpiresAt`(Instant), `refreshToken`(43자), `refreshTokenExpiresAt`(Instant), `isNewUser`. DTO의 `toString`은 토큰 값을 가린다.
- `Me.providers`의 `GOOGLE`·`KAKAO` 밖 값, `accountType`·`space.type`의 모르는 값은 매퍼에서 무시·기본 처리하지 않고 공통 오류로 본다 (문서에 없는 값을 지어내지 않음).
- 오류 문구: 세션이 끝나는 `SESSION_REPLACED`·`SESSION_INVALID`만 화면 스낵바를 띄우지 않는다 (세션 종료 흐름이 처리). `DataError.toMessageResOrNull()`이 이 두 코드에서 `null`("보여 줄 것 없음")을 돌려준다. `TOKEN_EXPIRED`는 갱신에 실패해 화면까지 올라온 경우이므로 공통 오류 문구를 보여 준다 (리뷰 1회차 Major 1).

### D. 토큰 저장 (NFR-SEC-01, MODULES.md 3장 "Tink (Android Keystore)")

| 부분 | 내용 | 테스트 |
| --- | --- | --- |
| 인터페이스 (`domain`) | `AuthTokens`(값 객체, `toString` 가림), `interface TokenStorage { fun current(): AuthTokens?; suspend fun save(tokens); suspend fun clear() }`, `SessionEndReason { REPLACED, INVALID }`. `data:remote`(Authenticator)와 `data:repository`가 같이 쓰려면 둘 다 아는 `domain`에 둔다 (`data:remote`는 `data:local`을 모름) | — |
| 구현 (`data:local`) | `EncryptedTokenStorage`: Tink `Aead`(AES256_GCM). 키셋은 `AndroidKeysetManager`가 SharedPreferences(`duriseo_auth_keyset`)에 두고 Android Keystore 마스터 키(`android-keystore://duriseo_auth_master_key`)로 감싼다. 암호문(Base64)은 별도 DataStore 파일 `auth_tokens`에 저장, associated data = `"duriseo.auth.tokens"`. 직렬화는 kotlinx.serialization JSON | `` `NFR-SEC-01 저장소에는 토큰 평문이 남지 않는다`() `` (가짜 `Aead`로 저장 바이트에 토큰 문자열이 없음) · `` `저장한 토큰을 다시 읽으면 같은 값이다`() `` · `` `clear 뒤에는 토큰이 없다`() `` |
| 메모리 캐시 | OkHttp 스레드가 매 요청 복호화하지 않도록 앱 시작 때 한 번 읽어 `AtomicReference`에 둔다. 저장은 캐시 갱신 후 DataStore에 쓴다 (Authenticator 안에서는 `runBlocking`으로 저장 완료를 기다림) | `` `저장 직후 current는 새 토큰을 돌려준다`() `` |
| 복호화 실패 | Keystore 키 없음·키셋 손상·`GeneralSecurityException` → 저장소를 비우고, 키셋 SharedPreferences(`duriseo_auth_keyset`)와 Keystore 마스터 키를 지워 새로 만들 수 있게 하고, 이전 사용자의 Room·설정도 지우고 로그아웃 상태 (FR-AUTH-01 AC3, 리뷰 1회차 Major 3·4). 저장(`save`) 중 키셋을 못 불러오면 같은 방식으로 한 번 초기화하고 다시 시도 | `` `FR-AUTH-01 AC3 토큰을 복호화할 수 없으면 저장소를 비우고 시작 화면을 보여 준다`() `` |
| 백업 | `allowBackup="true"`, 백업 규칙이 템플릿 그대로라 지금은 모든 파일이 클라우드 백업·기기 이전 대상이다. 토큰·키셋·Room이 새 기기로 옮겨지면 Keystore 키가 없어 복호화가 실패하고, D-02("기기 변경 시 복구 불가")와도 어긋난다 → 결정 3 | 실기기 확인 |

### E. 자동 갱신 (OkHttp `Authenticator`, ARCHITECTURE.md 4장)

| 부분 | 내용 | 테스트 (MockWebServer) |
| --- | --- | --- |
| 클라이언트 둘 | `@NoAuth` OkHttp/Retrofit(`/auth/guest`, `/auth/social`, `/auth/refresh`) · `@Authed` OkHttp/Retrofit(그 밖, `/auth/link`·`/auth/switch` 포함). `@Authed`에 `AuthInterceptor`(`Authorization: Bearer <access>`)와 `TokenAuthenticator` | `` `API 1장 인증 경로는 Bearer access 토큰을 붙인다`() `` · `` `API 1장 auth guest와 social과 refresh에는 Authorization을 붙이지 않는다`() `` |
| refresh 조건 | 401 응답 본문을 `peekBody`로 읽어 `code == TOKEN_EXPIRED`일 때만 refresh. `SESSION_REPLACED`/`SESSION_INVALID`는 refresh 없이 세션 종료 후 `null`. 그 밖의 401(`SOCIAL_TOKEN_INVALID`)은 `null` | `` `API 1장 401 TOKEN_EXPIRED면 refresh 후 원래 요청을 다시 보낸다`() `` · `` `API 1장 401 SESSION_INVALID면 refresh 하지 않고 세션을 끝낸다`() `` |
| 한 번만 | `synchronized` 잠금 안에서: 요청에 실린 access가 저장된 access와 다르면(다른 스레드가 이미 갱신) 저장된 것으로 다시 보냄. 같으면 refresh를 동기 호출(`execute()`), 성공하면 저장 후 새 access로 다시 보냄. 같은 요청이 두 번째 401이면(`priorResponse`) 포기 | `` `API 1장 동시에 만료된 요청 세 개는 refresh를 한 번만 부른다`() `` (`requestCount` 확인) · `` `API 1장 refresh 뒤에도 401이면 다시 갱신하지 않는다`() `` |
| refresh 실패 | 401 `SESSION_REPLACED` → `SessionManager.end(REPLACED)`. 401 `SESSION_INVALID`(이전 refresh 재사용, 만료 포함) → `end(INVALID)`. `IOException` → `IOException`을 그대로 던져 원래 호출이 `Offline`(토큰 유지). 그 밖(5xx) → `null` (원래 401이 `ApiError(TOKEN_EXPIRED)`로) | `` `API 1장 refresh가 네트워크 오류면 토큰을 지우지 않고 Offline이다`() `` |
| 토큰 없음 | 로그아웃 상태에서 인증 경로를 부르면 헤더 없이 보내고 401은 그대로 실패 (세션 종료 알림 없음) | `` `토큰이 없으면 401에 세션 종료를 알리지 않는다`() `` |

### F. 세션 끊김·로그아웃·탈퇴 (API 1장, SCREENS 공통 "다른 기기 로그인")

- `SessionManager` (`data:repository`, 싱글턴, 앱 범위 코루틴): `end(reason)` 한 번만(이미 끝났으면 무시) → `TokenStorage.clear()` → `DuriseoDatabase.clearAllTables()` → 사용자 DataStore 비우기 → 소셜 SDK 상태 정리(확인 필요 11) → `sessionEvents` 방출. `sessionEvents`는 버퍼가 있는 `Channel` 기반 `Flow`라 구독자가 없을 때 낸 이벤트도 사라지지 않는다 (리뷰 1회차 Minor).
- 같은 정리 함수를 로그아웃(204 뒤), 탈퇴(204 뒤), switch(새 토큰 저장 전 게스트 데이터 정리)가 쓴다. 이때는 이벤트 대신 각 ViewModel이 시작 화면/캘린더로 보낸다.
- `feature:main`의 `MainViewModel`이 `authState`(로그인 여부)와 `sessionEvents`를 구독: 끊기면 `navigate(Start) { popUpTo(graph) { inclusive = true } }`, `REPLACED`면 시작 화면 인자 `sessionReplaced = true` → 시작 화면이 스낵바 "다른 기기에서 로그인해서 로그아웃됐어요"(위에서 40). `INVALID`는 안내 없음.
- 앱 시작 분기: 토큰 저장소를 읽는 동안은 빈 `surface` 화면(문구 없음) → 토큰 있음: 캘린더 + 뒤에서 `GET /me`·시간대 갱신, 없음: SCR-START-01. 뒤로 가기로 시작 화면 ↔ 캘린더를 오가지 않게 로그인·로그아웃 이동은 백스택을 비운다.

### G. 소셜 SDK

| 부분 | 내용 |
| --- | --- |
| 위치 | 시작 화면(`feature:auth`)과 계정 화면(`feature:settings`)이 함께 쓴다. feature끼리 의존할 수 없어 `core:common`의 `social` 패키지에 `SocialTokenProvider`(Activity를 받아 `SocialTokenResult.Success(provider, token)`/`Cancelled`/`Failed`)를 둔다 → 확인 필요 16 |
| Google | Credential Manager `GetSignInWithGoogleOption.Builder(serverClientId = AppConfig.googleWebClientId)` (버튼으로 시작하는 흐름) → `CredentialManager.getCredential(activity, request)` → `CustomCredential`이고 type이 `GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL`이면 `GoogleIdTokenCredential.createFrom(data).idToken`. `GetCredentialCancellationException` → `Cancelled`. `NoCredentialException`·그 밖 → `Failed` (확인 필요 6). 서버는 `aud` = 웹 클라이언트 ID로 확인하므로(API 4.2) `serverClientId`가 같은 값이어야 한다. `googleWebClientId`가 비면(CI) `Failed` |
| 카카오 | `KakaoSdk.init(appContext, AppConfig.kakaoNativeAppKey)`를 `MyApp`에서 (키가 비면 건너뜀). `UserApiClient.isKakaoTalkLoginAvailable` → `loginWithKakaoTalk`, 사용자가 취소(`ClientErrorCause.Cancelled`)면 `Cancelled`, 그 밖 실패면 `loginWithKakaoAccount`로 다시. 서버에는 `OAuthToken.accessToken`. 콜백 API는 `suspendCancellableCoroutine`으로 감싼다 |
| 매니페스트 | `app/build.gradle.kts`에서 `manifestPlaceholders["kakaoNativeAppKey"] = localProperty("duriseo.kakao.nativeAppKey")`. `app` 매니페스트에 `com.kakao.sdk.auth.AuthCodeHandlerActivity`(exported, `VIEW`/`DEFAULT`/`BROWSABLE`, `android:scheme="kakao${kakaoNativeAppKey}"`, `android:host="oauth"`). 키가 비면 scheme이 `kakao`인 채로 빌드된다 (CI, 기능만 안 됨). 키 값은 레포에 쓰지 않는다 |
| 저장소 | 카카오 SDK는 `https://devrepo.kakao.com/nexus/content/groups/public/`에 있다. `settings.gradle.kts`에 `maven(url) { content { includeGroup("com.kakao.sdk") } }` 추가 (위험 1) |
| switch용 토큰 | 연동 409 뒤 다이얼로그 ①·② 동안 ViewModel 메모리에만 소셜 토큰을 둔다 (저장·로그 없음). 화면을 벗어나면 버린다 |

### H. 내 정보 캐시 (첫 Room 엔티티)

- 화면은 Room만 구독한다 (ARCHITECTURE 2장). 전체 메뉴·계정 화면은 `Me`가 필요하므로 단위 2에서 **첫 `@Database`**를 만든다 (unit-1 결정 3).
- `DuriseoDatabase`(version 1, `exportSchema = true`, 스키마 `data/local/schemas/` 커밋), `MeEntity`(한 행, 테이블 `me`): `id`, `account_type`, `nickname`, `time_zone`, `providers`(쉼표 문자열, TypeConverter), `has_profile_image`, `space_id`, `space_type`, `my_color`, `partner_id`, `partner_nickname`, `partner_color`, `partner_has_profile_image`, `together_color`, `start_date`(TEXT `YYYY-MM-DD`), `import_pending`. `MeDao`: `observe(): Flow<MeEntity?>`, `upsert`, `clear`.
- 출시 전이라 스키마가 바뀌면 버전을 올리고 destructive fallback 허용 (add-api Skill 4장).
- `domain`: `Me`, `AccountType`, `SocialProvider`, `MeSpace`, `SpaceType`, `MePartner`, `interface MeRepository { observeMe(); refreshMe(); syncTimeZone(zoneId) }`, `interface AuthRepository { authState; sessionEvents; startAsGuest(); signIn(provider, token); link(provider, token); switchAccount(provider, token); logout(); withdraw() }`. 로그인 결과는 `LoginResult(isNewUser)`.
- 테스트: (Repo) `` `GET me 결과를 Room에 저장하면 observeMe가 새 값을 낸다`() `` · `` `NFR-OFF-01 오프라인이면 refreshMe는 조용히 실패하고 저장된 내 정보는 그대로다`() `` — Room 메모리 DB는 Robolectric이 필요하다 (확인 필요 15).

### I. 모듈·내비게이션

- 새 모듈 `feature:auth` (`/new-feature-module`, MODULES.md 1장 "있음"으로). `feature:main`이 의존해 NavHost에 연결.
- 경로: `core:common` `navigation`에 `AuthRouteModel.Start(sessionReplaced: Boolean = false)`, `SettingsRouteModel.Account`. 전체 메뉴는 기존 `MainRouteModel.Settings`.
- 전체 메뉴 진입: 캘린더 앱바(단위 4) → 확인 필요 3.

## 서버 연동

| API | 개발 서버 배포 여부 | 확인 |
| --- | --- | --- |
| `POST /auth/guest`, `/auth/social`, `/auth/link`, `/auth/switch`, `/auth/refresh`, `/auth/logout`, `GET /me`, `PATCH /me`, `DELETE /me` | Server 단위 1 머지됨(`751692c`, PR #6). 개발 서버 배포는 사용자 확인으로 "배포됨". 다만 Server `docs/PLAN.md`는 "배포 1: 대기"로 아직 바뀌지 않았다 | 단위 테스트는 MockWebServer. 맥에서 debug 빌드로 실제 호출 확인 (게스트 시작 → 앱 재실행 → Google·카카오 로그인 → 연동 409 → switch → 두 기기 `SESSION_REPLACED` → 로그아웃 → 탈퇴) |
| `PUT /me/devices` | 서버 있음 | 단위 7 |

## 라이브러리 (MODULES.md 3장 단위 2분 + 필요한 것)

이 세션은 Maven 저장소에 닿지 않아 버전을 직접 확인하지 못했다. 아래는 2025년에 나온 것이 확실한 안정 버전이고 모두 **CI로 확인**한다.

| 용도 | 좌표 | 버전 | 저장소 | 모듈 | 비고 |
| --- | --- | --- | --- | --- | --- |
| 토큰 암호화 | `com.google.crypto.tink:tink-android` | 1.18.0 | Maven Central | `data:local` | CI로 확인 (안 되면 1.17.0) |
| Credential Manager | `androidx.credentials:credentials` | 1.5.0 | Google Maven | `core:common` | CI로 확인 |
| Credential Manager (Play 서비스) | `androidx.credentials:credentials-play-services-auth` | 1.5.0 | Google Maven | `core:common` | CI로 확인. Android 13 이하 기기에 필요 |
| Google ID | `com.google.android.libraries.identity.googleid:googleid` | 1.1.1 | Google Maven | `core:common` | CI로 확인 |
| 카카오 로그인 | `com.kakao.sdk:v2-user` | 2.21.0 | devrepo.kakao.com | `core:common`, `app`(init) | CI로 확인. 저장소 추가 필요 (위험 1) |
| (확인 필요 15) Room JVM 테스트 | `org.robolectric:robolectric` | 4.15.1 | Maven Central | `data:local`, `data:repository` `testImplementation` | CI로 확인. compileSdk 36을 못 돌리면 `@Config(sdk = [35])` |
| (확인 필요 15) 테스트 Context | `androidx.test:core` | 1.6.1 | Google Maven | 같음 | CI로 확인 |

릴리스 R8 규칙(Credential Manager·Tink·카카오)은 출시 단위에서 `minifyEnabled`를 켤 때 함께 넣는다.

## 결정

Android 레포 안에서 정할 수 있는 것. 리드가 범위 확인 때 함께 승인한다.

1. **토큰 인터페이스 위치**: `TokenStorage`·`AuthTokens`·`SessionEndReason`은 `domain`(순수 Kotlin). `data:remote`의 Authenticator와 `data:repository`가 같이 쓰고, `data:remote`는 `data:local`을 모르기 때문이다. 구현은 `data:local`, 바인딩은 `data:local` Hilt 모듈.
2. **로그인 상태의 기준**: "토큰 저장소에 토큰이 있다" = 로그인 상태. `Me` 캐시는 표시용이고 로그인 판단에 쓰지 않는다 (오프라인 재실행에서도 캘린더로).
3. **백업 끄기**: `android:allowBackup="false"`, `data_extraction_rules.xml`의 `cloud-backup`·`device-transfer`에서 모든 도메인 제외. 이유: Keystore 키는 백업되지 않아 복원된 토큰은 어차피 못 쓰고, D-02·FR-AUTH-01 AC3(재설치·기기 변경 시 복구 불가)와 맞춘다. 회원 복구는 로그인으로 한다.
4. **로그인 3종·연동·로그아웃·탈퇴는 쓰기**: `runOnlineWrite`로 감싸 오프라인이면 요청 없이 "인터넷 연결이 필요해요" (NFR-OFF-02). 시간대 `PATCH`와 `GET /me`는 사용자 동작이 아니라 조용히 실패.
5. **시간대 갱신 시점**: 로그인 상태로 앱 프로세스가 시작될 때 1번 + 로그인·switch 직후. `Me.timeZone`과 같으면 보내지 않는다. 앱 실행 중 시간대 변경(`ACTION_TIMEZONE_CHANGED`)은 다음 실행에 반영.
6. **세션 끊김 문구 위치**: SCR-START-01은 위에서 40(`Main.dc.html` 51행, design-system "스낵바" 위치). 시작 화면의 다른 스낵바(오프라인·공통 오류)도 같은 자리에 둔다 (확인 필요 6에서 다르게 정하면 따른다).
7. **로그아웃 확인 다이얼로그 재사용**: 단위 3의 SCR-COUPLE-01 "다른 계정으로 로그인"도 같은 다이얼로그(D-80)라, 문구와 `LogoutConfirmDialog`를 `core:designsystem`에 둔다 (feature끼리 공유할 수 없어서).

## 리뷰 기록

| 회차 | Critical | Major | 비고 |
| --- | --- | --- | --- |
| 1 | 0 | 7 | Minor 9. 아래 "리뷰 1회차 처리". CI 202는 리뷰 전 상태에서 통과, 수정분은 CI 재확인 대기 |

### 리뷰 1회차 처리

| 항목 | 처리 |
| --- | --- |
| Major 1 `TOKEN_EXPIRED` 문구 | 세션 종료 코드(`SESSION_REPLACED`·`SESSION_INVALID`)만 문구 없음. `TOKEN_EXPIRED`는 공통 오류. `` `ErrorMessagesTest` `` 수정 |
| Major 2 진입 시 `GET /me` | `SettingsViewModel`·`AccountViewModel`이 `init`에서 `refreshMe()`. 실패는 조용히. 테스트 `FR-SET-01 전체 메뉴에 들어오면…`, `FR-AUTH-03 계정 화면에 들어오면…` |
| Major 3 이전 사용자 데이터 | 복호화 실패 시 `clearUserData()`, 시작 때 토큰 없으면 `clearUserData()`, 로그인·switch는 토큰 저장 전에 정리하고 정리 실패 시 로그인 실패. `EncryptedTokenStorageTest`·`SessionStartupTest`·`AuthRepositoryTest` 테스트 추가 |
| Major 4 키셋 초기화 | 복호화·키셋 로드 실패 시 `duriseo_auth_keyset` SharedPreferences와 Keystore 마스터 키를 지우고 새로 만든다. **실기기 확인 필요**(JVM 테스트는 가짜 `Aead`와 훅만 확인) |
| Major 5 소셜 SDK 위치 | ViewModel에서 Route로 이동 (G절, "구현 중 정한 것 2부"). `StartViewModel`·`AccountViewModel` 생성자에서 `SocialTokenProvider` 제거 |
| Major 6 다이얼로그 접근성 | `DuriseoDialogLayer` (2부 결정). **실기기 TalkBack 확인 필요** |
| Major 7 토큰에 없는 값의 출처 | `NoteTopGap 4`·`SmallLineHeight 19.2` = `Account.dc.html` 41행(`margin: 4px 0 0`, `line-height: 1.6`), `ContentTop 4`(메뉴) = `Menu.dc.html` 24행(`padding: 4px 20px 0`). 모두 원본에 있어 유지하고 코드 주석에 출처를 적었다. **design-system 명세에 추가 필요(Docs 후속)** |
| Minor 스크롤 | 시작·계정 화면 세로 스크롤 |
| Minor `GuestId` | `combinedClickable(onClick = {})` 제거, 길게 누르기에 접근성 동작 이름 "복사" (SCREENS에 없음, Docs 후속) |
| Minor 스낵바 4초 | `SnackbarDuration.Short` |
| Minor `SessionManager.events` | 버퍼 `Channel` 기반. 테스트 `…구독 전에 낸 세션 종료 이벤트도 구독하면 받는다` |
| Minor 문서 | 이 파일, `ARCHITECTURE.md` 1장, `PLAN.md` 단위 8(FR-SET-02) |

### Docs 후속 (이 레포에서 고치지 않음)

- design-system: 계정 화면 안내 문구 위 여백 4와 작은 글 12/400 행간 1.6(19.2), 전체 메뉴 본문 위 여백 4.
- SCREENS SCR-SET-01: 게스트 ID 길게 누르기의 접근성 동작 이름("복사").

### 남은 일

- 첫 빌드가 만든 Room 스키마 JSON(`data/local/schemas/`)을 커밋한다.
- FR-SET-02(정보: 문의하기·약관·오픈소스 라이선스·앱 정보 카드)는 단위 8에서 만든다 (`PLAN.md` 단위 8에 적었다).
- 실기기 확인: 키셋 초기화(Major 4), TalkBack 포커스(Major 6), Google·카카오 로그인.

## 위험

1. **카카오 SDK 저장소**: CI(GitHub Actions)에서 `devrepo.kakao.com`에 닿아야 한다. 카카오 SDK가 끌고 오는 OkHttp·Retrofit 버전이 단위 1(OkHttp 4.12.0, Retrofit 2.11.0)과 달라 해석 충돌이 날 수 있다 → CI에서 `dependencies`로 확인.
2. **실기기에서만 확인되는 것**: Credential Manager·카카오톡 로그인·Keystore는 JVM 테스트로 확인할 수 없다. 가짜 `SocialTokenProvider`·가짜 `Aead`로 로직만 테스트하고, 맥 실기기 확인 목록을 PR에 적는다.
3. **Authenticator 동시성**: 잠금 안에서 동기 refresh와 저장(`runBlocking`)을 한다. 메인 스레드에서 OkHttp를 부르지 않는 한 문제없지만, 요청이 많을 때 잠금 대기가 길어질 수 있다 (refresh 한 번 시간만큼).
4. **Keystore 오류 기기**: 일부 기기는 Keystore 키 생성·사용이 실패한다. 이때는 토큰을 저장할 수 없어 매번 시작 화면이 된다 (게스트는 데이터를 잃음). 실패를 디버그 로그에 남기고 별도 대체 저장(평문)은 하지 않는다 (NFR-SEC-01).
5. **로그인 버튼 브랜드 가이드**: 보드의 [카카오로 계속하기](흰 바탕 아웃라인)와 [Google로 계속하기](검정 채움)는 카카오 로그인 버튼 디자인 가이드(노란 `#FEE500` + 말풍선 심볼)·Google 로그인 브랜딩 가이드와 다르다. 카카오 비즈 앱 검수·Play 심사에서 지적될 수 있다 (사람 판단).
6. **Google ID 토큰 수명**: switch는 연동 409 때 받은 토큰을 다시 쓴다. 다이얼로그에서 오래 머물면 토큰이 만료돼 `SOCIAL_TOKEN_INVALID`가 날 수 있다 (API 4.7은 다시 받아도 된다고 함 → 확인 필요 6의 처리안).

## 사람이 할 일

1. **카카오 Developers** (앱 설정 › 플랫폼 › Android): 패키지명 `com.duriseo.android`, **키 해시** 등록 — debug 키스토어 키 해시(`keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android | openssl sha1 -binary | openssl base64`). 출시 전 업로드 키·Play 앱 서명 키의 키 해시도 추가.
2. **카카오 로그인 활성화와 동의항목**: 카카오 로그인 ON, 동의항목 "닉네임"(`profile_nickname`)만 켬 (선택 동의도 됨 — 거부하면 서버가 "사용자", D-69). 이메일·전화번호 등은 끔 (NFR-PRIV-01). OpenID Connect는 쓰지 않음.
3. **Google**: config대로 Android OAuth 클라이언트(패키지 `com.duriseo.android` + debug SHA-1)가 등록돼 있다. 같은 Google Cloud 프로젝트의 **웹 클라이언트 ID**가 `duriseo.google.webClientId`이고 서버의 `aud` 설정과 같은 값인지 확인. 출시 때 release·Play 앱 서명 SHA-1 클라이언트 추가.
4. **`local.properties`**: `duriseo.api.devUrl`, `duriseo.google.webClientId`, `duriseo.kakao.nativeAppKey` (값은 claude.ai 프로젝트 `config/environment.md`). 새 키는 없다.
5. **맥 실기기 확인** (CI 밖): Google 계정 있는 기기로 Google 로그인, 카카오톡 설치·미설치 두 경우, 두 기기로 같은 계정 로그인 → 이전 기기 `SESSION_REPLACED` 안내, 앱 삭제 후 재설치 → 시작 화면(FR-AUTH-01 AC3), 카카오 회원 탈퇴 뒤 카카오 계정의 연결된 서비스에서 둘이서가 사라지는지.
6. 아래 확인 필요에 답하고 범위가 맞으면 **start**.

## 확인 필요 해결 (PRD v0.8)

아래 "확인 필요" 번호는 Docs v0.8 반영 또는 리드 결정으로 해결됐다. 구현은 이 결정을 따른다.

| 번호 | 해결 | 근거 |
| --- | --- | --- |
| 1 | `Me`에 `createdAt`(일시, 가입일) 추가. 앱이 기기 시간대 날짜로 바꿔 보여 줌 | Docs v0.8, API 3.1 |
| 5 | Docs v0.8에 반영돼 해결 (문구는 SCREENS.md 기준) | Docs v0.8 |
| 6 | 요청 중은 버튼 비활성만, 공통 오류 스낵바(소셜 SDK 실패·`SOCIAL_TOKEN_INVALID`·`GUEST_ONLY`·`GUEST_NOT_ALLOWED` 포함), 소셜 로그인 취소는 안내 없음, 스낵바는 위에서 40 | Docs v0.8, D-88, SCREENS 공통 표 |
| 7 | Docs v0.8에 반영돼 해결 (수치는 design-system·tokens.json 기준, 2부) | Docs v0.8 |
| 8 | Docs v0.8에 반영돼 해결 (로고는 2부, Docs `design/logo/` 기준) | Docs v0.8 |
| 10 | 로그아웃·탈퇴는 온라인에서만. 오프라인이면 "인터넷 연결이 필요해요", 서버 오류는 공통 오류 후 로그인 상태 유지, `SESSION_*`면 로컬 정리 후 시작 화면, 204면 소셜 SDK 상태도 지움 | D-86, API 3.1 |
| 11 | 로그아웃·탈퇴·세션 끊김 때 Kakao SDK 로그아웃과 Credential Manager 상태 지우기, 실패는 무시 | API 1장 |
| 12 | Docs v0.8에 반영돼 해결 (2부 화면에서 SCREENS.md대로) | Docs v0.8, FR-SET-01 |
| 13 | Docs v0.8에 반영돼 해결 (2부) | Docs v0.8 |
| 2 | 신규 회원·`importPending` 분기 결과는 만들되, 단위 3 전까지 캘린더로 보냄 | 리드 결정 |
| 3 | 캘린더 자리 화면 위에 임시 [전체 메뉴] 진입 버튼 | 리드 결정 |
| 4 | SCR-SET-01은 계정 부분만 | 리드 결정 |
| 9 | 약관 링크는 모양만, 누르면 동작 없음 | 리드 결정 |
| 14 | Server 단위 1은 개발 서버에 배포됨 | 리드 결정 |
| 15 | Robolectric(`org.robolectric:robolectric`)과 `androidx.test:core`를 `testImplementation`으로 추가 | 리드 결정 |
| 16 | 소셜 로그인 코드는 `core:common`의 `social` 패키지 | 리드 결정 |

구현 중 정한 것 (1부)

- 토큰 값 객체 `AuthTokens`는 네 필드(access·refresh와 각 만료)이고, `isNewUser`는 응답을 감싼 `IssuedTokens`/`LoginResult`가 가진다.
- `TokenStorage`는 `current()` 말고 `state: StateFlow<TokenState>`(Loading·Empty·Present)와 `restore()`를 가진다. "아직 읽는 중"과 "토큰 없음"을 구분해야 앱 시작 때 빈 화면 → 분기가 되기 때문이다.
- `SessionEndNotifier`(domain)로 `data:remote`의 Authenticator가 `data:repository`의 `SessionManager`를 부른다. `data:remote`는 `data:repository`를 모른다.
- `AuthApi`는 인증 없는 클라이언트용(guest·social·refresh), `AuthSessionApi`는 인증 클라이언트용(link·switch·logout)으로 나눴다. 잘못된 클라이언트로 부르는 실수를 막으려는 것이다.
- Retrofit/DTO는 `data:remote`의 `AuthRemoteDataSource`·`MeRemoteDataSource`가 감싸 도메인 모델만 `data:repository`로 나간다. Room도 `data:local`의 `MeLocalDataSource`·`LocalDataCleaner`가 감싼다.
- 시간대는 `Clock`(`Clock.systemDefaultZone()`)의 zone으로 비교한다. 프로세스가 도는 동안 기기 시간대를 바꾸면 다음 실행에 반영된다 (결정 5).
- Room 스키마 JSON(`data/local/schemas/`)은 첫 CI/맥 빌드가 만든다. 만들어지면 커밋한다 (이 환경은 Gradle을 못 돌려 아직 없다).
- 1부 검증은 CI에서만 한다 (이 환경은 Google Maven에 닿지 않는다). 라이브러리 버전과 `AGP 9` 호환(Tink, Credential Manager, Kakao `v2-user`, Robolectric `sdk = [35]`)은 CI 결과로 확인한다.
- `DataError.toMessageResOrNull()`("보여 줄 것 없음" 도우미, C절)은 2부에서 더했고 `SESSION_*` 두 코드만 `null`이다.
- 이전 사용자의 데이터는 새 토큰을 저장하기 **전에** 지운다 (리뷰 1회차 Major 3). 로그인(게스트 시작·소셜)·switch 모두 서버 성공 → Room·설정 정리(`clearUserDataOrFail`) → 토큰 저장 → `GET /me`·시간대 순서다. 정리가 실패하면 토큰을 저장하지 않고 공통 오류(`INTERNAL_ERROR`)로 로그인을 실패시킨다. 이 순서는 토큰 저장소가 비어 있는 동안 `authState`가 `SignedOut`이라 화면이 바뀌지 않으므로 시작 화면으로 튀지 않는다 (switch는 게스트 토큰이 그대로 남아 있다).
- 앱 시작 때 토큰이 없으면 `SessionStartup`이 남은 Room·설정을 지운다 (복호화 실패 등으로 토큰만 사라진 경우의 보험).
- 위험: switch에서 서버는 이미 계정을 바꿨는데 로컬 정리만 실패하면 화면에는 오류가 보이고 앱은 이전 게스트 토큰을 들고 있다. 다시 시도하면 서버가 같은 토큰으로 switch를 받아 주므로 복구된다.

구현 중 정한 것 (2부)

- 화면 이동은 `authState` 변화가 아니라 **명시한 곳**에서 한다: 앱 시작 분기와 세션 끊김은 `feature:main`의 `MainViewModel`(`start`, `effects`), 로그인·로그아웃·탈퇴·switch는 각 화면 ViewModel의 SideEffect → `MainNavigator.navigateToStart/Home`(백스택을 모두 비움). 세션 끊김 이벤트는 구독자가 없으면 사라지므로 `MainViewModel`이 만들어지자마자 구독해 `Channel`에 쌓는다.
- 소셜 SDK에는 Activity가 필요한데 ViewModel이 Activity를 들고 있으면 안 되므로, ViewModel이 SideEffect `RequestSocialToken(provider)`를 내보내면 **Route**가 `rememberCoroutineScope`와 현재 Activity(`Context.findActivity()`)로 `SocialTokenProvider.requestToken`을 부르고 결과는 `SocialTokenReceived` 액션으로 돌아온다 (리뷰 1회차 Major 5). ViewModel은 SDK·Activity를 모른다. 화면이 사라져 SDK 호출이 취소되면 `Cancelled`를 돌려줘 요청 중 상태가 남지 않게 한다. `SocialTokenProvider`는 Hilt EntryPoint(`Context.socialTokenProvider()`, `core:common`)로 Route가 얻는다. 단위 테스트는 액션으로 결과를 넣는다.
- 계정 화면·전체 메뉴는 들어올 때 `refreshMe()`를 한 번 부른다 (오프라인이면 조용히 실패, 화면은 Room 값 그대로. 리뷰 1회차 Major 2).
- 시작 화면 스낵바는 Route가 `SnackbarHostState`로 띄우고(`SnackbarDuration.Short`, 위에서 40) Screen에는 `snackbarHost` 슬롯으로 넘긴다. Preview에서는 정적 스낵바를 슬롯에 넣는다. 전체 메뉴·계정 화면의 스낵바는 `MainScreen`의 공통 스낵바(`onShowSnackBar`)를 쓴다.
- 비활성 버튼은 면 `line-100`(tokens.json), 글자색은 문서에 없어 `ink-600`으로 했다.
- 다이얼로그는 시스템 창(Compose `Dialog`)이 아니라 화면 위 오버레이로 그려 뒤 막이 `scrim` 토큰 그대로이고 Preview에도 보인다. 접근성은 `DuriseoDialogLayer`가 맡는다: 다이얼로그가 떠 있는 동안 뒤 화면을 `clearAndSetSemantics {}`로 TalkBack 탐색에서 빼고, 패널은 `paneTitle`(제목)과 `isTraversalGroup`을 갖고, 열릴 때 `FocusRequester`로 패널에 포커스를 준다 (리뷰 1회차 Major 6). `dialog()` semantics는 쓰지 않았다. **실기기 TalkBack 확인 필요.**
- 계정 화면 게스트 ID는 짧게 눌러서는 아무 일도 없다. 길게 누르기만 `pointerInput`과 `semantics { onLongClick(label = "복사") }`로 달았다. 동작 이름 "복사"는 SCREENS에 없어 strings.xml에 두었다 (Docs 후속).
- 시작 화면·계정 화면(게스트·회원)은 세로로 스크롤한다 (큰 글자·낮은 화면에서 잘리지 않게). 스크롤 안에서는 `weight`를 못 쓰므로 `BoxWithConstraints`의 높이를 최소 높이로 잡고 `SpaceBetween`으로 바닥 덩어리를 아래에 붙인다.
- 시작 화면의 위 96·스낵바 위 40은 시스템 바 안쪽(Scaffold `padding`)을 기준으로 둔다. 보드에는 시스템 바가 없다.
- `lifecycle-runtime-compose`는 MODULES.md 3장에 없어 `collectAsStateWithLifecycle` 대신 `collectAsState`를 쓴다.
- 전체 메뉴는 앱바·게스트 배너·"설정" 섹션의 계정 행·로그아웃만 그린다. 프로필 카드·꾸미기·알림 설정·연결 정보(단위 3·7·8)와 정보 섹션·앱 정보 카드(어느 단위에도 없음, 확인 필요 4)는 그리지 않는다.
- 캘린더 자리표시 화면의 앱바 자리에 임시 [전체 메뉴] 아이콘 버튼(접근성 "전체 메뉴")만 두었다. 단위 4의 앱바가 대신한다.

## 확인 필요

문서끼리 다르거나 비어 있던 곳. 1~16 모두 위 "확인 필요 해결" 표대로 해결됐다. Docs 수정은 Docs 레포 `/prd-sync`·`/design-sync`로.

1. (해결됨) **가입일 필드가 없음 (막힘)**: SCREENS SCR-SET-01 "계정 · 회원 … 로그인 방식, 가입일"과 `SCR-SET-01--account-member.png`·`Account.dc.html`("2026년 10월 12일")에는 가입일이 있는데, API.md 3.1 `Me` 표에는 가입 일시 필드가 없다 (ERD `users.created_at`은 있음). A) `/prd-sync`로 `Me`에 `createdAt`(일시) 추가 + 서버 변경 (추천), B) SCREENS에서 가입일 행 삭제, C) 단위 2는 행을 숨기고 A를 기다림. 날짜 표기 형식("2026년 10월 12일", 기기 시간대 기준)도 SCREENS에 적어야 한다.
2. (해결됨) **신규 소셜 회원의 첫 화면**: FR-AUTH-02 AC1·D-31·SCREENS SCR-START-01 "신규 소셜 회원 → SCR-COUPLE-01"인데 SCR-COUPLE-01은 단위 3이다. 기존 회원의 `importPending`(→ SCR-COUPLE-04)도 같다. A) 단위 2는 분기 결과(`NewMember`)까지 만들고 캘린더로 보낸 뒤 단위 3에서 연결 (추천), B) SCR-COUPLE-01 회원 화면을 단위 2로 당김(카드가 가리킬 SCR-COUPLE-02·03은 없음).
3. (해결됨) **전체 메뉴로 들어갈 길이 없음**: 진입은 SCR-CAL-01 앱바의 전체 메뉴 버튼(단위 4)뿐이라 단위 2의 연동·로그아웃·탈퇴를 실기기에서 누를 수 없다. A) 단위 2에서 캘린더 자리표시 화면 위에 앱바의 [전체 메뉴] 아이콘 버튼만 둠 (`Calendar.dc.html` 45행, 접근성 "전체 메뉴", 추천), B) debug 빌드 전용 진입, C) Preview·테스트만 하고 실기기 확인은 단위 4.
4. (해결됨) **SCR-SET-01을 어디까지 그릴지**: 단위 2는 앱바, 게스트 배너, "설정" 섹션의 계정 행, 로그아웃과 다이얼로그만 그리고 나머지(프로필 카드·우리 색상·연결 정보 → 단위 3, 알림 설정 → 7, 프로필 → 8)는 그 단위에서 더하는 안. 또 **FR-SET-02 정보(문의하기, 약관, 오픈소스 라이선스, 앱 정보 카드·인앱 업데이트)는 Android `docs/PLAN.md` 어느 단위에도 없다** → 어느 단위에 넣을지 (출시 단위 또는 단위 8 추천, PLAN은 이 PR에서 고칠 수 있음).
5. (해결됨) **보드·이미지에만 있고 SCREENS.md에 없는 문구** (CONVENTIONS "없는 문구는 만들지 않는다"): 기능 칩 "쉐어 캘린더"·"공지사항"·"메모장"(SCREENS는 "기능 칩 3개"와 "공지사항" 유지만 적음), 계정 · 게스트 배너 본문 "앱을 지우거나 휴대폰을 바꾸면 일정과 메모를 다시 찾을 수 없어요. 로그인하면 지금까지의 기록이 계정으로 그대로 옮겨져요."(전체 메뉴 배너 본문과 다름, SCREENS는 "안내 배너"만), 섹션 제목 "로그인 연결"·"설정", 계정 행 값 "Google 로그인"(카카오 회원 값은 보드에도 없음, "카카오 로그인"?), "게스트 ID", "로그인 방식", 로그인 방식 값 "Google"/"카카오", "가입일", 접근성 "뒤로 가기"·"닫기"·로고 "두 원이 겹친 둘이서 로고", 연동 성공 안내 "로그인했어요. 게스트 기록을 계정으로 옮겼어요."(`Account.dc.html` `justLinked`, 보일지 자체가 SCREENS에 없음). A) SCREENS가 요소를 가리키는 곳은 보드 문구를 그대로 쓰고 Docs에 반영 (추천), B) `/prd-sync`로 SCREENS에 먼저 적은 뒤 구현.
6. (해결됨) **문서에 없는 상태·오류 처리** (SCREENS SCR-START-01·SCR-SET-01에 상태 없음): (a) 로그인·연동 요청 중 표시 — 안: 버튼 비활성만, 문구·스피너 없음. (b) Google 계정이 기기에 없음(`NoCredentialException`)·SDK 실패 — 안: 공통 오류 스낵바. (c) `SOCIAL_TOKEN_INVALID`·`GUEST_ONLY`·`GUEST_NOT_ALLOWED` — 안: 공통 오류. (d) switch의 404 `SOCIAL_NOT_REGISTERED`("앱은 `/auth/link`로 연동하면 된다", API 4.7 3단계) — 안: 같은 토큰으로 `POST /auth/link`를 한 번 자동으로 보냄 vs 공통 오류. (e) switch의 `SOCIAL_TOKEN_INVALID`(토큰 만료) — 안: 소셜 SDK에서 토큰을 한 번 다시 받아 재시도. (f) 시작 화면의 오프라인·공통 오류 스낵바 위치 (결정 6).
7. (해결됨) **토큰에 없는 크기 값** (보드 원본): 시작 — 버튼 높이 54(design-system "버튼 56/48/44"와 다름), 위 여백 96, 로고 112×72, 묶음 간격 28, 소개 17/400 행간 1.55(`title-md`는 17/700), 칩 안쪽 6·12와 13/500(`label`은 13/600), 작은 글 12/400 행간 1.6(`caption`은 12/500/16), 게스트 아이콘 20. 계정·메뉴 — 버튼 54, 배너 안쪽 18·아이콘 22·본문 13 행간 1.6, 메뉴 행 높이 56·52, 섹션 제목 13/600, 로그인 카드 안쪽 20·체크 선 2.2, 정보 행 높이 52, 다이얼로그 안쪽 24/20/20·제목 아래 간격 8·버튼 위 16. A) 단위 1처럼 Docs 보충 PR(design-system에 버튼·배너·메뉴 행·다이얼로그·시작 화면 수치)을 먼저 (추천), B) 보드 값을 이름 붙인 상수로(단위 1 스낵바 방식). 54는 `size-button-lg` 56으로 맞출지도.
8. (해결됨) **로고 파일 없음**: design-system "로고"는 `Logos` 그룹의 `duriseo-mark.svg`를 말하지만 Docs에 파일이 없다. 보드 인라인 SVG는 두 가지(`Main.dc.html` 25~30행 112×72 클립 방식, `Menu.dc.html` 앱 정보 36×25 path 방식). 겹친 부분 색은 `brand-500`. Docs `design/icons/`에 넣을지(탭 아이콘처럼), 보드 SVG를 VectorDrawable로 옮겨도 될지.
9. (해결됨) **약관 링크 주소**: FR-AUTH-08 AC2·Q-03 미정. 안: 단위 2는 링크 모양만 두고 누르면 아무 동작 없음, 주소가 정해지면 설정값으로 추가.
10. (해결됨) **로그아웃·탈퇴 실패 처리**: FR-AUTH-05 AC1은 "확인하면 지워지고 이동"만 적었다. 안: 오프라인이면 실행하지 않고 "인터넷 연결이 필요해요", 서버 오류(5xx)면 공통 오류 후 그대로(서버의 `devices` 삭제(D-50)가 보장되도록), 응답이 `SESSION_*`면 이미 끊긴 세션이므로 로컬 정리 후 시작 화면. "서버 실패여도 로컬은 로그아웃"을 원하면 알려 주기.
11. (해결됨) **로그아웃 때 소셜 SDK 정리**: 문서에 없다. 안: 로그아웃·탈퇴·세션 끊김 때 카카오 `UserApiClient.logout()`(기기의 카카오 토큰 삭제)과 `CredentialManager.clearCredentialState()`를 부르고 실패는 무시. 카카오 연결 끊기는 서버가 한다(API 4.6).
12. (해결됨) **게스트 ID 표시**: `[게스트 ID]`가 `Me.id`(UUID 36자 전체)인지, 줄여 보여 주는지, 길게 눌러 복사할 수 있는지 문서에 없다 (FR-AUTH-09 AC3: 게스트는 본인 확인 수단이 없음 — 표시 목적도 불분명).
13. (해결됨) **탈퇴 다이얼로그 이미지가 원본과 다름**: `SCR-SET-01--withdraw-dialog.png` 뒤 화면은 맨 아래가 "회원 탈퇴"인데 원본 `Account.dc.html`과 `SCR-SET-01--account-member.png`는 "탈퇴하기". SCREENS 문구 "[탈퇴하기]"를 따른다 (이미지 재생성은 `/design-sync`).
14. (해결됨) **Server PLAN 배포 표시**: Server `docs/PLAN.md`의 "배포 1"은 "대기"(단위 2까지 묶어 배포)인데 사용자는 단위 1이 개발 서버에 배포됐다고 했다. 맥에서 실제 호출로 확인하고, Server 문서도 맞춰 두기.
15. (해결됨) **Robolectric 추가** (unit-1 결정 3): `MeEntity`가 첫 Room 엔티티라 CONVENTIONS "data:repository: 메모리 Room 테스트"에 Robolectric이 필요하다. MODULES.md 3장에 없다. A) `testImplementation`으로 추가 (추천), B) Room 테스트를 androidTest로(CI에서 안 돎).
16. (해결됨) **소셜 로그인 코드 위치**: `feature:auth`와 `feature:settings`가 같이 쓰는데 feature끼리 의존할 수 없다. A) `core:common`의 `social` 패키지 (지금 규칙 안, 추천. `core:common`에 Credential Manager·카카오 의존성이 생김), B) 새 모듈 `core:social`(MODULES.md 계획·`feature/CLAUDE.md` 의존 목록 변경).
