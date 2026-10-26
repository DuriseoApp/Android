package com.duriseo.android.domain.result

import com.duriseo.android.domain.error.DataError

/** 데이터 계층 호출의 결과. 성공이면 값, 실패면 [DataError]. */
sealed interface DataResult<out T> {
    data class Success<out T>(val value: T) : DataResult<T>

    data class Failure(val error: DataError) : DataResult<Nothing>
}

inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(value))
    is DataResult.Failure -> this
}

inline fun <T> DataResult<T>.onSuccess(action: (T) -> Unit): DataResult<T> {
    if (this is DataResult.Success) action(value)
    return this
}

inline fun <T> DataResult<T>.onFailure(action: (DataError) -> Unit): DataResult<T> {
    if (this is DataResult.Failure) action(error)
    return this
}

fun <T> DataResult<T>.getOrNull(): T? = (this as? DataResult.Success)?.value

fun <T> DataResult<T>.errorOrNull(): DataError? = (this as? DataResult.Failure)?.error
