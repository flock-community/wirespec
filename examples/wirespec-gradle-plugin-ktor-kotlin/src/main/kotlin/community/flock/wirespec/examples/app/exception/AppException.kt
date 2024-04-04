package community.flock.wirespec.examples.app.exception

import community.flock.wirespec.examples.app.common.invoke
import community.flock.wirespec.examples.app.todo.Todo

sealed class AppException(override val message: String) : RuntimeException(message)

sealed class TodoException(message: String) : AppException(message)

class TodoIdNotValidException(invalidId: String) : TodoException("TodoId: $invalidId, not valid")

class TodoNotFoundException(id: Todo.Id) : TodoException("Todo with id: ${id()}, not found")
