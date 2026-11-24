package com.duriseo.android.feature.settings.profile

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.error as semanticsError
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.core.designsystem.component.DuriseoAvatar
import com.duriseo.android.core.designsystem.component.DuriseoAvatarImage
import com.duriseo.android.core.designsystem.component.DuriseoAvatarSize
import com.duriseo.android.core.designsystem.component.DuriseoBottomSheetLayer
import com.duriseo.android.core.designsystem.component.DuriseoDialogLayer
import com.duriseo.android.core.designsystem.component.DuriseoSnackbar
import com.duriseo.android.core.designsystem.theme.Brand100
import com.duriseo.android.core.designsystem.theme.Brand700
import com.duriseo.android.core.designsystem.theme.Danger600
import com.duriseo.android.core.designsystem.theme.DuriseoShadows
import com.duriseo.android.core.designsystem.theme.DuriseoShapes
import com.duriseo.android.core.designsystem.theme.duriseoShadow
import com.duriseo.android.core.designsystem.theme.DuriseoSize
import com.duriseo.android.core.designsystem.theme.DuriseoSpacing
import com.duriseo.android.core.designsystem.theme.DuriseoTheme
import com.duriseo.android.core.designsystem.theme.Ink300
import com.duriseo.android.core.designsystem.theme.Ink500
import com.duriseo.android.core.designsystem.theme.Ink600
import com.duriseo.android.core.designsystem.theme.Ink900
import com.duriseo.android.core.designsystem.theme.Line200
import com.duriseo.android.core.designsystem.theme.Line300
import com.duriseo.android.core.designsystem.theme.PersonMe
import com.duriseo.android.core.designsystem.theme.Surface0
import com.duriseo.android.core.designsystem.theme.Surface100
import com.duriseo.android.domain.profile.ProfileNameRules
import com.duriseo.android.feature.settings.R
import com.duriseo.android.feature.settings.profile.photo.PickedPhoto
import com.duriseo.android.feature.settings.profile.photo.ProfilePhotoFiles

// SCR-SET-04 내 프로필 (FR-AUTH-07, 단위 8). 수치는 보드 `ProfileEdit.dc.html`. 토큰에 없는 값은 이름 붙인 상수로 둔다.

/** 앱바 [저장]: 높이 48, 좌우 14, 16/700 (18~22행) */
private val SaveHeight = 48.dp
private val SavePaddingHorizontal = 14.dp

/** 본문: 위 24·좌우 24, 덩어리 사이 28 (26행) */
private val ContentTop = DuriseoSpacing.Space6
private val ContentHorizontal = DuriseoSpacing.Space6
private val ContentBottom = DuriseoSpacing.Space6
private val BlockGap = 28.dp

/** 아바타 112 + 카메라 버튼 40(오른쪽·아래로 2 나감, 테두리 1 `line-200`), 아바타와 [사진 변경] 사이 10, [사진 변경] 높이 40 (27~41행) */
private val AvatarGap = 10.dp
private val CameraButtonSize = 40.dp
private val CameraButtonOverhang = 2.dp
private val CameraButtonBorder = 1.dp
private val CameraIconSize = 20.dp
private val PhotoChangeHeight = 40.dp
private val PhotoChangePaddingHorizontal = DuriseoSpacing.Space3

/** 이름 칸: 라벨·입력·도움말 사이 8, 입력 높이 52(`size-input`)·좌우 16·모서리 14, 오류는 테두리 1 + 바깥 링 1 (44~52행) */
private val FieldGap = DuriseoSpacing.Space2
private val InputBorder = 1.dp
private val InputErrorBorder = 2.dp

/** 미리보기 패널: 안쪽 16, 모서리 20, 사이 12. 안쪽 카드는 위아래 12·좌우 14, 모서리 14, 사이 12, 글자 사이 1 (54~64행) */
private val PreviewPadding = DuriseoSpacing.Space4
private val PreviewGap = DuriseoSpacing.Space3
private val PreviewCardPaddingVertical = DuriseoSpacing.Space3
private val PreviewCardPaddingHorizontal = 14.dp
private val PreviewTextGap = 1.dp

/** 로그인 계정 줄: 위아래 4·좌우 2 (68행) */
private val AccountPaddingVertical = DuriseoSpacing.Space1
private val AccountPaddingHorizontal = 2.dp

/** 사진 오류 스낵바: 위에서 40 (SCREENS.md "스낵바", D-126). 앱의 공통 스낵바가 띄운다. Preview만 직접 그린다 */
private val PreviewSnackbarTop = 40.dp

