@file:OptIn(ExperimentalContracts::class)
@file:Suppress(
    "KotlinRedundantDiagnosticSuppress",
    "MemberVisibilityCanBePrivate",
    "unused",
    "NOTHING_TO_INLINE",
    "TooManyFunctions",
    "ThrowingExceptionsWithoutMessageOrCause", "UNCHECKED_CAST",
)

package pro.respawn.apiresult

import pro.respawn.apiresult.ApiResult.Companion.Error
import pro.respawn.apiresult.ApiResult.Companion.Success
import pro.respawn.apiresult.ApiResult.Error
import pro.respawn.apiresult.ApiResult.Loading
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.JvmField
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmName

/**
 * A class that represents a result of an operation.
 *
 * This class is **efficient**:
 * * no actual objects are created,
 * * all operations are inlined
 * * no function resolution is performed.
 *
 * ApiResult is **not** an Rx-style callback chain -
 * the operators that are invoked are called **immediately** and in-place.
 */
@JvmInline
public value class ApiResult<out T> private constructor(@PublishedApi internal val value: Any?) {

    /**
     * Get the [Success] component of this result or null
     *
     * Use like:
     * ```
     * val (success, failure) = ApiResult { ... }
     * ```
     * @see orNull
     */
    public inline operator fun component1(): T? = orNull()

    /**
     * Get the [Error] component of this result or null
     *
     * Use like:
     * ```
     * val (success, failure) = ApiResult { ... }
     * ```
     * @see exceptionOrNull
     */
    public inline operator fun component2(): Exception? = exceptionOrNull()

    /**
     * Bang operator returns the result or throws if it is an [Error] or [Loading]
     * This is equivalent to calling [orThrow]
     */
    public inline operator fun not(): T = orThrow()

    /**
     * The state of [ApiResult] that represents an error.
     * @param e wrapped [Exception]
     */
    @JvmInline
    @PublishedApi
    internal value class Error private constructor(@JvmField val e: Exception) {

        override fun toString(): String = "ApiResult.Error: message=${e.message} and cause: $e"

        companion object {

            fun create(e: Exception) = Error(e)
        }
    }

    @PublishedApi
    internal data object Loading {

        override fun toString(): String = "ApiResult.Loading"
    }

    /**
     * Whether this is [Success]
     */
    public inline val isSuccess: Boolean get() = !isError && !isLoading

    /**
     *  Whether this is [Error]
     */
    public inline val isError: Boolean get() = value is Error

    /**
     * Whether this is [Loading]
     */
    public inline val isLoading: Boolean get() = value === Loading

    override fun toString(): String = when {
        value is Error || value === Loading -> value.toString()
        else -> "ApiResult.Success: $value"
    }

    public companion object {

        /**
         * Create a successful [ApiResult] value
         */
        public fun <T> Success(value: T): ApiResult<T> = ApiResult(value = value)

        /**
         * Create an error [ApiResult] value
         */
        public fun <T> Error(e: Exception): ApiResult<T> = ApiResult(value = Error.create(e = e))

        /**
         * Create a loading [ApiResult] value
         */
        public fun <T> Loading(): ApiResult<T> = ApiResult(value = Loading)

        /**
         * Create an [ApiResult] instance using the given value.
         * When [value] is an Exception, an error result will be created.
         * Otherwise, a success result will be created.
         *
         * If you want to directly create a success value of an [Exception], use [Success]
         */
        public inline operator fun <T> invoke(value: T): ApiResult<T> = when (value) {
            is Exception -> Error(e = value)
            else -> Success(value)
        }

        /**
         * Execute [call], catching any exceptions, and wrap it in an [ApiResult].
         *
         * Caught exceptions are mapped to [ApiResult.Error]s.
         * [Throwable]s are not caught on purpose.
         * [CancellationException]s are rethrown.
         */
        public inline operator fun <T> invoke(call: () -> T): ApiResult<T> = try {
            Success(value = call())
        } catch (e: CancellationException) {
            throw e
        } catch (expected: Exception) {
            Error(e = expected)
        }

        /**
         * Returns an [ApiResult](Unit) value.
         * Use this for applying operators such as `require` and `mapWrapping` to build chains of operators that should
         * start with an empty value.
         */
        public inline operator fun invoke(): ApiResult<Unit> = Success(Unit)
    }
}

