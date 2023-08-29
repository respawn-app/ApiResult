package pro.respawn.apiresult.sample

import pro.respawn.apiresult.ApiResult
import pro.respawn.apiresult.sample.domain.model.User
import java.util.UUID

interface UserRepository {

    suspend fun getUser(id: UUID): ApiResult<User>
    suspend fun getAnonymousUser(): ApiResult<User>
}
