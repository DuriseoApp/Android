# 설정 가이드

## 1. 맥에서 처음 한 번

1. 폴더 구조를 맞춰요. Docs와 같은 폴더 안에 있어야 `../Docs`를 읽을 수 있어요.
   ```
   Duriseo/
   ├── Docs/      (DuriseoApp/Docs)
   ├── Server/    (DuriseoApp/Server)
   └── Android/   (DuriseoApp/Android)
   ```
2. Android Studio 최신 안정판, JDK 17 이상.
3. `local.properties`에 아래 설정값을 넣어요 (2장). 이 파일은 커밋되지 않아요.
4. Claude Code는 `Android` 폴더에서 `claude --add-dir ../Docs`로 열어요. 세션이 열릴 때 `scripts/sync-docs.sh`가 Docs를 최신으로 받아요.

## 2. local.properties 설정값

공개 레포에 두지 않기로 한 값이에요. 값은 claude.ai 프로젝트 "둘이서 - 커플 앱"의 `config/environment.md`에 있어요. `app/build.gradle.kts`가 이 키를 읽어 `BuildConfig`로 넣어요. 값이 없어도 빌드는 되지만 그 기능은 동작하지 않아요.

| 키 | 내용 |
| --- | --- |
| `duriseo.api.devUrl` | 개발 서버 주소 (`https://…`). debug 빌드가 써요 |
| `duriseo.api.prodUrl` | 운영 서버 주소. release 빌드가 써요 |
| `duriseo.google.webClientId` | Google 웹 애플리케이션 클라이언트 ID (Credential Manager `serverClientId`) |
| `duriseo.kakao.nativeAppKey` | 카카오 네이티브 앱 키 |

새 키를 만들면 이 표, `app/build.gradle.kts`, CI 워크플로를 같은 PR에서 고쳐요.

`google-services.json`(Firebase)은 단위 7에서 `app/`에 두고, 커밋하지 않아요.

## 3. CI

- `.github/workflows/ci.yml`이 PR과 main push마다 `./gradlew assembleDebug testDebugUnitTest`를 돌려요. 결과 요약은 실행 화면의 Annotations에 나와요.
- 작업 브랜치는 push만으로는 CI가 돌지 않아요. PR을 열어야 돌아요 (Gradle을 못 돌리는 환경이면 draft PR을 먼저 열어요).
- 레포 Settings › Rules › Rulesets의 main 규칙에서 Require status checks에 **빌드와 테스트**를 넣어 두면 CI가 실패한 PR은 머지할 수 없어요.
- CI에는 `local.properties` 값이 없어요. 설정값이 필요한 테스트는 가짜 값을 써요.

## 4. 소셜 로그인 콘솔 설정 (한 번)

로그인을 실기기에서 확인하려면 콘솔에 앱을 등록해야 해요. 값은 `local.properties`가 아니라 각 콘솔에 있어요.

- **카카오 Developers** (앱 설정 › 플랫폼 › Android): 패키지명 `com.duriseo.android`와 **키 해시**를 등록해요. debug 키 해시는 `keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android | openssl sha1 -binary | openssl base64`로 구해요. 출시 전에 업로드 키·Play 앱 서명 키의 키 해시도 추가해요.
- **카카오 로그인**은 켜고, 동의항목은 "닉네임"(`profile_nickname`)만 켜요 (선택 동의). 이메일·전화번호 등은 꺼요 (NFR-PRIV-01). OpenID Connect는 쓰지 않아요.
- **Google**: Android OAuth 클라이언트(패키지 `com.duriseo.android` + debug SHA-1)를 등록해요. 같은 Google Cloud 프로젝트의 **웹 클라이언트 ID**가 `duriseo.google.webClientId`이고 서버의 `aud` 설정과 같은 값이어야 해요. 출시 때 release·Play 앱 서명 SHA-1 클라이언트를 추가해요.

## 5. 공개 레포 주의

이 레포는 공개예요. 커밋에 비밀값이나 비공개 설정값이 들어가면 지워도 기록에 남아요. 실수로 올렸으면 바로 그 키를 폐기하고 새로 발급해요.
