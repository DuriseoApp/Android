# 단위 8: 프로필 — SCR-SET-04, 프로필 사진, 전체 메뉴 정보, SCR-SET-05

> 브랜치: `feat/8-profile` · PR: #19 · 상태: 진행 중 (2026-11-16 범위 승인, 확인 필요 1~6 추천안대로)
> 기준: PRD v0.12 (FR-AUTH-07 AC1~AC9, FR-SET-01, FR-SET-02 AC1~AC4, NFR-FILE-01, NFR-LIMIT-01, NFR-PRIV-01, D-82·D-115·D-126·D-146~D-153), SCREENS.md SCR-SET-01(프로필 카드·정보)·SCR-SET-04·SCR-SET-05, design-system "메일 앱 없음 다이얼로그"·"프로필 카드·연결 상태 카드"·"앱 정보 카드 상태"·"오픈소스 라이선스 목록", API.md 3.1(`PATCH /me`, `/me/profile-image`, `/users/{id}/profile-image`)·5장(413·415)·6장(`SPACE` `UPDATED`)·8장
> Docs 기준 커밋: DuriseoApp/Docs main `cee0885` (캔버스 버전 43)
> 앞 단위: 단위 7 머지됨 (main `7d5e2cc`). 서버 단위 7(프로필 사진) 머지·개발 서버 배포됨 (2026-11-14 배포 성공)

## 요약

| 묶음 | 내용 |
| --- | --- |
| A. 사진·이름 데이터 | `MeEntity`에 내 사진 버전·상대 사진 버전(Room 버전 7, 마이그레이션), `GET /me`·`GET /space`·`SYNC SPACE UPDATED`의 `profileImageVersion` 반영(넘어온 일), `PATCH /me {nickname}`, `PUT /me/profile-image`(multipart `file`), `DELETE /me/profile-image`, 413·415 → D-147 문구, 사진 조회 `GET /users/{id}/profile-image?v=<version>`(인증 헤더, Coil) |
| B. SCR-SET-04 | 새 화면 "내 프로필": 아바타 112 + 카메라 버튼, [사진 변경], 사진 시트(앨범·사진 찍기·기본 이미지·취소), 이름(10자, 코드 포인트, 빈 값 오류), "상대에게 이렇게 보여요" 미리보기, 로그인 계정 줄(D-148), [저장](이름 → 사진 순서, D-146), 요청 중 덮개, 스낵바 위 40 |
| C. 사진 고르기 | 기기 사진 선택기(`PickVisualMedia`), 카메라 앱(`TakePicture` + FileProvider), 권한 요청 없음(D-149), 고를 때 10MB·JPEG/PNG 검사(D-147), 25MP 넘으면 줄여서 올림(D-153) |
| D. 아바타 | `DuriseoAvatar`가 사진을 받아 원형으로 가운데 채움, 사진 없거나 못 받으면 사람 색 + 글자(FR-AUTH-07 AC4). SCR-SET-01 프로필 카드의 연필 배지·[내 프로필 수정](넘어온 일) |
| E. 전체 메뉴 정보 | "정보" 섹션: 문의하기(메일 앱, 없으면 다이얼로그 D-150), 이용약관·개인정보처리방침, 오픈소스 라이선스 → SCR-SET-05, 앱 정보 카드(버전, Play 인앱 업데이트 확인 D-151) |
| F. SCR-SET-05 | 새 화면 "오픈소스 라이선스": 라이브러리 목록 + 글꼴 2행(끝), 누르면 그 자리에서 펼침 |
| G. 넘어온 일 | 큰 글자에서 연결 정보 아래쪽이 밀리는 것(스크롤), 이름 말줄임 점검 |

## 확인 필요