@Composable
fun ProfileEditRoute(
    padding: PaddingValues,
    onBack: () -> Unit,
    onShowSnackBar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 앨범: 기기 사진 선택기. 저장소 권한이 필요 없다 (D-149, FR-AUTH-07 AC9)
    val albumLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onAction(ProfileEditAction.PhotoPicked(uri.toString(), fromCamera = false))
    }

    // 카메라: 앱 캐시의 임시 파일 Uri(FileProvider)를 카메라 앱에 건넨다. 카메라 앱이 떠 있는 동안 앱이 정리돼도 이어지게 Uri 문자열을 저장해 둔다
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val uri = cameraUri
        cameraUri = null
        if (taken && uri != null) {
            viewModel.onAction(ProfileEditAction.PhotoPicked(uri, fromCamera = true))
        } else {
            // 찍지 않고 돌아왔다: 비어 있는 임시 파일을 지운다
            ProfilePhotoFiles.clear(context)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileEditSideEffect.Close -> onBack()
                is ProfileEditSideEffect.ShowMessage -> onShowSnackBar(effect.messageRes)
                ProfileEditSideEffect.LaunchAlbum ->
                    albumLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                ProfileEditSideEffect.LaunchCamera -> try {
                    val target = ProfilePhotoFiles.newCameraTarget(context)
                    cameraUri = target.uri.toString()
                    cameraLauncher.launch(target.uri)
                } catch (e: Exception) {
                    // 임시 파일을 못 만들거나 카메라 앱을 열지 못했다 (카메라 앱이 사라진 경우 등): 공통 오류
                    ProfilePhotoFiles.clear(context)
                    onShowSnackBar(DesignR.string.error_common)
                }
            }
        }
    }

    ProfileEditScreen(
        state = state,
        padding = padding,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun ProfileEditScreen(
    state: ProfileEditUiState,
    padding: PaddingValues,
    onAction: (ProfileEditAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DuriseoDialogLayer(
        dialogVisible = state.sheetVisible,
        dialog = {
            DuriseoBottomSheetLayer(onDismissRequest = { onAction(ProfileEditAction.SheetDismissed) }) {
                PhotoSheet(
                    cameraAvailable = state.cameraAvailable,
                    colorKey = state.colorKey,
                    onAlbum = { onAction(ProfileEditAction.AlbumClicked) },
                    onCamera = { onAction(ProfileEditAction.CameraClicked) },
                    onDefaultImage = { onAction(ProfileEditAction.DefaultImageClicked) },
                    onCancel = { onAction(ProfileEditAction.SheetDismissed) },
                )
            }
        },
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
        // [저장] 요청 중에는 화면 전체를 덮는다 (D-115)
        loading = state.saving,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ProfileEditAppBar(canSave = state.canSave, onBack = onBack, onSave = { onAction(ProfileEditAction.SaveClicked) })
            if (state.loaded) {
                // 큰 글자에서도 아래 칸이 잘리지 않게 세로로 스크롤한다
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = ContentHorizontal, end = ContentHorizontal, top = ContentTop, bottom = ContentBottom),
                    verticalArrangement = Arrangement.spacedBy(BlockGap),
                ) {
                    AvatarBlock(state, onAction)
                    NameField(state, onAction)
                    PartnerPreview(state)
                    state.account?.let { AccountLine(it) }
                }
            }
        }
    }
}

/** 앱바: 왼쪽 뒤로, 가운데 "내 프로필" 17/700, 오른쪽 [저장] (바뀐 게 있을 때만 활성: `brand-700`, 비활성 `ink-300`) */
@Composable
private fun ProfileEditAppBar(canSave: Boolean, onBack: () -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DuriseoSize.Appbar)
            .padding(horizontal = DuriseoSpacing.Space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(DuriseoSize.ButtonMd)
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_back),
                contentDescription = stringResource(DesignR.string.nav_back),
                modifier = Modifier.size(DuriseoSize.Icon),
                tint = Ink900,
            )
        }
        Text(
            text = stringResource(R.string.profile_edit_title),
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = Ink900,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .height(SaveHeight)
                .then(
                    if (canSave) {
                        Modifier.clickable(role = Role.Button, onClick = onSave)
                    } else {
                        Modifier.semantics { disabled() }
                    },
                )
                .padding(horizontal = SavePaddingHorizontal),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.profile_edit_save),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = if (canSave) Brand700 else Ink300,
            )
        }
    }
}

