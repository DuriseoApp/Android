package com.duriseo.android.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 버전 6 -> 7 (단위 8): `me`에 내 사진 버전·상대 사진 버전 열을 더한다 (API.md 3.1 `Me`·`MePartner`의 `profileImageVersion`).
 * 기존 행은 null(사진 없음)로 남고, 다음 `GET /me`가 실제 값으로 채운다. 열은 null 허용에 기본값 없음이라 엔티티와 같다.
 */
val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `me` ADD COLUMN `profile_image_version` TEXT")
        db.execSQL("ALTER TABLE `me` ADD COLUMN `partner_profile_image_version` TEXT")
    }
}

/**
 * 버전 7 -> 8 (메모와 캘린더 분리, D-174): `memo`에서 `memo_date` 열과 인덱스 `(list_type, memo_date)`를 지운다.
 * `ALTER TABLE ... DROP COLUMN`은 오래된 SQLite에 없어서 표를 새 모양으로 다시 만들어 옮긴다.
 * 캘린더용 날짜 목록 행(`list_type = 'DATED'`)은 더 이상 쓰지 않아 옮기지 않는다. 나머지 행은 `id`·위치 그대로 옮긴다.
 * 새 표의 열·인덱스 이름은 엔티티([com.duriseo.android.data.local.memo.MemoEntity])와 같아야 Room 검증을 통과한다.
 */
val MIGRATION_7_8: Migration = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `memo_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`list_type` TEXT NOT NULL, `position` INTEGER NOT NULL, `memo_id` TEXT NOT NULL, " +
                "`title` TEXT, `body` TEXT, `author_id` TEXT NOT NULL, `is_pinned` INTEGER NOT NULL, " +
                "`pinned_at` TEXT, `is_favorite` INTEGER NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL)",
        )
        db.execSQL(
            "INSERT INTO `memo_new` (`id`, `list_type`, `position`, `memo_id`, `title`, `body`, `author_id`, " +
                "`is_pinned`, `pinned_at`, `is_favorite`, `created_at`, `updated_at`) " +
                "SELECT `id`, `list_type`, `position`, `memo_id`, `title`, `body`, `author_id`, " +
                "`is_pinned`, `pinned_at`, `is_favorite`, `created_at`, `updated_at` FROM `memo` WHERE `list_type` <> 'DATED'",
        )
        db.execSQL("DROP TABLE `memo`")
        db.execSQL("ALTER TABLE `memo_new` RENAME TO `memo`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_memo_list_type_position` ON `memo` (`list_type`, `position`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_memo_memo_id` ON `memo` (`memo_id`)")
    }
}
