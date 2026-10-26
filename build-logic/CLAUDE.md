# build-logic (컨벤션 플러그인)

- 플러그인 ID는 `duriseo.android.*`. 모듈 `build.gradle.kts`에는 플러그인과 그 모듈만의 의존성만 두고, 공통 설정(SDK 버전, Compose, Hilt, JVM 타깃)은 여기서 한다.
- SDK·JVM 버전은 `gradle/libs.versions.toml`의 `[versions]`에서 읽는다. 플러그인 코드에 숫자를 쓰지 않는다.
- 플러그인을 추가·변경하면 `docs/MODULES.md` 1장의 "플러그인" 열과 맞춘다. `domain`용 Kotlin JVM 플러그인은 단위 1에서 추가한다.
- 이 폴더를 고치면 모든 모듈 빌드에 영향을 준다. `./gradlew assembleDebug testDebugUnitTest` 전체로 확인한다.
