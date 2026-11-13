package com.duriseo.android.feature.calendar.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * SCR-CAL-03 [메모] 탭 슬롯 (D-133, unit-6.md 확인 필요 3). 메모 입력은 `feature:memo`에 있고 feature끼리는 서로 모르므로,
 * `feature:main`이 `MemoEditViewModel`·`MemoForm`·`MemoDateSheet`로 이 값을 채워 일정 작성 화면에 넣는다.
 * 일정 작성 화면은 이 값으로 [메모] 탭을 그리고, 앱바 [저장]·로딩 덮개·날짜 시트를 현재 탭 기준으로 정한다.
 *
 * @property saveEnabled 메모 탭의 [저장] 활성 (제목·본문이 공백 제거 뒤 모두 비면 false, D-132)
 * @property loading 메모 저장 요청 중: 로딩 덮개 (D-115)
 * @property dateSheetVisible 메모 날짜 시트가 열려 있다
 * @property onSave 메모 탭에서 [저장]을 눌렀다. 성공하면 메모 쪽이 작성 화면을 닫는다
 * @property form 제목·날짜 영역·본문·알림 스위치. 스크롤과 좌우 여백은 일정 작성 화면이 준다
 * @property dateSheet 메모 날짜 시트. 화면 전체 덮개라 [dateSheetVisible]일 때 일정 작성 화면의 `DuriseoDialogLayer`에 둔다
 */
class EventEditMemoTab(
    val saveEnabled: Boolean,
    val loading: Boolean,
    val dateSheetVisible: Boolean,
    val onSave: () -> Unit,
    val form: @Composable (Modifier) -> Unit,
    val dateSheet: @Composable () -> Unit,
)

/**
 * [EventEditMemoTab]을 만드는 Composable. 새 일정 작성 화면의 내비게이션 항목 안에서 불러서, 메모 ViewModel이 일정 ViewModel과
 * 같은 항목에 살게 한다(탭마다 입력 보존). [onClose]는 메모를 저장해 화면을 닫을 때, [onShowSnackBar]는 메모 쪽 안내 문구용이다.
 */
typealias EventEditMemoTabProvider = @Composable (onClose: () -> Unit, onShowSnackBar: (Int) -> Unit) -> EventEditMemoTab
