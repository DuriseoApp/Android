package com.duriseo.android.domain.profile

import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.result.DataResult

/** 프로필 사진 오류의 종류 (D-147). 앱이 사진을 고를 때 검사한 결과([PickedPhotoChecker])와 서버 415가 쓴다 */
enum class PhotoError {
    /** 10MB 초과. 앱이 고를 때만 만든다 (서버 413은 25MP 초과 등이라 공통 오류, API.md 5장·D-153) */
    TOO_LARGE,

    /** JPEG·PNG가 아님 (415 `IMAGE_TYPE_NOT_ALLOWED`) */
    TYPE_NOT_ALLOWED,
}

/**
 * 서버 오류가 사진 형식 오류(415 `IMAGE_TYPE_NOT_ALLOWED`)면 [PhotoError.TYPE_NOT_ALLOWED], 아니면 null이다.
 * 413 `IMAGE_TOO_LARGE`는 앱이 고를 때 10MB를 이미 막으므로 25MP 초과 등이라 공통 오류로 보인다 (API.md 5장 413 행, D-153).
 * `code`로만 판단한다 (API.md 1.1)
 */
fun DataError.photoErrorOrNull(): PhotoError? =
    if (hasCode(ErrorCode.IMAGE_TYPE_NOT_ALLOWED)) PhotoError.TYPE_NOT_ALLOWED else null

/**
 * 내 프로필 쓰기 (FR-AUTH-07 AC6, 단위 8). 쓰기는 온라인에서만 하고 오프라인이면 요청 없이 `Offline`이다 (NFR-OFF-02).
 * 성공하면 Room의 내 정보가 새로 채워진다. 화면은 `MeRepository.observeMe()`로 본다.
 */
interface ProfileRepository {
    /** `PATCH /me {nickname}`. 앞뒤 공백을 지워 보내고 200 응답 `Me`를 Room에 저장한다 (D-82) */
    suspend fun updateNickname(nickname: String): DataResult<Unit>

    /**
     * `PUT /me/profile-image` (multipart `file`). [mimeType]은 `image/jpeg` 또는 `image/png`.
     * 204 뒤 `GET /me`로 새 사진 버전을 읽어 Room에 저장한다 (그 읽기가 실패해도 올리기는 성공이다).
     * 413·415는 `ApiError`로 돌아온다. 415만 [photoErrorOrNull]이 사진 형식 오류로 가리고 413은 공통 오류다.
     */
    suspend fun uploadPhoto(bytes: ByteArray, mimeType: String): DataResult<Unit>

    /** `DELETE /me/profile-image` (204, 멱등) 뒤 `GET /me`로 사진 버전이 null인 내 정보를 저장한다 */
    suspend fun deletePhoto(): DataResult<Unit>
}
