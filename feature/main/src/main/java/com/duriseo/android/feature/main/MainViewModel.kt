package com.duriseo.android.feature.main

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.theme.DefaultPersonColors
import com.duriseo.android.core.designsystem.theme.PersonColors
import com.duriseo.android.core.designsystem.theme.personColorsOf
import com.duriseo.android.domain.auth.AuthRepository
import com.duriseo.android.domain.auth.AuthState
import com.duriseo.android.domain.auth.SessionEndReason
import com.duriseo.android.domain.me.MeRepository
import com.duriseo.android.domain.me.SpaceChange
import com.duriseo.android.domain.me.SpaceType
import com.duriseo.android.domain.notification.NotificationDestination
import com.duriseo.android.domain.notification.NotificationTapHandler
import com.duriseo.android.domain.notification.PushTapTarget
import com.duriseo.android.feature.main.component.MainTab
import com.duriseo.android.domain.sync.SpaceSyncSignal
import com.duriseo.android.domain.sync.SyncSpaceSignals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 앱을 켰을 때 처음 보여 줄 곳 (FR-AUTH-01 AC2·AC3) */
enum class MainStart {
    /** 토큰 저장소를 아직 읽는 중. 빈 화면(문구 없음)을 보여 준다 */
    Loading,

    /** 저장된 토큰이 있다 → 시작 화면 없이 캘린더 (FR-AUTH-01 AC2) */
    Home,

    /** 토큰이 없거나 복호화할 수 없다 → SCR-START-01 (FR-AUTH-01 AC3) */
    Start,
}

sealed interface MainEffect {
    /**
     * 서버가 세션을 끊었다 (401 `SESSION_REPLACED`·`SESSION_INVALID`). 로컬 정리는 끝났다.
     * 시작 화면으로 가고 백스택을 비운다. [replaced]면 시작 화면이 안내 스낵바를 보여 준다 (D-51)
     */
    data class SessionEnded(val replaced: Boolean) : MainEffect

    /**
     * 연결이 해제된 것을 알았다 (상대가 해제했거나 내가 해제했다, API.md 4.5의 6단계, D-37, D-103).
     * 로그인 정보를 뺀 로컬 데이터는 이미 지웠다. 화면 이동 기록을 SCR-COUPLE-01 하나로 다시 만든다 (앞 화면이 없어 뒤로 버튼 없음, D-96)
     */
    data object SpaceUnlinked : MainEffect

    /**
     * 연결 해제를 알고 초기화한 뒤 내 정보를 읽었더니 그 사이 이미 다른 사람과 연결돼 있다 (D-125).
     * SCR-COUPLE-01 대신 캘린더(SCR-CAL-01)로 가고, 연결 완료 시트(SCR-COUPLE-04)는 [MainViewModel.linkedSheetNeeded]가 띄운다
     */
    data object SpaceRelinked : MainEffect

    /** 푸시를 눌렀는데 로그인 세션이 없다: SCR-START-01로 간다 (FR-NOTI-03 AC3, D-143). 읽음 처리는 하지 않는다 */
    data object PushToStart : MainEffect

    /** 푸시를 눌러 들어왔다: 홈을 뒤에 두고 대상 화면으로 간다 (FR-NOTI-03 AC3, D-143). 이미 읽음 처리를 시작했다 */
    data class PushOpen(val entry: PushEntry) : MainEffect
}

/**
 * 푸시 진입 계획 (SCREENS.md 공통 "푸시 진입", FR-NOTI-03).
 *
 * @property destination 홈 위에 쌓을 대상. 삭제된 대상([NotificationDestination.EventDeleted]·[NotificationDestination.MemoDeleted])과
 *   갈 곳 없음은 쌓지 않고 홈에 머문다
 * @property homeTab 뒤에 둘 홈: 메모 대상(상대 메모 추가·메모 고정, 삭제된 메모)은 메모 탭, 그 밖(일정·기념일·연결·해제)은 캘린더 탭
 *   (unit-7.md 확인 필요 3)
 * @property messageRes 홈에 머물며 보일 스낵바 문구. 삭제된 일정·메모일 때만 있다
 */