| # | 빈 곳 | 추천안 | 근거 |
| --- | --- | --- | --- |
| 1 | 이용약관·개인정보처리방침 웹 주소가 없음 (PRD Q-03, 출시 전 필수) | 행은 그림대로 보이고, 누르면 SCR-START-01의 두 링크처럼 지금은 아무 일도 하지 않아요. 주소 자리는 한 곳(상수)에 두고 출시 준비 때 채워요 | SCREENS SCR-SET-01 "정보", PLAN "출시" |
| 2 | 문의 메일 받는 주소가 없음 (PRD Q-10) | 주소는 한 곳(문자열 리소스)에 두고 지금 값은 SCREENS의 자리표시 `[문의 메일 주소]`. 메일 앱에는 받는 사람을 비운 채 제목·본문만 채워 열어요(자리표시를 주소로 넣지 않음). 메일 앱이 없을 때 다이얼로그·[복사]는 자리표시 그대로 | SCREENS "문의하기", design-system "메일 앱 없음 다이얼로그" |
| 3 | 사진을 어느 아바타에 보이는지 (SCREENS는 SCR-SET-01·SCR-SET-04만 적음, AC3은 "상대 화면의 아바타") | 사람 아바타를 그리는 모든 곳: SCR-SET-01 프로필 카드·연결 상태 카드, SCR-SET-04, SCR-CAL-01 앱바(커플 겹침·개인), 메모 작성자 아바타(SCR-MEMO-01·02). 공통 `DuriseoAvatar` 한 곳에서 처리 | FR-AUTH-07 AC3·AC4 |
| 4 | 카메라 앱이 없는 기기에서 [사진 찍기] | 사진 시트에서 [사진 찍기] 행을 숨겨요 (새 문구 없음). 열기가 실패하면 공통 오류 | D-149 |
| 5 | 인앱 업데이트를 받는 중·받은 뒤 모양과 설치 시점이 없음 (D-151은 "앱을 쓰는 중에 받는 방식"만) | Play 인앱 업데이트의 유연한 방식(FLEXIBLE). [업데이트]를 누르면 받는 동안 최신 여부 줄을 "확인 중"처럼 비워 두고(카드 높이 유지), 다 받으면 앱이 백그라운드로 갈 때 설치해요(사용 중에 앱을 다시 시작하지 않음). 새 문구 없음. Docs 후속에 올려요 | D-151, FR-SET-02 AC4 |
| 6 | 라이브러리 추가 (MODULES.md 계획에는 단위 8 Coil만) | ① Coil 3 + OkHttp 네트워크(앱의 인증 OkHttp를 써서 토큰 갱신도 같게) ② Play `app-update-ktx`(D-151) ③ AboutLibraries **Gradle 플러그인과 core만**(빌드 때 의존성의 이름·라이선스 JSON 생성, 화면은 우리 토큰으로 직접 그림, 라이브러리의 UI 모듈은 안 씀). 글꼴 2행과 OFL 1.1 본문은 앱에 직접 넣어요. MODULES.md 표에 더해요 | SCR-SET-05 "구현 시점의 의존성에 맞춰요", D-152 |

## 범위

### A. 사진·이름 데이터 (API.md 3.1·5장·6장·8장)

