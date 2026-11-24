package com.duriseo.android.domain.tutorial

import kotlinx.coroutines.flow.StateFlow

/**
 * 캘린더 튜토리얼(SCR-CAL-01, FR-CAL-06, D-168)을 언제 보여 줄지 정하는 기록과 요청.
 *
 * - "본 적 있는지"는 **기기 단위**로 저장한다. 로그아웃·탈퇴·연결 해제(앱 초기화)로 지우지 않고, 앱을 지우면 함께 사라져 다시 뜬다 (AC4).
 * - "다시 보기" 요청(전체 메뉴의 "튜토리얼 다시 보기", FR-SET-02 AC5, D-169)은 저장하지 않는 메모리 값이다.
 *   서버를 쓰지 않으므로 오프라인에서도 같다 (FR-CAL-06 AC5).
 */
interface CalendarTutorialRepository {
    /**
     * 처음 보여 줄 때면 true를 돌려주고 그 자리에서 "봤음"으로 적는다 (두 번째부터 false).
     * 동시에 불러도 true는 한 번이다. 처음 뜰 때 본 것으로 저장한다 (AC4).
     */
    suspend fun markShownIfFirst(): Boolean

    /** 전체 메뉴에서 "튜토리얼 다시 보기"를 눌러 1단계부터 다시 보여 달라는 요청이 남아 있는지 */
    val replayRequested: StateFlow<Boolean>

    /** "튜토리얼 다시 보기" 요청을 남긴다. 캘린더가 보이면 [consumeReplay]로 가져간다 */
    fun requestReplay()

    /** 남은 다시 보기 요청을 가져가 지운다 */
    fun consumeReplay()
}