/**
 * [ApiResult.Error.e]'s stack trace as string
 */
public inline val ApiResult<*>.stackTrace: String? get() = exceptionOrNull()?.stackTraceToString()

/**
 * [ApiResult.Error.e]'s cause
 */
public inline val ApiResult<*>.cause: Throwable? get() = exceptionOrNull()?.cause

/**
 * [ApiResult.Error.e]'s message.
 */
public inline val ApiResult<*>.message: String? get() = exceptionOrNull()?.message

/**
 * Execute [block] wrapping it in an [ApiResult]
 * @see ApiResult.invoke
 */
public inline fun <T, R> T.runResulting(block: T.() -> R): ApiResult<R> = ApiResult(call = { block() })

/**
 * Executes [block], wrapping it in an [ApiResult]
 * @see ApiResult.invoke
 */
public inline fun <T> runResulting(block: () -> T): ApiResult<T> = ApiResult(call = { block() })

/**
 * Executes [block] if [this] is an [ApiResult.Error], otherwise returns [ApiResult.value]
 * [Loading] will result in [NotFinishedException]
 */
@Suppress("UNCHECKED_CAST")
public inline infix fun <T, R : T> ApiResult<T>.orElse(block: (e: Exception) -> R): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return when (value) {
        is Error -> block(value.e)
        is Loading -> block(NotFinishedException())
        else -> value as T
    }
}

/**
 *  If [this] is [Error] or [Loading], returns [defaultValue].
 *  @see orElse
 */
public inline infix fun <T, R : T> ApiResult<T>.or(defaultValue: R): T = when (value) {
    is Error, is Loading -> defaultValue
    else -> value as T
}

/**
 * @return null if [this] is an [ApiResult.Error] or [ApiResult.Loading], otherwise return self.
 */
public inline fun <T> ApiResult<T>?.orNull(): T? = this?.or(null)

/**
 * @return exception if [this] is [Error] or null
 */
public inline fun <T> ApiResult<T>?.exceptionOrNull(): Exception? = (this?.value as? Error)?.e

/**
 * Throws [ApiResult.Error.e], or [NotFinishedException] if the request has not been completed yet.
 * @see ApiResult.not
 */
public inline fun <T> ApiResult<T>.orThrow(): T = orElse { throw it }

/**
 * Throws if [this] result is an [Error] and [Error.e] is of type [T]. Ignores all other exceptions.
 *
 * @return a result that can be [Error] but is guaranteed to not have an exception of type [T] wrapped.
 */
public inline fun <reified T : Exception, R> ApiResult<R>.rethrow(): ApiResult<R> = mapError<T, R> { throw it }

/**
 * Fold [this] returning the result of [onSuccess] or [onError]
 * By default, maps [Loading] to [Error] with [NotFinishedException]
 */
@Suppress("UNCHECKED_CAST")
public inline fun <T, R> ApiResult<T>.fold(
    onSuccess: (result: T) -> R,
    onError: (e: Exception) -> R,
    noinline onLoading: (() -> R)? = null,
): R {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onError, InvocationKind.AT_MOST_ONCE)
    }
    return when (value) {
        is Error -> onError(value.e)
        is Loading -> onLoading?.invoke() ?: onError(NotFinishedException())
        else -> onSuccess(value as T)
    }
}

/**
 * Invoke a given [block] if [this] is [Error]
 * @see onSuccess
 * @see onLoading
 */
public inline infix fun <T> ApiResult<T>.onError(block: (Exception) -> Unit): ApiResult<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (value is Error) block(value.e)
    return this
}

