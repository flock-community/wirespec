package community.flock.wirespec.example.maven.custom.app.todo

import community.flock.wirespec.example.maven.custom.app.common.Consumer
import community.flock.wirespec.example.maven.custom.app.common.Producer
import community.flock.wirespec.generated.kotlin.model.PotentialTodoDto
import community.flock.wirespec.generated.kotlin.model.TodoDto
import community.flock.wirespec.generated.kotlin.model.TodoId
import java.util.UUID

object TodoConsumer : Consumer<PotentialTodoDto, Todo> {
    override fun PotentialTodoDto.consume(): Todo = Todo(
        id = Todo.Id(value = UUID.randomUUID().toString()),
        name = Name(name),
        done = done,
    )
}

object TodoProducer : Producer<Todo, TodoDto> {
    override fun Todo.produce(): TodoDto = TodoDto(
        id = TodoId(id.value),
        name = name.value,
        done = done,
    )
}
