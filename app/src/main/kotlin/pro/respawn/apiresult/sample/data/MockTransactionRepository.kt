package pro.respawn.apiresult.sample.data

import kotlinx.coroutines.delay
import pro.respawn.apiresult.ApiResult
import pro.respawn.apiresult.sample.domain.TransactionRepository
import pro.respawn.apiresult.sample.domain.errors.transactionErrors
import pro.respawn.apiresult.sample.domain.model.Transaction
import pro.respawn.apiresult.sample.domain.model.User
import java.io.IOException
import kotlin.random.Random

class MockTransactionRepository : TransactionRepository {

    override suspend fun performTransaction(user: User) = ApiResult {
        require(Random.nextBoolean())
        delay(1000)
        Transaction()
    }
        .transactionErrors()

    override suspend fun persistTransaction(transaction: Transaction) = ApiResult {
        if (Random.nextBoolean()) throw IOException() // throwing an unhandled, unexpected exception
        delay(1000)
    }
        .transactionErrors()
}
