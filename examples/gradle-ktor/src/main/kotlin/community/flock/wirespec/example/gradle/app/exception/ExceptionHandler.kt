package community.flock.wirespec.example.gradle.app.exception

import community.flock.wirespec.generated.kotlin.Error

fun handleException(exception: AppException): Error = exception.run {
    when (this) {
        is TodoIdNotValidException -> Error(
            code = 400,
            description = message
        )

        is TodoNotFoundException -> Error(
            code = 404,
            description = message
        )
    }
}