| ID | 내용 | 테스트 |
| --- | --- | --- |
| API `Me.profileImageVersion`, `MePartner.profileImageVersion`, `SpaceMember.profileImageVersion` | Room `MeEntity`에 내·상대 사진 버전(버전 6 → 7, 마이그레이션, 버전 없음 = null), 도메인 `Me`·`MePartner`에 버전. `GET /space` 합치기에서 사진 버전도 반영(archive/unit-7.md 결정 3) | `MeRepositoryTest: GET me의 사진 버전을 저장한다`, `MeRepositoryTest: GET space의 구성원 사진 버전을 합친다`, `DatabaseMigrationTest: 6에서 7로 사진 버전 열을 더한다` |
| API `PATCH /me {nickname}` | 앞뒤 공백 지운 값으로 보냄, 200 `Me`를 Room에 저장. `VALIDATION_FAILED`(`REQUIRED`·`TOO_LONG`)·그 밖은 공통 오류 | `ProfileRepositoryTest: 이름을 PATCH me로 저장하고 응답을 Room에 쓴다`, `ProfileRepositoryTest: 이름 저장 실패는 오류 코드로 돌려준다` |
| API `PUT /me/profile-image` | multipart `file` 하나(JPEG 또는 PNG 바이트, Content-Type 맞춤), 204 뒤 `GET /me`로 새 버전을 읽어 Room에 저장. 413 → "10MB 이하의 사진을 골라 주세요", 415 → "JPEG나 PNG 사진만 쓸 수 있어요" | `ProfileRepositoryTest: 사진을 multipart file로 올리고 GET me로 버전을 다시 읽는다`, `ProfileRepositoryTest: 413은 사진 크기 오류 415는 사진 형식 오류다` |
| API `DELETE /me/profile-image` | 204 뒤 `GET /me` | `ProfileRepositoryTest: 사진을 지우고 GET me로 버전이 null이 된다` |
| API `GET /users/{id}/profile-image?v=` | Coil 이미지 요청에 앱의 인증 OkHttp(Bearer, 401 갱신), 캐시 키 = 사용자 ID + 버전. 버전이 null이면 요청하지 않음. 404·실패는 글자 아바타 | `ProfileImageUrlTest: 버전이 있으면 v 쿼리를 붙이고 없으면 null이다` |
| API 6장 `SPACE` `UPDATED` | 단위 7의 `GET /me`·`GET /space`가 사진 버전까지 반영 (따로 일 없음, 위 테스트로 확인) | (위) |

### B. SCR-SET-04 프로필 수정 (FR-AUTH-07)

| ID | 내용 | 테스트 / Preview |
| --- | --- | --- |
| FR-AUTH-07 AC6 (D-146) | [저장]: 이름이 바뀌었으면 이름 → 사진이 바뀌었으면 사진(기본 이미지면 삭제). 요청 중 덮개(D-115). 중간 실패면 멈추고 공통 오류(사진 오류는 D-147 문구), 된 것은 유지, 다시 누르면 남은 것만. 둘 다 성공하면 화면을 닫아요(보드). 오프라인이면 부르지 않고 "인터넷 연결이 필요해요" | `ProfileEditViewModelTest: 이름과 사진이 바뀌면 이름을 먼저 저장하고 사진을 올린다`, `…: 이름 저장이 실패하면 사진을 올리지 않고 화면에 머문다`, `…: 사진만 실패한 뒤 다시 누르면 사진만 다시 올린다`, `…: 기본 이미지로 바꾸면 사진을 지운다`, `…: 오프라인이면 저장하지 않고 인터넷 안내를 보인다` |
| FR-AUTH-07 AC6 (이름) | 10자(코드 포인트)에서 더 입력되지 않음, 글자 수 "N / 10", 공백 제거 뒤 비면 [저장] 비활성 + "이름을 한 글자 이상 입력해 주세요." | `ProfileEditViewModelTest: 이름은 코드 포인트 10개에서 더 입력되지 않는다`, `…: 공백뿐인 이름이면 저장할 수 없고 오류를 보인다` |
| SCR-SET-04 앱바 | [저장]은 바뀐 게 있을 때만 활성 (공백만 다른 이름은 바뀐 것 아님) | `ProfileEditViewModelTest: 바뀐 것이 없으면 저장이 비활성이다` |
| FR-AUTH-07 AC8 (D-148) | 로그인 계정 줄: "Google 로그인"·"카카오 로그인"·"게스트"·"Google · 카카오" | `ProfileEditViewModelTest: 로그인 계정 줄은 로그인 수단대로 보인다` |
| FR-AUTH-07 AC7 (D-147) | 사진 오류 스낵바 위 40, 아바타는 원래 모습 | `ProfileEditViewModelTest: 10MB를 넘는 사진은 고르지 않고 크기 오류를 보인다`, `…: JPEG PNG가 아닌 사진은 형식 오류를 보인다` |
| SCR-SET-04 상태 | 기본(사진 없음), 사진 고름(미리보기), 사진 시트, 사진 오류, 이름 빈 값, 저장 중 | `ProfileEditPreview_Default`, `_PhotoPicked`, `_PhotoSheet`, `_PhotoError`, `_EmptyName`, `_Saving` |

