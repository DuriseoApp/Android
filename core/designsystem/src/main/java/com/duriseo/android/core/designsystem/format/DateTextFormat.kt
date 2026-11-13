package com.duriseo.android.core.designsystem.format

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.duriseo.android.core.designsystem.R
import com.duriseo.android.domain.format.MemoDateChip
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

/** 메모 날짜 칩 문구: "10월 17일 (토)", 올해가 아니면 "2027년 10월 17일 (일)" */
fun Resources.memoDateChipText(chip: MemoDateChip): String {
    // 요일은 일요일이 0번인 배열이다 (DayOfWeek.value는 월요일 1 ~ 일요일 7)
    val weekday = getStringArray(R.array.weekday_short)[chip.dayOfWeek.value % 7]
    val year = chip.year
    return if (year == null) {
        getString(R.string.memo_date_chip, chip.month, chip.day, weekday)
    } else {
        getString(R.string.memo_date_chip_other_year, year, chip.month, chip.day, weekday)
    }
}

@Composable
fun writtenAtText(writtenAt: WrittenAt): String = LocalContext.current.resources.writtenAtText(writtenAt)

@Composable
fun memoDateChipText(chip: MemoDateChip): String = LocalContext.current.resources.memoDateChipText(chip)
