package com.duriseo.android.core.designsystem.message

import com.duriseo.android.domain.error.DataError
import org.junit.Assert.assertNull
import org.junit.Test

class DataErrorMessageTest {

    @Test
    fun `FR-APP-02 AC6 APP_VERSION_UNSUPPORTED는 스낵바 문구가 없다`() {
        val error = DataError.ApiError(code = "APP_VERSION_UNSUPPORTED", details = null, traceId = null)

        assertNull(error.toMessageResOrNull())
    }
}
