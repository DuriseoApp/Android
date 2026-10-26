# feature 모듈

- 화면 하나 = Contract(UiState·Action·SideEffect) + ViewModel + Route/Screen + navigation. 새 화면은 `/new-screen`, 새 모듈은 `/new-feature-module`.
- feature는 `domain`, `core:designsystem`, `core:common`에만 의존한다. `data:*`, 다른 `feature:*`에 의존하지 않는다 (`feature:main`은 예외: 모든 feature를 모아 내비게이션을 연결).
- 화면 구성·문구·상태는 `../Docs/docs/SCREENS.md` 그대로. 화면이 어느 모듈인지는 `docs/MODULES.md` 2장.
- 하단 탭은 캘린더·메모 두 개 (D-30). 전체 메뉴(설정)는 캘린더 앱바에서 들어간다.
- 쓰기 동작은 오프라인이면 실행하지 않고 "인터넷 연결이 필요해요"를 보여 준다 (NFR-OFF-02).
