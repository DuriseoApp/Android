package com.duriseo.android.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PickedPhotoCheckerTest {
    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0, 0x10)
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0)
    private val tenMb = 10L * 1024 * 1024

    @Test
    fun `FR-AUTH-07 AC7 10MB 초과는 크기 오류다`() {
        assertEquals(PhotoError.TOO_LARGE, PickedPhotoChecker.check(tenMb + 1, jpeg))
        assertEquals(PhotoError.TOO_LARGE, PickedPhotoChecker.check(tenMb + 1, png))
        // 크기 오류가 형식 오류보다 먼저다 (서버도 같은 순서)
        assertEquals(PhotoError.TOO_LARGE, PickedPhotoChecker.check(tenMb + 1, byteArrayOf(1, 2, 3)))
        // 정확히 10MB는 통과한다
        assertNull(PickedPhotoChecker.check(tenMb, jpeg))
    }

    @Test
    fun `FR-AUTH-07 AC7 시그니처가 JPEG PNG가 아니면 형식 오류다`() {
        assertNull(PickedPhotoChecker.check(1000, jpeg))
        assertNull(PickedPhotoChecker.check(1000, png))
        assertEquals(PhotoError.TYPE_NOT_ALLOWED, PickedPhotoChecker.check(1000, "GIF89a".toByteArray()))
        // WebP(RIFF....WEBP)
        assertEquals(PhotoError.TYPE_NOT_ALLOWED, PickedPhotoChecker.check(1000, "RIFF1234WEBP".toByteArray()))
        // 짧거나 빈 머리
        assertEquals(PhotoError.TYPE_NOT_ALLOWED, PickedPhotoChecker.check(0, ByteArray(0)))
        assertEquals(PhotoError.TYPE_NOT_ALLOWED, PickedPhotoChecker.check(2, byteArrayOf(0xFF.toByte(), 0xD8.toByte())))
        // PNG 앞 4바이트만 같은 경우
        assertEquals(PhotoError.TYPE_NOT_ALLOWED, PickedPhotoChecker.check(100, byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0, 0, 0, 0)))
    }

    @Test
    fun `FR-AUTH-07 AC7 머리 시그니처로 올릴 형식을 정한다`() {
        assertEquals("image/jpeg", PickedPhotoChecker.mimeTypeOf(jpeg))
        assertEquals("image/png", PickedPhotoChecker.mimeTypeOf(png))
        assertNull(PickedPhotoChecker.mimeTypeOf(byteArrayOf(0, 1, 2)))
    }
}
