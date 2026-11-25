package com.duriseo.android.data.repository.appops

import com.duriseo.android.data.remote.appops.AppOpsApi
import com.duriseo.android.data.remote.appops.AppOpsRemoteDataSource
import com.duriseo.android.data.remote.appops.RemoteConfigGateway
import com.duriseo.android.data.remote.appops.RemoteConfigValues
import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.data.remote.json.createApiJson
import com.duriseo.android.domain.appops.AppEnvironment
import com.duriseo.android.domain.appops.AppPlatform
import com.duriseo.android.domain.appops.AppPopupResolver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.time.Instant

/** 실행 때 두 곳 확인 (FR-APP-01 AC6, API.md 4.9 2단계). 가상 시간으로 3초를 확인한다 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppPopupRepositoryTest {

    private val now = Instant.parse("2026-11-25T03:00:00Z")
    private val env = AppEnvironment(AppPlatform.ANDROID, build = 10, osVersion = 34)

    private class FakeApi(
        private val noticesBody: String = """{"items":[]}""",
        private val rulesBody: String = """{"items":[]}""",
        private val delayMillis: Long = 0,
        private val fail: Boolean = false,
    ) : AppOpsApi {
        override suspend fun getAppNotices(): Response<ResponseBody> = respond(noticesBody)
        override suspend fun getAppUpdateRules(): Response<ResponseBody> = respond(rulesBody)

        private suspend fun respond(text: String): Response<ResponseBody> {
            delay(delayMillis)
            if (fail) throw IOException("연결 없음")
            return Response.success(text.toResponseBody("application/json".toMediaType()))
        }
    }

    private class FakeGateway(
        var values: RemoteConfigValues = RemoteConfigValues(null, null),
        var fetchMillis: Long = 0,
        var fetchOk: Boolean = true,
        /** 가져오기가 끝나면 이 값으로 바뀐다 */
        var valuesAfterFetch: RemoteConfigValues? = null,
    ) : RemoteConfigGateway {
        val updates = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
        override suspend fun fetchAndActivate(): Boolean {
            delay(fetchMillis)
            if (fetchOk) valuesAfterFetch?.let { values = it }
            return fetchOk
        }

        override fun currentValues(): RemoteConfigValues = values
        override fun updates(): Flow<Unit> = updates
    }

    private fun TestScope.repository(api: AppOpsApi, gateway: RemoteConfigGateway) = AppOpsRepositoryImpl(
        remote = AppOpsRemoteDataSource(api, ApiCaller(createApiJson()), StandardTestDispatcher(testScheduler)),
        gateway = gateway,
        scope = backgroundScope,
    )

    private fun notices(vararg ids: String) =
        """{"items":[${ids.joinToString(",") { """{"id":"$it","closable":true,"title":"제목 $it"}""" }}]}"""

    @Test
    fun `FR-APP-01 AC6 서버가 3초 안에 답하지 않으면 Remote Config 값만 쓴다`() = runTest {
        val gateway = FakeGateway(RemoteConfigValues(notices("rc"), null))
        val repo = repository(FakeApi(noticesBody = notices("server"), delayMillis = 10_000), gateway)

        val snapshot = repo.check()

        assertEquals(3_000L, testScheduler.currentTime)
        assertNull(snapshot.server)
        assertEquals(listOf("rc"), snapshot.remote.notices.map { it.id })
        assertEquals("rc", AppPopupResolver.resolve(snapshot.remote, snapshot.server, env, now)?.id)
    }

    @Test
    fun `FR-APP-01 AC6 두 곳 모두 합쳐서 같은 id는 Remote Config 값을 쓴다`() = runTest {
        val gateway = FakeGateway(RemoteConfigValues("""{"items":[{"id":"x","closable":true,"title":"RC"}]}""", null))
        val repo = repository(FakeApi(noticesBody = """{"items":[{"id":"x","closable":true,"title":"서버"},{"id":"y","closable":true,"title":"서버2"}]}"""), gateway)

        val snapshot = repo.check()
        val popups = AppPopupResolver.resolveAll(snapshot.remote, snapshot.server, env, now)

        assertEquals(listOf("RC", "서버2"), popups.map { it.title })
    }

    @Test
    fun `FR-APP-01 AC6 서버가 실패하거나 오프라인이면 기다리지 않고 Remote Config 값만 쓴다`() = runTest {
        val gateway = FakeGateway(RemoteConfigValues(notices("rc"), null))

        val snapshot = repository(FakeApi(fail = true), gateway).check()

        assertNull(snapshot.server)
        assertEquals(0L, testScheduler.currentTime)
        assertEquals(listOf("rc"), snapshot.remote.notices.map { it.id })
    }

    @Test
    fun `FR-APP-01 AC6 Remote Config를 못 받으면 마지막으로 활성화한 값을 쓴다`() = runTest {
        val gateway = FakeGateway(RemoteConfigValues(notices("last"), null), fetchOk = false)

        val snapshot = repository(FakeApi(noticesBody = notices("server")), gateway).check()

        assertEquals(listOf("last"), snapshot.remote.notices.map { it.id })
        assertEquals(listOf("server"), snapshot.server?.notices?.map { it.id })
    }

    @Test
    fun `unit-9 결정 1 Remote Config가 3초를 넘기면 마지막 값으로 먼저 판단하고 늦게 도착하면 변경으로 알린다`() = runTest {
        val gateway = FakeGateway(
            values = RemoteConfigValues(notices("old"), null),
            fetchMillis = 20_000,
            valuesAfterFetch = RemoteConfigValues(notices("new"), null),
        )
        val repo = repository(FakeApi(), gateway)
        val changes = mutableListOf<List<String>>()
        backgroundScope.launch { repo.changes.collect { changes += it.remote.notices.map { n -> n.id } } }
        runCurrent()

        val snapshot = repo.check()

        assertEquals(3_000L, testScheduler.currentTime)
        assertEquals(listOf("old"), snapshot.remote.notices.map { it.id })
        assertTrue(changes.isEmpty())

        testScheduler.advanceTimeBy(20_000)
        runCurrent()

        assertEquals(listOf(listOf("new")), changes)
    }

    @Test
    fun `D-178 실시간 리스너가 값을 활성화하면 변경으로 알린다`() = runTest {
        val gateway = FakeGateway(RemoteConfigValues(notices("a"), null))
        val repo = repository(FakeApi(noticesBody = notices("srv")), gateway)
        repo.check()

        gateway.values = RemoteConfigValues(notices("b"), null)
        val received = mutableListOf<com.duriseo.android.domain.appops.AppOpsSnapshot>()
        backgroundScope.launch { repo.changes.collect { received += it } }
        runCurrent()
        gateway.updates.tryEmit(Unit)
        runCurrent()

        assertEquals(1, received.size)
        assertEquals(listOf("b"), received.single().remote.notices.map { it.id })
        // 마지막으로 받은 서버 값과 합쳐서 낸다
        assertEquals(listOf("srv"), received.single().server?.notices?.map { it.id })
    }

    @Test
    fun `FR-APP-01 AC6 두 곳 모두 없으면 팝업이 없다`() = runTest {
        val snapshot = repository(FakeApi(fail = true), FakeGateway(fetchOk = false)).check()

        assertNull(snapshot.server)
        assertTrue(snapshot.remote.notices.isEmpty() && snapshot.remote.rules.isEmpty())
        assertNull(AppPopupResolver.resolve(snapshot.remote, snapshot.server, env, now))
    }
}
