package com.duriseo.android.feature.appops

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState

/**
 * 앱 전체 공지·업데이트 팝업의 자리 (SCR-APP-01·02). `MainActivity`의 맨 위층에 한 번 두고 모든 화면([content])을 감싼다:
 * 로그인 전 시작 화면·탭·시트·다이얼로그·튜토리얼 위에 뜬다 (팝업 창이 늘 맨 위다, `DuriseoPopupDialog`).
 * 팝업이 떠 있는 동안 뒤 화면은 접근성 트리에서 뺀다.
 */
@Composable
fun AppPopupHost(
    modifier: Modifier = Modifier,
    viewModel: AppPopupViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel, context) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                AppPopupSideEffect.FinishApp -> context.findActivity()?.finishAndRemoveTask()
                is AppPopupSideEffect.OpenUrl -> context.openUri(effect.url)
                is AppPopupSideEffect.OpenStore ->
                    if (!context.openUri(effect.marketUri)) context.openUri(effect.webUrl)
            }
        }
    }

    val popup = state.popup
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (popup != null) Modifier.clearAndSetSemantics { } else Modifier),
        ) {
            content()
        }
        if (popup != null) {
            AppPopupScreen(
                popup = popup,
                imageLoader = viewModel.imageLoader,
                onAction = viewModel::onAction,
                onUpdate = { context.findActivity()?.let { viewModel.onAction(AppPopupAction.Update(it)) } },
            )
        }
    }
}

/** 주소를 연다. 열 앱이 없으면 false */
private fun Context.openUri(uri: String): Boolean = try {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (e: ActivityNotFoundException) {
    false
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
