@file:Suppress(
    "KotlinRedundantDiagnosticSuppress",
    "MemberVisibilityCanBePrivate",
    "unused",
    "TooManyFunctions",
    "FunctionName",
    "NOTHING_TO_INLINE",
)

package pro.respawn.apiresult

import pro.respawn.apiresult.ApiResult.Error
import pro.respawn.apiresult.ApiResult.Success
import kotlin.jvm.JvmName

/**
 * Returns [emptyList] if [this]'s collection is empty
 */
public inline fun <T> ApiResult<Collection<T>>.orEmpty(): Collection<T> = or(emptyList())

/**
 * Returns [emptyList] if [this]'s collection is empty
 */
public inline fun <T> ApiResult<List<T>>.orEmpty(): List<T> = or(emptyList())

/**
 * Returns [emptyList] if [this]'s collection is empty
 */
public inline fun <T> ApiResult<Set<T>>.orEmpty(): Set<T> = or(emptySet())

/**
 * Returns [emptyMap] if [this]'s map is empty
 */
public inline fun <K, V> ApiResult<Map<K, V>>.orEmpty(): Map<K, V> = or(emptyMap())

/**
 * Returns [emptyList] if [this]'s collection is empty
 */
public inline fun <T> ApiResult<Sequence<T>>.orEmpty(): Sequence<T> = or(emptySequence())

/**
 * Execute [block] if [this]'s collection is empty.
 */
public inline infix fun <T : Iterable<R>, R> ApiResult<T>.onEmpty(
    block: () -> Unit,
): ApiResult<T> = onSuccess { if (it.none()) block() }

/**
 * Execute [block] if [this]'s collection is empty.
 */
public inline infix fun <T : Iterable<R>, R> ApiResult<T>.ifEmpty(
    block: () -> Unit,
): ApiResult<T> = onEmpty(block)

/**
 * Makes [this] an [error] if the collection is empty.
 */
public inline fun <T, R : Iterable<T>> ApiResult<R>.errorIfEmpty(
    exception: () -> Exception = { ConditionNotSatisfiedException("Collection was empty") },
): ApiResult<R> = errorIf(exception) { it.none() }

/**
 * Makes [this] an [error] if the collection is empty.
 */
@JvmName("sequenceErrorIfEmpty")
public inline fun <T, R : Sequence<T>> ApiResult<R>.errorIfEmpty(
    exception: () -> Exception = { ConditionNotSatisfiedException("Sequence was empty") },
): ApiResult<R> = errorIf(exception) { it.none() }

/**
 * Executes [ApiResult.map] on each value of the collection
 */
public inline fun <T, R> ApiResult<Iterable<T>>.mapValues(
    transform: (T) -> R
): ApiResult<List<R>> = map { it.map(transform) }

/**
 * Executes [ApiResult.map] on each value of the sequence
 */
@JvmName("sequenceMapValues")
public inline fun <T, R> ApiResult<Sequence<T>>.mapValues(
    noinline transform: (T) -> R
): ApiResult<Sequence<R>> = map { it.map(transform) }

/**
 * Maps every item of [this] using [transform]
 */
public inline infix fun <T, R> Iterable<ApiResult<T>>.mapResults(
    transform: (T) -> R
): List<ApiResult<R>> = map { it.map(transform) }

/**
 * Maps every item of [this] using [transform]
 */
public inline infix fun <T, R> Sequence<ApiResult<T>>.mapResults(
    crossinline transform: (T) -> R
): Sequence<ApiResult<R>> = map { it.map(transform) }

/**
 * Maps every [Error] in [this] using [transform]
 */
public inline infix fun <T> Iterable<ApiResult<T>>.mapErrors(
    transform: (Exception) -> Exception
): List<ApiResult<T>> = map { it.mapError(transform) }

/**
 * Maps every [Error] in [this] using [transform]
 */
public inline infix fun <T> Sequence<ApiResult<T>>.mapErrors(
    crossinline transform: (Exception) -> Exception,
): Sequence<ApiResult<T>> = map { it.mapError(transform) }

/**
 * Filter the underlying collection.
 */
public inline infix fun <T : Iterable<R>, R> ApiResult<T>.filter(block: (R) -> Boolean): ApiResult<List<R>> =
    map { it.filter(block) }

/**
 * Filter the underlying sequence.
 */
@JvmName("filterSequence")
public inline infix fun <T : Sequence<R>, R> ApiResult<T>.filter(
    noinline block: (R) -> Boolean
): ApiResult<Sequence<R>> = map { it.filter(block) }

/**
 * Filters only [Error] values
 */
public inline fun <T> Iterable<ApiResult<T>>.filterErrors(): List<Error> = filterIsInstance<Error>()

/**
 * Filters only [Error] values
 */
public inline fun <T> Sequence<ApiResult<T>>.filterErrors(): Sequence<Error> = filterIsInstance<Error>()

/**
 * Filters only [Success] values
 */
public inline fun <T> Iterable<ApiResult<T>>.filterSuccesses(): List<Success<T>> = filterIsInstance<Success<T>>()

/**
 * Filters only [Success] values
 */
public inline fun <T> Sequence<ApiResult<T>>.filterSuccesses(): Sequence<Success<T>> = filterIsInstance<Success<T>>()

/**
 * Filters all null values of [Success]es
 */
public inline fun <T> Iterable<ApiResult<T?>>.filterNotNull(): List<ApiResult<T & Any>> =
    filter { it !is Success || it.result != null }.mapResults { it!! }

/**
 * Filters all null values of [Success]es
 */
public inline fun <T> Sequence<ApiResult<T?>>.filterNotNull(): Sequence<ApiResult<T & Any>> =
    filter { it !is Success || it.result != null }.mapResults { it!! }

/**
 * Merges all [Success] results into a single [List], or if any has failed, returns [Error].
 */
public inline fun <T> Iterable<ApiResult<T>>.merge(): ApiResult<List<T>> = ApiResult { map { it.orThrow() } }

/**
 * Merges all [results] into a single [List], or if any has failed, returns [Error].
 */
public inline fun <T> ApiResult.Companion.merge(vararg results: ApiResult<T>): ApiResult<List<T>> =
    results.asIterable().merge()

/**
 * Returns a list of only [Success] values, discarding any errors
 */
public inline fun <T> Iterable<ApiResult<T>>.values(): List<T> = asSequence()
    .filterSuccesses()
    .map { it.result }
    .toList()

/**
 * Return the first [Success] value, or an [Error] if no success was found
 *
 * [Error.e] will always be [NoSuchElementException]
 * @see firstSuccessOrNull
 * @see firstSuccessOrThrow
 */
public inline fun <T> Iterable<ApiResult<T>>.firstSuccess(): ApiResult<T> =
    ApiResult { (first { it is Success } as Success<T>).result }

/**
 * Return the first [Success] value, or throw if no success was found
 * @see firstSuccess
 * @see firstSuccessOrNull
 */
public inline fun <T> Iterable<ApiResult<T>>.firstSuccessOrThrow(): T =
    (first { it is Success } as Success<T>).orThrow()

/**
 * Return the first [Success] value, or null if no success was found
 * @see firstSuccess
 * @see firstSuccessOrThrow
 */
public inline fun <T> Iterable<ApiResult<T>>.firstSuccessOrNull(): T? =
    (first { it is Success } as? Success<T>)?.orNull()
