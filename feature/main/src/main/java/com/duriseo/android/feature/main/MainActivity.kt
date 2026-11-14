package com.duriseo.android.feature.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.duriseo.android.core.common.push.toPushTapTarget
import com.duriseo.android.core.designsystem.permission.ProvideNotificationPermissionRequester
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** `MainScreen`의 `hiltViewModel()`과 같은 인스턴스다 (둘 다 이 Activity의 저장소) */
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 화면 회전 등으로 다시 만들어질 때는 이미 처리한 Intent라 건너뛴다
        if (savedInstanceState == null) handlePushIntent(intent)
        setContent {
            DuriseoTheme {
                // 알림 권한 요청기는 맨 위층에서 한 번 만든다: 화면이 닫혀도 요청이 끊기지 않는다 (FR-NOTI-01, D-144)
                ProvideNotificationPermissionRequester {
                    MainScreen()
                }
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
     * 푸시 알림을 눌러 들어온 Intent를 읽어 [MainViewModel]에 넘긴다 (FR-NOTI-03, D-142·D-143). 처음 켤 때(`onCreate`)와
     * 앱이 떠 있을 때(`onNewIntent`) 모두 여기로 온다. 푸시 Intent가 아니면(엑스트라 없음, 모르는 종류) 아무것도 하지 않는다.
     * 읽음 처리·이동·삭제 안내는 ViewModel과 `MainScreen`이 한다.
     */
    private fun handlePushIntent(intent: Intent?) {
        val tap = intent?.toPushTapTarget() ?: return
        mainViewModel.onPushTapped(tap)
    }
}
