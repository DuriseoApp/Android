package com.duriseo.android.domain.notification

/** 알림을 눌렀을 때 갈 곳 (SCREENS.md SCR-NOTI-01 이동 표, D-85). 목록과 푸시 진입이 같이 쓴다 */
sealed interface NotificationDestination {
    /** 일정 상세 (SCR-CAL-02): 일정 알림, 상대 일정 추가 */
    data class EventDetail(val eventId: String) : NotificationDestination

    /** 메모 상세 (SCR-MEMO-03): 상대 메모 추가, 메모 고정 */
    data class MemoDetail(val memoId: String) : NotificationDestination

    /** 기념일 목록 (SCR-ANNIV-01) */
    data object Anniversaries : NotificationDestination

    /** 설정 (SCR-SET-01): 연결, 해제, 상대 탈퇴 */
    data object Settings : NotificationDestination

    /** 대상 일정이 이미 삭제됐다. 읽음 처리만 하고 "삭제된 일정이에요"를 보인다 (FR-NOTI-05 AC5, FR-NOTI-03 AC3) */
    data object EventDeleted : NotificationDestination

    /** 대상 메모가 이미 삭제됐다. "삭제된 메모예요" */
    data object MemoDeleted : NotificationDestination

    /** 갈 곳이 없다 (대상 정보가 없거나 모르는 종류) */
    data object None : NotificationDestination
}

/**
 * 알림 하나를 눌렀을 때의 공통 처리 (FR-NOTI-03 AC2, FR-NOTI-05 AC5·AC6). 알림 목록과 푸시 진입이 같은 함수를 쓴다.
 */
interface NotificationTapHandler {
    /**
     * ① 그 알림을 기기에서 먼저 읽음으로 한다(`NotificationRepository.markRead`, D-127). ② 갈 곳을 돌려준다.
     * 일정·메모는 기기(Room)에 있으면 그대로 가고, 없으면 온라인일 때 단건을 읽어 보고 404면 삭제된 대상이다. 오프라인이거나
     * 그 밖의 오류면 가 본다(대상 화면이 자기 오류를 처리한다). 읽음 요청의 실패는 알리지 않는다.
     */
    suspend fun open(notificationId: String, kind: NotificationKind, target: NotificationTarget?): NotificationDestination
}

/** 대상 정보로 갈 곳을 정한다. 대상이 없으면 종류로 정한다 (연결·해제는 설정, 기념일은 목록) */
fun destinationOf(kind: NotificationKind, target: NotificationTarget?): NotificationDestination {
    if (target != null) {
        return when (target.type) {
            NotificationTargetType.EVENT -> target.id?.let { NotificationDestination.EventDetail(it) } ?: NotificationDestination.None
            NotificationTargetType.MEMO -> target.id?.let { NotificationDestination.MemoDetail(it) } ?: NotificationDestination.None
            NotificationTargetType.ANNIVERSARY -> NotificationDestination.Anniversaries
            NotificationTargetType.SETTINGS -> NotificationDestination.Settings
        }
    }
    return when (kind) {
        NotificationKind.ANNIVERSARY -> NotificationDestination.Anniversaries
        NotificationKind.COUPLE_LINKED, NotificationKind.COUPLE_UNLINKED -> NotificationDestination.Settings
        else -> NotificationDestination.None
    }
}
