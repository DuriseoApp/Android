# app 모듈

- Application(`MyApp`), Hilt 진입점, 빌드 타입별 설정만 둔다. 화면·기능 코드는 feature 모듈에.
- 빌드 타입: `debug` = 개발 서버 + Google 테스트 광고, `noAdsDebug` = `debug`와 같고 광고·광고 자리 없음(`initWith(debug)`, 라이브러리는 `debug`로 fallback), `release` = 운영 서버 + 실제 광고(`duriseo.admob.*` 키 4개, 비면 광고 끔). applicationId는 셋 다 `com.duriseo.android` (Google 로그인·Firebase 등록이 하나라서, `docs/ARCHITECTURE.md` 6장).
- 설정값(서버 주소, Google 웹 클라이언트 ID, 카카오 네이티브 앱 키)은 `local.properties`에서 읽어 `BuildConfig`로 넣는다. 값이 없으면 빈 문자열로 빌드되게 한다 (CI). 키 이름은 `docs/SETUP.md` 2장. 값을 코드나 `gradle.properties`에 쓰지 않는다.
- 릴리스 서명 정보는 `keystore.properties`(커밋 안 됨)에서 읽는다. 출시 단위 전까지는 서명 설정을 만들지 않는다.
- release 로그는 `ReleaseTree`로 거른다. release 빌드에서는 토큰·개인 정보를 찍지 않는다. debug 빌드는 OkHttp 요청·응답 전체(토큰 포함)를 찍어도 된다 (루트 CLAUDE.md 규칙 5).
