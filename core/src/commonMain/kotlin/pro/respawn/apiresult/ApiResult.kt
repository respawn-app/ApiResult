@file:OptIn(ExperimentalContracts::class)
@file:Suppress(
    "KotlinRedundantDiagnosticSuppress",
    "MemberVisibilityCanBePrivate",
    "unused",
    "NOTHING_TO_INLINE",
    "TooManyFunctions",
    "ThrowingExceptionsWithoutMessageOrCause",
    "INVISIBLE_REFERENCE",
    "INVISIBLE_MEMBER",
)

package pro.respawn.apiresult

import pro.respawn.apiresult.ApiResult.Error
import pro.respawn.apiresult.ApiResult.Loading
import pro.respawn.apiresult.ApiResult.Success
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmName

/**
 * A class that represents a result of an operation.
 *
 * This class is **efficient**: no actual objects are created unless dynamic type resolution is required,
 * all operations are inlined and no function resolution is performed.
 * ApiResult is **not** an Rx-style callback chain -
 * the operators that are invoked are called **immediately** and in-place.
 */
public sealed interface ApiResult<out T> {

    /**
     * Get the [Success] component of this result or null
     *
     * Use like:
     * ```
     * val (success, failure) = ApiResult { ... }
     * ```
     * @see orNull
     */
    public operator fun component1(): T? = orNull()

    /**
     * Get the [Error] component of this result or null
     *
     * Use like:
     * ```
     * val (success, failure) = ApiResult { ... }
     * ```
     * @see exceptionOrNull
     */
    public operator fun component2(): Exception? = exceptionOrNull()

    /**
     * Bang operator returns the result or throws if it is an [Error] or [Loading]
     * This is equivalent to calling [orThrow]
     */
    public operator fun not(): T = orThrow()

    /**
     * A value of [ApiResult] for its successful state.
     * @param result a successful result value
     */
    @JvmInline
    public value class Success<out T>(public val result: T) : ApiResult<T> {

        override fun toString(): String = "ApiResult.Success: $result"
    }

    /**
     * The state of [ApiResult] that represents an error.
     * @param e wrapped [Exception]
     */
    @JvmInline
    public value class Error(public val e: Exception) : ApiResult<Nothing> {

        override fun toString(): String = "ApiResult.Error: message=$message and cause: $e"
    }

    /**
     * Whether this is [Success]
     */
    public val isSuccess: Boolean get() = this is Success

    /**
     *  Whether this is [Error]
     */
    public val isError: Boolean get() = this is Error

    /**
     * Whether this is [Loading]
     */
    public val isLoading: Boolean get() = this is Loading

    /**
     * A loading state of an [ApiResult]
     */
    public data object Loading : ApiResult<Nothing>

    public companion object {
        /**
         * Execute [call], catching any exceptions, and wrap it in an [ApiResult].
         *
         * Caught exceptions are mapped to [ApiResult.Error]s.
         * [Throwable]s are not caught on purpose.
         * [CancellationException]s are rethrown.
         */
        public inline operator fun <T> invoke(call: () -> T): ApiResult<T> = try {
            Success(call())
        } catch (e: CancellationException) {
            throw e
        } catch (expected: Exception) {
            Error(expected)
        }

        /**
         *  * If [T] is an exception, will produce [ApiResult.Error]
         *  * If [T] is Loading, will produce [ApiResult.Loading]
         *  * Otherwise [ApiResult.Success].
         *  @see asResult
         */
        public inline operator fun <T> invoke(value: T): ApiResult<T> = when (value) {
            is Loading -> value
            is Exception -> Error(value)
            else -> Success(value)
        }

        /**
         * Returns an [Success] (Unit) value.
         * Use this for applying operators such as `require` and `mapWrapping` to build chains of operators that should
         * start with an empty value.
         */
        public inline operator fun invoke(): ApiResult<Unit> = Success(Unit)
    }
}

/**
 * [ApiResult.Error.e]'s stack trace as string
 */
public inline val Error.stackTrace: String get() = e.stackTraceToString()

