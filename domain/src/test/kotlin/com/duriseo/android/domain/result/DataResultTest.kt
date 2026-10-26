package com.duriseo.android.domain.result

import com.duriseo.android.domain.error.DataError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DataResultTest {

    @Test
    fun `성공 결과는 map으로 값을 바꾸고 실패 결과는 그대로 둔다`() {
        val success: DataResult<Int> = DataResult.Success(2)
        val failure: DataResult<Int> = DataResult.Failure(DataError.Offline)

        assertEquals(DataResult.Success(4), success.map { it * 2 })
        assertEquals(DataResult.Failure(DataError.Offline), failure.map { it * 2 })
    }

    @Test
    fun `getOrNull과 errorOrNull은 성공과 실패를 구분한다`() {
        val success: DataResult<String> = DataResult.Success("값")
        val failure: DataResult<String> = DataResult.Failure(DataError.Offline)

        assertEquals("값", success.getOrNull())
        assertNull(success.errorOrNull())
        assertNull(failure.getOrNull())
        assertEquals(DataError.Offline, failure.errorOrNull())
    }
}
