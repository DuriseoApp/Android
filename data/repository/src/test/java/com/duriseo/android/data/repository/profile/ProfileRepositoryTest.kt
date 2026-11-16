package com.duriseo.android.data.repository.profile

import com.duriseo.android.data.repository.RepoHarness
import com.duriseo.android.data.repository.errorJson
import com.duriseo.android.data.repository.fixtureMe
import com.duriseo.android.data.repository.jsonResponse
import com.duriseo.android.data.repository.meJson
import com.duriseo.android.data.repository.runTestWithTimeout
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.profile.PhotoError
import com.duriseo.android.domain.profile.photoErrorOrNull
import com.duriseo.android.domain.result.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/** 내 프로필 쓰기: 이름·사진 (FR-AUTH-07 AC6·AC7, NFR-FILE-01). 메모리 Room은 Robolectric이 필요하다 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProfileRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private var created: RepoHarness? = null

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
        RepoHarness(server, tempFolder.root, online = online).also { created = it }

    private fun meWithPhoto(version: String?): String {
        val photo = if (version == null) "null" else "\"$version\""
        return """{"id":"11111111-1111-1111-1111-111111111111","accountType":"MEMBER","nickname":"민수",
            "timeZone":"Asia/Seoul","providers":["GOOGLE"],"hasProfileImage":${version != null},"profileImageVersion":$photo,
            "createdAt":"2026-10-12T03:00:00Z",
            "space":{"id":"22222222-2222-2222-2222-222222222222","type":"PERSONAL","myColor":"pick-orange",
            "partner":null,"togetherColor":null,"startDate":null,"importPending":false}}"""
    }

    @Test
    fun `FR-AUTH-07 AC6 이름을 PATCH me로 저장하고 응답을 Room에 쓴다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe())
        server.enqueue(jsonResponse(200, meJson(accountType = "MEMBER").replace("\"게스트\"", "\"새 이름\"")))

        val result = h.profileRepository.updateNickname("  새 이름  ")

        assertEquals(DataResult.Success(Unit), result)
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/me", request.path)
        // 앞뒤 공백을 지운 값만 보내고 다른 필드는 보내지 않는다
        assertEquals("""{"nickname":"새 이름"}""", request.body.readUtf8())
        assertEquals("새 이름", h.roomMe()?.nickname)
    }

    @Test
    fun `FR-AUTH-07 AC6 이름 저장 실패는 오류 코드로 돌려준다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe())
        server.enqueue(jsonResponse(400, errorJson("VALIDATION_FAILED")))

        val result = h.profileRepository.updateNickname("이름")

        val error = (result as DataResult.Failure).error as DataError.ApiError
        assertEquals("VALIDATION_FAILED", error.code)
        // 실패하면 Room은 그대로다
        assertEquals("게스트", h.roomMe()?.nickname)
    }

    @Test
    fun `FR-AUTH-07 AC6 오프라인이면 이름도 사진도 요청하지 않는다`() = runTestWithTimeout {
        val h = harness(online = false)

        assertEquals(DataResult.Failure(DataError.Offline), h.profileRepository.updateNickname("이름"))
        assertEquals(DataResult.Failure(DataError.Offline), h.profileRepository.uploadPhoto(byteArrayOf(1), "image/jpeg"))
        assertEquals(DataResult.Failure(DataError.Offline), h.profileRepository.deletePhoto())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `FR-AUTH-07 AC6 사진을 multipart file로 올리고 GET me로 버전을 다시 읽는다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe())
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(jsonResponse(200, meWithPhoto("new-ver")))

        val result = h.profileRepository.uploadPhoto("jpegbytes".toByteArray(), "image/jpeg")

        assertEquals(DataResult.Success(Unit), result)
        val put = server.takeRequest()
        assertEquals("PUT", put.method)
        assertEquals("/api/v1/me/profile-image", put.path)
        assertTrue(put.getHeader("Content-Type")!!.startsWith("multipart/form-data"))
        val body = put.body.readUtf8()
        assertTrue(body.contains("name=\"file\""))
        assertTrue(body.contains("Content-Type: image/jpeg"))
        assertTrue(body.contains("jpegbytes"))
        assertEquals("GET", server.takeRequest().method)
        assertEquals("new-ver", h.roomMe()?.profileImageVersion)
    }

    @Test
    fun `FR-AUTH-07 AC6 PNG는 image png로 올린다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe())
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(jsonResponse(200, meWithPhoto("png-ver")))

        h.profileRepository.uploadPhoto("pngbytes".toByteArray(), "image/png")

        assertTrue(server.takeRequest().body.readUtf8().contains("Content-Type: image/png"))
    }

    @Test
    fun `FR-AUTH-07 AC6 올리기는 성공했는데 GET me가 두 번 실패해도 성공이다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe())
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(jsonResponse(500, errorJson("INTERNAL_ERROR")))
        server.enqueue(jsonResponse(500, errorJson("INTERNAL_ERROR")))

        val result = h.profileRepository.uploadPhoto(byteArrayOf(1, 2), "image/jpeg")

        assertEquals(DataResult.Success(Unit), result)
        assertNull(h.roomMe()?.profileImageVersion)
        // PUT 한 번 + GET 두 번 (한 번 더 해 보고 그만둔다)
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `FR-AUTH-07 AC6 GET me가 한 번 실패하면 한 번 더 읽어 새 버전을 저장한다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe())
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(jsonResponse(500, errorJson("INTERNAL_ERROR")))
        server.enqueue(jsonResponse(200, meWithPhoto("retry-ver")))

        val result = h.profileRepository.uploadPhoto(byteArrayOf(1, 2), "image/jpeg")

        assertEquals(DataResult.Success(Unit), result)
        assertEquals("retry-ver", h.roomMe()?.profileImageVersion)
    }

    @Test
    fun `FR-AUTH-07 AC6 이름 응답이 늦게 와서 캐시가 초기화된 뒤면 저장하지 않는다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe(id = "cached-user"))
        // PATCH 응답이 늦게 온다
        server.enqueue(jsonResponse(200, meJson(id = "stale-user", accountType = "MEMBER")).setHeadersDelay(1500, TimeUnit.MILLISECONDS))
        server.enqueue(jsonResponse(200, meJson(id = "fresh-user")))

        val stale = async(Dispatchers.Default) { h.profileRepository.updateNickname("새 이름") }
        assertEquals("PATCH", server.takeRequest(5, TimeUnit.SECONDS)?.method)
        h.meRepository.resetAfterUnlink()
        val result = stale.await()

        assertEquals(DataResult.Success(Unit), result)
        // 초기화 뒤 새로 받은 내 정보가 남고 늦은 이름 응답은 버려진다
        assertEquals("fresh-user", h.roomMe()?.id)
    }

    @Test
    fun `FR-AUTH-07 AC7 413은 공통 오류 415는 사진 형식 오류다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe())
        server.enqueue(jsonResponse(413, errorJson("IMAGE_TOO_LARGE")))
        server.enqueue(jsonResponse(415, errorJson("IMAGE_TYPE_NOT_ALLOWED")))
        server.enqueue(jsonResponse(500, errorJson("INTERNAL_ERROR")))

        val tooLarge = h.profileRepository.uploadPhoto(byteArrayOf(1), "image/jpeg") as DataResult.Failure
        val badType = h.profileRepository.uploadPhoto(byteArrayOf(1), "image/jpeg") as DataResult.Failure
        val other = h.profileRepository.uploadPhoto(byteArrayOf(1), "image/jpeg") as DataResult.Failure

        // 413은 앱이 고를 때 10MB를 막은 뒤라 25MP 초과 등이다: 사진 오류가 아니라 공통 오류(API.md 5장, D-153)
        assertEquals("IMAGE_TOO_LARGE", (tooLarge.error as DataError.ApiError).code)
        assertNull(tooLarge.error.photoErrorOrNull())
        assertEquals(PhotoError.TYPE_NOT_ALLOWED, badType.error.photoErrorOrNull())
        // 그 밖의 오류는 사진 오류가 아니라 공통 오류로 보인다
        assertNull(other.error.photoErrorOrNull())
        // 실패하면 GET /me를 부르지 않는다
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `FR-AUTH-07 AC5 사진을 지우고 GET me로 버전이 null이 된다`() = runTestWithTimeout {
        val h = harness()
        h.preload(fixtureMe().copy(profileImageVersion = "old-ver", hasProfileImage = true))
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(jsonResponse(200, meWithPhoto(null)))

        val result = h.profileRepository.deletePhoto()

        assertEquals(DataResult.Success(Unit), result)
        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/api/v1/me/profile-image", delete.path)
        assertEquals("GET", server.takeRequest().method)
        assertNull(h.roomMe()?.profileImageVersion)
    }
}
