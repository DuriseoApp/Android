package com.duriseo.android.core.designsystem.component

import com.duriseo.android.core.designsystem.R
import com.duriseo.android.domain.appops.PopupPreset
import org.junit.Assert.assertEquals
import org.junit.Test

class PopupPresetDrawableTest {
    @Test
    fun `D-181 기본 이미지 키 4개는 각자의 VectorDrawable이다`() {
        assertEquals(R.drawable.popup_maintenance, popupPresetDrawable(PopupPreset.MAINTENANCE))
        assertEquals(R.drawable.popup_error, popupPresetDrawable(PopupPreset.ERROR))
        assertEquals(R.drawable.popup_update, popupPresetDrawable(PopupPreset.UPDATE))
        assertEquals(R.drawable.popup_notice, popupPresetDrawable(PopupPreset.NOTICE))
        assertEquals(4, PopupPreset.entries.size)
        assertEquals(setOf("maintenance", "error", "update", "notice"), PopupPreset.entries.map { it.key }.toSet())
    }
}
