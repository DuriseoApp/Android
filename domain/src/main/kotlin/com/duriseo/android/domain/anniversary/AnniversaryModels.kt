package com.duriseo.android.domain.anniversary

import java.time.Instant
import java.time.LocalDate

/** 발생일의 종류 (API.md 3.3 `AnniversaryOccurrence.kind`). 자동은 처음 만난 날로 서버가 계산한다 (FR-ANNIV-02) */
enum class AnniversaryKind {
    AUTO,
    CUSTOM,
    ;

    companion object {
        /** 서버 문자열 -> 값. 모르는 값이면 null (그 항목은 그리지 않는다) */
        fun fromCode(code: String): AnniversaryKind? = entries.firstOrNull { it.name == code }
    }
}

/**
 * 기념일 발생일 한 건 (API.md 3.3 `AnniversaryOccurrence`). 캘린더용 `GET /anniversaries`와 `upcoming`이 같은 모양이다.
 *
 * @property anniversaryId 직접 기념일의 `id`. 자동은 null
 * @property date 이 발생의 날짜 (2월 29일 규칙 적용은 서버가 한다)
 * @property daysLeft `date` − 요청자 시간대의 오늘. 오늘이면 0. 캘린더용에서는 지난 날짜가 음수다
 * @property repeatsYearly 직접 기념일의 값. 자동은 false
 * @property authorId 직접 기념일의 작성자. 자동은 null
 */
data class AnniversaryOccurrence(
    val anniversaryId: String?,
    val kind: AnniversaryKind,
    val name: String,
    val date: LocalDate,
    val daysLeft: Int,
    val repeatsYearly: Boolean,
    val authorId: String?,
) {
    val isAuto: Boolean get() = kind == AnniversaryKind.AUTO
}

/** "지난 기념일" 한 건 (`GET /anniversaries/past`의 항목). 지난 1회성 직접 기념일이라 `kind`·`daysLeft`가 없다 */
data class PastAnniversary(
    val anniversaryId: String,
    val name: String,
    val date: LocalDate,
    val authorId: String,
)

/** `GET /anniversaries/upcoming`을 이어 붙인 목록. [hasMore]가 true면 다음 페이지가 있다 (`nextCursor`가 null이 아님) */
data class UpcomingAnniversaries(
    val items: List<AnniversaryOccurrence>,
    val hasMore: Boolean,
)

/** `GET /anniversaries/past`를 이어 붙인 목록. [totalCount]는 페이지와 관계없는 전체 개수다 (0이면 "지난 기념일" 행을 숨긴다, D-71) */
data class PastAnniversaries(
    val items: List<PastAnniversary>,
    val totalCount: Int,
    val hasMore: Boolean,
)

/** 직접 기념일 (API.md 3.3 `Anniversary`, `POST`·`PATCH` 응답). [date]는 등록한 날짜다 */
data class Anniversary(
    val id: String,
    val name: String,
    val date: LocalDate,
    val repeatsYearly: Boolean,
    val authorId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** `POST /anniversaries` 입력. [repeatsYearly]가 null이면 보내지 않는다(서버 기본: 켜짐). [name]은 부른 쪽이 앞뒤 공백을 지운 값 */
data class NewAnniversary(
    val name: String,
    val date: LocalDate,
    val repeatsYearly: Boolean? = null,
)

/** `PATCH /anniversaries/{id}` 입력: 바뀐 필드만 담는다. 없는 필드는 서버가 바꾸지 않는다 (unit-5.md 확인 필요 1) */
data class AnniversaryPatch(
    val name: String? = null,
    val date: LocalDate? = null,
    val repeatsYearly: Boolean? = null,
) {
    val isEmpty: Boolean get() = name == null && date == null && repeatsYearly == null
}
