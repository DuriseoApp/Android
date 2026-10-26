# 구조

화면·기능 내용은 Docs(PRD·SCREENS·API)를 본다. 이 문서는 Android에서 어떻게 나누고 흘려보내는지만 적는다. 지금 있는 모듈은 `settings.gradle.kts`가 기준이고, 앞으로 만들 모듈은 `docs/MODULES.md`에 있다.

## 1. 레이어와 의존 방향

```
app ──> feature:main ──> feature:<기능> ──> domain <── data:repository ──> data:remote
                                │                                    └──> data:local
                                └──> core:designsystem, core:common        push ──> domain
```

| 레이어 | 하는 일 | 알면 안 되는 것 |
| --- | --- | --- |
| `app` | Application, Hilt 진입점, 빌드 타입별 설정 주입 | 화면 로직 |
| `feature:main` | 단일 Activity, NavHost, 하단 탭(캘린더·메모, D-30), 시작 화면 분기 | 기능 화면 내부 |
| `feature:<기능>` | 화면(Compose), ViewModel(MVI), 기능 내 내비게이션 | `data:*` 모듈, Retrofit, Room |
| `domain` | 모델, 리포지토리 인터페이스, 여러 리포지토리를 묶는 유스케이스, **결과·오류 타입**(`DataResult`, `DataError`, `ErrorCode`), `NetworkMonitor`. **순수 Kotlin** | Android, `data:*` |
| `data:repository` | domain 인터페이스 구현. 서버 호출과 로컬 저장을 묶음 | 화면 |
| `data:remote` | Retrofit 서비스, DTO, 응답·오류 변환 (API.md 1.1) | Room |
| `data:local` | Room(오프라인 보기), DataStore(설정), 암호화 토큰 저장 | Retrofit |
| `push` | FCM 수신, 동기화 신호(SYNC) 처리, 알림 표시 | 화면 |
| `core:designsystem` | 토큰(색·글꼴·간격·모서리), 공용 컴포넌트, `PersonColors` | 기능 로직 |
| `core:common` | 로거, 디스패처(`@IoDispatcher`), `Clock`, 앱 설정(`AppConfig`), 내비게이션 경로 같은 공용 도구 | 기능 로직 |

- feature끼리 서로 의존하지 않는다. 다른 기능 화면으로 갈 때는 `feature:main`의 내비게이션이 연결한다.
- 유스케이스는 리포지토리 하나를 그대로 부르는 것이면 만들지 않는다. ViewModel이 리포지토리 인터페이스를 직접 쓴다.

## 2. 데이터 흐름

**읽기: 화면은 Room만 본다** (NFR-OFF-01, D-28)

```
Screen ← ViewModel ← Repository.observeX(): Flow  ← Room
                       Repository.refreshX() ─── API ──> Room에 저장 (트랜잭션)
```

- 다시 불러오는 때: 화면에 들어갈 때, 당겨서 새로고침, 동기화 신호를 받았을 때 (NFR-SYNC-01·02).
- 범위로 불러오는 데이터(월 일정 등)는 그 범위를 Room에서 지우고 새로 넣는다. 서버에서 지워진 행이 남지 않게 한다.
- 오프라인이면 refresh는 조용히 실패하고 화면은 마지막으로 저장한 데이터를 보여 준다.

**쓰기: 온라인에서만** (NFR-OFF-02, D-28)

```
ViewModel → Repository.create/update/delete → (오프라인이면 즉시 Offline 오류) → API → 성공 응답을 Room에 반영 → Flow로 화면 갱신
```

- 낙관적 갱신을 하지 않는다. 서버가 성공해야 Room을 바꾼다.
- 오프라인에서 쓰기를 누르면 실행하지 않고 "인터넷 연결이 필요해요"를 보여 준다. 리포지토리는 쓰기를 `NetworkMonitor.runOnlineWrite { … }`(`data:repository`)로 감싸 오프라인이면 API를 부르지 않고 `Offline`을 돌려준다.
- 동시 수정은 마지막 저장이 이긴다 (NFR-SYNC-03). 앱에서 따로 막지 않는다.

**동기화 신호** (NFR-SYNC-01, API.md 동기화 신호 장)