### C. 사진 고르기 (D-149, D-153, NFR-FILE-01)

| ID | 내용 | 테스트 |
| --- | --- | --- |
| FR-AUTH-07 AC9 | 앨범 = `PickVisualMedia(ImageOnly)`, 사진 찍기 = `TakePicture`(앱 캐시 + FileProvider), 매니페스트에 저장소·카메라 권한 없음, 카메라 앱이 없으면 행 숨김(확인 필요 4), 카메라 버튼 = [사진 변경] | `PhotoSheetTest: 카메라 앱이 없으면 사진 찍기를 숨긴다`, `ManifestPermissionTest: 카메라와 저장소 권한을 선언하지 않는다` |
| NFR-FILE-01, D-147 | 고른 원본 크기 > 10MB면 크기 오류, 실제 바이트 머리(시그니처)로 JPEG·PNG가 아니면 형식 오류 | `PickedPhotoCheckerTest: 10MB 초과는 크기 오류다`, `…: 시그니처가 JPEG PNG가 아니면 형식 오류다` |
| D-153 | 픽셀 수 > 25MP면 비율 유지로 25MP 안으로 줄여 JPEG로 다시 인코딩(회전 적용, 투명은 흰 배경) 뒤 올림. 25MP 이하는 원본 바이트 그대로 | `PhotoDownscalerTest: 25MP를 넘으면 비율을 지켜 25MP 안으로 줄인 크기를 계산한다`, `…: 25MP 이하는 그대로 둔다` |

### D. 아바타 (FR-AUTH-07 AC3·AC4, D-149)

| ID | 내용 | 테스트 / Preview |
| --- | --- | --- |
| FR-AUTH-07 AC3·AC4 | `DuriseoAvatar`에 사진 모델(없으면 null). 사진은 원형 가운데 채움(`ContentScale.Crop`), 받는 중·실패·없음은 사람 색 + "나"/"너". 쓰는 곳은 확인 필요 3 | `DuriseoAvatarPreview_Photo`, `DuriseoAvatarPreview_Fallback` |
| SCR-SET-01 프로필 카드 (D-149) | 내 아바타 연필 배지(28 흰 원, 4 나감, `surface-100` 2 링, 아이콘 15) → SCR-SET-04, [내 프로필 수정] → SCR-SET-04 (커플·혼자·게스트 모두) | `SettingsViewModelTest: 내 프로필 수정과 연필 배지는 프로필 화면으로 간다`, `SettingsPreview_Couple`(배지·버튼), `SettingsPreview_Solo`, `SettingsPreview_Guest` |

### E. 전체 메뉴 정보 (FR-SET-02)

| ID | 내용 | 테스트 / Preview |
| --- | --- | --- |
| FR-SET-02 AC1 (D-150) | 문의하기: `ACTION_SENDTO mailto:`, 제목 "[둘이서 문의]", 본문 세 줄 "앱 버전: …"·"Android 버전: …"·"기기 모델: …". 메일 앱이 없으면 다이얼로그(제목·본문·주소 상자·[닫기][복사]), 복사 뒤 스낵바 없음 | `SupportMailTest: 제목과 본문 세 줄을 채운다`, `SettingsViewModelTest: 메일 앱이 없으면 주소 다이얼로그를 보인다`, `SettingsPreview_MailDialog` |
| FR-SET-02 AC2 | 이용약관·개인정보처리방침 행 (확인 필요 1) | `SettingsPreview_Couple` |
| FR-SET-02 AC3 | 오픈소스 라이선스 행 → SCR-SET-05 | `SettingsViewModelTest: 오픈소스 라이선스는 라이선스 화면으로 간다` |
| FR-SET-02 AC4 (D-151) | 앱 정보 카드: 버전, 확인 중(줄 비움·높이 유지), 최신("최신 버전을 사용하고 있어요"), 새 버전([업데이트]), 확인할 수 없음(줄 뺌: 오프라인·Play 없음·직접 설치). 받는 중·설치는 확인 필요 5 | `AppUpdateViewModelTest: 확인 중에는 최신 여부 줄을 비운다`, `…: 새 버전이 있으면 업데이트 버튼을 보인다`, `…: 확인할 수 없으면 버전만 보인다`, `…: 오프라인이면 확인할 수 없음이다`, `SettingsPreview_UpdateChecking`, `_UpdateUnknown`, `_UpdateAvailable` |

