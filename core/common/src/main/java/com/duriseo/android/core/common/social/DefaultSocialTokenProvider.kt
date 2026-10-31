package com.duriseo.android.core.common.social

import android.app.Activity
import com.duriseo.android.domain.me.SocialProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSocialTokenProvider @Inject constructor(
    private val google: GoogleTokenSource,
    private val kakao: KakaoTokenSource,
) : SocialTokenProvider {
    override suspend fun requestToken(activity: Activity, provider: SocialProvider): SocialTokenResult =
        when (provider) {
            SocialProvider.GOOGLE -> google.request(activity)
            SocialProvider.KAKAO -> kakao.request(activity)
        }
}
