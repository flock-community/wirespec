package community.flock.wirespec.examples.todo_app

import community.flock.wirespec.generated.Todo
import community.flock.wirespec.generated.TodoId
import community.flock.wirespec.generated.TodoInput
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/todos")
class TodoController {

    private val todos = mutableListOf<Todo>()

    @GetMapping("/")
    fun list(): List<Todo> = todos

    @GetMapping("/:id")
    fun get(@PathVariable id: String) = todos.find { it.id.value == id }

    @PostMapping("/")
    fun post(input: TodoInput) =
        Todo(
            id = TodoId(value = UUID.randomUUID().toString()),
            name = input.name,
            done = input.done,
        ).run { todos.add(this) }
}
