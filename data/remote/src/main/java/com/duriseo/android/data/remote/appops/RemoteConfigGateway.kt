package com.duriseo.android.data.remote.appops

import android.content.Context
import com.duriseo.android.core.common.Logger
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Remote Config 값 두 개. 값이 없으면 null (한 번도 받지 못했거나 키가 없음) */
data class RemoteConfigValues(
    val notices: String?,
    val rules: String?,
)

/**
 * Firebase Remote Config를 감싼다 (D-176, D-177, API.md 4.9). 테스트는 이 인터페이스를 가짜로 바꾼다.
 * 키 `app_notices`·`app_update_rules`의 값은 `{"items":[…]}` JSON 문자열이다.
 */
interface RemoteConfigGateway {
    /**
     * 새 값을 가져와 활성화한다 (가져오기 간격 5분: 그 안이면 서버에 가지 않고 성공으로 끝난다).
     * 성공하면 true, 실패(네트워크, 제한)나 Firebase가 없으면 false. 던지지 않는다.
     */
    suspend fun fetchAndActivate(): Boolean

    /** 지금 활성화된 값(마지막으로 받아 둔 값). 앱 시작 직후에도 디스크에 남은 값을 읽는다 */
    fun currentValues(): RemoteConfigValues

    /** 실시간 리스너가 새 값을 활성화할 때마다 한 번씩 낸다 (`addOnConfigUpdateListener` → `activate`). 구독하는 동안만 듣는다 */
    fun updates(): Flow<Unit>
}

/** Firebase 설정(`google-services.json`)이 없는 빌드(CI): Remote Config 없이 서버 값만으로 동작한다 (unit-9 결정 5) */
object NoRemoteConfigGateway : RemoteConfigGateway {
    override suspend fun fetchAndActivate(): Boolean = false
    override fun currentValues(): RemoteConfigValues = RemoteConfigValues(notices = null, rules = null)
    override fun updates(): Flow<Unit> = emptyFlow()
}

internal const val KEY_APP_NOTICES = "app_notices"
internal const val KEY_APP_UPDATE_RULES = "app_update_rules"

/** 가져오기 간격: 5분 (D-178, 기본 12시간 캐시는 쓰지 않는다) */
internal const val MINIMUM_FETCH_INTERVAL_SECONDS = 300L

internal class FirebaseRemoteConfigGateway(
    private val config: FirebaseRemoteConfig,
) : RemoteConfigGateway {

    /** 가져오기 간격 설정. 설정이 끝나기 전에 가져오면 기본 12시간 간격으로 가져와서, 가져오기 전에 반드시 기다린다 */
    private val settingsApplied: Task<Void> = config.setConfigSettingsAsync(
        FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(MINIMUM_FETCH_INTERVAL_SECONDS)
            .build(),
    )

    override suspend fun fetchAndActivate(): Boolean = try {
        awaitTask(settingsApplied)
        suspendCancellableCoroutine<Boolean> { continuation ->
            config.fetchAndActivate().addOnCompleteListener { task ->
                if (continuation.isActive) continuation.resume(task.isSuccessful)
            }
        }
    } catch (e: kotlin.coroutines.cancellation.CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    override fun currentValues(): RemoteConfigValues = RemoteConfigValues(
        notices = config.getString(KEY_APP_NOTICES).takeIf { it.isNotBlank() },
        rules = config.getString(KEY_APP_UPDATE_RULES).takeIf { it.isNotBlank() },
    )

    override fun updates(): Flow<Unit> = callbackFlow {
        val registration = config.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                // 활성화한 값이 실제로 바뀐 때만 알린다(결과 true). 바뀐 게 없으면 팝업을 다시 고르지 않는다
                config.activate().addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result == true) trySend(Unit)
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                // 실시간 리스너 오류는 다음 확인 때 가져오기로 메운다. 값은 로그에 남기지 않는다
                Logger.w(TAG, "Remote Config 실시간 업데이트 오류: ${error.code}")
            }
        })
        awaitClose { registration.remove() }
    }

    private suspend fun awaitTask(task: Task<Void>) {
        suspendCancellableCoroutine<Unit> { continuation ->
            task.addOnCompleteListener { if (continuation.isActive) continuation.resume(Unit) }
        }
    }

    private companion object {
        const val TAG = "appops"
    }
}

/**
 * Firebase가 초기화돼 있으면 실제 Remote Config, 아니면 [NoRemoteConfigGateway]를 돌려준다.
 * `core:push`의 `FcmAvailability`와 같은 방식이다(`FirebaseApp.getApps`가 비어 있음 = `google-services.json` 없음).
 */
internal fun createRemoteConfigGateway(context: Context): RemoteConfigGateway {
    val available = try {
        FirebaseApp.getApps(context).isNotEmpty()
    } catch (e: Exception) {
        false
    }
    if (!available) {
        Logger.w("appops", "Firebase 설정(google-services.json)이 없어 Remote Config 없이 서버 값만 써요")
        return NoRemoteConfigGateway
    }
    return FirebaseRemoteConfigGateway(FirebaseRemoteConfig.getInstance())
}
