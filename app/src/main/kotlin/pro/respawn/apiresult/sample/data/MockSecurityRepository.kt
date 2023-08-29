package pro.respawn.apiresult.sample.data

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import pro.respawn.apiresult.ApiResult
import pro.respawn.apiresult.SuspendResult
import pro.respawn.apiresult.errorUnless
import pro.respawn.apiresult.mapError
import pro.respawn.apiresult.sample.domain.SecurityRepository
import pro.respawn.apiresult.sample.domain.errors.DeviceIntegrityException
import pro.respawn.apiresult.unit

class MockSecurityRepository : SecurityRepository {

    private suspend fun verifySignature() = ApiResult {
        delay(timeMillis = 500)
        true
    }

    private suspend fun verifyPackage() = ApiResult {
        delay(timeMillis = 500)
        true
    }

    override suspend fun verifyDeviceIntegrity() = SuspendResult {
        val packageResult = async {
            verifyPackage()
        }
        val signatureResult = async {
            verifySignature()
        }
        // monad comprehension - require both left and right to be successes
        !packageResult.await() && !signatureResult.await()
    }
        // error if return value is false
        .errorUnless { it }
        .mapError { DeviceIntegrityException() }
        // map to unit
        .unit()
}
