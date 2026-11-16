package com.duriseo.android.feature.settings.profile

import app.cash.turbine.test
import com.duriseo.android.core.designsystem.R as DesignR
import com.duriseo.android.domain.error.DataError
import com.duriseo.android.domain.me.SocialProvider
import com.duriseo.android.domain.profile.PhotoError
import com.duriseo.android.domain.profile.ProfileImage
import com.duriseo.android.domain.result.DataResult
import com.duriseo.android.feature.settings.FakeMeRepository
import com.duriseo.android.feature.settings.FakeProfileRepository
import com.duriseo.android.feature.settings.GuestId
import com.duriseo.android.feature.settings.R
import com.duriseo.android.feature.settings.apiError
import com.duriseo.android.feature.settings.guestMe
import com.duriseo.android.feature.settings.memberMe
import com.duriseo.android.feature.settings.profile.photo.CameraAvailability
import com.duriseo.android.feature.settings.profile.photo.PickedPhoto
import com.duriseo.android.feature.settings.profile.photo.PickedPhotoReader
import com.duriseo.android.feature.settings.profile.photo.PickedPhotoResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 고른 사진을 읽는 가짜. [result]를 그대로 돌려주고 받은 Uri를 [reads]에 남긴다 */
class FakePickedPhotoReader : PickedPhotoReader {
    var result: PickedPhotoResult = PickedPhotoResult.Ready(fakePhoto())
    val reads = mutableListOf<Pair<String, Boolean>>()

    /** 있으면 이 값이 완료될 때까지 읽기가 끝나지 않는다 (사진 읽는 중 상태를 만든다) */
    var gate: kotlinx.coroutines.CompletableDeferred<Unit>? = null

    override suspend fun read(uri: String, fromCamera: Boolean): PickedPhotoResult {
        reads += uri to fromCamera
        gate?.await()
        return result
    }
}

