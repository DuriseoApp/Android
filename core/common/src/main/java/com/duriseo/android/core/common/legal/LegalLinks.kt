package com.duriseo.android.core.common.legal

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * 이용약관·개인정보처리방침 웹 주소 (PRD D-154, 2026-11-20 게시). 주소는 이 한 곳에만 둔다.
 * SCR-START-01의 두 링크는 각 주소를, SCR-SET-01 "이용약관·개인정보처리방침" 한 행은 개인정보처리방침 주소를 연다.
 */
object LegalLinks {
    const val TERMS_URL: String = "https://juco-io.notion.site/41e5430a43d180f594d3c09751a82ebc"
    const val PRIVACY_URL: String = "https://juco-io.notion.site/41e5430a43d18054aba8fcbd4cf6c515"

    /** SCR-SET-01 "이용약관·개인정보처리방침" 행이 여는 주소: 개인정보처리방침 (D-154) */
    const val SETTINGS_ROW_URL: String = PRIVACY_URL
}

/** 외부 브라우저로 주소를 연다. 열 수 있는 앱이 없으면 조용히 무시한다 */
fun Context.openExternalUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        // 웹 브라우저가 없는 기기: 아무 일도 하지 않는다
    }
}
