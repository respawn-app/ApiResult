package pro.respawn.apiresult.sample.domain

import pro.respawn.apiresult.ApiResult
import pro.respawn.apiresult.sample.domain.model.User

interface SecurityRepository {

    suspend fun verifyDeviceIntegrity(): ApiResult<Unit>
}
