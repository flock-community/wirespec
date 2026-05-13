package community.flock.wirespec.example.maven.custom.app.todo

import community.flock.wirespec.example.maven.custom.app.common.Consumer
import community.flock.wirespec.example.maven.custom.app.common.Producer
import community.flock.wirespec.generated.kotlin.model.PotentialTodoDto
import community.flock.wirespec.generated.kotlin.model.TodoDto
import community.flock.wirespec.generated.kotlin.model.TodoId
import java.util.UUID

// Kept as an `object` (not a lambda) so callers can `import TodoConsumer.consume`
// and call `dto.consume()` as an extension — see TodoHandler.kt.
object TodoConsumer : Consumer<PotentialTodoDto, Todo> { // NOSONAR S6516
    override fun PotentialTodoDto.consume(): Todo = Todo(
        id = Todo.Id(value = UUID.randomUUID().toString()),
        name = Name(name),
        done = done,
    )
}

// Kept as an `object` (not a lambda) so callers can `import TodoProducer.produce`
// and call `domain.produce()` as an extension — see TodoHandler.kt.
object TodoProducer : Producer<Todo, TodoDto> { // NOSONAR S6516
    override fun Todo.produce(): TodoDto = TodoDto(
        id = TodoId(id.value),
        name = name.value,
        done = done,
    )
}
