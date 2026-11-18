package com.duriseo.android.feature.settings.profile.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Context
import android.net.Uri
import com.duriseo.android.core.common.coroutines.IoDispatcher
import com.duriseo.android.domain.profile.PhotoError
import com.duriseo.android.domain.profile.PickedPhotoChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 저장하기 전에 기기에 들고 있는 고른 사진 (FR-AUTH-07 AC6). [bytes]는 올릴 바이트(원본 또는 25MP 안으로 줄인 JPEG, D-153),
 * [preview]는 아바타에 그릴 작은 그림이다. 바이트 배열이라 값 비교가 아니라 같은 객체인지로 비교한다.
 * [preview]는 화면 크기에 맞춰 줄인 것이라 크지 않다 (원본 비트맵을 들고 있지 않는다).
 */
class PickedPhoto(val bytes: ByteArray, val mimeType: String, val preview: Bitmap?)

/** 고른 사진을 읽은 결과 */
sealed interface PickedPhotoResult {
    data class Ready(val photo: PickedPhoto) : PickedPhotoResult

    /** 고를 때 검사에서 걸렸다: 크기 또는 형식 (D-147) */
    data class Rejected(val error: PhotoError) : PickedPhotoResult

    /** 읽지 못했다 (사진이 사라졌거나 메모리 부족 등). 공통 오류로 보인다 */
    data object Failed : PickedPhotoResult
}

/**
 * 사진 선택기·카메라 앱이 건넨 Uri의 사진을 읽어 검사하고(10MB·시그니처, [PickedPhotoChecker]) 올릴 준비를 한다([PhotoDownscaler]).
 * 무거운 일이라 IO 디스패처에서 한다. ViewModel이 Android 없이 테스트되도록 인터페이스로 나눴고 Uri는 문자열로 받는다.
 */
interface PickedPhotoReader {
    /** [fromCamera]면 카메라 임시 파일을 다 읽은 뒤(성공·실패 모두) 지운다 */
    suspend fun read(uri: String, fromCamera: Boolean): PickedPhotoResult
}

/** 카메라 앱이 설치돼 있는지. 없으면 사진 시트에서 [사진 찍기]를 숨긴다 (unit-8.md 확인 필요 4) */
fun interface CameraAvailability {
    fun isAvailable(): Boolean
}

class AndroidPickedPhotoReader @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PickedPhotoReader {

    override suspend fun read(uri: String, fromCamera: Boolean): PickedPhotoResult = try {
        withContext(io) {
            try {
                readBlocking(Uri.parse(uri))
            } finally {
                // 카메라 임시 파일은 다 읽은 뒤(성공·실패 모두) 지운다. 파일 입출력이라 IO 스레드에서 한다
                if (fromCamera) ProfilePhotoFiles.clear(context)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        PickedPhotoResult.Failed
    } catch (e: OutOfMemoryError) {
        // 큰 사진을 읽다 메모리가 모자랄 수 있다. 앱이 죽지 않게 읽기 실패로 돌려준다
        PickedPhotoResult.Failed
    }

    private fun readBlocking(uri: Uri): PickedPhotoResult {
        val raw = readUpToLimit(uri) ?: return PickedPhotoResult.Failed
        // 10MB를 넘으면 더 읽지 않고 크기 오류다 (큰 파일 전체를 메모리에 올리지 않는다)
        if (raw.size > PickedPhotoChecker.MAX_BYTES) return PickedPhotoResult.Rejected(PhotoError.TOO_LARGE)
        val bytes = raw.toByteArray()
        val error = PickedPhotoChecker.check(bytes.size.toLong(), bytes.copyOfRange(0, minOf(HEADER_SIZE, bytes.size)))
        if (error != null) return PickedPhotoResult.Rejected(error)
        val mime = PickedPhotoChecker.mimeTypeOf(bytes) ?: return PickedPhotoResult.Rejected(PhotoError.TYPE_NOT_ALLOWED)
        val prepared = PhotoDownscaler.prepare(bytes, mime)
        val preview = decodePreview(prepared.bytes)
        return PickedPhotoResult.Ready(PickedPhoto(prepared.bytes, prepared.mimeType, preview))
    }

    /** 최대 10MB + 1바이트까지만 읽는다. 열 수 없으면 null */
    private fun readUpToLimit(uri: Uri): ReadBytes? {
        val stream = context.contentResolver.openInputStream(uri) ?: return null
        return stream.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER_SIZE)
            var total = 0L
            while (total <= PickedPhotoChecker.MAX_BYTES) {
                val count = input.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
                total += count
            }
            ReadBytes(output, total)
        }
    }

    /** 아바타(112)에 그릴 작은 미리보기. 크기만 읽고 `inSampleSize`로 줄여서 디코드한다 (메모리 절약). 못 읽으면 null */
    private fun decodePreview(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = PhotoPreview.sampleSizeFor(bounds.outWidth, bounds.outHeight, PREVIEW_TARGET_PIXELS)
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        return PhotoDownscaler.applyExifOrientation(decoded, bytes)
    }

    private class ReadBytes(private val output: java.io.ByteArrayOutputStream, total: Long) {
        val size: Long = total
        fun toByteArray(): ByteArray = output.toByteArray()
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val HEADER_SIZE = 16

        /** 미리보기는 긴 변이 이 값 안팎이 되게 줄인다 (112dp 아바타를 큰 화면 밀도에서도 또렷하게) */
        const val PREVIEW_TARGET_PIXELS = 512
    }
}

/** 미리보기 디코드 배율 계산 (순수 계산이라 단위 테스트한다) */
internal object PhotoPreview {
    /** 짧은 변이 [targetSide] 이상으로 남는 가장 큰 2의 거듭제곱 (최소 1) */
    fun sampleSizeFor(width: Int, height: Int, targetSide: Int): Int {
        var sample = 1
        val shortSide = minOf(width, height)
        while (shortSide / (sample * 2) >= targetSide) sample *= 2
        return sample
    }
}
