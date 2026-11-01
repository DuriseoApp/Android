package com.duriseo.android.domain.me

/**
 * 앱이 알아챈 공간의 변화. 구독하는 쪽(`feature:main`)이 화면 이동 기록을 다시 만든다.
 * 연결(커플이 됨)은 이벤트가 아니라 [MeRepository.observeLinkedSheetNeeded]의 상태로 전해진다.
 */
enum class SpaceChange {
    /**
     * 연결이 해제된 것을 알았다 (API.md 4.5의 6단계, D-103). 로그인 정보를 뺀 로컬 데이터는 이미 지웠다.
     * SCR-COUPLE-01 하나로 화면 이동 기록을 다시 만든다 (앞 화면이 없어 뒤로 버튼이 없다, D-96).
     */
    UNLINKED,
}
