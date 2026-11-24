package com.duriseo.android.data.repository.tutorial

import com.duriseo.android.data.local.tutorial.TutorialPreferencesLocalDataSource
import com.duriseo.android.domain.tutorial.CalendarTutorialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 캘린더 튜토리얼 기록(기기 단위, 설정 DataStore)과 "다시 보기" 요청(메모리). 서버를 쓰지 않는다 (FR-CAL-06 AC5).
 */
@Singleton
class CalendarTutorialRepositoryImpl @Inject constructor(
    private val preferences: TutorialPreferencesLocalDataSource,
) : CalendarTutorialRepository {
    private val replay = MutableStateFlow(false)

    override suspend fun markShownIfFirst(): Boolean = preferences.markShownIfFirst()

    override val replayRequested: StateFlow<Boolean> = replay.asStateFlow()

    override fun requestReplay() {
        replay.value = true
    }

    override fun consumeReplay() {
        replay.value = false
    }
}
