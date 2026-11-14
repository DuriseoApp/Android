package com.duriseo.android.data.remote.me

import com.duriseo.android.data.remote.call.ApiCaller
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.notification.NotificationSettingKind
import com.duriseo.android.domain.notification.NotificationSettings
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.domain.result.map
import javax.inject.Inject
import javax.inject.Singleton

/** 내 정보 API 호출을 [DataResult]와 도메인 모델로 돌려준다. `PUT /me/devices` 포함 */
@Singleton
class MeRemoteDataSource @Inject constructor(
    private val api: MeApi,
    private val apiCaller: ApiCaller,
) {
    suspend fun getMe(): DataResult<Me> = apiCaller.call { api.getMe() }.map { it.toDomain() }

    /** `PATCH /me {timeZone}` (NFR-TIME-01). `nickname`은 보내지 않는다 */
    suspend fun patchTimeZone(timeZone: String): DataResult<Me> =
        apiCaller.call { api.patchMe(UpdateMeRequest(timeZone = timeZone)) }.map { it.toDomain() }

    suspend fun deleteMe(): DataResult<Unit> = apiCaller.callUnit { api.deleteMe() }

    /** `PUT /me/devices {fcmToken, platform}` (204, 멱등 upsert). 토큰은 로그에 남기지 않는다 (NFR-SEC-01) */
    suspend fun putDevice(fcmToken: String): DataResult<Unit> =
        apiCaller.callUnit { api.putDevice(RegisterDeviceRequest(fcmToken = fcmToken, platform = "ANDROID")) }

    /** `GET /me/notification-settings` (API.md 3.1) */
    suspend fun getNotificationSettings(): DataResult<NotificationSettings> =
        apiCaller.call { api.getNotificationSettings() }.map { it.toDomain() }

    /** `PATCH /me/notification-settings`: 바뀐 종류 하나만 보낸다 (D-145). 응답은 전체 설정 */
    suspend fun patchNotificationSetting(kind: NotificationSettingKind, enabled: Boolean): DataResult<NotificationSettings> {
        val request = when (kind) {
            NotificationSettingKind.EVENT_REMINDER -> UpdateNotificationSettingsRequest(eventReminder = enabled)
            NotificationSettingKind.PARTNER_CREATED -> UpdateNotificationSettingsRequest(partnerCreated = enabled)
            NotificationSettingKind.MEMO_PINNED -> UpdateNotificationSettingsRequest(memoPinned = enabled)
            NotificationSettingKind.ANNIVERSARY -> UpdateNotificationSettingsRequest(anniversary = enabled)
        }
        return apiCaller.call { api.patchNotificationSettings(request) }.map { it.toDomain() }
    }
}
