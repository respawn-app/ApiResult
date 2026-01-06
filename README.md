# ApiResult

[![CI](https://github.com/respawn-app/ApiResult/actions/workflows/ci.yml/badge.svg)](https://github.com/respawn-app/ApiResult/actions/workflows/ci.yml)
![License](https://img.shields.io/github/license/respawn-app/ApiResult)
![GitHub last commit](https://img.shields.io/github/last-commit/respawn-app/ApiResult)
![Issues](https://img.shields.io/github/issues/respawn-app/ApiResult)
![GitHub top language](https://img.shields.io/github/languages/top/respawn-app/ApiResult)
[![CodeFactor](https://www.codefactor.io/repository/github/respawn-app/ApiResult/badge)](https://www.codefactor.io/repository/github/respawn-app/ApiResult)
[![AndroidWeekly #556](https://androidweekly.net/issues/issue-556/badge)](https://androidweekly.net/issues/issue-556/)

![badge][badge-android] ![badge][badge-jvm] ![badge][badge-js] ![badge][badge-nodejs] ![badge][badge-linux] ![badge][badge-windows] ![badge][badge-wasm] ![badge][badge-ios] ![badge][badge-mac] ![badge][badge-watchos] ![badge][badge-tvos]

ApiResult is a Kotlin Multiplatform declarative error handling framework that is performant, easy to use and
feature-rich.

ApiResult is [Railway Programming](https://proandroiddev.com/railway-oriented-programming-in-kotlin-f1bceed399e5) and
functional
error handling **on steroids**.

## Why use a library instead of try/catch?

Exceptions in Kotlin are **unchecked**. 
Each time you call a function, it can throw and crash you app. 
With ApiResult, you will never have this problem again.

* ApiResult **forces** your code users to handle errors. Forget about unhandled exceptions and unexpected crashes.
* ApiResult is **lightweight**. The library creates no objects and has ~0 performance impact.
* Use 90+ operators covering most of possible use cases to turn your
  code from imperative and procedural to declarative and functional, which is more readable and extensible.
* Core library has **no dependencies**. No need to worry about unexpected junk in your codebase.
* This isn't like Arrow, where with a monad you get a bunch of extra black magic. This framework focuses on **error handling** only.
* ApiResult is fully compatible with Exceptions and Coroutines. Just wrap a call and it will work.
* The library has 140+ tests for 92% operator coverage. Expect long-term support and stability.

## How do I use it?

```kotlin
// wrap a result of any computation and expose the result
class BillingRepository(private val api: RestApi) {

    suspend fun getSubscriptions() = ApiResult {
        api.getSubscriptions()
    } // -> ApiResult<List<Subscription>?>
}

// obtain and handle the result in the client code
fun onClickVerify() {
    val state: SubscriptionState = billingRepository.getSubscriptions()
        .errorOnNull() // map nulls to error states with compile-time safety
        .recover<NotSignedInException, _> { emptyList() } // recover from some or all errors
        .require { securityRepository.isDeviceTrusted() } // conditionally fail the chain
        .mapValues(::SubscriptionModel) // map list items
        .filter { it.isPurchased } // filter
        .mapError<NetworkException, _, _> { e -> BillingException(cause = e) } // map exceptions
        .then { validateSubscriptions(it) } // execute a computation and continue with its result, propagating errors
        .chain { updateGracePeriod(it) } // execute another computation, and if it fails, stop the chain
        .onError { subscriptionService.disconnect() } // executed on error
        .onEmpty { return SubscriptionState.NotSubscribed } // use non-local returns and short-circuit evaluation
        .fold(
            onSuccess = { SubscriptionState.Subscribed(it) },
            onError = { SubscriptionState.Error(it) },
        ) // unwrap the result to another value
    // ...
}
```

## Quickstart

* Documentation:
  [![Docs](https://img.shields.io/website?down_color=red&down_message=Offline&label=Docs&up_color=green&up_message=Online&url=https%3A%2F%2Fopensource.respawn.pro%2FApiResult%2F%23%2F)](https://opensource.respawn.pro/ApiResult)
* KDoc:
  [![Javadoc](https://javadoc.io/badge2/pro.respawn.apiresult/core/javadoc.svg)](https://opensource.respawn.pro/ApiResult/javadocs)
* Latest version:
  ![Maven Central](https://img.shields.io/maven-central/v/pro.respawn.apiresult/core?label=Maven%20Central)

```toml
[versions]
apiresult = "< Badge above 👆🏻 >"

[dependencies]
apiresult = { module = "pro.respawn.apiresult:core", version.ref = "apiresult" } 
```
-----
```kotlin
dependencies {
    // usually you will want to expose ApiResult types in your module APIs, so consider using api() for the dependency
    commonMainApi("pro.respawn.apiresult:core:<version>")
}
```

Ready to try? Start with reading the [Quickstart Guide](https://opensource.respawn.pro/ApiResult/#/quickstart).

## License

```
   Copyright 2022-2026 Respawn Team and contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

```

[badge-android]: http://img.shields.io/badge/-android-6EDB8D.svg?style=flat

[badge-android-native]: http://img.shields.io/badge/support-[AndroidNative]-6EDB8D.svg?style=flat

[badge-jvm]: http://img.shields.io/badge/-jvm-DB413D.svg?style=flat

[badge-js]: http://img.shields.io/badge/-js-F8DB5D.svg?style=flat

[badge-js-ir]: https://img.shields.io/badge/support-[IR]-AAC4E0.svg?style=flat

[badge-nodejs]: https://img.shields.io/badge/-nodejs-68a063.svg?style=flat

[badge-linux]: http://img.shields.io/badge/-linux-2D3F6C.svg?style=flat

[badge-windows]: http://img.shields.io/badge/-windows-4D76CD.svg?style=flat

[badge-wasm]: https://img.shields.io/badge/-wasm-624FE8.svg?style=flat

[badge-apple-silicon]: http://img.shields.io/badge/support-[AppleSilicon]-43BBFF.svg?style=flat

[badge-ios]: http://img.shields.io/badge/-ios-CDCDCD.svg?style=flat

[badge-mac]: http://img.shields.io/badge/-macos-111111.svg?style=flat

[badge-watchos]: http://img.shields.io/badge/-watchos-C0C0C0.svg?style=flat

[badge-tvos]: http://img.shields.io/badge/-tvos-808080.svg?style=flat
