package pro.respawn.apiresult.test

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot

inline fun <T> haveCalled(crossinline block: CallScope.(value: T) -> Unit) = Matcher<T> {
    val scope = CallScope()
    scope.block(it)
    MatcherResult(
        passed = scope.isPass,
        failureMessageFn = { "Expected function was not called" },
        negatedFailureMessageFn = { "Expected function should not have been called" },
    )
}

class CallScope(private var called: Boolean = false) {

    val isPass get() = called

    fun markCalled() {
        called = true
    }
}

inline infix fun <T> T.shouldCall(crossinline block: CallScope.(value: T) -> Unit): T {
    this should haveCalled(block)
    return this
}

inline infix fun <T> T.shouldNotCall(crossinline block: CallScope.(value: T) -> Unit): T {
    this shouldNot haveCalled(block)
    return this
}
