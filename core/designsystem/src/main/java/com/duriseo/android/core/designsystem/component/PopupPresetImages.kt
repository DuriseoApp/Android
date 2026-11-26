package com.duriseo.android.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.dp
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.domain.appops.PopupPreset

/** 기본 이미지(`preset:`) 크기 112×112 (보드 값, 화면에서 이 크기로 그린다) */
val PopupPresetImageSize = 112.dp

/** 팝업 기본 이미지 4종의 VectorDrawable (D-181, `design/illustrations/popup-*.svg`). 화면에서 112dp 그대로 쓴다 */
@DrawableRes
fun popupPresetDrawable(preset: PopupPreset): Int = when (preset) {
    PopupPreset.MAINTENANCE -> R.drawable.popup_maintenance
    PopupPreset.ERROR -> R.drawable.popup_error
    PopupPreset.UPDATE -> R.drawable.popup_update
    PopupPreset.NOTICE -> R.drawable.popup_notice
}
