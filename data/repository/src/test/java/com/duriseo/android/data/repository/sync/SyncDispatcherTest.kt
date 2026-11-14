package com.duriseo.android.data.repository.sync

import com.duriseo.android.data.repository.FakeTokenStorage
import com.duriseo.android.data.repository.testTokens
import com.duriseo.android.domain.anniversary.AnniversaryRepository
import com.duriseo.android.domain.event.EventRepository
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.memo.MemoRepository
import com.duriseo.android.domain.notification.NotificationRepository
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.domain.sync.SpaceSyncSignal
import com.duriseo.android.domain.sync.SyncAction
import com.duriseo.android.domain.sync.SyncResource
import com.duriseo.android.domain.sync.SyncSignal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.Continuation

/**
 * 동기화 신호 처리 (NFR-SYNC-01, API.md 6장 "SYNC 수신 시 앱 동작"). 저장소는 부른 함수만 기록하는 가짜다
 * (`Proxy`: 호출 이름과 인자를 `calls`에 적고 성공으로 돌려준다). 시간은 가상 시간이라 실제로 1초를 기다리지 않는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncDispatcherTest {

    private val calls = CopyOnWriteArrayList<String>()
    private var listOpen = false

    /** 인터페이스의 모든 메서드를 "이름(인자)"로 기록하고 성공으로 돌려주는 가짜. suspend 함수의 마지막 인자(Continuation)는 적지 않는다 */
    private inline fun <reified T : Any> recording(prefix: String): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
            if (method.name == "isListOpen") return@newProxyInstance listOpen
            val shown = args.orEmpty().filter { it !is Continuation<*> }.joinToString(",")
            calls += "$prefix.${method.name}($shown)"
            if (method.returnType == Void.TYPE) null else DataResult.Success(Unit)
        } as T

    private fun TestScope.dispatcher(signedIn: Boolean = true) = SyncDispatcher(
        events = recording<EventRepository>("event"),
        memos = recording<MemoRepository>("memo"),
        anniversaries = recording<AnniversaryRepository>("anniversary"),
        me = recording<MeRepository>("me"),
        notifications = recording<NotificationRepository>("notification"),
        tokenStorage = FakeTokenStorage(if (signedIn) testTokens() else null).also { storage ->
            // 로그아웃 상태: 토큰 저장소를 읽었더니 비었다
            if (!signedIn) kotlinx.coroutines.runBlocking { storage.restore() }
        },
        scope = this,
        windowMillis = 1_000,
    )

    private fun signal(resource: SyncResource, action: SyncAction = SyncAction.UPDATED, id: String? = null) =
        SyncSignal(resource, action, id)

    private suspend fun TestScope.settle() {
        advanceTimeBy(1_000)
        runCurrent()
    }

    @Test
    fun `NFR-SYNC-01 1초 안에 온 신호는 한 번만 처리한다`() = runTest {
        val dispatcher = dispatcher()

        dispatcher.receive(signal(SyncResource.EVENT, id = "e1"))
        advanceTimeBy(300)
        dispatcher.receive(signal(SyncResource.EVENT, id = "e1")) // 같은 리소스·id는 하나로 합친다
        advanceTimeBy(300)
        dispatcher.receive(signal(SyncResource.EVENT, id = "e2"))
        advanceTimeBy(300)
        runCurrent()
        // 첫 신호로부터 1초가 지나기 전에는 처리하지 않는다
        assertEquals(emptyList<String>(), calls.toList())

        advanceTimeBy(100)
        runCurrent()

        assertEquals(listOf("event.refreshEvent(e1)", "event.refreshEvent(e2)"), calls.sorted())
        assertEquals(1, calls.count { it == "event.refreshEvent(e1)" })

        // 묶음이 끝난 뒤 온 신호는 새 묶음이다
        calls.clear()
        dispatcher.receive(signal(SyncResource.EVENT, id = "e1"))
        settle()
        assertEquals(listOf("event.refreshEvent(e1)"), calls.toList())
    }

    @Test
    fun `NFR-SYNC-01 같은 일정의 UPDATED 뒤 DELETED는 마지막 DELETED만 처리한다`() = runTest {
        val dispatcher = dispatcher()

        dispatcher.receive(signal(SyncResource.EVENT, SyncAction.UPDATED, "e1"))
        dispatcher.receive(signal(SyncResource.EVENT, SyncAction.DELETED, "e1"))
        settle()

        assertEquals(listOf("event.forget(e1)"), calls.toList())
    }

    @Test
    fun `NFR-SYNC-01 신호가 없으면 아무 일도 예약하지 않는다`() = runTest {
        dispatcher()

        advanceTimeBy(10_000)
        runCurrent()

        assertEquals(emptyList<String>(), calls.toList())
    }

    @Test
    fun `EVENT UPDATED는 그 일정을 다시 읽고 DELETED는 기기에서 지운다`() = runTest {
        val dispatcher = dispatcher()

        dispatcher.receive(signal(SyncResource.EVENT, SyncAction.CREATED, "e1"))
        dispatcher.receive(signal(SyncResource.EVENT, SyncAction.UPDATED, "e2"))
        dispatcher.receive(signal(SyncResource.EVENT, SyncAction.DELETED, "e3"))
        settle()

        assertEquals(
            listOf("event.forget(e3)", "event.refreshEvent(e1)", "event.refreshEvent(e2)"),
            calls.sorted(),
        )
    }

    @Test
    fun `MEMO 신호는 메모를 다시 읽고 목록 고정을 갱신한다`() = runTest {
        val dispatcher = dispatcher()

        dispatcher.receive(signal(SyncResource.MEMO, SyncAction.UPDATED, "m1"))
        settle()
        assertEquals(listOf("memo.refreshMemo(m1)", "memo.refreshAll()"), calls.toList())

        // 삭제는 기기에서 지우고 목록·고정은 다시 불러온다
        calls.clear()
        dispatcher.receive(signal(SyncResource.MEMO, SyncAction.DELETED, "m2"))
        settle()
        assertEquals(listOf("memo.forget(m2)", "memo.refreshAll()"), calls.toList())
    }

    @Test
    fun `ANNIVERSARY 신호는 기념일을 다시 불러온다`() = runTest {
        val dispatcher = dispatcher()

        dispatcher.receive(signal(SyncResource.ANNIVERSARY, SyncAction.CREATED))
        dispatcher.receive(signal(SyncResource.ANNIVERSARY, SyncAction.DELETED, "a1"))
        settle()

        // 신호 둘이 하나로 모인다. 보이는 범위·upcoming·past는 저장소 refreshAll이 다시 부른다
        assertEquals(listOf("anniversary.refreshAll()"), calls.toList())
    }

    @Test
    fun `SPACE UPDATED는 내 정보 공간 기념일을 다시 읽는다`() = runTest {
        val dispatcher = dispatcher()

        dispatcher.receive(signal(SyncResource.SPACE, SyncAction.UPDATED))
        settle()

        // API.md 6장: GET /me, GET /space, 기념일을 다시 읽는다 (진행 기록 결정 3)
        assertEquals(listOf("anniversary.refreshAll()", "me.refreshMe()", "me.refreshSpace()"), calls.sorted())
    }

    @Test
    fun `NOTIFICATION 신호는 안 읽은 개수를 다시 읽는다`() = runTest {
        val dispatcher = dispatcher()

        dispatcher.receive(signal(SyncResource.NOTIFICATION, SyncAction.CREATED, "n1"))
        dispatcher.receive(signal(SyncResource.NOTIFICATION, SyncAction.CREATED, "n2"))
        settle()

        // 알림 목록이 닫혀 있으면 개수만 다시 읽는다
        assertEquals(listOf("notification.refreshUnreadCount()"), calls.toList())
    }

    @Test
    fun `NOTIFICATION 신호는 알림 목록이 열려 있으면 첫 페이지도 다시 부른다 D-141`() = runTest {
        val dispatcher = dispatcher()
        listOpen = true

        dispatcher.receive(signal(SyncResource.NOTIFICATION, SyncAction.CREATED, "n1"))
        settle()

        assertEquals(listOf("notification.refreshFirstPage()", "notification.refreshUnreadCount()"), calls.sorted())
    }

    @Test
    fun `SPACE LINKED와 UNLINKED는 저장소를 부르지 않고 MainViewModel이 듣는 흐름으로 알린다`() = runTest {
        val dispatcher = dispatcher()
        val received = mutableListOf<SpaceSyncSignal>()
        backgroundScope.launch { dispatcher.spaceSignals.collect { received += it } }
        runCurrent()

        dispatcher.receive(signal(SyncResource.SPACE, SyncAction.LINKED))
        dispatcher.receive(signal(SyncResource.SPACE, SyncAction.UNLINKED))
        settle()

        assertEquals(listOf(SpaceSyncSignal.LINKED, SpaceSyncSignal.UNLINKED), received)
        // GET /me와 이동은 MainViewModel이 한다
        assertEquals(emptyList<String>(), calls.toList())
    }

    @Test
    fun `API 6장 id가 없는 EVENT·MEMO와 리소스에 맞지 않는 action은 버린다`() = runTest {
        val dispatcher = dispatcher()

        dispatcher.receive(signal(SyncResource.EVENT, SyncAction.UPDATED, null))
        dispatcher.receive(signal(SyncResource.MEMO, SyncAction.UPDATED, " "))
        dispatcher.receive(signal(SyncResource.EVENT, SyncAction.LINKED, "e1"))
        dispatcher.receive(signal(SyncResource.SPACE, SyncAction.DELETED))
        settle()

        assertEquals(emptyList<String>(), calls.toList())
    }

    @Test
    fun `로그인하지 않은 상태에 온 신호는 무시한다`() = runTest {
        val dispatcher = dispatcher(signedIn = false)

        dispatcher.receive(signal(SyncResource.NOTIFICATION, SyncAction.CREATED, "n1"))
        settle()

        assertTrue(calls.isEmpty())
    }
}
