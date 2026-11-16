package com.duriseo.android.feature.settings.profile.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.duriseo.android.domain.profile.PickedPhotoChecker
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** 올릴 사진: 바이트와 Content-Type (`image/jpeg`·`image/png`) */
class PreparedPhoto(val bytes: ByteArray, val mimeType: String)

/**
 * 25MP(25,000,000픽셀)를 넘는 사진을 줄여서 올릴 준비를 한다 (D-153, NFR-FILE-01).
 * 서버가 픽셀 수 25MP 초과를 413으로 거부하므로 앱이 먼저 줄이고, 줄이지 않아도 되는 사진은 **원본 바이트를 그대로** 쓴다.
 *
 * 줄일 때: 디코드는 메모리를 아끼려고 크기(bounds)만 먼저 읽고 `inSampleSize`로 줄여서 읽는다. 그 뒤 EXIF 회전을 픽셀에 적용하고,
 * 투명한 부분은 흰 배경에 합성해 JPEG로 다시 인코딩한다 (서버도 같은 처리를 하지만 25MP 안으로 들어오게 하는 것이 앱의 몫이다).
 * 디코드·인코딩은 무거우니 **백그라운드 스레드에서** 부른다.
 */
object PhotoDownscaler {
    private const val MAX_PIXELS = PickedPhotoChecker.MAX_PIXELS
    private const val MAX_BYTES = PickedPhotoChecker.MAX_BYTES

    /** JPEG 품질. 서버가 긴 변 512px로 다시 줄이므로 크게 중요하지 않다. 결과가 10MB를 넘으면 낮춰 다시 인코딩한다 */
    private val JpegQualities = intArrayOf(90, 80, 70, 60)

    /**
     * [original]의 픽셀 수가 25MP 이하이거나 크기를 읽지 못하면(깨진 파일 등은 서버가 415로 거른다) [original] 그대로 [mimeType]과 함께 돌려준다.
     * 넘으면 줄인 JPEG를 `image/jpeg`로 돌려준다.
     */
    fun prepare(original: ByteArray, mimeType: String): PreparedPhoto {
        val bounds = readBounds(original) ?: return PreparedPhoto(original, mimeType)
        if (fitWithinPixels(bounds.width, bounds.height, MAX_PIXELS) == null) return PreparedPhoto(original, mimeType)
        val jpeg = downscaleToJpeg(original, bounds) ?: return PreparedPhoto(original, mimeType)
        return PreparedPhoto(jpeg, PickedPhotoChecker.MIME_JPEG)
    }

    /**
     * 미리보기용으로 디코드한 [decoded]에 [original] 바이트의 EXIF 방향을 적용해 돌려준다 (카메라 사진이 옆으로 눕지 않게).
     * 회전이 필요하면 새 비트맵을 만들고 [decoded]는 풀어 준다.
     */
    internal fun applyExifOrientation(decoded: Bitmap, original: ByteArray): Bitmap =
        replace(decoded, applyOrientation(decoded, readOrientation(original)))

    /** 픽셀은 읽지 않고 크기만 읽는다. 읽지 못하면 null */
    private fun readBounds(bytes: ByteArray): PixelSize? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        return PixelSize(options.outWidth, options.outHeight)
    }

    private fun downscaleToJpeg(original: ByteArray, bounds: PixelSize): ByteArray? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(original, 0, original.size, options) ?: return null
        var working: Bitmap = decoded
        try {
            // 2의 거듭제곱으로만 줄어들어 아직 25MP를 넘으면(드묾) 비율을 지켜 목표 크기로 맞춘다
            val target = fitWithinPixels(working.width, working.height, MAX_PIXELS)
            if (target != null) working = replace(working, Bitmap.createScaledBitmap(working, target.width, target.height, true))
            working = replace(working, applyOrientation(working, readOrientation(original)))
            working = replace(working, flattenOnWhite(working))
            return encodeJpeg(working)
        } finally {
            working.recycle()
        }
    }

    /** 디코드 결과가 25MP 이하가 되는 가장 작은 2의 거듭제곱. 큰 사진을 원래 크기로 읽지 않아 메모리를 아낀다 */
    internal fun sampleSizeFor(bounds: PixelSize): Int {
        var sample = 1
        while (decodedPixels(bounds, sample) > MAX_PIXELS) sample *= 2
        return sample
    }

    /** `inSampleSize`로 읽은 크기: 올림으로 센다 (디코더가 올림하는 경우가 있어 넉넉하게) */
    private fun decodedPixels(bounds: PixelSize, sample: Int): Long {
        val width = (bounds.width + sample - 1) / sample
        val height = (bounds.height + sample - 1) / sample
        return width.toLong() * height
    }

    /** [next]가 새 비트맵이면 이전 것을 풀어 주고 [next]를 돌려준다 (같은 객체면 그대로) */
    private fun replace(previous: Bitmap, next: Bitmap): Bitmap {
        if (next !== previous) previous.recycle()
        return next
    }

    private fun readOrientation(bytes: ByteArray): Int = try {
        ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (e: Exception) {
        // EXIF를 읽지 못하면 회전 없음으로 본다 (사진이 옆으로 누울 수는 있지만 올리기는 막지 않는다)
        ExifInterface.ORIENTATION_NORMAL
    }

    /** EXIF 방향(1~8)을 픽셀에 적용한다. 정상(1)·모르는 값은 그대로 */
    private fun applyOrientation(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return source
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /** 투명한 픽셀이 있으면 흰 배경에 합성한다 (JPEG는 투명을 못 담아 검게 나오기 때문). 불투명이면 그대로 */
    private fun flattenOnWhite(source: Bitmap): Bitmap {
        if (!source.hasAlpha()) return source
        val flattened = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(flattened)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(source, 0f, 0f, null)
        return flattened
    }

    /** JPEG로 인코딩한다. 10MB를 넘으면 품질을 낮춰 다시 인코딩하고, 끝까지 넘으면 가장 낮은 품질 결과를 돌려준다 */
    private fun encodeJpeg(bitmap: Bitmap): ByteArray {
        var result = ByteArray(0)
        for (quality in JpegQualities) {
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            result = output.toByteArray()
            if (result.size <= MAX_BYTES) break
        }
        return result
    }
}
