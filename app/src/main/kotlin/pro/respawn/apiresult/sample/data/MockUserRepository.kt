package pro.respawn.apiresult.sample.data

import pro.respawn.apiresult.ApiResult
import pro.respawn.apiresult.mapError
import pro.respawn.apiresult.sample.UserRepository
import pro.respawn.apiresult.sample.domain.errors.AuthenticationException
import pro.respawn.apiresult.sample.domain.model.User
import java.util.UUID

class MockUserRepository : UserRepository {

    override suspend fun getUser(id: UUID) = ApiResult {
        User(id)
    }
        .mapError { AuthenticationException() }

    override suspend fun getAnonymousUser() = ApiResult(User())
}
