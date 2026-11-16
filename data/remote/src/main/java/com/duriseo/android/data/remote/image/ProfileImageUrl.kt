package com.duriseo.android.data.remote.image

import coil3.map.Mapper
import coil3.request.Options
import com.duriseo.android.core.common.config.AppConfig
import com.duriseo.android.domain.profile.ProfileImage
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * 프로필 사진 주소: `<서버 주소>/api/v1/users/{id}/profile-image?v=<version>` (API.md 3.1).
 * `v`는 캐시 키라서 사진이 바뀌면 주소가 달라져 Coil의 메모리·디스크 캐시가 새로 받는다.
 * 서버 주소가 비어 있거나(CI 빌드) 주소 모양이 아니면 null이다. 사진 없음(버전 null)은 [ProfileImage]가 만들어지지 않아 여기까지 오지 않는다.
 */
internal fun profileImageUrl(config: AppConfig, image: ProfileImage): String? {
    val base = config.apiV1BaseUrl()?.toHttpUrlOrNull() ?: return null
    return base.newBuilder()
        .addPathSegment("users")
        .addPathSegment(image.userId)
        .addPathSegment("profile-image")
        .addQueryParameter("v", image.version)
        .build()
        .toString()
}

/** 화면이 넘긴 [ProfileImage]를 주소로 바꾸는 Coil 매퍼. 이 뒤의 요청은 인증 OkHttp가 보낸다 ([ProfileImageModule]) */
internal class ProfileImageMapper(private val config: AppConfig) : Mapper<ProfileImage, String> {
    override fun map(data: ProfileImage, options: Options): String? = profileImageUrl(config, data)
}
