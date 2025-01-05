package pro.respawn.apiresult.test

import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import pro.respawn.apiresult.ApiResult
import pro.respawn.apiresult.cause
import pro.respawn.apiresult.chain
import pro.respawn.apiresult.errorIf
import pro.respawn.apiresult.errorOnLoading
import pro.respawn.apiresult.errorUnless
import pro.respawn.apiresult.exceptionOrNull
import pro.respawn.apiresult.flatMap
import pro.respawn.apiresult.fold
import pro.respawn.apiresult.map
import pro.respawn.apiresult.mapEither
import pro.respawn.apiresult.mapError
import pro.respawn.apiresult.mapErrorToCause
import pro.respawn.apiresult.mapLoading
import pro.respawn.apiresult.mapOrDefault
import pro.respawn.apiresult.message
import pro.respawn.apiresult.nullOnError
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
import pro.respawn.apiresult.rethrow
import pro.respawn.apiresult.stackTrace
import pro.respawn.apiresult.tryChain
import pro.respawn.apiresult.tryMap
import pro.respawn.apiresult.tryRecover
import pro.respawn.apiresult.tryRecoverIf
import pro.respawn.apiresult.unit
import pro.respawn.apiresult.unwrap

class ErrorOperatorTests : FreeSpec({
    val value = 42
    val cause = Exception("cause")
    val exception = RuntimeException("error", cause)

    "given success value" - {
        val result = ApiResult.Error<Int>(e = exception)
        "then isSuccess should be false" {
            result.isSuccess shouldBe false
        }
        "then isError should be true" {
            result.isError shouldBe true
        }
        "then isLoading should be false" {
            result.isLoading shouldBe false
        }
        "then components should return 1 - null and 2 - exception" {
            val (res, err) = result
            res shouldBe null
            err shouldBe exception
        }
        "then bang should throw" {
            shouldThrowExactly<RuntimeException> { !result }
        }
        "then stacktrace is not null" {
            result.stackTrace shouldBe exception.stackTraceToString()
        }
        "then message is not null" {
            result.message shouldBe exception.message
        }
        "then cause is not null" {
            result.cause shouldBe cause
        }
        "then orElse returns default" {
            result.orElse { 0 } shouldBe 0
        }
        "then rethrow" - {
            "throws for matching types" {
                shouldThrowExactly<RuntimeException> {
                    result.rethrow<RuntimeException, _>()
                }
            }
            "does not throw for all other types" {
                shouldNotThrowAny {
                    result.rethrow<NoSuchElementException, _>()
                }
            }
        }
        "then or returns default" {
            result.or(0) shouldBe 0
        }
        "then orNull returns null value" {
            result.orNull() shouldBe null
        }
        "then exceptionOrNull returns exception" {
            result.exceptionOrNull() shouldBe exception
        }
        "then fold returns default" {
            val default = 0
            result.fold({ it }, { default }, onLoading = { -1 }) shouldBe default
        }
        "then onError calls error" {
            result.shouldCall { result ->
                result.onError {
                    it shouldBe exception
                    markCalled()
                }
            }
        }
        "then onSuccess does not call block" {
            result.onSuccess { fail("Called onSuccess") }
        }
        "then onLoading does not call loading" {
            result.onLoading { fail("Called onLoading") }
        }
        "then nullOnError returns null" {
            result.nullOnError().orThrow() shouldBe null
        }
        "then errorIf returns true always" {
            result.errorIf { false }.isError shouldBe true
            result.errorIf { true }.isError shouldBe true
        }
        "then errorUnless always returns original error" {
            result.errorUnless { true }.exceptionOrNull() shouldBe exception
            result.errorUnless { false }.exceptionOrNull() shouldBe exception
        }
        "then errorOnLoading does not produce an error" {
            // assert we did not get "ConditionNotSatisfiedException"
            result.errorOnLoading().exceptionOrNull() shouldBe exception
        }
        "then requireNotNull returns error" {
            result.requireNotNull() shouldBe result
        }
        "then map returns error value" {
            result.map { it + 1 } shouldBe result
        }
        "then mapOrDefault returns new value" {
            val default = 0
            result.mapOrDefault({ default }) { it + 1 } shouldBe default
        }
        "then mapEither returns exception value" {
            val mappedError = IllegalArgumentException(exception)
            result.mapEither({ it + 1 }) { mappedError }.exceptionOrNull() shouldBe mappedError
        }
        "then mapLoading is not executed" {
            result.mapLoading { fail("Called mapLoading") }
        }
        "then mapError returns new error value" {
            val mappedError = IllegalArgumentException(exception)
            result.mapError { mappedError }.exceptionOrNull() shouldBe mappedError
        }
        "then mapErrorToCause returns cause" {
            result.mapErrorToCause().exceptionOrNull() shouldBe cause
        }
        "then unwrap returns error" {
            val wrapped = ApiResult(result)
            wrapped.unwrap() shouldBe result
            wrapped.unwrap().exceptionOrNull() shouldBe exception
        }
        "then tryMap is not executed" {
            result.tryMap { fail("Called tryMap") }
        }
        "then recover recovers from exception" {
            result.recover { ApiResult(value) }.orThrow() shouldBe value
        }
        "then tryRecover recovers from exception" {
            result.tryRecover { value }.orThrow() shouldBe value
        }
        "then tryRecoverIf catches exceptions" {
            val e = IllegalArgumentException()
            shouldNotThrowAny {
                result
                    .tryRecoverIf({ true }) { throw e }
                    .exceptionOrNull() shouldBe e
            }
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
                "then value is still false" {
                    required.isSuccess shouldBe false
                }
            }
        }
        "then recoverIf is executed" {
            result.recoverIf({ true }) { ApiResult(value) }.orThrow() shouldBe value
        }
        "then chain is not executed" {
            result.chain { fail("chain should not be executed") }
        }
        "then tryChain is not executed" {
            result.tryChain { fail("Called tryChain") }
        }
        "and a flatMap operator" - {
            "then the result is always error" - {
                forAll(
                    row(ApiResult.Error(IllegalArgumentException("another"))),
                    row(ApiResult.Success(value)),
                    row(ApiResult.Loading()),
                ) { other ->
                    "for value $other" {
                        result.flatMap { other } shouldBe result
                    }
                }
            }
        }
        "then unit does not do anything" {
            result.unit() shouldBe result
        }
        "then requireIs returns error" {
            result.requireIs<Int, _>() shouldBe result
        }
    }
})