fun fakePhoto(size: Int = 12, mime: String = "image/jpeg") = PickedPhoto(ByteArray(size), mime, preview = null)

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditViewModelTest {

    private val me = FakeMeRepository(memberMe())
    private val profile = FakeProfileRepository(me)
    private val reader = FakePickedPhotoReader()
    private var cameraAvailable = true

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(): ProfileEditViewModel {
        val vm = ProfileEditViewModel(me, profile, reader, CameraAvailability { cameraAvailable })
        // stateIn(WhileSubscribed)라서 값을 읽는 동안 구독해 둔다
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        return vm
    }

    private fun ProfileEditViewModel.pickPhoto() = onAction(ProfileEditAction.PhotoPicked("content://photo/1", fromCamera = false))

    // ---- 저장 (D-146) ----

    @Test
    fun `FR-AUTH-07 AC6 이름과 사진이 바뀌면 이름을 먼저 저장하고 사진을 올린다`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(ProfileEditAction.NameChanged("  새 이름 "))
            vm.pickPhoto()
            assertTrue(vm.uiState.value.canSave)

            vm.onAction(ProfileEditAction.SaveClicked)

            // 앞뒤 공백을 지운 이름 -> 사진 순서, 모두 되면 화면을 닫는다
            assertEquals(listOf("name:새 이름", "upload:image/jpeg:12"), profile.calls)
            assertEquals(ProfileEditSideEffect.Close, awaitItem())
            assertFalse(vm.uiState.value.saving)
        }
    }

    @Test
    fun `FR-AUTH-07 AC6 고른 사진은 저장하기 전에는 올리지 않고 기기에서 미리보기만 한다`() = runTest {
        val vm = viewModel()

        vm.pickPhoto()

        assertTrue(vm.uiState.value.selection is PhotoSelection.Picked)
        assertTrue(vm.uiState.value.canSave)
        assertEquals(emptyList<String>(), profile.calls)
        assertEquals(listOf("content://photo/1" to false), reader.reads)
    }

    @Test
    fun `FR-AUTH-07 AC6 이름 저장이 실패하면 사진을 올리지 않고 화면에 머문다`() = runTest {
        profile.nameResults += apiError("INTERNAL_ERROR")
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(ProfileEditAction.NameChanged("새 이름"))
            vm.pickPhoto()

            vm.onAction(ProfileEditAction.SaveClicked)

            assertEquals(listOf("name:새 이름"), profile.calls)
            assertEquals(ProfileEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            expectNoEvents()
            // 고른 사진은 그대로라 다시 누를 수 있다
            assertTrue(vm.uiState.value.selection is PhotoSelection.Picked)
            assertFalse(vm.uiState.value.saving)
        }
    }

    @Test
    fun `FR-AUTH-07 AC6 사진만 실패한 뒤 다시 누르면 사진만 다시 올린다`() = runTest {
        profile.uploadResults += apiError("INTERNAL_ERROR")
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(ProfileEditAction.NameChanged("새 이름"))
            vm.pickPhoto()
            vm.onAction(ProfileEditAction.SaveClicked)
            assertEquals(ProfileEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            // 이름은 됐으니 Room의 값이 새 이름이고, 이름은 더 이상 바뀐 것이 아니다
            assertFalse(vm.uiState.value.nameChanged)
            assertTrue(vm.uiState.value.canSave)

            vm.onAction(ProfileEditAction.SaveClicked)

            assertEquals(listOf("name:새 이름", "upload:image/jpeg:12", "upload:image/jpeg:12"), profile.calls)
            assertEquals(ProfileEditSideEffect.Close, awaitItem())
        }
    }

    @Test
    fun `FR-AUTH-07 AC6 기본 이미지로 바꾸면 사진을 지운다`() = runTest {
        me.me.value = memberMe().copy(profileImageVersion = "v1")
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(ProfileEditAction.PhotoButtonClicked)
            vm.onAction(ProfileEditAction.DefaultImageClicked)
            assertFalse(vm.uiState.value.sheetVisible)
            assertEquals(PhotoChange.Delete, vm.uiState.value.photoChange)

            vm.onAction(ProfileEditAction.SaveClicked)

            assertEquals(listOf("delete"), profile.calls)
            assertEquals(ProfileEditSideEffect.Close, awaitItem())
        }
    }

    @Test
    fun `FR-AUTH-07 AC6 기본 이미지로 바꿨는데 원래 사진이 없으면 바뀐 것이 아니다`() = runTest {
        val vm = viewModel()

        vm.onAction(ProfileEditAction.DefaultImageClicked)

        assertEquals(PhotoChange.None, vm.uiState.value.photoChange)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `NFR-OFF-02 오프라인이면 저장하지 않고 인터넷 안내를 보인다`() = runTest {
        profile.nameResults += DataResult.Failure(DataError.Offline)
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(ProfileEditAction.NameChanged("새 이름"))

            vm.onAction(ProfileEditAction.SaveClicked)

            assertEquals(ProfileEditSideEffect.ShowMessage(DesignR.string.offline_required), awaitItem())
            assertFalse(vm.uiState.value.saving)
        }
    }

    @Test
    fun `D-115 저장하는 동안 덮개가 보이고 끝나면 항상 걷힌다`() = runTest {
        val gate = CompletableDeferred<Unit>()
        profile.nameGate = gate
        val vm = viewModel()
        vm.onAction(ProfileEditAction.NameChanged("새 이름"))

        vm.onAction(ProfileEditAction.SaveClicked)

        assertTrue(vm.uiState.value.saving)
        assertFalse(vm.uiState.value.canSave)
        // 요청 중에 또 눌러도 한 번만 보낸다
        vm.onAction(ProfileEditAction.SaveClicked)
        assertEquals(1, profile.calls.size)

        gate.complete(Unit)
        assertFalse(vm.uiState.value.saving)
    }

    @Test
    fun `D-115 저장소가 예외를 던져도 덮개를 걷고 공통 오류를 보인다`() = runTest {
        profile.uploadThrows = true
        val vm = viewModel()
        vm.effects.test {
            vm.pickPhoto()

            vm.onAction(ProfileEditAction.SaveClicked)

            assertEquals(ProfileEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertFalse(vm.uiState.value.saving)
        }
    }

    // ---- 이름 ----

    @Test
    fun `FR-AUTH-07 AC6 이름은 코드 포인트 10개에서 더 입력되지 않는다`() = runTest {
        val vm = viewModel()

        // 이모지는 UTF-16으로 두 칸이지만 한 글자로 센다
        vm.onAction(ProfileEditAction.NameChanged("😀".repeat(12)))

        assertEquals(10, vm.uiState.value.nameLength)
        assertEquals("😀".repeat(10), vm.uiState.value.name)
    }

    @Test
    fun `FR-AUTH-07 AC6 공백뿐인 이름이면 저장할 수 없고 오류를 보인다`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(ProfileEditAction.NameChanged("    "))
            vm.pickPhoto()

            assertTrue(vm.uiState.value.nameBlank)
            assertFalse(vm.uiState.value.canSave)
            assertEquals("", vm.uiState.value.previewName)
            vm.onAction(ProfileEditAction.SaveClicked)

            assertEquals(emptyList<String>(), profile.calls)
            expectNoEvents()
        }
    }

    @Test
    fun `SCR-SET-04 바뀐 것이 없으면 저장이 비활성이다`() = runTest {
        val vm = viewModel()
        assertFalse(vm.uiState.value.canSave)

        // 공백만 다른 이름은 바뀐 것이 아니다
        vm.onAction(ProfileEditAction.NameChanged("  닉네임 "))
        assertFalse(vm.uiState.value.canSave)

        vm.onAction(ProfileEditAction.NameChanged("다른 이름"))
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `SCR-SET-04 내 정보를 읽기 전에는 저장할 수 없다`() = runTest {
        me.me.value = null
        val vm = viewModel()

        assertFalse(vm.uiState.value.loaded)
        assertFalse(vm.uiState.value.canSave)
    }

    // ---- 로그인 계정 줄 ----

    @Test
    fun `FR-AUTH-07 AC8 로그인 계정 줄은 로그인 수단대로 보인다`() = runTest {
        val vm = viewModel()
        assertEquals(ProfileAccountLine.Google, vm.uiState.value.account)

        me.me.value = memberMe(SocialProvider.KAKAO)
        assertEquals(ProfileAccountLine.Kakao, vm.uiState.value.account)

        me.me.value = memberMe().copy(providers = listOf(SocialProvider.GOOGLE, SocialProvider.KAKAO))
        assertEquals(ProfileAccountLine.GoogleAndKakao, vm.uiState.value.account)

        me.me.value = guestMe()
        assertEquals(ProfileAccountLine.Guest, vm.uiState.value.account)
    }

    // ---- 사진 오류 (D-147) ----

    @Test
    fun `FR-AUTH-07 AC7 10MB를 넘는 사진은 고르지 않고 크기 오류를 보인다`() = runTest {
        reader.result = PickedPhotoResult.Rejected(PhotoError.TOO_LARGE)
        val vm = viewModel()
        vm.effects.test {
            vm.pickPhoto()

            assertEquals(ProfileEditSideEffect.ShowMessage(R.string.profile_photo_error_too_large), awaitItem())
            // 아바타는 원래 모습 그대로
            assertEquals(PhotoSelection.Keep, vm.uiState.value.selection)
            assertFalse(vm.uiState.value.canSave)
        }
    }

    @Test
    fun `FR-AUTH-07 AC7 JPEG PNG가 아닌 사진은 형식 오류를 보인다`() = runTest {
        reader.result = PickedPhotoResult.Rejected(PhotoError.TYPE_NOT_ALLOWED)
        val vm = viewModel()
        vm.effects.test {
            vm.pickPhoto()

            assertEquals(ProfileEditSideEffect.ShowMessage(R.string.profile_photo_error_type), awaitItem())
            assertEquals(PhotoSelection.Keep, vm.uiState.value.selection)
        }
    }

    @Test
    fun `FR-AUTH-07 AC7 서버 415는 형식 문구 413은 공통 오류로 보이고 고른 사진을 버린다`() = runTest {
        profile.uploadResults += apiError("IMAGE_TOO_LARGE")
        profile.uploadResults += apiError("IMAGE_TYPE_NOT_ALLOWED")
        val vm = viewModel()
        vm.effects.test {
            vm.pickPhoto()
            vm.onAction(ProfileEditAction.SaveClicked)
            // 10MB는 고를 때 이미 막았으니 서버 413은 25MP 초과 등이다: "10MB" 문구 대신 공통 오류 (API.md 5장, D-153)
            assertEquals(ProfileEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(PhotoSelection.Keep, vm.uiState.value.selection)

            vm.pickPhoto()
            vm.onAction(ProfileEditAction.SaveClicked)
            assertEquals(ProfileEditSideEffect.ShowMessage(R.string.profile_photo_error_type), awaitItem())
        }
    }

    @Test
    fun `FR-AUTH-07 AC7 사진을 읽지 못하면 공통 오류를 보인다`() = runTest {
        reader.result = PickedPhotoResult.Failed
        val vm = viewModel()
        vm.effects.test {
            vm.pickPhoto()

            assertEquals(ProfileEditSideEffect.ShowMessage(DesignR.string.error_common), awaitItem())
            assertEquals(PhotoSelection.Keep, vm.uiState.value.selection)
        }
    }

    @Test
    fun `FR-AUTH-07 AC6 사진을 읽는 중에는 저장하지 않고 읽기가 끝나면 고른 사진이 남는다`() = runTest {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        reader.gate = gate
        val vm = viewModel()
        vm.onAction(ProfileEditAction.NameChanged("새 이름"))

        vm.pickPhoto()
        vm.onAction(ProfileEditAction.SaveClicked)
        assertFalse(vm.uiState.value.saving)
        assertTrue(profile.calls.isEmpty())

        gate.complete(Unit)
        assertTrue(vm.uiState.value.selection is PhotoSelection.Picked)
    }

    // ---- 사진 시트 ----

    @Test
    fun `FR-AUTH-07 AC9 카메라 버튼과 사진 변경은 사진 시트를 연다`() = runTest {
        val vm = viewModel()

        vm.onAction(ProfileEditAction.PhotoButtonClicked)
        assertTrue(vm.uiState.value.sheetVisible)

        vm.onAction(ProfileEditAction.SheetDismissed)
        assertFalse(vm.uiState.value.sheetVisible)
    }

    @Test
    fun `FR-AUTH-07 AC9 앨범과 사진 찍기는 시트를 닫고 선택기를 연다`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(ProfileEditAction.PhotoButtonClicked)
            vm.onAction(ProfileEditAction.AlbumClicked)
            assertEquals(ProfileEditSideEffect.LaunchAlbum, awaitItem())
            assertFalse(vm.uiState.value.sheetVisible)

            vm.onAction(ProfileEditAction.PhotoButtonClicked)
            vm.onAction(ProfileEditAction.CameraClicked)
            assertEquals(ProfileEditSideEffect.LaunchCamera, awaitItem())
        }
    }

    @Test
    fun `FR-AUTH-07 AC9 카메라 앱이 없으면 사진 찍기는 열지 않는다`() = runTest {
        cameraAvailable = false
        val vm = viewModel()
        vm.effects.test {
            vm.onAction(ProfileEditAction.CameraClicked)

            expectNoEvents()
            assertFalse(vm.uiState.value.cameraAvailable)
        }
    }

    @Test
    fun `FR-AUTH-07 AC6 저장한 사진의 모델은 내 정보의 사진 버전으로 만든다`() = runTest {
        me.me.value = memberMe().copy(profileImageVersion = "v9")
        val vm = viewModel()

        assertEquals(ProfileImage(GuestId, "v9"), vm.uiState.value.savedPhoto)
        me.me.value = memberMe()
        assertNull(vm.uiState.value.savedPhoto)
    }
}
