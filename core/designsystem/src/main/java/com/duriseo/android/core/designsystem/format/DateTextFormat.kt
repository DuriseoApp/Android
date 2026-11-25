package com.duriseo.android.core.designsystem.format

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.domain.format.WrittenAt

// 날짜·시각 표기의 문자열부 (NFR-TIME-02, D-129). 계산은 domain의 `DateTexts`가 하고 여기는 strings.xml 조각만 붙인다.

/** 작성일 문구: "오늘 오전 9:12", "어제", "10월 8일", "2025년 10월 8일" */
fun Resources.writtenAtText(writtenAt: WrittenAt): String = when (writtenAt) {
    is WrittenAt.Today -> getString(
        R.string.written_at_today,
        getString(if (writtenAt.isPm) R.string.date_meridiem_pm else R.string.date_meridiem_am),
        writtenAt.hour12,
        writtenAt.minute,
    )

    WrittenAt.Yesterday -> getString(R.string.written_at_yesterday)
    is WrittenAt.ThisYear -> getString(R.string.written_at_this_year, writtenAt.month, writtenAt.day)
    is WrittenAt.OtherYear -> getString(R.string.date_year_month_day, writtenAt.year, writtenAt.month, writtenAt.day)
}

@Composable
fun writtenAtText(writtenAt: WrittenAt): String = LocalContext.current.resources.writtenAtText(writtenAt)
