package com.duriseo.android.feature.appops

import coil3.size.Size
import com.duriseo.android.core.common.image.popupImageRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Q-23: 미리 받기와 화면이 같은 요청(같은 주소·같은 크기)을 써야 메모리 캐시 키가 맞는다 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PopupImageRequestTest {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun `Q-23 미리 받기와 화면의 요청은 주소와 크기가 같다`() = runTest {
        val prefetch = popupImageRequest(context, "https://example.com/a.png")
        val screen = popupImageRequest(context, "https://example.com/a.png")

        assertEquals(prefetch.data, screen.data)
        assertEquals(Size.ORIGINAL, prefetch.sizeResolver.size())
        assertEquals(prefetch.sizeResolver.size(), screen.sizeResolver.size())
        assertEquals(prefetch.sizeResolver, screen.sizeResolver)
    }
}