### F. SCR-SET-05 오픈소스 라이선스 (FR-SET-02 AC3, D-152)

| ID | 내용 | 테스트 / Preview |
| --- | --- | --- |
| SCR-SET-05 | 앱바(뒤로, "오픈소스 라이선스"), 목록(이름 16/500, 라이선스 이름 13 `ink-500`, 화살표), 누르면 그 자리에서 펼침(면 `surface-50`, 라이선스 본문), 다시 누르면 접힘. 글꼴 IBM Plex Sans KR·IBM Plex Mono(SIL OFL 1.1)는 끝. 설명 줄 없음 | `LicensesViewModelTest: 글꼴 두 행은 목록 끝에 있다`, `…: 행을 누르면 펼치고 다시 누르면 접는다`, `LicensesPreview_Default`, `LicensesPreview_Expanded` |

### G. 넘어온 일

| ID | 내용 | 테스트 / Preview |
| --- | --- | --- |
| 접근성 (PLAN 단위 8) | 연결 정보 화면을 세로 스크롤로 감싸 큰 글자에서도 안내·버튼이 보이게, 프로필 카드·연결 상태 카드 이름 한 줄 말줄임 확인 | `ConnectionInfoPreview_LargeFont` |
| 글꼴 고지 (PLAN 단위 8) | D-152로 정해짐: 넣어요 (F 묶음) | (F) |

## 서버 연동

| API | 개발 서버 배포 여부 | 확인 |
| --- | --- | --- |
| `PATCH /me` | 배포됨 (서버 단위 1) | MockWebServer |
| `PUT`·`DELETE /me/profile-image`, `GET /users/{id}/profile-image` | 배포됨 (서버 단위 7, 2026-11-14) | MockWebServer. 실기기 확인은 사람 |
| `GET /space` 사진 버전 | 배포됨 (서버 단위 2·7) | MockWebServer |

## 범위 밖

- 이용약관·개인정보처리방침 주소, 문의 메일 주소 (출시, Q-03·Q-10)
- 사진 자르기 화면 (D-149로 없음)

## 결정

1단계(묶음 A·C·D)에서 문서에 없어 정한 것:

