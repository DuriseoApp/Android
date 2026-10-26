package com.duriseo.android.core.designsystem.component

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.OnBrand

/**
 * 오프라인 안내("인터넷 연결이 필요해요", NFR-OFF-02)와 공통 오류 안내를 보여 주는 스낵바
 * (리드 결정 2026-10-26: 확인 필요 7·8).
 *
 * 면은 `ink-900`, 글자는 `on-brand`(흰색)다.
 * TODO(Docs PR): design-system.md "컴포넌트"에 Snackbar 명세(글자색 흰색 계열 확정, 모서리, 위치)가
 *  들어오면 맞춘다. 지금 모서리는 M3 기본이다.
 */
@Composable
fun DuriseoSnackbar(
    message: String,
    modifier: Modifier = Modifier,
) {
    Snackbar(
        modifier = modifier,
        containerColor = Ink900,
        contentColor = OnBrand,
    ) {
        Text(text = message)
    }
}

/** `Scaffold(snackbarHost = { DuriseoSnackbarHost(hostState) })`로 쓴다. 메시지는 `visuals.message`. */
@Composable
fun DuriseoSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
    ) { data ->
        DuriseoSnackbar(message = data.visuals.message)
    }
}

@Preview(showBackground = true)
@Composable
private fun DuriseoSnackbarOfflinePreview() {
    DuriseoTheme {
        DuriseoSnackbar(message = stringResource(R.string.offline_required))
    }
}

@Preview(showBackground = true)
@Composable
private fun DuriseoSnackbarCommonErrorPreview() {
    DuriseoTheme {
        DuriseoSnackbar(message = stringResource(R.string.error_common))
    }
}
