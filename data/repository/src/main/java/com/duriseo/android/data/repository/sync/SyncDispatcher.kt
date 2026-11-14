package com.duriseo.android.data.repository.sync

import com.duriseo.android.core.common.Logger
import com.duriseo.android.core.common.coroutines.ApplicationScope
import com.duriseo.android.domain.anniversary.AnniversaryRepository
import com.duriseo.android.domain.auth.TokenStorage
import com.duriseo.android.domain.event.EventRepository
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.memo.MemoRepository
import com.duriseo.android.domain.notification.NotificationRepository
import com.duriseo.android.domain.sync.SpaceSyncSignal
import com.duriseo.android.domain.sync.SyncAction
import com.duriseo.android.domain.sync.SyncResource
import com.duriseo.android.domain.sync.SyncSignal
import com.duriseo.android.domain.sync.SyncSignalReceiver
import com.duriseo.android.domain.sync.SyncSpaceSignals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * 동기화 신호(`type=SYNC`) 처리 (NFR-SYNC-01, API.md 6장 "SYNC 수신 시 앱 동작").
 * `push`가 [receive]로 넘기고, 프로세스 안에서만 1초 안의 신호를 모아([SyncBatcher]) 리소스별로 저장소의 refresh를 부른다.
 * 로그아웃 상태(토큰 없음)면 무시한다. 실패(오프라인 포함)는 조용히 넘긴다: 화면이 진입 때 다시 불러온다 (NFR-SYNC-02).
 *
 * | resource | 처리 |
 * | --- | --- |
 * | `EVENT` | `DELETED`면 기기에서 지운다, 아니면 `id` 단건 `GET` (404면 지움) |
 * | `MEMO` | 같은 단건 처리 + 받은 목록·고정·보이는 달을 다시 불러온다 |
 * | `ANNIVERSARY` | 보이는 범위·upcoming·past |
 * | `SPACE` `UPDATED` | `GET /me`(공간·상대 정보 포함)·기념일 |
 * | `SPACE` `LINKED`·`UNLINKED` | [spaceSignals]로 `MainViewModel`에 알린다 (`GET /me`와 이동은 그쪽) |
 * | `NOTIFICATION` | 안 읽은 개수, 알림 목록이 열려 있으면 첫 페이지도 |
 */
@Singleton
class SyncDispatcher internal constructor(
    private val events: EventRepository,
    private val memos: MemoRepository,
    private val anniversaries: AnniversaryRepository,
    private val me: MeRepository,
    private val notifications: NotificationRepository,
    private val tokenStorage: TokenStorage,
    scope: CoroutineScope,
    windowMillis: Long,
) : SyncSignalReceiver, SyncSpaceSignals {

    @Inject
    constructor(
        events: EventRepository,
        memos: MemoRepository,
        anniversaries: AnniversaryRepository,
        me: MeRepository,
        notifications: NotificationRepository,
        tokenStorage: TokenStorage,
        @ApplicationScope scope: CoroutineScope,
    ) : this(events, memos, anniversaries, me, notifications, tokenStorage, scope, WINDOW_MILLIS)

    private val batcher = SyncBatcher(scope, windowMillis, ::processBatch)

    private val spaceSignalFlow = MutableSharedFlow<SpaceSyncSignal>(extraBufferCapacity = 8)

    override val spaceSignals: Flow<SpaceSyncSignal> = spaceSignalFlow.asSharedFlow()

    override fun receive(signal: SyncSignal) {
        // 로그인하지 않은 기기에 늦게 온 신호는 쓸 곳이 없다 (요청해도 401이다)
        if (tokenStorage.current() == null) return
        batcher.add(signal)
    }

    private suspend fun processBatch(batch: List<SyncSignal>) {
        // 한 신호가 실패해도 나머지는 계속한다
        supervisorScope {
            for (signal in batch) {
                launch { quietly { handle(signal) } }
            }
        }
    }

    private suspend fun handle(signal: SyncSignal) {
        val id = signal.id
        when (signal.resource) {
            SyncResource.EVENT -> {
                if (id == null) return
                if (signal.action == SyncAction.DELETED) events.forget(id) else events.refreshEvent(id)
            }

            SyncResource.MEMO -> {
                if (id == null) return
                if (signal.action == SyncAction.DELETED) memos.forget(id) else memos.refreshMemo(id)
                // 생성·수정·삭제·고정·해제는 목록(받은 보기들)·고정·개수·보이는 달에도 영향을 준다
                memos.refreshAll()
            }

            SyncResource.ANNIVERSARY -> anniversaries.refreshAll()

            SyncResource.SPACE -> when (signal.action) {
                SyncAction.UPDATED -> supervisorScope {
                    launch { quietly { me.refreshMe() } }
                    launch { quietly { anniversaries.refreshAll() } }
                }

                SyncAction.LINKED -> spaceSignalFlow.tryEmit(SpaceSyncSignal.LINKED)
                SyncAction.UNLINKED -> spaceSignalFlow.tryEmit(SpaceSyncSignal.UNLINKED)
                else -> Unit
            }

            SyncResource.NOTIFICATION -> supervisorScope {
                launch { quietly { notifications.refreshUnreadCount() } }
                // 알림 목록이 열려 있을 때만 첫 페이지도 다시 부른다 (D-141)
                if (notifications.isListOpen) launch { quietly { notifications.refreshFirstPage() } }
            }
        }
    }

    /** 취소는 그대로 던지고, 그 밖의 예외는 로그만 남긴다 (신호 내용·토큰은 찍지 않는다) */
    private suspend fun quietly(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(TAG, "동기화 신호 처리에 실패했어요: ${e.javaClass.simpleName}")
        }
    }

    internal companion object {
        /** 연속으로 온 신호를 모으는 시간 (API.md 6장) */
        const val WINDOW_MILLIS = 1_000L
        private const val TAG = "sync"
    }
}
