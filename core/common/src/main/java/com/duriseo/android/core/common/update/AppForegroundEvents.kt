package com.duriseo.android.core.common.update

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "앱으로 돌아옴" 신호 (D-178: 공지·업데이트 팝업을 다시 확인하는 시점). `MyApp`의 `ForegroundCallbacks`가 알려 준다.
 * 화면 회전처럼 Activity만 다시 만들어지는 경우는 돌아옴이 아니다: 백그라운드(화면이 하나도 없는 채 잠시 지남)를 거친 뒤의
 * 첫 포그라운드만 [returns]로 낸다.
 */
interface AppForegroundEvents {
    val returns: SharedFlow<Unit>
}

@Singleton
class AppForegroundTracker @Inject constructor() : AppForegroundEvents {
    private val mutableReturns = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val returns: SharedFlow<Unit> = mutableReturns.asSharedFlow()

    private val wentBackground = AtomicBoolean(false)

    /** 앱이 백그라운드로 갔다 (보이는 화면이 없는 채 잠시 지남) */
    fun onBackground() {
        wentBackground.set(true)
    }

    /** 화면이 다시 보이기 시작했다 */
    fun onForeground() {
        if (wentBackground.compareAndSet(true, false)) mutableReturns.tryEmit(Unit)
    }
}
