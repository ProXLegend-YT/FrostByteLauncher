package com.frostbyte.launcher.core.common

/**
 * Lightweight result wrapper used across repositories/managers instead of
 * throwing exceptions across layer boundaries. Kept intentionally simple
 * (no dependency on kotlin.Result's inline-class quirks with suspend fns).
 */
sealed class FrostByteResult<out T> {
    data class Success<T>(val value: T) : FrostByteResult<T>()
    data class Failure(val message: String, val cause: Throwable? = null) : FrostByteResult<Nothing>()

    inline fun onSuccess(block: (T) -> Unit): FrostByteResult<T> {
        if (this is Success) block(value)
        return this
    }

    inline fun onFailure(block: (String, Throwable?) -> Unit): FrostByteResult<T> {
        if (this is Failure) block(message, cause)
        return this
    }
}
