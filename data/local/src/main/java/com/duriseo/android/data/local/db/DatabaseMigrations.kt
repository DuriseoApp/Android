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