/**
 * Invoke a given block if [this] is [Error] and it's [Error.e] is of type [E].
 */
@JvmName("onErrorTyped")
public inline infix fun <reified E : Exception, T> ApiResult<T>.onError(block: (E) -> Unit): ApiResult<T> = onError {
    if (it is E) block(it)
}

/**
 * Invoke a given [block] if [this] is [Success]
 * @see onError
 * @see onLoading
 */
public inline infix fun <T> ApiResult<T>.onSuccess(block: (T) -> Unit): ApiResult<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (isSuccess) block(value as T)
    return this
}

/**
 * Invoke given [block] if [this] is [Loading]
 * @see onError
 * @see onSuccess
 */
public inline infix fun <T> ApiResult<T>.onLoading(block: () -> Unit): ApiResult<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (isLoading) block()
    return this
}

/**
 * Makes [this] an [Error] if [predicate] returns false
 * @see errorIf
 */
public inline fun <T> ApiResult<T>.errorUnless(
    exception: () -> Exception = { ConditionNotSatisfiedException() },
    predicate: (T) -> Boolean,
): ApiResult<T> = errorIf(exception) { !predicate(it) }

/**
 * Makes [this] an [Error] if [predicate] returns true
 * @see errorUnless
 */
public inline fun <T> ApiResult<T>.errorIf(
    exception: () -> Exception = { ConditionNotSatisfiedException() },
    predicate: (T) -> Boolean,
): ApiResult<T> {
    contract {
        callsInPlace(predicate, InvocationKind.AT_MOST_ONCE)
        callsInPlace(exception, InvocationKind.AT_MOST_ONCE)
    }
    if (!isSuccess) return this
    if (!predicate(value as T)) return this
    return Error(e = exception())
}

/**
 * Makes this result an [Error] if [this] result is [Loading]
 */
public inline fun <T> ApiResult<T>.errorOnLoading(
    exception: () -> Exception = { NotFinishedException() }
): ApiResult<T> {
    contract {
        callsInPlace(exception, InvocationKind.AT_MOST_ONCE)
    }

    return when (value) {
        is Loading -> Error(e = exception())
        else -> this
    }
}

/**
 * Alias for [errorOnNull]
 */
public inline fun <T> ApiResult<T?>?.requireNotNull(): ApiResult<T & Any> = errorOnNull()

/**
 * Alias for [orThrow]
 * @see orThrow
 */
public inline fun <T> ApiResult<T>.require(): T = orThrow()

/**
 * Change the type of the [Success] to [R] without affecting [Error]/[Loading] results
 * @see mapError
 * @see map
 * @see tryMap
 */
public inline infix fun <T, R> ApiResult<T>.map(block: (T) -> R): ApiResult<R> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (isSuccess) return Success(value = block(value as T))
    return this as ApiResult<R>
}

/**
 * Map the [Success] result using [transform], and if the result is not a success, return [default]
 */
public inline fun <T, R> ApiResult<T>.mapOrDefault(default: (e: Exception) -> R, transform: (T) -> R): R {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
        callsInPlace(default, InvocationKind.AT_MOST_ONCE)
    }
    return map(transform).orElse(default)
}

/**
 * Map both [Error] and [Success]. Does not affect [Loading]
 */
public inline fun <T, R> ApiResult<T>.mapEither(
    success: (T) -> R,
    error: (Exception) -> Exception,
): ApiResult<R> = map(success).mapError(error)

/**
 * Maps [Loading] to a [Success], not affecting other states.
 * @see mapError
 * @see map
 * @see tryMap
 */
public inline infix fun <T, R : T> ApiResult<T>.mapLoading(block: () -> R): ApiResult<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (isLoading) Success(block()) else this
}

/**
 * Change the exception of the [Error] response without affecting loading/success results
 */
