package com.duriseo.android.feature.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavDestination.Companion.hasRoute
import com.duriseo.android.core.common.navigation.AuthRouteModel
import com.duriseo.android.core.common.navigation.MainRouteModel
import com.duriseo.android.feature.anniversary.navigation.navigateToAnniversaries
import com.duriseo.android.feature.anniversary.navigation.navigateToAnniversaryCreate
import com.duriseo.android.feature.anniversary.navigation.navigateToAnniversaryEdit
import com.duriseo.android.feature.calendar.navigation.navigateToCalendar
import com.duriseo.android.feature.calendar.navigation.navigateToEventCreate
import com.duriseo.android.feature.calendar.navigation.navigateToEventDetail
import com.duriseo.android.feature.calendar.navigation.navigateToEventEdit
import com.duriseo.android.feature.couple.navigation.navigateToConnect
import com.duriseo.android.feature.couple.navigation.navigateToInviteCreate
import com.duriseo.android.feature.couple.navigation.navigateToInviteEnter
import com.duriseo.android.feature.main.MainStart
import com.duriseo.android.feature.main.PushEntry
import com.duriseo.android.feature.main.component.MainTab
import com.duriseo.android.domain.notification.NotificationDestination
import com.duriseo.android.feature.memo.navigation.closeMemoEditAndDetail
import com.duriseo.android.feature.notification.navigation.navigateToNotifications
import com.duriseo.android.feature.memo.navigation.navigateToMemoDetail
import com.duriseo.android.feature.memo.navigation.navigateToMemoEdit
import com.duriseo.android.feature.memo.navigation.navigateToPinnedMemos
import com.duriseo.android.feature.settings.navigation.navigateToAccount
import com.duriseo.android.feature.settings.navigation.navigateToColors
import com.duriseo.android.feature.settings.navigation.navigateToConnectionInfo
import com.duriseo.android.feature.settings.navigation.navigateToNotificationSettings
import com.duriseo.android.feature.settings.navigation.navigateToSettings

