package com.duriseo.android.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.duriseo.android.data.local.anniversary.AnniversaryDao
import com.duriseo.android.data.local.anniversary.AnniversaryOccurrenceEntity
import com.duriseo.android.data.local.anniversary.AnniversaryPageStateEntity
import com.duriseo.android.data.local.anniversary.PastAnniversaryEntity
import com.duriseo.android.data.local.event.EventDao
import com.duriseo.android.data.local.event.EventEntity
import com.duriseo.android.data.local.me.MeDao
import com.duriseo.android.data.local.me.MeEntity
import com.duriseo.android.data.local.memo.MemoDao
import com.duriseo.android.data.local.memo.MemoEntity
import com.duriseo.android.data.local.memo.MemoPageStateEntity
import com.duriseo.android.data.local.notification.NotificationDao
import com.duriseo.android.data.local.notification.NotificationEntity
import com.duriseo.android.data.local.notification.NotificationStateEntity

/**
 * 앱의 Room 데이터베이스 (오프라인 보기 캐시, NFR-OFF-01). 첫 엔티티는 `MeEntity`(단위 2).
 * 출시 전이라 스키마가 바뀌면 버전을 올리고 destructive fallback을 쓴다. 출시 후에는 마이그레이션이 필요하다.
 * 스키마 JSON은 `data/local/schemas/`에 커밋한다.
 *
 * 버전 2 (단위 3): `me`에 `linked_at`, `import_events`, `import_anniversaries`, `import_memos`를 더했다
 * (API.md 3.1 `MeSpace.linkedAt`·`importCounts`). 출시 전이라 마이그레이션 없이 지우고 다시 만든다 (캐시, unit-3.md 결정 15).
 *
 * 버전 3 (단위 4): `event` 테이블을 더했다 (API.md 3.3 `Event`). 같은 방식으로 지우고 다시 만든다.
 *
 * 버전 4 (단위 5): 기념일 `anniversary_occurrence`(캘린더 범위·다가오는 목록, 응답 위치 저장)·`anniversary_past`(지난 기념일)·
 * `anniversary_page_state`(다음 커서·지난 기념일 개수)를 더했다 (API.md 3.3 "기념일"). 같은 방식으로 지우고 다시 만든다.
 *
 * 버전 5 (단위 6): 메모 `memo`(보기별 목록·고정 목록·날짜 메모·상세, 응답 위치 저장)·`memo_page_state`(보기별 다음 커서·전체 개수, 즐겨찾기 개수)를
 * 더했다 (API.md 3.4). 같은 방식으로 지우고 다시 만든다.
 *
 * 버전 6 (단위 7): 알림 `notification`(응답 위치 저장)·`notification_state`(다음 커서, 안 읽은 개수)를 더했다 (API.md 3.5). 같은 방식으로 지우고 다시 만든다.
 * 알림은 사용자 것이라 공간이 바뀌어도 지우지 않고 로그아웃·계정 전환의 `clearAllTables`가 지운다.
 */
@Database(
    entities = [
        MeEntity::class,
        EventEntity::class,
        AnniversaryOccurrenceEntity::class,
        PastAnniversaryEntity::class,
        AnniversaryPageStateEntity::class,
        MemoEntity::class,
        MemoPageStateEntity::class,
        NotificationEntity::class,
        NotificationStateEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class DuriseoDatabase : RoomDatabase() {
    abstract fun meDao(): MeDao

    abstract fun eventDao(): EventDao

    abstract fun anniversaryDao(): AnniversaryDao

    abstract fun memoDao(): MemoDao

    abstract fun notificationDao(): NotificationDao

    companion object {
        const val NAME = "duriseo.db"
    }
}
