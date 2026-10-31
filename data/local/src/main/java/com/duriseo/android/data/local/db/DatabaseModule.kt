package com.duriseo.android.data.local.db

import android.content.Context
import androidx.room.Room
import com.duriseo.android.data.local.me.MeDao
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
            // 출시 전: 스키마가 바뀌면 지우고 다시 만든다 (캐시라서 서버에서 다시 받는다)
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideMeDao(database: DuriseoDatabase): MeDao = database.meDao()
}