/** 이름 입력 옆 아바타: 고른 사진 > 기본 이미지 > 서버의 사진 > 글자 원 순서로 그린다 (FR-AUTH-07 AC4·AC6) */
@Composable
private fun ProfileAvatar(state: ProfileEditUiState, size: DuriseoAvatarSize, modifier: Modifier = Modifier) {
    val letter = stringResource(R.string.profile_letter_me)
    val selection = state.selection
    val preview = (selection as? PhotoSelection.Picked)?.photo?.preview
    when {
        preview != null -> DuriseoAvatarImage(
            label = letter,
            colorKey = state.colorKey,
            image = preview.asImageBitmap(),
            modifier = modifier,
            size = size,
            fallback = PersonMe,
        )
        else -> DuriseoAvatar(
            label = letter,
            colorKey = state.colorKey,
            modifier = modifier,
            size = size,
            fallback = PersonMe,
            // 서버에 있는 사진은 건드리지 않았을 때만. 기본 이미지로 바꿨으면 글자 원이다
            photo = if (selection == PhotoSelection.Keep) state.savedPhoto else null,
        )
    }
}

/** 아바타 112 + 카메라 버튼 + [사진 변경] */
@Composable
private fun AvatarBlock(state: ProfileEditUiState, onAction: (ProfileEditAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AvatarGap),
    ) {
        val selection = state.selection
        val avatarDescription = when {
            selection is PhotoSelection.Picked -> stringResource(R.string.profile_edit_avatar_picked_description)
            selection == PhotoSelection.DefaultImage || state.savedPhoto == null ->
                stringResource(R.string.profile_edit_avatar_default_description)
            else -> null
        }
        Box(modifier = Modifier.size(DuriseoAvatarSize.XLarge.diameter)) {
            Box(
                modifier = if (avatarDescription != null) {
                    Modifier.semantics { contentDescription = avatarDescription; role = Role.Image }
                } else {
                    Modifier
                },
            ) {
                ProfileAvatar(state, DuriseoAvatarSize.XLarge)
            }
            val buttonShape = DuriseoShapes.Full
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = CameraButtonOverhang, y = CameraButtonOverhang)
                    .size(CameraButtonSize)
                    .duriseoShadow(buttonShape, DuriseoShadows.Sm)
                    .background(Surface0, buttonShape)
                    .border(CameraButtonBorder, Line200, buttonShape)
                    .clickable(enabled = !state.saving, role = Role.Button) { onAction(ProfileEditAction.PhotoButtonClicked) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(DesignR.drawable.ic_camera),
                    contentDescription = stringResource(R.string.profile_edit_photo_button_description),
                    modifier = Modifier.size(CameraIconSize),
                    tint = Ink900,
                )
            }
        }
        Box(
            modifier = Modifier
                .height(PhotoChangeHeight)
                .clickable(enabled = !state.saving, role = Role.Button) { onAction(ProfileEditAction.PhotoButtonClicked) }
                .padding(horizontal = PhotoChangePaddingHorizontal),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.profile_edit_photo_change),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Brand700,
            )
        }
    }
}

/** 이름: 라벨 + 글자 수 "N / 10" + 입력 + 도움말(비면 오류 문구). 10자에서 더 입력되지 않는다 (코드 포인트) */
@Composable
private fun NameField(state: ProfileEditUiState, onAction: (ProfileEditAction) -> Unit) {
    val error = state.nameBlank
    val errorText = stringResource(R.string.profile_edit_name_error)
    Column(verticalArrangement = Arrangement.spacedBy(FieldGap)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.profile_edit_name_label),
                style = MaterialTheme.typography.labelLarge,
                color = Ink600,
            )
            Text(
                text = stringResource(R.string.profile_edit_name_count, state.nameLength, ProfileNameRules.MAX),
                style = MaterialTheme.typography.labelMedium,
                color = Ink500,
            )
        }
        val shape = MaterialTheme.shapes.medium
        BasicTextField(
            value = state.name,
            onValueChange = { onAction(ProfileEditAction.NameChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DuriseoSize.Input)
                .background(Surface0, shape)
                .border(
                    width = if (error) InputErrorBorder else InputBorder,
                    color = if (error) Danger600 else Line300,
                    shape = shape,
                )
                .semantics { if (error) semanticsError(errorText) },
            enabled = !state.saving,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink900),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            cursorBrush = SolidColor(Ink900),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .heightIn(min = DuriseoSize.Input)
                        .padding(horizontal = DuriseoSpacing.Space4),
                    contentAlignment = Alignment.CenterStart,
                ) { inner() }
            },
        )
        Text(
            text = if (error) errorText else stringResource(R.string.profile_edit_name_help),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
            color = if (error) Danger600 else Ink600,
        )
    }
}

