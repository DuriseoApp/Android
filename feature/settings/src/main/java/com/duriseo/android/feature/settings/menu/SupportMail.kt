package com.duriseo.android.feature.settings.menu

/** 문의하기로 메일 앱에 미리 채울 제목과 본문 (D-150, FR-SET-02 AC1) */
data class SupportMail(val subject: String, val body: String)

/**
 * 문의 메일의 제목과 본문. 본문은 세 줄 "앱 버전: …", "Android 버전: …", "기기 모델: …"이다 (SCREENS.md SCR-SET-01 "문의하기").
 * 메일에 들어가는 글이라 화면 문구(`strings.xml`)가 아니라 여기에 둔다. Android 없이 테스트하려고 값은 받아서 쓴다.
 */
fun supportMail(appVersion: String, androidVersion: String, deviceModel: String): SupportMail = SupportMail(
    subject = SUPPORT_MAIL_SUBJECT,
    body = "앱 버전: $appVersion\nAndroid 버전: $androidVersion\n기기 모델: $deviceModel",
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

private const val SUPPORT_MAIL_SUBJECT = "[둘이서 문의]"
