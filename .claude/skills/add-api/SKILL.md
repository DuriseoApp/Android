---
name: add-api
description: API.md의 경로 하나를 앱에 연결한다 (DTO, Retrofit, 오류 변환, 리포지토리, Room 저장, 테스트). "GET /memos 연결해줘", "API 하나 추가" 같은 요청이나 /dev-unit 중 서버 호출이 필요할 때 쓴다.
---

# API 연결

API.md에 있는 경로만 연결한다. 경로·필드·오류 코드가 문서에 없거나 서로 다르면 **추측하지 말고 멈추고 보고한다.**

## 1. 기준 읽기

1. `../Docs/docs/API.md`의 그 경로: 메서드, 경로, 요청·응답 필드와 타입, null 여부, 페이지(`{items, nextCursor}`), 오류 코드, 권한 규칙(R-), 흐름(4장)이 있으면 그 순서.
2. 1.1 응답 형식: 성공 본문에 봉투가 없다. 목록은 `{items}` 또는 `{items, nextCursor}`, 내용 없는 성공은 204, 오류는 `{code, message, details, traceId}`.
3. 이 데이터가 바뀔 때 오는 동기화 신호(API.md 동기화 신호 장)와 오프라인에서 보여야 하는지(NFR-OFF-01).

## 2. data:remote

- DTO: `@Serializable data class <이름>Dto(...)`. 필드 이름·타입은 API.md 그대로. 날짜는 문자열로 받고 변환은 매퍼에서.
- 서비스: Retrofit 인터페이스 `<도메인>Api`에 메서드 하나. 경로는 `/api/v1/…` 그대로. 204는 `Response<Unit>`.
- 오류: 공용 호출 도우미가 오류 본문을 `ApiError(code, details, traceId)`로 바꾼다. 새 `code`를 쓰면 API.md 5장에 있는지 확인한다.
- 비밀값·토큰을 로그에 찍지 않는다 (OkHttp 로깅은 debug 빌드에서 헤더 제외).

## 3. domain

- 화면이 쓰는 모델이 없으면 `domain`에 만든다 (DTO와 분리). 리포지토리 인터페이스에 메서드를 추가한다.
  - 읽기: `observe…(): Flow<…>` + `refresh…()`
  - 쓰기: `suspend fun create/update/delete…(): Result`

## 4. data:repository, data:local

- 읽기: refresh가 API를 부르고 결과를 Room에 트랜잭션으로 저장한다. 범위 조회는 그 범위를 지우고 새로 넣는다. 화면은 Room Flow만 본다.
- 쓰기: 먼저 온라인인지 본다. 오프라인이면 API를 부르지 않고 `Offline` 오류를 돌려준다 (NFR-OFF-02). 성공하면 응답을 Room에 반영한다. 낙관적 갱신은 하지 않는다.
- 404는 Room에서도 그 행을 지운다.
- Room 엔티티·DAO가 없으면 `data:local`에 만든다. 스키마가 바뀌면 Room 마이그레이션을 추가한다 (출시 전에는 버전을 올리고 destructive fallback 허용, 출시 후에는 마이그레이션 필수).
- 동기화 신호로 refresh를 불러야 하면 `push`가 부를 수 있게 리포지토리 메서드를 연다.

## 5. 테스트

- data:remote (MockWebServer): 요청 메서드·경로·본문 필드, 성공 파싱, 오류 본문 → `ApiError(code)`, 204 처리.
- data:repository (가짜 remote + 메모리 Room): refresh 후 Flow 값, 쓰기 성공 시 Room 반영, 오프라인 쓰기 거부, 404 시 로컬 삭제.
- 테스트 이름에 FR·AC 또는 API 경로를 넣는다.

## 6. 확인

- [ ] 필드·경로·오류 코드가 API.md와 글자까지 같다
- [ ] 화면이 서버 응답을 직접 쓰지 않는다 (Room 경유)
- [ ] 오프라인 쓰기를 실행하지 않는다
- [ ] 토큰·개인 정보가 로그에 없다
- [ ] `./gradlew :data:remote:testDebugUnitTest :data:repository:testDebugUnitTest`