/**
 * [ApiResult.Error.e]'s cause
 */
public inline val Error.cause: Throwable? get() = e.cause

/**
 * [ApiResult.Error.e]'s message.
 */
public inline val Error.message: String? get() = e.message

/**
 * Execute [block] wrapping it in an [ApiResult]
 * @see ApiResult.invoke
 */
public inline fun <T, R> T.runResulting(block: T.() -> R): ApiResult<R> = ApiResult { block() }

/**
 * Executes [block], wrapping it in an [ApiResult]
 * @see ApiResult.invoke
 */
public inline fun <T> runResulting(block: () -> T): ApiResult<T> = ApiResult { block() }

/**
 * Executes [block] if [this] is an [ApiResult.Error], otherwise returns [ApiResult.Success.result]
 * [Loading] will result in [NotFinishedException]
 */
public inline infix fun <T, R : T> ApiResult<T>.orElse(block: (e: Exception) -> R): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Success -> result
        is Error -> block(e)
        is Loading -> block(NotFinishedException())
    }
}

/**
 *  If [this] is [Error], returns [defaultValue].
 *  @see orElse
 */
public inline infix fun <T, R : T> ApiResult<T>.or(defaultValue: R): T = orElse { defaultValue }

/**
 * @return null if [this] is an [ApiResult.Error] or [ApiResult.Loading], otherwise return self.
 */
public inline fun <T> ApiResult<T>?.orNull(): T? = this?.or(null)

/**
 * @return exception if [this] is [Error] or null
 */
public inline fun <T> ApiResult<T>?.exceptionOrNull(): Exception? = (this as? Error)?.e

/**
 * Throws [ApiResult.Error.e], or [NotFinishedException] if the request has not been completed yet.
 * @see ApiResult.not
 */
public inline fun <T> ApiResult<T>.orThrow(): T = when (this) {
    is Loading -> throw NotFinishedException()
    is Error -> throw e
    is Success -> result
}

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
public inline fun <T, R> ApiResult<T>.fold(
    onSuccess: (result: T) -> R,
    onError: (e: Exception) -> R,
    noinline onLoading: (() -> R)? = null,
): R {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onError, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Success -> onSuccess(result)
        is Error -> onError(e)
        is Loading -> onLoading?.invoke() ?: onError(NotFinishedException())
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
    if (this is Error) block(e)
    return this
}

/**
 * Invoke a given block if [this] is [Error] and it's [Error.e] is of type [E].
 */
@JvmName("onErrorTyped")
public inline infix fun <reified E : Exception, T> ApiResult<T>.onError(block: (E) -> Unit): ApiResult<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (this is Error && e is E) block(e)
    return this
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
    if (this is Success) block(result)
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
    if (this is Loading) block()
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
    return if (this is Success && predicate(result)) Error(exception()) else this
}

/**
 * Makes this result an [Error] if [this] result is [Loading]
 */
public inline fun <T> ApiResult<T>.errorOnLoading(
    exception: () -> Exception = { NotFinishedException() }
): ApiResult<T> {
    contract {
        callsInPlace(exception, InvocationKind.AT_MOST_ONCE)
        returns() implies (this@errorOnLoading !is Loading)
    }

    return when (this) {
        is Loading -> Error(exception())
        else -> this
    }
}

/**
 * Alias for [errorOnNull]
 */
public inline fun <T> ApiResult<T?>?.requireNotNull(): ApiResult<T & Any> = errorOnNull()

/**
 * Throws if [this] is not [Success] and returns [Success] otherwise.
 * @see orThrow
 */
public inline fun <T> ApiResult<T>.require(): Success<T> = Success(!this)

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
    return when (this) {
        is Success -> Success(block(result))
        is Error -> Error(e)
        is Loading -> this
    }
}

/**
 * Map the [Success] result using [transform], and if the result is not a success, return [default]
 */
public inline fun <T, R> ApiResult<T>.mapOrDefault(default: () -> R, transform: (T) -> R): R {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
        callsInPlace(default, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Success -> transform(result)
        else -> default()
    }
}

