package com.duriseo.android.core.common.update

import android.app.Activity
import android.content.Context
import com.duriseo.android.core.common.coroutines.ApplicationScope
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [AppUpdateSource]의 Google Play 구현 (D-151). Play가 설치하지 않은 앱(디버그 빌드, 직접 설치)은 API가 실패해서
 * [AppUpdateAvailability.UNKNOWN]이 되고, 이것이 정상이다.
 */
@Singleton
class PlayAppUpdateSource @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) : AppUpdateSource {

    private val manager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(context) }

    private val mutablePhase = MutableStateFlow(AppUpdatePhase.IDLE)
    override val phase: StateFlow<AppUpdatePhase> = mutablePhase.asStateFlow()

    private val listenerRegistered = AtomicBoolean(false)
    private val listener = InstallStateUpdatedListener { state ->
        mutablePhase.value = phaseOf(state.installStatus())
    }

    override fun start() {
        // 앱 시작 때 이미 받아 둔 업데이트가 있으면 phase가 DOWNLOADED가 되어 다음 백그라운드 때 설치한다
        scope.launch { check() }
    }

    override suspend fun check(): AppUpdateAvailability = try {
        ensureListener()
        val info = manager.requestAppUpdateInfo()
        mutablePhase.value = phaseOf(info.installStatus())
        when (info.updateAvailability()) {
            UpdateAvailability.UPDATE_AVAILABLE,
            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
            -> if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) || mutablePhase.value != AppUpdatePhase.IDLE) {
                AppUpdateAvailability.AVAILABLE
            } else {
                AppUpdateAvailability.UNKNOWN
            }
            UpdateAvailability.UPDATE_NOT_AVAILABLE -> AppUpdateAvailability.UP_TO_DATE
            else -> AppUpdateAvailability.UNKNOWN
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Play 스토어가 없거나 직접 설치한 앱이면 여기로 온다. 종류만 남긴다
        Timber.d("앱 업데이트 확인 실패: %s", e.javaClass.simpleName)
        AppUpdateAvailability.UNKNOWN
    }

    override suspend fun startUpdate(activity: Activity): Boolean = try {
        ensureListener()
        val info = manager.requestAppUpdateInfo()
        val options = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
        manager.startUpdateFlow(info, activity, options).await() == Activity.RESULT_OK
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.d("앱 업데이트 시작 실패: %s", e.javaClass.simpleName)
        false
    }

    override fun completeIfDownloaded() {
        if (mutablePhase.value != AppUpdatePhase.DOWNLOADED) return
        try {
            manager.completeUpdate()
        } catch (e: Exception) {
            Timber.d("앱 업데이트 설치 실패: %s", e.javaClass.simpleName)
        }
    }

    private fun ensureListener() {
        if (listenerRegistered.compareAndSet(false, true)) manager.registerListener(listener)
    }

    private fun phaseOf(status: Int): AppUpdatePhase = when (status) {
        InstallStatus.PENDING, InstallStatus.DOWNLOADING, InstallStatus.INSTALLING -> AppUpdatePhase.DOWNLOADING
        InstallStatus.DOWNLOADED -> AppUpdatePhase.DOWNLOADED
        else -> AppUpdatePhase.IDLE
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
