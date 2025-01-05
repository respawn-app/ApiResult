package pro.respawn.apiresult.test

import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import pro.respawn.apiresult.SuspendResult
import pro.respawn.apiresult.exceptionOrNull
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalStdlibApi::class)
class SuspendResultTests : FreeSpec({
    coroutineTestScope = true

    val e = IllegalStateException("Failure")

    "Given empty context" - {
        val ctx = EmptyCoroutineContext
        "And SuspendResult that throws in a coroutine" - {
            val result = SuspendResult(ctx) {
                launch { throw e }
            }
            "Then exception is wrapped" {
                result.exceptionOrNull() shouldBe e
            }
        }
        "And SuspendResult that throws in asyncs" - {
            val result = SuspendResult(ctx) {
                async { 42 }
                async { throw e }
            }
            "Then exception is wrapped" {
                result.exceptionOrNull() shouldBe e
            }
        }
        "And SuspendResult that nests coroutines" - {
            val result = SuspendResult(ctx) {
                launch { launch { throw e } }
            }
            "Then exception is wrapped" {
                result.exceptionOrNull() shouldBe e
            }
        }
        "And SuspendResult that throws in another context" - {
            val result = SuspendResult(ctx) {
                launch(Dispatchers.Default) { throw e }
            }
            "Then exception is wrapped" {
                result.exceptionOrNull() shouldBe e
            }
        }
    }
    "Given non-empty context" - {
        val ctx = UnconfinedTestDispatcher(testCoroutineScheduler, "Ctx")
        "And SuspendResult that throws in a coroutine" - {
            val result = SuspendResult(ctx) {
                launch { throw e }
            }
            "Then exception is wrapped" {
                result.exceptionOrNull() shouldBe e
            }
        }
        "And SuspendResult that nests coroutines" - {
            val result = SuspendResult(ctx) {
                launch { launch { throw e } }
            }
            "Then exception is wrapped" {
                result.exceptionOrNull() shouldBe e
            }
        }
    }
})
