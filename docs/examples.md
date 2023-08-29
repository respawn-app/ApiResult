## Advanced examples of error handling

Learning by example can be easier than reading through the documentation with dozens of operators.
The sheer api surface of the library can be a little bit overwhelming.
Here is a collection of examples you can take a look at to understand how Railway-oriented programming and declarative,
functional error handling works in general. After a few minutes of using the library, writing your own operator chains
will become very easy.

### Example: Publishing a game with several sequential API calls and progress reporting.

This is a `doWork()` method of Android's Worker. Our use case was to publish a game that requires uploading multiple
files, all the while reporting the publish progress. The process has to be halted if any of the operations fails.
This resulted in a scarily long at first sight, but easy to understand in practice call chain

This code is stripped down from all the unnecessary stuff.

```kotlin

// created a custom operator for logging to logcat
inline fun <T> ApiResult<T>.log() = onSuccess {
    Logger.v { "ApiResult Success: $it" }
} onError { e ->
    Logger.e { "ApiResult Error: $e" }
}


class PublishGameWorker : CoroutineWorker(context, workerParams) {

    // we started with an empty result to defer the computations until checks pass
    override suspend fun doWork() = ApiResult()
        .tryMap { inputData.getLong(DATA_LOCAL_GAME_ID, 0L) } // then tried to get worker parameters
        .requireNotNull() // and required the data to be not null. Failure on this step will cancel the worker already
        .map(GameId::Local) // mapped the long to game id class
        .tryMap { repo.getLocalGameSync(it) } // then tried to get the game
        .requireNotNull() // failing if the game was not found
        .tryChain { updateProgress(POSTING_GAME_DATA) } // then tried to update worker progress safely
        .then { game -> // proceeding to load the local game's data
            repo.loadGameData(game) // first finding the data
                .then { data -> repo.publishGame(game, data) } // and then publishing that data safely
        }
        .tryChain { updateProgress(WorkerProgressState.GETTING_VIDEO_URLS) } // updating the progress again
        .tryMap { game ->
            // here we wanted to preserve the previous result and add another one to it. We used the Pair class 
            // and monad comprehensions with tryMap() and bang operator (!) to achieve the desired outcome
            val id = !game.getRemoteId()
            val url = !repo.getGameUrl(id)
            game to url
        }
        .tryChain { updateProgress(UPLOADING_VIDEO, 0f) }
        .chain { (game, url) -> uploadVideo(url, game.videoUri) } // then chained another API call 
        .map { (game, _) -> game } // thrown away the now unnecessary url
        .tryChain { updateProgress(SIGNALING_VIDEO_UPLOAD) }
        .chain { signalVideoUpload(it.remoteId?.id) } // another safe api call
        .tryChain { updateProgress(FINISHED) }
        .also { cleanupFiles() } // also (from kotlin) makes the result cleanup on fail too, unlike tryChain
        .log()
        .fold(
            onSuccess = { Result.success(workDataOf(RESULT_SHARE_URL to it.url)) },
            onError = { Result.failure() },
        ) // and folding to worker result. Profit!


    fun RemoteGame.getRemoteId() = ApiResult(this.remoteId?.id)
        .errorOnNull()
        .map(GameId::Remote)

    // we're using suspend result to wrap the upload progress
    private suspend fun uploadVideo(url: String, videoUri: Uri) = SuspendResult {
        api.uploadVideo(
            url = url,
            file = getVideoFile(videoUri),
            progressCallback = { progress ->
                // suspend result allowed us to launch nested coroutines and wait for their completion easily
                launch {
                    updateProgress(
                        state = UPLOADING_VIDEO,
                        currentStateProgress = progress,
                    )
                }
            },
        )
    }.log()

    private suspend fun signalVideoUpload(gameId: String?) = ApiResult(gameId) // starting with a nullable game id
        .requireNotNull() // and then failing if it was not found, before executing any database calls
        .tryMap { api.signalUploadedVideo(it) } // and then making a safe api call
        .log()

    private enum class WorkerProgressState {
        POSTING_GAME_DATA,
        GETTING_VIDEO_URLS,
        UPLOADING_VIDEO,
        SIGNALING_VIDEO_UPLOAD,
        FINISHED,
    }
}
```

Writing all this code by hand with try/catch, `map`, `let`, `also`, `finally` and other stuff would produce 3 to 4 times
more code, all while still being error-prone and fragile. This code correctly halts worker's execution, skipping
unnecessary steps while making sure the worker's body never throws or leaves wasted resources behind.

### Example: Handling a financial transaction

This example is illustrated in
the [sample app](https://github.com/respawn-app/ApiResult/tree/master/app/src/main/kotlin/pro/respawn/apiresult/sample)
