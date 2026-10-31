package com.duriseo.android.data.local.me

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.duriseo.android.data.local.db.DuriseoDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 내 정보 캐시(첫 Room 엔티티). 메모리 DB는 Robolectric이 필요하다 (unit-2.md 확인 필요 15) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MeDaoTest {
    private lateinit var database: DuriseoDatabase
    private lateinit var dao: MeDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DuriseoDatabase::class.java,
        ).allowMainThreadQueries().build() // Robolectric 테스트 스레드는 메인 스레드라서
        dao = database.meDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun entity(id: String, nickname: String = "게스트") = MeEntity(
        id = id,
        accountType = "GUEST",
        nickname = nickname,
        timeZone = "Asia/Seoul",
        providers = "",
        hasProfileImage = false,
        createdAt = 1_760_000_000_000,
        spaceId = "space-$id",
        spaceType = "PERSONAL",
        myColor = "pick-orange",
        partnerId = null,
        partnerNickname = null,
        partnerColor = null,
        partnerHasProfileImage = null,
        togetherColor = null,
        startDate = null,
        importPending = false,
    )

    @Test
    fun `내 정보가 없으면 observe는 null을 낸다`() = runBlocking {
        assertNull(dao.observe().first())
        assertNull(dao.get())
    }

    @Test
    fun `replace는 한 행만 남기고 observe가 새 값을 낸다`() = runBlocking {
        dao.replace(entity("user-1", nickname = "처음"))
        dao.replace(entity("user-2", nickname = "나중"))

        assertEquals("user-2", dao.observe().first()?.id)
        assertEquals("나중", dao.get()?.nickname)
    }

    @Test
    fun `테이블을 모두 지우면 내 정보가 사라진다`() = runBlocking {
        dao.replace(entity("user-1"))

        database.clearAllTables()

        assertNull(dao.get())
    }
}
