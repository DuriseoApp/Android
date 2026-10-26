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

공개 레포에 두지 않기로 한 값이에요. 값은 claude.ai 프로젝트 "둘이서 - 커플 앱"의 `config/environment.md`에 있어요. 단위 1에서 빌드가 이 키를 읽게 만들어요. 값이 없어도 빌드는 되지만 그 기능은 동작하지 않아요.

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
- 레포 Settings › Rules › Rulesets의 main 규칙에서 Require status checks에 **빌드와 테스트**를 넣어 두면 CI가 실패한 PR은 머지할 수 없어요.
- CI에는 `local.properties` 값이 없어요. 설정값이 필요한 테스트는 가짜 값을 써요.

## 4. 공개 레포 주의

이 레포는 공개예요. 커밋에 비밀값이나 비공개 설정값이 들어가면 지워도 기록에 남아요. 실수로 올렸으면 바로 그 키를 폐기하고 새로 발급해요.
