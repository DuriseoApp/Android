package com.duriseo.android.feature.settings.profile

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.DuriseoAvatar
import com.duriseo.android.core.designsystem.component.DuriseoAvatarSize
import com.duriseo.android.core.designsystem.component.DuriseoSheetPanel
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line200
import com.duriseo.android.core.designsystem.theme.PersonMe
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.feature.settings.R

// SCR-SET-04 사진 시트 "프로필 사진" (보드 `ProfileEdit.dc.html` 91~103행, `ProfilePhotoSheet.dc.html`, `SCR-SET-04--photo-sheet.png`).
// 시트 면·손잡이는 공용 `DuriseoSheetPanel`이다. 수치는 보드 값을 이름 붙인 상수로 둔다.

/** 손잡이와 제목 사이 14, 제목과 첫 줄 사이 6 (보드 `margin-bottom`) */
private val HandleGap = 14.dp
private val TitleGap = 6.dp

/** 행: 높이 56, 아이콘 22, 아이콘과 글자 사이 14, 글자 16/500 */
private val RowHeight = 56.dp
private val RowIconSize = 22.dp
private val RowGap = 14.dp

/** [취소]: 위 12, 높이 48, 모서리 `radius-md`, 테두리 1 `line-200`, 글자 15/600 */
private val CancelTop = 12.dp
private val CancelHeight = 48.dp
private val CancelBorder = 1.dp

/** 사진 시트의 행. [취소]는 행이 아니라 아래 버튼이다 */
internal enum class PhotoSheetRow { Album, Camera, DefaultImage }

/** 시트에 보일 행. 카메라 앱이 없으면 [PhotoSheetRow.Camera]를 숨긴다 (unit-8.md 확인 필요 4, FR-AUTH-07 AC9) */
internal fun photoSheetRows(cameraAvailable: Boolean): List<PhotoSheetRow> =
    if (cameraAvailable) {
        listOf(PhotoSheetRow.Album, PhotoSheetRow.Camera, PhotoSheetRow.DefaultImage)
    } else {
        listOf(PhotoSheetRow.Album, PhotoSheetRow.DefaultImage)
    }

/**
 * "프로필 사진" 시트: [앨범에서 선택] [사진 찍기] [기본 이미지로 변경] [취소].
 * [DuriseoBottomSheetLayer] 안에서 쓴다. [colorKey]는 [기본 이미지로 변경] 줄의 작은 아바타 색(내 색)이다.
 */
@Composable
internal fun PhotoSheet(
    cameraAvailable: Boolean,
    colorKey: String,
    onAlbum: () -> Unit,
    onCamera: () -> Unit,
    onDefaultImage: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DuriseoSheetPanel(modifier = modifier, gap = 0.dp) {
        Spacer(Modifier.height(HandleGap))
        Text(
            text = stringResource(R.string.profile_photo_sheet_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = Ink900,
        )
        Spacer(Modifier.height(TitleGap))
        photoSheetRows(cameraAvailable).forEach { row ->
            when (row) {
                PhotoSheetRow.Album -> SheetRow(
                    label = stringResource(R.string.profile_photo_sheet_album),
                    onClick = onAlbum,
                ) { SheetIcon(DesignR.drawable.ic_image) }
                PhotoSheetRow.Camera -> SheetRow(
                    label = stringResource(R.string.profile_photo_sheet_camera),
                    onClick = onCamera,
                ) { SheetIcon(DesignR.drawable.ic_camera) }
                PhotoSheetRow.DefaultImage -> SheetRow(
                    label = stringResource(R.string.profile_photo_sheet_default),
                    onClick = onDefaultImage,
                ) {
                    DuriseoAvatar(
                        label = stringResource(R.string.profile_letter_me),
                        colorKey = colorKey,
                        size = DuriseoAvatarSize.Mini,
                        fallback = PersonMe,
                    )
                }
            }
        }
        Spacer(Modifier.height(CancelTop))
        val shape = MaterialTheme.shapes.medium
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CancelHeight)
                .background(Surface0, shape)
                .border(CancelBorder, Line200, shape)
                .clickable(role = Role.Button, onClick = onCancel),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.profile_photo_sheet_cancel),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Ink900,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SheetRow(label: String, onClick: () -> Unit, leading: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RowGap),
    ) {
        leading()
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = Ink900,
        )
    }
}

@Composable
private fun SheetIcon(@DrawableRes iconRes: Int) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        modifier = Modifier.size(RowIconSize),
        tint = Ink900,
    )
}
