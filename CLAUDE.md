# 둘이서 Android 레포 규칙

둘이서(커플 일정·메모 앱)의 Android 앱이에요. Kotlin + Jetpack Compose, Clean Architecture 멀티모듈, Hilt + KSP.

## 언어

- 사용자에게는 항상 한국어로 답한다.
- 문서·주석·커밋 메시지도 한국어로 쓴다. 코드 식별자는 영어. 앱 문구는 해요체 (PRD NFR-COPY-01).

## 기준 문서

기획 문서는 `../Docs` 레포(DuriseoApp/Docs의 main)에 있다. **이 문서와 코드가 다르면 문서가 맞다.** 세션은 `claude --add-dir ../Docs`로 연다. 세션을 열 때 `scripts/sync-docs.sh`가 Docs를 최신으로 받는다.

- 요구사항: `../Docs/docs/PRD.md` (커밋·PR·테스트 이름에 FR·SCR ID를 쓴다)
- 화면: `../Docs/docs/SCREENS.md` (화면별 명세. 이미지 `../Docs/design/screens/<SCR ID>.png`, 정확한 크기·색은 `../Docs/design/source/`)
- 디자인 토큰: `../Docs/design/tokens.json`, `../Docs/design/design-system.md` (Compose 매핑 포함)
- API: `../Docs/docs/API.md` (경로, 필드, 오류 코드, 응답 형식 1.1, 동기화 신호)

문서에 없는 화면·문구·필드·오류 코드·토큰 값은 만들지 않는다. 문서가 틀려 보이거나 비어 있으면 **작업을 멈추고 보고한다.** 문서를 고치는 건 Docs 레포의 `/prd-sync`가 한다 (이 레포에서 Docs를 고치지 않는다).

## 이 레포의 문서

| 문서 | 내용 |
| --- | --- |
| `docs/ARCHITECTURE.md` | 레이어, 의존 규칙, 데이터 흐름(Room 구독·온라인 쓰기·SYNC), 오류 모델, 인증 토큰 |
| `docs/MODULES.md` | 모듈 계획, 화면 → 모듈 표, 라이브러리 추가 계획 |
| `docs/CONVENTIONS.md` | MVI·Compose·문구·이름·테스트·커밋 규칙 |
| `docs/PLAN.md` | 작업 단위와 순서. 지금 어디까지 했는지 |
| `docs/SETUP.md` | 맥 설정, `local.properties` 설정값, CI |
| `docs/progress/` | 작업 단위별 진행 기록 |
| 폴더별 `CLAUDE.md` | 그 폴더를 다룰 때만 필요한 규칙 (`app/`, `build-logic/`, `core/designsystem/`, `data/`, `feature/`) |

## 가장 중요한 규칙

1. **의존 방향은 `feature → domain ← data`다.** feature는 data 모듈을 모르고, domain은 Android·data를 모른다 (`docs/ARCHITECTURE.md`).
2. **화면은 Room(로컬)의 Flow만 구독한다.** 서버 응답을 화면에 바로 쓰지 않는다. 쓰기는 온라인에서만 하고, 오프라인이면 실행하지 않고 "인터넷 연결이 필요해요"를 보여 준다 (PRD NFR-OFF-01·02, D-28).
3. **API 필드·경로·오류 코드는 API.md 그대로 쓴다.** 오류는 응답의 `code`로 판단하고 `message`는 화면에 쓰지 않는다 (API.md 1.1).
4. **색·크기·글꼴은 디자인 토큰(`core:designsystem`)으로만 쓴다.** 하드코딩한 색·dp·sp를 화면에 두지 않는다. 문구는 `strings.xml`에 둔다.
5. **비밀값과 비공개 설정값을 레포에 쓰지 않는다.** 서버 주소, Google 웹 클라이언트 ID, 카카오 네이티브 앱 키는 `local.properties`(커밋 안 됨)에서 읽는다. 토큰은 암호화해 저장하고 로그에 남기지 않는다 (NFR-SEC-01).

## 빌드와 확인

- 코드를 고친 뒤 `./gradlew assembleDebug testDebugUnitTest`로 빌드와 단위 테스트를 확인한다. 실패하면 고쳐서 성공할 때까지 반복한다.
- Android SDK나 Google Maven에 닿지 않는 환경(클라우드 세션 등)이면 PR을 연 뒤 `gh pr checks`로 CI 결과를 확인한다. **CI가 초록불이 아니면 작업이 끝난 게 아니다.**

## Git

- `main`에 직접 커밋·push하지 않는다. 작업 브랜치는 `feat/<단위>-<내용>`, `fix/…`, `chore/…`.
- `git push --force`, `git reset --hard`, `rm -rf`를 쓰지 않는다.
- PR은 에이전트가 열고, **머지는 사람이 GitHub에서 한다** (`gh pr merge` 금지).
