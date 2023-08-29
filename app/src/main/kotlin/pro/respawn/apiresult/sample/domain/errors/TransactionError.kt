package pro.respawn.apiresult.sample.domain.errors

import pro.respawn.apiresult.ApiResult
import pro.respawn.apiresult.mapError

sealed class TransactionError : RuntimeException() {
    class NotEnoughFunds : TransactionError()
}

fun <T> ApiResult<T>.transactionErrors() = mapError {
    when (it) {
        is IllegalArgumentException -> TransactionError.NotEnoughFunds()
        else -> it // unknown/unhandled error
    }
}
