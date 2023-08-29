package pro.respawn.apiresult.sample.util

import android.util.Log
import pro.respawn.apiresult.ApiResult
import pro.respawn.apiresult.onError
import pro.respawn.apiresult.onSuccess

fun ApiResult<*>.log() = onSuccess {
    Log.d("ApiResult", toString())
} onError {
    Log.e("ApiResult", toString())
}
