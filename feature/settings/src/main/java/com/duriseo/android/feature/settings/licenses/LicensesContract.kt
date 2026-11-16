package com.duriseo.android.feature.settings.licenses

/** 라이선스 한 건. [body]는 펼쳤을 때 보이는 글이다 (라이선스 전문, 없으면 이름·주소) */
data class LicenseEntry(
    val id: String,
    val name: String,
    val licenseName: String,
    val body: String,
)

/** SCR-SET-05 목록의 한 행. [expanded]면 그 자리에서 [LicenseEntry.body]가 펼쳐진다 */
data class LicenseRow(
    val id: String,
    val name: String,
    val licenseName: String,
    val body: String,
    val expanded: Boolean = false,
)

/**
 * SCR-SET-05 오픈소스 라이선스 (FR-SET-02 AC3, D-152). 행은 이름순 라이브러리 뒤에 글꼴 두 행이 온다.
 * 펼친 행은 하나만이다 (보드 `openRow`).
 */
data class LicensesUiState(
    val rows: List<LicenseRow> = emptyList(),
    val expandedId: String? = null,
) {
    val displayRows: List<LicenseRow> get() = rows.map { it.copy(expanded = it.id == expandedId) }
}

sealed interface LicensesAction {
    /** 행을 누름: 접혀 있으면 펼치고(다른 행은 접힘), 펼쳐 있으면 접는다 */
    data class RowClicked(val id: String) : LicensesAction
}
