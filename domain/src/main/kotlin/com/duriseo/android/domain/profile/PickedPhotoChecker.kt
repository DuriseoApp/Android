package com.duriseo.android.domain.profile

/**
 * 고른 사진을 올리기 전에 하는 검사 (FR-AUTH-07 AC7, NFR-FILE-01, D-147). 순수 Kotlin이라 Android 없이 테스트한다.
 *
 * 한계 값은 서버(`ProfileImageProcessor`)와 같다: 원본 10MB(10 * 1024 * 1024 바이트, 정확히 10MB는 통과), 픽셀 25,000,000개 (D-153).
 * 크기는 형식보다 먼저 본다 (서버도 크기 -> 형식 순서다).
 */
object PickedPhotoChecker {
    /** 원본 최대 바이트 수 (서버 `ProfileImageProcessor.MAX_BYTES`) */
    const val MAX_BYTES: Long = 10L * 1024 * 1024

    /** 픽셀 수 상한 (서버 `ProfileImageProcessor.MAX_PIXELS`). 넘으면 앱이 줄여서 올린다 (D-153) */
    const val MAX_PIXELS: Long = 25_000_000L

    private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val PNG_SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    )

    const val MIME_JPEG = "image/jpeg"
    const val MIME_PNG = "image/png"

    /**
     * [byteCount]가 10MB를 넘으면 [PhotoError.TOO_LARGE], [header]의 머리 시그니처가 JPEG(FF D8 FF)·PNG(89 50 4E 47 0D 0A 1A 0A)가
     * 아니면 [PhotoError.TYPE_NOT_ALLOWED], 통과하면 null. [header]는 파일 맨 앞 8바이트 이상이면 된다.
     */
    fun check(byteCount: Long, header: ByteArray): PhotoError? = when {
        byteCount > MAX_BYTES -> PhotoError.TOO_LARGE
        mimeTypeOf(header) == null -> PhotoError.TYPE_NOT_ALLOWED
        else -> null
    }

    /** 머리 시그니처로 본 형식(`image/jpeg`·`image/png`). 둘 다 아니면 null. 확장자나 선택기가 알려 준 형식은 믿지 않는다 */
    fun mimeTypeOf(header: ByteArray): String? = when {
        header.startsWith(JPEG_SIGNATURE) -> MIME_JPEG
        header.startsWith(PNG_SIGNATURE) -> MIME_PNG
        else -> null
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
}