1. **사진 모델은 도메인 `ProfileImage(userId, version)`.** feature는 URL·서버 주소를 모른다. `DuriseoAvatar(photo: Any?)`에 이 값을 넘기면 `data:remote`의 Coil `Mapper<ProfileImage, String>`(`ProfileImageMapper`)이 주소로 바꾼다. 버전이 null이면 `profileImageOf`가 null을 돌려줘 요청하지 않는다.
2. **주소는 `<서버 주소>/api/v1/users/{id}/profile-image?v=<version>`.** API.md 경로는 모두 `/api/v1` 아래라 `AppConfig.apiV1BaseUrl()`에 붙인다 (요청 문장의 `<서버주소>/users/…`는 `/api/v1`이 빠진 줄임으로 봄). 서버 주소가 비면(CI) 주소가 null이라 요청하지 않는다.
3. **`ImageLoader`는 `data:remote`의 `ProfileImageModule`이 인증 OkHttp(`@Authed`)로 만들고, 앱 `MyApp`이 `SingletonImageLoader.Factory`로 등록한다** (`dagger.Lazy`). 디스크·메모리 캐시는 Coil 기본이고 키는 주소(사용자 ID + 버전)다. 로그아웃 때 디스크 캐시를 비우는 일은 하지 않았다 (남은 일).
4. **라이브러리**: Coil 3.3.0 (`coil-core`·`coil-compose`·`coil-network-okhttp`, `coil-core`는 `ImageLoader`·`Mapper`를 data:remote·app이 직접 써서 더함), `androidx.exifinterface` 1.4.1. 버전은 이 환경에서 못 받아 봐서 CI로 확인한다. MODULES.md 3장에 적었다.
5. **Room 6 → 7**: `Migration(6, 7)`이 두 열(`profile_image_version`·`partner_profile_image_version`, null 허용)을 더한다. 그 밖의 옛 버전은 기존대로 destructive. 스키마 JSON을 커밋하지 않아(단위 2 결정) `MigrationTestHelper`를 못 써서, 테스트는 버전 7 DB를 연 뒤 `me`를 6 모양으로 다시 만들고 마이그레이션을 직접 돌려 Room 엔티티로 읽는다.
6. **새 필드는 모두 끝에 기본값 null로 더했다** (`Me`·`MePartner`·`SpaceMember`·`SettingsPerson`·`CalendarHeader`·`MemoItemUi`). 기존 호출·Fake가 그대로 컴파일되고, 사진을 보는 곳만 새 값을 넣는다. `GET /space` 합치기는 구성원이 있으면 **null도 그대로** 반영한다 (사진 삭제가 이전 버전을 남기지 않게).
7. **`ProfileRepository`**(domain) / `ProfileRepositoryImpl`(data:repository). 이름: `runOnlineWrite` → `PATCH /me {nickname.trim()}` → 응답 `Me`를 `MeRepositoryImpl.saveWrittenMe`(내부 함수, 기존 `save`: `MeWriteLock`·캐시 세대)로 저장. 사진: 204 뒤 `refreshMe()`로 새 버전을 읽고, 그 읽기가 실패해도 올리기·지우기는 성공이다(`CoupleRepositoryImpl.thenRefreshMe`와 같은 규칙). `MeRepository` 인터페이스는 바꾸지 않았다(Fake 영향 없음).
8. **사진 오류 타입 `PhotoError`(TOO_LARGE·TYPE_NOT_ALLOWED) + `DataError.photoErrorOrNull()`** (domain `profile`). `DataError`에 하위 타입을 더하면 모든 `when`이 깨지고 `ErrorCode`에 이미 두 코드가 있어서, `code`로 판단하는 도우미로 했다. 그 밖의 오류는 공통 오류 변환 그대로. 고를 때 검사(`PickedPhotoChecker`)도 같은 타입을 쓴다.
9. **`PickedPhotoChecker`는 domain**(순수 Kotlin이라 Android 없이 테스트). 한계는 서버 `ProfileImageProcessor`와 같다: `MAX_BYTES = 10 * 1024 * 1024`(정확히 10MB는 통과), `MAX_PIXELS = 25_000_000`. 크기를 형식보다 먼저 본다(서버 순서).
10. **`PhotoDownscaler`는 `feature:settings`의 `profile/photo/`.** 25MP 이하(또는 크기를 못 읽음)면 원본 바이트·형식 그대로(EXIF 회전은 서버가 적용). 넘으면 `inSampleSize`는 디코드 결과가 25MP 이하가 되는 가장 작은 2의 거듭제곱(메모리 절약), 그래도 넘으면 `fitWithinPixels` 크기로 스케일 → EXIF 회전 → 투명이면 흰 배경 합성 → JPEG(품질 90, 10MB를 넘으면 80·70·60). 순수 계산(`fitWithinPixels`·`sampleSizeFor`)만 단위 테스트하고 실제 비트맵 경로는 실기기 확인이다.
11. **FileProvider는 `feature:settings` 매니페스트**(authority `${applicationId}.profilephoto`, 앱 캐시 `profile_photo/`만, `exported=false`). 카메라 앱을 찾는 `<queries>` `IMAGE_CAPTURE`(권한 아님)도 같이 두었다 (확인 필요 4의 "카메라 앱 없으면 행 숨김"에 필요, 2단계가 씀). `ManifestPermissionTest`는 레포의 모든 모듈 소스 매니페스트를 훑어 카메라·저장소 권한이 없는지, provider 설정이 맞는지 확인한다.
12. **`DuriseoAvatar`**: designsystem이 Coil(`coil-compose`)에 의존한다 (ARCHITECTURE의 금지 목록에 없음). 글자 원을 먼저 그리고 그 위에 `AsyncImage`(원형·`Crop`)를 얹어서, 받는 중·실패·null은 따로 처리하지 않아도 글자 원이 보인다. 지름이 달라 `DuriseoAvatar`를 못 쓰는 메모 핀 카드 작성자 아바타(24)가 쓰도록 `DuriseoAvatarPhoto`를 공개했다. Preview는 Coil 없이 같은 자리·같은 자르기로 그림 하나를 얹는다(`_Photo`).
13. **사진을 연결한 곳**: SCR-SET-01 프로필 카드(`SettingsPerson.photo`, 연결 정보 SCR-SET 연결 상태 카드와 공용), SCR-CAL-01 앱바(커플 겹침·개인), 메모 핀 카드의 작성자 아바타(SCR-MEMO-01·02 공용 `MemoItemUi.photo`). **메모 행은 사람 색 점만 있고 아바타가 없어** 바꿀 것이 없다. SCR-SET-03 색 미리보기의 글자 원은 색을 고르는 미리보기라 사진을 넣지 않았다 (필요하면 알려 주세요).

