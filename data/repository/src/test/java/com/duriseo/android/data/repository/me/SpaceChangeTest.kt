package com.duriseo.android.data.repository.me

import com.duriseo.android.data.repository.RepoHarness
import com.duriseo.android.data.repository.coupleMeJson
import com.duriseo.android.data.repository.errorJson
import com.duriseo.android.data.repository.fixtureCoupleMe
import com.duriseo.android.data.repository.fixtureMe
import com.duriseo.android.data.repository.jsonResponse
import com.duriseo.android.data.repository.meJson
import com.duriseo.android.data.repository.runTestWithTimeout
import com.duriseo.android.data.repository.testTokens
import com.duriseo.android.domain.auth.TokenState
import com.duriseo.android.domain.couple.ColorChange
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.me.ImportCounts
import com.duriseo.android.domain.me.SpaceChange
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

/**
 * 공간이 바뀐 것을 아는 규칙: 확인한 공간 ID 저장 시점 3가지, 해제 감지, 앱 초기화
 * (API.md 4.4의 8단계, 4.5의 6단계, PRD D-98·D-103).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SpaceChangeTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private var created: RepoHarness? = null

    private val personalId = "22222222-2222-2222-2222-222222222222"
    private val coupleId = "33333333-3333-3333-3333-333333333333"
    private val newPersonalId = "55555555-5555-5555-5555-555555555555"

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        created?.close()
        runCatching { server.shutdown() }
    }

    private fun harness(online: Boolean = true): RepoHarness =
        RepoHarness(server, tempFolder.root, online = online, initialTokens = testTokens()).also { created = it }

    private fun personalJson(spaceId: String): String =
        meJson(accountType = "MEMBER").replace(personalId, spaceId)

    private suspend fun RepoHarness.nextChange(): SpaceChange? =
        withTimeoutOrNull(300) { meRepository.spaceChanges.first() }

    @Test
    fun `FR-COUPLE-03 AC1 importCounts와 linkedAt이 Room을 거쳐 그대로 돌아온다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(
            jsonResponse(
                200,
                coupleMeJson(
                    importPending = true,
                    importCountsJson = """{"events":3,"anniversaries":0,"memos":2}""",
                ),
            ),
        )

        h.meRepository.refreshMe()

        val space = h.roomMe()!!.space
        assertEquals(ImportCounts(events = 3, anniversaries = 0, memos = 2), space.importCounts)
        assertEquals(Instant.parse("2026-10-12T03:00:00Z"), space.linkedAt)
    }

    @Test
    fun `D-93 importPending이 false면 importCounts는 없다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, coupleMeJson()))

        h.meRepository.refreshMe()

        assertNull(h.roomMe()!!.space.importCounts)
    }

    @Test
    fun `D-98 저장 시점 1 GET me가 개인 공간을 줄 때마다 그 공간 ID를 확인한 공간으로 저장한다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, personalJson(personalId)))

        h.meRepository.refreshMe()

        assertEquals(personalId, h.acknowledgedSpace.get())
    }

    @Test
    fun `D-98 연결되면 시트가 필요하고 시트를 닫으면 저장 시점 2로 필요 없어진다`() = runTestWithTimeout {
        val h = harness()
        server.enqueue(jsonResponse(200, personalJson(personalId)))
        h.meRepository.refreshMe()
        assertFalse(h.meRepository.observeLinkedSheetNeeded().first())

        server.enqueue(jsonResponse(200, coupleMeJson()))
        h.meRepository.refreshMe()

        // 커플 공간 ID는 아직 저장하지 않는다 (시트를 닫기 전에 앱을 끄면 다시 뜬다)
        assertEquals(personalId, h.acknowledgedSpace.get())
        assertTrue(h.meRepository.observeLinkedSheetNeeded().first())

        h.meRepository.acknowledgeSpace()

        assertEquals(coupleId, h.acknowledgedSpace.get())
        assertFalse(h.meRepository.observeLinkedSheetNeeded().first())
    }

    @Test
    fun `FR-COUPLE-03 AC6 혼자 쓴 기록이 없어도 연결 뒤 시트를 닫기 전에는 시트가 필요하다`() = runTestWithTimeout {
        val h = harness()
        h.acknowledgedSpace.set(personalId)
        h.meLocal.replace(fixtureCoupleMe(importPending = false).toEntity())

        assertTrue(h.meRepository.observeLinkedSheetNeeded().first())
    }

    @Test
    fun `FR-COUPLE-03 AC4 확인한 공간 ID가 없어도 importPending이 true면 시트가 필요하다`() = runTestWithTimeout {
        val h = harness()
        h.meLocal.replace(fixtureCoupleMe(importPending = true, importCounts = ImportCounts(1, 0, 0)).toEntity())

        assertTrue(h.meRepository.observeLinkedSheetNeeded().first())
    }

    @Test
    fun `API 4-4 처음 로그인한 기기에서 처음부터 커플 공간이면 변화로 보지 않는다`() = runTestWithTimeout {
        val h = harness()
        h.meLocal.replace(fixtureCoupleMe(importPending = false).toEntity())

        assertFalse(h.meRepository.observeLinkedSheetNeeded().first())
    }

    @Test
    fun `FR-COUPLE-05 AC3 확인한 커플 공간 ID와 다른 개인 공간을 받으면 해제로 보고 초기화한다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureCoupleMe())
        h.acknowledgedSpace.set(coupleId)
        server.enqueue(jsonResponse(200, personalJson(newPersonalId)))

        h.meRepository.refreshMe()

        assertEquals(SpaceChange.UNLINKED, h.nextChange())
        // 감지가 저장보다 먼저: 설정은 지워졌고 새 개인 공간이 저장된다
        assertNull(h.settingValue())
        assertEquals(newPersonalId, h.roomMe()!!.space.id)
        assertEquals(newPersonalId, h.acknowledgedSpace.get())
        assertTrue(h.tokens.state.value is TokenState.Present)
    }

    @Test
    fun `FR-COUPLE-05 AC3 시트를 닫기 전에 해제돼 같은 개인 공간이 되살아나도 Room의 공간 종류로 해제를 안다`() =
        runTestWithTimeout {
            val h = harness()
            // 연결 직후: 확인한 공간 ID는 아직 개인 공간 P, Room은 커플
            h.acknowledgedSpace.set(personalId)
            h.preload(fixtureCoupleMe())
            server.enqueue(jsonResponse(200, personalJson(personalId)))

            h.meRepository.refreshMe()

            assertEquals(SpaceChange.UNLINKED, h.nextChange())
            assertNull(h.settingValue())
            assertEquals(SpaceType.PERSONAL, h.roomMe()!!.space.type)
        }

    @Test
    fun `FR-COUPLE-05 AC3 개인 공간이 그대로면 해제로 보지 않는다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe(accountType = AccountType.MEMBER))
        h.acknowledgedSpace.set(personalId)
        server.enqueue(jsonResponse(200, personalJson(personalId)))

        h.meRepository.refreshMe()

        assertNull(h.nextChange())
        assertEquals("저장된 설정", h.settingValue())
    }

    @Test
    fun `FR-COUPLE-05 AC5 PATCH space가 NOT_COUPLE이면 해제로 보고 초기화한다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureCoupleMe())
        h.acknowledgedSpace.set(coupleId)
        server.enqueue(jsonResponse(409, errorJson("NOT_COUPLE")))
        server.enqueue(jsonResponse(200, personalJson(newPersonalId)))

        val result = h.coupleRepository.updateStartDate(LocalDate.of(2026, 3, 14))

        // 실패 결과는 그대로 돌려주고(화면은 아무것도 하지 않는다), 이동은 spaceChanges를 받는 쪽이 한다
        assertTrue(result is DataResult.Failure)
        assertEquals(SpaceChange.UNLINKED, h.nextChange())
        assertNull(h.settingValue())
        assertEquals(newPersonalId, h.roomMe()!!.space.id)
        assertTrue(h.tokens.state.value is TokenState.Present)
    }

    @Test
    fun `FR-COUPLE-05 AC5 PATCH space colors가 NOT_COUPLE이면 해제로 보고 초기화한다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureCoupleMe())
        server.enqueue(jsonResponse(409, errorJson("NOT_COUPLE")))
        server.enqueue(jsonResponse(200, personalJson(newPersonalId)))

        val result = h.coupleRepository.updateColors(
            ColorChange(together = "pick-rose"),
        )

        assertTrue(result is DataResult.Failure)
        assertEquals(SpaceChange.UNLINKED, h.nextChange())
    }

    @Test
    fun `FR-COUPLE-05 AC5 couple import가 NOT_COUPLE이면 해제로 보고 초기화한다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureCoupleMe(importPending = true))
        server.enqueue(jsonResponse(409, errorJson("NOT_COUPLE")))
        server.enqueue(jsonResponse(200, personalJson(newPersonalId)))

        h.coupleRepository.decideImport(true)

        assertEquals(SpaceChange.UNLINKED, h.nextChange())
    }

    @Test
    fun `FR-COUPLE-03 NO_IMPORT_PENDING은 초기화하지 않고 실패 그대로 돌려준다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureCoupleMe(importPending = true))
        server.enqueue(jsonResponse(409, errorJson("NO_IMPORT_PENDING")))

        val result = h.coupleRepository.decideImport(false)

        assertTrue(result is DataResult.Failure)
        assertNull(h.nextChange())
        assertEquals("저장된 설정", h.settingValue())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `FR-COUPLE-05 AC3 내가 해제하면 초기화 알림이 한 번 나가고 새 개인 공간이 확인한 공간으로 저장된다`() =
        runTestWithTimeout {
            val h = harness()
            h.preload(fixtureCoupleMe())
            h.acknowledgedSpace.set(coupleId)
            server.enqueue(MockResponse().setResponseCode(204))
            server.enqueue(jsonResponse(200, personalJson(newPersonalId)))

            h.coupleRepository.unlink()

            assertEquals(SpaceChange.UNLINKED, h.nextChange())
            // 알림은 한 번뿐이다 (GET me의 감지가 또 내지 않는다)
            assertNull(h.nextChange())
            assertEquals(newPersonalId, h.acknowledgedSpace.get())
        }

    @Test
    fun `API 4-4 12단계 ALREADY_COUPLED면 오류를 돌려주고 내 정보를 다시 읽는다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe(accountType = AccountType.MEMBER))
        server.enqueue(jsonResponse(409, errorJson("ALREADY_COUPLED")))
        server.enqueue(jsonResponse(200, coupleMeJson()))

        val result = h.coupleRepository.issueInviteCode()

        assertTrue(result is DataResult.Failure)
        assertEquals(2, server.requestCount)
        assertEquals(SpaceType.COUPLE, h.roomMe()!!.space.type)
    }

    @Test
    fun `API 4-4 12단계 GUEST_NOT_ALLOWED면 오류를 돌려주고 내 정보를 다시 읽는다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe(accountType = AccountType.MEMBER))
        server.enqueue(jsonResponse(403, errorJson("GUEST_NOT_ALLOWED")))
        server.enqueue(jsonResponse(200, meJson(accountType = "MEMBER")))

        val result = h.coupleRepository.join("K7Q29X")

        assertTrue(result is DataResult.Failure)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `API 4-4 12단계 코드 오류(INVITE_USED)는 내 정보를 다시 읽지 않는다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe(accountType = AccountType.MEMBER))
        server.enqueue(jsonResponse(409, errorJson("INVITE_USED")))

        h.coupleRepository.join("K7Q29X")

        assertEquals(1, server.requestCount)
    }
}