/**
 * Map both [Error] and [Success]. Does not affect [Loading]
 */
public inline fun <T, R> ApiResult<T>.mapEither(
    success: (T) -> R,
    error: (Exception) -> Exception,
): ApiResult<R> = when (this) {
    is Error -> Error(error(e))
    is Loading -> this
    is Success -> Success(success(result))
}

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
    return when (this) {
        is Success, is Error -> this
        is Loading -> Success(block())
    }
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
        this is Error && e is R -> Error(block(e))
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
public inline fun <T> ApiResult<ApiResult<T>>.unwrap(): ApiResult<T> = fold(
    onSuccess = { it },
    onError = { Error(it) },
    onLoading = { Loading }
)

/**
 * Change the type of successful result to [R], also wrapping [block]
 * in another result then folding it (handling exceptions)
 * @see map
 * @see mapError
 * @see mapLoading
 */
public inline infix fun <T, R> ApiResult<T>.tryMap(block: (T) -> R): ApiResult<R> =
    map { ApiResult { block(it) } }.unwrap()

/**
 * Make this result an [Error] if [Success] value was null.
 * @see errorUnless
 * @see errorIf
 * @see errorIfEmpty
 */
public inline fun <T> ApiResult<T?>?.errorOnNull(
    exception: () -> Exception = { ConditionNotSatisfiedException("Value was null") },
): ApiResult<T & Any> {
    contract {
        returns() implies (this@errorOnNull != null)
    }
    return this?.errorIf(exception) { it == null }?.map { it!! } ?: Error(exception())
}

/**
 * Maps [Error] values to nulls
 * @see orNull
 */
public inline fun <T> ApiResult<T>.nullOnError(): ApiResult<T?> {
    contract {
        returns() implies (this@nullOnError !is Error)
    }
    return if (this is Error) Success(null) else this
}

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
    this is Error && e is T -> another(e)
    else -> this
}

/**
 * Recover from an exception. Does not affect [Loading]
 * See also the typed version of this function to recover from a specific exception type
 */
public inline infix fun <T> ApiResult<T>.recover(another: (e: Exception) -> ApiResult<T>): ApiResult<T> {
    contract {
        returns() implies (this@recover !is Error)
    }
    return recover<Exception, T>(another)
}

/**
 * calls [recover] catching and wrapping any exceptions thrown inside [block].
 */
@JvmName("tryRecoverTyped")
public inline infix fun <reified T : Exception, R> ApiResult<R>.tryRecover(block: (T) -> R): ApiResult<R> =
    recover<T, R>(another = { ApiResult { block(it) } })

/**
 * Calls [recover] catching and wrapping any exceptions thrown inside [block].
 * See also the typed version of this function to recover from a specific exception type
 */
public inline infix fun <T> ApiResult<T>.tryRecover(
    block: (e: Exception) -> T
): ApiResult<T> {
    contract {
        returns() implies (this@tryRecover !is Error)
    }
    return tryRecover<Exception, T>(block)
}

/**
 * Recover from an [Error] only if the [condition] is true, else no-op.
 * Does not affect [Loading]
 * @see recoverIf
 */
public inline fun <T> ApiResult<T>.tryRecoverIf(
    condition: (Exception) -> Boolean,
    block: (Exception) -> T,
): ApiResult<T> = recoverIf(condition) { ApiResult { block(it) } }

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
    return when {
        this is Error && condition(e) -> block(e)
        else -> this
    }
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
    return when (this) {
        is Loading, is Error -> this
        is Success -> another(result).map { result }
    }
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
public inline infix fun <T> ApiResult<T>.tryChain(block: (T) -> Unit): ApiResult<T> =
    chain(another = { ApiResult { block(it) } })

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
        "Result value is of type ${value?.let { it::class.simpleName }} but expected ${R::class}"
            .let(::ConditionNotSatisfiedException)
    },
): ApiResult<R> = tryMap { value ->
    if (value !is R) throw exception(value)
    value
}
