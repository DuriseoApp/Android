package com.duriseo.android.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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

/**
 * Room 마이그레이션. 스키마 JSON(`MigrationTestHelper`용)을 커밋하지 않아서(unit-2.md) 최신 버전 데이터베이스를 연 뒤
 * 표를 이전 버전 모양으로 다시 만들어 놓고 [MIGRATION_6_7]·[MIGRATION_7_8]을 돌린다. 끝난 표를 Room 엔티티로 읽을 수 있는지까지 본다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DatabaseMigrationTest {
    private lateinit var database: DuriseoDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DuriseoDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** 버전 6의 `me` (사진 버전 열이 없다) */
    private val createMeV6 = """
        CREATE TABLE `me` (
          `id` TEXT NOT NULL, `account_type` TEXT NOT NULL, `nickname` TEXT NOT NULL, `time_zone` TEXT NOT NULL,
          `providers` TEXT NOT NULL, `has_profile_image` INTEGER NOT NULL, `created_at` INTEGER NOT NULL,
          `space_id` TEXT NOT NULL, `space_type` TEXT NOT NULL, `my_color` TEXT NOT NULL,
          `partner_id` TEXT, `partner_nickname` TEXT, `partner_color` TEXT, `partner_has_profile_image` INTEGER,
          `together_color` TEXT, `start_date` TEXT, `import_pending` INTEGER NOT NULL,
          `linked_at` INTEGER, `import_events` INTEGER, `import_anniversaries` INTEGER, `import_memos` INTEGER,
          PRIMARY KEY(`id`))
    """.trimIndent()

    @Test
    fun `DatabaseMigrationTest 6에서 7로 사진 버전 열을 더한다`() = runBlocking {
        val db = database.openHelper.writableDatabase
        db.execSQL("DROP TABLE `me`")
        db.execSQL(createMeV6)
        db.execSQL(
            "INSERT INTO `me` VALUES ('u1','MEMBER','민수','Asia/Seoul','GOOGLE',1,1760000000000," +
                "'s1','COUPLE','pick-orange','u2','지우','pick-blue',1,'pick-purple','2025-05-20',0,NULL,NULL,NULL,NULL)",
        )

        MIGRATION_6_7.migrate(db)

        val columns = buildList {
            db.query("PRAGMA table_info(`me`)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) add(cursor.getString(nameIndex))
            }
        }
        assertEquals(true, "profile_image_version" in columns)
        assertEquals(true, "partner_profile_image_version" in columns)

        // 기존 행은 그대로이고 새 열은 null(사진 없음). Room 엔티티로도 읽힌다
        val migrated = database.meDao().get()
        assertEquals("u1", migrated?.id)
        assertEquals("민수", migrated?.nickname)
        assertEquals(true, migrated?.hasProfileImage)
        assertNull(migrated?.profileImageVersion)
        assertNull(migrated?.partnerProfileImageVersion)
    }

    /** 버전 7의 `memo` (`memo_date` 열과 그 인덱스가 있다) */
    private val createMemoV7 = """
        CREATE TABLE `memo` (
          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `list_type` TEXT NOT NULL, `position` INTEGER NOT NULL,
          `memo_id` TEXT NOT NULL, `title` TEXT, `body` TEXT, `memo_date` TEXT, `author_id` TEXT NOT NULL,
          `is_pinned` INTEGER NOT NULL, `pinned_at` TEXT, `is_favorite` INTEGER NOT NULL,
          `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL)
    """.trimIndent()

    @Test
    fun `D-174 7에서 8로 메모 날짜 열과 날짜 목록 행을 지우고 나머지는 그대로 둔다`() = runBlocking {
        val db = database.openHelper.writableDatabase
        db.execSQL("DROP TABLE `memo`")
        db.execSQL(createMemoV7)
        db.execSQL("CREATE INDEX `index_memo_list_type_position` ON `memo` (`list_type`, `position`)")
        db.execSQL("CREATE INDEX `index_memo_list_type_memo_date` ON `memo` (`list_type`, `memo_date`)")
        db.execSQL("CREATE INDEX `index_memo_memo_id` ON `memo` (`memo_id`)")
        db.execSQL(
            "INSERT INTO `memo` VALUES (1,'NEWEST',0,'m1','장보기','우유','2026-10-17','u1',0,NULL,1," +
                "'2026-10-08T00:12:00Z','2026-10-08T00:12:00Z')",
        )
        db.execSQL(
            "INSERT INTO `memo` VALUES (2,'DATED',0,'m1','장보기','우유','2026-10-17','u1',0,NULL,1," +
                "'2026-10-08T00:12:00Z','2026-10-08T00:12:00Z')",
        )

        MIGRATION_7_8.migrate(db)

        val columns = buildList {
            db.query("PRAGMA table_info(`memo`)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) add(cursor.getString(nameIndex))
            }
        }
        assertEquals(false, "memo_date" in columns)
        assertEquals(true, "title" in columns)

        // 날짜 목록 행은 버리고 나머지는 그대로이며 Room 엔티티로 읽힌다
        val rows = database.memoDao().observeList("NEWEST").first()
        assertEquals(listOf("m1"), rows.map { it.memoId })
        assertEquals("장보기", rows.single().title)
        assertEquals(0, database.memoDao().observeList("DATED").first().size)
    }
}
