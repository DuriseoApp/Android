package com.duriseo.android.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.duriseo.android.data.local.me.MeDao
import com.duriseo.android.data.local.me.MeEntity

/**
 * 앱의 Room 데이터베이스 (오프라인 보기 캐시, NFR-OFF-01). 첫 엔티티는 `MeEntity`(단위 2).
 * 출시 전이라 스키마가 바뀌면 버전을 올리고 destructive fallback을 쓴다. 출시 후에는 마이그레이션이 필요하다.
 * 스키마 JSON은 `data/local/schemas/`에 커밋한다.
 */
@Database(
    entities = [MeEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DuriseoDatabase : RoomDatabase() {
    abstract fun meDao(): MeDao

    companion object {
        const val NAME = "duriseo.db"
    }
}
