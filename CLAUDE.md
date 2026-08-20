# CLAUDE.md

## Build

- 코드를 새로 생성하거나 수정한 후에는 반드시 `./gradlew assembleDebug` 로 빌드를 확인할 것
- 빌드 실패 시 에러를 분석하고 수정한 뒤 다시 빌드를 돌려 성공할 때까지 반복할 것

## Project Structure

- Multi-module Android project (app, feature/*, core/*, data/*)
- Version catalog: `gradle/libs.versions.toml`
- DI: Hilt + KSP
- UI: Jetpack Compose
- Navigation: AndroidX Navigation Compose with typed routes
