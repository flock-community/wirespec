package community.flock.wirespec.example.maven.custom.app.todo

import community.flock.wirespec.generated.kotlin.model.PotentialTodoDto
import community.flock.wirespec.generated.kotlin.model.TodoDto
import community.flock.wirespec.generated.kotlin.model.TodoId
import java.util.UUID

object TodoConsumer {
    fun PotentialTodoDto.consume(): Todo = Todo(
        id = Todo.Id(value = UUID.randomUUID().toString()),
        name = Name(name),
        done = done,
    )
}

object TodoProducer {
    fun Todo.produce(): TodoDto = TodoDto(
        id = TodoId(id.value),
        name = name.value,
        done = done,
    )
}