class MainNavigator(
    val navController: NavHostController,
) {
    val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination

    val currentTab: MainTab?
        @Composable get() = MainTab.fromDestination(currentDestination)

    /**
     * 탭 전환. 첫 탭(캘린더)까지 되돌려 상태를 저장하고, 다시 누르면 복원한다.
     * 그래프의 시작 화면은 로그인 상태에 따라 시작 화면일 수 있어서, 되돌릴 기준을 캘린더 경로로 직접 정한다.
     */
    fun navigate(tab: MainTab) {
        val navOptions = navOptions {
            popUpTo<MainRouteModel.Calendar> {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }

        when (tab) {
            MainTab.CALENDAR -> navController.navigateToCalendar(navOptions)
            MainTab.MEMO -> navController.navigate(MainRouteModel.Memo, navOptions)
        }
    }

    /** 앱을 켰을 때 갈 곳 (FR-AUTH-01 AC2·AC3): 토큰이 있으면 캘린더, 없으면 시작 화면 */
    fun startDestination(start: MainStart): Any? = when (start) {
        MainStart.Loading -> null
        MainStart.Home -> MainTab.START.route
        MainStart.Start -> AuthRouteModel.Start()
    }

    /** 시작 화면으로 간다. 뒤로 가기로 돌아올 화면이 없도록 백스택을 모두 비운다 (로그아웃·탈퇴·세션 끊김) */
    fun navigateToStart(sessionReplaced: Boolean = false) {
        navController.navigate(AuthRouteModel.Start(sessionReplaced)) { clearBackStack() }
    }

    /** 캘린더로 간다. 백스택을 모두 비운다 (로그인 뒤, 그 계정으로 로그인 뒤) */
    fun navigateToHome() {
        navController.navigate(MainTab.START.route) { clearBackStack() }
    }

    /**
     * 이미 캘린더 하나만 남아 있으면 다시 열지 않는다. 연결 코드 화면의 5초 확인이 먼저 캘린더로 보냈는데
     * 연결 신호가 뒤따라 와도 캘린더가 두 번 열려 깜박이지 않게 한다 (D-48).
     */
    fun navigateToHomeIfNeeded() {
        val atHomeOnly = navController.currentDestination?.hasRoute<MainRouteModel.Calendar>() == true &&
            navController.previousBackStackEntry == null
        if (!atHomeOnly) navigateToHome()
    }

    /** 커플 연결(SCR-COUPLE-01)로 간다. 백스택을 비운다 (신규 회원 로그인 직후, 연결 해제 뒤 앱 초기화) */
    fun navigateToCoupleConnect() {
        navController.navigateToConnect(navOptions { clearBackStack() })
    }

    /**
     * 커플 연결(SCR-COUPLE-01)로 간다. 백스택을 그대로 둬서 뒤로 가기가 전체 메뉴로 돌아온다
     * (전체 메뉴의 [상대와 연결하기], 게스트의 연결 정보 행. D-96)
     */
    fun navigateToConnect() = navController.navigateToConnect()

    fun navigateToInviteCreate() = navController.navigateToInviteCreate()

    fun navigateToInviteEnter() = navController.navigateToInviteEnter()

    /** 지금 화면 아래에 돌아갈 화면이 있는지 (없으면 뒤로 가기 버튼을 그리지 않는 화면이 쓴다) */
    fun hasPreviousScreen(): Boolean = navController.previousBackStackEntry != null

    /** 일정 상세 (SCR-CAL-02) */
    fun navigateToEventDetail(eventId: String) = navController.navigateToEventDetail(eventId)

    /** 새 작성 화면 (SCR-CAL-03, [일정|메모] 탭). [date]는 캘린더에서 고른 날 (일정 탭의 날짜만, 메모 날짜는 비움 D-133) */
    fun navigateToEventCreate(date: java.time.LocalDate) = navController.navigateToEventCreate(date = date)

    /** 일정 수정 (SCR-CAL-03) */
    fun navigateToEventEdit(eventId: String) = navController.navigateToEventEdit(eventId)

    /** 기념일 목록 (SCR-ANNIV-01). 캘린더 앱바·날짜 목록의 기념일 카드가 부른다 (단위 5 묶음 B) */
    fun navigateToAnniversaries() = navController.navigateToAnniversaries()

    /** 새 직접 기념일 (SCR-ANNIV-02) */
    fun navigateToAnniversaryCreate() = navController.navigateToAnniversaryCreate()

    /** 내 직접 기념일 수정 (SCR-ANNIV-02). [date]는 누른 항목의 날짜 */
    fun navigateToAnniversaryEdit(anniversaryId: String, date: java.time.LocalDate) =
        navController.navigateToAnniversaryEdit(anniversaryId, date)

    /** 고정 메모 전체 (SCR-MEMO-02). SCR-MEMO-01의 [고정 메모 전체 보기] */
    fun navigateToPinnedMemos() = navController.navigateToPinnedMemos()

    /** 메모 상세 (SCR-MEMO-03). 메모 목록·핀 카드·고정 메모 전체가 부른다 (캘린더 날짜 목록의 메모도) */
    fun navigateToMemoDetail(memoId: String) = navController.navigateToMemoDetail(memoId)

    /**
     * 새 메모 작성. 메모 탭의 FAB. SCR-MEMO-04는 SCR-CAL-03의 [메모] 탭이라(D-133, SCREENS SCR-MEMO-04 진입) 같은 작성 화면을
     * [메모] 탭이 골라진 채로 연다. 일정 탭의 날짜는 오늘이고 메모 날짜는 비어 있다.
     */
    fun navigateToMemoCreate() = navController.navigateToEventCreate(startOnMemo = true)

    /** 메모 수정 (SCR-MEMO-04). 상세의 [수정] */
    fun navigateToMemoEdit(memoId: String) = navController.navigateToMemoEdit(memoId)

    /** 수정 화면과 상세를 함께 닫는다 (수정 중 서버가 그 메모가 없다고 답함, D-121) */
    fun closeMemoEditAndDetail() = navController.closeMemoEditAndDetail()

    /** 전체 메뉴(SCR-SET-01). 진입은 캘린더 앱바 */
    fun navigateToSettings() = navController.navigateToSettings()

    /** [fromCouple]이면 연동을 마친 뒤 SCR-COUPLE-01로 돌아온다 (게스트의 [로그인하고 연결하기], FR-AUTH-03 AC4) */
    fun navigateToAccount(fromCouple: Boolean = false) = navController.navigateToAccount(fromCouple)

    fun navigateToColors() = navController.navigateToColors()

    fun navigateToConnectionInfo() = navController.navigateToConnectionInfo()

    /** 알림 설정 (SCR-SET-02). 전체 메뉴의 알림 설정 행 */
    fun navigateToNotificationSettings() = navController.navigateToNotificationSettings()

    /** 알림 목록 (SCR-NOTI-01). 캘린더 앱바의 알림 버튼 */
    fun navigateToNotifications() = navController.navigateToNotifications()

    /**
     * 알림 목록에서 항목을 눌러 읽음 처리한 뒤 대상 화면으로 간다 (SCR-NOTI-01 이동 표). 목록 위에 쌓는다.
     * 푸시 진입([openFromPush])과 같은 이동 함수를 쓰고 차이는 홈을 뒤에 두는지 하나뿐이다 (SCREENS.md 공통 "푸시 진입").
     * 삭제된 대상은 목록 ViewModel이 스낵바로 안내하고 목록에 머물러서 여기까지 오지 않는다.
     */
    fun openFromNotificationList(destination: NotificationDestination) =
        openNotificationDestination(destination)

    /**
     * 푸시를 눌러 들어왔다: 홈(캘린더 또는 메모 탭)을 뒤에 두고 대상 화면으로 간다 (FR-NOTI-03 AC3). 백스택을 비우고 홈부터 다시 쌓는다.
     * 삭제된 대상이나 갈 곳 없음이면 홈에만 머문다(스낵바는 호출한 쪽이 띄운다).
     */
    fun openFromPush(entry: PushEntry) {
        navigateToHome()
        if (entry.homeTab == MainTab.MEMO) navigate(MainTab.MEMO)
        openNotificationDestination(entry.destination)
    }

    /** 알림 대상 화면을 지금 화면 위에 쌓는다. 삭제된 대상·갈 곳 없음은 쌓지 않는다 */
    private fun openNotificationDestination(destination: NotificationDestination) {
        when (destination) {
            is NotificationDestination.EventDetail -> navigateToEventDetail(destination.eventId)
            is NotificationDestination.MemoDetail -> navigateToMemoDetail(destination.memoId)
            NotificationDestination.Anniversaries -> navigateToAnniversaries()
            NotificationDestination.Settings -> navigateToSettings()
            NotificationDestination.EventDeleted,
            NotificationDestination.MemoDeleted,
            NotificationDestination.None,
            -> Unit
        }
    }

    fun navigateUp() {
        navController.popBackStack()
    }

    private fun NavOptionsBuilder.clearBackStack() {
        popUpTo(navController.graph.id) { inclusive = true }
        launchSingleTop = true
    }

    /**
     * 스낵바 위치 (D-126). 하단 탭이 있는 화면(SCR-CAL-01·메모 탭 루트)은 하단 탭 위(바닥에서 96)이고 false다.
     * 탭이 없는 화면(SCR-CAL-02·03, SCR-MEMO-02·03·04, SCR-ANNIV-01·02, SCR-COUPLE-01~03, SCR-SET-01~)은 위에서 40이다.
     * 연결 완료 시트([linkedSheetVisible])가 떠 있는 동안은 탭 화면 위여도 위에서 40이다.
     */
    @Composable
    fun snackbarAtTop(linkedSheetVisible: Boolean = false): Boolean {
        val destination = currentDestination ?: return false
        return isSnackbarAtTop(linkedSheetVisible) { destination.hasRoute(it) }
    }

    /** 하단 탭은 탭 화면(캘린더·메모)에서만 보인다 */
    @Composable
    fun showBottomBar(): Boolean = currentTab != null
}

@Composable
fun rememberMainNavigator(
    navController: NavHostController = rememberNavController(),
): MainNavigator = remember(navController) {
    MainNavigator(navController)
}

/** 하단 탭이 있는 화면(탭 루트). 스낵바가 하단 탭 위에 뜬다 (D-126) */
private val TabRoutes = listOf(
    MainRouteModel.Calendar::class,
    MainRouteModel.Memo::class,
)

/**
 * 스낵바를 위에서 40에 띄우는가 (D-126): 탭 화면이 아니면 항상, 탭 화면이어도 연결 완료 시트가 떠 있으면.
 * [hasRoute]가 지금 화면이 그 경로인지 알려 준다.
 */
internal fun isSnackbarAtTop(linkedSheetVisible: Boolean, hasRoute: (kotlin.reflect.KClass<*>) -> Boolean): Boolean =
    linkedSheetVisible || TabRoutes.none(hasRoute)
