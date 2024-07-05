package pro.respawn.apiresult.test

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.shouldBe
import pro.respawn.apiresult.ApiResult
import pro.respawn.apiresult.orThrow
import pro.respawn.apiresult.runResulting
import kotlin.coroutines.cancellation.CancellationException

class MiscOperatorTests : FreeSpec({
    val value = 42
    val exception = IllegalArgumentException("error")
    "Given no-arg result builder" - {
        val result = ApiResult()
        "then returns success" {
            result.orThrow() shouldBe Unit
        }
    }
    "Given single-arg result builder" - {
        "then exceptions result in error instances" {
            ApiResult(value = exception) shouldBeEqual ApiResult.Error<Int>(exception)
        }
        "then values result in success instances" {
            ApiResult(value = value) shouldBeEqual ApiResult.Success(value)
        }
    }
    "Given success result" - {
        val result = ApiResult(value)
        "then compares exactly to another success value" {
            result shouldBeEqual ApiResult(42)
        }
        "then is not equal to an error value" {
            result shouldNotBeEqual ApiResult.Error(exception)
        }
        "then is not equal to a loading value" {
            result shouldNotBeEqual ApiResult.Loading()
        }
    }
    "Given result block that throws cancellation" - {
        val block = { throw CancellationException() }
        "then does not catch cancellations" {
            shouldThrowExactly<CancellationException> {
                runResulting(block = block)
            }
        }
    }
})
