package community.flock.wirespec.examples.app

import community.flock.wirespec.examples.app.exception.AppException
import community.flock.wirespec.examples.app.exception.TodoNotFoundException
import community.flock.wirespec.examples.app.exception.TodoIdNotValidException
import community.flock.wirespec.generated.kotlin.Error
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class AppExceptionHandler {

    @ExceptionHandler(AppException::class)
    fun handleException(exception: AppException): ResponseEntity<Error> = exception.run {
        when (this) {
            is TodoIdNotValidException -> Error(
                code = 400,
                description = message
            )

            is TodoNotFoundException -> Error(
                code = 404,
                description = message
            )
        }.run { ResponseEntity.status(code.toInt()).body(this) }
    }
}
