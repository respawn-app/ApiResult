package pro.respawn.apiresult.sample.domain

import pro.respawn.apiresult.ApiResult

interface SecurityRepository {

    suspend fun verifyDeviceIntegrity(): ApiResult<Unit>
}
