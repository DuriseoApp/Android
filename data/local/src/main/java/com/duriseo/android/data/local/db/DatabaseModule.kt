package com.duriseo.android.data.local.db

import android.content.Context
import androidx.room.Room
import com.duriseo.android.data.local.anniversary.AnniversaryDao
import com.duriseo.android.data.local.event.EventDao
import com.duriseo.android.data.local.me.MeDao
import com.duriseo.android.data.local.memo.MemoDao
import com.duriseo.android.data.local.notification.NotificationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DuriseoDatabase =
        Room.databaseBuilder(context, DuriseoDatabase::class.java, DuriseoDatabase.NAME)
            // 6 -> 7은 열만 더하고 7 -> 8은 `memo`를 다시 만든다. 그 밖에 이어지는 길이 없는 버전은 지우고 다시 만든다 (출시 전, 캐시라서 서버에서 다시 받는다)
            .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideMeDao(database: DuriseoDatabase): MeDao = database.meDao()

    @Provides
    fun provideEventDao(database: DuriseoDatabase): EventDao = database.eventDao()

    @Provides
    fun provideAnniversaryDao(database: DuriseoDatabase): AnniversaryDao = database.anniversaryDao()

    @Provides
    fun provideMemoDao(database: DuriseoDatabase): MemoDao = database.memoDao()

    @Provides
    fun provideNotificationDao(database: DuriseoDatabase): NotificationDao = database.notificationDao()
}
