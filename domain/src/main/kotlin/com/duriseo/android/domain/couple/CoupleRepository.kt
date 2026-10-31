package com.duriseo.android.domain.couple

import com.duriseo.android.domain.result.DataResult
import java.time.LocalDate

/**
 * 커플 연결·해제와 공간 설정 (API.md 3.2, 4.4, 4.5).
 *
 * - 쓰기(`issueInviteCode`·`join`·`decideImport`·`updateStartDate`·`updateColors`·`unlink`)는 온라인에서만 한다.
 *   오프라인이면 요청 없이 `DataError.Offline` (NFR-OFF-02, D-28).
 * - 쓰기가 성공하면 `GET /me`로 Room을 새로 채운다. 화면은 `MeRepository.observeMe()`만 구독한다
 *   (낙관적 갱신 없음, ARCHITECTURE.md 2장). 이 갱신이 실패해도 쓰기 결과는 성공이다.
 */
interface CoupleRepository {

    /**
     * `GET /invite-codes/current`. 유효한 코드가 없으면(404 `NOT_FOUND`) 오류가 아니라 `Success(null)`이고,
     * 부른 쪽이 [issueInviteCode]를 부른다 (API.md 4.4의 1단계).
     */
    suspend fun currentInviteCode(): DataResult<InviteCode?>

    /**
     * `POST /invite-codes`. 이전 코드는 즉시 만료된다. 마지막 발급 후 60초 안이면 429 `INVITE_REISSUE_TOO_SOON`
     * (`ErrorDetails.RetryAfter`)이다.
     */
    suspend fun issueInviteCode(): DataResult<InviteCode>

    /**
     * `POST /couple/join {code}`. [code]는 부른 쪽이 대문자 6자리로 만들어 보낸다(서버도 공백 제거·대문자 변환).
     * 성공하면 `GET /me`로 Room을 채운다.
     */
    suspend fun join(code: String): DataResult<CoupleJoinResult>

    /** `POST /couple/import {import}` -> 204. 성공하면 `GET /me` (`importPending`이 false로 바뀜) */
    suspend fun decideImport(import: Boolean): DataResult<Unit>

    /** `PATCH /space {startDate}`. 지우기는 없다 (D-84). 성공하면 `GET /me` */
    suspend fun updateStartDate(date: LocalDate): DataResult<Unit>

    /** `PATCH /space/colors`. [change]가 비었으면 보내지 않고 성공이다. 성공하면 `GET /me` */
    suspend fun updateColors(change: ColorChange): DataResult<Unit>

    /**
     * `DELETE /couple` (FR-COUPLE-05). 204이거나 409 `NOT_COUPLE`(이미 해제됨, 4.5의 1단계)이면
     * 토큰은 두고 Room·설정을 지운 뒤(앱 초기화, D-37) `Success`다. 부른 쪽이 SCR-COUPLE-01로 보낸다.
     */
    suspend fun unlink(): DataResult<Unit>
}
