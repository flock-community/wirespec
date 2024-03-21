package community.flock.wirespec.examples.app.todo

import community.flock.wirespec.examples.app.common.Consumer
import community.flock.wirespec.examples.app.common.Producer
import community.flock.wirespec.examples.app.common.invoke
import community.flock.wirespec.generated.kotlin.PotentialTodoDto
import community.flock.wirespec.generated.kotlin.TodoDto
import community.flock.wirespec.generated.kotlin.TodoId
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
        name = name(),
        done = done,
    )
}
