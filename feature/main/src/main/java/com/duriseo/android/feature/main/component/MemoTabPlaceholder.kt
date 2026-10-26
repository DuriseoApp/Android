package com.duriseo.android.feature.main.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 메모 탭 자리표시 (SCR-MEMO-01 자리). `feature:memo`(단위 6)가 생기면 그 모듈의 화면으로 바꾼다.
 * 문서에 없는 문구를 만들지 않기 위해 문구 없는 빈 화면이다.
 */
@Composable
fun MemoTabPlaceholder(
    padding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(padding),
    )
}
