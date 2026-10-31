package com.duriseo.android.data.local.me

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MeDao {
    @Query("SELECT * FROM me LIMIT 1")
    abstract fun observe(): Flow<MeEntity?>

    @Query("SELECT * FROM me LIMIT 1")
    abstract suspend fun get(): MeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: MeEntity)

    @Query("DELETE FROM me")
    abstract suspend fun clear()

    /** 한 행만 두므로 다른 사용자의 행이 남지 않게 지우고 넣는다 (한 트랜잭션) */
    @Transaction
    open suspend fun replace(entity: MeEntity) {
        clear()
        insert(entity)
    }
}
