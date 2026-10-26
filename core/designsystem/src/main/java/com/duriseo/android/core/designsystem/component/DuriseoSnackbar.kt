package com.duriseo.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.core.designsystem.theme.DuriseoShadows
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.duriseoShadow

// 스낵바 보드 값 (design-system.md "스낵바", 보드 `InviteCreate.dc.html` 74행, `Main.dc.html` 51행).
// 토큰에 없는 수치라 이름을 붙인 상수로 둔다.

/** 안쪽 위아래 여백 14 */
private val VerticalPadding = 14.dp

/** 글자 행간 21 (14 x 1.5). 크기·굵기는 `body-sm`(14/400)과 같다 */
private val LineHeight = 21.sp

/**
 * 오프라인 안내("인터넷 연결이 필요해요", NFR-OFF-02), 공통 오류, 다른 기기 로그인 안내를 보여 주는 스낵바.
 *
 * 면 `ink-900`(`inverseSurface`), 글자 `on-brand`(`inverseOnSurface`) `body-sm` 14/400 행간 21,
 * 모서리 `radius-md`, 안쪽 위아래 14·좌우 `space-4`, 화면 좌우에서 `space-4`씩 띄운 전체 폭,
 * 그림자 `shadow-fab`. 문구 한 줄만 있고 행동 버튼은 없다. 나타나면 스크린리더가 읽는다(`liveRegion = Polite`).
 */
@Composable
fun DuriseoSnackbar(
    message: String,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DuriseoSpacing.Space4)
            .duriseoShadow(shape, DuriseoShadows.Fab)
            .background(MaterialTheme.colorScheme.inverseSurface, shape)
            .padding(horizontal = DuriseoSpacing.Space4, vertical = VerticalPadding)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = LineHeight),
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )
    }
}

/**
 * `Scaffold(snackbarHost = { DuriseoSnackbarHost(hostState) })`로 쓴다. 메시지는 `visuals.message`다.
 * 보이는 시간은 Material 기본(`SnackbarDuration.Short`)이다. 위치는 Scaffold가 하단 탭 위에 놓고,
 * FAB가 있는 화면에서 FAB 위로 올리는 간격은 호출하는 쪽이 `modifier`의 bottom padding으로 준다.
 */
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
        Box(Modifier.padding(DuriseoSpacing.Space4)) {
            DuriseoSnackbar(message = stringResource(R.string.offline_required))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DuriseoSnackbarCommonErrorPreview() {
    DuriseoTheme {
        Box(Modifier.padding(DuriseoSpacing.Space4)) {
            DuriseoSnackbar(message = stringResource(R.string.error_common))
        }
    }
}
