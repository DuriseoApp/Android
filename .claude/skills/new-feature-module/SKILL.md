---
name: new-feature-module
description: feature:<이름> 모듈을 새로 만들고 앱에 연결한다. "feature:memo 모듈 만들어줘", "새 기능 모듈" 같은 요청이나 /dev-unit 중 모듈이 필요할 때 쓴다.
---

# 기능 모듈 추가

`docs/MODULES.md`의 모듈 계획에 있는 모듈만 만든다. 계획에 없으면 멈추고, 어느 화면을 위한 모듈인지와 함께 사용자에게 묻는다.

## 1. 만들기

1. 폴더: `feature/<이름>/` (이름은 영어 소문자 한 단어. 예: `memo`, `anniversary`)
2. `settings.gradle.kts`에 `include(":feature:<이름>")`를 기존 줄 근처에 추가한다.
3. `feature/<이름>/build.gradle.kts`:
   ```kotlin
   plugins {
       id("duriseo.android.feature")
   }

   android {
       namespace = "com.duriseo.android.feature.<이름>"
   }

   dependencies {
       implementation(projects.core.designsystem)
       implementation(projects.core.common)
       implementation(projects.domain)

       // 테스트 도구(JUnit, 코루틴 테스트, Turbine)는 duriseo.android.feature 플러그인이 넣어 준다
   }
   ```
   - `data:*` 모듈에 의존하지 않는다. 리포지토리 구현은 Hilt가 `data:repository`에서 넣어 준다.
   - 다른 `feature:*`에 의존하지 않는다.
4. `feature/<이름>/src/main/AndroidManifest.xml`은 빈 manifest로 둔다.
5. 패키지 폴더 `src/main/java/com/duriseo/android/feature/<이름>/`와 `navigation/` 패키지를 만든다. 화면은 `/new-screen`으로 채운다.
6. 템플릿 예시 테스트(`ExampleUnitTest`, `ExampleInstrumentedTest`)는 만들지 않는다.

## 2. 연결

1. `feature:main/build.gradle.kts`에 `implementation(projects.feature.<이름>)`을 추가한다.
2. `MainNavHost`에 그 모듈의 그래프를 넣는다.
3. 하단 탭은 캘린더·메모 두 개뿐이다 (D-30). 탭을 늘리지 않는다. 다른 화면은 앱바·메뉴에서 들어간다 (SCREENS.md).

## 3. 문서

같은 PR에서 `docs/MODULES.md` 1장의 그 모듈 줄을 "있음"으로 바꾸고, 2장 화면 표가 맞는지 확인한다.

## 4. 확인

`./gradlew :feature:<이름>:assembleDebug assembleDebug` (SDK가 없는 환경이면 PR CI로).
