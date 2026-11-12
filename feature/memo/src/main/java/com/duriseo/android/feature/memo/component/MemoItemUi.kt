package com.duriseo.android.feature.memo.component

import com.duriseo.android.domain.format.DateTexts
import com.duriseo.android.domain.format.MemoDateChip
import com.duriseo.android.domain.format.WrittenAt
import com.duriseo.android.domain.me.Me
import com.duriseo.android.domain.memo.Memo
import com.duriseo.android.domain.memo.MemoTexts
import java.time.Clock
import java.time.LocalDate

/** 메모 작성자를 보는 사람 기준으로 나눈 것. 사람 색 점·아바타 글자("나"·"너")를 정한다 (D-57) */
enum class MemoAuthor { ME, PARTNER }

/**
 * 핀 카드·메모 행·상세가 같이 쓰는 한 메모의 화면 값 (SCREENS.md SCR-MEMO-01 "목록 항목": 제목(D-131), 작성자 닉네임, 작성일(D-129), 메모 날짜).
 *
 * @property title 제목, 없으면 본문 첫 줄 (D-131). 제목도 본문도 없으면 빈 문자열
 * @property preview 미리보기 본문. 줄바꿈을 공백으로 접은 한 줄이고 몇 줄로 자를지는 화면이 정한다 ([MemoPreview])
 * @property writtenAt 작성일 표기 계산 결과 (D-129). 문구는 화면이 붙인다
 * @property memoDate 메모 날짜 칩. 없으면 null
 * @property canEdit 내가 만든 메모만 true: 수정·삭제 (FR-MEMO-03 AC1·AC2)
 */
data class MemoItemUi(
    val id: String,
    val title: String,
    val preview: String,
    val author: MemoAuthor,
    val nickname: String,
    val writtenAt: WrittenAt,
    val memoDate: MemoDateChip?,
    val isPinned: Boolean,
    val isFavorite: Boolean,
    val canEdit: Boolean,
)

/** [Memo]를 화면 값으로 바꾼다. 오늘·어제·올해는 [clock]의 기기 시간대로 가른다 (D-129) */
internal fun Memo.toItemUi(me: Me, clock: Clock): MemoItemUi {
    val mine = authorId == me.id
    val today = LocalDate.now(clock)
    return MemoItemUi(
        id = id,
        title = MemoTexts.listTitle(this),
        preview = MemoPreview.of(title, body),
        author = if (mine) MemoAuthor.ME else MemoAuthor.PARTNER,
        nickname = if (mine) me.nickname else me.space.partner?.nickname.orEmpty(),
        writtenAt = DateTexts.writtenAt(createdAt, clock),
        memoDate = memoDate?.let { DateTexts.memoDateChip(it, today) },
        isPinned = isPinned,
        isFavorite = isFavorite,
        canEdit = mine,
    )
}

/**
 * 미리보기 글: 본문의 줄바꿈·연속 공백을 공백 하나로 접은 한 줄 (보드는 CSS로 접고 말줄임한다).
 * 제목이 없어 본문 첫 줄이 제목 자리에 올라갔으면 그 줄은 빼고 나머지를 쓴다 (unit-6.md 결정 8: 같은 글이 두 번 나오지 않게).
 */
object MemoPreview {
    /** 상태에 너무 긴 글을 싣지 않으려고 자르는 길이. 화면이 말줄임으로 다시 자른다 */
    const val MAX_CHARS = 200

    fun of(title: String?, body: String?): String {
        val text = body.orEmpty().trim()
        if (text.isEmpty()) return ""
        val hasTitle = !title?.trim().isNullOrEmpty()
        val source = if (hasTitle) {
            text
        } else {
            val lines = text.lines().map { it.trim() }
            val firstIndex = lines.indexOfFirst { it.isNotEmpty() }
            lines.drop(firstIndex + 1).joinToString("\n")
        }
        return source.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ").take(MAX_CHARS)
    }
}
