package com.duriseo.android.data.repository.sync

import com.duriseo.android.domain.sync.SyncAction
import com.duriseo.android.domain.sync.SyncResource
import com.duriseo.android.domain.sync.SyncSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 동기화 신호를 모은다 (NFR-SYNC-01, API.md 6장 "연속으로 온 신호는 1초 안에 온 것을 모아 한 번만 처리한다").
 *
 * 첫 신호가 오면 [windowMillis] 뒤에 한 번 처리하는 일을 예약하고, 그 안에 온 신호는 같은 묶음에 넣는다.
 * 같은 리소스·`id`는 하나로 합치고 마지막에 온 것이 남는다 (일정 `UPDATED` 뒤 `DELETED`면 `DELETED`).
 * 신호가 올 때만 도는 구조라 신호가 없으면 코루틴이 없다 (끝없는 대기 루프 없음).
 * 묶는 동안 막힌 신호가 계속 이어져도 지연은 [windowMillis]를 넘지 않는다 (처음 신호 기준 고정 창).
 */
internal class SyncBatcher(
    private val scope: CoroutineScope,
    private val windowMillis: Long,
    private val process: suspend (List<SyncSignal>) -> Unit,
) {
    private val lock = Any()
    private val pending = LinkedHashMap<String, SyncSignal>()
    private var flushJob: Job? = null

    fun add(signal: SyncSignal) {
        val key = keyOf(signal) ?: return
        synchronized(lock) {
            pending.remove(key) // 마지막에 온 것이 뒤에 오게 순서를 갱신한다
            pending[key] = signal
            if (flushJob?.isActive != true) {
                flushJob = scope.launch {
                    delay(windowMillis)
                    val batch = synchronized(lock) {
                        pending.values.toList().also {
                            pending.clear()
                            flushJob = null
                        }
                    }
                    process(batch)
                }
            }
        }
    }

    internal companion object {
        /**
         * 같은 처리를 가리키는 키. `EVENT`·`MEMO`는 `id`마다, `SPACE`는 `action`마다, 나머지는 리소스 하나다.
         * 앱이 처리할 수 없는 모양(`id` 없는 `EVENT`·`MEMO`, 리소스에 맞지 않는 `action`)은 null이라 버린다.
         */
        fun keyOf(signal: SyncSignal): String? = when (signal.resource) {
            SyncResource.EVENT, SyncResource.MEMO -> {
                val id = signal.id?.takeIf { it.isNotBlank() }
                if (id == null || signal.action in SPACE_ONLY) null else "${signal.resource}:$id"
            }

            SyncResource.ANNIVERSARY -> if (signal.action in SPACE_ONLY) null else "ANNIVERSARY"
            SyncResource.NOTIFICATION -> if (signal.action in SPACE_ONLY) null else "NOTIFICATION"
            // `SPACE`는 `UPDATED`·`LINKED`·`UNLINKED`만 처리한다
            SyncResource.SPACE -> if (signal.action in SPACE_ACTIONS) "SPACE:${signal.action}" else null
        }

        private val SPACE_ONLY = setOf(SyncAction.LINKED, SyncAction.UNLINKED)
        private val SPACE_ACTIONS = setOf(SyncAction.UPDATED, SyncAction.LINKED, SyncAction.UNLINKED)
    }
}
