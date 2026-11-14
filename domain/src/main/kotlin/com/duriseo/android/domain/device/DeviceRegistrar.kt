package com.duriseo.android.domain.device

import com.duriseo.android.domain.result.DataResult

/**
 * FCM 토큰을 서버에 등록한다 (`PUT /me/devices`, API.md 3.1, NFR-SYNC-01). 알림 권한과 관계없이 등록한다(데이터 메시지는 권한이 필요 없다).
 *
 * - 로그아웃·세션 끊김 상태(토큰 저장소가 비었음)에서는 요청 없이 성공으로 끝낸다.
 * - 오프라인이면 요청 없이 `Offline`이다. 실패는 호출한 쪽이 조용히 넘기고, 다음 앱 시작 때 다시 등록한다.
 * - 토큰은 로그에 남기지 않는다 (NFR-SEC-01).
 */
interface DeviceRegistrar {
    /** 지금 기기의 FCM 토큰([PushTokenProvider])을 얻어 등록한다. 토큰이 없으면(FCM이 꺼짐) 요청 없이 성공이다 */
    suspend fun registerCurrentToken(): DataResult<Unit>

    /** `onNewToken`이 알려 준 새 [token]을 등록한다 */
    suspend fun registerToken(token: String): DataResult<Unit>
}

/**
 * 기기의 FCM 토큰을 얻는다. 구현은 `core:push`(Firebase)에 있고 domain은 Firebase를 모른다.
 * `google-services.json`이 없는 빌드(FCM 꺼짐)나 토큰을 얻지 못했을 때는 null이다.
 */
interface PushTokenProvider {
    suspend fun currentToken(): String?
}
