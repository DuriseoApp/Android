package com.duriseo.android.feature.settings.profile

import androidx.annotation.StringRes
import com.duriseo.android.domain.me.AccountType
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.profile.PhotoError
import com.duriseo.android.domain.profile.ProfileImage
import com.duriseo.android.domain.profile.ProfileNameRules
import com.duriseo.android.domain.profile.profileImageOf
import com.duriseo.android.feature.settings.R
import com.duriseo.android.feature.settings.profile.photo.PickedPhoto

/** 로그인 계정 줄의 값 (FR-AUTH-07 AC8, D-148). 이메일은 보여 주지 않는다 (NFR-PRIV-01) */
enum class ProfileAccountLine {
    /** "게스트" */
    Guest,

    /** "Google 로그인" */
    Google,

    /** "카카오 로그인" */
    Kakao,

    /** 로그인 수단이 둘: "Google · 카카오" */
    GoogleAndKakao,
}

/** 아바타에 보이는 사진의 선택 (저장 전에는 기기에서 고르기만 한다, D-146) */
sealed interface PhotoSelection {
    /** 건드리지 않음: 서버에 있는 사진(없으면 글자 아바타)을 그대로 보인다 */
    data object Keep : PhotoSelection

    /** 앨범·카메라에서 고른 사진. 저장 전이라 기기에서 미리보기만 한다 */
    data class Picked(val photo: PickedPhoto) : PhotoSelection

    /** [기본 이미지로 변경]을 골랐다: 저장하면 서버의 사진을 지운다 */
    data object DefaultImage : PhotoSelection
}

/** [저장]이 서버에 보낼 사진 일 (D-146) */
sealed interface PhotoChange {
    data object None : PhotoChange
    data class Upload(val photo: PickedPhoto) : PhotoChange
    data object Delete : PhotoChange
}

/**
 * SCR-SET-04 내 프로필의 상태 (FR-AUTH-07). 이름·사진의 "원래 값"은 Room의 내 정보다. 저장이 중간에 실패해도
 * 된 것은 Room이 바뀌어 원래 값이 되므로, 다시 누르면 남은 것만 저장된다 (D-146).
 *
 * @property loaded 내 정보를 읽었다. 읽기 전에는 앱바만 그린다
 * @property name 입력 중인 이름 (코드 포인트 10개까지, 공백 포함)
 * @property savedName Room의 내 이름
 * @property colorKey 내 색 팔레트 키 (아바타 색)
 * @property savedPhoto Room의 내 사진 모델. 사진이 없으면 null
 * @property selection 이번에 고른 사진
 * @property account 로그인 계정 줄. 알 수 없으면 null이라 줄을 그리지 않는다
 * @property sheetVisible 사진 시트가 열려 있다
 * @property cameraAvailable 카메라 앱이 있다. 없으면 시트에서 [사진 찍기]를 숨긴다
 * @property saving [저장] 요청 중. 로딩 덮개를 그린다 (D-115)
 */
data class ProfileEditUiState(
    val loaded: Boolean = false,
    val name: String = "",
    val savedName: String = "",
    val colorKey: String = "",
    val savedPhoto: ProfileImage? = null,
    val selection: PhotoSelection = PhotoSelection.Keep,
    val account: ProfileAccountLine? = null,
    val sheetVisible: Boolean = false,
    val cameraAvailable: Boolean = true,
    val saving: Boolean = false,
) {
    /** 글자 수 "N / 10"의 N */
    val nameLength: Int get() = ProfileNameRules.length(name)

    /** 앞뒤 공백을 지운 뒤 비면 오류 문구를 보이고 저장할 수 없다 */
    val nameBlank: Boolean get() = ProfileNameRules.isBlank(name)

    /** 공백만 다른 이름은 바뀐 것이 아니다 */
    val nameChanged: Boolean
        get() = ProfileNameRules.normalize(name) != ProfileNameRules.normalize(savedName)

    /** 기본 이미지로 바꿨는데 원래 사진이 없었으면 바뀐 것이 아니다 */
    val photoChange: PhotoChange
        get() = when (val current = selection) {
            PhotoSelection.Keep -> PhotoChange.None
            is PhotoSelection.Picked -> PhotoChange.Upload(current.photo)
            PhotoSelection.DefaultImage -> if (savedPhoto != null) PhotoChange.Delete else PhotoChange.None
        }

    /** [저장]은 바뀐 게 있고 이름이 비지 않았을 때만 활성이다 */
    val canSave: Boolean
        get() = loaded && !saving && !nameBlank && (nameChanged || photoChange != PhotoChange.None)

    /** "상대에게 이렇게 보여요"의 이름. 비면 [R.string.profile_edit_preview_empty_name]을 쓴다 (보드 `previewName`) */
    val previewName: String get() = ProfileNameRules.normalize(name)
}

sealed interface ProfileEditAction {
    data class NameChanged(val text: String) : ProfileEditAction

    /** 카메라 버튼·[사진 변경] */
    data object PhotoButtonClicked : ProfileEditAction
    data object SheetDismissed : ProfileEditAction
    data object AlbumClicked : ProfileEditAction
    data object CameraClicked : ProfileEditAction
    data object DefaultImageClicked : ProfileEditAction

    /** 사진 선택기·카메라 앱이 사진을 건넸다. [uri]는 `Uri.toString()` */
    data class PhotoPicked(val uri: String, val fromCamera: Boolean) : ProfileEditAction
    data object SaveClicked : ProfileEditAction
}

sealed interface ProfileEditSideEffect {
    /** 공통 스낵바 (위에서 40, D-126) */
    data class ShowMessage(@StringRes val messageRes: Int) : ProfileEditSideEffect

    /** 이름·사진이 모두 저장됐다 -> 전체 메뉴로 돌아간다 (보드 [저장] -> Menu) */
    data object Close : ProfileEditSideEffect

    /** 기기 사진 선택기를 연다 (`PickVisualMedia(ImageOnly)`) */
    data object LaunchAlbum : ProfileEditSideEffect

    /** 카메라 앱을 연다 (`TakePicture` + FileProvider) */
    data object LaunchCamera : ProfileEditSideEffect
}

/** 사진 오류 문구 (D-147) */
@StringRes
internal fun PhotoError.toMessageRes(): Int = when (this) {
    PhotoError.TOO_LARGE -> R.string.profile_photo_error_too_large
    PhotoError.TYPE_NOT_ALLOWED -> R.string.profile_photo_error_type
}

internal fun Me.toAccountLine(): ProfileAccountLine? = when (accountType) {
    AccountType.GUEST -> ProfileAccountLine.Guest
    AccountType.MEMBER -> when {
        SocialProvider.GOOGLE in providers && SocialProvider.KAKAO in providers -> ProfileAccountLine.GoogleAndKakao
        SocialProvider.GOOGLE in providers -> ProfileAccountLine.Google
        SocialProvider.KAKAO in providers -> ProfileAccountLine.Kakao
        else -> null
    }
}

internal fun Me.savedPhoto(): ProfileImage? = profileImageOf(id, profileImageVersion)
