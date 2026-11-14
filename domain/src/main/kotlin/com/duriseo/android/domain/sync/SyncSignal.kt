package com.duriseo.android.domain.sync

import kotlinx.coroutines.flow.Flow

/** 동기화 신호의 `resource` (API.md 6장) */
enum class SyncResource { EVENT, ANNIVERSARY, MEMO, SPACE, NOTIFICATION }

/** 동기화 신호의 `action` (API.md 6장). `UNLINKED`·`LINKED`는 `SPACE`만 쓴다 */
enum class SyncAction { CREATED, UPDATED, DELETED, UNLINKED, LINKED }

/**
 * FCM 데이터 메시지 `type=SYNC` 하나 (API.md 6장). [id]는 `EVENT`·`MEMO`·`NOTIFICATION`에 있고
 * `ANNIVERSARY`·`SPACE`에는 없을 수 있다 (`LINKED`·`UNLINKED`는 `id` 없음).
 */
data class SyncSignal(
    val resource: SyncResource,
    val action: SyncAction,
    val id: String? = null,
)

/**
 * `push`가 받은 신호를 넘기는 곳. 구현(`SyncDispatcher`)은 `data:repository`에 있다.
 * 프로세스가 살아 있을 때만 처리한다 (API.md 6장): 프로세스가 없으면 `FirebaseMessagingService`가 신호를 받아도
 * 이 함수가 불리지 않고 버려진다 (다음 진입 때 NFR-SYNC-02로 다시 읽는다).
 */
interface SyncSignalReceiver {
    /** 기다리지 않고 돌아온다. 1초 안에 온 신호는 모아 한 번만 처리한다 (NFR-SYNC-01) */
    fun receive(signal: SyncSignal)
}

/**
 * `LINKED`·`UNLINKED` 신호를 `feature:main`의 `MainViewModel`에 알린다. `GET /me`를 읽은 뒤 해제·연결 경로(D-125)로 가는 일은
 * `MainViewModel`이 한다. 구독자가 없을 때 온 신호는 버려진다 (다음 `GET /me`가 Room을 바로잡는다).
 */
interface SyncSpaceSignals {
    val spaceSignals: Flow<SpaceSyncSignal>
}

/** 공간 신호 두 가지 (API.md 6장 `SPACE`의 `LINKED`·`UNLINKED`) */
enum class SpaceSyncSignal { LINKED, UNLINKED }