/** "상대에게 이렇게 보여요" 패널: 작은 아바타(36) + 이름(없으면 "이름 없음") + "일정 · 메모 작성자로 표시돼요" */
@Composable
private fun PartnerPreview(state: ProfileEditUiState) {
    val description = stringResource(R.string.profile_edit_preview_description)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface100, DuriseoShapes.Xl)
            .padding(PreviewPadding)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(PreviewGap),
    ) {
        Text(
            text = stringResource(R.string.profile_edit_preview_title),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Ink600,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface0, MaterialTheme.shapes.medium)
                .padding(horizontal = PreviewCardPaddingHorizontal, vertical = PreviewCardPaddingVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3),
        ) {
            ProfileAvatar(state, DuriseoAvatarSize.Compact)
            Column(verticalArrangement = Arrangement.spacedBy(PreviewTextGap)) {
                Text(
                    text = state.previewName.ifEmpty { stringResource(R.string.profile_edit_preview_empty_name) },
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.profile_edit_preview_caption),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                    color = Ink600,
                )
            }
        }
    }
}

/** 로그인 계정 줄 (FR-AUTH-07 AC8, D-148): 이메일 없이 로그인 수단만 */
@Composable
private fun AccountLine(line: ProfileAccountLine) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AccountPaddingVertical, horizontal = AccountPaddingHorizontal),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.profile_edit_account_label),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
            color = Ink600,
        )
        Text(
            text = when (line) {
                ProfileAccountLine.Guest -> stringResource(R.string.profile_edit_account_guest)
                ProfileAccountLine.Google -> stringResource(R.string.settings_account_google)
                ProfileAccountLine.Kakao -> stringResource(R.string.settings_account_kakao)
                ProfileAccountLine.GoogleAndKakao -> stringResource(R.string.profile_edit_account_both)
            },
            style = MaterialTheme.typography.labelLarge,
            color = Ink900,
        )
    }
}

// ---- Preview ----
// 견본 이름·색은 보드 `ProfileEdit.dc.html`의 샘플이다. 고른 사진은 Coil 없이 단색 비트맵 하나로 그린다.

private fun previewState(
    name: String = "민지",
    savedName: String = "민지",
    selection: PhotoSelection = PhotoSelection.Keep,
    sheetVisible: Boolean = false,
    saving: Boolean = false,
    cameraAvailable: Boolean = true,
) = ProfileEditUiState(
    loaded = true,
    name = name,
    savedName = savedName,
    colorKey = "pick-orange",
    selection = selection,
    account = ProfileAccountLine.Google,
    sheetVisible = sheetVisible,
    cameraAvailable = cameraAvailable,
    saving = saving,
)

private fun previewPickedPhoto(): PickedPhoto {
    val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Brand100.toArgb())
    return PickedPhoto(bytes = ByteArray(0), mimeType = "image/jpeg", preview = bitmap)
}

@Composable
private fun PreviewScreen(state: ProfileEditUiState, snackbar: Int? = null) {
    DuriseoTheme {
        Box(Modifier.fillMaxSize()) {
            ProfileEditScreen(state = state, padding = PaddingValues(), onAction = {}, onBack = {})
            if (snackbar != null) {
                DuriseoSnackbar(
                    message = stringResource(snackbar),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = PreviewSnackbarTop),
                )
            }
        }
    }
}

/** 기본: 사진 없음(사람 색 + "나"), [저장] 비활성 (`SCR-SET-04.png`) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfileEditPreview_Default() {
    PreviewScreen(previewState())
}

/** 사진 고름: 아바타와 미리보기에 고른 사진, [저장] 활성 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfileEditPreview_PhotoPicked() {
    PreviewScreen(previewState(selection = PhotoSelection.Picked(previewPickedPhoto())))
}

/** 사진 시트 (`SCR-SET-04--photo-sheet.png`) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfileEditPreview_PhotoSheet() {
    PreviewScreen(previewState(sheetVisible = true))
}

/** 사진 오류: 위에서 40 스낵바, 아바타는 원래 모습 (`SCR-SET-04--photo-error.png`) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfileEditPreview_PhotoError() {
    PreviewScreen(previewState(), snackbar = R.string.profile_photo_error_too_large)
}

/** 이름 빈 값: 오류 문구, 미리보기 "이름 없음", [저장] 비활성 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfileEditPreview_EmptyName() {
    PreviewScreen(previewState(name = ""))
}

/** 저장 중: 로딩 덮개 (D-115) */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfileEditPreview_Saving() {
    PreviewScreen(previewState(name = "지우", saving = true))
}