2단계(묶음 B와 D의 SCR-SET-01 연결)에서 문서에 없어 정한 것:

14. **사진 읽기·카메라 앱 판정은 인터페이스**(`PickedPhotoReader`, `CameraAvailability`)로 ViewModel과 나눴다. Uri는 `String`으로 주고받아 ViewModel 테스트가 Android 없이 돈다. 구현(`AndroidPickedPhotoReader`, `AndroidCameraAvailability`)은 Hilt `ProfilePhotoModule`이 묶는다. 읽기는 `@IoDispatcher`에서 하고 10MB + 1바이트까지만 읽어(큰 파일 전체를 메모리에 올리지 않음) 넘으면 바로 크기 오류, 읽다 `OutOfMemoryError`가 나면 읽기 실패(공통 오류)다.
15. **고른 사진은 `PickedPhoto`(올릴 바이트·형식·작은 미리보기 비트맵)로 ViewModel이 저장 전까지 들고 있다.** 미리보기는 짧은 변 512 안팎으로 `inSampleSize`로 줄여 디코드하고 EXIF 회전을 적용해 `Image`로 그린다(Coil에 비트맵을 넣지 않음). 카메라 임시 파일은 읽은 뒤(성공·실패)와 찍지 않고 돌아왔을 때 지운다. 카메라 Uri 문자열은 `rememberSaveable`에 둔다.
16. **서버가 사진을 413·415로 거부하면 고른 사진을 버린다**(원래 모습으로, 같은 사진은 다시 보내도 실패). 그 밖의 실패는 고른 사진을 그대로 둬 다시 누를 수 있다. 사진이 올라가거나 지워지면 고른 상태를 비운다(Room이 새 원래 값).
17. **저장이 중간에 실패했을 때 "원래 값"은 Room의 내 정보다.** 이름이 저장되면 `PATCH /me` 응답이 Room을 바꿔 이름은 더 이상 바뀐 것이 아니다. 이름 입력은 사용자가 건드릴 때까지 Room 값을 그대로 보이고, 공백만 다른 이름은 바뀐 것이 아니다. 저장하지 않고 뒤로 가도 확인 대화상자는 없다(문서에 없음).
18. **디자인시스템에 보드 값 추가**: `DuriseoAvatarSize.XLarge`(112, 글자 40)·`Compact`(36, 14)·`Mini`(22, 10), 고른 사진 미리보기용 `DuriseoAvatarImage`, 아이콘 `ic_pencil`·`ic_camera`·`ic_image`(보드 SVG 그대로). 보드의 ink-300 `#B3AEBB`(비활성 [저장])·brand-700(활성)은 토큰이다. 토큰에 없는 수치(카메라 버튼 40, [사진 변경] 높이 40, 배지 28 등)는 화면 파일의 이름 붙은 상수다.
19. **프로필 카드 연결**: 보드가 내 아바타·이름·표지를 한 링크로 묶고 접근성 이름을 "내 프로필 수정: [내 이름]"으로 두어 그대로 했다(내 칸 전체가 버튼, 연필 배지는 그 안의 장식). 버튼 줄은 커플 [내 프로필 수정][처음 만난 날 수정], 혼자·게스트 [내 프로필 수정][상대와 연결하기](보드 71·101행). ViewModel은 `SettingsAction.ProfileEditClicked` -> `NavigateToProfileEdit` 효과로 보내 테스트한다. 요청 중(`requesting`)에는 누를 수 없다.
20. **로그아웃·탈퇴·세션 끊김·계정 전환 때 Coil 메모리·디스크 캐시를 비운다**(NFR-PRIV-01 취지). domain `ProfileImageCacheCleaner`(구현 `data:remote`의 `CoilProfileImageCacheCleaner`, 로더는 `Provider`로 늦게 받아 의존 순환 방지)를 `SessionManager.clearAll`(로그아웃·탈퇴·세션 끊김)과 `clearUserDataOrFail`(로그인·switch가 새 토큰을 저장하기 전)에서 부른다. 실패해도 나머지 정리와 로그인은 계속한다. 결정 3의 남은 일 해결.
21. **사진 시트의 면은 공용 `DuriseoSheetPanel`**(좌우 24·위 10)을 그대로 썼다(보드는 좌우 20·위 8). 다른 시트와 같은 모양을 지키려는 선택이고 차이는 4dp 안팎이다.
22. **로그인 계정 줄은 회원인데 로그인 수단을 모르면 줄을 그리지 않는다**(SCR-SET-01 계정 행이 값이 빈 것과 같음). 도움말 "상대 화면과 캘린더에 이 이름으로 보여요. 최대 10자."는 SCREENS.md에는 없고 보드(`ProfileEdit.dc.html` 99행) 문구다.
23. **오프라인**: 저장소가 요청 없이 `Offline`을 돌려주는 기존 규칙(`runOnlineWrite`)을 그대로 쓰고, ViewModel은 그 결과를 "인터넷 연결이 필요해요"로 보인다. 사진 오류·공통 오류 스낵바는 앱 공통 `onShowSnackBar`가 위 40에 띄운다(`isSnackbarAtTop`이 탭 없는 화면을 이미 포함, `SnackbarPositionTest`에 SCR-SET-04 추가).

## 리뷰 기록

| 회차 | Critical | Major | 비고 |
| --- | --- | --- | --- |

## 남은 일

- (2단계 끝) 아직 못 확인: 이 환경에서 Gradle이 안 돌아 2단계 코드는 CI로 컴파일·테스트 확인이 필요하다. 실기기 확인(사람): 앨범·카메라로 고르기, 카메라 앱 없는 기기에서 [사진 찍기] 숨김, 큰 사진 줄이기, 저장 중 덮개, 로그아웃 뒤 이전 사진이 남지 않는지.
- 묶음 E·F·G (정보 섹션, SCR-SET-05, 큰 글자 스크롤)는 3단계.
- CI에서 컴파일 확인: Coil 3.3.0 API(`Mapper`·`ImageLoader.Builder.components`·`OkHttpNetworkFetcherFactory`), exifinterface 1.4.1.
-
