package com.duriseo.android.core.designsystem.component

import androidx.annotation.DrawableRes
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.domain.appops.PopupPreset

/** 팝업 기본 이미지 4종의 VectorDrawable (D-181, `design/illustrations/popup-*.svg`). 화면에서 112dp 그대로 쓴다 */
@DrawableRes
fun popupPresetDrawable(preset: PopupPreset): Int = when (preset) {
    PopupPreset.MAINTENANCE -> R.drawable.popup_maintenance
    PopupPreset.ERROR -> R.drawable.popup_error
    PopupPreset.UPDATE -> R.drawable.popup_update
    PopupPreset.NOTICE -> R.drawable.popup_notice
}
