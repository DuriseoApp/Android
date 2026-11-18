package com.duriseo.android.feature.settings.profile.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 25MP 줄이기의 순수 계산 (D-153). 실제 디코드·인코딩은 Android 비트맵이라 단위 테스트 밖이다 */
class PhotoDownscalerTest {
    private val max = 25_000_000L

    @Test
    fun `D-153 25MP를 넘으면 비율을 지켜 25MP 안으로 줄인 크기를 계산한다`() {
        // 8000 x 6000 = 48MP (4:3)
        val size = fitWithinPixels(8000, 6000, max)!!

        assertTrue(size.pixels <= max)
        // 가능한 한 크게 남긴다 (상한의 99.9% 이상)
        assertTrue(size.pixels > max * 999 / 1000)
        // 가로세로 비율은 4:3 그대로 (내림 오차 1픽셀 안)
        assertTrue(Math.abs(size.width * 3L - size.height * 4L) <= 4)
        assertEquals(PixelSize(5773, 4330), size)

        // 세로 사진도 같다 (6000 x 8000)
        val portrait = fitWithinPixels(6000, 8000, max)!!
        assertEquals(PixelSize(4330, 5773), portrait)

        // 25,000,001 픽셀이면 줄인다
        val justOver = fitWithinPixels(5001, 5000, max)!!
        assertTrue(justOver.pixels <= max)

        // 아주 긴 사진(파노라마)도 비율을 지킨다
        val panorama = fitWithinPixels(30_000, 1_000, max)!!
        assertTrue(panorama.pixels <= max)
        assertTrue(Math.abs(panorama.width.toDouble() / panorama.height - 30.0) < 0.1)
    }

    @Test
    fun `D-153 25MP 이하는 그대로 둔다`() {
        assertNull(fitWithinPixels(4000, 3000, max))
        // 정확히 25MP는 서버도 통과시킨다 (넘을 때만 413)
        assertNull(fitWithinPixels(5000, 5000, max))
        assertNull(fitWithinPixels(1, 1, max))
        // 크기를 모르는 값은 줄이지 않는다
        assertNull(fitWithinPixels(0, 100, max))
        assertNull(fitWithinPixels(100, -1, max))
    }

    @Test
    fun `D-153 큰 사진은 디코드 결과가 25MP 이하가 되는 가장 작은 2의 거듭제곱으로 읽는다`() {
        assertEquals(1, PhotoDownscaler.sampleSizeFor(PixelSize(4000, 3000)))
        assertEquals(2, PhotoDownscaler.sampleSizeFor(PixelSize(8000, 6000)))
        // 10000 x 10000을 2로 읽으면 정확히 25MP
        assertEquals(2, PhotoDownscaler.sampleSizeFor(PixelSize(10_000, 10_000)))
        assertEquals(4, PhotoDownscaler.sampleSizeFor(PixelSize(10_001, 10_001)))
        assertNotNull(fitWithinPixels(10_001, 10_001, max))
    }
}