public inline infix fun <T> ApiResult<T>.mapError(
    block: (Exception) -> Exception
): ApiResult<T> = mapError<Exception, _>(block)

/**
 * Map the exception of the [Error] state, but only if this exception is of type [R].
 * [Loading] and [Success] are unaffected
 */
@JvmName("mapErrorTyped")
public inline infix fun <reified R : Exception, T> ApiResult<T>.mapError(block: (R) -> Exception): ApiResult<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return when {
        value is Error && value.e is R -> Error(e = block(value.e))
        else -> this
    }
}

/**
 * Maps the error of the result, if present, to its cause, or self if cause is not available
 */
public inline fun <T> ApiResult<T>.mapErrorToCause(): ApiResult<T> = mapError { it.cause as? Exception ?: it }

/**
 * Unwrap an ApiResult<ApiResult<T>> to be ApiResult<T>
 */
public inline fun <T> ApiResult<ApiResult<T>>.unwrap(): ApiResult<T> = when (value) {
    is Error, is Loading -> this
    else -> value
} as ApiResult<T>

/**
 * Change the type of successful result to [R], also wrapping [block]
 * in another result then folding it (handling exceptions)
 * @see map
 * @see mapError
 * @see mapLoading
 */
public inline infix fun <T, R> ApiResult<T>.tryMap(
    block: (T) -> R
): ApiResult<R> = map { ApiResult(call = { block(it) }) }.unwrap()

/**
 * Make this result an [Error] if [Success] value was null.
 * @see errorUnless
 * @see errorIf
 * @see errorIfEmpty
 */
public inline fun <T : Any> ApiResult<T?>?.errorOnNull(
    exception: () -> Exception = { ConditionNotSatisfiedException("Value was null") },
): ApiResult<T> {
    contract {
        returnsNotNull()
    }
    return when (val r = this?.value) {
        is Error -> Error(e = r.e)
        is Loading -> ApiResult.Loading()
        null -> Error(e = exception())
        else -> Success(value = r as T)
    }
}

/**
 * Maps [Error] values to nulls
 * @see orNull
 */
public inline fun <T> ApiResult<T>.nullOnError(): ApiResult<T?> = if (isError) Success(null) else this

/**
 * Recover from an exception of type [R], else no-op.
 * Does not affect [Loading].
 *
 * Overload for a lambda that already returns an [ApiResult].
 * @see recover
 */
@JvmName("recoverTyped")
public inline infix fun <reified T : Exception, R> ApiResult<R>.recover(
    another: (e: T) -> ApiResult<R>
): ApiResult<R> = when {
    value is Error && value.e is T -> another(value.e)
    else -> this
}

/**
 * Recover from an exception. Does not affect [Loading]
 * See also the typed version of this function to recover from a specific exception type
 */
public inline infix fun <T> ApiResult<T>.recover(
    another: (e: Exception) -> ApiResult<T>
): ApiResult<T> = recover<Exception, T>(another)

/**
 * calls [recover] catching and wrapping any exceptions thrown inside [block].
 */
@JvmName("tryRecoverTyped")
public inline infix fun <reified T : Exception, R> ApiResult<R>.tryRecover(block: (T) -> R): ApiResult<R> =
    recover<T, R>(another = { ApiResult(call = { block(it) }) })

/**
 * Calls [recover] catching and wrapping any exceptions thrown inside [block].
 * See also the typed version of this function to recover from a specific exception type
 */
public inline infix fun <T> ApiResult<T>.tryRecover(
    block: (e: Exception) -> T
): ApiResult<T> = tryRecover<Exception, T>(block)

/**
 * Recover from an [Error] only if the [condition] is true, else no-op.
 * Does not affect [Loading]
 * @see recoverIf
 */
public inline fun <T> ApiResult<T>.tryRecoverIf(
    condition: (Exception) -> Boolean,
    block: (Exception) -> T,
): ApiResult<T> = recoverIf(condition) { ApiResult(call = { block(it) }) }

