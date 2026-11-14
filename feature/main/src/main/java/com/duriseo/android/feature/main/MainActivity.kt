package com.duriseo.android.feature.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 화면 회전 등으로 다시 만들어질 때는 이미 처리한 Intent라 건너뛴다
        if (savedInstanceState == null) handlePushIntent(intent)
        setContent {
            DuriseoTheme {
                MainScreen()
            }
        }
    }

    /** 앱이 떠 있을 때 푸시를 누르면 새 화면을 만들지 않고 여기로 온다 (`core:push`가 SINGLE_TOP·CLEAR_TOP으로 보낸다) */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    /**
     * 푸시 알림을 눌러 들어온 Intent를 받는 자리 (FR-NOTI-03, D-142·D-143). **단위 7의 D 묶음이 채운다.**
     * 엑스트라는 `com.duriseo.android.core.common.push.PushIntentExtras`의 `notificationId`·`kind`·`targetType`·`targetId`이고,
     * `Intent.toPushTapTarget()`이 `PushTapTarget`으로 읽어 준다. 푸시 Intent가 아니면(`PushIntentExtras.isPushIntent`) 아무것도 하지 않는다.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun handlePushIntent(intent: Intent?) = Unit
}
