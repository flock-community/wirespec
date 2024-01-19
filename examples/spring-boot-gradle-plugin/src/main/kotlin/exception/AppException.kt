package community.flock.wirespec.examples.app.exception

sealed class AppException(override val message: String) : RuntimeException(message) {
    class TodoNotFoundException(id: String) : AppException("Todo with id: $id, not found")
}