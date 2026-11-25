package com.duriseo.android.domain.appops

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class AppNoticeFilterTest {

    @Test
    fun `FR-APP-01 AC1 기간과 플랫폼으로 거른다`() {
        val past = Instant.parse("2026-11-24T00:00:00Z")
        val future = Instant.parse("2026-11-26T00:00:00Z")
        val notices = listOf(
            notice("always"),
            notice("started", startsAt = past),
            notice("starts-exactly-now", startsAt = NOW),
            notice("not-yet", startsAt = future),
            notice("ends-later", endsAt = future),
            notice("ends-exactly-now", endsAt = NOW),
            notice("ended", endsAt = past),
            notice("android-only", platform = AppPlatform.ANDROID),
            notice("ios-only", platform = AppPlatform.IOS),
        )

        val ids = notices.filterActive(NOW, AppPlatform.ANDROID).map { it.id }

        // startsAt <= now (같으면 유효), now < endsAt (같으면 끝남)
        assertEquals(listOf("always", "started", "starts-exactly-now", "ends-later", "android-only"), ids)
    }
}
