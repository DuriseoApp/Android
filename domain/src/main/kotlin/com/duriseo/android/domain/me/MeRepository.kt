package com.duriseo.android.domain.me

import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.Flow

/** 내 정보. 화면은 Room만 구독한다 (ARCHITECTURE.md 2장) */
interface MeRepository {
    /** Room의 내 정보. 아직 받은 적이 없으면 null */
    fun observeMe(): Flow<Me?>

    /**
     * `GET /me` -> Room. 오프라인이면 요청 없이 `Offline`이고 저장된 값은 그대로 (NFR-OFF-01).
     *
     * 저장하기 **전에** 해제를 살핀다 (API.md 4.5의 6단계): 받은 공간이 개인 공간인데 ① 확인한 공간 ID와 다르거나
     * ② Room에 저장된 공간이 커플이었으면 해제로 보고 로그인 정보를 뺀 로컬 데이터를 지운 뒤 받은 내 정보를 저장하고
     * [spaceChanges]에 [SpaceChange.UNLINKED]를 낸다. 그다음 받은 공간이 개인 공간이면 그 ID를 확인한 공간 ID로 저장한다
     * (4.4의 8단계 저장 시점 1).
     */
    suspend fun refreshMe(): DataResult<Unit>

    /** 해제를 알았을 때의 알림 (구독자는 하나, 구독자가 없을 때 난 것은 첫 구독자에게 전달) */
    val spaceChanges: Flow<SpaceChange>

    /**
     * 연결 완료 시트(SCR-COUPLE-04)를 띄워야 하는지. Room의 공간이 커플이고, 가져오기 대기(`importPending`)가 있거나
     * 확인한 공간 ID가 있는데 그 공간과 다르면 true다 (API.md 4.4의 8·10단계, D-98). 확인한 공간 ID가 없는 기기
     * (처음 로그인)는 변화로 보지 않고 `importPending`만 본다.
     */
    fun observeLinkedSheetNeeded(): Flow<Boolean>

    /** 시트를 닫거나 [시작하기]를 마쳤다: Room의 현재 공간 ID를 확인한 공간 ID로 저장한다 (4.4의 8단계 저장 시점 2) */
    suspend fun acknowledgeSpace()

    /**
     * 연결 해제를 알았을 때의 앱 초기화 (D-103): 로그인 정보를 뺀 로컬 데이터를 지우고 `GET /me`로 새 개인 공간을 채운 뒤
     * [SpaceChange.UNLINKED]를 낸다. `DELETE /couple` 응답 직후, 그리고 `PATCH /space`·`PATCH /space/colors`·
     * `POST /couple/import`가 409 `NOT_COUPLE`을 줬을 때 부른다. `GET /me`가 실패해도(오프라인) 지운 채로 끝낸다.
     */
    suspend fun resetAfterUnlink()

    /**
     * 기기 시간대 [zoneId]가 저장된 `Me.timeZone`과 다르면 `PATCH /me {timeZone}` -> Room (NFR-TIME-01).
     * 같거나 저장된 내 정보가 없으면 보내지 않는다. 오프라인이면 요청 없이 `Offline`. 호출한 쪽은 실패를 조용히 넘긴다.
     */
    suspend fun syncTimeZone(zoneId: String): DataResult<Unit>
}
