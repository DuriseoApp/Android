# app 모듈

- Application(`MyApp`), Hilt 진입점, 빌드 타입별 설정만 둔다. 화면·기능 코드는 feature 모듈에.
- 빌드 타입: `debug` = 개발 서버, `release` = 운영 서버. applicationId는 둘 다 `com.duriseo.android` (Google 로그인·Firebase 등록이 하나라서, `docs/ARCHITECTURE.md` 6장).
- 설정값(서버 주소, Google 웹 클라이언트 ID, 카카오 네이티브 앱 키)은 `local.properties`에서 읽어 `BuildConfig`로 넣는다. 값이 없으면 빈 문자열로 빌드되게 한다 (CI). 키 이름은 `docs/SETUP.md` 2장. 값을 코드나 `gradle.properties`에 쓰지 않는다.
- 릴리스 서명 정보는 `keystore.properties`(커밋 안 됨)에서 읽는다. 출시 단위 전까지는 서명 설정을 만들지 않는다.
- release 로그는 `ReleaseTree`로 거른다. 토큰·개인 정보는 어느 빌드에서도 찍지 않는다.