data class PushEntry(
    val destination: NotificationDestination,
    val homeTab: MainTab,
    @StringRes val messageRes: Int?,
)

/** 누른 알림 처리 결과로 푸시 진입 계획을 세운다 */
internal fun pushEntryOf(destination: NotificationDestination): PushEntry = PushEntry(
    destination = destination,
    homeTab = when (destination) {
        is NotificationDestination.MemoDetail, NotificationDestination.MemoDeleted -> MainTab.MEMO
        else -> MainTab.CALENDAR
    },
    messageRes = when (destination) {
        NotificationDestination.EventDeleted -> DesignR.string.notification_event_deleted
        NotificationDestination.MemoDeleted -> DesignR.string.notification_memo_deleted
        else -> null
    },
)

/**
 * 앱 전체 로그인 상태를 지킨다. 시작 분기(앱 시작 때 한 번)와 세션 끊김 알림을 맡는다.
 * 로그인·로그아웃·탈퇴 뒤 이동은 각 화면의 ViewModel이 알려 준다.
 *
 * 세션 끊김 알림은 구독자가 없을 때 나오면 사라지므로(`SessionManager.events`) 이 ViewModel이 만들어지자마자 구독한다.
 * 연결 해제 알림(`MeRepository.spaceChanges`)도 같다. 연결 완료 시트(SCR-COUPLE-04)를 띄울지는 [linkedSheetNeeded]다.
 *
 * 동기화 신호 `SPACE` `LINKED`·`UNLINKED`(API.md 6장, NFR-SYNC-01)도 여기서 듣는다: 먼저 `GET /me`를 읽고,
 * 해제는 `refreshMe`가 저장 전에 감지해 위의 해제 알림으로 D-125 경로를 타고, 연결은 캘린더로 가 연결 완료 시트를 띄운다.
 *
 * 푸시를 눌러 들어오면(`MainActivity`가 [onPushTapped]를 부른다, 앱이 꺼져 있었든 떠 있었든) 그 알림을 읽음 처리하고
 * 홈을 뒤에 둔 대상 화면으로 가는 효과를 낸다 (FR-NOTI-03, D-142·D-143).
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    meRepository: MeRepository,
    syncSpaceSignals: SyncSpaceSignals,
    private val notificationTapHandler: NotificationTapHandler,
) : ViewModel() {

    /** 토큰 저장소를 읽은 첫 결과로 정한다. 그 뒤로는 바뀌지 않는다 (이후 이동은 내비게이션이 한다) */
    val start: StateFlow<MainStart> = flow {
        val state = authRepository.authState.first { it != AuthState.Unknown }
        emit(if (state == AuthState.SignedIn) MainStart.Home else MainStart.Start)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainStart.Loading)

    /**
     * 연결 완료 시트가 필요한지 (Room의 공간이 커플이고 가져오기 대기가 있거나 확인한 공간 ID와 다르다, API.md 4.4의 8·10단계).
     * 실제로 그릴지는 화면이 정한다: 캘린더 위에서만 그린다 (SCREENS.md SCR-COUPLE-04 "진입").
     */
    val linkedSheetNeeded: StateFlow<Boolean> =
        meRepository.observeLinkedSheetNeeded().stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * 사람 색 (FR-CAL-04 AC1). Room의 내 정보에서 나·상대·함께 색 키를 읽어 `DuriseoTheme`에 넣는다.
     * 내 정보가 없거나 모르는 키면 기본 사람 색이다. 개인 공간은 나 색만 쓴다.
     */
    val personColors: StateFlow<PersonColors> = meRepository.observeMe()
        .map { me ->
            val space = me?.space ?: return@map DefaultPersonColors
            personColorsOf(space.myColor, space.partner?.color, space.togetherColor)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DefaultPersonColors)

    private val effectChannel = Channel<MainEffect>(Channel.BUFFERED)

    /** 캘린더로 보낸 마지막 공간 ID. 같은 연결로 캘린더를 두 번 열지 않게 한다. 해제하면 비운다 (같은 공간으로 다시 연결할 수 있다) */
    private val relinkedSpaceId = java.util.concurrent.atomic.AtomicReference<String?>(null)
    val effects = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            meRepository.spaceChanges.collect { change ->
                when (change) {
                    SpaceChange.UNLINKED -> {
                        relinkedSpaceId.set(null)
                        effectChannel.trySend(destinationAfterUnlink(meRepository))
                    }
                }
            }
        }
        viewModelScope.launch {
            syncSpaceSignals.spaceSignals.collect { signal -> onSpaceSignal(signal, meRepository) }
        }
        viewModelScope.launch {
            authRepository.sessionEvents.collect { reason ->
                effectChannel.trySend(MainEffect.SessionEnded(replaced = reason == SessionEndReason.REPLACED))
            }
        }
    }

    /**
     * `GET /me`를 읽은 뒤 4.5의 6단계 경로로 간다 (API.md 6장, D-125).
     * - `UNLINKED`: `refreshMe`가 해제를 감지하면(Room이 커플이었고 받은 공간이 다름) 해제 알림이 나가 [destinationAfterUnlink]로 간다.
     *   이미 초기화됐으면(Room이 개인 공간) 아무 일도 없다.
     * - `LINKED`: 이 신호로 개인 공간에서 커플 공간이 됐으면 캘린더로 가고 연결 완료 시트는 [linkedSheetNeeded]가 띄운다 (D-48).
     *   이미 커플 공간이던 기기(내가 연결한 쪽, 이미 화면 이동을 마침)는 이동하지 않는다.
     */
    private suspend fun onSpaceSignal(signal: SpaceSyncSignal, meRepository: MeRepository) {
        val wasCouple = meRepository.observeMe().first()?.space?.type == SpaceType.COUPLE
        if (signal == SpaceSyncSignal.UNLINKED) relinkedSpaceId.set(null)
        meRepository.refreshMe()
        if (signal == SpaceSyncSignal.LINKED && !wasCouple) {
            val space = meRepository.observeMe().first()?.space
            // 연결 코드 화면의 5초 확인과 같은 시점에 신호가 와도 같은 공간으로는 한 번만 캘린더를 연다
            if (space?.type == SpaceType.COUPLE && relinkedSpaceId.getAndSet(space.id) != space.id) {
                effectChannel.trySend(MainEffect.SpaceRelinked)
            }
        }
    }


    /**
     * 초기화(`MeRepository.resetAfterUnlink`·`refreshMe`)는 `GET /me`를 받아 Room에 저장한 뒤 알림을 낸다 (D-125).
     * 저장된 내 정보의 공간이 커플이면 이미 다른 사람과 연결된 것이라 캘린더로 가고, 개인 공간이면 SCR-COUPLE-01이다.
     * `GET /me`가 실패했거나 오프라인이면 Room이 비어 있어 기존처럼 SCR-COUPLE-01이다.
     */
    private suspend fun destinationAfterUnlink(meRepository: MeRepository): MainEffect {
        val me = meRepository.observeMe().first()
        return if (me?.space?.type == SpaceType.COUPLE) MainEffect.SpaceRelinked else MainEffect.SpaceUnlinked
    }

    /**
     * 푸시 알림을 눌렀다 (FR-NOTI-03). 로그인 세션이 없으면 SCR-START-01로 간다(D-143). 있으면 알림 목록에서 누른 것과 같은
     * [NotificationTapHandler]로 읽음 처리(기기 먼저, 덮개 없음)와 대상 확인을 하고, 홈을 뒤에 둔 이동을 효과로 낸다.
     * 처음 켤 때는 토큰 저장소를 다 읽은 뒤에 판단한다 (읽는 동안은 세션 여부를 모른다).
     */
    fun onPushTapped(tap: PushTapTarget) {
        viewModelScope.launch {
            val state = authRepository.authState.first { it != AuthState.Unknown }
            if (state != AuthState.SignedIn) {
                effectChannel.trySend(MainEffect.PushToStart)
                return@launch
            }
            val destination = notificationTapHandler.open(tap.notificationId, tap.kind, tap.target)
            effectChannel.trySend(MainEffect.PushOpen(pushEntryOf(destination)))
        }
    }
}
