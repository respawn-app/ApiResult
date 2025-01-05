package pro.respawn.apiresult.test

import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import pro.respawn.apiresult.ApiResult
import pro.respawn.apiresult.NotFinishedException
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
import pro.respawn.apiresult.stackTrace
import pro.respawn.apiresult.tryChain
import pro.respawn.apiresult.tryMap
import pro.respawn.apiresult.tryRecover
import pro.respawn.apiresult.unit
import pro.respawn.apiresult.unwrap

class LoadingOperatorTests : FreeSpec({
    val value = 42
    val cause = Exception("cause")
    val exception = RuntimeException("error", cause)

    "given loading value" - {
        val result = ApiResult.Loading<Int>()

        "then isSuccess should be false" {
            result.isSuccess shouldBe false
        }
        "then isError should be true" {
            result.isError shouldBe false
        }
        "then isLoading should be true" {
            result.isLoading shouldBe true
        }
        "then components should return null" {
            val (res, err) = result
            res shouldBe null
            err shouldBe null
        }
        "then bang should throw NotFinishedException" {
            shouldThrowExactly<NotFinishedException> { !result }
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
        "Then orElse returns default" {
            result.orElse { 0 } shouldBe 0
        }
        "then or returns default" {
            result.or(0) shouldBe 0
        }
        "then orNull returns null" {
            result.orNull() shouldBe null
        }
        "then exceptionOrNull returns null" {
            result.exceptionOrNull() shouldBe null
        }
        "then fold returns default" {
            val default = 0
            val loading = -1
            result.fold({ it }, { default }, { loading }) shouldBe loading
        }
        "then onError does not call error" {
            result.onError { fail("Called onError") }
        }
        "then onSuccess does not call block" {
            result.onSuccess { fail("Called onSuccess") }
        }
        "then onLoading calls block" {
            result.shouldCall { it.onLoading { markCalled() } }
        }
        "then nullOnError returns loading" {
            result.nullOnError() shouldBe result
        }

        "then errorIf returns false always" {
            result.errorIf { false }.isError shouldBe false
            result.errorIf { true }.isError shouldBe false
        }
        "then errorUnless always returns false" {
            result.errorUnless { true }.isError shouldBe false
            result.errorUnless { false }.isError shouldBe false
        }
        "then errorOnLoading produces an error" {
            result
                .errorOnLoading()
                .exceptionOrNull()
                .shouldBeInstanceOf<NotFinishedException>()
        }
        "then requireNotNull returns loading" {
            result.requireNotNull() shouldBe result
        }
        "then map returns the same value" {
            result.map {
                fail("Called map")
                it + 1
            } shouldBe result
        }
        "then mapOrDefault returns new value" {
            val default = 0
            result.mapOrDefault({ default }) { it + 1 } shouldBe default
        }
        "then mapEither does nothing" {
            val mappedError = IllegalArgumentException(exception)
            result.mapEither({ it + 1 }) { mappedError } shouldBe result
        }
        "then mapLoading is executed" {
            shouldCall {
                result.mapLoading {
                    markCalled()
                    value
                }.orThrow() shouldBe value
            }
        }
        "then mapError returns null" {
            val mappedError = IllegalArgumentException(exception)
            result.mapError { mappedError }.exceptionOrNull() shouldBe null
        }
        "then mapErrorToCause does nothing" {
            result.mapErrorToCause() shouldBe result
        }
        "then unwrap returns error" {
            val wrapped = ApiResult(result)
            wrapped.unwrap() shouldBe result
        }
        "then tryMap is not executed" {
            result.tryMap { fail("Called tryMap") }
        }
        "then recover is not executed" {
            result.recover { fail("called recover") } shouldBe result
        }
        "then tryRecover is not executed" {
            result.tryRecover { fail("called tryRecover") } shouldBe result
        }
        "and given require operator" - {
            forAll(row(false), row(true)) { cond ->
                "and condition is $cond then value is loading" {
                    result.require { cond } shouldBe result
                }
            }
        }
        "then recoverIf is not executed" {
            result.recoverIf({ true }) { fail("called recoverIf") } shouldBe result
        }
        "then chain is not executed" {
            result.chain { fail("chain should not be executed") }
        }
        "then tryChain is not executed" {
            result.tryChain { fail("Called tryChain") }
        }
        "and a flatMap operator" - {
            "then the result is always loading" - {
                forAll(
                    row(ApiResult.Error(IllegalArgumentException("another"))),
                    row(ApiResult.Success(value)),
                    row(ApiResult.Loading()),
                ) { other ->
                    "for value $other" - {
                        result.flatMap { other } shouldBe result
                    }
                }
            }
        }
        "then unit does not do anything" {
            result.unit() shouldBe result
        }
        "then requireIs does nothing" - {
            result.requireIs<Int, _>() shouldBe result
        }
    }
})
