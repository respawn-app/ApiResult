# Get started with ApiResult

Browse
code: [ApiResult](https://github.com/respawn-app/kmmutils/tree/master/apiresult/src/commonMain/kotlin/pro/respawn/kmmutils/apiresult)

ApiResult is a class that wraps the result of a computation.
Similar to monads, it has 2 main and 1 additional state:

* `Success` - contains a value returned by a computation
* `Error` - wraps an exception caught during a computation
* `Loading` - intermediate and optional state for async operations

## Usage

Example usages cover three main cases:

* Wrapping a result of a computation
* Wrapping a result of an async computation with multiple coroutines
* Turning a computation into a flow

```kotlin
// wrap a result of a computation
suspend fun getSubscriptions(userId: String): ApiResult<List<SubscriptionResponse>> = ApiResult {
        api.getSubscriptions(userId)
    }

// emits: Loading -> Success<User> / Error<Exception>
fun getSubscriptionsAsync(userId: String): Flow<Apiresult<List<SubscriptionResponse>>> = ApiResult.flow {
    api.getSubscriptions(id)
}

// SuspendResult will wait for the result of nested coroutines and propagate exceptions thrown in them
suspend fun getVerifiedSubs(userId: String) = SuspendResult { // this: CoroutineScope
    val subs = api.getSubscriptions(userId)

    launch {
        api.verifySubscriptions(subs)
    }
    launch {
        storage.saveSubsscriptions(subs)
    }

    subs
}
```

After you create your ApiResult, apply a variety of transformations on it:

```kotlin
val state: SubscriptionState = repo.getSubscriptions(userId)
    .errorOnNull() // map nulls to error states with compile-time safety
    .recover<NotSignedInException, _> { emptyList() } // recover from some or all errors
    .require { securityRepository.isDeviceTrusted() } // conditionally fail the chain
    .mapValues(::SubscriptionModel) // map list items
    .filter { it.isPurchased } // filter values
    .mapError<NetworkException, _, _> { e -> BillingException(cause = e) } // map exceptions
    .then { validateSubscriptions(it) } // execute a computation and continue with its result, propagating errors
    .chain { updateGracePeriod(it) } // execute another computation, and if it fails, stop the chain
    .onError { subscriptionService.disconnect() } // executed on error
    .onEmpty { return SubscriptionState.NotSubscribed } // use non-local returns and short-circuit evaluation
    .fold(
        onSuccess = { SubscriptionState.Subscribed(it) },
        onError = { SubscriptionState.Error(it) },
    ) // unwrap the result to another value
```

## Operators

There are more than 85 operators covering possible use cases for transforming, wrapping, handling, reducing the result
for collections as well as coroutines.

### Create:

* `ApiResult { computation() } ` - wrap the result of a computation
* `ApiResult.flow { computation() }` - produce a flow
* `ApiResult(value) ` - either Error or Success based on the type of the value
* `runResulting { computation() }` - for parity with `runCatching`
* `ApiResult()` - start with an `ApiResult<Unit>` and then run mapping operators such as `then`. Useful when you want to
  check some conditions or evaluate other properties before executing an expensive operation.

### Fold results:

* `val (result, error) = ApiResult { ... }` - disassemble the result into two nullable types: `T?` of success,
  and `Exception?` of error
* `or(value)` - returns `value` if the result is an Error
* `orElse { computeValue() }` - returns the result of `computeValue()`
* `orNull()` - return `null` if the result is an `Error`
* `exceptionOrNull()` - if the result is an error, returns the exception and discards the result
* `orThrow()` - throw `Error`s. This is the same as the binding operator from functional programming
* `fold(onSuccess = { /* ... */ }, onError = { /* ... */ })` - fold the result to another type
* `onSuccess { computation(it) }` - execute an operation if the result is a `Success`. `it` is the result value
* `onError<CustomException, _> { e -> e.fallbackValue }` - execute `onError` if exception is of type `CustomException`
* `onLoading { setLoading(true) }` - execute an operation if the result is `Loading`
* `!` (bang) operator:
    ```kotlin
    val result: ApiResult<Int> = ApiResult(1)
    val value: Int = !result
    ```
  Get the result or throw if it is an `Error`. It is the same as Kotlin's `!!` or calling `orThrow()`.

### Transform:

* `unwrap()` - sometimes you get into a situation where you have `ApiResult<ApiResult<T>>`. Fix using this operator.
* `then { anotherCall(it) }` or `flatMap()` - execute another ApiResult call and continue with its result type
* `chain { anotherCall(it) }` - execute another ApiResult call,
  but discard it's Success result and continue with the previous result
* `map { it.transform() }` - map `Success` result values to another type
* `tryMap { it.transformOrThrow() } ` - map, but catch exceptions in the `transform` block. This is the same as `then`,
  but for calls that do not return an `ApiResult`
* `mapError { e -> CustomException(cause = e) } ` - map `Error` values to another exception type
* `mapLoading { null }` - map `Loading` values
* `mapEither(success = { it.toModel() }, error { e -> CustomException() } )` - map both `Success` and `Error` results.
  Loading is not affected and is handled by other operators like `errorOnLoading()`
* `mapOrDefault(default = { null }, { it.toModel() } )` - map the value and return it if the result is `Success`. If
  not, just return `default`
* `errorIf { it.isInvalid }` - error if the predicate is true
* `errorUnless { it.isAuthorized }` - make this result an `Error` if a condition is false
* `errorOnNull()` - make this result an `Error` if the success value is null
* `errorOnLoading()` - turns this result to an `Error` if it is `Loading`. Some operators do this under the hood
* `require()`  - throws if the result is an `Error` or `Loading` and always returns `Success<T>`
* `require { condition() } ` - aliases for `errorUnless`
* `requireNotNull()` - require that the `Success` value is non-null
* `nullOnError()` - returns `Success<T?>` if the result is an error
* `recover { e -> e.defaultValue }` - recover from all exceptions
* `recover<EmptyException, _> { e -> e.defaultValue }` - recover from a specific exception type
* `recoverIf(condition = { it.isRecoverable }, block = { null })`
* `tryRecover { it.defaultValueOrThrow() } ` - recover, but if the `block` throws, wrap the exception and continue
* `tryChain { anotherCallOrThrow(it) } ` - chain another call that can throw and wrap the error if it does. Useful when
  the call that you are trying to chain to does not return an `ApiResult` already, unlike what `chain` expects
* `mapErrorToCause()` - map errors to their causes, if present, and if not, return that same exception

### Collection operators:

* `mapValues { item -> item.transform() } ` - map collection values of the `Success` result
* `onEmpty { block() } ` - execute an operation if the result is `Success` and the collection is empty
* `orEmpty()` - return an empty collection if this result is an `Error` or `Loading`
* `errorIfEmpty()` - make this result an error if the collection is empty.
* `mapResults { it.toModel() }` - map all successful results of a collection of results
* `mapErrors { e -> CustomException(e) }` - map all errors of a collection of results
* `filter { it.isValid }` - filter each value of a collection of a successful result
* `filterErrors()` - return a list that contains only `Error` results
* `filterSuccesses()` - return a list that contains only `Success` results
* `filterNotNull()` - return a collection where each `Success` result is returned only if its value is not null
* `merge()` - merges all `Success` results into a list, or if any failed, returns `Error`
* `merge(vararg results: ApiResult<T>)` - same as `merge`, but for arbitrary parameters
* `values()` - return a list of values from `Success` results, discarding `Error` or `Loading`
* `firstSuccess()` - return a first `Success` value from a collection of results, or if not present, an `Error`
* `firstSuccessOrNull()` - return the first success value or null if not present
* `firstSuccessOrThrow()` - return the first success value or throws if not present

### Coroutine operators

* `SuspendResult { }` - an `ApiResult` builder that takes a suspending `block` and allows to launch coroutines
  inside and handle exceptions in any of them.
* `ApiResult.flow { suspendingCall() } ` - emits `Loading` first, then executes `call` and emits its result
* `Flow<T>.asApiResult()` - transforms this flow into another, where the `Loading` value is emitted first, and then
  a `Success` or `Error` value is emitted based on exceptions that are thrown in the flow's scope. The resulting flow
  will **not** throw **unless** other operators are applied on top of it that can throw (i.e. `onEach`)
* `mapResults { it.transform() }` - maps `Success` value of a flow of results
* `rethrowCancellation()` - an operator that is used when a `CancellationException` may have accidentally been
  wrapped. `ApiResult` does not wrap cancellation exceptions, but other code can. Cancellation exceptions should not be
  wrapped
* `onEachResult { action() }` - execute `action` on each successful result of the flow

### Monad comprehensions

Monad comprehensions are when you "bind" multiple results during a certain operation,
and if any of the bound expressions fails, computation is halted and an error is returned.

Monad comprehensions are simply not needed with ApiResult. The same can be achieved using existing operators:

```kotlin
interface Repository {
    fun getUser(): ApiResult<User>
    fun getSubscriptions(user: User): ApiResult<List<Subscription>>
    fun verifyDevice(): ApiResult<Unit>
}

val subscriptions: ApiResult<List<Subscription>> = ApiResult {
    val verificationResult = repo.verifyDevice()

    // bang (!) operator throws Errors, equivalent to binding
    // if bang does not throw, the device is verified
    !verificationResult

    val user: User = !userRepository.getUser() // if bang does not throw, user is logged in

    !repo.getSubscriptions(user)
}
```

## Notes and usage advice

* ApiResult is **not** an async scheduling engine like Rx.
  As soon as you call an operator on the result, it is executed. So pay attention to the order of operators that you
  apply on your results. If needed, start with `ApiResult` empty constructor and then use `tryMap` or `then` to delay
  the execution until you run other operators.
* ApiResult does **not** catch `Throwable`s. This was a purposeful decision. We want to only catch exceptions that can
  be handled. Most `Error`s can not be handled effectively by the application.
* ApiResult does **not** catch `CancellationException`s as they are not meant to be caught at all.
  In case you think you might have wrapped a `CancellationException` in your result,
  use `rethrowCancellation()` at the end of the chain.
* Same as `kotlin.Result`, ApiResult is not meant to be passed around to the UI layer.
  Be sure not to propagate results everywhere in your code, and handle them on the layer responsible for error handling.

## How does ApiResult differ from other wrappers?

* `kotlin.Result` is an existing solution for result wrapping,
  however, it's far less performant, less type safe and, most importantly, doesn't offer the declarative api as rich as
  ApiResult. You could call ApiResult a successor to `kotlin.Result`.
* [kotlin-result](https://github.com/michaelbull/kotlin-result/) is similar to ApiResult and is multiplatform, however
  its api is not as rich as ApiResult's and the author decided to not limit the Error type to be a child of `Exception`.
  If you don't like that ApiResult uses Exceptions, you may use kotlin-result. In our opinion, not using exceptions is a
  drawback as they are a powerful, existing, widespread language feature. Having `Exception` as a parent
  does not limit what you can do with `ApiResult` but saves you from extra type and operator overhead while enabling
  better compatibility with existing language and library features.
* ApiResult serves a different purpose than [Sandwich](https://github.com/skydoves/sandwich).
  Sandwich specializes in integration with Retrofit and, therefore, is not multiplatform.  
  ApiResult allows you to wrap any computation, be it Ktor, Retrofit, or a database call. ApiResult is more lightweight
  and extensible, because it does not hardcode error handling logic. A simple extension on an ApiResult that
  uses `mapErrors` will allow you to transform exceptions to your own error types.
* ApiResult is different from [EitherNet](https://github.com/slackhq/EitherNet) because once again -
  it doesn't hardcode your error types. ApiResult is multiplatform and lightweight:
  no crazy mappings that use reflection to save you from writing 0.5 lines of code to wrap a call in an ApiResult.
* ApiResult is a lighter version of Arrow.kt Monads such as Either. Sometimes you want a monad to wrap your  
  computations, but don't want to introduce the full complexity and intricacies of Arrow and functional programming.
  ApiResult also utilizes existing Exception support in Kotlin instead of trying to wrap any type as an error type. You
  can still use sealed interfaces and other features if you subclass Exception from that interface.
  ApiResult is easier to understand and use, although less powerful than Arrow.
