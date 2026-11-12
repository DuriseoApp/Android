package com.duriseo.android.data.local.memo

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 메모 캐시(Room)를 `data:repository`에 보여 주는 얇은 창구. Room 타입은 이 모듈 밖으로 나가지 않는다.
 * `open`인 것은 저장소 테스트가 Room 쓰기 실패를 결정적으로 만들려고 상속하기 때문이다.
 */
@Singleton
open class MemoLocalDataSource @Inject constructor(
    private val dao: MemoDao,
) {
    open fun observeList(listType: String): Flow<List<MemoEntity>> = dao.observeList(listType)

    open fun observeState(key: String): Flow<MemoPageStateEntity?> = dao.observeState(key)

    open suspend fun nextCursor(listType: String): String? = dao.getState(listType)?.nextCursor

    /** 받은 적이 있는 보기의 `listType`들 */
    open suspend fun loadedViews(): List<String> = dao.loadedViewKeys()

    open suspend fun replaceList(listType: String, items: List<MemoEntity>, nextCursor: String?, totalCount: Int, favoriteCount: Int) =
        dao.replaceList(listType, items, nextCursor, totalCount, favoriteCount)

    open suspend fun appendList(listType: String, items: List<MemoEntity>, nextCursor: String?, totalCount: Int, favoriteCount: Int) =
        dao.appendList(listType, items, nextCursor, totalCount, favoriteCount)

    open suspend fun replacePinned(items: List<MemoEntity>) = dao.replacePinned(items)

    open fun observeDated(from: LocalDate, to: LocalDate): Flow<List<MemoEntity>> = dao.observeDated(from.toString(), to.toString())

    open suspend fun replaceDated(from: LocalDate, to: LocalDate, entities: List<MemoEntity>) =
        dao.replaceDated(from.toString(), to.toString(), entities)

    open fun observeMemo(memoId: String): Flow<MemoEntity?> = dao.observeMemo(memoId)

    open suspend fun replaceDetail(entity: MemoEntity) = dao.replaceDetail(entity)

    open suspend fun hasDetail(memoId: String): Boolean = dao.detailCount(memoId) > 0

    /** 404·삭제 성공: 그 메모의 모든 행을 지운다 */
    open suspend fun delete(memoId: String) = dao.deleteMemo(memoId)

    open suspend fun clear() = dao.clear()
}
