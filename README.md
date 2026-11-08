# 둘이서 Android

둘이서(커플 일정·메모 앱)의 Android 앱이에요. Kotlin + Jetpack Compose, Clean Architecture 멀티모듈, Hilt + KSP. 기획 문서는 [DuriseoApp/Docs](https://github.com/DuriseoApp/Docs)에 있어요.

- 개발 규칙은 `CLAUDE.md`, 구조는 `docs/ARCHITECTURE.md`, 모듈은 `docs/MODULES.md`, 코드 규칙은 `docs/CONVENTIONS.md`, 작업 순서와 지금 위치는 `docs/PLAN.md`, 맥 설정은 `docs/SETUP.md`를 봐요.
- 작업 단위 하나는 `/dev-unit`(`.claude/skills/`)으로 진행하고, 진행 기록은 `docs/progress/`에 남겨요 (끝난 단위는 `docs/progress/archive/`).

```bash
./gradlew assembleDebug testDebugUnitTest
```