/**
 * Recover from an [Error] only if the [condition] is true, else no-op.
 * Does not affect [Loading]
 * @see tryRecoverIf
 */
public inline fun <T> ApiResult<T>.recoverIf(
    condition: (Exception) -> Boolean,
    block: (Exception) -> ApiResult<T>,
): ApiResult<T> {
    contract {
        callsInPlace(condition, InvocationKind.AT_MOST_ONCE)
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (value !is Error || !condition(value.e)) return this
    return block(value.e)
}

/**
 * Call [another] and retrieve the result.
 * If the result is success, continue (**the result of calling [another] is discarded**).
 * If the result is an error, propagate it to [this].
 * Effectively, requires for another [ApiResult] to succeed before proceeding with this one.
 * @see [ApiResult.then]
 */
public inline infix fun <T> ApiResult<T>.chain(another: (T) -> ApiResult<*>): ApiResult<T> {
    contract {
        callsInPlace(another, InvocationKind.AT_MOST_ONCE)
    }
    return map { outer -> another(outer).map { outer } }.unwrap()
}

/**
 * Call [block], wrapping it in an [ApiResult], and then discard the [Success] value, but propagate [Error]s.
 *
 * If the result is success, continue (**the result of calling [block] is discarded**).
 * If the result is an error, propagate it to [this].
 *
 * Alias for [chain] for calls that do not return an ApiResult already.
 * @see [ApiResult.chain]
 * @see [ApiResult.then]
 */
public inline infix fun <T> ApiResult<T>.tryChain(
    block: (T) -> Unit
): ApiResult<T> = chain(another = { ApiResult(call = { block(it) }) })

/**
 * Call [another] and if it succeeds, continue with [another]'s result.
 * If it fails, propagate the error.
 * Effectively, [flatMap] to another result.
 *
 * @see ApiResult.chain
 * @see ApiResult.flatMap
 */
public inline infix fun <T, R> ApiResult<T>.then(another: (T) -> ApiResult<R>): ApiResult<R> {
    contract {
        callsInPlace(another, InvocationKind.AT_MOST_ONCE)
    }
    return map(another).unwrap()
}

/**
 * Call [another] and if it succeeds, continue with [another]'s result.
 * If it fails, propagate the error.
 * An alias for [then].
 *
 * @see ApiResult.then
 * @see ApiResult.chain
 */
public inline infix fun <T, R> ApiResult<T>.flatMap(another: (T) -> ApiResult<R>): ApiResult<R> = then(another)

/**
 * Makes [this] an error with [ConditionNotSatisfiedException]
 * using specified [message] if the [predicate] returns false.
 */
public inline fun <T> ApiResult<T>.require(
    message: () -> String? = { null },
    predicate: (T) -> Boolean
): ApiResult<T> = errorUnless(
    exception = { ConditionNotSatisfiedException(message()) },
    predicate = predicate
)

/**
 * Map [this] result to [Unit], discarding the value
 */
public inline fun ApiResult<*>.unit(): ApiResult<Unit> = map {}

/**
 * Create an [ApiResult] from `this` value, based on the type of it.
 *
 * @see ApiResult.invoke
 */
public inline val <T> T.asResult: ApiResult<T> get() = ApiResult(this)

/**
 * Alias for [map] that takes [this] as a parameter
 */
public inline infix fun <T, R> ApiResult<T>.apply(block: T.() -> R): ApiResult<R> = map(block)

/**
 * @return if [this] result value is [R], then returns it. If not, returns an [ApiResult.Error]
 */
public inline fun <reified R, T> ApiResult<T>.requireIs(
    exception: (T) -> Exception = { value ->
        "Result value is of type ${value?.let { it::class.simpleName }} but expected ${R::class.simpleName}"
            .let(::ConditionNotSatisfiedException)
    },
): ApiResult<R> = tryMap { value ->
    if (value !is R) throw exception(value)
    value
}
