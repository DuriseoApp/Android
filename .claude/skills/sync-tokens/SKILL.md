---
name: sync-tokens
description: Docs의 디자인 토큰(design/tokens.json)을 core:designsystem의 Compose 테마에 반영한다. "토큰 반영해줘", "디자인 시스템 업데이트", Docs의 /design-sync PR이 머지된 뒤나 Android 디자인 이슈를 처리할 때 쓴다.
---

# 디자인 토큰 반영

`../Docs/design/tokens.json`이 기준이다. **값을 지어내지 않는다.** 토큰에 없거나 `null`인 값은 만들지 않고, 필요한 곳은 멈추고 보고한다 (토큰 추가는 Docs의 `/design-sync`가 한다).

## 1. 기준 읽기

1. `../Docs/design/tokens.json` 전체와 `$meta.updated`.
2. `../Docs/design/design-system.md`의 "Android (Jetpack Compose) 매핑" 표: Material 3 역할 ↔ 토큰, Typography·Shapes 대응, `PersonColors`, 다크 모드 여부.
3. 지난 반영 이후 바뀐 것: `git -C ../Docs log --oneline -- design/tokens.json`과 diff.

## 2. 반영 위치

`core/designsystem/src/main/java/com/duriseo/android/core/designsystem/theme/`

| 토큰 | 파일 | 방법 |
| --- | --- | --- |
| `color.*` | `Color.kt` | 토큰 이름을 camelCase로 (`brand-500` → `Brand500`). 값은 hex 그대로 |
| Material 역할 | `Theme.kt` `lightColorScheme(...)` | 매핑 표 그대로. 표에 없는 역할은 기본값을 두지 말고 매핑 표에 있는 것만 지정 |
| `person-*`, `pick` | `PersonColors.kt` | `data class PersonColors(me, partner, together)` + `LocalPersonColors`. `pick` 팔레트는 키(`pick-*`) → 색 맵. 서버의 팔레트 키를 그대로 쓴다 (ERD `space_members.color`) |
| `font`, `typography.*` | `Type.kt` | 매핑 표의 `Typography` 대응. `size`·`lineHeight`는 sp, `weight`는 `FontWeight`. 글꼴 파일이 필요하면 `res/font`에 두고 라이선스를 확인한다 |
| `radius.*` | `Shape.kt` | 매핑 표의 `Shapes` 대응 + 나머지는 `DuriseoShapes` 상수 |
| `space.*`, `size.*` | `Dimens.kt` | `object DuriseoSpacing`, `object DuriseoSize`. 값은 dp |
| `shadow.*` | `Elevation.kt` | Compose 그림자로 표현 가능한 값만. 표현할 수 없으면 주석으로 원본 값을 남긴다 |
| `icon` | 아이콘 그리기 규칙 | 아이콘 리소스를 만들 때 격자·선 두께를 따른다 |

- 다크 모드는 design-system.md에 정의되기 전까지 만들지 않는다 (라이트만).
- `usage`는 각 토큰을 어디에 쓰는지 설명이다. KDoc 주석으로 옮긴다.
- 토큰 이름이 바뀌거나 없어지면 그 상수를 쓰는 곳을 모두 고친다. 같은 값을 다른 이름으로 남겨 두지 않는다.

## 3. 확인

1. 화면 코드에 하드코딩한 색·dp가 생기지 않았는지: `grep -rn "Color(0x\|\.dp\b" feature/ --include=*.kt`로 보고, 토큰을 거치지 않는 값은 고친다.
2. 테마 Preview(색 견본, 글꼴 견본)를 `core:designsystem`에 두고 갱신한다.
3. `./gradlew :core:designsystem:assembleDebug assembleDebug`

## 4. PR

- 커밋: `chore(designsystem): 디자인 토큰 반영 (tokens.json <updated 날짜>)`
- 본문: 바뀐 토큰 목록(추가·변경·삭제), 영향을 받는 화면, `null`이라 반영하지 않은 값, 관련 Android 이슈(`Closes #N`).
