package com.duriseo.android.core.designsystem.component

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.core.view.WindowCompat
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing

// 덮개(하단 시트·다이얼로그·요청 중 덮개)를 그리는 별도 창.
// 화면 안의 Box로 그리면 하단 탭(MainScaffold의 bottomBar)이 덮개 위에 그려져 탭이 시트를 가리고 요청 중에도 눌린다.
// 별도 창은 앱 화면 전체(하단 탭·상태바·내비게이션 바)보다 위에 뜬다.

/**
 * 덮개용 별도 창 안에 [content]를 그린다. 이 파일 밖에서 직접 쓰지 않는다 (`internal`).
 * 시트·다이얼로그·요청 중 덮개는 [DuriseoBottomSheetLayer]·[DuriseoDialog]·[DuriseoLoadingOverlay]로만 그린다.
 *
 * - 창 크기는 화면 전체, 창 기본 뒤 막(dim)은 끄고(토큰 `scrim`만 쓴다) 상태바·내비게이션 바 밑까지 그린다.
 *   내비게이션 바 inset은 창 안에서도 `WindowInsets.navigationBars`로 읽힌다.
 * - 뒤로 가기: [onBack]이 있으면 그것을 부르고, null이면 아무것도 하지 않고 막는다 (창이 받으므로 뒤 화면으로 새지 않는다).
 *   바깥(뒤 막) 누름은 창이 아니라 호출한 쪽의 scrim이 받는다 (`dismissOnClickOutside = false`).
 * - 나중에 연 창이 위에 뜬다. 요청 중 덮개는 요청이 시작될 때 열려서 이미 떠 있는 시트·다이얼로그 위에 온다.
 * - Preview(`LocalInspectionMode`)에서는 창을 열지 못하므로 화면 안에 그린다.
 *
 * @param snackbarAtTop true면 스낵바를 늘 위에서 40에 그린다 (하단 시트 창). 아니면 [LocalOverlaySnackbar]의 위치 그대로
 * @param showSnackbar 앱 스낵바([LocalOverlaySnackbar])를 이 창 위에도 그릴지. 창이 화면을 덮으면 아래 스낵바가 가려지기 때문이다
 */
@Composable
internal fun DuriseoOverlayWindow(
    onBack: (() -> Unit)?,
    showSnackbar: Boolean = true,
    snackbarAtTop: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (LocalInspectionMode.current) {
        content()
        return
    }
    // 열린 창 목록에 등록한다. 등록 순서가 곧 위아래 순서다 (나중에 연 창이 위)
    val token = remember { Any() }
    DisposableEffect(token) {
        DuriseoOverlayWindows.open(token)
        onDispose { DuriseoOverlayWindows.close(token) }
    }
    val isTop = DuriseoOverlayWindows.isTop(token)
    Dialog(
        // dismissOnBackPress를 항상 true로 두어 뒤로 가기를 이 창이 받는다. onBack이 null이면 아무것도 하지 않고 막는다
        onDismissRequest = { onBack?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        ConfigureOverlayWindow()
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 위에 다른 창(요청 중 덮개 등)이 있으면 이 창의 내용은 접근성 트리에서 뺀다 (창 경계를 넘어 TalkBack이 새지 않게)
                .then(if (isTop) Modifier else Modifier.clearAndSetSemantics { }),
        ) {
            content()
            // 스낵바는 맨 위 창만 그린다 (같은 스낵바를 창 수만큼 읽지 않게)
            if (showSnackbar && isTop) OverlaySnackbar(forceTop = snackbarAtTop)
        }
    }
}

/**
 * 지금 열려 있는 덮개 창 목록 (앱 전체에서 하나). 맨 위 창만 앱 스낵바를 그리고, 창이 하나라도 있으면
 * 아래 화면의 스낵바는 접근성에서 뺀다 ([overlayAwareSnackbar]).
 */
object DuriseoOverlayWindows {
    private val tokens = mutableStateListOf<Any>()

    /** 덮개 창이 하나라도 열려 있으면 true */
    val isOpen: Boolean get() = tokens.isNotEmpty()

    internal fun open(token: Any) {
        tokens.add(token)
    }

    internal fun close(token: Any) {
        tokens.remove(token)
    }

    /** 등록 전(첫 프레임)에는 목록이 비어 있어 맨 위로 본다 */
    internal fun isTop(token: Any): Boolean = tokens.isEmpty() || tokens.last() === token
}

/**
 * 덮개 창이 열려 있는 동안 아래 화면의 스낵바를 접근성 트리에서 뺀다. 같은 스낵바를 창 안에서 다시 그리기 때문이다.
 * 화면의 스낵바 자리(`DuriseoSnackbarHost`·`DuriseoTopSnackbarHost`)의 `modifier`에 붙인다.
 */
fun Modifier.overlayAwareSnackbar(): Modifier =
    if (DuriseoOverlayWindows.isOpen) clearAndSetSemantics { } else this

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** 창 기본 dim을 끄고, 창을 화면 전체로 하고, 시스템 바를 투명하게 해 scrim이 바 밑까지 덮게 한다. */
@Composable
private fun ConfigureOverlayWindow() {
    val view = LocalView.current
    val hostWindow = LocalContext.current.findActivity()?.window
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0f)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        // 컷아웃(노치) 영역까지 scrim이 덮게
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = params
        }
        // 시스템 바 아이콘 밝기는 호스트 Activity 창의 것을 그대로 쓴다 (새 창은 기본값이라 아이콘이 안 보일 수 있다)
        if (hostWindow != null) {
            val host = WindowCompat.getInsetsController(hostWindow, hostWindow.decorView)
            val overlay = WindowCompat.getInsetsController(window, window.decorView)
            overlay.isAppearanceLightStatusBars = host.isAppearanceLightStatusBars
            overlay.isAppearanceLightNavigationBars = host.isAppearanceLightNavigationBars
        }
    }
}

/**
 * 덮개 창 위에 그릴 앱 스낵바 (D-126). 덮개 창이 화면을 덮으면 아래 화면의 스낵바가 가려지므로, 같은 [hostState]를
 * 같은 자리에 창 안에서도 그린다. 위에서 40([atTop]) 또는 하단 탭 위 16([aboveBottomBar]).
 * `feature:main`이 앱 스낵바를, 연결 완료 시트가 자기 스낵바를 제공한다.
 */
@Stable
class DuriseoOverlaySnackbar(
    val hostState: SnackbarHostState,
    val atTop: Boolean,
    val aboveBottomBar: Boolean,
)

val LocalOverlaySnackbar = compositionLocalOf<DuriseoOverlaySnackbar?> { null }

@Composable
private fun BoxScope.OverlaySnackbar(forceTop: Boolean) {
    val spec = LocalOverlaySnackbar.current ?: return
    // 하단 시트가 떠 있는 동안은 탭 위가 아니라 위에서 40이다 (D-126): 아래에 두면 시트의 [저장]을 덮는다
    if (spec.atTop || forceTop) {
        DuriseoTopSnackbarHost(hostState = spec.hostState, modifier = Modifier.align(Alignment.TopCenter))
    } else {
        DuriseoSnackbarHost(
            hostState = spec.hostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(bottom = if (spec.aboveBottomBar) DuriseoSize.BottomNav + DuriseoSpacing.Space4 else DuriseoSpacing.Space4),
        )
    }
}
