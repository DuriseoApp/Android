package com.duriseo.android.domain.auth

import com.duriseo.android.domain.me.SocialProvider
import kotlinx.coroutines.flow.Flow

/**
 * 이 기기에서 마지막으로 로그인에 성공한 소셜 수단 (FR-AUTH-10, D-188). 기기 단위 기록이라 서버를 쓰지 않는다.
 * 기억하고 지우는 일은 [AuthRepository]가 로그인·연동·전환·탈퇴 성공 때 한다. 화면은 읽기만 한다.
 */
interface LastSignInRepository {
    /** 기억한 수단. 없으면 null */
    val lastProvider: Flow<SocialProvider?>
}
