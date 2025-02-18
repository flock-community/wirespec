package community.flock.wirespec.example.maven.custom.app.exception

import community.flock.wirespec.example.maven.custom.app.todo.Todo

sealed class AppException(
    override val message: String,
) : RuntimeException(message)

sealed class TodoException(
    message: String,
) : AppException(message)

class TodoIdNotValidException(
    invalidId: String,
) : TodoException("TodoId: $invalidId, not valid")

class TodoNotFoundException(
    id: Todo.Id,
) : TodoException("Todo with id: ${id.value}, not found")
