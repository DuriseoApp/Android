package com.duriseo.android.domain.profile

import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.error.ErrorCode
import com.duriseo.android.domain.error.hasCode
import com.duriseo.android.domain.result.DataResult

/** 서버가 프로필 사진을 거부한 이유 (API.md 5장 413·415, D-147). 앱이 사진을 고를 때 검사한 결과([PickedPhotoChecker])도 같은 타입을 쓴다 */
enum class PhotoError {
    /** 10MB 초과 (413 `IMAGE_TOO_LARGE`) */
    TOO_LARGE,

    /** JPEG·PNG가 아님 (415 `IMAGE_TYPE_NOT_ALLOWED`) */
    TYPE_NOT_ALLOWED,
}

/** 오류가 사진 크기·형식 오류면 그 종류, 아니면 null (나머지는 공통 오류 문구로 보인다). `code`로만 판단한다 (API.md 1.1) */
fun DataError.photoErrorOrNull(): PhotoError? = when {
    hasCode(ErrorCode.IMAGE_TOO_LARGE) -> PhotoError.TOO_LARGE
    hasCode(ErrorCode.IMAGE_TYPE_NOT_ALLOWED) -> PhotoError.TYPE_NOT_ALLOWED
    else -> null
}

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
     * 413·415는 `ApiError`로 돌아오고 [photoErrorOrNull]로 구분한다.
     */
    suspend fun uploadPhoto(bytes: ByteArray, mimeType: String): DataResult<Unit>

    /** `DELETE /me/profile-image` (204, 멱등) 뒤 `GET /me`로 사진 버전이 null인 내 정보를 저장한다 */
    suspend fun deletePhoto(): DataResult<Unit>
}
