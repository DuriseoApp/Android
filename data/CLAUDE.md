# data 모듈 (remote, local, repository)

- `data:remote`: Retrofit 서비스와 DTO. 필드·경로·오류 코드는 `../Docs/docs/API.md` 그대로. 응답 형식은 API.md 1.1 (성공은 봉투 없음, 목록 `{items, nextCursor}`, 204, 오류 `{code, message, details, traceId}`). 오류는 `ApiError(code, details, traceId)`로 바꾸고, 네트워크 오류는 `Offline`으로 구분한다.
- `data:local`: Room(오프라인 보기용 캐시), DataStore(설정), 암호화 토큰 저장. 로그아웃·탈퇴·다른 기기 로그인으로 세션이 끝나면 모두 지운다.
- `data:repository`: domain 인터페이스 구현. 읽기는 Room Flow + refresh(API → Room 트랜잭션), 쓰기는 온라인 확인 → API → 성공 시 Room 반영. 낙관적 갱신 없음 (`docs/ARCHITECTURE.md` 2장).
- 새 API는 `/add-api` 절차로 한다.
- OkHttp 로깅은 debug 빌드에서만, `Authorization` 헤더는 가린다.
