package com.duriseo.android.data.local.me

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 내 정보 캐시 (API.md 3.1 `Me`). 한 행만 둔다. 화면은 이 테이블의 Flow만 구독한다 (ARCHITECTURE.md 2장).
 *
 * `providers`는 쉼표로 이은 문자열(`GOOGLE,KAKAO`, 게스트는 빈 문자열), `start_date`는 `YYYY-MM-DD`,
 * `created_at`은 epoch 밀리초다. 문자열을 도메인 값으로 바꾸는 일은 `data:repository`의 매퍼가 한다.
 */
@Entity(tableName = "me")
data class MeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "account_type") val accountType: String,
    @ColumnInfo(name = "nickname") val nickname: String,
    @ColumnInfo(name = "time_zone") val timeZone: String,
    @ColumnInfo(name = "providers") val providers: String,
    @ColumnInfo(name = "has_profile_image") val hasProfileImage: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "space_id") val spaceId: String,
    @ColumnInfo(name = "space_type") val spaceType: String,
    @ColumnInfo(name = "my_color") val myColor: String,
    @ColumnInfo(name = "partner_id") val partnerId: String?,
    @ColumnInfo(name = "partner_nickname") val partnerNickname: String?,
    @ColumnInfo(name = "partner_color") val partnerColor: String?,
    @ColumnInfo(name = "partner_has_profile_image") val partnerHasProfileImage: Boolean?,
    @ColumnInfo(name = "together_color") val togetherColor: String?,
    @ColumnInfo(name = "start_date") val startDate: String?,
    @ColumnInfo(name = "import_pending") val importPending: Boolean,
    /** 연결한 시각 epoch 밀리초 (D-94). 개인 공간이면 null. 스키마 버전 2에서 더함 */
    @ColumnInfo(name = "linked_at") val linkedAt: Long? = null,
    /** 가져오기 대기 공간의 기록 개수 (D-93). 세 칸은 함께 null이거나 함께 값이 있다. 스키마 버전 2에서 더함 */
    @ColumnInfo(name = "import_events") val importEvents: Int? = null,
    @ColumnInfo(name = "import_anniversaries") val importAnniversaries: Int? = null,
    @ColumnInfo(name = "import_memos") val importMemos: Int? = null,
)