- FCM 데이터 메시지는 `push`가 받아 해당 리포지토리의 refresh를 부른다. 사용자에게 보이지 않는다.
- 신호의 종류·필드는 API.md 그대로 쓴다.

## 3. 오류

- 결과·오류 타입은 `domain`에 있다. `core:common`은 Android 라이브러리라 순수 Kotlin인 `domain`이 의존할 수 없기 때문이다. 호출 결과는 `DataResult<T>`(`Success` / `Failure(DataError)`), 오류는 `DataError`(`ApiError` / `Offline`)다.
- 서버 오류는 API.md 1.1의 오류 객체 `{code, message, details, traceId}`다. `data:remote`의 `ApiCaller`가 이를 `ApiError(code, details, traceId)`로 바꾼다. `code`는 서버 문자열 그대로 두고 `ErrorCode`(API.md 5장 26개)로 해석하며, 모르는 값은 `knownCode = null`이다.
- JSON이 아닌 오류 본문(프록시 HTML, 빈 본문)과 모양이 맞지 않는 성공 본문은 `ApiError(code = "INTERNAL_ERROR")`로 본다. 서버 주소가 비어 있는 빌드(CI)에서 요청하면 같은 `INTERNAL_ERROR`로 실패한다.
- 화면 문구는 `code`로 정한다. `message`는 로그용이라 화면에 쓰지 않는다. 모르는 `code`는 일반 오류 문구로 보여 준다.
- 네트워크 오류(연결 없음, 시간 초과, 그 밖의 `IOException`)는 `Offline`으로 따로 구분한다.
- 사용자에게 보이는 문구는 `core:designsystem`의 `DataError.toMessageRes()`로 바꾼다. `Offline`은 "인터넷 연결이 필요해요", `ApiError`는 공통 오류 문구이고 스낵바(`DuriseoSnackbar`)로 보여 준다.
- `traceId`는 디버그 로그에만 남긴다.
- 화면에 보이는 문구는 SCREENS.md에 있는 것만 쓴다. 없으면 멈추고 보고한다.

## 4. 인증

- 서버가 발급한 access·refresh 토큰(JWT)을 쓴다. 기기에는 **암호화해 저장**한다 (NFR-SEC-01). 토큰을 로그에 남기지 않는다.
- access가 만료되면 OkHttp `Authenticator`가 refresh를 **한 번만** 부르고(동시 요청은 기다림) 원래 요청을 다시 보낸다.
- refresh가 세션 끊김 오류로 실패하면 로컬 데이터를 지우고 시작 화면으로 보낸다. 오류 코드별 안내(다른 기기 로그인 등)는 API.md·SCREENS.md를 따른다.
- 사용자 시간대(`users.time_zone`)는 앱 실행 때 서버에 갱신한다 (ERD 1장).

## 5. 권한과 화면

- 권한의 최종 판단은 서버다 (NFR-SEC-02). 앱은 PRD 7장 권한 매트릭스대로 수정·삭제 메뉴를 보이거나 숨길 뿐이다.
- 서버가 404를 주면 "없어진 항목"으로 처리하고 Room에서도 지운다.

## 6. 빌드 타입과 설정값

| 빌드 타입 | 서버 | applicationId |
| --- | --- | --- |
| `debug` | 개발 서버 | `com.duriseo.android` |
| `release` | 운영 서버 | `com.duriseo.android` |

- 두 빌드 타입은 applicationId가 같다. Google 로그인·Firebase에 등록한 패키지가 하나이기 때문이다.
- 서버 주소, Google 웹 클라이언트 ID, 카카오 네이티브 앱 키는 `local.properties`에서 읽어 `BuildConfig`로 넣는다. 값이 없으면 빌드는 되지만 그 기능은 동작하지 않는다 (CI 빌드용). 키 이름은 `docs/SETUP.md`.

## 7. 시간

- 일시는 `Instant`, 날짜만 있는 값(종일 일정, 기념일, 메모 날짜, 처음 만난 날)은 `LocalDate`로 다룬다 (NFR-TIME-01).
- 날짜 선택은 1950-01-01 ~ 2050-12-31만 허용한다. 처음 만난 날은 오늘까지 (FR-CAL-01 AC6, D-74).
- 현재 시각은 주입받은 `Clock`으로 얻는다 (테스트에서 고정하려고).
