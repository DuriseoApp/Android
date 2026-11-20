package com.duriseo.android.feature.settings.menu

/** 문의하기로 메일 앱에 미리 채울 제목과 본문 (D-150, FR-SET-02 AC1) */
data class SupportMail(val subject: String, val body: String)

/**
 * 문의 메일의 제목과 본문. 본문은 세 줄 "앱 버전: …", "Android 버전: …", "기기 모델: …"이다 (SCREENS.md SCR-SET-01 "문의하기").
 * 문구는 `strings.xml`(`settings_mail_subject`, `settings_mail_body_*`)에 있고, 호출한 쪽이 값을 채운 제목·세 줄을 넘긴다.
 * 이 함수는 줄을 이어 붙일 뿐이라 Android 없이 테스트한다.
 */
fun supportMail(subject: String, appVersionLine: String, androidVersionLine: String, deviceModelLine: String): SupportMail =
    SupportMail(
        subject = subject,
        body = listOf(appVersionLine, androidVersionLine, deviceModelLine).joinToString("\n"),
    )

/**
 * 기기 모델 표기: "제조사 모델" (예: "Google Pixel 8"). 모델이 이미 제조사 이름으로 시작하면 모델만 쓰고,
 * 제조사 첫 글자는 대문자로 한다.
 */
fun deviceModelLabel(manufacturer: String, model: String): String {
    val maker = manufacturer.trim().replaceFirstChar { it.uppercase() }
    val name = model.trim()
    return when {
        maker.isEmpty() -> name
        name.isEmpty() -> maker
        name.startsWith(maker, ignoreCase = true) -> name
        else -> "$maker $name"
    }
}

/** 문의 메일 앱을 여는 `mailto:` 주소. 받는 사람은 문의 메일 주소(D-155)다 */
fun supportMailUri(address: String): String = "mailto:${address.trim()}"
