package com.duriseo.android.feature.memo.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duriseo.android.core.designsystem.component.DuriseoSwitch
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line100
import com.duriseo.android.core.designsystem.theme.Line300
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.feature.memo.R

// SCR-MEMO-04 메모 입력 부분 (FR-MEMO-02, FR-NOTI-04). 수치는 보드 `AddEvent.dc.html`(메모 종류, 123~137행).
// 단독 화면(탭 없음, D-174)이라 [MemoEditScreen]만 쓴다. 메모 날짜는 없다 (날짜 영역·날짜 시트 삭제).

/** 덩어리 사이 20 (26행). 라벨과 입력 사이 8, 라벨 13/600 `ink-600` */
private val SectionGap = DuriseoSpacing.Space5
private val LabelGap = DuriseoSpacing.Space2

/** 본문 입력 최소 높이: 보드 `textarea rows=2`, 안쪽 위아래 14·좌우 16, 15/1.6 (125행) */
private val BodyMinHeight = 96.dp
private val BodyPaddingVertical = 14.dp

/** 알림 카드: 안쪽 14·16, 모서리 16, 테두리 1 `line-100`, 글 사이 2, 카드 안 사이 12 (128~131행) */
private val NotifyPaddingVertical = 14.dp
private val NotifyTextGap = 2.dp

/**
 * 메모 입력 부분: 제목, 본문, 알림 스위치 (커플의 새 메모만). 앱바·[저장] 버튼·스크롤은 포함하지 않는다.
 * 바깥(SCR-MEMO-04 화면)이 [MemoEditUiState.saveEnabled]·[MemoEditAction.SaveClicked]로 저장을 맡는다.
 */
@Composable
internal fun MemoForm(
    state: MemoEditUiState,
    onAction: (MemoEditAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        LabeledField(label = stringResource(R.string.memo_edit_title_label)) {
            TextInput(
                value = state.title,
                onValueChange = { onAction(MemoEditAction.TitleChanged(it)) },
                placeholder = stringResource(R.string.memo_edit_title_placeholder),
                singleLine = true,
                enabled = !state.saving,
            )
        }

        LabeledField(label = stringResource(R.string.memo_edit_body_label)) {
            TextInput(
                value = state.body,
                onValueChange = { onAction(MemoEditAction.BodyChanged(it)) },
                placeholder = stringResource(R.string.memo_edit_body_placeholder),
                singleLine = false,
                enabled = !state.saving,
                modifier = Modifier.heightIn(min = BodyMinHeight),
            )
        }

        // 커플 공간의 새 메모에서만. 수정·개인 공간에는 스위치도 값도 없다 (FR-NOTI-04 AC3)
        if (state.showNotify) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .border(1.dp, Line100, MaterialTheme.shapes.large)
                    .padding(horizontal = DuriseoSpacing.Space4, vertical = NotifyPaddingVertical),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NotifyTextGap)) {
                    Text(
                        text = stringResource(R.string.memo_edit_notify_title),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Ink900,
                    )
                    Text(
                        text = stringResource(R.string.memo_edit_notify_description),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                        color = Ink600,
                    )
                }
                DuriseoSwitch(
                    checked = state.notifyPartner,
                    onCheckedChange = { onAction(MemoEditAction.NotifyChanged(it)) },
                    enabled = !state.saving,
                )
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(LabelGap)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = Ink600)
        content()
    }
}

@Composable
private fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DuriseoSize.Input)
            .background(Surface0, shape)
            .border(1.dp, Line300, shape),
        enabled = enabled,
        singleLine = singleLine,
        // 제목 17, 본문 15/1.6 (35·125행)
        textStyle = (if (singleLine) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal) else MaterialTheme.typography.bodyMedium)
            .copy(color = Ink900),
        cursorBrush = SolidColor(Ink900),
        decorationBox = { inner ->
            Box(
                modifier = Modifier.padding(
                    horizontal = DuriseoSpacing.Space4,
                    vertical = if (singleLine) DuriseoSpacing.Space3 else BodyPaddingVertical,
                ),
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = if (singleLine) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal) else MaterialTheme.typography.bodyMedium,
                        color = Ink500,
                    )
                }
                inner()
            }
        },
    )
}

// --- Preview. 견본은 보드 `AddEvent.dc.html`(커플, 메모 종류)와 같다. 날짜 영역은 없다 (D-174) ---

private val PreviewBase = MemoEditUiState(loaded = true, isCouple = true)

@Composable
private fun PreviewForm(state: MemoEditUiState) {
    DuriseoTheme {
        Column(modifier = Modifier.background(Surface0).padding(horizontal = DuriseoSpacing.Space5, vertical = DuriseoSpacing.Space2)) {
            MemoForm(state = state, onAction = {})
        }
    }
}

/** 기본: 알림 스위치 켜짐 (`SCR-MEMO-04` 입력 부분) */
@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun MemoFormPreview_Default() = PreviewForm(PreviewBase)

/** 개인 공간 또는 수정: 알림 스위치 없음 */
@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun MemoFormPreview_NoNotify() = PreviewForm(
    PreviewBase.copy(isCouple = false, title = "팀 회식", body = "늦게 끝날 수도 있어요"),
)
