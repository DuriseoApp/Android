# 단위 1 템플릿 정리 목록

`IamJuco/android-template`에서 시작해서 남아 있는 것을 단위 1에서 정리한다. **단위 1이 머지되면 이 파일을 지운다.**

- [x] `feature:home` → `feature:calendar` (모듈 폴더, `settings.gradle.kts`, namespace, 패키지, 클래스 이름, 내비게이션)
- [x] `feature:menu` → `feature:settings` (같은 방법)
- [x] `feature:main`의 하단 탭을 캘린더·메모 두 개로 (D-30). 메모 탭은 `feature:memo`가 생기기 전까지 빈 화면 자리표시 (탭 수치·아이콘은 Docs 보충 PR 머지 뒤 `TODO(Docs PR)` 자리에 반영)
- [x] `feature:main/component/MainMenu.kt`처럼 템플릿 예시 화면·문구 정리
- [x] `ExampleUnitTest`, `ExampleInstrumentedTest` 템플릿 테스트 삭제 (실제 테스트가 생기는 모듈부터)
- [x] `core:designsystem` 테마를 템플릿 값에서 `tokens.json` 값으로 (`/sync-tokens`). 매핑에 없는 M3 역할·글자 스타일은 Docs 보충 PR 뒤에 (`TODO(Docs PR)`)
- [x] `gradle/libs.versions.toml`에서 쓰지 않는 라이브러리 정리, 오래된 버전 확인 (Compose BOM 등)
- [ ] 이 파일 삭제, 루트 `CLAUDE.md`와 `android-reviewer`의 단위 1 관련 줄 확인
