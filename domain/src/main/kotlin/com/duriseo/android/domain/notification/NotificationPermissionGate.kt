package com.duriseo.android.domain.notification

/**
 * 알림 권한을 "이미 물었는지" 기록 하나 (FR-NOTI-01 AC3, D-144). 일정 알림 설정·연결 완료 시트를 닫은 직후·직접 기념일 저장 성공 직후가
 * 함께 쓴다. 한 번 물었으면 거부한 뒤에도 다시 묻지 않는다.
 * Android 13 미만인지, 이미 허용됐는지는 요청하는 화면 쪽이 가린다 (권한 요청 자체가 Android API라서).
 */
interface NotificationPermissionGate {
    /** 처음 물어야 하면 true를 돌려주고 그 자리에서 "물었음"으로 적는다 (두 번째부터 false). 동시에 불러도 한 번만 true */
    suspend fun shouldAskNow(): Boolean
}
