package com.duriseo.android.domain.notification

/**
 * 기기 알림창에 이미 그려진 푸시 알림을 모두 지운다 (D-136, NFR-SEC-02). 로그아웃·탈퇴·세션 끊김·계정 전환 때 부른다.
 * 앱 안의 알림 목록(Room)과는 관계없다. 구현은 Android를 아는 `core:push`에 있다.
 */
interface DrawnNotificationCleaner {
    fun cancelAll()
}
