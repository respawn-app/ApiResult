package pro.respawn.apiresult.sample.domain

import pro.respawn.apiresult.ApiResult
import pro.respawn.apiresult.sample.domain.model.Transaction
import pro.respawn.apiresult.sample.domain.model.User

interface TransactionRepository {

    suspend fun persistTransaction(transaction: Transaction): ApiResult<Unit>
    suspend fun performTransaction(user: User): ApiResult<Transaction>
}
