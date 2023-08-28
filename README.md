# ApiResult

[![CI](https://github.com/respawn-app/ApiResult/actions/workflows/ci.yml/badge.svg)](https://github.com/respawn-app/ApiResult/actions/workflows/ci.yml)
![License](https://img.shields.io/github/license/respawn-app/ApiResult)
![GitHub last commit](https://img.shields.io/github/last-commit/respawn-app/ApiResult)
![Issues](https://img.shields.io/github/issues/respawn-app/ApiResult)
![GitHub top language](https://img.shields.io/github/languages/top/respawn-app/ApiResult)
[![CodeFactor](https://www.codefactor.io/repository/github/respawn-app/ApiResult/badge)](https://www.codefactor.io/repository/github/respawn-app/ApiResult)
[![AndroidWeekly #556](https://androidweekly.net/issues/issue-556/badge)](https://androidweekly.net/issues/issue-556/)

ApiResult is a Kotlin Multiplatform declarative error handling framework that is performant, easy to use and
feature-rich.

ApiResult is [Railway Programming](https://proandroiddev.com/railway-oriented-programming-in-kotlin-f1bceed399e5) and functional
error handling **on steroids**.

## Features

* ApiResult is **extremely lightweight**. It is lighter than kotlin.Result.
  All instances of it are `value class`es, all operations are `inline`, which means literally 0 overhead.
* ApiResult offers 85+ operators covering most of possible use cases to turn your
  code from imperative and procedural to declarative and functional, which is more readable and extensible.
* ApiResult defines a contract that you can use in your code. No one will be able to obtain the result of a computation
  without being forced to handle errors at compilation time.

## Preview

```kotlin
// wrap a result of a computation
suspend fun getSubscriptions(userId: String): ApiResult<List<Subscription>?> = ApiResult {
    api.getSubscriptions(userId)
}

// use and transform the result
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

Ready to try? Start with reading the [Quickstart Guide](https://opensource.respawn.pro/ApiResult/#/quickstart).

## Supported platforms:

* JVM: [ `Android`, `JRE 11+` ],
* Linux [ `x64`, `mingw64` ],
* Apple: [ `iOSx64`, `macOSx64`, `watchOSx64`, `tvOSx64` ],
* js: [ `nodejs`, `browser` ]

## License

```
   Copyright 2022-2023 Respawn Team and contributors

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
