package pro.respawn.apiresult.test

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import pro.respawn.apiresult.ApiResult
import pro.respawn.apiresult.cause
import pro.respawn.apiresult.chain
import pro.respawn.apiresult.errorIf
import pro.respawn.apiresult.errorOnLoading
import pro.respawn.apiresult.errorOnNull
import pro.respawn.apiresult.errorUnless
import pro.respawn.apiresult.exceptionOrNull
import pro.respawn.apiresult.fold
import pro.respawn.apiresult.map
import pro.respawn.apiresult.mapEither
import pro.respawn.apiresult.mapError
import pro.respawn.apiresult.mapErrorToCause
import pro.respawn.apiresult.mapLoading
import pro.respawn.apiresult.mapOrDefault
import pro.respawn.apiresult.message
import pro.respawn.apiresult.onError
import pro.respawn.apiresult.onLoading
import pro.respawn.apiresult.onSuccess
import pro.respawn.apiresult.or
import pro.respawn.apiresult.orElse
import pro.respawn.apiresult.orNull
import pro.respawn.apiresult.orThrow
import pro.respawn.apiresult.recover
import pro.respawn.apiresult.recoverIf
import pro.respawn.apiresult.require
import pro.respawn.apiresult.requireIs
import pro.respawn.apiresult.requireNotNull
import pro.respawn.apiresult.stackTrace
import pro.respawn.apiresult.then
import pro.respawn.apiresult.tryChain
import pro.respawn.apiresult.tryMap
import pro.respawn.apiresult.tryRecover
import pro.respawn.apiresult.unit
import pro.respawn.apiresult.unwrap

class SuccessOperatorTests : FreeSpec({
    val value = 42
    val exception = RuntimeException("error")

    "given success value" - {
        val result = ApiResult.Success(value)

        "then isSuccess should be true" {
            result.isSuccess shouldBe true
        }
        "then isFailure should be false" {
            result.isError shouldBe false
        }
        "then isLoading should be false" {
            result.isLoading shouldBe false
        }
        "then components should return result and no exception" {
            val (res, err) = result
            res shouldBe value
            err shouldBe null
        }
        "then bang should not throw" {
            shouldNotThrowAny {
                !result
            }
        }
        "then stacktrace is null" {
            result.stackTrace shouldBe null
        }
        "then message is null" {
            result.message shouldBe null
        }
        "then cause is null" {
            result.cause shouldBe null
        }
        "Then orElse returns value" {
            result.orElse { 0 } shouldBe value
        }
        "then or returns value" {
            result.or(0) shouldBe value
        }
        "then orNull returns non-null value" {
            result.orNull() shouldBe value
        }
        "then exceptionOrNull returns null" {
            result.exceptionOrNull() shouldBe null
        }
        "then fold returns value" {
            result.fold({ it }, { 0 }) shouldBe value
        }
        "then onError does not call error" {
            result.onError { throw AssertionError("Called onError") }
        }
        "then onSuccess calls success" {
            var called = false
            result.onSuccess {
                it shouldBe value
                called = true
            }
            called shouldBe true
        }
        "then onLoading does not call loading" {
            result.onLoading { throw AssertionError("Called onLoading") }
        }
        "then errorIf returns same value as received" {
            result.errorIf { false }.isError shouldBe false
            result.errorIf { true }.isError shouldBe true
        }
        "then errorUnless returns the opposite value" {
            result.errorUnless { true }.isError shouldBe false
            result.errorUnless { false }.isError shouldBe true
        }
        "then errorOnLoading does not produce an error" {
            result.errorOnLoading().isError shouldBe false
        }
        "then requireNotNull returns value" {
            result.requireNotNull().orThrow() shouldBe value
        }
        "then map returns new value" {
            result.map { it + 1 }.orThrow() shouldBe value + 1
        }
        "then mapOrDefault returns new value" {
            result.mapOrDefault({ 0 }) { it + 1 } shouldBe value + 1
        }
        "then mapEither returns new value" {
            val mappedError = IllegalArgumentException(exception)
            result.mapEither({ it + 1 }) { mappedError }.orThrow() shouldBe value + 1
        }
        "then mapLoading is not executed" {
            result.mapLoading { throw AssertionError("Called mapLoading") }
        }
        "then mapError is not executed" {
            result.mapError { throw AssertionError("Called mapError") }
        }
        "then mapErrorToCause is not executed" {
            result.mapErrorToCause().isError shouldBe false
        }
        "then unwrap returns value" {
            val wrapped = ApiResult(result)
            wrapped.unwrap() shouldBe result
            wrapped.unwrap().orThrow() shouldBe value
        }
        "then tryMap catches exceptions" {
            val e = IllegalArgumentException()
            result.tryMap { throw e }.exceptionOrNull() shouldBe e
        }
        "then recover does not change value" {
            result.recover { throw AssertionError("recovered") } shouldBe result
        }
        "then tryRecover does not change value" {
            result.tryRecover { throw AssertionError("recovered") } shouldBe result
        }
        "and given require operator" - {
            "and condition is false" - {
                val required = result.require { value == 0 }
                "then value is error" {
                    required.isError shouldBe true
                }
            }
            "and condition is true" - {
                val required = result.require { value == it }
                "then value is success" {
                    required.isSuccess shouldBe true
                }
            }
        }
        "and recoverIf is not Executed" {
            result.recoverIf({ true }) { throw AssertionError("Called recoverIf") }
        }
        "and a chain operator" - {
            "and chained is an error" - {
                val other = ApiResult.Error<Int>(exception)
                "then the result is an error" {
                    result.chain { other }.isError shouldBe true
                }
            }
            "and chained is a success" - {
                val other = ApiResult.Success(0)
                "then the value is unchanged" {
                    result.chain { other }.orThrow() shouldBe value
                }
            }
            "and chained is loading" - {
                val other = ApiResult.Loading<Int>()
                "then the result is loading" {
                    result.chain { other }.isLoading shouldBe true
                }

            }
        }
        "then tryChain catches exceptions" {
            val e = IllegalArgumentException()
            result.tryChain { throw e }.exceptionOrNull() shouldBe e
        }
        "and a then operator" - {
            "and then is an error" - {
                val other = ApiResult.Error<Int>(exception)
                "then the result is an error" {
                    result.then { other }.isError shouldBe true
                }
            }
            "and then is a success" - {
                val newValue = 0
                val other = ApiResult.Success(newValue)
                "then the value is the new one" {
                    result.then { other }.orThrow() shouldBe newValue
                }
            }
            "and then is loading" - {
                val other = ApiResult.Loading<Int>()
                "then the result is loading" {
                    result.then { other }.isLoading shouldBe true
                }
            }
        }
        "then unit returns Unit" {
            result.unit().orThrow() shouldBe Unit
        }
        "and requireIs is applied" - {
            "and cast succeeds" - {
                "then the result is success" {
                    result.requireIs<Int, _>().orThrow() shouldBe value
                }
            }
            "and cast fails" - {
                "then the result is error" {
                    result.requireIs<String, _>().isError shouldBe true
                }
            }
        }
    }
    "given a null success value" - {
        val result: ApiResult<Int?> = ApiResult(null)
        "then errorOnNull returns error" {
            val e = IllegalArgumentException()
            result.errorOnNull { e }.exceptionOrNull() shouldBe e
        }
    }
})
