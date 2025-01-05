package pro.respawn.apiresult

import pro.respawn.apiresult.ApiResult.Loading

/**
 * An exception that is thrown when an attempt to retrieve a result of an [ApiResult] is being made when the
 * result is [Loading]
 */
public class NotFinishedException(
    message: String? = "ApiResult is still in the Loading state",
) : IllegalStateException(message)

/**
 * Exception representing unsatisfied condition when using [errorIf]
 */
public open class ConditionNotSatisfiedException(
    message: String? = "ApiResult condition was not satisfied",
) : IllegalArgumentException(message)
